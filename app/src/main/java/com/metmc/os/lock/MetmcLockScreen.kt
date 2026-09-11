package com.metmc.os.lock

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MetmcLockScreen(
    context: Context,
    private val correctPin: String = "0000",
    private val onUnlocked: () -> Unit
) : FrameLayout(context) {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var pinDisplay: TextView

    private var enteredPin = ""

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        setBackgroundColor(Color.rgb(7, 10, 20))
        isClickable = true
        isFocusable = true

        buildInterface()

        handler.post(clockRunnable)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun text(
        value: String,
        size: Float,
        color: Int = Color.WHITE,
        bold: Boolean = false
    ): TextView {
        return TextView(context).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER

            if (bold) {
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            }
        }
    }

    private fun buildInterface() {

        /*
         * Background layer
         */
        val background = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.rgb(7, 10, 20))
        }

        addView(
            background,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        /*
         * METMC branding
         */
        val brand = text(
            "METMC",
            18f,
            Color.rgb(170, 190, 255),
            true
        )

        background.addView(
            brand,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        val subtitle = text(
            "PRIVATE SYSTEM",
            10f,
            Color.rgb(125, 140, 170),
            true
        )

        background.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(24)
            )
        )

        /*
         * Clock
         */
        clock = text(
            "--:--",
            64f,
            Color.WHITE,
            true
        )

        clock.setPadding(0, dp(16), 0, 0)

        background.addView(
            clock,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(90)
            )
        )

        date = text(
            "",
            16f,
            Color.rgb(180, 190, 215)
        )

        background.addView(
            date,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        /*
         * Profile card
         */
        val profile = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                dp(25),
                dp(22),
                dp(25),
                dp(22)
            )

            setBackgroundColor(
                Color.rgb(20, 25, 42)
            )
        }

        val profileParams =
            LinearLayout.LayoutParams(
                dp(340),
                LayoutParams.WRAP_CONTENT
            )

        profileParams.gravity = Gravity.CENTER
        profileParams.topMargin = dp(20)

        background.addView(profile, profileParams)

        /*
         * Profile circle
         */
        val avatar = text(
            "T",
            34f,
            Color.WHITE,
            true
        )

        avatar.setBackgroundColor(
            Color.rgb(70, 90, 160)
        )

        profile.addView(
            avatar,
            LinearLayout.LayoutParams(
                dp(82),
                dp(82)
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        val username = text(
            "TEMMC",
            22f,
            Color.WHITE,
            true
        )

        username.setPadding(0, dp(14), 0, 0)

        profile.addView(
            username,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )

        val role = text(
            "METMC OS Administrator",
            12f,
            Color.rgb(145, 155, 180)
        )

        profile.addView(
            role,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        /*
         * PIN display
         */
        pinDisplay = text(
            "Enter PIN",
            15f,
            Color.rgb(170, 180, 205)
        )

        pinDisplay.setPadding(0, dp(15), 0, dp(5))

        profile.addView(
            pinDisplay,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        /*
         * Keypad
         */
        val keypad = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        profile.addView(
            keypad,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        val numbers = arrayOf(
            arrayOf("1", "2", "3"),
            arrayOf("4", "5", "6"),
            arrayOf("7", "8", "9"),
            arrayOf("⌫", "0", "✓")
        )

        for (row in numbers) {

            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            keypad.addView(
                rowLayout,
                LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    dp(58)
                )
            )

            for (value in row) {

                val button = Button(context).apply {

                    text = value
                    textSize = 17f
                    setTextColor(Color.WHITE)

                    setBackgroundColor(
                        Color.rgb(30, 37, 60)
                    )

                    setOnClickListener {
                        handleKey(value)
                    }
                }

                rowLayout.addView(
                    button,
                    LinearLayout.LayoutParams(
                        dp(78),
                        dp(48)
                    ).apply {
                        gravity = Gravity.CENTER
                        setMargins(
                            dp(4),
                            dp(4),
                            dp(4),
                            dp(4)
                        )
                    }
                )
            }
        }

        /*
         * Footer
         */
        val footer = text(
            "METMC OS  •  Secure Session",
            10f,
            Color.rgb(105, 115, 140)
        )

        background.addView(
            footer,
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(35)
            ).apply {
                topMargin = dp(12)
            }
        )
    }

    private fun handleKey(key: String) {

        when (key) {

            "⌫" -> {
                if (enteredPin.isNotEmpty()) {
                    enteredPin =
                        enteredPin.dropLast(1)
                    updatePinDisplay()
                }
            }

            "✓" -> {
                verifyPin()
            }

            else -> {
                if (enteredPin.length < 8) {
                    enteredPin += key
                    updatePinDisplay()

                    if (enteredPin.length == correctPin.length) {
                        verifyPin()
                    }
                }
            }
        }
    }

    private fun updatePinDisplay() {

        pinDisplay.text =
            if (enteredPin.isEmpty()) {
                "Enter PIN"
            } else {
                "• ".repeat(enteredPin.length)
            }
    }

    private fun verifyPin() {

        if (enteredPin == correctPin) {

            handler.removeCallbacks(clockRunnable)

            animateUnlock()

        } else {

            enteredPin = ""

            pinDisplay.text = "Incorrect PIN"

            postDelayed({
                pinDisplay.text = "Enter PIN"
            }, 1200)
        }
    }

    private fun animateUnlock() {

        animate()
            .alpha(0f)
            .setDuration(350)
            .withEndAction {
                visibility = View.GONE
                alpha = 1f
                onUnlocked()
            }
            .start()
    }

    private fun updateClock() {

        val now = Date()

        clock.text =
            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(now)

        date.text =
            SimpleDateFormat(
                "EEEE, d MMMM",
                Locale.getDefault()
            ).format(now)
    }

    fun destroy() {
        handler.removeCallbacks(clockRunnable)
    }
}
