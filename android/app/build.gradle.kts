import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseSigningPropertiesFile = rootProject.file("signing/keystore.properties")
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.isFile) releaseSigningPropertiesFile.inputStream().use(::load)
}

val preparePatchedXlorie by tasks.registering {
    val upstream = layout.projectDirectory.file("src/main/xlorie-upstream/arm64-v8a/libXlorie.so")
    val patched = layout.buildDirectory.file("generated/metmcJniLibs/arm64-v8a/libXlorie.so")
    inputs.file(upstream)
    outputs.file(patched)
    doLast {
        val bytes = upstream.asFile.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(digest == "f9d60f48c5af45971571783166437b25dfb84cf7a2233c0265c49f652a9fe851") { "libXlorie.so changed; review and update the cursor patch offsets" }
        fun patch(offset: Int, expected: ByteArray, replacement: ByteArray) {
            check(bytes.copyOfRange(offset, offset + expected.size).contentEquals(expected)) { "Unexpected libXlorie instruction at 0x${offset.toString(16)}" }
            replacement.copyInto(bytes, offset)
        }
        patch(0x0e3288, byteArrayOf(0x2b, 0x00, 0x80.toByte(), 0x52), byteArrayOf(0xeb.toByte(), 0x03, 0x1f, 0x2a))
        patch(0x0e32b8, byteArrayOf(0x80.toByte(), 0x01, 0x3f, 0xd6.toByte()), byteArrayOf(0x1f, 0x20, 0x03, 0xd5.toByte()))
        patched.get().asFile.apply { parentFile.mkdirs(); writeBytes(bytes) }
    }
}

val metmcVersionCode = providers.gradleProperty("METMC_VERSION_CODE").orNull?.toIntOrNull() ?: 1
val metmcVersionName = providers.gradleProperty("METMC_VERSION_NAME").orNull ?: "0.1.0-alpha"

android {
    namespace = "com.metmc.os"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.metmc.os"
        minSdk = 28
        targetSdk = 35
        versionCode = metmcVersionCode
        versionName = metmcVersionName
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
            jniLibs.srcDir(layout.buildDirectory.dir("generated/metmcJniLibs"))
        }
    }
    signingConfigs {
        if (releaseSigningPropertiesFile.isFile) {
            create("release") {
                storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningPropertiesFile.isFile) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true; aidl = true }
    androidResources { noCompress += "tgz" }
}

tasks.named("preBuild").configure {
    dependsOn(preparePatchedXlorie)
    doLast {
        // Reuse a complete Debian installation already present on the rooted phone.
        // The patch is idempotent and accepts both common bash locations.
        val rootfsManager = file("src/main/java/com/metmc/os/runtime/RootfsManager.kt")
        var rootfsText = rootfsManager.readText()
        val oldRootfsProbe = """test -f ${'$'}EXISTING_DEBIAN_ROOTFS/etc/debian_version && \" +
                    \"test -x ${'$'}EXISTING_DEBIAN_ROOTFS/usr/bin/bash"""
        val newRootfsProbe = """test -f ${'$'}EXISTING_DEBIAN_ROOTFS/etc/debian_version && \" +
                    \"(test -x ${'$'}EXISTING_DEBIAN_ROOTFS/usr/bin/bash || \" +
                    \"test -x ${'$'}EXISTING_DEBIAN_ROOTFS/bin/bash)"""
        if (oldRootfsProbe in rootfsText) {
            rootfsText = rootfsText.replace(oldRootfsProbe, newRootfsProbe)
        }

        // /data/local/linux/rootfs is outside the Android app sandbox. File.exists()
        // against that path therefore gives false negatives even after apt succeeds.
        // Run Phosh and terminal readiness probes through the already-rooted chroot.
        val phoshOld = """        val schema = File(
            rootfsDir,
            \"usr/share/glib-2.0/schemas/org.gnome.settings-daemon.peripherals.gschema.xml\"
        )
        val svgLoader = File(
            rootfsDir,
            \"usr/lib/aarch64-linux-gnu/gdk-pixbuf-2.0/2.10.0/loaders/libpixbufloader-svg.so\"
        )
        if (schema.exists() && svgLoader.exists()) return true
"""
        val phoshNew = """        val runtimeCheck = chrootManager.execChroot(
            \"test -f /usr/share/glib-2.0/schemas/org.gnome.settings-daemon.peripherals.gschema.xml && \" +
                \"test -f /usr/lib/aarch64-linux-gnu/gdk-pixbuf-2.0/2.10.0/loaders/libpixbufloader-svg.so\"
        )
        if (runtimeCheck == 0) return true
"""
        if (phoshOld in rootfsText) rootfsText = rootfsText.replace(phoshOld, phoshNew)

        val phoshResultOld = """        if (result == 0 && schema.exists() && svgLoader.exists()) {
            Log.i(TAG, \"Phosh runtime schemas and SVG loader repaired\")
            return true
        }
"""
        val phoshResultNew = """        if (result == 0 && chrootManager.execChroot(
                \"test -f /usr/share/glib-2.0/schemas/org.gnome.settings-daemon.peripherals.gschema.xml && \" +
                    \"test -f /usr/lib/aarch64-linux-gnu/gdk-pixbuf-2.0/2.10.0/loaders/libpixbufloader-svg.so\"
            ) == 0) {
            Log.i(TAG, \"Phosh runtime schemas and SVG loader repaired\")
            return true
        }
"""
        if (phoshResultOld in rootfsText) rootfsText = rootfsText.replace(phoshResultOld, phoshResultNew)

        val terminalOld = """        val console = File(rootfsDir, \"usr/bin/kgx\")
        val legacyXterm = File(rootfsDir, \"usr/bin/xterm\")
        val legacyGnomeTerminal = File(rootfsDir, \"usr/bin/gnome-terminal\")
        if (console.exists() && !legacyXterm.exists() && !legacyGnomeTerminal.exists()) return true
"""
        val terminalNew = """        val terminalCheck = chrootManager.execChroot(
            \"test -x /usr/bin/kgx && ! test -e /usr/bin/xterm && ! test -e /usr/bin/gnome-terminal\"
        )
        if (terminalCheck == 0) return true
"""
        if (terminalOld in rootfsText) rootfsText = rootfsText.replace(terminalOld, terminalNew)
        rootfsManager.writeText(rootfsText)

        val chrootManager = file("src/main/java/com/metmc/os/runtime/ChrootManager.kt")
        val chrootText = chrootManager.readText()
        val oldChrootProbe = """test -f ${'$'}{existing.absolutePath}/etc/debian_version && \" +
                \"test -x ${'$'}{existing.absolutePath}/usr/bin/bash && \" +
                \"echo METMC_EXISTING_DEBIAN_ROOTFS"""
        val newChrootProbe = """test -f ${'$'}{existing.absolutePath}/etc/debian_version && \" +
                \"(test -x ${'$'}{existing.absolutePath}/usr/bin/bash || \" +
                \"test -x ${'$'}{existing.absolutePath}/bin/bash) && \" +
                \"echo METMC_EXISTING_DEBIAN_ROOTFS"""
        if (oldChrootProbe in chrootText) {
            chrootManager.writeText(chrootText.replace(oldChrootProbe, newChrootProbe))
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.annotation:annotation:1.8.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.json:json:20240303")
}
