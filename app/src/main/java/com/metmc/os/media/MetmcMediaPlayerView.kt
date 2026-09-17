package com.metmc.os.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.DragEvent
import android.view.Gravity
import android.widget.*
import java.io.File
import java.util.Locale

class MetmcMediaPlayerView(private val context: Context) : LinearLayout(context) {
    private var player: android.media.MediaPlayer?=null
    private val title=TextView(context); private val status=TextView(context); private val seek=SeekBar(context); private val video=VideoView(context); private val play=Button(context); private val dropHint=TextView(context)
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val mediaExt=setOf("mp3","m4a","aac","wav","ogg","flac","mp4","mkv","webm","3gp","avi","mov")
    private val videoExt=setOf("mp4","mkv","webm","3gp","avi","mov")
    init{orientation=VERTICAL;setBackgroundColor(Color.rgb(12,14,18));build()}
    private fun build(){
        val top=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(7),dp(10),dp(7));background=panel()}
        top.addView(TextView(context).apply{text="♫";textSize=23f;setTextColor(Color.WHITE);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(42),dp(44)))
        top.addView(TextView(context).apply{text="METMC Media Player";textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL},LinearLayout.LayoutParams(0,dp(44),1f))
        top.addView(actionButton("Open"){openPicker()},LinearLayout.LayoutParams(dp(66),dp(40)));top.addView(actionButton("Library"){scanLibrary()},LinearLayout.LayoutParams(dp(76),dp(40)))
        addView(top,LinearLayout.LayoutParams(-1,dp(58)))
        val stage=FrameLayout(context).apply{setBackgroundColor(Color.BLACK);setOnDragListener(dragListener())};video.setBackgroundColor(Color.BLACK);video.visibility=GONE;stage.addView(video,FrameLayout.LayoutParams(-1,-1))
        dropHint.apply{text="Open a video or audio file";textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(155,162,175));gravity=Gravity.CENTER;setOnDragListener(dragListener())};stage.addView(dropHint,FrameLayout.LayoutParams(-1,-1));addView(stage,LinearLayout.LayoutParams(-1,0,1f))
        title.apply{text="No media selected";textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),0,dp(16),0);maxLines=1};addView(title,LinearLayout.LayoutParams(-1,dp(42)))
        status.apply{text="Ready";textSize=11f;setTextColor(Color.LTGRAY);gravity=Gravity.CENTER};addView(status,LinearLayout.LayoutParams(-1,dp(22)))
        seek.max=1000;addView(seek,LinearLayout.LayoutParams(-1,dp(34)))
        val controls=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(10),dp(4),dp(10),dp(10))};controls.addView(actionButton("−10"){seekBy(-10000)},LinearLayout.LayoutParams(dp(72),dp(46)))
        play.apply{text="▶";textSize=18f;isAllCaps=false;setTextColor(Color.WHITE);minWidth=0;minimumWidth=0;background=accentButton();setOnClickListener{toggle()}};controls.addView(play,LinearLayout.LayoutParams(0,dp(46),1f));controls.addView(actionButton("Stop"){stop()},LinearLayout.LayoutParams(dp(72),dp(46)));controls.addView(actionButton("+10"){seekBy(10000)},LinearLayout.LayoutParams(dp(72),dp(46)));addView(controls,LinearLayout.LayoutParams(-1,dp(64)))
        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar,p:Int,fromUser:Boolean){if(fromUser)player?.let{if(it.duration>0)it.seekTo(it.duration*p/1000)}};override fun onStartTrackingTouch(s:SeekBar){};override fun onStopTrackingTouch(s:SeekBar){}})
    }
    private fun panel()=GradientDrawable().apply{setColor(Color.rgb(24,27,34));cornerRadius=dp(10).toFloat()};private fun accentButton()=GradientDrawable().apply{setColor(Color.rgb(52,60,76));cornerRadius=dp(9).toFloat()}
    private fun actionButton(label:String,action:()->Unit)=Button(context).apply{text=label;textSize=12f;isAllCaps=false;setTextColor(Color.WHITE);minWidth=0;minimumWidth=0;background=panel();setOnClickListener{action()}}
    private fun dragListener()=android.view.View.OnDragListener{_,e->when(e.action){DragEvent.ACTION_DRAG_STARTED->e.clipDescription?.hasMimeType("text/uri-list")==true||e.clipDescription?.hasMimeType("*/*")==true;DragEvent.ACTION_DROP->{e.clipData?.getItemAt(0)?.uri?.let{openUri(it);true}?:false};else->true}}
    private fun openPicker(){val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("audio/*","video/*"));addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)};try{(context as Activity).startActivityForResult(i,9010)}catch(e:Exception){status.text="File picker unavailable: ${e.message}"}}
    fun openUri(uri:Uri){try{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};val name=uri.lastPathSegment?.substringAfterLast('/')?:"Media";if(name.substringAfterLast('.',"").lowercase(Locale.ROOT) in videoExt)openVideoUri(uri,name) else openAudioUri(uri,name)}
    private fun openVideoUri(uri:Uri,name:String){release();title.text=name;dropHint.visibility=GONE;video.visibility=VISIBLE;status.text="Loading video…";video.setVideoURI(uri);video.setOnPreparedListener{m->player=m;status.text=format(m.duration);play.text="❚❚";m.start();updateProgress()};video.setOnCompletionListener{play.text="▶"};video.setOnErrorListener{_,_,_->status.text="Video format is not supported by Android";true}}
    private fun openAudioUri(uri:Uri,name:String){release();title.text=name;dropHint.visibility=GONE;video.visibility=GONE;status.text="Loading audio…";try{val m=android.media.MediaPlayer();player=m;m.setDataSource(context,uri);m.setOnPreparedListener{p->status.text=format(p.duration);play.text="❚❚";p.start();updateProgress()};m.setOnCompletionListener{play.text="▶"};m.setOnErrorListener{_,_,_->status.text="Audio format is not supported";true};m.prepareAsync()}catch(e:Exception){status.text="Cannot open media: ${e.message}"}}
    private fun scanLibrary(){status.text="Scanning library…";Thread{val files=ArrayList<File>();fun walk(f:File,d:Int){if(d>7||files.size>=500)return;(try{f.listFiles()}catch(_:Exception){null})?.forEach{if(it.isDirectory)walk(it,d+1)else if(it.extension.lowercase(Locale.ROOT)in mediaExt)files.add(it)}};walk(File("/storage/emulated/0"),0);(context as?Activity)?.runOnUiThread{showLibrary(files.sortedBy{it.name.lowercase(Locale.ROOT)})}}.start()}
    private fun showLibrary(files:List<File>){val box=LinearLayout(context).apply{orientation=VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));background=panel()};box.addView(TextView(context).apply{text="Media Library";textSize=18f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});val scroll=ScrollView(context);val list=LinearLayout(context).apply{orientation=VERTICAL};files.forEach{f->list.addView(actionButton("${if(f.extension.lowercase(Locale.ROOT)in videoExt)"▣" else "♫"}  ${f.name}"){openFile(f)},LinearLayout.LayoutParams(-1,dp(48)))};if(files.isEmpty())list.addView(TextView(context).apply{text="No media found";setTextColor(Color.LTGRAY)});scroll.addView(list);box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val d=android.app.Dialog(context);d.setContentView(box);d.show()}
    private fun openFile(f:File){if(f.extension.lowercase(Locale.ROOT)in videoExt){release();title.text=f.name;dropHint.visibility=GONE;video.visibility=VISIBLE;status.text="Loading video…";video.setVideoPath(f.absolutePath);video.setOnPreparedListener{m->player=m;status.text=format(m.duration);play.text="❚❚";m.start();updateProgress()};video.setOnErrorListener{_,_,_->status.text="Video format is not supported";true}}else{try{openAudioUri(Uri.fromFile(f),f.name)}catch(_:Exception){status.text="Cannot open audio"}}}
    private fun toggle(){player?.let{if(it.isPlaying){it.pause();play.text="▶"}else{it.start();play.text="❚❚";updateProgress()}}};private fun stop(){player?.let{try{it.pause();it.seekTo(0)}catch(_:Exception){}};play.text="▶";seek.progress=0};private fun seekBy(ms:Int){player?.let{if(it.duration>0)it.seekTo((it.currentPosition+ms).coerceIn(0,it.duration))}};private fun updateProgress(){val m=player?:return;if(!m.isPlaying)return;if(m.duration>0)seek.progress=m.currentPosition*1000/m.duration;handler.postDelayed({updateProgress()},500)};private fun release(){try{player?.release()}catch(_:Exception){};player=null;video.stopPlayback()};override fun onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);release();super.onDetachedFromWindow()};private fun format(ms:Int)="${ms/60000}:${String.format(Locale.US,"%02d",(ms/1000)%60)}";private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
