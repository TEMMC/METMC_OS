package com.metmc.os.shell

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

import com.metmc.os.linux.LinuxDesktopActivity

class MetmcShell(private val context: Context) {

    fun create(): FrameLayout {
        val root = FrameLayout(context)
        root.setBackgroundColor(Color.rgb(30, 34, 40))

        val desktop = FrameLayout(context)
        root.addView(
            desktop,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val panel = LinearLayout(context)
        panel.orientation = LinearLayout.HORIZONTAL
        panel.gravity = Gravity.CENTER_VERTICAL
        panel.setPadding(16, 0, 16, 0)
        panel.setBackgroundColor(Color.rgb(20, 22, 26))

        val activities = Button(context)
        activities.text = "Activities"
        activities.isAllCaps = false

        val title = TextView(context)
        title.text = "METMC OS"
        title.setTextColor(Color.WHITE)
        title.textSize = 17f
        title.gravity = Gravity.CENTER
        title.typeface = Typeface.DEFAULT_BOLD

        val clock = TextView(context)
        clock.text = "●"
        clock.setTextColor(Color.WHITE)
        clock.gravity = Gravity.CENTER

        panel.addView(
            activities,
            LinearLayout.LayoutParams(
                160,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        panel.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        panel.addView(
            clock,
            LinearLayout.LayoutParams(
                100,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            panel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                64,
                Gravity.TOP
            )
        )

        val dock = LinearLayout(context)
        dock.orientation = LinearLayout.HORIZONTAL
        dock.gravity = Gravity.CENTER
        dock.setPadding(12, 8, 12, 8)
        dock.setBackgroundColor(Color.rgb(40, 44, 52))

        addAppButton(dock, "Terminal") {
            context.startActivity(
                Intent(context, LinuxDesktopActivity::class.java)
            )
        }

        addAppButton(dock, "Linux") {
            context.startActivity(
                Intent(context, LinuxDesktopActivity::class.java)
            )
        }

        addAppButton(dock, "Apps") {
            showOverview(root)
        }

        root.addView(
            dock,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                72,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        )

        activities.setOnClickListener {
            showOverview(root)
        }

        return root
    }

    private fun addAppButton(
        dock: LinearLayout,
        name: String,
        action: () -> Unit
    ) {
        val button = Button(context)
        button.text = name
        button.isAllCaps = false
        button.setOnClickListener { action() }

        dock.addView(
            button,
            LinearLayout.LayoutParams(150, 64)
        )
    }

    private fun showOverview(parent: FrameLayout) {
        val dialog = Dialog(context)

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)
        layout.setBackgroundColor(Color.rgb(35, 39, 46))

        val title = TextView(context)
        title.text = "Applications"
        title.textSize = 24f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        layout.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                80
            )
        )

        val linux = Button(context)
        linux.text = "🐧 Linux Desktop"
        linux.isAllCaps = false

        linux.setOnClickListener {
            dialog.dismiss()
            context.startActivity(
                Intent(context, LinuxDesktopActivity::class.java)
            )
        }

        layout.addView(linux)

        dialog.setContentView(layout)

        val window = dialog.window
        window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }
}
