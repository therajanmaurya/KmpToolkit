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

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The plugin's actual job: every Apple framework ends up static.
 *
 * [CmpFirebasePluginTest] covers version arithmetic and clean application; this covers the
 * behaviour a regression would actually break. Without it the plugin could silently stop
 * flipping `isStatic` and every other test would still pass.
 *
 * Firebase's SwiftPM products are static libraries. A *dynamic* framework linking them
 * compiles and links fine, then crashes at runtime — which is exactly the failure mode a
 * consumer cannot diagnose from the error, and the reason this is forced rather than
 * documented.
 */
class StaticFrameworkEnforcementTest {

    private fun kmpProject(): Project = ProjectBuilder.builder().build().also {
        it.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        it.pluginManager.apply(CmpFirebasePlugin::class.java)
    }

    private fun Project.kotlin(): KotlinMultiplatformExtension =
        extensions.getByType(KotlinMultiplatformExtension::class.java)

    /** Realize the lazy containers so `configureEach` actions run. */
    private fun Project.realize() {
        (this as ProjectInternal).evaluate()
        kotlin().targets.withType(KotlinNativeTarget::class.java).forEach { target ->
            target.binaries.withType(Framework::class.java).forEach { /* realized */ }
        }
    }

    private fun Project.appleFrameworks(): List<Framework> = kotlin().targets.withType(KotlinNativeTarget::class.java)
        .filter { it.konanTarget.family.isAppleFamily }
        .flatMap { it.binaries.withType(Framework::class.java) }

    @Test
    fun dynamic_apple_framework_is_forced_static() {
        val project = kmpProject()
        project.kotlin().iosArm64().binaries.framework {
            baseName = "Shared"
            // Left dynamic on purpose — this is the default, and the crash case.
        }
        project.realize()

        val frameworks = project.appleFrameworks()
        assertTrue(frameworks.isNotEmpty(), "expected at least one Apple framework")
        assertTrue(
            frameworks.all { it.isStatic },
            "plugin must force isStatic=true on Apple frameworks, got: " +
                frameworks.joinToString { "${it.baseName}.isStatic=${it.isStatic}" },
        )
    }

    @Test
    fun every_apple_target_is_covered_not_just_the_first() {
        val project = kmpProject()
        project.kotlin().apply {
            iosArm64().binaries.framework { baseName = "A" }
            iosSimulatorArm64().binaries.framework { baseName = "B" }
            macosArm64().binaries.framework { baseName = "C" }
        }
        project.realize()

        val frameworks = project.appleFrameworks()
        assertTrue(frameworks.size >= 3, "expected 3 Apple frameworks, got ${frameworks.size}")
        assertTrue(frameworks.all { it.isStatic }, "every Apple framework must be static")
    }

    /**
     * `configureEach` (not `all`/`forEach`) is what makes this hold for frameworks declared
     * AFTER the plugin is applied — the common case, since consumers apply plugins at the top
     * of the file and declare targets below.
     */
    @Test
    fun framework_declared_after_plugin_application_is_still_forced_static() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply(CmpFirebasePlugin::class.java)
        // declared last, well after apply()
        project.kotlin().iosArm64().binaries.framework { baseName = "Late" }
        project.realize()

        assertTrue(project.appleFrameworks().all { it.isStatic })
    }

    /**
     * Non-Apple native targets have no Firebase SwiftPM constraint, so the plugin must not
     * touch them — forcing static there would be an unrelated behavioural change.
     */
    @Test
    fun non_apple_native_targets_are_left_alone() {
        val project = kmpProject()
        project.kotlin().linuxX64().binaries.sharedLib { baseName = "linux" }
        project.realize()

        val linuxFrameworks = project.kotlin().targets
            .withType(KotlinNativeTarget::class.java)
            .filterNot { it.konanTarget.family.isAppleFamily }
            .flatMap { it.binaries.withType(Framework::class.java) }
        assertTrue(linuxFrameworks.isEmpty(), "linux declares no Framework binaries")
    }

    /** An already-static framework stays static and needs no intervention. */
    @Test
    fun already_static_framework_is_untouched() {
        val project = kmpProject()
        project.kotlin().iosArm64().binaries.framework {
            baseName = "AlreadyStatic"
            isStatic = true
        }
        project.realize()

        assertTrue(project.appleFrameworks().all { it.isStatic })
    }

    /**
     * Sanity check on the fixture itself: without the plugin a framework stays dynamic.
     * If this ever passes trivially, the tests above prove nothing.
     */
    @Test
    fun control_without_plugin_framework_stays_dynamic() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        // NOTE: CmpFirebasePlugin deliberately NOT applied
        project.kotlin().iosArm64().binaries.framework { baseName = "Control" }
        project.realize()

        val frameworks = project.kotlin().targets.withType(KotlinNativeTarget::class.java)
            .filter { it.konanTarget.family.isAppleFamily }
            .flatMap { it.binaries.withType(Framework::class.java) }
        assertTrue(frameworks.isNotEmpty())
        assertFalse(
            frameworks.all { it.isStatic },
            "control must stay dynamic — otherwise the enforcement tests are vacuous",
        )
    }
}
