# Deep links

## Contents

1. The pipeline
2. How a link is parsed
3. Recipe: a handler
4. Recipe: several pages in one handler
5. Resolving at the root
6. Platform wiring
7. Outbound links
8. Testing

Packages: `io.thernal.navkit.navigation.api.presentation.deeplink` (`DeepLinkHandler`,
`TypedDeepLinkHandler`, `DeepLinkOutcome`, `DeepLinkDispatcher`, `DeepLinkIngress`, `DeepLinkEvents`) and
`io.thernal.navkit.navigation.api.domain` (`DeepLink`, `DeepLinkRequest`, `DeepLinkSource`, `DeepLinkPage`,
`pageOf`, `buildDeepLinkUri`, `buildUri`).

## 1. The pipeline

```
platform → DeepLinkIngress.publish(uri, source) → DeepLinkEvents.links (buffered)
  → root collects → DeepLinkDispatcher.dispatch(raw, source) → handler owning the page → DeepLinkOutcome
  → root applies Navigate(routes) to its own stack → the host guards the stack → rendered
```

- The platform only publishes. The root resolves — once, for the whole app. Hosts never resolve links.
- `publish` returns `false` only for a blank URI or a full buffer; `true` means queued, not opened.
- Links published before the root collects (cold start) are buffered and still arrive.

## 2. How a link is parsed

Custom scheme: the host is the first page. `http`/`https`: the host is a domain; only the path counts.

| Raw | `pathSegments` | `page` |
|---|---|---|
| `myapp://orders/77` | `[orders, 77]` | `orders` |
| `https://example.com/orders/77` | `[orders, 77]` | `orders` |
| `myapp://search?q=bar%20table&tag=a&tag=b` | `[search]`; `query("q")` = `bar table`, `query["tag"]` = `[a, b]` | `search` |
| `https://example.com` | none → `NotFound` | — |

Segments and query values arrive decoded. A handler therefore serves the app-scheme and web forms of a link
without configuration.

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
`<data android:scheme="myapp" />`. Verified `https` App Links additionally need `android:autoVerify="true"`,
host data elements and a hosted `assetlinks.json`.
`publish(intent)` is the `androidMain` extension `io.thernal.navkit.navigation.api.presentation.deeplink.publish`;
a notification tap passes `source = DeepLinkSource.PUSH_NOTIFICATION`.

iOS: `CFBundleURLTypes` with the scheme in `Info.plist`; SwiftUI
`.onOpenURL { url in _ = MainViewControllerKt.handleDeepLink(url: url.absoluteString) }`, where the Kotlin
function calls `graph.deepLinkIngress.publish(uri = url, source = DeepLinkSource.EXTERNAL_LINK)`.

Manual checks: `adb shell am start -a android.intent.action.VIEW -d myapp://orders/77`,
`xcrun simctl openurl booted myapp://orders/77` — cold, warm, and after a rotation.

## 7. Outbound links

```kotlin
buildDeepLinkUri(baseUrl = "https://example.com", page = "orders", query = mapOf("tab" to "open"))
// https://example.com/orders?tab=open
ProfilePage.View.buildUri(baseUrl = "https://example.com", query = mapOf("id" to "42"))
// https://example.com/profile?id=42
```

Use an `http(s)` base. With a custom-scheme base (`"myapp://"`) the builder inserts `localhost` as the host
(`myapp://localhost/profile`) and the parser reads `localhost` as the page. Write a custom-scheme link as a
literal string (`"myapp://profile?id=42"`) instead.

## 8. Testing

```kotlin
@Test
fun orderLinkOpensTheOrderAboveTheList() = runTest {
    val dispatcher = DeepLinkDispatcherImpl(setOf(OrdersDeepLinkHandler()))

    val outcome = dispatcher.dispatch(raw = "myapp://orders/77", source = DeepLinkSource.EXTERNAL_LINK)

    assertEquals(DeepLinkOutcome.Navigate(listOf(HomeRoute, OrdersRoute, OrderRoute("77"))), outcome)
}
```

Also test the web form, a rejected source, and a missing id. `parseDeepLink(raw)` (impl, `domain.deeplink`)
tests parsing alone.
