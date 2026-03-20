package dev.perfoverlay.service

import android.content.Context
import dev.perfoverlay.PerfOverlayApp

/**
 * Shizuku UserService that runs with shell (ADB) or root privileges.
 * Used to grant SYSTEM_ALERT_WINDOW permission via appops without
 * the user having to navigate to Settings manually.
 *
 * Runs in a separate process with elevated privileges.
 */
class ShizukuOverlayGrantService : IShizukuGrantService.Stub {

    private val context: Context = PerfOverlayApp.instance

    /**
     * Grant overlay permission using appops.
     *
     * With shell (ADB) identity, we can call:
     *   appops set <package> SYSTEM_ALERT_WINDOW allow
     *
     * This is equivalent to the user toggling the overlay permission
     * in Settings, but done programmatically via Shizuku's privileges.
     */
    override fun grantOverlayPermission(): Boolean {
        return try {
            val packageName = context.packageName

            // Use AppOpsManager through shell identity
            // Runtime.exec runs as shell (uid 2000) or root (uid 0)
            // depending on how Shizuku was started
            val process = Runtime.getRuntime().exec(
                arrayOf("appops", "set", packageName, "SYSTEM_ALERT_WINDOW", "allow")
            )
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Called when Shizuku service is destroyed.
     * Per Shizuku docs, we should call System.exit() here.
     */
    fun destroy() {
        System.exit(0)
    }
}
