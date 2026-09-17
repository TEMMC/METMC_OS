package com.metmc.os.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.metmc.os.R
import com.metmc.os.ui.PremiumUi

class SystemAppRemoverActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private var suggestions: List<SystemAppManager.SuggestedApp> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_app_remover)
        status = findViewById(R.id.system_app_status)
        list = findViewById(R.id.system_app_list)
        findViewById<View>(R.id.system_app_scan).setOnClickListener { loadApps() }
        findViewById<View>(R.id.system_app_restore).setOnClickListener { confirmRestore() }
        loadApps()
    }

    private fun loadApps() {
        status.text = "Scanning system apps…"
        Thread({
            val result = runCatching { SystemAppManager(this).scanSuggestions() }
            runOnUiThread {
                result.onSuccess {
                    suggestions = it
                    renderApps(it)
                }.onFailure {
                    status.text = "Scan failed · ${it.message}"
                    renderApps(emptyList())
                }
            }
        }, "metmc-system-app-scan").start()
    }

    private fun renderApps(apps: List<SystemAppManager.SuggestedApp>) {
        list.removeAllViews()
        if (apps.isEmpty()) {
            status.text = if (status.text.startsWith("Scan failed")) status.text else "No catalogued system apps found."
            return
        }
        status.text = "${apps.size} catalogued apps"
        apps.forEach { app ->
            val row = TextView(this).apply {
                text = "${app.title}\n${app.packageName}\n${if (app.disabled) "Disabled" else "Enabled"}"
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener {
                    if (app.disabled) runAction(listOf(app.packageName), false)
                    else runAction(listOf(app.packageName), true)
                }
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
        Thread({
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
        }, "metmc-system-app-action").start()
    }

    private fun dp(value: Int) = PremiumUi.dp(this, value)
}
