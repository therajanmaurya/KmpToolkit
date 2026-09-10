/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlin.reflect.KClass

/**
 * Retained no-op marker. **cmp-intent-launcher graduated to a stable API — no opt-in is required.**
 *
 * This annotation no longer carries [RequiresOptIn], so it neither warns nor demands `@OptIn`. It
 * is kept only so source written against the experimental era — `@OptIn(...)` or
 * `-opt-in=...ExperimentalIntentLauncherApi` — keeps compiling. Both are now redundant and can be
 * deleted.
 *
 * Scheduled for removal in the next major version.
 */
@Deprecated(
    message = "cmp-intent-launcher is stable; the opt-in is no longer required. Remove the @OptIn / annotation.",
    level = DeprecationLevel.WARNING,
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalIntentLauncherApi

// -----------------------------------------------------------------------------
// Result + error types
// -----------------------------------------------------------------------------

public sealed class IntentResult {
    public data class Ok(val data: IntentData?) : IntentResult()
    public object Cancelled : IntentResult()
    public data class Failed(val cause: IntentError) : IntentResult()
}

public sealed class IntentError {
    public object UnsupportedPlatform : IntentError()
    public object NoHandler : IntentError()
    public object UserGestureMissing : IntentError()
    public data class Unknown(val message: String) : IntentError()
}

public data class IntentData(
    val uri: String? = null,
    val mimeType: String? = null,
    val extras: Map<String, Any?> = emptyMap(),
)

// -----------------------------------------------------------------------------
// Result contract typeclass
// -----------------------------------------------------------------------------

public sealed interface ResultContract<R> {
    public val resultType: KClass<*>
    public fun parse(data: IntentData?): R
}

public object ResultContracts {
    public object PickImage : ResultContract<String?> {
        override val resultType: KClass<*> = String::class
        override fun parse(data: IntentData?): String? = data?.uri
    }

    public object PickMultipleImages : ResultContract<List<String>> {
        override val resultType: KClass<*> = List::class

        @Suppress("UNCHECKED_CAST")
        override fun parse(data: IntentData?): List<String> =
            (data?.extras?.get("uris") as? List<String>) ?: listOfNotNull(data?.uri)
    }

    public object PickDocument : ResultContract<String?> {
        override val resultType: KClass<*> = String::class
        override fun parse(data: IntentData?): String? = data?.uri
    }

    public object PickContact : ResultContract<String?> {
        override val resultType: KClass<*> = String::class
        override fun parse(data: IntentData?): String? = data?.uri
    }

    public class Custom<R>(private val parse: (IntentData?) -> R, override val resultType: KClass<*>) :
        ResultContract<R> {
        override fun parse(data: IntentData?): R = parse.invoke(data)
    }
}

// -----------------------------------------------------------------------------
// IntentBuilder DSL — collects builder state; per-platform converters consume
// -----------------------------------------------------------------------------

public class IntentBuilder internal constructor() {
    internal var action: String? = null
    internal var data: String? = null
    internal var type: String? = null
    internal val categories: MutableList<String> = mutableListOf()
    internal var flags: Int = 0
    internal val extras: MutableMap<String, Any?> = mutableMapOf()
    internal var packageName: String? = null
    internal var resultContract: ResultContract<*>? = null
    internal var onUnsupportedHandler: (() -> IntentResult)? = null

    public fun action(name: String): IntentBuilder = apply { action = name }
    public fun data(uri: String): IntentBuilder = apply { data = uri }
    public fun type(mime: String): IntentBuilder = apply { type = mime }
    public fun category(cat: String): IntentBuilder = apply { categories.add(cat) }
    public fun flag(flag: Int): IntentBuilder = apply { flags = flags or flag }
    public fun extra(key: String, value: Any?): IntentBuilder = apply { extras[key] = value }
    public fun packageName(pkg: String): IntentBuilder = apply { packageName = pkg }
    public fun <R> result(contract: ResultContract<R>): IntentBuilder = apply { resultContract = contract }
    public fun onUnsupported(handler: () -> IntentResult): IntentBuilder = apply { onUnsupportedHandler = handler }
}

// -----------------------------------------------------------------------------
// Public entry point — Compose-free core (v0.3 split per inter-app-comms-real-native-impls Phase 1)
// -----------------------------------------------------------------------------
//
// BREAKING (v0.3): `@Composable rememberIntentLauncher()` moved to a separate
// `cmp-intent-launcher-compose` adapter module so this core module can reach all 19
// KMP targets (Compose Compiler plugin is module-level and required compose.runtime
// on every target classpath, blocking tvOS/watchOS/Linux/mingw).
//
// Consumers using Compose: add `io.github.mobilebytelabs:cmp-intent-launcher-compose`
// alongside this dep. Other consumers: call `ComponentActivity.intentLauncher()`
// directly (Android) or construct `IntentLauncher` via the platform actual ctor.

public expect class IntentLauncher {
    public suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult
}
