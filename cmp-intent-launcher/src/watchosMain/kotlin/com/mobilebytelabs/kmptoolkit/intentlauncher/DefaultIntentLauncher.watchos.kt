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
 * This target's launcher is stateless, so the no-arg constructor is always the right answer.
 *
 * Written per source set rather than in one shared file because `IntentLauncher` is an
 * `expect class` whose `actual` lives in each leaf — an intermediate source set can see the
 * expect but cannot construct it.
 */
internal actual fun defaultIntentLauncher(): IntentLauncher? = IntentLauncher()
