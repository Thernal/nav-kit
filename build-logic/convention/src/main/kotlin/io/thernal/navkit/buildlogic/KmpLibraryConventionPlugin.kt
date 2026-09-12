package io.thernal.navkit.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * The single baseline every module in this repository shares: the Kotlin Multiplatform plugin, the
 * target set, and the test dependencies. Anything Compose- or injection-specific is a separate
 * convention so a module names only the capabilities it actually uses.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        QualityConventionPlugin().apply(target)

        val catalog = libs
        val namespace = defaultNamespace()

        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(catalog.version("jvm").toInt())

            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }

            // iosArm64 + iosSimulatorArm64 only: those are the two targets every Navigation3
            // artifact in the catalog publishes. iosX64 has no navigation3-ui variant.
            androidTarget(namespace, catalog)
            iosArm64()
            iosSimulatorArm64()

            applyDefaultHierarchyTemplate()

            sourceSets.named("commonMain") {
                dependencies {
                    implementation(catalog.library("kotlin-stdlib"))
                }
            }

            sourceSets.named("commonTest") {
                dependencies {
                    implementation(catalog.library("kotlin-test"))
                    implementation(catalog.library("kotlinx-coroutines-test"))
                }
            }
        }
    }
}

private fun KotlinMultiplatformExtension.androidTarget(
    namespace: String,
    catalog: org.gradle.api.artifacts.VersionCatalog,
) {
    targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
        this.namespace = namespace
        this.compileSdk = catalog.version("android-compile-sdk").toInt()
        this.minSdk = catalog.version("android-min-sdk").toInt()

        // Gives commonTest somewhere to run: the host (JVM) test compilation of the Android target
        // is the only non-native test target in this build.
        withHostTest {}
    }
}
