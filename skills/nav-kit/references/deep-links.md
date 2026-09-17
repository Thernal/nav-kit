# Deep links

## Contents

1. The pipeline
2. Link bases and how a link is parsed
3. Recipe: a handler
4. Recipe: several pages in one handler
5. Resolving at the root
6. Platform wiring
7. Outbound links
8. Testing

Packages: `io.thernal.navkit.navigation.api.presentation.deeplink` (`DeepLinkHandler`,
`TypedDeepLinkHandler`, `DeepLinkOutcome`, `DeepLinkDispatcher`, `DeepLinkIngress`, `DeepLinkEvents`) and
`io.thernal.navkit.navigation.api.domain` (`DeepLink`, `DeepLinkBase`, `DeepLinkRequest`, `DeepLinkSource`,
`DeepLinkPage`, `pageOf`, `buildDeepLinkUri`, `buildUri`).

## 1. The pipeline

```
platform → DeepLinkIngress.publish(uri, source) → DeepLinkEvents.links (buffered)
  → root collects → DeepLinkDispatcher.dispatch(raw, source) → strip the registered base → handler owning the page → DeepLinkOutcome
  → root applies Navigate(routes) to its own stack → the host guards the stack → rendered
```

- The platform only publishes. The root resolves — once, for the whole app. Hosts never resolve links.
- `publish` returns `false` only for a blank URI or a full buffer; `true` means queued, not opened.
- Links published before the root collects (cold start) are buffered and still arrive.

## 2. Link bases and how a link is parsed

The application registers a `DeepLinkBase` for every scheme and web origin its links start with — the same
ones its intent filters, `CFBundleURLTypes` and verified domains declare:

```kotlin
@Provides
@IntoSet
fun provideAppSchemeBase(): DeepLinkBase {
    return DeepLinkBase("myapp://")
}

@Provides
@IntoSet
fun provideWebOriginBase(): DeepLinkBase {
    return DeepLinkBase("https://example.com")        // a path is allowed: "https://example.com/app"
}
```

The parser removes the **most specific** registered base a link starts with; what follows is the page.

| Bases | Raw | `pathSegments` | `page` |
|---|---|---|---|
| `myapp://` | `myapp://orders/77` | `[orders, 77]` | `orders` |
| `https://example.com` | `https://example.com/orders/77` | `[orders, 77]` | `orders` |
| `https://example.com`, `https://example.com/app` | `https://example.com/app/orders` | `[orders]` | `orders` |
| `myapp://` | `myapp://search?q=bar%20table&tag=a&tag=b` | `[search]`; `query("q")` = `bar table`, `query["tag"]` = `[a, b]` | `search` |
| `https://example.com` | `https://elsewhere.example/orders/77` | — | `NotFound` |
| `https://example.com` | `https://example.com` | — | `NotFound` |

- On an app-scheme base the host is the first page. Scheme and host compare case-insensitively; a base's
  path compares whole segments.
- A link that matches no base is `NotFound` — an unregistered domain never reaches a handler.
- Handlers registered with no base fail when the dispatcher is built.
- `DeepLinkBase(...)` fails for a query or fragment, a web base without a host, or a hostless base with a path.
- `request.deepLink.base` says which base matched (app scheme vs web), for trust decisions.
- Segments and query values arrive decoded.

## 3. Recipe: a handler

```kotlin
class OrdersDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("orders")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        if (request.source == DeepLinkSource.IN_APP_NOTIFICATION) {
            return DeepLinkOutcome.Rejected(reason = "In-app notifications do not open orders")
        }
        val id = request.deepLink.pathSegments.getOrNull(1)
        val base = listOf(HomeRoute, OrdersRoute)          // the app's root at the bottom
        if (id == null) {
            return DeepLinkOutcome.Navigate(routes = base)
        }
        return DeepLinkOutcome.Navigate(routes = base + OrderRoute(id = id))
    }
}

@Provides
@IntoSet
fun provideOrdersHandler(): DeepLinkHandler {
    return OrdersDeepLinkHandler()
}
```

- Return the **whole stack**, with the app's root at the bottom — back must not leave the app.
- **Do not check access.** The root applies the stack through its setter and the host runs guards on it; a
  link to a protected route becomes the sign-in screen by itself.
- Use `request.source` (`EXTERNAL_LINK`, `PUSH_NOTIFICATION`, `IN_APP_NOTIFICATION`) to decide trust.
- Validate what the link carries — it comes from outside the app.
- One handler per page. Two handlers claiming a page fail when the dispatcher is constructed
  (`Deep link page 'orders' is claimed by A and B`); a blank page fails too.
