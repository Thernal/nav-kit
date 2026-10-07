plugins {
    alias(libs.plugins.navkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            // Navigation3, coroutines, the immutable collections and ViewModel all appear in this
            // module's public signatures (`Route : NavKey`, `EntryProviderScope`, `StateFlow`,
            // `ImmutableList`), yet none is re-exported: `api(...)` is not used in this repository,
            // so a consumer declares each of them itself (navigation/api/README.md → Dependencies you declare).
            dependencies {
                implementation(libs.compose.animation)
                implementation(libs.navigation3.runtime)
                implementation(libs.navigation3.ui)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.lifecycle.viewmodel)
                // Only reached from inside `buildDeepLinkUri`; no type of it escapes.
                implementation(libs.ktor.http)
            }
        }
    }
}
