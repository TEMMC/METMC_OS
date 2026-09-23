package com.metmc.os.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * METMC OS self-update client.
 *
 * Release workflow publishes metmc-update.json and a signed APK to the stable
 * metmc-os-latest GitHub release. The source APK is never replaced in-place;
 * Android's package installer performs the upgrade.
 */
object MetmcSelfUpdater {
    private const val UPDATE_URL =
        "https://github.com/TEMMC/METMC_OS/releases/download/metmc-os-latest/metmc-update.json"
    private val checking = AtomicBoolean(false)

    fun check(activity: Activity) {
        if (!checking.compareAndSet(false, true)) return
        Thread({
            try {
                val update = fetchUpdate() ?: return@Thread
                val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                val installedCode = if (Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                val remoteCode = update.optLong("versionCode", installedCode)
                if (remoteCode <= installedCode) return@Thread

                val apkUrl = update.optString("apkUrl").takeIf { it.isNotBlank() } ?: return@Thread
                val expectedSha = update.optString("sha256").lowercase().takeIf { it.matches(Regex("[0-9a-f]{64}")) }
                    ?: return@Thread
                val apk = downloadApk(apkUrl, expectedSha) ?: return@Thread

                activity.runOnUiThread {
                    install(activity, apk)
                }
            } catch (_: Throwable) {
                // Updates are optional. A network or release error must never
                // interfere with the normal METMC OS desktop.
            } finally {
                checking.set(false)
            }
        }, "metmc-self-updater").start()
    }

    private fun fetchUpdate(): JSONObject? {
        val connection = (URL(UPDATE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadApk(url: String, expectedSha: String): File? {
        val directory = File.createTempFile("metmc-update-", ".dir").apply {
            delete()
            mkdirs()
        }
        val part = File(directory, "metmc-os.apk.part")
        val apk = File(directory, "metmc-os.apk")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 30000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            val digest = MessageDigest.getInstance("SHA-256")
            part.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(expectedSha, ignoreCase = true)) {
                part.delete()
                return null
            }
            if (!part.renameTo(apk)) return null
            apk
        } finally {
            connection.disconnect()
        }
    }

    private fun install(activity: Activity, apk: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            activity.packageName + ".update-files",
            apk
        )
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + activity.packageName)
            ))
            return
        }
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            type = "application/vnd.android.package-archive"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        activity.startActivity(intent)
    }
}
