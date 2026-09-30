plugins {
    alias(libs.plugins.navkit.compose)
    alias(libs.plugins.navkit.injection)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // The contracts are not re-exported: an app that injects them depends on `api` itself.
                implementation(projects.navigation.api)
                implementation(projects.navigation.impl)
            }
        }
    }
}
