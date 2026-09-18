package io.thernal.navkit.sample.android

import android.app.Application
import io.thernal.navkit.sample.app.SampleGraph
import io.thernal.navkit.sample.app.createSampleGraph

/**
 * Owns the application graph for the life of the process.
 *
 * The graph used to be remembered by the composition, and an activity recreated for a rotation
 * starts a new composition. The session, the drafts, the argument store and the results mailbox were
 * all rebuilt, while the back stack — held by a ViewModel, which survives — still pointed at the
 * state they had held.
 */
class SampleApplication : Application() {
    val graph: SampleGraph by lazy { createSampleGraph() }
}
