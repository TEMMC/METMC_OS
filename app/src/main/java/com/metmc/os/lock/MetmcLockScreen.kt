package com.metmc.os.lock

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import com.metmc.os.wallpaper.MetmcWallpaper
import com.metmc.os.media.MetmcMediaInfo
import android.widget.ImageView
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MetmcLockScreen(
    context: Context,
    private val correctPassword: String = "metmc",
    private val onUnlocked: Runnable
) : FrameLayout(context) {

    private val backgroundColor = Color.rgb(9, 11, 15)
    private val panelColor = Color.rgb(22, 25, 32)
    private val fieldColor = Color.rgb(31, 35, 44)
    private val textColor = Color.WHITE
    private val secondaryColor = Color.rgb(170, 177, 190)
    private val accentColor = Color.rgb(70, 135, 255)

    private lateinit var passwordInput: EditText
    private lateinit var status: TextView
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var mediaInfo: TextView
    private lateinit var wallpaperView: ImageView

    private val clockUpdater = object : Runnable {
        override fun run() {
            updateDateTime()
            updateMediaInfo()
            postDelayed(this, 1000)
        }
    }

    init {
        setBackgroundColor(backgroundColor)
        isFocusable = true
        isFocusableInTouchMode = true

        setupWallpaper()

        buildUi()

        post {
            requestFocus()
            passwordInput.requestFocus()

            val imm = context.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as? InputMethodManager

            imm?.showSoftInput(
                passwordInput,
                InputMethodManager.SHOW_IMPLICIT
            )
        }

        post(clockUpdater)
    }

    private fun setupWallpaper() {
        wallpaperView = ImageView(context)
        wallpaperView.scaleType = ImageView.ScaleType.CENTER_CROP
        wallpaperView.alpha = 0.72f
        wallpaperView.setBackgroundColor(backgroundColor)

        addView(
            wallpaperView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        // Keep the wallpaper behind all lock-screen controls.
        wallpaperView.sendToBack()

        MetmcWallpaper.apply(context, wallpaperView)
    }

    private fun buildUi() {
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(32), dp(28), dp(32), dp(28))

        addView(
            root,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        clock = TextView(context)
        clock.setTextColor(textColor)
        clock.textSize = 52f
        clock.typeface = Typeface.create(
            Typeface.DEFAULT,
            Typeface.NORMAL
        )
        clock.gravity = Gravity.CENTER

        root.addView(
            clock,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        date = TextView(context)
        date.setTextColor(secondaryColor)
        date.textSize = 17f
        date.gravity = Gravity.CENTER

        root.addView(
            date,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        mediaInfo = TextView(context)
        mediaInfo.setTextColor(secondaryColor)
        mediaInfo.textSize = 14f
        mediaInfo.gravity = Gravity.CENTER
        mediaInfo.maxLines = 1
        mediaInfo.ellipsize = android.text.TextUtils.TruncateAt.END
        mediaInfo.text = ""

        root.addView(
            mediaInfo,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(34)
            )
        )

        val spacer = View(context)
        root.addView(
            spacer,
            LinearLayout.LayoutParams(
                1,
                0,
                1f
            )
        )

        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(30), dp(28), dp(30), dp(30))
        card.background = rounded(panelColor, dp(22))

        val cardParams = LinearLayout.LayoutParams(
            dp(430),
            LayoutParams.WRAP_CONTENT
        )

        root.addView(card, cardParams)

        val logo = TextView(context)
        logo.text = "METMC OS"
        logo.setTextColor(textColor)
        logo.textSize = 28f
        logo.typeface = Typeface.DEFAULT_BOLD
        logo.gravity = Gravity.CENTER

        card.addView(
            logo,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val subtitle = TextView(context)
        subtitle.text = "Welcome back"
        subtitle.setTextColor(secondaryColor)
        subtitle.textSize = 16f
        subtitle.gravity = Gravity.CENTER

        card.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        val user = TextView(context)
        user.text = "Dr TEMMC"
        user.setTextColor(textColor)
        user.textSize = 18f
        user.typeface = Typeface.DEFAULT_BOLD
        user.gravity = Gravity.CENTER

        card.addView(
            user,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        passwordInput = EditText(context)
        passwordInput.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        passwordInput.imeOptions = EditorInfo.IME_ACTION_DONE
        passwordInput.setSingleLine(true)
        passwordInput.hint = "Password"
        passwordInput.setTextColor(textColor)
        passwordInput.setHintTextColor(secondaryColor)
        passwordInput.textSize = 20f
        passwordInput.gravity = Gravity.CENTER
        passwordInput.setPadding(dp(16), 0, dp(16), 0)
        passwordInput.background = rounded(fieldColor, dp(12))

        card.addView(
            passwordInput,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                topMargin = dp(18)
            }
        )

        val unlock = Button(context)
        unlock.text = "Unlock"
        unlock.isAllCaps = false
        unlock.textSize = 17f
        unlock.setTextColor(Color.WHITE)
        unlock.background = rounded(accentColor, dp(12))

        card.addView(
            unlock,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(14)
            }
        )

        status = TextView(context)
        status.setTextColor(secondaryColor)
        status.textSize = 14f
        status.gravity = Gravity.CENTER
        status.text = "Enter your password to continue"

        card.addView(
            status,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        val footer = TextView(context)
        footer.text =
            "METMC OS powered by Tinotenda Enock Mapfumo aka Dr TEMMC"
        footer.setTextColor(Color.rgb(110, 116, 128))
        footer.textSize = 11f
        footer.gravity = Gravity.CENTER
        footer.setPadding(dp(4), dp(8), dp(4), 0)

        card.addView(
            footer,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )

        unlock.setOnClickListener {
            attemptUnlock()
        }

        passwordInput.setOnEditorActionListener { _, actionId, event ->
            if (
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null &&
                 event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                attemptUnlock()
                true
            } else {
                false
            }
        }

        val bottomSpacer = View(context)
        root.addView(
            bottomSpacer,
            LinearLayout.LayoutParams(
                1,
                0,
                1f
            )
        )

        updateDateTime()
    }

    private fun updateMediaInfo() {
        if (!::mediaInfo.isInitialized) return

        val text = MetmcMediaInfo.displayText()

        mediaInfo.text = if (MetmcMediaInfo.playing && text.isNotBlank()) {
            "♫ $text"
        } else {
            text
        }
    }

    private fun attemptUnlock() {
        val entered = passwordInput.text.toString()

        if (entered == correctPassword) {
            status.text = "Unlocking..."
            status.setTextColor(Color.rgb(100, 220, 140))

            val imm = context.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as? InputMethodManager

            imm?.hideSoftInputFromWindow(
                passwordInput.windowToken,
                0
            )

            onUnlocked.run()
            destroy()
        } else {
            status.text = "Incorrect password"
            status.setTextColor(Color.rgb(255, 105, 105))
            passwordInput.text.clear()
            passwordInput.requestFocus()
        }
    }

    private fun updateDateTime() {
        val now = Date()

        clock.text = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(now)

        date.text = SimpleDateFormat(
            "EEEE, d MMMM yyyy",
            Locale.getDefault()
        ).format(now)
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density)
            .toInt()
    }

    fun destroy() {
        removeCallbacks(clockUpdater)

        val parent = parent
        if (parent is android.view.ViewGroup) {
            parent.removeView(this)
        }
    }
}
