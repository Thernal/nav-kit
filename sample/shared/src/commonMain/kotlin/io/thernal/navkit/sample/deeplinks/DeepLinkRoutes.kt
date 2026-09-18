package io.thernal.navkit.sample.deeplinks

import io.thernal.navkit.sample.app.SampleRoute

sealed interface DeepLinksRoute : SampleRoute

data object LinkPlaygroundRoute : DeepLinksRoute

data class ProductRoute(val id: String) : DeepLinksRoute

data object LinkCampaignRoute : DeepLinksRoute

data object OrdersRoute : DeepLinksRoute

data class OrderRoute(val id: String) : DeepLinksRoute
