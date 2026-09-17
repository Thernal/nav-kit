# Guards — deciding which stacks may exist

Catalog group **Guards**: [Members area](#simple-members-area) (simple) and
[401 and a PIN](#real-life-401-and-a-pin) (real life). The third kind of guard — a rule about
*leaving* a screen — is in [`backoverride`](../backoverride/README.md).

A guard is where an access rule lives instead of an `if` before every `push`. It sees the stack as
it stands (`old`) and the stack being proposed (`new`), and answers with the stack that may exist.
The host runs guards before anything renders — for commands, for stacks handed to it directly (a deep
link, a restored stack), and whenever a guard announces its answer may have changed.

| The rule is about… | Write | Here |
|---|---|---|
| which destinations may be on the stack | `RouteGuard<Marker>` | [`AuthGuard.kt`](AuthGuard.kt) |
| something that cannot be answered yet | a guard returning `GuardVerdict.Deferred` | [`PinGuard.kt`](PinGuard.kt) |
| leaving a screen | `NavigationGuard` comparing `old` and `new` | [`UnsavedWorkGuard.kt`](../backoverride/UnsavedWorkGuard.kt) |

| File | What is in it |
|---|---|
| [`GuardsRoutes.kt`](GuardsRoutes.kt) | the routes, the `AuthGuarded` and `PinProtected` markers, `SignInRoute(next)`, the transient `PinEntryRoute`, the two `BlockReason`s |
| [`SessionStore.kt`](SessionStore.kt) | whether there is a session, and a hot stream of changes |
| [`AuthGuard.kt`](AuthGuard.kt) | a destination rule with invalidations |
| [`PinSession.kt`](PinSession.kt) | stands in for what a 401 lands in: lock, submit, await unlock |
| [`PinGuard.kt`](PinGuard.kt) | a guard that defers |
| [`GuardsScreens.kt`](GuardsScreens.kt) | the members, sign-in, vault and PIN screens |
| [`GuardsBindings.kt`](GuardsBindings.kt) | contributes both guards into the kit's guard set |

## Simple: Members area

**Mark the routes.** The marker is named after the guard that reads it:

```kotlin
interface AuthGuarded : Route

data object MembersSecretRoute : GuardsRoute, AuthGuarded

/** Carries where the user was going, so signing in continues there. */
data class SignInRoute(val next: Route?) : GuardsRoute

data object SignInRequired : BlockReason {
    override val message: String = "Sign in to continue"
}
```

**Write the rule.**

```kotlin
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
```

**Contribute it.**

```kotlin
@Provides
@IntoSet
fun provideAuthGuard(session: SessionStore): NavigationGuard {
    return AuthGuard(session)
}
```

**Continue after signing in.**

```kotlin
ExampleAction(
    label = "Sign in and continue",
    onClick = {
        session.signIn()
        val next = route.next
        if (next == null) navigator.popBack() else navigator.replace(next)
    },
)
```

What to notice:

- **The narrowing is passed to the constructor**, so the guard applies to `AuthGuarded` routes and
  nothing else — a subclass cannot skip it. Protecting another screen is adding the marker to its route.
- **It judges every matching route in the proposed stack**, not only the one entering. "The secret page
  requires a session" holds however the page got there — a push, a `replaceAll`, a deep link.
- **It redirects by substitution.** The secret route is replaced where it sits by `SignInRoute`, and
  the original destination rides along in `next`. A redirect that forgets `next` loses the user's intent.
- **`invalidations` is what makes signing *out* work.** Nothing navigates when a session ends. Every
  mounted host collects the guard's `invalidations` and revalidates its stack in place, so the secret
  page leaves on its own.

```kotlin
class SessionStore {
    private val state = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = state.asStateFlow()
    // drop(1): a StateFlow replays its value to each new collector; the host needs the *changes*.
    val changes: Flow<Unit> = state.drop(1).map { }
}
```

**Try it:** open *Members area* signed out and open the secret page — you land on sign-in, which knows
where you were heading. Sign in and continue. Now sign out from the secret page: it is replaced by the
sign-in screen without any navigation call.

## Real life: 401 and a PIN

A 401 arrives while the user is somewhere protected. The destination is still right; the session just
needs re-proving. Redirecting would lose where the user was going, so the guard **defers**: it says what
may exist meanwhile and hands back a suspending function that answers once it knows.

```kotlin
interface PinProtected : Route
data object VaultRoute : GuardsRoute, PinProtected

/** The placeholder the guard shows while it waits. Transient: it must not survive process death. */
data object PinEntryRoute : GuardsRoute, TransientRoute
```

```kotlin
class PinGuard(private val session: PinSession) : NavigationGuard {
    override val invalidations: Flow<Unit> = session.changes

    override fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict {
        if (!session.locked.value) {
            return GuardVerdict.Resolved(new)
        }
        if (new.none { route -> route is PinProtected }) {
            return GuardVerdict.Resolved(new)
        }
        return GuardVerdict.Deferred(meanwhile = lockedStack(old)) { navigator ->
            navigator.push(PinEntryRoute)
            if (session.awaitUnlock()) {
                GuardVerdict.Resolved(new)   // the stack the user asked for
            } else {
                GuardVerdict.Resolved(stack = lockedStack(old), reason = PinRequired)
            }
        }
    }

    // Everything except the protected routes — but never an empty stack, which the runner rejects.
    private fun lockedStack(stack: ImmutableList<Route>): ImmutableList<Route> {
        val visible = stack.filterNot { route -> route is PinProtected }
        if (visible.isEmpty()) {
            return stack
        }
        return visible.toImmutableList()
    }
}
```

```kotlin
class PinSession {
    private val lockedState = MutableStateFlow(false)
    private val submissions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val locked: StateFlow<Boolean> = lockedState.asStateFlow()
    val changes: Flow<Unit> = lockedState.drop(1).map { }

    fun lock() { lockedState.value = true }            // what an HTTP interceptor calls on a 401
    fun submit(pin: String) { submissions.tryEmit(pin) }

    suspend fun awaitUnlock(): Boolean {
        val didUnlock = submissions.first() == "1234"
        if (didUnlock) {
            lockedState.value = false                  // so the guard can answer synchronously next time
        }
        return didUnlock
    }
}
```

Two paths reach the deferral, and the host drives both the same way:

- **Opening the vault while locked:** `push(VaultRoute)` is deferred. The command answers
  `NavigationOutcome.Deferred`, the lobby stays on screen, the host awaits the deferral, which pushes the
  PIN screen; the right PIN continues to the vault.
- **Locking while inside the vault:** the session emits a change, the host revalidates, the guard defers
  with `meanwhile` = the stack without the vault. The PIN screen goes on top; the right PIN brings the
  same stack back — not a rebuilt one.

What to notice — and all of it is the kit's doing rather than the guard's:

- **Only the mounted host awaits a deferral**, one at a time, keyed on the stack that was attempted. A
  double tap or a revalidation storm cannot show the prompt twice; unmounting the host cancels the wait.
- **The deferral's own navigator is part of the wait.** Its `push(PinEntryRoute)` does not count as the
  user walking away.
- **Walking away abandons it.** Back out of the PIN screen, or let a deep link arrive, and the wait ends;
  its answer is never applied. Opening the vault again asks again.
- **After settling, the guard answers synchronously.** `awaitUnlock` flips `locked` before it returns, so
  when the settled stack is applied and guarded again, `evaluate` returns `Resolved(new)`. A guard that
  deferred a second time for the same stack would never converge.
- **`PinEntryRoute` is a `TransientRoute`.** A coroutine does not survive process death; a restored
  stack showing a PIN prompt with nothing left to answer it would be a screen nobody can leave. The host
  drops transient routes from the stack it first composes.

**Try it:** open *401 and a PIN*, tap *Simulate a 401*, then *Open the vault*: the PIN screen appears.
Enter `1234` → the vault. Inside the vault, *Simulate a 401* again: the vault leaves, the prompt appears,
`1234` brings it back. Enter a wrong PIN instead and the vault stays shut.

## Doing this in your app

**A sign-in requirement**

1. `interface AuthGuarded : Route`; add it to every protected route (or to a sealed parent to protect a
   whole graph).
2. `data class SignInRoute(val next: Route?) : Route`, registered in **every host whose stack can
   contain a protected route** — a nested host included.
3. `class AuthGuard(session) : RouteGuard<AuthGuarded>({ it as? AuthGuarded })` returning
   `SignInRoute(next = route)` when signed out; `invalidations` = the session's changes, hot, `drop(1)`.
4. Contribute it `@IntoSet NavigationGuard`.
5. After signing in: `navigator.replace(next)`.

**An asynchronous check**

1. Keep the thing being awaited outside composition, with a synchronous "already known" answer.
2. Return `GuardVerdict.Deferred(meanwhile) { navigator -> … }` only while the answer is unknown.
3. Push a `TransientRoute` placeholder from the lambda; register it in the host.
4. Return `Resolved(new)` to continue, or `Resolved(meanwhile-like stack, reason)` to refuse.

## Pitfalls

- **Access checks at call sites** instead of a guard — a deep link or a `replaceAll` walks past them.
- **A substitute route not registered in a host** the guard rewrites — `IllegalStateException: Unknown screen`.
- **A cold `invalidations` flow doing work per collector** — every mounted host collects it.
- **Forgetting `drop(1)` on a `StateFlow`** — every host revalidates once on mount for nothing.
- **A `meanwhile` that drops every route** — the runner rejects a guard that empties the stack.
- **A deferral that defers again after settling** — never converges; cache the answer.
- **A placeholder that is not a `TransientRoute`** — can come back after process death with nothing to
  resolve it.
- **Side effects in `evaluate`** — it runs several times per navigation; keep it pure and cheap.

## Read more

- [Guards](../../../../../../../../../../navigation/api/README.md#guards) in the API guide — verdicts, the runner's rules, registration
- [Nested hosts](../tabs/README.md) — an application-wide guard inside a tab
- [Deep links](../deeplinks/README.md) — a link to a guarded route
- [All examples](../../../../../../../../../README.md#the-examples)
