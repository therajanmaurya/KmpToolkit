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
 * Android has no context-free launcher: [IntentLauncher] wraps an `ActivityResultLauncher` and the
 * pending-result plumbing, both of which belong to a live Activity. Callers obtain one with
 * `ComponentActivity.intentLauncher()`.
 */
internal actual fun defaultIntentLauncher(): IntentLauncher? = null
