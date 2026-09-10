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

/** One thing an [IntentManager] can be asked to do. Pair with [IntentManager.supports]. */
public enum class IntentOperation {
    /** Hand a URI to whatever handles it — the commonest intent of all. */
    ViewUri,

    /** Let the user choose one image. */
    PickImage,

    /** Let the user choose several images. */
    PickMultipleImages,

    /** Let the user choose an arbitrary document. */
    PickDocument,

    /** Let the user choose a contact. */
    PickContact,

    /** Open this app's own settings screen. */
    OpenAppSettings,

    /** Ask the user where to save a new file. */
    CreateDocument,
}

/**
 * Which [IntentOperation]s this target can actually perform.
 *
 * ## Why this exists
 * Intent support is genuinely ragged: a phone has every picker, a desktop has file dialogs but no
 * contact picker, a browser has neither app settings nor contacts, and tvOS has no picker surface
 * at all. Without a way to ask, a UI must either offer actions that fail once tapped or hide
 * features everywhere because one platform lacks them.
 *
 * Ask [IntentManager.supports] before rendering an affordance. A `true` means *the implementation
 * attempts it*, not that it must succeed — a cancelled picker or a missing handler is still
 * reported per call through [IntentResult].
 */
public data class IntentCapabilities(
    public val viewUri: Boolean,
    public val pickImage: Boolean,
    public val pickMultipleImages: Boolean,
    public val pickDocument: Boolean,
    public val pickContact: Boolean,
    public val openAppSettings: Boolean,
    public val createDocument: Boolean,
) {
    /** Whether [operation] is supported here. */
    public operator fun contains(operation: IntentOperation): Boolean = when (operation) {
        IntentOperation.ViewUri -> viewUri
        IntentOperation.PickImage -> pickImage
        IntentOperation.PickMultipleImages -> pickMultipleImages
        IntentOperation.PickDocument -> pickDocument
        IntentOperation.PickContact -> pickContact
        IntentOperation.OpenAppSettings -> openAppSettings
        IntentOperation.CreateDocument -> createDocument
    }

    public companion object {
        /** Everything — Android and iOS. */
        public val Full: IntentCapabilities = IntentCapabilities(
            viewUri = true,
            pickImage = true,
            pickMultipleImages = true,
            pickDocument = true,
            pickContact = true,
            openAppSettings = true,
            createDocument = true,
        )

        /** Nothing — tvOS, and wasmWasi before a host handler is registered. */
        public val None: IntentCapabilities = IntentCapabilities(
            viewUri = false,
            pickImage = false,
            pickMultipleImages = false,
            pickDocument = false,
            pickContact = false,
            openAppSettings = false,
            createDocument = false,
        )

        /**
         * File dialogs and URL opening, but no contact picker — every desktop target.
         *
         * `pickMultipleImages` is left to the caller: Linux, macOS and Windows manage it, the JVM
         * actual does not.
         */
        public fun desktop(pickMultipleImages: Boolean): IntentCapabilities = IntentCapabilities(
            viewUri = true,
            pickImage = true,
            pickMultipleImages = pickMultipleImages,
            pickDocument = true,
            pickContact = false,
            openAppSettings = true,
            createDocument = true,
        )
    }
}

/**
 * What THIS target can do. See each `actual` for the reasoning behind its values.
 *
 * | Target | view | pick image | multi | document | contact | settings | create |
 * |---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
 * | Android, iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
 * | macOS, Linux, Windows | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
 * | JVM | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ |
 * | JS, wasmJs | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
 * | watchOS | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
 * | tvOS | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
 * | wasmWasi | ⚙️ | ⚙️ | ⚙️ | ⚙️ | ⚙️ | ⚙️ | ⚙️ |
 *
 * ⚙️ Dynamic — [None] until a `WasiIntents.handler` is registered, then [Full].
 */
public expect val platformIntentCapabilities: IntentCapabilities
