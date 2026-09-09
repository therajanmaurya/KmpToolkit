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

import io.github.mobilebytelabs.kmptoolkit.firebase.gradle.CmpFirebasePlugin.Companion.MIN_KOTLIN_VERSION
import io.github.mobilebytelabs.kmptoolkit.firebase.gradle.CmpFirebasePlugin.Companion.compareVersions
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CmpFirebasePluginTest {

    // ── Version comparison ──────────────────────────────────────────────────
    //
    // The Kotlin-floor gate is only as good as this comparison. A naive string
    // compare would rank "2.4.9" above "2.4.20" and let a too-old Kotlin through
    // (or reject a valid one), so the ordering is pinned explicitly.

    @Test
    fun patch_versions_compare_numerically_not_lexically() {
        // "2.4.9" > "2.4.20" under string ordering — the exact trap this must avoid.
        assertTrue(compareVersions("2.4.9", "2.4.20") < 0)
        assertTrue(compareVersions("2.4.20", "2.4.9") > 0)
    }

    @Test
    fun equal_versions_compare_equal() {
        assertEquals(0, compareVersions("2.4.20", "2.4.20"))
    }

    @Test
    fun prerelease_suffix_is_ignored() {
        // 2.4.20-RC2 carries the feature; it must not be rejected as "below 2.4.20".
        assertEquals(0, compareVersions("2.4.20-RC2", "2.4.20"))
        assertEquals(0, compareVersions("2.4.20-Beta1", "2.4.20"))
    }

    @Test
    fun shorter_versions_pad_with_zero() {
        assertTrue(compareVersions("2.4", "2.4.20") < 0)
        assertEquals(0, compareVersions("2.4.0", "2.4"))
    }

    @Test
    fun major_and_minor_dominate_patch() {
        assertTrue(compareVersions("2.5.0", "2.4.99") > 0)
        assertTrue(compareVersions("3.0.0", "2.4.20") > 0)
        assertTrue(compareVersions("2.3.99", MIN_KOTLIN_VERSION) < 0)
    }

    @Test
    fun declared_floor_is_the_version_the_library_is_built_against() {
        // Guards against the floor drifting away from the catalog's kotlin version
        // without a deliberate decision.
        assertEquals("2.4.20", MIN_KOTLIN_VERSION)
    }

    // ── Plugin application ──────────────────────────────────────────────────

    @Test
    fun applies_cleanly_to_a_project_without_kotlin_multiplatform() {
        // A consumer may apply this to a non-KMP module by mistake, or apply it before
        // the Kotlin plugin. Neither is an error — the framework configuration is
        // registered lazily via plugins.withId and simply never fires.
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CmpFirebasePlugin::class.java)
        assertNotNull(project.plugins.findPlugin(CmpFirebasePlugin::class.java))
    }

    @Test
    fun is_idempotent_when_applied_twice() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CmpFirebasePlugin::class.java)
        project.pluginManager.apply(CmpFirebasePlugin::class.java)
        assertNotNull(project.plugins.findPlugin(CmpFirebasePlugin::class.java))
    }
}
