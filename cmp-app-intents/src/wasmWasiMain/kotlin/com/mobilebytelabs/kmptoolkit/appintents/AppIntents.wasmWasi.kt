/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

/**
 * The wasmWasi app-intents channel.
 *
 * WASI has no assistant, no launcher and no shortcut system — but registering intents is
 * fundamentally *publishing a manifest*, and a WASI module can always hand that to the host that
 * embedded it. The host is then free to expose the intents however it likes: a CLI subcommand, an
 * HTTP route, an MCP tool list.
 *
 * ```kotlin
 * WasiAppIntents.onRegister = { manifestJson -> hostRegisterTools(manifestJson) }
 * AppIntents.register(appIntents { intent("add_task") { title = "Add task" } })
 * ```
 *
 * With no handler the manifest is buffered in [lastManifest] and echoed to stdout, so a host that
 * only reads process output still receives it. Invocation comes back the other way — the host
 * calls [invoke] when it wants an intent performed.
 */
public object WasiAppIntents {

    /** Called with the serialized manifest each time [AppIntents.register] runs. */
    public var onRegister: ((String) -> Unit)? = null

    /** Whether un-handled manifests are printed to stdout. Default `true`. */
    public var echoToStdout: Boolean = true

    /** The most recently registered manifest JSON, or `null` before the first registration. */
    public var lastManifest: String? = null
        private set

    /** Whether a host handler is registered. */
    public val isConfigured: Boolean get() = onRegister != null

    /**
     * Perform a registered intent — the host's way in.
     *
     * Returns `null` when no intent with [id] is registered, which a host should surface as an
     * unknown-command error rather than silence.
     */
    public suspend fun invoke(id: String, params: Map<String, Any> = emptyMap()): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)

    /** Clear the handler and buffered manifest — intended for tests. */
    public fun reset() {
        onRegister = null
        echoToStdout = true
        lastManifest = null
    }

    internal fun publish(manifest: String) {
        lastManifest = manifest
        val handler = onRegister
        if (handler != null) {
            handler(manifest)
            return
        }
        if (echoToStdout) println("cmp-app-intents|manifest|$manifest")
    }
}

/** wasmWasi `AppIntents` — publishes the manifest across the host boundary via [WasiAppIntents]. */
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
        WasiAppIntents.publish(config.serializeManifest())
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
