package io.thernal.navkit.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * One strict, reproducible Detekt setup for every module. Applied by
 * [KmpLibraryConventionPlugin] rather than by modules, so it carries no plugin id of its own —
 * static analysis is not a capability a module opts into.
 *
 * Findings fail the build. The repository starts with none, and a warning nobody is forced to
 * clear is a rule that decays; a genuinely wrong finding is silenced in `config/detekt/detekt.yml`
 * or with `@Suppress`, where the decision is visible in review.
 */
internal class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)
                allRules.set(false)
                parallel.set(true)
                config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            }

            dependencies {
                add("detektPlugins", libs.library("detekt-ktlint-wrapper"))
                // Substituted for build-logic's `:detekt-rules` project, which is an included build.
                add("detektPlugins", "io.thernal.navkit.buildlogic:detekt-rules")
            }

            tasks.withType<Detekt>().configureEach {
                exclude("**/build/**", "**/generated/**")
                reports {
                    html.required.set(true)
                    checkstyle.required.set(true)
                    sarif.required.set(false)
                    markdown.required.set(false)
                }
            }

            // Detekt registers two families of task for a multiplatform module: one per source set
            // (`detektCommonMainSourceSet`), and one per compilation (`detektMainAndroid`). Only the
            // compilation tasks run with type resolution, which `UnsafeCollectionIndexAccess` needs to
            // tell a List apart from anything else — so `check` runs those, and they already cover
            // every source, since each common source set is compiled into some compilation.
            val typeResolvedAnalysis = tasks.withType(Detekt::class.java).matching { task ->
                task.name != "detekt" && !task.name.endsWith("SourceSet")
            }
            tasks.named("check") { dependsOn(typeResolvedAnalysis) }
        }
    }
}
