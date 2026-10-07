package io.thernal.navkit.sample.shared.guards

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason

data object PinRequired : BlockReason {
    override val message: String = "The vault stays locked"
}
