package io.thernal.navkit.sample.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.thernal.navkit.navigation.api.presentation.deeplink.publish
import io.thernal.navkit.sample.app.SampleApp
import io.thernal.navkit.sample.app.SampleGraph

/**
 * One activity, one composition. The sample has no Android-specific navigation of its own on
 * purpose: everything a reader is here to look at lives in the shared module and runs unchanged on
 * iOS.
 *
 * The one platform duty it has is handing links over. A `navkit://` link starts or reaches this
 * activity as an intent, and the ingress is all it is given — resolving the link is the root's job,
 * in the shared composition.
 */
class MainActivity : ComponentActivity() {
    private val graph: SampleGraph
        get() {
            return (application as SampleApplication).graph
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity still carries the intent it was started with; publishing it again
        // would apply the same link a second time.
        if (savedInstanceState == null) {
            graph.deepLinkIngress.publish(intent)
        }
        setContent {
            SampleApp(graph)
        }
    }

    // `singleTop` in the manifest routes a link that arrives while the app is open here, rather than
    // to a second activity with a second composition.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        graph.deepLinkIngress.publish(intent)
    }
}
