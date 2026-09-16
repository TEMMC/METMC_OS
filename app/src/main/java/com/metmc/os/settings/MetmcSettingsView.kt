package com.metmc.os.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MetmcSettingsView(private val context: Context) : LinearLayout(context) {
    private val body = LinearLayout(context)
    private val content = ScrollView(context)
    private val accent = Color.rgb(120, 170, 255)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(15, 18, 24))
        val bar = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(8), 0); setBackgroundColor(Color.rgb(28,32,41)) }
        bar.addView(TextView(context).apply { text = "⚙  METMC OS Settings"; textSize = 17f; setTextColor(Color.WHITE) }, LayoutParams(0, dp(50), 1f))
        val refresh = Button(context).apply { text = "↻"; isAllCaps = false; setOnClickListener { rebuild() } }
        bar.addView(refresh, LayoutParams(dp(52), dp(44)))
        addView(bar, LayoutParams(-1, dp(56)))
        content.addView(body, LayoutParams(-1, -2)); addView(content, LayoutParams(-1, 0, 1f)); rebuild()
    }

    private fun rebuild() {
        body.removeAllViews()
        section("Security & Lock Screen")
        row("Lock method", MetmcSettingsStore.getLockType(context).uppercase()) { security() }
        row("Change password / PIN / pattern", "Configure lock credential") { security() }
        section("Appearance")
        row("Theme", MetmcSettingsStore.getTheme(context).replaceFirstChar { it.uppercase() }) { appearance() }
        row("Wallpaper", "Choose wallpaper") { chooseWallpaper() }
        section("Desktop")
        switchRow("Window animations", "window_animations", true)
        switchRow("Window shadows", "window_shadows", true)
        switchRow("Auto focus windows", "auto_focus", true)
        switchRow("Portrait mode", "portrait_mode", false)
        section("Linux")
        switchRow("Start Linux environment", "linux_start", true)
        switchRow("Linux application windows", "linux_windows", true)
        switchRow("Use root environment", "linux_root", true)
        section("Terminal")
        row("Terminal user", "metmc (non-root)") { Toast.makeText(context, "The desktop terminal runs as the non-root metmc user.", Toast.LENGTH_SHORT).show() }
        row("Terminal options", "Monospace • history • Ctrl shortcuts") { Toast.makeText(context, "Terminal settings are active in the METMC Terminal window.", Toast.LENGTH_SHORT).show() }
        section("System")
        row("Updates", "Check for METMC OS updates") { try { Class.forName("com.metmc.os.update.MetmcUpdater").getMethod("checkForUpdate", Activity::class.java, Boolean::class.javaPrimitiveType).invoke(null, context, true) } catch (_: Exception) { Toast.makeText(context, "Update checker unavailable", Toast.LENGTH_SHORT).show() } }
        row("About", "METMC OS • Android + Linux desktop") { AlertDialog.Builder(context).setTitle("METMC OS").setMessage("METMC OS\nAndroid + Debian Linux unified desktop\n\nPowered by Tinotenda Enock Mapfumo aka Dr TEMMC").setPositiveButton("Close", null).show() }
    }

    private fun section(text: String) { body.addView(TextView(context).apply { this.text = text.uppercase(); textSize = 11f; setTextColor(accent); setPadding(dp(14), dp(16), dp(14), dp(6)) }, LayoutParams(-1, dp(38))) }
    private fun row(title: String, detail: String, action: () -> Unit) { val b = Button(context).apply { text = "$title\n$detail"; textSize = 13f; isAllCaps = false; gravity = Gravity.START or Gravity.CENTER_VERTICAL; setTextColor(Color.WHITE); setOnClickListener { action() }; setPadding(dp(16), dp(5), dp(12), dp(5)) }; body.addView(b, LayoutParams(-1, dp(62))) }
    private fun switchRow(label: String, key: String, def: Boolean) { val s = Switch(context).apply { text = label; textSize = 14f; setTextColor(Color.WHITE); setPadding(dp(16), 0, dp(16), 0); isChecked = MetmcSettingsStore.getBoolean(context, key, def); setOnCheckedChangeListener { _, checked -> MetmcSettingsStore.setBoolean(context, key, checked) } }; body.addView(s, LayoutParams(-1, dp(52))) }

    private fun security() {
        val values = arrayOf("Password", "PIN", "Pattern", "None"); val keys = arrayOf("password", "pin", "pattern", "none"); val current = MetmcSettingsStore.getLockType(context); val checked = keys.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(context).setTitle("Lock method").setSingleChoiceItems(values, checked) { d, which ->
            val type = keys[which]; if (type == "none") { MetmcSettingsStore.setLock(context, type, ""); d.dismiss(); rebuild(); return@setSingleChoiceItems }
            val input = EditText(context).apply { hint = if (type == "pin") "PIN" else if (type == "pattern") "Pattern code" else "Password"; setSingleLine(true); inputType = if (type == "pin") android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
            AlertDialog.Builder(context).setTitle("Set ${values[which]}").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ -> val v = input.text.toString(); if (v.isNotBlank()) { MetmcSettingsStore.setLock(context, type, v); rebuild() } }.show(); d.dismiss()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun appearance() { val values = arrayOf("Dark", "Light", "System"); val keys = arrayOf("dark", "light", "system"); val current = MetmcSettingsStore.getTheme(context); val checked = keys.indexOf(current).coerceAtLeast(0); AlertDialog.Builder(context).setTitle("Theme").setSingleChoiceItems(values, checked) { d, which -> MetmcSettingsStore.setTheme(context, keys[which]); d.dismiss(); rebuild() }.setNegativeButton("Cancel", null).show() }
    private fun chooseWallpaper() { val a = context as? Activity ?: return; val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }; a.startActivityForResult(i, 9002) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
