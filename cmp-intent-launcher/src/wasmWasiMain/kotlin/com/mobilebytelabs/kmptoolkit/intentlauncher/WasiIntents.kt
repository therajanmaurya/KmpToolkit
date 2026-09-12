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

/**
 * The wasmWasi intent channel — see [HostIntentRequest] for why a sandbox still has somewhere to
 * send a request.
 *
 * WASI has no windowing system and no picker, so there is nothing to degrade to *inside* the
 * sandbox. What it does have is the host that embedded it, and a host commonly can serve exactly
 * these requests — open a file chooser, resolve a path, surface a settings page.
 *
 * ```kotlin
 * WasiIntents.handler = { request ->
 *     when (request.action) {
 *         HostIntentRequest.ACTION_CREATE_DOCUMENT ->
 *             hostSavePanel(request.extras[HostIntentRequest.EXTRA_SUGGESTED_NAME] as? String)
 *                 ?.let { IntentResult.Ok(IntentData(uri = it)) }
 *                 ?: IntentResult.Cancelled
 *         else -> IntentResult.Failed(IntentError.NoHandler)
 *     }
 * }
 * ```
 *
 * With no handler, every call reports [IntentError.NoHandler] with a message saying how to wire
 * one — never a silent success. [SystemIntents] routes through here too, under the synthetic
 * `cmp.action.*` actions.
 */
public object WasiIntents {

    /** Serves intent requests for this module. `null` until the host registers one. */
    public var handler: ((HostIntentRequest) -> IntentResult)? = null

    /** Whether a handler is currently registered. */
    public val isConfigured: Boolean get() = handler != null

    /** Unregister the handler — intended for tests. */
    public fun reset() {
        handler = null
    }

    internal fun dispatch(request: HostIntentRequest): IntentResult = handler?.invoke(request) ?: IntentResult.Failed(
        IntentError.Unknown(
            "No WasiIntents.handler registered. WASI has no picker or settings surface of its " +
                "own, so the embedding host must serve intent requests. Set WasiIntents.handler " +
                "at startup, or check IntentManager.capabilities before offering the action.",
        ),
    )
}

/**
 * wasmWasi `IntentLauncher` — routes to [WasiIntents].
 *
 * A per-call `onUnsupported { }` handler still wins, matching every other target.
 */
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        if (!WasiIntents.isConfigured) {
            builder.onUnsupportedHandler?.let { return it.invoke() }
        }
        return WasiIntents.dispatch(builder.toHostRequest())
    }
}

/** wasmWasi `SystemIntents` — both entry points cross the same host boundary. */
public actual object SystemIntents {
    public actual suspend fun openAppSettings(): IntentResult = WasiIntents.dispatch(
        HostIntentRequest(
            action = HostIntentRequest.ACTION_APP_SETTINGS,
            data = null,
            type = null,
            categories = emptyList(),
            extras = emptyMap(),
            packageName = null,
            expectsResult = false,
        ),
    )

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        WasiIntents.dispatch(
            HostIntentRequest(
                action = HostIntentRequest.ACTION_CREATE_DOCUMENT,
                data = null,
                type = mimeType,
                categories = emptyList(),
                extras = mapOf(HostIntentRequest.EXTRA_SUGGESTED_NAME to suggestedName),
                packageName = null,
                expectsResult = true,
            ),
        )
}
