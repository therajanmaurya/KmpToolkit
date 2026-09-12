/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Pins the bound on the Apple HTML→PDF route.
 *
 * `WKWebView` completes through `WKNavigationDelegate`, and it only navigates inside a live
 * `UIApplication`/window context — which the headless Kotlin/Native test binary does not provide, so
 * here neither `didFinishNavigation` nor `didFailNavigation` ever fires. That makes this harness an
 * exact reproduction of the production hazard: an engine that never calls back.
 *
 * Before [PdfGeneratorOptions.renderTimeout] existed, the actual awaited a bare `CompletableDeferred`
 * and this call never returned. It surfaced as `PdfManagerTest` failing with
 * `UncompletedCoroutinesError: After waiting for 1m, the test body did not run to completion` the
 * first time the iOS suite was executed in CI — a hang in the library, reported as a test timeout.
 *
 * A caller must get the documented typed [PdfResult.Failure] instead.
 */
class PdfRenderTimeoutTest {

    @Test
    fun an_engine_that_never_calls_back_fails_with_RenderTimeout_rather_than_hanging() = runTest {
        val result =
            PdfGenerator().generateFromHtml(
                html = "<html><body><h1>never navigates in a headless host</h1></body></html>",
                output = PdfOutput.ByteArrayOutput,
                options = PdfGeneratorOptions(renderTimeout = 1.seconds),
            )

        val failure = assertIs<PdfResult.Failure>(result, "a stuck render must fail, not hang")
        val timeout = assertIs<PdfError.RenderTimeout>(
            failure.error,
            "must be the typed timeout, not EngineFailure/CancellationError — the engine reported " +
                "no problem, it simply never called back",
        )
        assertTrue(timeout.timeout == 1.seconds, "the error carries the bound that was exceeded")
        assertTrue(
            !timeout.message.isNullOrBlank(),
            "PdfError contract: a failure explains itself",
        )
    }

    @Test
    fun the_timeout_is_configurable_and_must_be_positive() {
        // Guards the `require` — a zero or negative bound would make every render fail instantly,
        // which is a far more confusing symptom than the hang this replaced.
        assertTrue(PdfGeneratorOptions().renderTimeout == 60.seconds, "documented default")
        for (bad in listOf(0.seconds, (-1).seconds)) {
            val e = runCatching { PdfGeneratorOptions(renderTimeout = bad) }.exceptionOrNull()
            assertIs<IllegalArgumentException>(e, "renderTimeout=$bad must be rejected")
        }
    }
}
