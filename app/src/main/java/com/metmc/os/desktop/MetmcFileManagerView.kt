package com.metmc.os.desktop

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.widget.*
import java.io.File
import java.util.Locale

class MetmcFileManagerView(private val context: Context) : LinearLayout(context) {
    private enum class Tab { INTERNAL, SD, LINUX, ROOT }
    private var tab=Tab.INTERNAL
    private var internalPath="/storage/emulated/0"
    private var sdPath:String?=null
    private var sdCurrent:String?=null
    private var linuxPath="/"
    private var rootPath="/"
    private val path=TextView(context)
    private val list=LinearLayout(context)
    private val scroll=ScrollView(context)
    private val tabs=ArrayList<Button>()

    init { orientation=VERTICAL;setBackgroundColor(Color.rgb(13,16,22));toolbar();tabBar();pathBar();scroll.addView(list,LayoutParams(-1,-2));addView(scroll,LayoutParams(-1,0,1f));detectSdAndRefresh() }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun toolbar(){val bar=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(6),dp(8),dp(6));setBackgroundColor(Color.rgb(24,28,37))};fun b(t:String,a:()->Unit)=Button(context).apply{text=t;isAllCaps=false;textSize=12f;setTextColor(Color.WHITE);setOnClickListener{a()}};bar.addView(b("‹"){up()},LinearLayout.LayoutParams(dp(48),dp(42)));bar.addView(b("↻ Refresh"){refresh()},LinearLayout.LayoutParams(0,dp(42),1f));bar.addView(b("＋ Folder"){newFolder()},LinearLayout.LayoutParams(dp(96),dp(42)));bar.addView(b("Home"){home()},LinearLayout.LayoutParams(dp(70),dp(42)));addView(bar,LayoutParams(-1,dp(56)))}
    private fun tabBar(){val row=LinearLayout(context).apply{orientation=HORIZONTAL;setBackgroundColor(Color.rgb(18,21,28))};arrayOf("Internal","SD Card","Linux","ROOT").forEachIndexed{i,label->val b=Button(context);b.text=label;b.isAllCaps=false;b.textSize=11f;b.setOnClickListener{tab=Tab.entries[i];redrawTabs();refresh()};tabs.add(b);row.addView(b,LayoutParams(0,dp(46),1f))};addView(row,LayoutParams(-1,dp(46)));redrawTabs()}
    private fun redrawTabs(){tabs.forEachIndexed{i,b->val selected=Tab.entries[i]==tab;b.setTextColor(if(selected)Color.WHITE else Color.rgb(145,152,166));b.setBackgroundColor(if(selected)Color.rgb(50,57,72) else Color.rgb(18,21,28));b.isEnabled=i!=1||sdPath!=null}}
    private fun pathBar(){path.setTextColor(Color.rgb(178,205,255));path.textSize=12f;path.typeface=Typeface.MONOSPACE;path.setSingleLine(true);path.setPadding(dp(12),dp(8),dp(12),dp(8));path.setBackgroundColor(Color.rgb(10,12,17));addView(path,LayoutParams(-1,dp(38)))}
    private fun detectSdAndRefresh(){Thread{val out=runRoot("find /storage -mindepth 1 -maxdepth 1 -type d -print 2>/dev/null");val candidates=out.lines().map{it.trim()}.filter{it.startsWith("/storage/")&&!it.startsWith("/storage/emulated")&&!it.endsWith("self")};sdPath=candidates.firstOrNull();sdCurrent=sdPath;(context as?Activity)?.runOnUiThread{redrawTabs();refresh()}}.start()}
    private fun refresh(){path.text=when(tab){Tab.INTERNAL->internalPath;Tab.SD->sdCurrent?:sdPath?:"/storage (no SD card detected)";Tab.LINUX->"LINUX:$linuxPath";Tab.ROOT->"ROOT:$rootPath"};list.removeAllViews();when(tab){Tab.INTERNAL->loadAndroidPath(internalPath);Tab.SD->sdCurrent?.let{loadAndroidPath(it)}?:show("No removable SD card was detected.");Tab.LINUX->loadLinux(linuxPath);Tab.ROOT->loadAndroidPath(rootPath)}}
    private fun loadAndroidPath(target:String){show("Loading…");Thread{val out=runRoot("find ${quote(target)} -mindepth 1 -maxdepth 1 -printf '%y\\t%p\\t%s\\n' 2>&1");(context as?Activity)?.runOnUiThread{list.removeAllViews();if(out.contains("Permission denied")||out.startsWith("ERROR"))show(out.trim())else renderFind(out)}}.start()}
    private fun loadLinux(target:String){show("Loading Debian…");Thread{val cmd="chroot /data/local/linux/rootfs /bin/sh -c ${quote("find ${quote(target)} -mindepth 1 -maxdepth 1 -printf '%y\\t%p\\t%s\\n' 2>&1")}";val out=runRoot(cmd);(context as?Activity)?.runOnUiThread{list.removeAllViews();if(out.contains("Permission denied")||out.startsWith("ERROR"))show(out.trim())else renderFind(out)}}.start()}
    private fun renderFind(raw:String){val rows=raw.lines().mapNotNull{val p=it.split('\t');if(p.size<3)null else Triple(p[0],p[1],p[2])}.sortedWith(compareByDescending<Triple<String,String,String>>{it.first=="d"}.thenBy{it.second.lowercase(Locale.ROOT)});if(rows.isEmpty()){show("This folder is empty.");return};rows.forEach{(type,full,bytes)->val name=full.substringAfterLast('/');addRow(if(type=="d")"▰"else"□",name,if(type=="d")"Folder" else formatSize(bytes)){if(type=="d")enter(full)else open(full)}}}
    private fun enter(full:String){when(tab){Tab.INTERNAL->internalPath=full;Tab.SD->sdCurrent=full;Tab.LINUX->linuxPath=full;Tab.ROOT->rootPath=full};refresh()}
    private fun addRow(icon:String,name:String,detail:String,action:()->Unit){val row=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(5),dp(14),dp(5));setOnClickListener{action()}};row.addView(TextView(context).apply{text=icon;textSize=22f;gravity=Gravity.CENTER},LayoutParams(dp(44),dp(58)));val texts=LinearLayout(context).apply{orientation=VERTICAL;gravity=Gravity.CENTER_VERTICAL};texts.addView(TextView(context).apply{text=name;textSize=15f;setTextColor(Color.WHITE);maxLines=1});texts.addView(TextView(context).apply{text=detail;textSize=11f;setTextColor(Color.rgb(145,152,166));maxLines=1});row.addView(texts,LayoutParams(0,dp(58),1f));list.addView(row,LayoutParams(-1,dp(68)))}
    private fun show(message:String){list.addView(TextView(context).apply{text=message;textSize=14f;setTextColor(Color.LTGRAY);setPadding(dp(18),dp(22),dp(18),dp(22))},LayoutParams(-1,-2))}
    private fun home(){when(tab){Tab.INTERNAL->internalPath="/storage/emulated/0";Tab.SD->sdCurrent=sdPath;Tab.LINUX->linuxPath="/";Tab.ROOT->rootPath="/"};refresh()}
    private fun up(){when(tab){Tab.INTERNAL->if(internalPath!="/storage/emulated/0")internalPath=parent(internalPath,"/storage/emulated/0");Tab.SD->{val base=sdPath;if(base!=null&&sdCurrent!=null&&sdCurrent!=base)sdCurrent=parent(sdCurrent!!,base)};Tab.LINUX->if(linuxPath!="/")linuxPath=parent(linuxPath,"/");Tab.ROOT->if(rootPath!="/")rootPath=parent(rootPath,"/")};refresh()}
    private fun parent(p:String,root:String):String{val q=p.trimEnd('/');val x=q.substringBeforeLast('/','/');return if(x.length<root.length)root else x.ifBlank{"/"}}
    private fun newFolder(){val input=EditText(context).apply{hint="Folder name"};AlertDialog.Builder(context).setTitle("New folder").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Create"){_,_->val name=input.text.toString().trim();if(name.isBlank()||name.contains('/')||name=="."||name==".."){Toast.makeText(context,"Invalid folder name",Toast.LENGTH_SHORT).show();return@setPositiveButton};val base=when(tab){Tab.INTERNAL->internalPath;Tab.SD->sdCurrent;Tab.LINUX->linuxPath;Tab.ROOT->rootPath};val target=if(tab==Tab.LINUX)"chroot /data/local/linux/rootfs /bin/mkdir -p ${quote("$base/$name")}" else "mkdir -p ${quote("$base/$name")}";Thread{val r=runRoot(target);(context as?Activity)?.runOnUiThread{if(r.startsWith("ERROR"))Toast.makeText(context,r,Toast.LENGTH_LONG).show();refresh()}}}.show()}
    private fun open(full:String){try{val file=File(full);if(file.exists()){context.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(Uri.fromFile(file),"*/*");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)})}else Toast.makeText(context,"File is not directly readable by Android",Toast.LENGTH_SHORT).show()}catch(_:Exception){Toast.makeText(context,"No application can open this file",Toast.LENGTH_SHORT).show()}}
    private fun runRoot(command:String):String=try{val p=ProcessBuilder("su","-c",command).redirectErrorStream(true).start();val text=p.inputStream.bufferedReader().readText();val code=p.waitFor();if(code==0)text else "ERROR: $text"}catch(e:Exception){"ERROR: ${e.message}"}
    private fun quote(s:String)="'"+s.replace("'","'\\''")+"'"
    private fun formatSize(v:String):String{val n=v.toLongOrNull()?:0;return when{n>=1073741824->String.format(Locale.US,"%.1f GB",n/1073741824.0);n>=1048576->String.format(Locale.US,"%.1f MB",n/1048576.0);n>=1024->String.format(Locale.US,"%.1f KB",n/1024.0);else->"$n B"}}
}
