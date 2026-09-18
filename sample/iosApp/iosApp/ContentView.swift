import SwiftUI
import SampleShared

/// The whole iOS app. Everything on screen is the shared Compose composition — the same one the
/// Android activity hosts — so there is no navigation, no routing and no state here to drift apart
/// from it.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(edges: .all)
    }
}
