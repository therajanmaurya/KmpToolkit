/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Zero-config Android wiring for [AppReviewManager].
 *
 * Play's In-App Review needs BOTH an application [Context] (to build the `ReviewManager`) and the
 * **currently resumed [Activity]** (to launch the flow into). This module therefore combines the two
 * patterns already used elsewhere in the toolkit rather than inventing a third:
 *  - a [ContentProvider], which Android boots before `Application.onCreate`, captures the app
 *    context — as in `cmp-share`'s and `cmp-intent-launcher`'s init providers;
 *  - [Application.ActivityLifecycleCallbacks] tracks the foreground activity — as in `cmp-deep-link`.
 *
 * The activity is held in a [WeakReference] and cleared on pause. Holding it strongly would leak
 * every Activity the user ever visits, and this object lives for the whole process.
 */
internal object AppReviewContext {
    private var appContext: Context? = null
    private var activityRef: WeakReference<Activity>? = null

    /** Application context, or null if the init provider was stripped from the merged manifest. */
    val context: Context? get() = appContext

    /** The resumed Activity, or null when the app is backgrounded or it has been collected. */
    val activity: Activity? get() = activityRef?.get()

    fun attach(ctx: Context) {
        val app = ctx.applicationContext
        appContext = app
        (app as? Application)?.registerActivityLifecycleCallbacks(Callbacks)
    }

    private object Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            activityRef = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            // Cleared on pause so a launched review flow can never target a dead Activity.
            if (activityRef?.get() === activity) activityRef = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}

/** Declared in this module's `AndroidManifest.xml` with `android:exported="false"`. */
internal class AppReviewInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        AppReviewContext.attach(ctx)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0
}
