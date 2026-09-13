/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import com.mobilebytelabs.kmptoolkit.appreview.di.appReviewModule
import com.mobilebytelabs.kmptoolkit.openurl.testing.FakeUrlLauncher
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class AppReviewModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
        AppReview.reset()
    }

    @Test
    fun manager_resolves_from_the_module_and_is_a_singleton() {
        val koin = startKoin { modules(appReviewModule()) }.koin
        assertIs<AppReviewManagerImpl>(koin.get<AppReviewManager>())
        assertSame(koin.get<AppReviewManager>(), koin.get<AppReviewManager>())
    }

    @Test
    fun the_injected_manager_and_the_shared_object_see_the_same_configuration() {
        // They are deliberately NOT the same instance any more: the module builds its own, and both
        // read the one globally configured listing at call time. That is what removes the drift —
        // there is a single listing, not two copies to keep in step.
        val koin = startKoin {
            modules(appReviewModule(StoreListing(webUrl = "https://example.com/app")))
        }.koin

        val injected = koin.get<AppReviewManager>()

        assertSame(injected.capabilities, AppReview.capabilities)
        assertEquals(injected.openStoreListing()::class, AppReview.openStoreListing()::class)
    }

    @Test
    fun the_listing_passed_to_the_module_reaches_the_shared_entry_point() = runTest {
        // Without the configure() call this would be NoStoreConfigured: the global object would still
        // be holding its unconfigured default while the graph had the real listing.
        // The module itself applies the listing — no separate startup call.
        startKoin { modules(appReviewModule(StoreListing(webUrl = "https://example.com/app"))) }

        val result = AppReview.requestReview()

        assertEquals(
            false,
            result is AppReviewResult.NoStoreConfigured,
            "the module's listing must be visible to AppReview, got $result",
        )
    }

    @Test
    fun an_injected_launcher_is_used_by_both_paths() {
        val launcher = FakeUrlLauncher()
        // Module first: it is what applies the listing. Installing a custom manager afterwards keeps
        // that listing — the manager reads it at call time rather than holding its own.
        startKoin { modules(appReviewModule(StoreListing(webUrl = "https://example.com/app"))) }
        AppReview.configure(AppReviewManagerImpl(urlLauncher = launcher))

        AppReview.openStoreListing()

        assertEquals(1, launcher.opened.size, "the store listing went through the injected launcher")
    }

    @Test
    fun registering_the_module_with_no_listing_clears_a_previously_configured_one() {
        // Worth pinning because it can surprise: the module APPLIES its listing, so the no-argument
        // form resets to None. DI setup is the source of truth by design — but a consumer who
        // configured separately and then registered `appReviewModule()` would lose it, and this test
        // is where that shows up rather than in their store button doing nothing.
        AppReview.configure(StoreListing(webUrl = "https://example.com/app"))

        startKoin { modules(appReviewModule()) }

        assertIs<AppReviewResult.NoStoreConfigured>(AppReview.openStoreListing())
    }
}
