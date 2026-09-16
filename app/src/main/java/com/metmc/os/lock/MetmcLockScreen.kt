package com.metmc.os.lock

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.metmc.os.media.MetmcMediaInfo
import com.metmc.os.settings.MetmcSettingsStore
import com.metmc.os.wallpaper.MetmcWallpaper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MetmcLockScreen(
    context: Context,
    private val correctPassword: String = "metmc",
    private val onUnlocked: Runnable
) : FrameLayout(context) {

    private val panelColor = Color.rgb(22,25,32)
    private val fieldColor = Color.rgb(31,35,44)
    private val textColor = Color.WHITE
    private val secondaryColor = Color.rgb(170,177,190)
    private val accentColor = Color.rgb(70,135,255)
    private lateinit var passwordInput: EditText
    private lateinit var unlock: Button
    private lateinit var status: TextView
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var mediaInfo: TextView
    private lateinit var wallpaperView: ImageView

    private val clockUpdater = object : Runnable {
        override fun run() {
            updateDateTime(); updateMediaInfo(); postDelayed(this,1000)
        }
    }

    init {
        setBackgroundColor(Color.rgb(9,11,15)); isFocusable=true; isFocusableInTouchMode=true
        setupWallpaper(); buildUi()
        setOnApplyWindowInsetsListener { _, insets -> insets }
        post {
            requestFocus()
            if (MetmcSettingsStore.getLockType(context) != "none") {
                passwordInput.requestFocus()
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(passwordInput,InputMethodManager.SHOW_IMPLICIT)
            }
        }
        post(clockUpdater)
    }

    private fun setupWallpaper() {
        wallpaperView=ImageView(context).apply { scaleType=ImageView.ScaleType.CENTER_CROP; alpha=.72f; setBackgroundColor(Color.rgb(9,11,15)) }
        addView(wallpaperView,LayoutParams(-1,-1)); MetmcWallpaper.apply(context,wallpaperView)
    }

    private fun buildUi() {
        val scroll=ScrollView(context).apply { isFillViewport=true; clipToPadding=false }
        val root=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL
            setPadding(dp(20),dp(16),dp(20),dp(20)); clipToPadding=false
        }
        scroll.addView(root,ScrollView.LayoutParams(-1,-1)); addView(scroll,LayoutParams(-1,-1))

        clock=TextView(context).apply { setTextColor(textColor); textSize=44f; gravity=Gravity.CENTER }
        root.addView(clock,LinearLayout.LayoutParams(-1,dp(56)))
        date=TextView(context).apply { setTextColor(secondaryColor); textSize=15f; gravity=Gravity.CENTER }
        root.addView(date,LinearLayout.LayoutParams(-1,dp(34)))
        mediaInfo=TextView(context).apply { setTextColor(secondaryColor); textSize=13f; gravity=Gravity.CENTER; maxLines=1; ellipsize=TextUtils.TruncateAt.END }
        root.addView(mediaInfo,LinearLayout.LayoutParams(-1,dp(30)))
        root.addView(Space(context),LinearLayout.LayoutParams(1,0,1f))

        val card=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL; setPadding(dp(22),dp(18),dp(22),dp(18)); background=rounded(panelColor,dp(20)); clipChildren=false }
        val cardParams=LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity=Gravity.CENTER_HORIZONTAL; topMargin=dp(8); bottomMargin=dp(8); width=ViewGroup.LayoutParams.MATCH_PARENT }
        root.addView(card,cardParams)

        card.addView(TextView(context).apply { text="METMC OS"; setTextColor(textColor); textSize=25f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER },LinearLayout.LayoutParams(-1,dp(38)))
        card.addView(TextView(context).apply { text="Welcome back"; setTextColor(secondaryColor); textSize=14f; gravity=Gravity.CENTER },LinearLayout.LayoutParams(-1,dp(28)))
        card.addView(TextView(context).apply { text="Dr TEMMC"; setTextColor(textColor); textSize=17f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER },LinearLayout.LayoutParams(-1,dp(36)))

        passwordInput=EditText(context).apply {
            inputType=when(MetmcSettingsStore.getLockType(context)){"pin"->InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD;else->InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD}
            imeOptions=EditorInfo.IME_ACTION_DONE; setSingleLine(true); hint=credentialHint(); setTextColor(textColor); setHintTextColor(secondaryColor); textSize=18f; gravity=Gravity.CENTER; setPadding(dp(14),0,dp(14),0); background=rounded(fieldColor,dp(12))
        }
        card.addView(passwordInput,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(12)})

        unlock=Button(context).apply { text="Unlock"; isAllCaps=false; textSize=16f; setTextColor(Color.WHITE); background=rounded(accentColor,dp(12)); setOnClickListener{attemptUnlock()} }
        card.addView(unlock,LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(10)})

        status=TextView(context).apply { setTextColor(secondaryColor); textSize=13f; gravity=Gravity.CENTER; text="Enter your credential to continue" }
        card.addView(status,LinearLayout.LayoutParams(-1,dp(38)).apply{topMargin=dp(2)})
        card.addView(TextView(context).apply { text="METMC OS powered by Tinotenda Enock Mapfumo aka Dr TEMMC"; setTextColor(Color.rgb(110,116,128)); textSize=10f; gravity=Gravity.CENTER; maxLines=2; ellipsize=TextUtils.TruncateAt.END },LinearLayout.LayoutParams(-1,dp(32)))

        unlock.setOnEditorActionListener { _,action,event -> if(action==EditorInfo.IME_ACTION_DONE || (event!=null&&event.keyCode==KeyEvent.KEYCODE_ENTER)){attemptUnlock();true}else false }

        if(MetmcSettingsStore.getLockType(context)=="none") {
            passwordInput.visibility=GONE; unlock.visibility=GONE; status.text="Lock screen disabled"; postDelayed({attemptUnlock()},150)
        }
        updateDateTime()
    }

    private fun credentialHint():String=when(MetmcSettingsStore.getLockType(context)){"pin"->"PIN";"pattern"->"Pattern";"none"->"";else->"Password"}

    private fun attemptUnlock() {
        val type=MetmcSettingsStore.getLockType(context)
        val entered=passwordInput.text.toString()
        if(type=="none" || entered==MetmcSettingsStore.getCredential(context) || (MetmcSettingsStore.getCredential(context).isBlank() && entered==correctPassword)) {
            status.text="Unlocking..."; status.setTextColor(Color.rgb(100,220,140))
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(passwordInput.windowToken,0)
            onUnlocked.run(); destroy()
        } else {
            status.text="Incorrect credential"; status.setTextColor(Color.rgb(255,105,105)); passwordInput.text.clear(); passwordInput.requestFocus()
        }
    }

    private fun updateMediaInfo(){ if(::mediaInfo.isInitialized){ val t=MetmcMediaInfo.displayText(); mediaInfo.text=if(MetmcMediaInfo.playing&&t.isNotBlank())"♫ $t" else t } }
    private fun updateDateTime(){ val now=Date(); clock.text=SimpleDateFormat("HH:mm",Locale.getDefault()).format(now); date.text=SimpleDateFormat("EEEE, d MMMM yyyy",Locale.getDefault()).format(now) }
    override fun onAttachedToWindow(){ super.onAttachedToWindow(); MetmcWallpaper.apply(context,wallpaperView); updateMediaInfo() }
    private fun rounded(color:Int,radius:Int)=GradientDrawable().apply{setColor(color);cornerRadius=radius.toFloat()}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    fun destroy(){removeCallbacks(clockUpdater);(parent as? ViewGroup)?.removeView(this)}
}
