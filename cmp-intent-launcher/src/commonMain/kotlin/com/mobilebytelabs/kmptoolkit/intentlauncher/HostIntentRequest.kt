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
 * A read-only snapshot of an [IntentBuilder], for targets that route requests across a **host
 * boundary** rather than to an OS intent system.
 *
 * ## Why this exists
 * A picker is not something you can degrade the way a share sheet degrades to a clipboard — there
 * is no "copy" equivalent for "let the user choose a photo". But a target with no picker still has
 * something outside it: on wasmWasi, the host that embedded the module. Handing the request over
 * lets that host serve it, which beats refusing on a platform where the embedder may well have a
 * perfectly good file dialog of its own.
 *
 * [SystemIntents] requests arrive here too, under the synthetic actions
 * [ACTION_APP_SETTINGS] and [ACTION_CREATE_DOCUMENT], so a host implements one function rather
 * than three.
 */
public class HostIntentRequest(
    /** Platform-style action string, or one of the synthetic `cmp.action.*` constants. */
    public val action: String?,
    /** Target URI, when the caller set one. */
    public val data: String?,
    /** MIME type filter, when the caller set one. */
    public val type: String?,
    /** Category strings collected by the builder. */
    public val categories: List<String>,
    /** Extras collected by the builder. */
    public val extras: Map<String, Any?>,
    /** Explicit target package, when the caller set one. */
    public val packageName: String?,
    /** Whether the caller expects a result back — a picker rather than a fire-and-forget launch. */
    public val expectsResult: Boolean,
) {
    override fun toString(): String =
        "HostIntentRequest(action=$action, data=$data, type=$type, expectsResult=$expectsResult)"

    public companion object {
        /** Synthetic action for [SystemIntents.openAppSettings]. */
        public const val ACTION_APP_SETTINGS: String = "cmp.action.APP_SETTINGS"

        /** Synthetic action for [SystemIntents.createDocument]. */
        public const val ACTION_CREATE_DOCUMENT: String = "cmp.action.CREATE_DOCUMENT"

        /** Extra key carrying `suggestedName` for [ACTION_CREATE_DOCUMENT]. */
        public const val EXTRA_SUGGESTED_NAME: String = "cmp.extra.SUGGESTED_NAME"
    }
}

/** Snapshot this builder's collected state for a host handler. */
internal fun IntentBuilder.toHostRequest(): HostIntentRequest = HostIntentRequest(
    action = action,
    data = data,
    type = type,
    categories = categories.toList(),
    extras = extras.toMap(),
    packageName = packageName,
    expectsResult = resultContract != null,
)

/** Build a request snapshot from a builder block, without dispatching it. Used by test doubles. */
public fun snapshotOf(block: IntentBuilder.() -> Unit): HostIntentRequest = IntentBuilder().apply(block).toHostRequest()
