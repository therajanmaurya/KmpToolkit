/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.binaryCompatibilityValidator)
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
    id("io.github.mobilebytelabs.kmptoolkit.kover")
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-intent-launcher-compose
// ============================================================================
// Compose Multiplatform extensions for cmp-intent-launcher core.
// Holds @Composable rememberIntentLauncher() (extracted from cmp-intent-launcher
// during inter-app-comms-real-native-impls Phase 1 — BREAKING split).
//
// 9 Compose-MP-supported targets (Android, iOS×3, macOS×2, JVM, JS, wasmJs).
// Compose Compiler module-level constraint blocks tvOS/watchOS/Linux/mingw —
// consumers needing those targets use the core module's non-Compose APIs
// (ComponentActivity.intentLauncher() Android extension, or direct ctor).
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    android {
        namespace = "com.mobilebytelabs.kmptoolkit.intentlauncher.compose"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        withJava()

        withHostTestBuilder {}.configure {
            // android.jar in a JVM host test is a stub whose methods THROW by default, so a
            // framework call aborts a test even when the code under test handled it correctly.
            isReturnDefaultValues = true

            // Robolectric reads the MERGED manifest/resources; without this the ui-test-manifest
            // activity Compose's ActivityScenario launches is invisible and every UI test dies
            // with "Unable to resolve activity for Intent { MAIN }".
            isIncludeAndroidResources = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    macosArm64()

    js {
        browser()
    }

    wasmJs {
        browser()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":cmp-intent-launcher"))
            implementation(compose.runtime)
            // v0.4 Phase 8 — opinionated UX Composables (IntentPickerDialog + IntentPickerSheet)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            // Real composition testing — the CompositionLocal defaults and overrides asserted
            // here are unobservable from a direct function call.
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmTest.dependencies {
            // Skiko's renderer, required for runComposeUiTest on JVM/desktop.
            implementation(compose.desktop.currentOs)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        // getByName: the com.android.kotlin.multiplatform.library plugin does not generate a
        // typed `androidHostTest` accessor the way it does for commonTest/jvmTest.
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.junit)
            implementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING CONFIGURATION
// ============================================================================
mavenPublishing {
    // Bundle Dokka v2 HTML output inside -javadoc.jar so consumers browsing
    // Maven Central artifacts get real API docs rather than an empty jar.
    // Task name is the Dokka v2 ID; the DokkaConventionPlugin in build-logic
    // registers it via `org.jetbrains.dokka` + DokkaExtension.
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = true,
        ),
    )
    signAllPublications()

    pom {
        name = "CMP Intent Launcher Compose"
        description =
            "Compose Multiplatform extensions for cmp-intent-launcher — @Composable rememberIntentLauncher() " +
            "with Android rememberLauncherForActivityResult bridge. Add alongside cmp-intent-launcher to keep " +
            "Composable usage; core module now reaches 19 KMP targets without this dep."
        inceptionYear = "2026"
        url = "https://github.com/MobileByteLabs/KmpToolkit/"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "MobileByteLabs"
                name = "MobileByteLabs"
                url = "https://github.com/MobileByteLabs"
            }
        }

        scm {
            url = "https://github.com/MobileByteLabs/KmpToolkit/"
            connection = "scm:git:git://github.com/MobileByteLabs/KmpToolkit.git"
            developerConnection = "scm:git:ssh://git@github.com/MobileByteLabs/KmpToolkit.git"
        }
    }
}

// Library Runtime Observability — auto-generate CmpMetadata.kt for cmp-observe hooks (epic 2026-05-30)
apply(from = "$rootDir/cmp-observe-metadata.gradle.kts")

// Generates robolectric.ROBOLECTRIC_SDK from the version catalog for androidHostTest.
apply(from = "$rootDir/robolectric-host-test.gradle.kts")
kotlin.sourceSets.getByName("androidHostTest").kotlin.srcDir(
    tasks.named("generateRobolectricSdkConstant"),
)
