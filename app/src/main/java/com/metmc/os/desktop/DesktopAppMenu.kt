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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class DesktopAppMenu(
    private val context: Context,
    private val onLaunchWindow: (String, View) -> Unit
) {

    fun showAndroidApps() {

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(10), dp(10), dp(10), dp(10))

        val title = TextView(context)
        title.text = "Android Applications"
        title.textSize = 20f
        title.setTextColor(Color.WHITE)
        title.setPadding(dp(8), dp(6), dp(8), dp(12))

        root.addView(title)

        val settingsRow = createAppRow(
            context.resources.getDrawable(android.R.drawable.ic_menu_preferences, context.theme),
            "Settings"
        )

        settingsRow.setOnClickListener {
            try {
                context.startActivity(
                    Intent(
                        context,
                        com.metmc.os.settings.SettingsActivity::class.java
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
        }

        root.addView(
            settingsRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        val scroll = ScrollView(context)

        val apps = LinearLayout(context)
        apps.orientation = LinearLayout.VERTICAL

        val pm = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val activities =
            pm.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            ).sortedBy {
                it.loadLabel(pm).toString().lowercase()
            }

        val added = HashSet<String>()

        activities.forEach { resolveInfo ->

            val packageName =
                resolveInfo.activityInfo.packageName

            if (packageName == context.packageName) {
                return@forEach
            }

            if (!added.add(packageName)) {
                return@forEach
            }

            val label =
                resolveInfo.loadLabel(pm)
                    .toString()
                    .trim()

            if (label.isEmpty()) {
                return@forEach
            }

            val row =
                createAppRow(
                    resolveInfo.loadIcon(pm),
                    label
                )

            row.setOnClickListener {

                try {

                    val launchIntent =
                        Intent(Intent.ACTION_MAIN)

                    launchIntent.addCategory(
                        Intent.CATEGORY_LAUNCHER
                    )

                    launchIntent.setClassName(
                        packageName,
                        resolveInfo.activityInfo.name
                    )

                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )

                    context.startActivity(
                        launchIntent
                    )

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
            try {
                val intent = Intent(
                    context,
                    com.metmc.os.linux.LinuxTerminalActivity::class.java
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }

        val status = TextView(context)
        status.text = "Scanning Linux applications..."
        status.textSize = 16f
        status.setTextColor(Color.LTGRAY)
        status.setPadding(
            dp(14),
            dp(20),
            dp(14),
            dp(20)
        )

        apps.addView(status)

        Thread {

            try {

                val scanCommand =
                    "export HOME=/root; " +
                    "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                    "chroot /data/local/linux/rootfs /bin/bash -c " +
                    "\"find /usr/share/applications /usr/local/share/applications " +
                    "-type f -name '*.desktop' 2>/dev/null | while read f; do " +
                    "name=\\$(grep -m1 '^Name=' \\\"\\${'$'}f\\\" | cut -d= -f2-); " +
                    "exec=\\$(grep -m1 '^Exec=' \\\"\\${'$'}f\\\" | cut -d= -f2-); " +
                    "if [ -n \\\"\\${'$'}name\\\" ] && [ -n \\\"\\${'$'}exec\\\" ]; then " +
                    "printf '%s|%s\\\\n' \\\"\\${'$'}name\\\" \\\"\\${'$'}exec\\\"; " +
                    "fi; " +
                    "done\""

                val process =
                    Runtime.getRuntime().exec(
                        arrayOf(
                            "/debug_ramdisk/su",
                            "-c",
                            scanCommand
                        )
                    )

                val result =
                    process.inputStream
                        .bufferedReader()
                        .readLines()

                process.waitFor()

                val linuxApps =
                    result.mapNotNull { line ->

                        val index = line.indexOf("|")

                        if (index <= 0) {
                            null
                        } else {

                            val name =
                                line.substring(
                                    0,
                                    index
                                ).trim()

                            val exec =
                                line.substring(
                                    index + 1
                                )
                                .replace(
                                    Regex("\\s+%[a-zA-Z]"),
                                    ""
                                )
                                .trim()

                            if (
                                name.isBlank() ||
                                exec.isBlank()
                            ) {
                                null
                            } else {
                                Pair(name, exec)
                            }
                        }
                    }
                    .distinctBy {
                        it.first.lowercase()
                    }
                    .sortedBy {
                        it.first.lowercase()
                    }

                (context as? Activity)
                    ?.runOnUiThread {

                        apps.removeView(status)

                        linuxApps.forEach { app ->

                            addLinuxButton(
                                apps,
                                app.first
                            ) {

                                try {

                                    val intent =
                                        Intent(
                                            context,
                                            com.metmc.os.linux.LinuxDesktopActivity::class.java
                                        )

                                    intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                    )

                                    intent.putExtra(
                                        "METMC_LINUX_COMMAND",
                                        app.second
                                    )

                                    intent.putExtra(
                                        "METMC_APP_NAME",
                                        app.first
                                    )

                                    context.startActivity(intent)

                                } catch (_: Exception) {
                                }
                            }
                        }

                        if (linuxApps.isEmpty()) {

                            val empty =
                                TextView(context)

                            empty.text =
                                "No Linux desktop applications found."

                            empty.textSize = 16f
                            empty.setTextColor(Color.LTGRAY)

                            empty.setPadding(
                                dp(14),
                                dp(20),
                                dp(14),
                                dp(20)
                            )

                            apps.addView(empty)
                        }
                    }

            } catch (_: Exception) {

                (context as? Activity)
                    ?.runOnUiThread {

                        apps.removeView(status)

                        val error =
                            TextView(context)

                        error.text =
                            "Unable to scan the METMC Linux environment."

                        error.textSize = 16f
                        error.setTextColor(Color.LTGRAY)

                        error.setPadding(
                            dp(14),
                            dp(20),
                            dp(14),
                            dp(20)
                        )

                        apps.addView(error)
                    }
            }

        }.start()

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
    }

    private fun createAppRow(
        drawable: android.graphics.drawable.Drawable,
        text: String
    ): LinearLayout {

        val row = LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.setPadding(
            dp(8),
            dp(6),
            dp(8),
            dp(6)
        )

        val icon =
            ImageView(context)

        icon.setImageDrawable(drawable)

        row.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(42),
                dp(42)
            )
        )

        val name =
            TextView(context)

        name.text = text
        name.textSize = 16f
        name.setTextColor(Color.WHITE)

        name.setPadding(
            dp(12),
            0,
            dp(8),
            0
        )

        name.gravity =
            Gravity.CENTER_VERTICAL

        row.addView(
            name,
            LinearLayout.LayoutParams(
                0,
                dp(54),
                1f
            )
        )

        return row
    }

    private fun addLinuxButton(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val row =
            LinearLayout(context)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.setPadding(
            dp(14),
            0,
            dp(14),
            0
        )

        val icon =
            TextView(context)

        icon.text = "▣"
        icon.textSize = 24f
        icon.setTextColor(Color.WHITE)

        row.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(48),
                dp(56)
            )
        )

        val label =
            TextView(context)

        label.text = text
        label.textSize = 16f
        label.setTextColor(Color.WHITE)

        label.gravity =
            Gravity.CENTER_VERTICAL

        label.setPadding(
            dp(10),
            0,
            0,
            0
        )

        row.addView(
            label,
            LinearLayout.LayoutParams(
                0,
                dp(56),
                1f
            )
        )

        row.isClickable = true
        row.isFocusable = true

        icon.isClickable = false
        icon.isFocusable = false
        label.isClickable = false
        label.isFocusable = false

        row.setOnClickListener {
            try {
                action()
            } catch (e: Exception) {
                android.util.Log.e(
                    "METMC-LinuxApps",
                    "Failed to launch: $text",
                    e
                )
                android.widget.Toast.makeText(
                    context,
                    "Failed to launch $text",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )
    }

    private fun shellQuote(
        value: String
    ): String {

        return "'" +
            value.replace(
                "'",
                "'\\''"
            ) +
            "'"
    }

    private fun style(
        view: View
    ) {

        view.background =
            GradientDrawable().apply {

                setColor(
                    Color.rgb(
                        25,
                        27,
                        34
                    )
                )

                cornerRadius =
                    dp(10).toFloat()
            }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
            context.resources
                .displayMetrics
                .density
        ).toInt()
    }
}
