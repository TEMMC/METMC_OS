package com.metmc.os.settings

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SystemAppRemoverActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val scan = Button(this).apply { text = "Scan system apps" }
        val restore = Button(this).apply { text = "Restore apps changed by METMC OS" }
        status = TextView(this).apply { setPadding(0, dp(8), 0, dp(8)) }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list, ViewGroup.LayoutParams(-1, -2)) }
        root.addView(scan)
        root.addView(restore)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        scan.setOnClickListener { loadApps() }
        restore.setOnClickListener { confirmRestore() }
        loadApps()
    }

    private fun loadApps() {
        status.text = "Scanning system apps…"
        Thread {
            val result = runCatching { SystemAppManager(this).scanSuggestions() }
            runOnUiThread {
                result.onSuccess { renderApps(it) }
                    .onFailure {
                        status.text = "Scan failed · ${it.message}"
                        list.removeAllViews()
                    }
            }
        }.start()
    }

    private fun renderApps(apps: List<SystemAppManager.SuggestedApp>) {
        list.removeAllViews()
        status.text = if (apps.isEmpty()) "No catalogued system apps found." else "${apps.size} catalogued apps"
        apps.forEach { app ->
            val row = TextView(this).apply {
                text = "${app.title}\n${app.packageName}\n${if (app.disabled) "Disabled" else "Enabled"}"
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { runAction(listOf(app.packageName), !app.disabled) }
            }
            list.addView(row, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun confirmRestore() {
        val changed = runCatching { SystemAppManager(this).changedPackages().size }.getOrDefault(0)
        if (changed == 0) {
            status.text = "No apps changed by METMC OS."
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Restore $changed app${if (changed == 1) "" else "s"}?")
            .setMessage("Every package disabled through METMC OS will be re-enabled.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore") { _, _ -> runAction(emptyList(), false) }
            .show()
    }

    private fun runAction(packages: List<String>, disable: Boolean) {
        status.text = if (disable) "Applying changes…" else "Restoring apps…"
        Thread {
            val result = runCatching {
                val manager = SystemAppManager(this)
                if (disable) manager.disable(packages) else manager.restoreAllChangedByMETMC()
            }
            runOnUiThread {
                status.text = result.fold(
                    onSuccess = { "${it.succeeded.size} changed · ${it.failed.size} failed" },
                    onFailure = { "Operation failed · ${it.message}" }
                )
                loadApps()
            }
        }.start()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
