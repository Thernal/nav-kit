package io.thernal.navkit.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure

/**
 * The only application module shape this repository has: the sample that demonstrates the
 * navigation kit. Everything the libraries share lives in [KmpLibraryConventionPlugin]; this one
 * exists because an Android application is not a Kotlin Multiplatform library and cannot use it.
 *
 * It still applies [QualityConventionPlugin], so the sample is held to the same Detekt rules as the
 * kit — an example that would not pass review is not an example.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // No `org.jetbrains.kotlin.android`: since AGP 9 the Android plugin carries Kotlin
        // support itself, and applying the standalone plugin on top of it is an error.
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        QualityConventionPlugin().apply(target)

        val catalog = libs
        val jvm = catalog.version("jvm").toInt()

        extensions.configure<ApplicationExtension> {
            namespace = defaultNamespace()
            compileSdk = catalog.version("android-compile-sdk").toInt()

            defaultConfig {
                applicationId = defaultNamespace()
                minSdk = catalog.version("android-min-sdk").toInt()
                targetSdk = catalog.version("android-compile-sdk").toInt()
                versionCode = 1
                versionName = "1.0"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(jvm)
                targetCompatibility = JavaVersion.toVersion(jvm)
            }

            buildFeatures {
                compose = true
            }
        }

        extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(jvm))
        }
    }
}
