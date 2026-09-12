plugins {
    alias(libs.plugins.navkit.compose)
    alias(libs.plugins.navkit.injection)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // api: the contracts an app injects are this module's whole point.
                api(projects.navigation.api)
                // implementation: which concrete class satisfies a contract is nobody else's business.
                implementation(projects.navigation.impl)
            }
        }
    }
}
