# Arguments and results — open work

Where the design discussion about passing data between screens landed, what was ruled out and why,
and what is left to build. Evidence is cited to the file it came from: this repository, or the
sources of Navigation3 1.1.1 and lifecycle 2.11.0 as published to Maven.

## Vocabulary

Two mechanisms, told apart by direction. Using one for the other is the mistake this document is
mostly about.

| | Result | Argument |
|---|---|---|
| Direction | a closing screen → a screen already in the stack | an opening screen → screens about to be pushed |
| Who waits | the producer is gone, the consumer stays | the consumer does not exist yet |
| Lifetime | one delivery, gone once consumed | as long as the screens that need it |
| Natural home | a mailbox (`ScreenResults` below) | the route, or the flow that owns the screens |

## 1. Results: rename and type the store

`NavigationResultStore` (`navigation/api/.../presentation/result/`) is misnamed twice over and
unsafe in two ways:

- **`Navigation`** — it holds no route, no stack and no navigation; it is a keyed mailbox.
- **`Store`** — reads as durable, read-many storage. `consume` removes the value: it is one-shot.
- `results: StateFlow<Map<String, Any>>` exposes every feature's pending values to every screen.
- The type is asserted where the value is read. A producer that wrote an `Int` and a consumer that
  asks for a `String` get `null`, silently (`KClass.isInstance` in `NavigationResultStoreImpl`).

- [ ] Rename to `ScreenResults`, with a typed key declared next to the feature's routes:
      `class ResultKey<T : Any>(val name: String, val type: KClass<T>)` and
      `inline fun <reified T : Any> resultKey(name: String)`.
- [ ] Surface `post(key, value)`, `consume(key)`, `clear(key)`, and
      `pending: StateFlow<Set<String>>` in place of the whole map.
- [ ] `LocalScreenResults = staticCompositionLocalOf<ScreenResults> { NoOpScreenResults }` in `api`,
      with a no-op default so previews compose — the same shape as `LocalNavigator`.
- [ ] Install it the way `LocalNavigationHostRenderer` already is: a `@Provides @IntoSet` function
      returning `ProvidedValue<*>` in `NavigationWiring`. The composition root already spreads
      `graph.providedValues`, so nothing else changes.
- [ ] Add `ResultEffect(key) { value -> … }`: collects `pending`, consumes in a `LaunchedEffect`.
      The waiting screen is not composed while covered (see §2), so the effect runs exactly when
      the user comes back to it. The composable forwards to its state holder; it does not decide.
- [ ] Document that results are in memory only and are lost on process death.
- [ ] Later: move it out of `navigation` altogether — nothing in it is navigation.

Results are for backward data only. Nothing forward-bound goes through this store.

## 2. Arguments across several screens

**The requirement.** A value is set on screen 1 and read on screens 2–6, without threading it
through every intermediate route, and it must disappear once those screens are gone.

### Ruled out: a consumer reference count released on dispose

The proposal was: each consumer registers on the argument, unregisters in `DisposableEffect`, and
the argument is removed when the count reaches zero. It fails on its trigger:

- **Dispose does not mean "done".** Navigation3 composes only the top entry —
  `SinglePaneScene.entries = listOf(entry)` (`androidx/navigation3/scene/SinglePaneScene.kt`). A
  screen that pushes the next one leaves composition while it is still in the stack, so the count
  hits zero one push early and screen 6 finds nothing. A configuration change disposes everything
  too.
- **Moving the release to `ViewModel.onCleared`** fixes the timing but only re-derives stack
  membership, which the back stack already states.
- A forgotten or crashed release leaks the value for the life of the process.
- The value is lost on process death while the routes that need it are restored.
- A deep link straight to screen 4 finds no value, because the producer never ran.
- Nothing in screen 6's route says it depends on the value.

### Chosen: the flow's root entry owns the data, in its `SavedStateHandle`

Mount the flow as a nested `NavigationHost` inside one entry. That entry's ViewModel holds both the
flow's inner back stack and the shared value:

```kotlin
@Inject
class CheckoutFlowViewModel(state: SavedStateHandle) : ViewModel() {
    var steps: List<CheckoutStep> by state.saved { listOf(CheckoutStep.Amount) }
        private set

    var draft: CheckoutDraft? by state.saved { null }

    fun updateSteps(next: List<CheckoutStep>) {
        steps = next
    }
}
```

Why this holds, with the source for each claim:

- **Lifetime is the flow's.** `ViewModelStoreNavEntryDecorator` clears the entry's store on pop —
  `onPop = { key -> viewModelStoreProvider.clearKey(key) }`
  (`androidx/lifecycle/viewmodel/navigation3/ViewModelStoreNavEntryDecorator.kt`). No prune, no
  count, no key to remove.
- **It survives a configuration change.** The same file: "Configuration changes are now handled
  internally."
- **It survives process death**, through `SavedStateHandle`. `saved { }` is
  `androidx.lifecycle.serialization.SavedStateHandle.saved` and takes any `@Serializable` type
  (`SavedStateHandleDelegate.kt`, present in the KMP artifacts).
