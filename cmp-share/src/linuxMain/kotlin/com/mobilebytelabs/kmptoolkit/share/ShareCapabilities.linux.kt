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
 * Linux — `xdg-open` for links and files, `xclip` for text; image payloads are written to a
 * temp file and opened. Every kind has a path, though what appears is a handler app rather
 * than a share chooser (X11 has no share-sheet concept).
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.Full
