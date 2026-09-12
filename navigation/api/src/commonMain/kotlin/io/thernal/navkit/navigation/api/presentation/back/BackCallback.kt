package io.thernal.navkit.navigation.api.presentation.back

fun interface BackCallback {
    /** Returns true when this callback consumed the back action. */
    fun handle(): Boolean
}
