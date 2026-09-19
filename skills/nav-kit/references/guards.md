# Guards

## Contents

1. Pick the kind
2. Recipe: members-only screens with a sign-in redirect
3. Recipe: remove screens when access ends
4. Recipe: forbid leaving a screen
5. Recipe: decide asynchronously (401, PIN, token refresh)
6. Guards for one host only
7. The runner's rules
8. Testing a guard

A guard decides which **stacks** may exist. `old` is the stack in effect, `new` the one proposed; the
verdict is the stack that may exist.

```kotlin
fun interface NavigationGuard {
    val invalidations: Flow<Unit> get() = emptyFlow()
    fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict
}
```

| Verdict | Means |
|---|---|
| `GuardVerdict.Resolved(new)` | allow |
| `GuardVerdict.Resolved(old, reason)` | refuse (also refuses pops) |
| `GuardVerdict.Resolved(other, reason)` | rewrite: redirect, insert, drop |
| `GuardVerdict.Deferred(meanwhile) { navigator -> … }` | cannot answer yet |

Package: `io.thernal.navkit.navigation.api.presentation.guard`. Guards run for every navigator command,
for every stack handed to a host directly (deep link, restored state) and when `invalidations` emits.

## 1. Pick the kind

| The rule is about… | Write |
|---|---|
| which destinations may be on the stack (auth, role, entitlement, feature flag) | `RouteGuard<Marker>` |
| leaving a screen (unsaved work, a running payment) | `NavigationGuard` comparing `old` and `new` |
| an answer not available synchronously | either kind, returning `GuardVerdict.Deferred` |

## 2. Recipe: members-only screens with a sign-in redirect

```kotlin
// 1. The marker, named after the guard.
interface AuthGuarded : Route

// 2. Mark protected routes (a sealed parent protects a whole graph).
data object AccountRoute : Route, AuthGuarded
sealed interface AdminRoute : Route, AuthGuarded

// 3. The substitute carries the intent.
data class SignInRoute(val next: Route?) : Route

// 4. The app's own refusal vocabulary.
data object SignInRequired : BlockReason {
    override val message: String = "Sign in to continue"
}

// 5. The guard.
class AuthGuard(private val session: SessionStore) : RouteGuard<AuthGuarded>({ it as? AuthGuarded }) {
    override val reason: BlockReason = SignInRequired
    override val invalidations: Flow<Unit> = session.changes

    override fun redirect(route: AuthGuarded, stack: ImmutableList<Route>): Route? {
        if (session.signedIn.value) {
            return null
        }
        return SignInRoute(next = route)
    }
}

// 6. Contribute it.
@Provides
@IntoSet
fun provideAuthGuard(session: SessionStore): NavigationGuard {
    return AuthGuard(session)
}
```

7. Register `SignInRoute` in **every host whose stack can hold an `AuthGuarded` route**, nested hosts
   included — otherwise that host fails on the substituted route (or renders its `fallback`).
8. The sign-in screen continues: `session.signIn(); if (next == null) navigator.popBack() else navigator.replace(next)`.
9. Provide `SessionStore` app-scoped (`@SingleIn(AppScope::class)`): guards run outside composition.

`RouteGuard` facts: it judges every matching route in `new` (not only entering ones), replaces each where it
sits, de-duplicates the result, and its `evaluate` is `final` so the narrowing cannot be skipped. `redirect`
returning `null` lets the route stand.

## 3. Recipe: remove screens when access ends

Nothing navigates when a session ends; `invalidations` makes every mounted host revalidate its stack in
place, so a route that became invalid is rewritten immediately.

```kotlin
class SessionStore {
    private val state = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = state.asStateFlow()
    val changes: Flow<Unit> = state.drop(1).map { }   // drop(1): StateFlow replays on collect
    fun signIn() { state.value = true }
    fun signOut() { state.value = false }
}
```

Keep `invalidations` hot (derived from a `StateFlow`/`SharedFlow`): every mounted host collects it.

## 4. Recipe: forbid leaving a screen

```kotlin
data object UnsavedWork : BlockReason {
    override val message: String = "The article has unsaved changes"
}

class UnsavedWorkGuard(private val drafts: ArticleDraftStore) : NavigationGuard {
    override fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict {
        val wasEditing = old.any { it is ArticleEditorRoute }
        val isStillEditing = new.any { it is ArticleEditorRoute }
        if (wasEditing && !isStillEditing && drafts.hasUnsavedChanges) {
            return GuardVerdict.Resolved(stack = old, reason = UnsavedWork)
        }
        return GuardVerdict.Resolved(new)
    }
}
```

