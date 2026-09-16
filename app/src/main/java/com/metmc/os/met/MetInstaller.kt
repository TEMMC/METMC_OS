package com.metmc.os.met

import android.app.Activity
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

// .met package format for METMC OS.
object MetInstaller {
    private const val ROOTFS = "/data/local/linux/rootfs"

    fun install(activity: Activity, metFile: File, onDone: (success: Boolean, message: String) -> Unit) {
        Thread {
            var staged: File? = null
            try {
                if (!metFile.exists()) {
                    finish(activity,onDone,false,"Install failed: package does not exist: ${metFile.absolutePath}");return@Thread
                }
                val workDir = File(activity.cacheDir, "met_install_${System.currentTimeMillis()}")
                workDir.mkdirs()
                // Shared storage can be inaccessible inside the app/Chroot namespace even with root.
                // Stage through a root cp so .met packages from /storage/emulated/0 are readable.
                staged=File(workDir,"package.met")
                val stage=ProcessBuilder("su","-c","cp ${quote(metFile.absolutePath)} ${quote(staged.absolutePath)} && chmod 600 ${quote(staged.absolutePath)}").redirectErrorStream(true).start()
                val stageOut=stage.inputStream.bufferedReader().readText();val stageCode=stage.waitFor()
                if(stageCode!=0||!staged.exists()){finish(activity,onDone,false,"Install failed: cannot read package from shared storage. $stageOut");return@Thread}
                unzip(staged,workDir)

                val nativeManifest = File(workDir, "T3-Private-Browser/manifest.met")
                if (nativeManifest.exists()) {
                    val values=nativeManifest.readLines().mapNotNull{line->val i=line.indexOf('=');if(i>0)line.substring(0,i).trim() to line.substring(i+1).trim()else null}.toMap()
                    val appId=values["id"].orEmpty();val entry=values["entrypoint"].orEmpty()
                    if(appId.isBlank()||entry.isBlank()){finish(activity,onDone,false,"Invalid native .met package: missing id or entrypoint");return@Thread}
                    val target=File("/data/local/met-apps/$appId");target.parentFile?.mkdirs();if(target.exists())target.deleteRecursively();File(workDir,"T3-Private-Browser").copyRecursively(target,overwrite=true)
                    finish(activity,onDone,true,"OK - installed native METMC app: ${values["name"]?:appId}\nEntry: $entry\nDesktop: native DesktopWindow")
                    return@Thread
                }

                val manifestFile=File(workDir,"manifest.json")
                if(!manifestFile.exists()){finish(activity,onDone,false,"Invalid .met package: no manifest.json found");return@Thread}
                val manifest=JSONObject(manifestFile.readText());val name=manifest.optString("name",metFile.name);val author=manifest.optString("author","Unknown");val platforms=manifest.optJSONObject("platforms")
                if(platforms==null){finish(activity,onDone,false,"Invalid .met package: no platforms defined");return@Thread}
                var installed=false;val results=StringBuilder()
                val androidKey=when{platforms.has("metmc_os")->"metmc_os";platforms.has("android")->"android";else->null}
                if(androidKey!=null){val block=platforms.getJSONObject(androidKey);val entry=block.optString("entry");val apk=File(workDir,entry);if(apk.exists()){val r=installApk(apk);results.append("$androidKey: $r\n");if(r.startsWith("OK"))installed=true}else results.append("$androidKey: entry file not found ($entry)\n")}
                if(platforms.has("linux")){val linux=platforms.getJSONObject("linux");val entry=linux.optString("entry");val script=File(workDir,entry);if(script.exists()){val r=installLinuxScript(name,workDir,entry);results.append("Linux: $r\n");if(r.startsWith("OK"))installed=true}else results.append("Linux: entry file not found ($entry)\n")}
                finish(activity,onDone,installed,"Installed \"$name\" by $author:\n\n${results.toString().trim()}")
            }catch(e:Exception){finish(activity,onDone,false,"Install failed: ${e.message}")}
            finally{try{staged?.parentFile?.deleteRecursively()}catch(_:Exception){}}
        }.start()
    }
    private fun installApk(apk:File):String=try{val p=ProcessBuilder("su","-c","pm install -r ${quote(apk.absolutePath)}").redirectErrorStream(true).start();val o=p.inputStream.bufferedReader().readText();p.waitFor();if(o.contains("Success"))"OK - installed"else"FAILED - $o"}catch(e:Exception){"FAILED - ${e.message}"}
    private fun installLinuxScript(appName:String,workDir:File,entry:String):String=try{val safe=appName.replace(Regex("[^a-zA-Z0-9_-]"),"_");val target="/opt/met-apps/$safe";val copy="mkdir -p $target && cp -r ${quote(workDir.absolutePath)}/linux/. $target/";val install="cd $target && chmod +x ${quote(File(entry).name)} && ./${quote(File(entry).name)}";val p=ProcessBuilder("su","-c","chroot ${quote(ROOTFS)} /bin/bash -c ${quote(copy)}").redirectErrorStream(true).start();p.inputStream.bufferedReader().readText();p.waitFor();val p2=ProcessBuilder("su","-c","chroot ${quote(ROOTFS)} /bin/bash -c ${quote(install)}").redirectErrorStream(true).start();val o=p2.inputStream.bufferedReader().readText();val c=p2.waitFor();if(c==0)"OK - installed to $target"else"FAILED (exit $c) - $o"}catch(e:Exception){"FAILED - ${e.message}"}
    private fun unzip(zipFile:File,targetDir:File){ZipFile(zipFile).use{zip->zip.entries().asSequence().forEach{entry->val out=File(targetDir,entry.name);if(entry.isDirectory)out.mkdirs()else{out.parentFile?.mkdirs();zip.getInputStream(entry).use{input->out.outputStream().use{output->input.copyTo(output)}}}}}}
    private fun finish(activity:Activity,onDone:(Boolean,String)->Unit,success:Boolean,message:String){activity.runOnUiThread{onDone(success,message)}}
    private fun quote(value:String)="'"+value.replace("'","'\\''")+"'"
}
