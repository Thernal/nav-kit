plugins {
    alias(libs.plugins.navkit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.navigation.api)
                implementation(projects.navigation.impl)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.navigation3.runtime)
            }
        }
    }
}