- **Nested saved state chains correctly.** The ViewModel decorator passes
  `LocalSavedStateRegistryOwner.current` to each entry's owner, and the outer entry is what
  provides it. `NavigationView` installs both decorators on every host, nested ones included.

Neither piece does this alone, which is why the combination is the answer:

- `SaveableStateHolder` is keyed per entry — `SaveableStateProvider(entry.contentKey)`, removed on
  pop (`SaveableStateHolderNavEntryDecorator.kt`) — and is reachable only from inside that entry's
  own composition. It exists for a screen's own `rememberSaveable` state.
- A step's own `SavedStateHandle` is per entry as well. Navigation3 has no equivalent of
  Navigation2's `previousBackStackEntry.savedStateHandle`, so one entry cannot reach another's.

Limits:

- Only serializable, small values. A bitmap, a parsed document or an open connection belongs in a
  repository, with only its id in the handle.
- It needs a shared entry. Without nesting there is no common owner — see the fallbacks.

- [ ] Add a worked flow (checkout, say) to `navigation/README.md` showing this pattern end to end.
- [ ] Consumers treat a missing value as "restart the flow", never as a crash.

### Fallbacks when nesting is not wanted

- **`flowId` in the routes + a DI scope or repository keyed by it.** One small field rather than
  a chain of values. Survives process death if the repository persists; a deep link works.
- **A scoped argument store whose lifetime is derived from the stack**, not counted:
  `fun interface ArgumentScope { fun isAliveIn(stack: List<Route>): Boolean }`, with
  `put(key, value, scope)` / `get(key)` / `remove(key)`, and `pruneFor(stack)` called by the root
  state holder on every back stack change. Scoping to a sealed flow type
  (`whileInStack { it is CheckoutRoute }`) makes back, `popBackTo`, a guard rewrite and a deep
  link all correct at once. In memory only. **Never prune inside a guard** — `evaluate` must stay
  pure and runs several times per navigation.

| | Flow entry + `SavedStateHandle` | `flowId` + scope / repository | Scoped argument store |
|---|---|---|---|
| Lifetime | Navigation3 clears it | while the id is in the stack | derived from the stack |
| Process death | restored | restored if persisted | lost |
| Deep link | the flow starts from its root | works | `null` unless something sets it |
| Extra machinery | none | a scope or a repository | store, bind, prune |

## 3. The kit, before nested hosts are the recommended pattern

Cost scales with nesting **depth**, not with how many hosts exist: only the entries of the current
scene are composed, so `Root → Flow → SubFlow` means three mounted hosts. Each `NavDisplay` still
carries its own scene state, predictive-back registration, transition state, saveable state holder
and per-entry ViewModel stores (`androidx/navigation3/ui/NavDisplay.kt`), on top of what
`NavigationView` adds.

- [ ] **`entryProvider` is rebuilt on every recomposition.** `NavigationView.kt` calls
      `entryProvider(builder = entries)` outside `remember`. Navigation3's `entryProvider` is a plain
      inline function that allocates an `EntryProviderScope` and two maps and runs the whole
      builder (`androidx/navigation3/runtime/EntryProvider.kt`). Wrap it in `remember(entries)`.
- [ ] **App-wide guards run on every host's stack.** A nested host whose stack holds a guarded
      route gets that guard's rewrite — confirmed with a standalone run of the real runner and
      navigator, where a second host received its own sign-in prompt. If the nested host did not
      register the route the guard inserted, Navigation3's default fallback throws
      `IllegalStateException("Unknown screen …")`. Decide between applying app-wide guards to the
      root host only (`NavigationHostParams.guards`) and giving guards a host scope.
- [ ] **Every mounted host collects the guard invalidations** (`hostRunner.invalidations` in
      `NavigationView.kt`), so N mounted hosts run N full revalidations per emission.
- [ ] **One `BackDispatcher` is shared by every host.** `NavigationHostRendererImpl` receives the
      single app-scoped instance and each host provides it as `LocalBackDispatcher`, so a callback
      registered inside a nested host is consulted by the outer host's `popBack()`. Navigation3's
      own `NavigationBackHandler`s nest correctly; ours has no hierarchy.

Guidance to write down alongside the pattern:

- Keep nesting to depth 1 for a flow.
- Do not nest for tabs: transitions do not run across a host boundary, and that is where it shows.
- Navigation3's own documentation says nothing about nested `NavDisplay`s; it is an open request in
  [android/nav3-recipes#176](https://github.com/android/nav3-recipes/issues/176).

## 4. Where state survives

| Holder | Configuration change | Process death | Entry popped |
|---|---|---|---|
| `remember` | lost | lost | lost |
| `rememberSaveable` | kept | kept, if saveable | removed |
| Entry-scoped ViewModel | kept | lost | cleared |
| `SavedStateHandle` | kept | kept | cleared |
| App-scoped singleton (`@SingleIn(AppScope::class)`) | kept | lost | kept — nothing clears it |
| Kotlin `object` | kept | lost | kept |
| Activity-scoped graph | lost | lost | — |

An app-scoped result or argument store survives rotation, so a configuration-change test makes it
look correct while it is empty after process death and never clears itself. Verify these
mechanisms against process death, not rotation. iOS has no configuration change at all, so shared
code cannot lean on that survival either.
