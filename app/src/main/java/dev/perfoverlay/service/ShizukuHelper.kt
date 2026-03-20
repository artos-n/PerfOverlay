package dev.perfoverlay.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Process
import dev.rikka.shizuku.Shizuku
import dev.rikka.shizuku.ShizukuBinderWrapper
import dev.rikka.shizuku.ShizukuProvider
import dev.rikka.shizuku.Shizuku.UserServiceArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shizuku integration for PerfOverlay.
 *
 * With Shizuku running (via ADB or root), PerfOverlay can:
 * 1. Grant SYSTEM_ALERT_WINDOW permission automatically via appops
 * 2. Eventually create overlay windows directly using shell privileges
 *
 * Shizuku states:
 * - NOT_INSTALLED: Shizuku app not found
 * - NOT_RUNNING: Shizuku installed but daemon not started
 * - RUNNING: Shizuku active, ready to use
 * - PERMISSION_DENIED: Running but user denied permission to PerfOverlay
 */
object ShizukuHelper {

    enum class State {
        NOT_INSTALLED,
        NOT_RUNNING,
        RUNNING,
        PERMISSION_DENIED
    }

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE_PERMISSION = 1001
    private const val USER_SERVICE_TAG = "PerfOverlayShizukuService"
    private const val USER_SERVICE_VERSION = 2

    private val _state = MutableStateFlow(State.NOT_INSTALLED)
    val state: StateFlow<State> = _state

    private var binderReceivedListener: Shizuku.OnBinderReceivedListener? = null
    private var binderDeadListener: Shizuku.OnBinderDeadListener? = null
    private var permissionResultListener: Shizuku.OnRequestPermissionResultListener? = null

    private var overlayPermissionGrantedCallback: ((Boolean) -> Unit)? = null

    /**
     * Initialize Shizuku listeners. Call from Application.onCreate() or Activity.onCreate().
     */
    fun init(context: Context) {
        if (!isShizukuInstalled(context)) {
            _state.value = State.NOT_INSTALLED
            return
        }

        // Try Sui first (root-based, faster init)
        val suiAvailable = Sui.init(context.packageName)
        if (suiAvailable) {
            updatePermissionState()
            return
        }

        // Shizuku binder listeners
        binderReceivedListener = Shizuku.OnBinderReceivedListener {
            updatePermissionState()
        }

        binderDeadListener = Shizuku.OnBinderDeadListener {
            _state.value = State.NOT_RUNNING
        }

        permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_PERMISSION) {
                updatePermissionState()
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                overlayPermissionGrantedCallback?.invoke(granted)
                overlayPermissionGrantedCallback = null
            }
        }

        Shizuku.addBinderReceivedListener(binderReceivedListener!!)
        Shizuku.addBinderDeadListener(binderDeadListener!!)
        Shizuku.addRequestPermissionResultListener(permissionResultListener!!)

        // Shizuku might already be running
        if (Shizuku.pingBinder()) {
            updatePermissionState()
        }
    }

    /**
     * Clean up listeners. Call from onDestroy().
     */
    fun destroy() {
        binderReceivedListener?.let { Shizuku.removeBinderReceivedListener(it) }
        binderDeadListener?.let { Shizuku.removeBinderDeadListener(it) }
        permissionResultListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
        binderReceivedListener = null
        binderDeadListener = null
        permissionResultListener = null
    }

    /**
     * Request Shizuku permission from the user. Shows the system permission dialog.
     */
    fun requestPermission(callback: (Boolean) -> Unit) {
        if (!Shizuku.pingBinder()) {
            callback(false)
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            callback(true)
            return
        }

        if (Shizuku.shouldShowRequestPermissionRationale()) {
            // User chose "Deny and don't ask again"
            callback(false)
            return
        }

        overlayPermissionGrantedCallback = callback
        Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
    }

    /**
     * Grant SYSTEM_ALERT_WINDOW permission via appops using Shizuku shell privileges.
     * This is the main use case — no need for user to manually go to Settings.
     */
    fun grantOverlayPermission(callback: (Boolean) -> Unit) {
        if (!Shizuku.pingBinder()) {
            callback(false)
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            // Need permission first, then retry
            requestPermission { granted ->
                if (granted) {
                    grantOverlayPermission(callback)
                } else {
                    callback(false)
                }
            }
            return
        }

        try {
            val serviceArgs = UserServiceArgs(
                android.content.ComponentName(
                    PerfOverlayApp.instance,
                    ShizukuOverlayGrantService::class.java.name
                )
            )
                .daemon(false)
                .processNameSuffix("shizuku_grant")
                .tag(USER_SERVICE_TAG)
                .version(USER_SERVICE_VERSION)

            Shizuku.bindUserService(serviceArgs, object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName, service: IBinder) {
                    try {
                        val grantService = IShizukuGrantService.Stub.asInterface(service)
                        val result = grantService.grantOverlayPermission()
                        callback(result)
                    } catch (e: Exception) {
                        callback(false)
                    } finally {
                        try {
                            Shizuku.unbindUserService(serviceArgs)
                        } catch (_: Exception) {}
                    }
                }

                override fun onServiceDisconnected(name: android.content.ComponentName) {
                    callback(false)
                }
            })
        } catch (e: Exception) {
            callback(false)
        }
    }

    /**
     * Check if Shizuku is installed on the device.
     */
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun updatePermissionState() {
        if (!Shizuku.pingBinder()) {
            _state.value = State.NOT_RUNNING
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            _state.value = State.RUNNING
        } else {
            _state.value = State.PERMISSION_DENIED
        }
    }
}

// Sui is auto-initialized via ShizukuProvider, but we need this reference
private object Sui {
    fun init(packageName: String): Boolean {
        return try {
            dev.rikka.shizuku.ShizukuProvider.enableMultiProcessSupport()
            false // Shizuku handles this internally now
        } catch (e: Exception) {
            false
        }
    }
}
