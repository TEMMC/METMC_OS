package com.metmc.os.wallpaper

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import java.io.File

object MetmcWallpaper {

    private const val PREFS = "metmc_wallpaper"
    private const val KEY_URI = "uri"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun set(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        prefs(context).edit()
            .putString(KEY_URI, uri.toString())
            .apply()

        true
    }

    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_URI)
            .apply()
    }

    fun exists(context: Context): Boolean {
        return !prefs(context).getString(KEY_URI, null).isNullOrBlank()
    }

    fun getUri(context: Context): Uri? {
        return prefs(context)
            .getString(KEY_URI, null)
            ?.let { Uri.parse(it) }
    }

    fun apply(context: Context, target: ImageView) {
        val uri = getUri(context)

        if (uri == null) {
            target.setImageDrawable(null)
            return
        }

        try {
            target.scaleType = ImageView.ScaleType.CENTER_CROP
            target.setImageURI(uri)
        } catch (_: Exception) {
            target.setImageDrawable(null)
        }
    }
}