- Outcomes: `Navigate(routes)`, `Rejected(reason)` (our page, unacceptable request), `NotFound`.
- Every route in the returned stack needs an entry in the root host.

## 4. Recipe: several pages in one handler

```kotlin
enum class ProfilePage(override val page: String) : DeepLinkPage {
    View("profile"),
    Edit("profile-edit"),
}

class ProfileDeepLinkHandler : TypedDeepLinkHandler<ProfilePage>(ProfilePage.entries) {
    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        val name = request.deepLink.page ?: return DeepLinkOutcome.NotFound
        val page = pageOf<ProfilePage>(name) ?: return DeepLinkOutcome.NotFound
        val id = request.deepLink.query("id") ?: return DeepLinkOutcome.Rejected("missing id")
        return when (page) {
            ProfilePage.View -> DeepLinkOutcome.Navigate(listOf(HomeRoute, ProfileRoute.Overview(id)))
            ProfilePage.Edit -> DeepLinkOutcome.Navigate(listOf(HomeRoute, ProfileRoute.Edit(id)))
        }
    }
}
```

`TypedDeepLinkHandler` derives `pages` from the enum; `pageOf<T>(name)` maps back.

## 5. Resolving at the root

```kotlin
LaunchedEffect(graph) {
    graph.deepLinkEvents.links.collect { incoming ->
        val outcome = graph.deepLinkDispatcher.dispatch(raw = incoming.uri, source = incoming.source)
        // record Rejected / NotFound here if a screen should show what happened
        if (outcome is DeepLinkOutcome.Navigate) {
            rootViewModel.onDeepLink(outcome.routes)     // replaces the root stack
        }
    }
}
```

`onDeepLink` sets the root stack directly (ignoring an empty list). It does not go through `Navigator`, emits
no `NavigationEvent`, and abandons any guard deferral that was waiting in the root host.

## 6. Platform wiring

Android (`MainActivity`, `singleTop`):

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) {
        graph.deepLinkIngress.publish(intent)     // not on recreation, or the link is applied twice
    }
    setContent { App(graph) }
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    graph.deepLinkIngress.publish(intent)
}
```

Manifest: `android:launchMode="singleTop"`, and an intent filter with `VIEW`, `DEFAULT`, `BROWSABLE` and
`<data android:scheme="myapp" />` — with `DeepLinkBase("myapp://")` registered. Verified `https` App Links additionally need `android:autoVerify="true"`,
host data elements and a hosted `assetlinks.json`.
`publish(intent)` is the `androidMain` extension `io.thernal.navkit.navigation.api.presentation.deeplink.publish`;
a notification tap passes `source = DeepLinkSource.PUSH_NOTIFICATION`.

iOS: `CFBundleURLTypes` with the scheme in `Info.plist` (registered as a base too); SwiftUI
`.onOpenURL { url in _ = MainViewControllerKt.handleDeepLink(url: url.absoluteString) }`, where the Kotlin
function calls `graph.deepLinkIngress.publish(uri = url, source = DeepLinkSource.EXTERNAL_LINK)`.

Manual checks: `adb shell am start -a android.intent.action.VIEW -d myapp://orders/77`,
`xcrun simctl openurl booted myapp://orders/77` — cold, warm, and after a rotation.

## 7. Outbound links

```kotlin
val appScheme = DeepLinkBase("myapp://")
val web = DeepLinkBase("https://example.com")

buildDeepLinkUri(base = appScheme, page = "orders", query = mapOf("tab" to "open"))
// myapp://orders?tab=open
ProfilePage.View.buildUri(base = web, query = mapOf("id" to "42"))
// https://example.com/profile?id=42
```

Building appends the page to a base; parsing removes it — a link built on a registered base is read back as
the same page. Share the registered `DeepLinkBase` values instead of concatenating link strings.

## 8. Testing

```kotlin
@Test
fun orderLinkOpensTheOrderAboveTheList() = runTest {
    val dispatcher = DeepLinkDispatcherImpl(
        handlers = setOf(OrdersDeepLinkHandler()),
        bases = setOf(DeepLinkBase("myapp://"), DeepLinkBase("https://example.com")),
    )

    val outcome = dispatcher.dispatch(raw = "myapp://orders/77", source = DeepLinkSource.EXTERNAL_LINK)

    assertEquals(DeepLinkOutcome.Navigate(listOf(HomeRoute, OrdersRoute, OrderRoute("77"))), outcome)
}
```

Also test the web form, an unregistered domain (`NotFound`), a rejected source, and a missing id.
`parseDeepLink(raw, bases)` (impl, `domain.deeplink`) tests parsing alone; round-trip
`buildDeepLinkUri(base, page)` through it.
