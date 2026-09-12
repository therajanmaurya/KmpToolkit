/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

import com.mobilebytelabs.kmptoolkit.share.di.shareModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

private var started = false

/**
 * Start Koin with the toolkit's own `shareModule` — the module cmp-share ships, not one written
 * here. That is the point of the demo: a consumer adds one module and injects `ShareManager`.
 *
 * Called from [App] rather than from each platform entry point, because this sample has six of
 * them (Android Activity, iOS view controller, JVM main, JS, wasmJs) and the guard makes a single
 * shared call safe. A real app starts Koin once in its own platform entry point.
 */
internal fun initKoinOnce() {
    if (started) return
    started = true
    startKoin { modules(shareModule) }
}

/** Resolve a dependency the way an app would, once Koin is running. */
internal inline fun <reified T : Any> inject(): T = KoinPlatform.getKoin().get<T>()