- Refuses back, `popBackTo`, `replaceAll`, `navigate`, deep links — every stack change.
- The dirty state lives in an app-scoped object (snapshot state works well for a text field bound to it).
- **Phrase it about this screen's own routes.** `old` does not change during the fold, so a rule that
  rejects any difference between `old` and `new` undoes every rewriting guard and the runner fails after 8
  rounds.
- Callers see the refusal as `NavigationOutcome.Rewritten(reason = UnsavedWork)`, or `false` from a pop.

## 5. Recipe: decide asynchronously

```kotlin
interface PinProtected : Route
data object VaultRoute : Route, PinProtected
data object PinEntryRoute : Route, TransientRoute        // placeholder; register it in the host

class PinGuard(private val session: PinSession) : NavigationGuard {
    override val invalidations: Flow<Unit> = session.changes

    override fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict {
        if (!session.locked.value || new.none { it is PinProtected }) {
            return GuardVerdict.Resolved(new)
        }
        return GuardVerdict.Deferred(meanwhile = lockedStack(old)) { navigator ->
            navigator.push(PinEntryRoute)
            if (session.awaitUnlock()) {                  // suspends; sets locked = false on success
                GuardVerdict.Resolved(new)                // continue where the user was going
            } else {
                GuardVerdict.Resolved(stack = lockedStack(old), reason = PinRequired)
            }
        }
    }

    private fun lockedStack(stack: ImmutableList<Route>): ImmutableList<Route> {
        val visible = stack.filterNot { it is PinProtected }
        return if (visible.isEmpty()) stack else visible.toImmutableList()   // never empty
    }
}
```

Rules, each load-bearing:

- **Answer synchronously once settled.** The settled stack is applied and guarded again; defer again for
  the same stack and it never converges. Update the cached state before `resolve` returns.
- **The placeholder is a `TransientRoute`** and has an entry in the host. A restored or recreated host drops
  transient routes, so no prompt comes back without a coroutine to answer it.
- **`meanwhile` follows the runner's rules** — never empty, no reordering.
- **Only a mounted host awaits**, one deferral at a time keyed on the attempted stack; unmounting cancels it.
  The command that triggered it answers `NavigationOutcome.Deferred`.
- **The wait is abandoned** if the stack moves without it — the user backs out of the placeholder, a deep link
  arrives, the host is handed another list. Its answer is discarded. Pushes by the `navigator` passed to the
  lambda are part of the wait.
- Deferral also starts from revalidation: locking while on a protected screen defers with `meanwhile`
  without it, then restores the same stack.

## 6. Guards for one host only

```kotlin
NavigationHostParams(
    backStack = steps,
    onBackStackChange = flow::onStepsChange,
    guards = persistentListOf(WizardOrderGuard(flowState)),
)
```

For a flow's internal rules, or a guard whose dependencies live in a scope the app graph cannot reach. They
run after the application-wide guards. Application-wide guards (the `@IntoSet` ones) still apply to this host.

## 7. The runner's rules

1. Guards fold in order; each sees the previous output as `new`. `old` stays fixed.
2. The fold repeats until a whole round rewrites nothing — a route a guard inserted is guarded too.
   After 8 rounds: `IllegalStateException: Navigation guards did not settle after 8 rounds; two of … rewrite each other's stack.`
3. A `Deferred` stops the fold.
4. A verdict that empties a non-empty stack, reorders kept routes or adds a duplicate fails:
   `… emptied the back stack`, `… reordered the routes it kept`, `… returned a stack with a duplicate route`.
5. `evaluate` must be pure and cheap: it runs once per guard per round, per command, per handed stack, per
   invalidation. No I/O, no logging side effects, no argument pruning.

`BlockReason` reaches `NavigationOutcome.Rewritten.reason` and `NavigationEvent.Blocked.reason`.

## 8. Testing a guard

```kotlin
@Test
fun signedOutAccountBecomesSignIn() {
    val runner = NavigationGuardRunnerImpl(listOf(AuthGuard(SessionStore())))

    val verdict = runner.resolve(
        old = persistentListOf(HomeRoute),
        new = persistentListOf(HomeRoute, AccountRoute),
    )

    assertEquals(persistentListOf(HomeRoute, SignInRoute(next = AccountRoute)), verdict.stack)
    assertEquals(SignInRequired, verdict.reason)
}
```

- `resolve` collapses a deferral to `meanwhile`; use `resolveDeferrable` to assert a `GuardVerdict.Deferred`
  and invoke its `resolve(navigator)` in `runTest` with a `BackStackNavigator` (see events-and-testing.md).
- Revalidation is `resolve(old = current, new = current)` — test sign-out that way.
- Test a transition guard with `old` containing the screen and `new` without it.
