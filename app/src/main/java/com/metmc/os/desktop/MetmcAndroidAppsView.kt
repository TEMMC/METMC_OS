package com.metmc.os.desktop

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MetmcAndroidAppsView(private val context: Context, private val launch:(String)->Unit): LinearLayout(context){
    init{
        orientation=VERTICAL;setPadding(dp(10),dp(10),dp(10),dp(10));setBackgroundColor(Color.rgb(15,18,24))
        addView(TextView(context).apply{text="Android Applications";textSize=20f;setTextColor(Color.WHITE);setPadding(dp(8),dp(6),dp(8),dp(12))},LinearLayout.LayoutParams(-1,dp(50)))
        val scroll=ScrollView(context);val list=LinearLayout(context).apply{orientation=VERTICAL};val pm=context.packageManager
        val intent=android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent,PackageManager.MATCH_ALL).distinctBy{it.activityInfo.packageName}.sortedBy{it.loadLabel(pm).toString().lowercase()}.forEach{r->
            val pkg=r.activityInfo.packageName;if(pkg==context.packageName)return@forEach
            val row=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(4),dp(8),dp(4));setOnClickListener{launch(pkg)}}
            row.addView(ImageView(context).apply{setImageDrawable(r.loadIcon(pm))},LinearLayout.LayoutParams(dp(44),dp(44)))
            row.addView(TextView(context).apply{text=r.loadLabel(pm).toString();textSize=16f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,0,0)},LinearLayout.LayoutParams(0,dp(54),1f))
            list.addView(row,LinearLayout.LayoutParams(-1,dp(58)))
        }
        scroll.addView(list,FrameLayout.LayoutParams(-1,-2));addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
