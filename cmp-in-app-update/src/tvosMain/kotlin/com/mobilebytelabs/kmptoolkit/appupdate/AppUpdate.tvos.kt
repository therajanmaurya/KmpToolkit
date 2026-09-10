package com.mobilebytelabs.kmptoolkit.appupdate

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * tvOS implementation of AppUpdate.
 *
 * In-app updates are not supported on tvOS as the platform
 * handles app updates through the tvOS App Store automatically.
 *
 * @since 0.5.0
 */
actual object AppUpdate {
    /**
     * Returns NotSupported as tvOS doesn't support in-app updates.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        // Check if tvOS is disabled in config
        if (!config.tvosEnabled) {
            return UpdateResult.NotSupported("tvOS updates disabled in configuration")
        }

        return UpdateResult.NotSupported(
            "In-app updates are not supported on tvOS. " +
                "Updates are handled automatically by the tvOS App Store.",
        )
    }

    /**
     * Gets the currently installed app version from Info.plist.
     */
    actual fun getCurrentVersion(): AppVersion {
        val bundle = NSBundle.mainBundle
        val versionString = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val buildString = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String

        val versionName = versionString ?: "0.0.0"
        val versionCode = buildString?.toLongOrNull()

        return AppVersion.parse(versionName)?.copy(versionCode = versionCode)
            ?: AppVersion.UNKNOWN.copy(versionName = versionName, versionCode = versionCode)
    }

    /**
     * Returns NotSupported as tvOS doesn't support in-app updates.
     */
    actual suspend fun startUpdate(updateType: UpdateType, config: AppUpdateConfig): UpdateResult {
        // Check if tvOS is disabled in config
        if (!config.tvosEnabled) {
            return UpdateResult.NotSupported("tvOS updates disabled in configuration")
        }

        return UpdateResult.NotSupported(
            "In-app updates are not supported on tvOS.",
        )
    }

    /**
     * Open this app's App Store page.
     *
     * tvOS has no Play-Core-style in-app update flow, but it does have an App Store and
     * `UIApplication.openURL` works there — so "we found a newer version, take me to the store"
     * is perfectly serviceable. This returned `false` unconditionally before, which left an app
     * that had already detected an update with nowhere to send the user.
     *
     * A tvOS app shares its iOS App Store listing, so [AppUpdateConfig.iosAppStoreId] is the id.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean {
        val appId = config.iosAppStoreId?.takeIf { it.isNotBlank() } ?: return false
        val url = NSURL.URLWithString("https://apps.apple.com/app/id$appId") ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(url)) return false
        app.openURL(url, emptyMap<Any?, Any?>(), null)
        return true
    }

    /**
     * In-app updates are not supported on tvOS.
     */
    actual fun isSupported(): Boolean = false
}
