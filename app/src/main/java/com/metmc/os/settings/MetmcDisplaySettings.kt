package com.metmc.os.settings

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo

object MetmcDisplaySettings {

    private const val PREFS = "metmc_settings"
    private const val PORTRAIT = "metmc_portrait"

    fun isPortraitEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(PORTRAIT, false)
    }

    fun setPortraitEnabled(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PORTRAIT, enabled)
            .apply()

        if (context is Activity) {
            applyOrientation(context)
        }
    }

    fun applyOrientation(activity: Activity) {
        activity.requestedOrientation =
            if (isPortraitEnabled(activity)) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
    }

    fun resetToDesktopOrientation(context: Context) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PORTRAIT, false)
            .apply()

        if (context is Activity) {
            activityOrientation(context, false)
        }
    }

    private fun activityOrientation(activity: Activity, portrait: Boolean) {
        activity.requestedOrientation =
            if (portrait) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
    }
}
