package io.thernal.navkit.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Set by whoever wants a stability report; off for every ordinary build. */
private const val STABILITY_REPORT_PROPERTY = "composeStabilityReport"

/**
 * Compose Multiplatform on top of [KmpLibraryConventionPlugin]. Carries only the Compose artifacts
 * every UI-bearing module in this repository needs; a module adds anything beyond that itself.
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.thernal.navkit.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val catalog = libs

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.named("commonMain") {
                // implementation, not api: nothing is re-exported (D24). A module whose surface has
                // Compose types still compiles; its consumer applies Compose itself, as every
                // module using those types does.
                dependencies {
                    implementation(catalog.library("compose-runtime"))
                    implementation(catalog.library("compose-foundation"))
                    implementation(catalog.library("compose-ui"))
                    implementation(catalog.library("compose-animation"))
                }
            }
        }

        if (providers.gradleProperty(STABILITY_REPORT_PROPERTY).orNull == "true") {
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
                reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
            }
        }
    }
}
