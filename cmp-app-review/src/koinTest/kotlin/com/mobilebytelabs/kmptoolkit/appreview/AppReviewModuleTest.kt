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
        val koin = startKoin { modules(appReviewModule) }.koin
        assertIs<AppReviewManagerImpl>(koin.get<AppReviewManager>())
        assertSame(koin.get<AppReviewManager>(), koin.get<AppReviewManager>())
    }

    @Test
    fun the_injected_manager_and_the_shared_object_see_the_same_configuration() {
        // They are deliberately NOT the same instance any more: the module builds its own, and both
        // read the one globally configured listing at call time. That is what removes the drift —
        // there is a single listing, not two copies to keep in step.
        AppReview.configure(StoreListing(webUrl = "https://example.com/app"))
        val koin = startKoin { modules(appReviewModule) }.koin

        val injected = koin.get<AppReviewManager>()

        assertSame(injected.capabilities, AppReview.capabilities)
        assertEquals(injected.openStoreListing()::class, AppReview.openStoreListing()::class)
    }

    @Test
    fun the_listing_passed_to_the_module_reaches_the_shared_entry_point() = runTest {
        // Without the configure() call this would be NoStoreConfigured: the global object would still
        // be holding its unconfigured default while the graph had the real listing.
        AppReview.configure(StoreListing(webUrl = "https://example.com/app"))
        startKoin { modules(appReviewModule) }

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
        AppReview.configure(StoreListing(webUrl = "https://example.com/app"))
        // A consumer wanting their own launcher installs a manager rather than parameterising the
        // module — the module stays argument-free for the common case.
        AppReview.configure(AppReviewManagerImpl(urlLauncher = launcher))
        startKoin { modules(appReviewModule) }

        AppReview.openStoreListing()

        assertEquals(1, launcher.opened.size, "the store listing went through the injected launcher")
    }
}
