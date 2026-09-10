/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [ClipboardMonitor] over [InAppClipboard], for the targets with no system clipboard to poll —
 * tvOS, watchOS and wasmWasi.
 *
 * ## Why this replaced a no-op
 * All three shipped a monitor whose `start()` did nothing, leaving [state] on
 * [ClipboardMonitorState.Idle] forever. The shared monitor tests in `commonTest` assert the
 * documented state machine, so those targets were failing them — silently, because no CI job runs
 * their test tasks.
 *
 * The fix is not to weaken the tests. A monitor needs a clipboard to watch, and now there is one:
 * [InAppClipboard] notifies on every copy, so this observes real changes rather than polling
 * something that cannot change. Being event-driven it also has no timer, which makes
 * [ClipboardMonitorConfig.pollingIntervalMs] irrelevant here rather than ignored-in-silence.
 */
public class InAppClipboardMonitor : ClipboardMonitor {

    private val _state = MutableStateFlow<ClipboardMonitorState>(ClipboardMonitorState.Idle)
    override val state: StateFlow<ClipboardMonitorState> = _state.asStateFlow()

    private val _changes = MutableSharedFlow<ClipboardChange>(extraBufferCapacity = 64)
    override val changes: SharedFlow<ClipboardChange> = _changes.asSharedFlow()

    private val _latestChange = MutableStateFlow<ClipboardChange?>(null)
    override val latestChange: StateFlow<ClipboardChange?> = _latestChange.asStateFlow()

    private val _urlDetections = MutableSharedFlow<UrlDetection>(extraBufferCapacity = 64)
    override val urlDetections: SharedFlow<UrlDetection> = _urlDetections.asSharedFlow()

    private val matchers: MutableList<ClipboardUrlMatcher> = mutableListOf()
    private val filters: MutableList<ClipboardFilter> = mutableListOf()

    private var previous: String? = null
    private val listener: (String) -> Unit = { text -> onCopy(text) }

    override fun start(config: ClipboardMonitorConfig) {
        // Idempotent: starting twice must not register a second listener and double every event.
        if (_state.value is ClipboardMonitorState.Monitoring) return
        InAppClipboard.removeListener(listener)
        InAppClipboard.addListener(listener)
        _state.value = ClipboardMonitorState.Monitoring
    }

    override fun stop() {
        InAppClipboard.removeListener(listener)
        previous = null
        _state.value = ClipboardMonitorState.Idle
    }

    override fun pause() {
        // Only a running monitor can pause; pausing an idle one would strand it in Paused with
        // no way back that makes sense.
        if (_state.value is ClipboardMonitorState.Monitoring) {
            _state.value = ClipboardMonitorState.Paused
        }
    }

    override fun resume() {
        if (_state.value is ClipboardMonitorState.Paused) {
            _state.value = ClipboardMonitorState.Monitoring
        }
    }

    override fun addUrlMatcher(matcher: ClipboardUrlMatcher) {
        matchers += matcher
    }

    override fun addFilter(filter: ClipboardFilter) {
        filters += filter
    }

    private fun onCopy(text: String) {
        if (_state.value !is ClipboardMonitorState.Monitoring) return
        if (text == previous) return
        if (filters.any { !it.shouldProcess(text) }) return

        val change = ClipboardChange(
            content = text,
            timestamp = 0L,
            previousContent = previous,
        )
        previous = text
        _latestChange.value = change
        _changes.tryEmit(change)

        matchers.forEach { matcher ->
            if (matcher.matches(text)) {
                // Report the URL the matcher found, not the whole clipboard text — the content
                // may carry surrounding prose.
                val url = matcher.extractUrl(text) ?: text
                _urlDetections.tryEmit(UrlDetection(url, matcher, change))
            }
        }
    }
}
