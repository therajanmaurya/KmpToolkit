/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

/**
 * Linux — manifest under `$XDG_DATA_HOME/cmp-app-intents/` plus a `.desktop` action handler per
 * intent, which xdg-desktop-portal and GNOME Shell surface as application actions.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities =
    AppIntentsCapabilities.OsIntegrated
