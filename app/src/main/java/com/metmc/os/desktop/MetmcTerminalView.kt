package com.metmc.os.desktop

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MetmcTerminalView(private val context: android.content.Context) : LinearLayout(context) {
    private val output=TextView(context)
    private val input=EditText(context)
    private val history=ArrayList<String>()
    private var historyIndex=0
    private val prompt="root@debian:~# "
    private val rootfs="/data/local/linux/rootfs"

    init {
        orientation=VERTICAL;setBackgroundColor(Color.rgb(5,7,10))
        val top=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,dp(6),0);setBackgroundColor(Color.rgb(24,28,35))}
        top.addView(TextView(context).apply{text="METMC Terminal";textSize=16f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD},LinearLayout.LayoutParams(0,dp(50),1f))
        top.addView(Button(context).apply{text="Clear";isAllCaps=false;setOnClickListener{output.text="";append(prompt)}},LinearLayout.LayoutParams(dp(72),dp(42)))
        top.addView(Button(context).apply{text="Root";isAllCaps=false;setOnClickListener{input.requestFocus()}},LinearLayout.LayoutParams(dp(72),dp(42)))
        addView(top,LayoutParams(-1,dp(56)))
        output.setTextColor(Color.rgb(225,230,235));output.textSize=14f;output.typeface=Typeface.MONOSPACE;output.setTextIsSelectable(true);output.setPadding(dp(14),dp(12),dp(14),dp(12));output.text="METMC Terminal\nDebian ARM64 • root shell • shared storage bridge active\n\n$prompt"
        val scroll=ScrollView(context).apply{isFillViewport=true;addView(output,ScrollView.LayoutParams(-1,-2))};addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val bar=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(4),dp(8),dp(4));setBackgroundColor(Color.rgb(20,23,28))}
        bar.addView(TextView(context).apply{text=prompt;setTextColor(Color.rgb(110,205,135));textSize=13f;typeface=Typeface.MONOSPACE},LinearLayout.LayoutParams(-2,dp(50)))
        input.hint="enter command";input.setHintTextColor(Color.rgb(100,105,112));input.setTextColor(Color.WHITE);input.textSize=14f;input.typeface=Typeface.MONOSPACE;input.setSingleLine(true);input.background=null;input.imeOptions=EditorInfo.IME_ACTION_DONE;bar.addView(input,LinearLayout.LayoutParams(0,dp(50),1f));addView(bar,LayoutParams(-1,dp(58)))
        input.setOnKeyListener{_,key,event->if(event.action!=KeyEvent.ACTION_DOWN)false else when(key){KeyEvent.KEYCODE_ENTER->{execute(input.text.toString());true};KeyEvent.KEYCODE_DPAD_UP->{if(history.isNotEmpty()){historyIndex=(historyIndex-1).coerceAtLeast(0);input.setText(history[historyIndex]);input.setSelection(input.length())};true};KeyEvent.KEYCODE_DPAD_DOWN->{if(history.isNotEmpty()){historyIndex=(historyIndex+1).coerceAtMost(history.size);input.setText(if(historyIndex<history.size)history[historyIndex]else "");input.setSelection(input.length())};true};else->false}}
        input.setOnEditorActionListener{_,action,_->if(action==EditorInfo.IME_ACTION_DONE){execute(input.text.toString());true}else false}
        input.requestFocus()
    }
    private fun execute(command:String){val cmd=command.trim();input.setText("");if(cmd.isBlank()){append(prompt);return};if(history.lastOrNull()!=cmd)history.add(cmd);historyIndex=history.size;append("$prompt$cmd\n");if(cmd=="clear"){output.text="";append(prompt);return};if(cmd=="exit"){append("exit is disabled for the desktop shell.\n$prompt");return};Thread{try{val bridge="mkdir -p '$rootfs/storage/emulated/0'; mountpoint -q '$rootfs/storage/emulated/0' || mount --bind /storage/emulated/0 '$rootfs/storage/emulated/0' 2>/dev/null || true; cd /root 2>/dev/null || cd /; export HOME=/root; export USER=root; export LOGNAME=root; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export DISPLAY=:100; exec /bin/bash -lc ${quote(cmd)}";val p=ProcessBuilder("su","-c","chroot '$rootfs' /bin/bash -c ${quote(bridge)}").redirectErrorStream(true).start();val r=BufferedReader(InputStreamReader(p.inputStream));val sb=StringBuilder();var line:String?;while(r.readLine().also{line=it}!=null)sb.append(line).append('\n');val code=p.waitFor();(context as?Activity)?.runOnUiThread{append(sb.toString());if(code!=0)append("[exit $code]\n");append(prompt)}}catch(e:Exception){(context as?Activity)?.runOnUiThread{append("Terminal error: ${e.message}\n$prompt")}}}.start()}
    private fun append(s:String){output.append(s);(output.parent as?ScrollView)?.post{(output.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)}}
    private fun quote(s:String)="'"+s.replace("'","'\\''")+"'"
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
