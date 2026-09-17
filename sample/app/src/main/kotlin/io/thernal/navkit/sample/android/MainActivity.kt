package io.thernal.navkit.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.thernal.navkit.sample.app.SampleApp

/**
 * One activity, one composition. The sample has no Android-specific navigation of its own on
 * purpose: everything a reader is here to look at lives in the shared module and runs unchanged on
 * iOS.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as SampleApplication).graph
        setContent {
            SampleApp(graph)
        }
    }
}
