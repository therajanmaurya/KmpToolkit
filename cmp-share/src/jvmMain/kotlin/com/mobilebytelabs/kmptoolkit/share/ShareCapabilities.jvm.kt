/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

/**
 * JVM desktop — native dispatch (`open` / `xdg-open` / `cmd /c start`) for links and files,
 * AWT clipboard and a save dialog for everything else. Every kind has a path.
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.Full
