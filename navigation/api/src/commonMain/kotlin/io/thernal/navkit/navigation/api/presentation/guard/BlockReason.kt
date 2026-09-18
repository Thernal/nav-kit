package io.thernal.navkit.navigation.api.presentation.guard

/**
 * Why a guard refused a stack. The vocabulary is the application's; this module only ever renders
 * [message] into the event stream.
 */
interface BlockReason {
    val message: String
}
