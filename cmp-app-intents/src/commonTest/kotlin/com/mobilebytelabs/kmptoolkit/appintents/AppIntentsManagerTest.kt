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

import com.mobilebytelabs.kmptoolkit.appintents.testing.FakeAppIntentsManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract for the injectable [AppIntentsManager] facade, and for the reach model that tells a
 * caller whether registering will actually get anywhere.
 */
class AppIntentsManagerTest {

    private fun sample() = appIntents {
        intent("add_task") {
            title = "Add task"
            description = "Create a new task"
            parameter("text", ParamType.Text)
            perform { params -> AppIntentResult.Dialog("added ${params["text"]}") }
        }
    }

    // ---- registration ------------------------------------------------------------------------

    @Test
    fun register_records_the_config() {
        val fake = FakeAppIntentsManager()
        fake.register(sample())
        assertEquals(listOf("add_task"), fake.registered.single().intents.map { it.id })
    }

    @Test
    fun the_builder_overload_registers_what_it_built_and_returns_it() {
        val fake = FakeAppIntentsManager()
        val config = fake.register {
            intent("open_inbox") { title = "Open inbox" }
        }
        assertEquals(config, fake.registered.single())
        assertEquals("open_inbox", config.intents.single().id)
    }

    @Test
    fun current_reflects_the_latest_registration() {
        val fake = FakeAppIntentsManager()
        assertNull(fake.current())
        fake.register(sample())
        fake.register { intent("second") { title = "Second" } }
        assertEquals("second", fake.current()?.intents?.single()?.id)
    }

    // ---- invocation ---------------------------------------------------------------------------

    @Test
    fun invoke_runs_the_perform_block_with_its_parameters() = runTest {
        val fake = FakeAppIntentsManager()
        fake.register(sample())

        val result = assertIs<AppIntentResult.Dialog>(fake.invoke("add_task", mapOf("text" to "milk")))
        assertEquals("added milk", result.message)
    }

    @Test
    fun invoking_an_unknown_id_is_null_not_a_failure_result() = runTest {
        // Distinct outcomes: null means "no such intent", Failed means "it ran and went wrong".
        val fake = FakeAppIntentsManager()
        fake.register(sample())
        assertNull(fake.invoke("nope"))
    }

    // ---- reach --------------------------------------------------------------------------------

    @Test
    fun reach_is_a_spectrum_not_a_boolean() {
        assertEquals("os", AppIntentsCapabilities.OsIntegrated.assistantReach)
        assertEquals("manifest", AppIntentsCapabilities.ManifestOnly.assistantReach)
        assertEquals("in-process", AppIntentsCapabilities.InProcessOnly.assistantReach)
    }

    @Test
    fun every_target_at_least_registers_in_process() {
        // The floor the runtime registry guarantees everywhere — the reason invoke() always works.
        listOf(
            AppIntentsCapabilities.OsIntegrated,
            AppIntentsCapabilities.ManifestOnly,
            AppIntentsCapabilities.InProcessOnly,
        ).forEach { assertTrue(it.inProcess, "in-process registration is universal") }
    }

    @Test
    fun reachesOs_gates_the_voice_affordance() {
        assertTrue(FakeAppIntentsManager(AppIntentsCapabilities.OsIntegrated).reachesOs())
        // A manifest nobody consumes must NOT be advertised as assistant reach.
        assertFalse(FakeAppIntentsManager(AppIntentsCapabilities.ManifestOnly).reachesOs())
        assertFalse(FakeAppIntentsManager(AppIntentsCapabilities.InProcessOnly).reachesOs())
    }

    @Test
    fun the_real_impl_reports_this_platforms_reach() {
        assertEquals(platformAppIntentsCapabilities, AppIntentsManagerImpl().capabilities)
        assertTrue(AppIntentsManagerImpl().capabilities.inProcess)
    }

    // ---- web app manifest shortcuts ------------------------------------------------------------

    @Test
    fun shortcuts_json_carries_one_entry_per_intent_with_its_id_in_the_url() {
        val json = sample().webAppManifestShortcuts(baseUrl = "/intent")
        assertTrue(json.contains("\"name\":\"Add task\""), json)
        assertTrue(json.contains("\"url\":\"/intent?id=add_task\""), json)
    }

    @Test
    fun shortcuts_json_appends_to_an_existing_query_string() {
        val json = sample().webAppManifestShortcuts(baseUrl = "/app?src=pwa")
        assertTrue(json.contains("/app?src=pwa&id=add_task"), json)
    }

    @Test
    fun short_name_is_truncated_for_launchers_with_little_room() {
        val long = appIntents { intent("x") { title = "A very long intent title indeed" } }
        val json = long.webAppManifestShortcuts(shortNameMaxLength = 8)
        assertTrue(json.contains("\"short_name\":\"A very l\""), json)
    }

    @Test
    fun an_intent_with_no_title_falls_back_to_its_id() {
        val untitled = appIntents { intent("fallback_id") { } }
        val json = untitled.webAppManifestShortcuts()
        assertTrue(json.contains("\"name\":\"fallback_id\""), json)
    }
}
