plugins {
    alias(libs.plugins.navkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            // api, not implementation: Navigation3, coroutines and the immutable collections all
            // appear in this module's own public signatures (`Route : NavKey`, `EntryProviderScope`,
            // `StateFlow`, `ImmutableList`), so a consumer cannot compile against it without them.
            dependencies {
                api(libs.navigation3.runtime)
                api(libs.navigation3.ui)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.collections.immutable)
                // Only reached from inside `buildDeepLinkUri`; no type of it escapes.
                implementation(libs.ktor.http)
            }
        }
    }
}
