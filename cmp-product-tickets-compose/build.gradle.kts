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
    // REQUIRED, not optional: ProductTicketsNavigation declares @Serializable route classes for
    // type-safe Navigation Compose. Without this plugin the code still COMPILES — @Serializable is
    // just an annotation — but no serializer is generated, so `navigate(CreateTicketRoute(...))` and
    // `toRoute<TicketDetailRoute>()` fail at RUNTIME. Its absence was caught by the BCV baseline
    // diff (the generated $$serializer / $Companion vanished), not by the build.
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.binaryCompatibilityValidator)
    id("io.github.mobilebytelabs.kmptoolkit.dokka")
    id("io.github.mobilebytelabs.kmptoolkit.kover")
}

// ============================================================================
// LIBRARY CONFIGURATION — cmp-product-tickets-compose
// ============================================================================
// The Compose surface of cmp-product-tickets: the tickets list / detail / create screens, the
// navigation graph, ProductTicketsViewModel, and the `productTicketsModule` Koin module.
//
// WHY THIS MODULE EXISTS (E4, 2026-09-12)
// Structurally the same problem E2 solved for cmp-remote-config, and the same fix: 7 of the
// module's 13 files needed no renderer, but material3 / navigation-compose /
// koin-compose-viewmodel in `commonMain` pinned the whole library to the 7 Compose-Multiplatform
// targets. Filing a ticket from a server, a CLI or a background worker was impossible.
//
// As with E2, a Compose-only source set inside one module is NOT an option: the Compose compiler
// plugin applies to every compilation and fails with "The Compose Compiler requires the Compose
// Runtime to be on the class path" wherever the runtime is absent.
//
// Targets are the 7 Compose-Multiplatform ones. iosX64 / macosX64 are absent because Compose
// 1.12.0 publishes no artifact for either.
// ============================================================================
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    android {
        namespace = "com.mobilebytelabs.producttickets.compose"
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
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder { sourceSetTreeName = "test" }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources.enable = true
    }

    iosArm64()
    iosSimulatorArm64()

    macosArm64()

    js {
        browser()
        nodejs()
    }

    wasmJs { browser() }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: this module's public surface takes and returns core
            // types (RemoteConfig, UiNode, ActionType), so a consumer must see them — and it keeps
            // a single `cmp-product-tickets-compose` dependency sufficient to use the whole library.
            api(project(":cmp-product-tickets"))

            // Compose
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)

            // DI — koin-core is `implementation` in the core module, so it is not visible
            // transitively; `productTicketsModule` needs it (and `productTicketsDataModule`, which it
            // includes) at compile time here.
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)

            // Navigation
            implementation(libs.navigation.compose)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING
// ============================================================================
mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = true,
        ),
    )
    signAllPublications()

    pom {
        name = "CMP Product Tickets Compose"
        description =
            "Compose Multiplatform surface for cmp-product-tickets — tickets list / detail / create " +
            "screens, the navigation graph, ProductTicketsViewModel and the productTicketsModule Koin " +
            "module. Add alongside cmp-product-tickets to render the ticketing UI."
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

// Library Runtime Observability — auto-generate CmpMetadata.kt for cmp-observe hooks
apply(from = "$rootDir/cmp-observe-metadata.gradle.kts")
