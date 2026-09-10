package com.mobilebytelabs.kmptoolkit.appupdate

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.WatchKit.WKExtension

/**
 * watchOS implementation of AppUpdate.
 *
 * In-app updates are not supported on watchOS as the platform
 * handles app updates through the paired iPhone or automatically.
 *
 * @since 0.5.0
 */
actual object AppUpdate {
    /**
     * Returns NotSupported as watchOS doesn't support in-app updates.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        // Check if watchOS is disabled in config
        if (!config.watchosEnabled) {
            return UpdateResult.NotSupported("watchOS updates disabled in configuration")
        }

        return UpdateResult.NotSupported(
            "In-app updates are not supported on watchOS. " +
                "Updates are handled through the paired iPhone or automatically.",
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
     * Returns NotSupported as watchOS doesn't support in-app updates.
     */
    actual suspend fun startUpdate(updateType: UpdateType, config: AppUpdateConfig): UpdateResult {
        // Check if watchOS is disabled in config
        if (!config.watchosEnabled) {
            return UpdateResult.NotSupported("watchOS updates disabled in configuration")
        }

        return UpdateResult.NotSupported(
            "In-app updates are not supported on watchOS.",
        )
    }

    /**
     * Hand this app's App Store page to the paired iPhone.
     *
     * watchOS cannot show the App Store itself, but `WKExtension.openSystemURL` routes an
     * `https://` link to the paired phone, which opens it — so the user still lands on the right
     * page. Returning `false` unconditionally, as this used to, gave an app that had detected an
     * update nowhere to send them.
     *
     * A watchOS app shares its iOS App Store listing, so [AppUpdateConfig.iosAppStoreId] is the id.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean {
        val appId = config.iosAppStoreId?.takeIf { it.isNotBlank() } ?: return false
        val url = NSURL.URLWithString("https://apps.apple.com/app/id$appId") ?: return false
        WKExtension.sharedExtension().openSystemURL(url)
        return true
    }

    /**
     * In-app updates are not supported on watchOS.
     */
    actual fun isSupported(): Boolean = false
}
