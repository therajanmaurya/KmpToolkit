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

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * wasmWasi publishes the manifest across the host boundary, and reach reflects whether a host is
 * listening — so an app can tell the difference between "registered" and "reachable".
 */
class WasiAppIntentsTest {

    @BeforeTest
    fun setUp() {
        WasiAppIntents.reset()
        WasiAppIntents.echoToStdout = false
    }

    @AfterTest
    fun tearDown() = WasiAppIntents.reset()

    private fun sample() = appIntents {
        intent("add_task") {
            title = "Add task"
            perform { AppIntentResult.Done }
        }
    }

    @Test
    fun without_a_host_handler_reach_is_manifest_not_nothing() {
        // stdout still carries it out of the process, so claiming zero reach would be wrong.
        assertEquals(AppIntentsCapabilities.ManifestOnly, platformAppIntentsCapabilities)
        assertTrue(platformAppIntentsCapabilities.publishesManifest)
    }

    @Test
    fun registering_buffers_the_manifest_for_the_host() {
        AppIntentsManagerImpl().register(sample())
        val manifest = assertIs<String>(WasiAppIntents.lastManifest)
        assertTrue(manifest.contains("add_task"), manifest)
        assertTrue(manifest.contains("Add task"), manifest)
    }

    @Test
    fun a_registered_handler_receives_the_manifest_and_raises_reach_to_os() {
        val seen = mutableListOf<String>()
        WasiAppIntents.onRegister = { seen += it }

        assertEquals(AppIntentsCapabilities.OsIntegrated, platformAppIntentsCapabilities)
        AppIntentsManagerImpl().register(sample())
        assertTrue(seen.single().contains("add_task"))
    }

    @Test
    fun the_host_can_invoke_a_registered_intent() = runTest {
        AppIntentsManagerImpl().register(sample())
        // The other direction: the host calls in when it wants the intent performed.
        assertIs<AppIntentResult.Done>(WasiAppIntents.invoke("add_task"))
    }

    @Test
    fun invoking_an_unknown_intent_is_null_so_a_host_can_report_it() = runTest {
        AppIntentsManagerImpl().register(sample())
        assertEquals(null, WasiAppIntents.invoke("no_such_intent"))
    }
}
