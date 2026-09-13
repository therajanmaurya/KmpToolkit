/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import com.mobilebytelabs.kmptoolkit.appreview.testing.FakeAppReviewManager
import com.mobilebytelabs.kmptoolkit.openurl.UrlLauncher
import com.mobilebytelabs.kmptoolkit.openurl.testing.FakeUrlLauncher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behaviour that must hold on EVERY target, asserted through the injected seams rather than the real
 * platform APIs — Play and StoreKit report nothing about what the user did, so there is nothing
 * truthful to assert about them from a unit test.
 */
class AppReviewTest {

    @AfterTest
    fun tearDown() = AppReview.reset()

    private fun manager(listing: StoreListing, launcher: UrlLauncher) =
        AppReviewManagerImpl(listingOverride = listing, urlLauncher = launcher)

    @Test
    fun an_unconfigured_store_reports_NoStoreConfigured_rather_than_failing_silently() {
        // The distinction matters: NoStoreConfigured is a CONFIGURATION gap the developer can fix,
        // whereas Failed would imply the platform refused.
        val launcher = FakeUrlLauncher()
        val result = manager(StoreListing.None, launcher).openStoreListing()

        assertIs<AppReviewResult.NoStoreConfigured>(result)
        assertTrue(launcher.opened.isEmpty(), "nothing was opened")
    }

    @Test
    fun openStoreListing_opens_a_resolved_url_through_the_injected_launcher() {
        // Every platform resolves SOMETHING from a webUrl, so this assertion holds on all 21 targets
        // while the scheme-specific forms differ.
        val launcher = FakeUrlLauncher()
        val listing = StoreListing(webUrl = "https://example.com/app")

        val result = manager(listing, launcher).openStoreListing()

        assertIs<AppReviewResult.StoreOpened>(result)
        assertEquals(1, launcher.opened.size, "opened exactly once")
        assertTrue(launcher.opened.single().url.isNotBlank())
    }

    @Test
    fun a_launcher_that_refuses_yields_Failed_not_a_false_success() {
        // A launcher that refuses everything — the real platform's "no handler" case.
        val launcher = FakeUrlLauncher(canOpenPredicate = { false })
        val listing = StoreListing(webUrl = "https://example.com/app")

        val result = manager(listing, launcher).openStoreListing()

        assertIs<AppReviewResult.Failed>(result)
    }

    @Test
    fun requestReview_never_throws_on_any_target() = runTest {
        // The contract the whole module rests on: a review request is a value, never an exception,
        // whatever the platform does.
        val launcher = FakeUrlLauncher()
        val configured = manager(StoreListing(webUrl = "https://example.com/app"), launcher)
        val unconfigured = manager(StoreListing.None, launcher)

        assertTrue(configured.requestReview() is AppReviewResult)
        assertTrue(unconfigured.requestReview() is AppReviewResult)
    }

    @Test
    fun capabilities_are_self_consistent_on_this_target() {
        val caps = platformAppReviewCapabilities
        assertEquals(
            caps.nativeInAppReview || caps.storeListing,
            caps.canRequestReview,
            "canRequestReview must be the disjunction it documents",
        )
        // A target claiming a native prompt must also be able to reach its store — every platform
        // with an in-app API has a store behind it.
        if (caps.nativeInAppReview) assertTrue(caps.storeListing)
    }

    @Test
    fun the_commonMain_object_delegates_to_whatever_was_configured() = runTest {
        val fake = FakeAppReviewManager()
        AppReview.configure(fake)

        AppReview.requestReview()
        AppReview.openStoreListing()

        assertEquals(1, fake.requestCount)
        assertEquals(1, fake.storeOpenCount)
        assertEquals(listOf("request", "store"), fake.calls)
    }

    @Test
    fun configure_with_a_listing_means_the_not_configured_branch_can_never_fire() = runTest {
        // The SoT promise, stated in the only form that is true on EVERY target: once a listing is
        // configured, `requestReview()` must never come back with NoStoreConfigured.
        //
        // It deliberately does NOT assert StoreOpened. This test first failed on js/node for a
        // legitimate reason — `configure(listing)` uses the real UrlLauncher, and headless Node has
        // no browser to open, so Failed is the correct answer there. Asserting "opened" would have
        // encoded a browser assumption into a test that runs on 21 targets.
        AppReview.configure(StoreListing(webUrl = "https://example.com/app"))

        val result = AppReview.requestReview()

        assertTrue(
            result !is AppReviewResult.NoStoreConfigured,
            "a configured listing must not report NoStoreConfigured, got $result",
        )
    }

    @Test
    fun reset_restores_the_unconfigured_default() {
        AppReview.configure(StoreListing(webUrl = "https://example.com/app"))
        AppReview.reset()

        assertIs<AppReviewResult.NoStoreConfigured>(AppReview.openStoreListing())
    }
}
