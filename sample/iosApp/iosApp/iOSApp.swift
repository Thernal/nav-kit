import SwiftUI
import SampleShared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // A `navkit://` link opening or reaching the app goes to the shared ingress, the
                // same one the Android activity publishes an intent's data to.
                .onOpenURL { url in
                    _ = MainViewControllerKt.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}
