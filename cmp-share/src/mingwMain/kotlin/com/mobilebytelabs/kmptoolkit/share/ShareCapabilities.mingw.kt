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
 * Windows — everything is carried, though only links reach a real handler.
 *
 * `ShellExecuteW` opens a link; text goes to the Win32 clipboard via `clip.exe`; a file's PATH
 * goes to the clipboard; an image is written to `%TEMP%` and its path copied. There is no
 * ambient Windows share sheet short of the WinRT share contract, so the clipboard is the
 * degradation — the same one the JVM, Linux and web targets use, and far more useful than
 * refusing.
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.Full
