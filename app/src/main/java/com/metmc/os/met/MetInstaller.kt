package com.metmc.os.met

import android.app.Activity
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

// .met package format -- created by Tinotenda Enock Mapfumo (Dr TEMMC) for METMC OS.
// Supported manifest platform keys: "metmc_os" (native METMC OS app, installed as APK
// and immediately usable inside the unified desktop), "android" (generic APK),
// "linux" (script/bundle run via chroot), "windows", "macos" (handled by met-install.py).
object MetInstaller {

    private const val ROOTFS = "/data/local/linux/rootfs"

    fun install(activity: Activity, metFile: File, onDone: (success: Boolean, message: String) -> Unit) {
        Thread {
            try {
                val workDir = File(activity.cacheDir, "met_install_${System.currentTimeMillis()}")
                workDir.mkdirs()

                unzip(metFile, workDir)

                val manifestFile = File(workDir, "manifest.json")
                if (!manifestFile.exists()) {
                    finish(activity, onDone, false, "Invalid .met package: no manifest.json found")
                    return@Thread
                }

                val manifest = JSONObject(manifestFile.readText())
                val name = manifest.optString("name", metFile.name)
                val author = manifest.optString("author", "Unknown")
                val platforms = manifest.optJSONObject("platforms")

                if (platforms == null) {
                    finish(activity, onDone, false, "Invalid .met package: no platforms defined")
                    return@Thread
                }

                var installedSomething = false
                val results = StringBuilder()

                // "metmc_os" takes priority -- it's a native METMC OS app.
                val androidKey = when {
                    platforms.has("metmc_os") -> "metmc_os"
                    platforms.has("android") -> "android"
                    else -> null
                }

                if (androidKey != null) {
                    val block = platforms.getJSONObject(androidKey)
                    val entry = block.optString("entry")
                    val apkFile = File(workDir, entry)

                    if (apkFile.exists()) {
                        val result = installApk(apkFile)
                        val label = if (androidKey == "metmc_os") "METMC OS" else "Android"
                        results.append("$label: $result\n")
                        if (result.startsWith("OK")) installedSomething = true
                    } else {
                        results.append("$androidKey: entry file not found ($entry)\n")
                    }
                }

                if (platforms.has("linux")) {
                    val linux = platforms.getJSONObject("linux")
                    val entry = linux.optString("entry")
                    val scriptFile = File(workDir, entry)

                    if (scriptFile.exists()) {
                        val result = installLinuxScript(name, workDir, entry)
                        results.append("Linux: $result\n")
                        if (result.startsWith("OK")) installedSomething = true
                    } else {
                        results.append("Linux: entry file not found ($entry)\n")
                    }
                }

                workDir.deleteRecursively()

                finish(
                    activity, onDone,
                    installedSomething,
                    "Installed \"$name\" by $author:\n\n${results.toString().trim()}"
                )

            } catch (e: Exception) {
                finish(activity, onDone, false, "Install failed: ${e.message}")
            }
        }.start()
    }

    private fun installApk(apkFile: File): String {
        return try {
            apkFile.setReadable(true, false)

            val process = ProcessBuilder(
                "su", "-c", "pm install -r ${quote(apkFile.absolutePath)}"
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (output.contains("Success")) "OK - installed" else "FAILED - $output"
        } catch (e: Exception) {
            "FAILED - ${e.message}"
        }
    }

    private fun installLinuxScript(appName: String, workDir: File, entry: String): String {
        return try {
            val safeName = appName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetDir = "/opt/met-apps/$safeName"

            val copyCmd = "mkdir -p $targetDir && cp -r ${quote(workDir.absolutePath)}/linux/. $targetDir/"
            val installCmd = "cd $targetDir && chmod +x ${quote(File(entry).name)} && ./${quote(File(entry).name)}"

            val process = ProcessBuilder(
                "su", "-c",
                "chroot ${quote(ROOTFS)} /bin/bash -c ${quote(copyCmd)}"
            ).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readText()
            process.waitFor()

            val process2 = ProcessBuilder(
                "su", "-c",
                "chroot ${quote(ROOTFS)} /bin/bash -c ${quote(installCmd)}"
            ).redirectErrorStream(true).start()
            val out2 = process2.inputStream.bufferedReader().readText()
            val code = process2.waitFor()

            if (code == 0) "OK - installed to $targetDir" else "FAILED (exit $code) - $out2"
        } catch (e: Exception) {
            "FAILED - ${e.message}"
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    private fun finish(
        activity: Activity,
        onDone: (Boolean, String) -> Unit,
        success: Boolean,
        message: String
    ) {
        activity.runOnUiThread {
            onDone(success, message)
        }
    }

    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"
}
