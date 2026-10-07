package io.thernal.navkit.sample.shared.ui

import io.thernal.navkit.navigation.api.presentation.host.BottomSheetContainer

/** The application's sheet surface, installed once on the root host. */
val SampleSheetContainer: BottomSheetContainer = { dismiss, step ->
    SheetSurface(dismiss = dismiss, content = step)
}
