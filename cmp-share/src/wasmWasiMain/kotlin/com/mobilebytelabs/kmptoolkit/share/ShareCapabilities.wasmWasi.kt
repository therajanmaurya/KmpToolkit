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
 * wasmWasi — every payload kind crosses the host boundary via [WasiShare], either to a registered
 * handler or to the stdout/outbox default. Nothing is refused, so nothing here is `false`.
 *
 * Image bytes are Base64-encoded into the emitted record, and a file is passed by URI for the host
 * to resolve — WASI cannot open an arbitrary path itself without a matching preopened directory.
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.Full
