/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/** Configuration for the `cmp-firebase` Gradle plugin. */
public open class CmpFirebaseExtension {
    /**
     * Whether the plugin adds the `cmp-firebase` dependency to `commonMain` at its own
     * version. Set `false` when the dependency is declared by hand or supplied by a
     * platform/BOM.
     */
    public var addDependency: Boolean = true
}

/**
 * Applies everything a `cmp-firebase` consumer needs, so one plugin line replaces a
 * dependency declaration plus build setup that is easy to get silently wrong.
 *
 * A published Maven artifact's `build.gradle.kts` runs only when building THAT artifact —
 * never in the consumer's build — so `cmp-firebase` cannot check how a consumer configures
 * their Apple frameworks. A Gradle plugin does run there, which is the whole reason this
 * module exists.
 *
 * What it does:
 *
 * 1. **Adds `cmp-firebase`** to `commonMain` at [CMP_FIREBASE_VERSION] — this plugin's own
 *    version, so the library and the contract enforced for it can never drift apart. Skipped
 *    when the project already declares it, so a hand-pinned version is never overridden.
 *    Opt out entirely with `cmpFirebase { addDependency = false }`.
 * 2. **Forces `isStatic = true` on every Apple framework.** Firebase's SwiftPM products are
 *    static libraries; embedding them in a *dynamic* framework compiles and links, then
 *    crashes at runtime. This is the one requirement `cmp-firebase` genuinely imposes on a
 *    consumer's build that a plain KMP app would not have.
 * 3. **Requires Kotlin >= [MIN_KOTLIN_VERSION].** The transitive SwiftPM resolution that
 *    pulls `firebase-ios-sdk` across the Maven boundary is a Kotlin 2.4 feature. Below it no
 *    SwiftPM package is generated and the build fails at link time with
 *    `ld: framework 'FirebaseCore' not found`, which names nothing useful.
 *
 * The plugin is published independently to Maven Central and the Gradle Plugin Portal, so it
 * is usable on its own; in practice it is how `cmp-firebase` is consumed.
 */
public class CmpFirebasePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions.create("cmpFirebase", CmpFirebaseExtension::class.java)

        // Everything hangs off the Kotlin plugin: the version accessor needs it applied, and
        // there are no Apple frameworks or source sets without it. withId fires whether this
        // plugin is applied before or after Kotlin, so consumer ordering does not matter.
        target.plugins.withId(KOTLIN_MULTIPLATFORM_ID) {
            verifyKotlinVersion(target)
            forceStaticAppleFrameworks(target)
            // afterEvaluate so the consumer's `cmpFirebase { }` block has been read first.
            target.afterEvaluate {
                if (extension.addDependency) addCmpFirebaseDependency(target)
            }
        }
    }

    /**
     * Fails configuration when the consumer's Kotlin is older than [MIN_KOTLIN_VERSION].
     *
     * Deliberately a hard failure, not a warning: on an older Kotlin the SwiftPM machinery
     * does not exist at all, so the Apple build cannot succeed — it just fails later with a
     * linker error that points at the wrong thing.
     */
    private fun verifyKotlinVersion(project: Project) {
        val actual = runCatching { project.getKotlinPluginVersion() }.getOrNull() ?: return
        if (compareVersions(actual, MIN_KOTLIN_VERSION) < 0) {
            error(
                """
                |cmp-firebase requires Kotlin $MIN_KOTLIN_VERSION or newer — this build uses $actual.
                |
                |GitLive Firebase 3.x links the native firebase-ios-sdk via SwiftPM, and the
                |transitive resolution that carries it across the Maven boundary is a Kotlin 2.4
                |feature. On $actual no SwiftPM package is generated, nothing resolves the native
                |SDK, and the Apple build fails at link time with:
                |
                |    ld: framework 'FirebaseCore' not found
                |
                |Fix: upgrade Kotlin to $MIN_KOTLIN_VERSION+, or provision the Firebase Apple
                |frameworks yourself (e.g. CocoaPods), which cmp-firebase does not support.
                """.trimMargin(),
            )
        }
    }

    /**
     * Forces `isStatic = true` on every Apple framework the consumer declares, including
     * frameworks registered after this plugin is applied (hence `configureEach`).
     */
    private fun forceStaticAppleFrameworks(project: Project) {
        val logger = project.logger
        val kotlin = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach {
            if (!konanTarget.family.isAppleFamily) return@configureEach
            val targetName = name
            binaries.withType(Framework::class.java).configureEach {
                if (!isStatic) {
                    logger.lifecycle(
                        "cmp-firebase: forcing isStatic=true on $targetName framework " +
                            "'$baseName' — Firebase's SwiftPM products are static libraries " +
                            "and a dynamic framework crashes at runtime.",
                    )
                    isStatic = true
                }
            }
        }
    }

    /**
     * Adds `cmp-firebase` to `commonMain` at this plugin's own version, so the library and
     * the build contract enforced for it always match.
     */
    private fun addCmpFirebaseDependency(project: Project) {
        val kotlin = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        val commonMain = kotlin.sourceSets.findByName("commonMain") ?: return
        val configurationName = commonMain.implementationConfigurationName

        val alreadyDeclared = project.configurations.findByName(configurationName)
            ?.allDependencies
            ?.any { it.group == CMP_FIREBASE_GROUP && it.name == CMP_FIREBASE_ARTIFACT }
            ?: false

        if (alreadyDeclared) {
            project.logger.info(
                "cmp-firebase: dependency already declared by the project — plugin will not add it.",
            )
            return
        }

        project.dependencies.add(
            configurationName,
            "$CMP_FIREBASE_GROUP:$CMP_FIREBASE_ARTIFACT:$CMP_FIREBASE_VERSION",
        )
        project.logger.info(
            "cmp-firebase: added $CMP_FIREBASE_ARTIFACT:$CMP_FIREBASE_VERSION to commonMain.",
        )
    }

    internal companion object {
        const val MIN_KOTLIN_VERSION: String = "2.4.20"
        const val KOTLIN_MULTIPLATFORM_ID: String = "org.jetbrains.kotlin.multiplatform"
        const val CMP_FIREBASE_GROUP: String = "io.github.mobilebytelabs"
        const val CMP_FIREBASE_ARTIFACT: String = "cmp-firebase"

        /**
         * Compares dotted version strings numerically, ignoring any pre-release suffix
         * (`2.4.20-RC2` compares equal to `2.4.20`). Returns <0, 0 or >0.
         */
        internal fun compareVersions(lhs: String, rhs: String): Int {
            val l = lhs.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val r = rhs.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(l.size, r.size)) {
                val cmp = (l.getOrElse(i) { 0 }).compareTo(r.getOrElse(i) { 0 })
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}
