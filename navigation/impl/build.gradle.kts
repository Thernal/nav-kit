plugins {
    alias(libs.plugins.navkit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.navigation.api)
                implementation(libs.navigation3.runtime)
                implementation(libs.navigation3.ui)
                implementation(libs.lifecycle.viewmodel.navigation3)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.navigationevent.compose)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.ktor.http)
            }
        }
    }
}
