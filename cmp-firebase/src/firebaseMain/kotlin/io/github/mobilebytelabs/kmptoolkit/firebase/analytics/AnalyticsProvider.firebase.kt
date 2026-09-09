/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.FirebaseAnalyticsHelper

private const val TAG = "FirebaseAnalytics"

/**
 * Firebase-tier actual: Android · iOS (×3) · macOS (×2) · tvOS (×3) · JS · wasmJs.
 *
 * Returns a [FirebaseAnalyticsHelper] backed by GitLive's `Firebase.analytics`, or
 * [NoOpAnalyticsHelper] when the platform's native Firebase was never initialized.
 *
 * **Why the guard.** `Firebase.analytics` requires a configured default `FirebaseApp`.
 * An app that never supplied options for this platform (no `FirebaseConfig.web` on
 * JS/wasmJs, no `google-services.json` on Android, no `GoogleService-Info.plist` on
 * Apple) has no default app, and the accessor throws — on web as
 * `Firebase: No Firebase App '[DEFAULT]' has been created - call initializeApp() first
 * (app/no-app)`. Letting that escape would break the app on the very path
 * `FirebaseKit.initialize` already logs as "analytics will NoOp", violating its
 * documented contract:
 *
 * > "analytics degrades to NoOp with a WARN — it never throws (analytics must not
 * > break the app)."
 *
 * So an uninitialized Firebase degrades here exactly as the non-Firebase tier does,
 * with a WARN naming the missing setup. Pinned by `AnalyticsProviderContractTest`,
 * which runs on every target including `wasmJsBrowserTest`/`jsBrowserTest`.
 *
 * Setup prerequisites:
 * - **Android**: `google-services.json` + `com.google.gms.google-services` plugin applied
 * - **iOS / macOS / tvOS**: `GoogleService-Info.plist` + `FirebaseApp.configure()` in AppDelegate
 *   (or `@main App.init`). The native `firebase-ios-sdk` is linked via SwiftPM, resolved
 *   transitively from GitLive 3.x — not CocoaPods, and not declared by the consumer.
 * - **JS / wasmJs**: Firebase config object passed during app init (both read
 *   `FirebaseConfig.web`; wasmJs is JS-parity via GitLive 3.0.0-alpha02+)
 */
internal actual fun createPlatformAnalyticsHelper(): AnalyticsHelper =
    runCatching { FirebaseAnalyticsHelper(Firebase.analytics) }
        .getOrElse { cause ->
            Logger.w(TAG, cause) {
                "Native Firebase is not initialized on this platform — analytics degrades to NoOp. " +
                    "Supply this platform's options via FirebaseKit.initialize(FirebaseConfig...) " +
                    "(web/wasmJs read FirebaseConfig.web), or add the platform's native config file."
            }
            NoOpAnalyticsHelper
        }
