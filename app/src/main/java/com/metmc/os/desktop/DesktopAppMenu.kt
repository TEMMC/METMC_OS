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

        val intent = Intent(
            Intent.ACTION_MAIN,
            null
        ).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val activities =
            pm.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            ).sortedBy {
                it.loadLabel(pm)
                    .toString()
                    .lowercase()
            }

        activities.forEach { info ->

            val packageName =
                info.activityInfo.packageName

            if (packageName == context.packageName) {
                return@forEach
            }

            val label =
                info.loadLabel(pm)
                    .toString()

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
                info.loadIcon(pm)
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
            ScrollView.LayoutParams(
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

        addLinuxButton(
            root,
            "Linux Desktop"
        ) {
            context.startActivity(
                Intent(
                    context,
                    com.metmc.os.linux.LinuxDesktopActivity::class.java
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
        }

        style(root)

        onLaunchWindow(
            "Linux Applications",
            root
        )
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
