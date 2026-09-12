package io.thernal.navkit.navigation.api.domain

/** Where an inbound link came from, which handlers use to decide whether to honour it. */
enum class DeepLinkSource {
    EXTERNAL_LINK,
    PUSH_NOTIFICATION,
    IN_APP_NOTIFICATION,
}
