package io.thernal.navkit.navigation.api.presentation.guard

/**
 * Why a guard refused a stack. The vocabulary belongs to the application — a navigation layer cannot
 * know whether a refusal reads as a sign-in prompt, a paywall or a permission error — so this module
 * only ever renders [message] into the event stream.
 */
interface BlockReason {
    val message: String
}
