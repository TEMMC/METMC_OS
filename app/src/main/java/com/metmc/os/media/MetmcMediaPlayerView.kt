package com.metmc.os.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.widget.*
import java.io.File
import java.util.Locale

class MetmcMediaPlayerView(private val context: Context) : LinearLayout(context) {
    private var player: android.media.MediaPlayer?=null
    private val title=TextView(context)
    private val status=TextView(context)
    private val seek=SeekBar(context)
    private val video=VideoView(context)
    private val play=Button(context)
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val mediaExt=setOf("mp3","m4a","aac","wav","ogg","flac","mp4","mkv","webm","3gp","avi","mov")
    private val videoExt=setOf("mp4","mkv","webm","3gp","avi","mov")

    init{orientation=VERTICAL;setBackgroundColor(Color.rgb(9,12,17));build()}
    private fun build(){
        val top=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(6),dp(8),dp(6));setBackgroundColor(Color.rgb(24,29,37))}
        top.addView(TextView(context).apply{text="METMC Media Player";textSize=17f;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,dp(48),1f))
        top.addView(button("Open"){openPicker()},LinearLayout.LayoutParams(dp(78),dp(44)))
        top.addView(button("Library"){scanLibrary()},LinearLayout.LayoutParams(dp(84),dp(44)))
        addView(top,LayoutParams(-1,dp(60)))
        video.setBackgroundColor(Color.BLACK);video.visibility=GONE;addView(video,LayoutParams(-1,0,1f))
        title.text="No media selected";title.textSize=17f;title.setTextColor(Color.WHITE);title.gravity=Gravity.CENTER;title.maxLines=1;addView(title,LayoutParams(-1,dp(48)))
        status.text="Open a file or scan your music and video library";status.textSize=12f;status.setTextColor(Color.LTGRAY);status.gravity=Gravity.CENTER;addView(status,LayoutParams(-1,dp(30)))
        seek.max=1000;addView(seek,LayoutParams(-1,dp(40)))
        val controls=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(8),dp(4),dp(8),dp(8))}
        controls.addView(button("−10s"){seekBy(-10000)},controlParams());play.text="▶ Play";play.isAllCaps=false;play.setOnClickListener{toggle()};controls.addView(play,LinearLayout.LayoutParams(0,dp(46),1f));controls.addView(button("Stop"){stop()},controlParams());controls.addView(button("+10s"){seekBy(10000)},controlParams());addView(controls,LayoutParams(-1,dp(62)))
        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar,p:Int,fromUser:Boolean){if(fromUser)player?.let{if(it.duration>0)it.seekTo(it.duration*p/1000)}};override fun onStartTrackingTouch(s:SeekBar){};override fun onStopTrackingTouch(s:SeekBar){}})
    }
    private fun button(text:String,a:()->Unit)=Button(context).apply{this.text=text;isAllCaps=false;textSize=12f;setTextColor(Color.WHITE);minWidth=0;minimumWidth=0;setOnClickListener{a()}}
    private fun controlParams()=LinearLayout.LayoutParams(dp(70),dp(46)).apply{marginStart=dp(3);marginEnd=dp(3)}
    private fun openPicker(){val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="audio/*";putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("audio/*","video/*"));addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)};try{(context as Activity).startActivityForResult(i,9010)}catch(_:Exception){Toast.makeText(context,"File picker unavailable",Toast.LENGTH_SHORT).show()}}
    fun openUri(uri:Uri){try{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};release();title.text=uri.lastPathSegment?.substringAfterLast('/')?:"Media";video.visibility=GONE;try{val p=android.media.MediaPlayer();p.setDataSource(context,uri);player=p;p.setOnPreparedListener{m->status.text=format(m.duration);play.text="❚❚ Pause";m.start();updateProgress()};p.setOnCompletionListener{play.text="▶ Play";seek.progress=0};p.setOnErrorListener{_,_,_->status.text="This audio format could not be played";true};p.prepareAsync()}catch(e:Exception){status.text="Cannot open media: ${e.message}"}}
    private fun scanLibrary(){status.text="Scanning media…";Thread{val roots=ArrayList<String>();roots.add("/storage/emulated/0");try{val p=ProcessBuilder("su","-c","for d in /storage/*; do [ -d \"$d\" ] && case \"$d\" in /storage/emulated|/storage/self) ;; *) echo \"$d\";; esac; done").start();roots.addAll(p.inputStream.bufferedReader().readLines())}catch(_:Exception){};val files=ArrayList<File>();fun walk(f:File,depth:Int){if(depth>6||files.size>=500)return;val a=try{f.listFiles()}catch(_:Exception){null}?:return;a.forEach{x->if(x.isDirectory)walk(x,depth+1)else if(x.extension.lowercase(Locale.ROOT) in mediaExt)files.add(x)}};roots.distinct().forEach{walk(File(it),0)};(context as?Activity)?.runOnUiThread{showLibrary(files.distinctBy{it.absolutePath}.sortedBy{it.name.lowercase(Locale.ROOT)})}}.start()}
    private fun showLibrary(files:List<File>){val box=LinearLayout(context).apply{orientation=VERTICAL;setPadding(dp(10),dp(8),dp(10),dp(8))};box.addView(TextView(context).apply{text="Media Library • ${files.size} files";textSize=16f;setTextColor(Color.WHITE);setPadding(dp(6),dp(6),dp(6),dp(10))});val scroll=ScrollView(context);val list=LinearLayout(context).apply{orientation=VERTICAL};files.forEach{f->list.addView(button("${if(f.extension.lowercase(Locale.ROOT) in videoExt)"▣"else"♫"}  ${f.name}"){openFile(f)},LayoutParams(-1,dp(52)))};if(files.isEmpty())list.addView(TextView(context).apply{text="No supported media files found.";setTextColor(Color.LTGRAY);setPadding(dp(12),dp(20),dp(12),dp(20))});scroll.addView(list);box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));AlertDialogCompat.show(context,box)}
    private fun openFile(file:File){if(!file.exists()){status.text="File is unavailable";return};release();title.text=file.name;if(file.extension.lowercase(Locale.ROOT) in videoExt){video.visibility=VISIBLE;video.setVideoPath(file.absolutePath);video.setOnPreparedListener{m->player=m;status.text=format(m.duration);play.text="❚❚ Pause";m.start();updateProgress()};video.setOnCompletionListener{play.text="▶ Play"};video.setOnErrorListener{_,_,_->status.text="Video format could not be played";true}}else{video.visibility=GONE;try{val p=android.media.MediaPlayer();player=p;p.setDataSource(file.absolutePath);p.setOnPreparedListener{m->status.text=format(m.duration);play.text="❚❚ Pause";m.start();updateProgress()};p.setOnCompletionListener{play.text="▶ Play"};p.setOnErrorListener{_,_,_->status.text="Audio format could not be played";true};p.prepareAsync()}catch(e:Exception){status.text="Cannot open media: ${e.message}"}}}
    private fun toggle(){player?.let{if(it.isPlaying){it.pause();play.text="▶ Play"}else{it.start();play.text="❚❚ Pause";updateProgress()}}}
    private fun stop(){player?.let{try{it.pause();it.seekTo(0)}catch(_:Exception){}};play.text="▶ Play";seek.progress=0}
    private fun seekBy(ms:Int){player?.let{if(it.duration>0)it.seekTo((it.currentPosition+ms).coerceIn(0,it.duration))}}
    private fun updateProgress(){val p=player?:return;if(!p.isPlaying)return;if(p.duration>0)seek.progress=p.currentPosition*1000/p.duration;handler.postDelayed({updateProgress()},500)}
    private fun release(){try{player?.release()}catch(_:Exception){};player=null;video.stopPlayback()}
    override fun onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);release();super.onDetachedFromWindow()}
    private fun format(ms:Int)="${ms/60000}:${String.format(Locale.US,"%02d",(ms/1000)%60)}"
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}

private object AlertDialogCompat {
    fun show(context:Context,content:android.view.View){val d=android.app.Dialog(context);d.setContentView(content);d.show();d.window?.setLayout((600*context.resources.displayMetrics.density).toInt(),(460*context.resources.displayMetrics.density).toInt())}
}
