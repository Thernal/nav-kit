package io.thernal.navkit.sample.shared.guards

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason

data object SignInRequired : BlockReason {
    override val message: String = "Sign in to continue"
}
