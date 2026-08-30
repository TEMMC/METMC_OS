package com.metmc.os.desktop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class DesktopAppMenu(
    private val context: Context,
    private val onLaunchWindow: (String, View) -> Unit
) {

    private val rootfs = "/data/local/linux/rootfs"

    fun showAndroidApps() {

        val activity = context as? Activity ?: return

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(10), dp(10), dp(10), dp(10))

        val title = TextView(context)
        title.text = "Android Applications"
        title.textSize = 20f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(8), dp(6), dp(8), dp(12))
        root.addView(title)

        val scroll = ScrollView(context)

        val apps = LinearLayout(context)
        apps.orientation = LinearLayout.VERTICAL

        val pm = context.packageManager

        val installed = pm.getInstalledApplications(
            PackageManager.GET_META_DATA
        ).sortedBy {
            pm.getApplicationLabel(it)
                .toString()
                .lowercase()
        }

        var count = 0

        installed.forEach { appInfo ->

            val packageName = appInfo.packageName

            if (packageName == context.packageName) {
                return@forEach
            }

            val launchIntent =
                pm.getLaunchIntentForPackage(packageName)
                    ?: return@forEach

            val label =
                pm.getApplicationLabel(appInfo)
                    .toString()
                    .trim()

            if (label.isEmpty()) {
                return@forEach
            }

            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(
                dp(8), dp(6),
                dp(8), dp(6)
            )

            val icon = ImageView(context)
            icon.setImageDrawable(appInfo.loadIcon(pm))

            row.addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(42), dp(42)
                )
            )

            val name = TextView(context)
            name.text = label
            name.textSize = 16f
            name.setTextColor(Color.WHITE)
            name.setPadding(dp(12), 0, dp(8), 0)
            name.gravity = Gravity.CENTER_VERTICAL

            row.addView(
                name,
                LinearLayout.LayoutParams(
                    0, dp(54), 1f
                )
            )

            row.setOnClickListener {
                try {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                    context.startActivity(launchIntent)
                } catch (_: Exception) {
                }
            }

            apps.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(58)
                )
            )

            count++
        }

        if (count == 0) {
            val empty = TextView(context)
            empty.text = "No launchable Android applications found."
            empty.setTextColor(Color.LTGRAY)
            empty.setPadding(dp(12), dp(20), dp(12), dp(20))
            apps.addView(empty)
        }

        scroll.addView(
            apps,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        style(root)

        onLaunchWindow(
            "Android Applications",
            root
        )
    }

    fun showLinuxApps() {

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(12), dp(12), dp(12), dp(12))

        val title = TextView(context)
        title.text = "Linux Applications"
        title.textSize = 20f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(8), dp(6), dp(8), dp(12))
        root.addView(title)

        val scroll = ScrollView(context)

        val apps = LinearLayout(context)
        apps.orientation = LinearLayout.VERTICAL

        addLinuxButton(
            apps,
            "Terminal"
        ) {
            context.startActivity(
                Intent(
                    context,
                    com.metmc.os.linux.LinuxTerminalActivity::class.java
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
        }

        val loading = TextView(context)
        loading.text = "Scanning Linux applications..."
        loading.setTextColor(Color.LTGRAY)
        loading.setPadding(dp(14), dp(12), dp(14), dp(12))
        apps.addView(loading)

        scroll.addView(
            apps,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        style(root)

        onLaunchWindow(
            "Linux Applications",
            root
        )

        Thread {

            val found = linkedSetOf<String>()

            val command =
                "for d in /usr/share/applications /usr/local/share/applications; do " +
                "[ -d \"\$d\" ] || continue; " +
                "find \"\$d\" -maxdepth 1 -type f -name '*.desktop' 2>/dev/null; " +
                "done"

            try {

                val process = ProcessBuilder(
                    "/debug_ramdisk/su",
                    "-c",
                    "chroot $rootfs /bin/bash -c ${quote(command)}"
                )
                    .redirectErrorStream(true)
                    .start()

                process.inputStream.bufferedReader().useLines { lines ->

                    lines.forEach { desktopPath ->

                        try {

                            val file = java.io.File(
                                rootfs + desktopPath
                            )

                            if (!file.exists()) {
                                return@forEach
                            }

                            var name: String? = null
                            var exec: String? = null
                            var terminal = false

                            file.forEachLine { line ->

                                when {

                                    line.startsWith("Name=") &&
                                    name == null -> {
                                        name =
                                            line.substringAfter("Name=")
                                    }

                                    line.startsWith("Exec=") &&
                                    exec == null -> {
                                        exec =
                                            line.substringAfter("Exec=")
                                    }

                                    line == "Terminal=true" -> {
                                        terminal = true
                                    }
                                }
                            }

                            val appName =
                                name?.trim()

                            val appExec =
                                exec
                                    ?.trim()
                                    ?.replace(
                                        Regex("\\s+%[fFuUdDnNickvm]"),
                                        ""
                                    )
                                    ?.trim()

                            if (
                                !appName.isNullOrEmpty() &&
                                !appExec.isNullOrEmpty()
                            ) {

                                found.add(
                                    appName + "\u0001" +
                                    appExec + "\u0001" +
                                    terminal
                                )
                            }

                        } catch (_: Exception) {
                        }
                    }
                }

            } catch (_: Exception) {
            }

            context.mainExecutor.execute {

                apps.removeView(loading)

                if (found.isEmpty()) {

                    val empty = TextView(context)
                    empty.text =
                        "No Linux desktop applications found.\n" +
                        "Install Linux applications inside the METMC Linux environment."
                    empty.setTextColor(Color.LTGRAY)
                    empty.setPadding(
                        dp(14), dp(12),
                        dp(14), dp(12)
                    )

                    apps.addView(empty)

                } else {

                    found
                        .sortedBy {
                            it.substringBefore("\u0001")
                                .lowercase()
                        }
                        .forEach { entry ->

                            val parts =
                                entry.split(
                                    "\u0001",
                                    limit = 3
                                )

                            val appName = parts[0]
                            val appExec = parts[1]
                            val terminal =
                                parts.getOrNull(2) == "true"

                            addLinuxButton(
                                apps,
                                appName
                            ) {

                                launchLinuxApp(
                                    appExec,
                                    terminal
                                )
                            }
                        }
                }
            }

        }.start()
    }

    private fun launchLinuxApp(
        command: String,
        terminal: Boolean
    ) {

        if (terminal) {

            val intent = Intent(
                context,
                com.metmc.os.linux.LinuxTerminalActivity::class.java
            )

            intent.putExtra(
                "command",
                command
            )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            context.startActivity(intent)

            return
        }

        try {

            val shellCommand =
                "export HOME=/root; " +
                "export USER=root; " +
                "export DISPLAY=:0; " +
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                command

            ProcessBuilder(
                "/debug_ramdisk/su",
                "-c",
                "chroot $rootfs /bin/bash -c ${quote(shellCommand)}"
            )
                .redirectErrorStream(true)
                .start()

        } catch (_: Exception) {
        }
    }

    private fun addLinuxButton(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val button = LinearLayout(context)
        button.orientation = LinearLayout.HORIZONTAL
        button.gravity = Gravity.CENTER_VERTICAL
        button.setPadding(dp(14), 0, dp(14), 0)

        val icon = TextView(context)
        icon.text = "▣"
        icon.textSize = 24f
        icon.setTextColor(Color.WHITE)
        icon.gravity = Gravity.CENTER

        button.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(48),
                dp(56)
            )
        )

        val label = TextView(context)
        label.text = text
        label.textSize = 16f
        label.setTextColor(Color.WHITE)
        label.gravity = Gravity.CENTER_VERTICAL
        label.setPadding(dp(10), 0, 0, 0)

        button.addView(
            label,
            LinearLayout.LayoutParams(
                0,
                dp(56),
                1f
            )
        )

        button.setOnClickListener {
            action()
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )
    }

    private fun quote(
        value: String
    ): String {

        return "'" +
            value.replace(
                "'",
                "'\\''"
            ) +
            "'"
    }

    private fun style(view: View) {

        view.background =
            GradientDrawable().apply {
                setColor(
                    Color.rgb(25, 27, 34)
                )
                cornerRadius =
                    dp(10).toFloat()
            }
    }

    private fun dp(value: Int): Int {

        return (
            value *
                context.resources
                    .displayMetrics
                    .density
        ).toInt()
    }
}
