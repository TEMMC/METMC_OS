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
import android.widget.LinearLayout

class DesktopAppMenu(
    private val context: Context,
    private val onLaunchWindow: (String, View) -> Unit
) {

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

        val activities = pm.getInstalledApplications(
            PackageManager.GET_META_DATA
        ).sortedBy {
            pm.getApplicationLabel(it)
                .toString()
                .lowercase()
        }

        activities.forEach { appInfo ->

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
                dp(8),
                dp(6),
                dp(8),
                dp(6)
            )

            val icon = ImageView(context)
            icon.setImageDrawable(
                appInfo.loadIcon(pm)
            )

            row.addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(42),
                    dp(42)
                )
            )

            val name = TextView(context)
            name.text = label
            name.textSize = 16f
            name.setTextColor(Color.WHITE)
            name.setPadding(
                dp(12),
                0,
                dp(8),
                0
            )

            row.addView(
                name,
                LinearLayout.LayoutParams(
                    0,
                    dp(54),
                    1f
                )
            )

            row.setOnClickListener {

                AndroidWindowLauncher.launch(
                    activity,
                    packageName
                )
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

        addLinuxButton(
            root,
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

        /*
         * Real Linux applications.
         *
         * Read .desktop files from the Debian rootfs. Terminal
         * intentionally remains the first entry.
         */
        val linuxApps = loadLinuxApplications()

        linuxApps.forEach { app ->

            addLinuxButton(
                root,
                app.first
            ) {
                launchLinuxApplication(app.second)
            }
        }

        style(root)

        onLaunchWindow(
            "Linux Applications",
            root
        )
    }

    /*
     * Returns pairs of:
     *
     * Name
     * Exec
     *
     * from real Debian .desktop launchers.
     */
    private fun loadLinuxApplications():
        List<Pair<String, String>> {

        val result =
            mutableListOf<Pair<String, String>>()

        try {

            val command =
                "chroot /data/local/linux/rootfs " +
                "/bin/bash -c " +
                "\"for f in /usr/share/applications/*.desktop; do " +
                "[ -f \\\"\\$f\\\" ] || continue; " +
                "grep -q '^Type=Application' \\\"\\$f\\\" || continue; " +
                "grep -q '^NoDisplay=true' \\\"\\$f\\\" && continue; " +
                "grep -q '^Hidden=true' \\\"\\$f\\\" && continue; " +
                "name=\\$(grep -m1 '^Name=' \\\"\\$f\\\" | cut -d= -f2-); " +
                "exec=\\$(grep -m1 '^Exec=' \\\"\\$f\\\" | cut -d= -f2-); " +
                "[ -n \\\"\\$name\\\" ] && [ -n \\\"\\$exec\\\" ] && " +
                "printf '%s|%s\\\\n' \\\"\\$name\\\" \\\"\\$exec\\\"; " +
                "done\""

            val process =
                ProcessBuilder(
                    "su",
                    "-c",
                    command
                )
                    .redirectErrorStream(true)
                    .start()

            val reader =
                java.io.BufferedReader(
                    java.io.InputStreamReader(
                        process.inputStream
                    )
                )

            while (true) {

                val line =
                    reader.readLine()
                        ?: break

                val separator =
                    line.indexOf("|")

                if (separator <= 0)
                    continue

                val name =
                    line.substring(
                        0,
                        separator
                    ).trim()

                var exec =
                    line.substring(
                        separator + 1
                    ).trim()

                /*
                 * Remove standard .desktop field codes such as:
                 *
                 * %U
                 * %u
                 * %F
                 * %f
                 * %i
                 * %c
                 * %k
                 */
                exec =
                    exec.replace(
                        Regex("%[fFuUdDnNickvm]"),
                        ""
                    ).trim()

                if (
                    name.isNotEmpty() &&
                    exec.isNotEmpty() &&
                    name != "Terminal"
                ) {

                    result.add(
                        Pair(
                            name,
                            exec
                        )
                    )
                }
            }

            reader.close()
            process.waitFor()

        } catch (ignored: Exception) {
        }

        return result
            .distinctBy {
                it.first.lowercase()
            }
            .sortedBy {
                it.first.lowercase()
            }
    }

    /*
     * Start the selected Linux program inside the existing
     * METMC Xvfb display.
     */
    private fun launchLinuxApplication(
        exec: String
    ) {

        try {

            val safeExec =
                exec.replace(
                    "'",
                    "'\\\\''"
                )

            val command =
                "chroot /data/local/linux/rootfs " +
                "/bin/bash -c " +
                "\"export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; " +
                "export HOME=/root; " +
                "export USER=root; " +
                "export DISPLAY=:100; " +
                "export XDG_RUNTIME_DIR=/tmp/metmc-runtime; " +
                "mkdir -p /tmp/metmc-runtime; " +
                "chmod 700 /tmp/metmc-runtime; " +
                "nohup /bin/bash -lc '" +
                safeExec +
                "' >/tmp/metmc-linux-app.log 2>&1 &\""

            ProcessBuilder(
                "su",
                "-c",
                command
            )
                .redirectErrorStream(true)
                .start()

        } catch (ignored: Exception) {
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
        button.setPadding(
            dp(14),
            0,
            dp(14),
            0
        )

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
        label.setPadding(
            dp(10),
            0,
            0,
            0
        )

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

    private fun style(
        view: View
    ) {

        view.background =
            GradientDrawable().apply {
                setColor(
                    Color.rgb(25, 27, 34)
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
