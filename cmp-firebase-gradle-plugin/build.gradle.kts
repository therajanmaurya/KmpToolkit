import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.vanniktech.mavenPublish)
}

// ============================================================================
// LIBRARY CONFIGURATION
// ============================================================================
// Version is locked to kmptoolkit.version — the plugin enforces build-side setup for a
// SPECIFIC cmp-firebase release (its Kotlin floor, its static-framework requirement), so a
// plugin/library version skew would enforce the wrong contract.
group = "io.github.mobilebytelabs"
version = providers.gradleProperty("kmptoolkit.version").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
    explicitApi()
}

dependencies {
    // compileOnly: the consumer's build already has the Kotlin Gradle plugin on its
    // classpath. Bundling it would risk a version clash with whatever KGP they apply.
    compileOnly(libs.kotlin.gradle.plugin)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlin.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("cmpFirebase") {
            id = "io.github.mobilebytelabs.kmptoolkit.firebase"
            implementationClass =
                "io.github.mobilebytelabs.kmptoolkit.firebase.gradle.CmpFirebasePlugin"
            displayName = "cmp-firebase build setup"
            description =
                "Enforces the build-side setup cmp-firebase requires but cannot check from a " +
                "published artifact: isStatic=true on every Apple framework (Firebase's SwiftPM " +
                "products are static; a dynamic framework crashes at runtime) and a Kotlin 2.4.20 " +
                "floor (below it the transitive SwiftPM resolution that pulls firebase-ios-sdk " +
                "does not exist, and the build fails with an unrelated-looking linker error)."
            tags = listOf("kotlin-multiplatform", "firebase", "swiftpm", "ios")
        }
    }
}

// ============================================================================
// MAVEN CENTRAL PUBLISHING CONFIGURATION
// ============================================================================
// GradlePlugin() also publishes the plugin MARKER artifact
// (io.github.mobilebytelabs.kmptoolkit.firebase:…gradle.plugin), which is what lets a
// consumer resolve `plugins { id("…") version "…" }` from Maven Central. No Gradle Plugin
// Portal account or release step is required — consumers only need mavenCentral() in their
// settings.gradle.kts `pluginManagement.repositories`, which nearly every KMP project has.
mavenPublishing {
    configure(
        GradlePlugin(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
        ),
    )
    signAllPublications()

    pom {
        name = "CMP Firebase Gradle Plugin"
        description =
            "Build-side setup enforcement for cmp-firebase — static Apple frameworks + Kotlin floor check."
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
