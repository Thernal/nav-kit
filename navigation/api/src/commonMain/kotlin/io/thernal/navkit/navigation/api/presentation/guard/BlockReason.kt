package io.thernal.navkit.navigation.api.presentation.guard

/**
 * Why a guard refused a stack.
 *
 * The vocabulary belongs to the application, not to this module: a navigation layer cannot know
 * whether a refusal should read as a sign-in prompt, a paywall or a permission error, so it
 * carries the app's own type and only ever renders [message] into the event stream.
 */
interface BlockReason {
    val message: String
}
