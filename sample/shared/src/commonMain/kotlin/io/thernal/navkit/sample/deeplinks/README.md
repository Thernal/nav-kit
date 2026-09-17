# Deep links — from a URI to a guarded stack

Catalog group **Deep links**: [One link, one route](#simple-one-link-one-route) (simple) and
[Campaign links](#real-life-campaign-links) (real life). The platform side — an Android intent filter
and an iOS URL type — is in [the sample README](../../../../../../../../../README.md#platform-entry-points).

A link travels through four hands, and each does one thing:

```
platform (intent, onOpenURL) ─publish─▶ DeepLinkIngress ═══ DeepLinkEvents
                                                                │ collected by the root (SampleApp)
                                                                ▼
              DeepLinkDispatcher ─ removes the registered base ─▶ the handler that owns the page
                                                                │ DeepLinkOutcome
                                                                ▼
                        RootViewModel.onDeepLink(routes) ─▶ root host guards the stack ─▶ screen
```

| File | What is in it |
|---|---|
| [`DeepLinkHandlers.kt`](DeepLinkHandlers.kt) | three handlers: one page to one route, a multi-screen stack that reads its source, a guarded destination |
| [`DeepLinkScreens.kt`](DeepLinkScreens.kt) | a playground that publishes links by hand, and the screens the links open |
| [`DeepLinkRoutes.kt`](DeepLinkRoutes.kt) | the routes |
| [`DeepLinksBindings.kt`](DeepLinksBindings.kt) | contributes the handlers into the kit's handler set |
| [`../app/SampleBindings.kt`](../app/SampleBindings.kt) | registers the two bases links start with: `navkit://` and `https://example.com` |
| [`../app/SampleApp.kt`](../app/SampleApp.kt) | where links are resolved and applied |
| [`../app/DeepLinkLog.kt`](../app/DeepLinkLog.kt) | where what a handler decided is recorded |

## Simple: One link, one route

The application first says which links are its own. Each `DeepLinkBase` is a prefix — a scheme, or a
web origin — and the parser removes the one a link starts with before looking for the page:

```kotlin
// app/SampleBindings.kt
@Provides
@IntoSet
fun provideAppSchemeBase(): DeepLinkBase {
    return DeepLinkBase("navkit://")        // the scheme the manifest and Info.plist declare
}

@Provides
@IntoSet
fun provideWebOriginBase(): DeepLinkBase {
    return DeepLinkBase("https://example.com")
}
```

A feature then only names its page:

```kotlin
class ProductDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("product")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        val id = request.deepLink.pathSegments.getOrNull(1)
            ?: return DeepLinkOutcome.Rejected(reason = "A product link needs an id")
        // The catalog stays at the bottom, so back from the product goes somewhere inside the app.
        return DeepLinkOutcome.Navigate(
            routes = listOf(CatalogRoute, LinkPlaygroundRoute, ProductRoute(id = id)),
        )
    }
}
```

```kotlin
@Provides
@IntoSet
fun provideProductHandler(): DeepLinkHandler {
    return ProductDeepLinkHandler()
}
```

The playground delivers a link the way a platform entry point does — it only publishes:

```kotlin
private fun deliver(ingress: DeepLinkIngress, log: DeepLinkLog, uri: String, source: DeepLinkSource) {
    if (!ingress.publish(uri = uri, source = source)) {
        log.recordRefused(uri)
    }
}
```

What to notice:

- **Every registered form of a link reaches the same handler.** The parser removes the registered base
  a link starts with: `navkit://product/42` minus `navkit://` and `https://example.com/product/42` minus
  `https://example.com` both leave `product`, `42`. A feature declares its page once; the bases are the
  application's, registered in one place.
- **A domain the app does not own is refused.** `https://elsewhere.example/product/42` starts with no
  registered base, so it is `NotFound` before any handler sees it.
- **The bases have to agree with the platform.** The Android intent filter and the iOS URL type deliver
  `navkit://`; if nothing registered `navkit://`, every such link would be `NotFound`.
- **One owner per page.** The dispatcher is built from the handler set; two handlers claiming a page fail
  at construction instead of last-one-wins.
- **A link resolves to a stack, not a destination.** Back from the product goes to the playground, then
  the catalog — not out of the app.
- **The ingress only answers "queued".** `publish` returns `false` for a blank URI or a full buffer. What
  the handler decided arrives later, at the root.

**Try it:** open *One link, one route*, deliver `navkit://product/42`, go back; then *Try the web form
instead*, and *Try a domain the app does not own* — the readout says it was not found. Edit the link to
`navkit://product` — the readout says it was rejected, and why. From a terminal:
`adb shell am start -a android.intent.action.VIEW -d navkit://product/7` or
`xcrun simctl openurl booted navkit://product/7`.

## Real life: Campaign links

```kotlin
class OrdersDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("orders")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        if (request.source == DeepLinkSource.IN_APP_NOTIFICATION) {
            return DeepLinkOutcome.Rejected(reason = "In-app notifications do not deep link here")
        }
        val id = request.deepLink.pathSegments.getOrNull(1)
        val base = listOf(CatalogRoute, LinkCampaignRoute, OrdersRoute)
        if (id == null) {
            return DeepLinkOutcome.Navigate(routes = base)
        }
        return DeepLinkOutcome.Navigate(routes = base + OrderRoute(id = id))
    }
}

/** Does not check the session, and does not need to. */
class SecretDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("secret")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        return DeepLinkOutcome.Navigate(routes = listOf(CatalogRoute, MembersSecretRoute))
    }
}
```

The root resolves every link and records what became of it — whether or not it navigates:

```kotlin
LaunchedEffect(graph) {
    graph.deepLinkEvents.links.collect { incoming ->
        val outcome = graph.deepLinkDispatcher.dispatch(raw = incoming.uri, source = incoming.source)
        graph.deepLinkLog.record(link = incoming, outcome = outcome)
        if (outcome is DeepLinkOutcome.Navigate) {
            root.onDeepLink(outcome.routes)
        }
    }
}
```

What to notice:

- **A link lands deep with a sensible back path.** `navkit://orders/77` builds catalog → campaign → orders
  → order 77.
- **The source is part of the decision.** The same link from an in-app notification is rejected; a push
  notification or an external link is not. Not every entry point deserves the same trust.
- **A link to a guarded route is guarded without anyone asking.** `navkit://secret` resolves to the
  members-only page. The root applies the stack through its own setter, not through `Navigator` — and the
  host resolves whatever stack it is handed before rendering it, so signed out, the secret page is
  replaced by the sign-in screen before anything draws.
- **Resolving is the root's job, not a host's.** A host can be mounted anywhere; there is one link stream
  for the whole app. The root owns the stack a link replaces.
- **Outcomes are logged where they are known.** The ingress only knows the link was queued. Without
  [`DeepLinkLog`](../app/DeepLinkLog.kt), a rejected link read as "accepted" and then nothing happened.

**Try it:** open *Campaign links* and try each button: the list, three screens deep (then back through the
stack), the in-app notification (rejected — the readout says why), and the secret page while signed out
(sign-in appears; sign in and continue).

## Doing this in your app

1. Register a `DeepLinkBase` `@IntoSet` for every scheme and domain the app answers to — the same ones
   the platform declarations below name.
2. Handle each page in one `DeepLinkHandler` (or a `TypedDeepLinkHandler` over an enum of
   `DeepLinkPage`s), returning the **whole stack**, the app's root at the bottom. Contribute it
   `@IntoSet`.
3. At the root: collect `DeepLinkEvents.links`, `dispatch`, apply `Navigate(routes)` through the same
   setter the root host uses. Log `Rejected`/`NotFound` if anyone needs to see them.
4. Android: `singleTop` activity with a `VIEW`/`BROWSABLE` intent filter for your scheme; publish
   `intent` in `onCreate` when `savedInstanceState == null`, and in `onNewIntent`.
5. iOS: the scheme in `CFBundleURLTypes`; `.onOpenURL` calls a Kotlin function that publishes.
6. Build outbound links with `buildDeepLinkUri(base, page)` on a registered base.
7. Do not check access in handlers — guards run on the applied stack. Do validate what the link carries.

## Pitfalls

- **Returning only the destination** — back leaves the app.
- **Publishing the launch intent on every `onCreate`** — a rotation re-applies the link.
- **Resolving links inside a nested host** — there is one stream; resolve at the root.
- **Two handlers owning one page** — construction fails.
- **Expecting the ingress's `true` to mean the link opened** — it means queued.
- **A scheme or domain the platform delivers but nothing registers** — every such link is `NotFound`.
  Change the manifest, `Info.plist` and the registered bases together.
- **Handlers without any registered base** — the dispatcher fails when it is built.

## Read more

- [Deep links](../../../../../../../../../../navigation/api/README.md#deep-links) in the API guide — link bases, typed handlers, outbound links
- [Guards](../guards/README.md) — the `AuthGuard` the secret link meets
- [All examples](../../../../../../../../../README.md#the-examples)
