package com.metmc.os.media

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.view.Gravity
import android.widget.*
import java.io.File
import java.util.Locale

class MetmcMediaPlayerView(private val context: Context) : LinearLayout(context) {
    private var player: MediaPlayer?=null
    private val title=TextView(context)
    private val status=TextView(context)
    private val seek=SeekBar(context)
    private val video=VideoView(context)
    private val play=Button(context)
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val videoExt=setOf("mp4","mkv","webm","3gp","avi","mov")
    private val mediaExt=setOf("mp3","m4a","aac","wav","ogg","flac","mp4","mkv","webm","3gp","avi","mov")

    init {
        orientation=VERTICAL;setBackgroundColor(Color.rgb(10,12,17))
        val top=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(6),dp(8),dp(6));setBackgroundColor(Color.rgb(25,29,37))}
        top.addView(TextView(context).apply{text="METMC Media Player";textSize=16f;setTextColor(Color.WHITE)},LayoutParams(0,dp(48),1f))
        top.addView(Button(context).apply{text="Library";isAllCaps=false;setOnClickListener{scanLibrary()}},LayoutParams(dp(88),dp(44)));addView(top,LayoutParams(-1,dp(60)))
        video.setBackgroundColor(Color.BLACK);video.visibility=GONE;addView(video,LayoutParams(-1,0,1f))
        title.text="No media selected";title.textSize=17f;title.setTextColor(Color.WHITE);title.gravity=Gravity.CENTER;addView(title,LayoutParams(-1,dp(48)))
        status.text="Tap Library to find music and videos";status.textSize=12f;status.setTextColor(Color.LTGRAY);status.gravity=Gravity.CENTER;addView(status,LayoutParams(-1,dp(28)))
        seek.max=1000;addView(seek,LayoutParams(-1,dp(40)))
        val controls=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(6),dp(4),dp(6),dp(8))};controls.addView(control("↶"){seekBy(-10000)},buttonParams());play.text="▶ Play";play.isAllCaps=false;play.setOnClickListener{toggle()};controls.addView(play,LinearLayout.LayoutParams(0,dp(46),1f));controls.addView(control("■"){stop()},buttonParams());controls.addView(control("↷"){seekBy(10000)},buttonParams());addView(controls,LayoutParams(-1,dp(62)))
        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar,p:Int,fromUser:Boolean){if(fromUser)player?.let{it.seekTo(it.duration*p/1000)}};override fun onStartTrackingTouch(s:SeekBar){};override fun onStopTrackingTouch(s:SeekBar){}})
    }

    private fun scanLibrary(){status.text="Scanning Internal Storage and SD Card…";Thread{val roots=ArrayList<File>();roots.add(File("/storage/emulated/0"));try{val p=ProcessBuilder("su","-c","find /storage -mindepth 1 -maxdepth 1 -type d 2>/dev/null").start();p.inputStream.bufferedReader().readLines().filter{!it.startsWith("/storage/emulated")&&!it.endsWith("self")}.forEach{roots.add(File(it))}}catch(_:Exception){};val files=ArrayList<File>();fun walk(f:File,depth:Int){if(depth>5||files.size>=300)return;val a=try{f.listFiles()}catch(_:Exception){null}?:return;for(x in a){if(x.isDirectory)walk(x,depth+1)else if(x.extension.lowercase(Locale.ROOT) in mediaExt)files.add(x)}};roots.forEach{walk(it,0)};(context as?android.app.Activity)?.runOnUiThread{showLibrary(files.distinctBy{it.absolutePath}.sortedBy{it.name.lowercase(Locale.ROOT)})}}.start()}
    private fun showLibrary(files:List<File>){val box=LinearLayout(context).apply{orientation=VERTICAL;setPadding(dp(10),dp(8),dp(10),dp(8))};box.addView(TextView(context).apply{text="Media Library • ${files.size} files";textSize=15f;setTextColor(Color.WHITE);setPadding(dp(6),dp(6),dp(6),dp(10))});val scroll=ScrollView(context);val list=LinearLayout(context).apply{orientation=VERTICAL};files.forEach{f->list.addView(Button(context).apply{text="${if(isVideo(f))"▣"else"♫"}  ${f.name}";isAllCaps=false;gravity=Gravity.START or Gravity.CENTER_VERTICAL;setTextColor(Color.WHITE);setOnClickListener{openFile(f)}},LayoutParams(-1,dp(52)))};if(files.isEmpty())list.addView(TextView(context).apply{text="No supported media files found.";setTextColor(Color.LTGRAY);setPadding(dp(12),dp(20),dp(12),dp(20))});scroll.addView(list);box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val d=android.app.Dialog(context);d.setContentView(box);d.show();d.window?.setLayout(dp(600),dp(460))}
    private fun openFile(file:File){if(!file.exists()){status.text="File is unavailable";return};title.text=file.name;release();if(isVideo(file)){video.visibility=VISIBLE;video.setVideoPath(file.absolutePath);video.setOnPreparedListener{mp->player=mp;status.text=format(mp.duration);play.text="❚❚ Pause";mp.start();updateProgress()};video.setOnErrorListener{_,_,_->status.text="Video format could not be played";true};video.setOnCompletionListener{play.text="▶ Play"}}else{video.visibility=GONE;try{player=MediaPlayer();player!!.setDataSource(file.absolutePath);player!!.setOnPreparedListener{mp->status.text=format(mp.duration);play.text="❚❚ Pause";mp.start();updateProgress()};player!!.setOnErrorListener{_,_,_->status.text="Audio format could not be played";true};player!!.setOnCompletionListener{play.text="▶ Play"};player!!.prepareAsync()}catch(e:Exception){status.text="Cannot open media: ${e.message}"}}}
    private fun isVideo(f:File)=f.extension.lowercase(Locale.ROOT) in videoExt
    private fun toggle(){player?.let{if(it.isPlaying){it.pause();play.text="▶ Play"}else{it.start();play.text="❚❚ Pause";updateProgress()}}}
    private fun stop(){player?.let{try{it.pause();it.seekTo(0)}catch(_:Exception){}};play.text="▶ Play";seek.progress=0}
    private fun seekBy(ms:Int){player?.let{it.seekTo((it.currentPosition+ms).coerceIn(0,it.duration))}}
    private fun updateProgress(){val p=player?:return;if(!p.isPlaying)return;if(p.duration>0)seek.progress=p.currentPosition*1000/p.duration;handler.postDelayed({updateProgress()},500)}
    private fun release(){try{player?.release()}catch(_:Exception){};player=null;video.stopPlayback()}
    override fun onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);release();super.onDetachedFromWindow()}
    private fun format(ms:Int)="${ms/60000}:${String.format(Locale.US,"%02d",(ms/1000)%60)}"
    private fun control(t:String,a:()->Unit)=Button(context).apply{text=t;textSize=18f;isAllCaps=false;setOnClickListener{a()}}
    private fun buttonParams()=LinearLayout.LayoutParams(dp(60),dp(46)).apply{marginStart=dp(3);marginEnd=dp(3)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
