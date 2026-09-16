plugins {
    alias(libs.plugins.navkit.compose)
    alias(libs.plugins.navkit.injection)
}

kotlin {
    // One static framework per iOS target, embedded by the Xcode project in `sample/iosApp`.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SampleShared"
            isStatic = true
            // Kotlin/Native cannot infer one from the source packages, and says so on every link.
            binaryOption("bundleId", "io.thernal.navkit.sample.shared")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // The sample is a consumer, so it names the three modules an application names:
                // the contracts, the implementation that backs them, and the bindings that install
                // both into a graph.
                api(projects.navigation.api)
                implementation(projects.navigation.impl)
                implementation(projects.navigation.wiring)
                implementation(libs.kotlinx.collections.immutable)
                // The kit draws nothing and names no design system; the sample needs one to be
                // worth running, and Material 3 is it.
                implementation(libs.compose.material3)
                // Every back stack in the sample is owned by a ViewModel, which is what the kit
                // expects of a caller: the host is controlled, so the state lives outside it.
                implementation(libs.lifecycle.viewmodel.compose)
            }
        }
    }
}
