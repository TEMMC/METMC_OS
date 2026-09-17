package com.metmc.os.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.metmc.os.settings.HomeRoleManager
import com.metmc.os.settings.METMCPreferences

/**
 * Observes boot without forcing METMC OS over the user's selected Android Home app.
 *
 * Handles:
 * - BOOT_COMPLETED (normal boot)
 * - LOCKED_BOOT_COMPLETED (direct boot aware)
 * - QUICKBOOT_POWERON (HTC, Xiaomi quick boot)
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "METMC OS.Boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Boot event received: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                val mode = METMCPreferences.operatingMode(context)
                val isHome = HomeRoleManager.isDefaultHome(context)
                Log.i(TAG, "Boot mode=$mode, defaultHome=$isHome")

                // Android itself launches KioskActivity when METMC OS owns the
                // Home role. In desktop-app mode we intentionally do nothing.
                // This keeps Linux opt-in and avoids starting it over Android.
            }
        }
    }
}
