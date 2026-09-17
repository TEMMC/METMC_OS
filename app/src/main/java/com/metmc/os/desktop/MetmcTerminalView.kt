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
    private val output=TextView(context); private val input=EditText(context); private val history=ArrayList<String>(); private var historyIndex=0
    private val prompt="root@debian:~$ "; private val rootfs="/data/local/linux/rootfs"
    private val interactive=Regex("^(nano|vim|vi|python3?|htop|top|less|man)(\\s.*)?$")
    init { orientation=VERTICAL; setBackgroundColor(Color.rgb(5,7,10)); build() }
    private fun build(){
        val top=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.rgb(24,28,35))}
        top.addView(Space(context),LinearLayout.LayoutParams(0,dp(50),1f)); top.addView(Button(context).apply{text="Clear";isAllCaps=false;setOnClickListener{output.text=prompt}},LinearLayout.LayoutParams(dp(72),dp(42))); addView(top,LinearLayout.LayoutParams(-1,dp(56)))
        output.apply{setTextColor(Color.rgb(225,230,235));textSize=14f;typeface=Typeface.MONOSPACE;setTextIsSelectable(true);setPadding(dp(14),dp(12),dp(14),dp(12));text=prompt}
        val scroll=ScrollView(context).apply{isFillViewport=true;addView(output,FrameLayout.LayoutParams(-1,-2))}; addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val bar=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(4),dp(8),dp(4));setBackgroundColor(Color.rgb(20,23,28))}
        bar.addView(TextView(context).apply{text=prompt;setTextColor(Color.rgb(110,205,135));textSize=13f;typeface=Typeface.MONOSPACE},LinearLayout.LayoutParams(-2,dp(50)))
        input.apply{hint="enter command";setHintTextColor(Color.rgb(100,105,112));setTextColor(Color.WHITE);textSize=14f;typeface=Typeface.MONOSPACE;setSingleLine(true);background=null;imeOptions=EditorInfo.IME_ACTION_DONE}
        bar.addView(input,LinearLayout.LayoutParams(0,dp(50),1f)); addView(bar,LinearLayout.LayoutParams(-1,dp(58)))
        input.setOnKeyListener{_,key,event->if(event.action!=KeyEvent.ACTION_DOWN)false else when(key){KeyEvent.KEYCODE_ENTER->{execute(input.text.toString());true};KeyEvent.KEYCODE_DPAD_UP->{if(history.isNotEmpty()){historyIndex=(historyIndex-1).coerceAtLeast(0);input.setText(history[historyIndex]);input.setSelection(input.text.length)};true};KeyEvent.KEYCODE_DPAD_DOWN->{if(history.isNotEmpty()){historyIndex=(historyIndex+1).coerceAtMost(history.size);input.setText(if(historyIndex<history.size)history[historyIndex] else "");input.setSelection(input.text.length)};true};else->false}}
        input.setOnEditorActionListener{_,action,_->if(action==EditorInfo.IME_ACTION_DONE){execute(input.text.toString());true}else false}; input.requestFocus()
    }
    private fun execute(command:String){
        val cmd=command.trim();input.setText("");if(cmd.isBlank()){append(prompt);return};if(history.lastOrNull()!=cmd)history.add(cmd);historyIndex=history.size;append("$prompt$cmd\n")
        if(cmd=="clear"){output.text=prompt;return}
        if(cmd=="exit"){append("exit is disabled for the desktop shell.\n$prompt");return}
        if(interactive.matches(cmd)){
            try{com.metmc.os.linux.LinuxGuiLauncher.launch(context as Activity,rootfs,"xterm -fa Monospace -fs 12 -e $cmd");append("Opened interactive Linux program in a real X terminal.\n$prompt")}catch(e:Exception){append("Interactive launch failed: ${e.message}\n$prompt")};return
        }
        Thread{try{
            val bridge="mkdir -p '$rootfs/storage/emulated/0'; mountpoint -q '$rootfs/storage/emulated/0' || mount --bind /storage/emulated/0 '$rootfs/storage/emulated/0' 2>/dev/null || true; cd /root; export HOME=/root; export USER=root; export LOGNAME=root; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export DISPLAY=:100; export XDG_RUNTIME_DIR=/tmp/metmc-runtime; exec /bin/bash -lc ${q(cmd)}"
            val p=ProcessBuilder("su","-c","chroot '$rootfs' /bin/bash -c ${q(bridge)}").redirectErrorStream(true).start(); val r=BufferedReader(InputStreamReader(p.inputStream)); val result=StringBuilder();var line:String?;while(r.readLine().also{line=it}!=null)result.append(line).append('\n');val code=p.waitFor();(context as?Activity)?.runOnUiThread{append(result.toString());if(code!=0)append("[exit $code]\n");append(prompt)}
        }catch(e:Exception){(context as?Activity)?.runOnUiThread{append("Terminal error: ${e.message}\n$prompt")}}}.start()
    }
    private fun append(value:String){output.append(value);(output.parent as?ScrollView)?.post{(output.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)}}
    private fun q(value:String)="'"+value.replace("'","'\\''")+"'"
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
}
