#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <mutex>
#include <cstring>

#define LOG_TAG "METMC_DISPLAY"

static ANativeWindow *g_window = nullptr;
static std::mutex g_mutex;

static int g_width = 1280;
static int g_height = 720;
static bool g_running = false;

static void releaseWindow() {
    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_linux_LinuxDisplayBridge_nativeAttach(
        JNIEnv *env,
        jclass,
        jobject surface) {

    std::lock_guard<std::mutex> lock(g_mutex);

    releaseWindow();

    if (!surface)
        return;

    g_window = ANativeWindow_fromSurface(env, surface);

    if (g_window) {
        ANativeWindow_setBuffersGeometry(
            g_window,
            g_width,
            g_height,
            WINDOW_FORMAT_RGBA_8888
        );
    }

    __android_log_print(
        ANDROID_LOG_INFO,
        LOG_TAG,
        "Linux Surface attached"
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_linux_LinuxDisplayBridge_nativeDetach(
        JNIEnv *,
        jclass) {

    std::lock_guard<std::mutex> lock(g_mutex);

    g_running = false;
    releaseWindow();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_linux_LinuxDisplayBridge_nativeResize(
        JNIEnv *,
        jclass,
        jint width,
        jint height) {

    std::lock_guard<std::mutex> lock(g_mutex);

    if (width > 0)
        g_width = width;

    if (height > 0)
        g_height = height;

    if (g_window) {
        ANativeWindow_setBuffersGeometry(
            g_window,
            g_width,
            g_height,
            WINDOW_FORMAT_RGBA_8888
        );
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_linux_LinuxDisplayBridge_nativeStart(
        JNIEnv *,
        jclass) {

    std::lock_guard<std::mutex> lock(g_mutex);

    g_running = true;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_linux_LinuxDisplayBridge_nativeStop(
        JNIEnv *,
        jclass) {

    std::lock_guard<std::mutex> lock(g_mutex);

    g_running = false;
}
