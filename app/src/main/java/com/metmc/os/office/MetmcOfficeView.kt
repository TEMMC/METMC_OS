package com.metmc.os.office

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class MetmcOfficeView(private val context:Context):LinearLayout(context){
    private val title=TextView(context);private val body=FrameLayout(context);private val status=TextView(context)
    init{orientation=VERTICAL;setBackgroundColor(Color.rgb(15,18,24));toolbar();val welcome=TextView(context).apply{text="METMC Office\n\nOpen documents, spreadsheets, presentations, PDFs, images, code and other files from one workspace.";setTextColor(Color.LTGRAY);textSize=16f;gravity=Gravity.CENTER;setPadding(dp(28),dp(28),dp(28),dp(28))};body.addView(welcome,FrameLayout.LayoutParams(-1,-1));addView(body,LinearLayout.LayoutParams(-1,0,1f));status.apply{textSize=11f;setTextColor(Color.rgb(150,158,174));setPadding(dp(12),dp(5),dp(12),dp(5))};addView(status,LinearLayout.LayoutParams(-1,dp(30)))}
    private fun toolbar(){val bar=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(5),dp(8),dp(5));setBackgroundColor(Color.rgb(27,32,42))};title.apply{text="METMC Office";textSize=19f;setTextColor(Color.WHITE)};bar.addView(title,LinearLayout.LayoutParams(0,dp(46),1f));bar.addView(Button(context).apply{text="Open File";isAllCaps=false;setOnClickListener{chooseFile(false)}},LinearLayout.LayoutParams(dp(105),dp(44)));bar.addView(Button(context).apply{text="Open With";isAllCaps=false;setOnClickListener{chooseFile(true)}},LinearLayout.LayoutParams(dp(105),dp(44)));addView(bar,LinearLayout.LayoutParams(-1,dp(56)))}
    private fun chooseFile(external:Boolean){val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)};try{(context as Activity).startActivityForResult(i,if(external)9021 else 9020)}catch(_:Exception){context.startActivity(Intent.createChooser(i,"Open file"))}}
    fun openUri(uri:Uri,external:Boolean){if(external){external(uri);return};val name=queryName(uri);title.text=name;status.text=uri.toString();val l=name.lowercase();when{l.endsWith(".txt")||l.endsWith(".md")||l.endsWith(".log")||l.endsWith(".json")||l.endsWith(".xml")||l.endsWith(".csv")||l.endsWith(".java")||l.endsWith(".kt")||l.endsWith(".py")||l.endsWith(".sh")||l.endsWith(".html")||l.endsWith(".css")->previewText(uri);l.endsWith(".docx")||l.endsWith(".xlsx")||l.endsWith(".pptx")->previewOffice(uri,l);l.endsWith(".jpg")||l.endsWith(".jpeg")||l.endsWith(".png")||l.endsWith(".webp")||l.endsWith(".gif")->previewImage(uri);l.endsWith(".pdf")->external(uri);l.endsWith(".mp3")||l.endsWith(".m4a")||l.endsWith(".aac")||l.endsWith(".wav")||l.endsWith(".ogg")||l.endsWith(".flac")||l.endsWith(".mp4")||l.endsWith(".mkv")||l.endsWith(".webm")||l.endsWith(".3gp")||l.endsWith(".avi")||l.endsWith(".mov")->external(uri);else->binary(uri,name)}}
    private fun previewText(uri:Uri){Thread{val text=try{context.contentResolver.openInputStream(uri)?.use{BufferedReader(InputStreamReader(it)).readText().take(500000)}?:"Unable to read file."}catch(e:Exception){"Unable to read file: ${e.message}"};post{showText(text)}}.start()}
    private fun previewOffice(uri:Uri,name:String){Thread{val text=try{context.contentResolver.openInputStream(uri)?.use{input->val zip=ZipInputStream(input);val wanted=when{ name.endsWith(".docx")->"word/document.xml";name.endsWith(".xlsx")->"xl/sharedStrings.xml";else->"ppt/slides/slide1.xml"};var found="";var entry=zip.nextEntry;while(entry!=null){if(entry.name==wanted){found=zip.readBytes().toString(Charsets.UTF_8);break};entry=zip.nextEntry};strip(found)}?:"Unable to read document."}catch(e:Exception){"Unable to preview Office document: ${e.message}"};post{showText(text)}}.start()}
    private fun previewImage(uri:Uri){body.removeAllViews();val image=ImageView(context).apply{setBackgroundColor(Color.BLACK);scaleType=ImageView.ScaleType.FIT_CENTER;setImageURI(uri)};body.addView(image,FrameLayout.LayoutParams(-1,-1))}
    private fun binary(uri:Uri,name:String){showText("$name\n\nNo built-in renderer is available for this format.\nUse Open With to launch a compatible application.");AlertDialog.Builder(context).setTitle("Unsupported format").setMessage("Open this file with another installed application?").setNegativeButton("Cancel",null).setPositiveButton("Open With"){_,_->external(uri)}.show()}
    private fun external(uri:Uri){try{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply{data=uri;addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Open with"))}catch(_:Exception){Toast.makeText(context,"No compatible application is installed",Toast.LENGTH_LONG).show()}}
    private fun showText(value:String){body.removeAllViews();val scroll=ScrollView(context);val tv=TextView(context).apply{text=value;setTextColor(Color.rgb(225,230,238));textSize=13f;typeface=android.graphics.Typeface.MONOSPACE;setPadding(dp(18),dp(18),dp(18),dp(18));setTextIsSelectable(true)};scroll.addView(tv);body.addView(scroll,FrameLayout.LayoutParams(-1,-1))}
    private fun strip(v:String)=v.replace(Regex("<w:tab[^>]*/>"),"\t").replace(Regex("</(w:p|a:p|p:t|c|row|si|t)>"),"\n").replace(Regex("<[^>]+>"),"").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&apos;", "'").replace(Regex("\\n{3,}"),"\n\n").trim().ifBlank{"No readable text was found in this document."})
    private fun queryName(uri:Uri):String{var n=uri.lastPathSegment?:"Document";try{context.contentResolver.query(uri,arrayOf("_display_name"),null,null,null)?.use{if(it.moveToFirst())n=it.getString(0)?:n}}catch(_:Exception){};return n}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
