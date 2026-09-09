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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Applying the plugin must bring `cmp-firebase` with it, at the plugin's own version — that
 * is what makes one plugin line the whole integration.
 */
class DependencyWiringTest {

    private fun kmpProject(): Project = ProjectBuilder.builder().build().also {
        it.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        it.pluginManager.apply(CmpFirebasePlugin::class.java)
        it.extensions.getByType(KotlinMultiplatformExtension::class.java).jvm()
    }

    private fun Project.commonMainDeps() = extensions
        .getByType(KotlinMultiplatformExtension::class.java)
        .sourceSets.getByName("commonMain")
        .let { configurations.getByName(it.implementationConfigurationName).allDependencies }

    @Test
    fun plugin_adds_cmp_firebase_at_its_own_version() {
        val project = kmpProject()
        (project as ProjectInternal).evaluate()

        val dep = project.commonMainDeps().singleOrNull {
            it.group == "io.github.mobilebytelabs" && it.name == "cmp-firebase"
        }
        assertTrue(dep != null, "plugin must add cmp-firebase to commonMain")
        assertEquals(CMP_FIREBASE_VERSION, dep.version, "version must match the plugin's own")
    }

    @Test
    fun opt_out_suppresses_the_dependency() {
        val project = kmpProject()
        project.extensions.getByType(CmpFirebaseExtension::class.java).addDependency = false
        (project as ProjectInternal).evaluate()

        assertTrue(
            project.commonMainDeps().none { it.name == "cmp-firebase" },
            "cmpFirebase { addDependency = false } must suppress it",
        )
    }

    @Test
    fun hand_declared_dependency_is_not_duplicated_or_overridden() {
        val project = kmpProject()
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val cfg = kotlin.sourceSets.getByName("commonMain").implementationConfigurationName
        project.dependencies.add(cfg, "io.github.mobilebytelabs:cmp-firebase:1.2.3")
        (project as ProjectInternal).evaluate()

        val deps = project.commonMainDeps().filter { it.name == "cmp-firebase" }
        assertEquals(1, deps.size, "must not duplicate a hand-declared dependency")
        assertEquals("1.2.3", deps.single().version, "must not override a hand-pinned version")
    }
}
