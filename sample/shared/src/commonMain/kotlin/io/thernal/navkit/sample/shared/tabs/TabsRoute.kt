package io.thernal.navkit.sample.shared.tabs

import io.thernal.navkit.sample.shared.app.SampleRoute

sealed interface TabsRoute : SampleRoute

/** The outer route. Everything below lives inside the host this one screen mounts. */
data object SingleHostTabsRoute : TabsRoute
