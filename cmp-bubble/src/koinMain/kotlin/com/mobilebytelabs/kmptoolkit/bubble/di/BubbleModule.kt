/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.bubble.di

import com.mobilebytelabs.kmptoolkit.bubble.Bubble
import com.mobilebytelabs.kmptoolkit.bubble.BubbleConfig
import com.mobilebytelabs.kmptoolkit.bubble.BubblePermission
import com.mobilebytelabs.kmptoolkit.bubble.createBubble
import com.mobilebytelabs.kmptoolkit.bubble.createBubblePermission
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module providing a [Bubble] and its [BubblePermission].
 *
 * ```kotlin
 * startKoin { modules(bubbleModule(BubbleConfig.Chat), appModule) }
 * ```
 *
 * The config is a parameter because it carries the notification channel id and name — values the
 * OS surfaces to users and which no library default could guess correctly for your app.
 *
 * Both are `single`s: a bubble owns platform resources (a tray icon on JVM, a notification channel
 * on Android), and building a second one would register those twice.
 *
 * **Not using Koin?** Call `createBubble(config)` once and share the instance yourself.
 */
public fun bubbleModule(config: BubbleConfig = BubbleConfig.Default): Module = module {
    single<Bubble> { createBubble(config) }
    single<BubblePermission> { createBubblePermission() }
}
