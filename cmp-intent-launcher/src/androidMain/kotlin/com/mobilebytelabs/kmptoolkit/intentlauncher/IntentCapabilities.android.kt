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
 * Android — every operation maps to a real system Intent, with the library-bundled
 * `IntentLauncherInitProvider` and `CreateDocumentProxyActivity` supplying the dispatch context.
 */
public actual val platformIntentCapabilities: IntentCapabilities = IntentCapabilities.Full
