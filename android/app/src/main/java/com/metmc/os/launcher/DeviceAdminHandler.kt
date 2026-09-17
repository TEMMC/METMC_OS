package com.metmc.os.launcher

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device admin receiver for kiosk mode capabilities.
 *
 * When METMC OS is set as Device Owner via:
 *   adb shell dpm set-device-owner com.metmc/.launcher.DeviceAdminHandler
 *
 * It can:
 * - Disable the lockscreen
 * - Pin the app (lock task mode)
 * - Prevent user from leaving METMC OS without the escape hatch
 */
class DeviceAdminHandler : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "METMC OS.DeviceAdmin"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device admin enabled — METMC OS has kiosk capabilities")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.w(TAG, "Device admin disabled")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        Log.i(TAG, "Entering lock task mode: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        Log.i(TAG, "Exiting lock task mode")
    }
}
