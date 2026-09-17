package com.metmc.os.settings

import android.content.Context

object MetmcSettingsStore {
    private const val PREF = "metmc_settings"
    const val SETTINGS_CHANGED = "com.metmc.os.SETTINGS_CHANGED"

    private fun p(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    @JvmStatic
    fun getLockType(context: Context): String =
        p(context).getString("lock_type", "password") ?: "password"

    @JvmStatic
    fun getCredential(context: Context): String =
        p(context).getString("credential", "metmc") ?: "metmc"

    @JvmStatic
    fun setLock(context: Context, type: String, credential: String) {
        p(context).edit()
            .putString("lock_type", type)
            .putString("credential", credential)
            .apply()
    }

    @JvmStatic
    fun isLocked(context: Context): Boolean =
        getLockType(context) != "none"

    @JvmStatic
    fun getTheme(context: Context): String =
        p(context).getString("theme", "dark") ?: "dark"

    @JvmStatic
    fun setTheme(context: Context, theme: String) {
        p(context).edit().putString("theme", theme).apply()
    }

    @JvmStatic
    fun getWallpaper(context: Context): String =
        p(context).getString("wallpaper", "default") ?: "default"

    @JvmStatic
    fun setWallpaper(context: Context, wallpaper: String) {
        p(context).edit().putString("wallpaper", wallpaper).apply()
    }

    @JvmStatic
    fun getBoolean(context: Context, key: String, default: Boolean): Boolean =
        p(context).getBoolean(key, default)

    @JvmStatic
    fun setBoolean(context: Context, key: String, value: Boolean) {
        p(context).edit().putBoolean(key, value).apply()
    }
}
