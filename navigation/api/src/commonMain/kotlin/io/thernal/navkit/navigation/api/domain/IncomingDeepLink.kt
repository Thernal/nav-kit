package io.thernal.navkit.navigation.api.domain

data class IncomingDeepLink(
    val uri: String,
    val source: DeepLinkSource,
)
