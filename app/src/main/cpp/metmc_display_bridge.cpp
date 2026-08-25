#include <jni.h>
#include <android/native_window.h>
#include <android/log.h>

#define LOG_TAG "METMC_DISPLAY"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static ANativeWindow *g_window = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_metmc_os_linux_MetmcDisplayBridge_nativeStart(
        JNIEnv *env,
        jobject thiz,
        jobject surface,
        jint width,
        jint height) {

    if (surface == nullptr) {
        LOGE("Surface is null");
        return JNI_FALSE;
    }

    g_window = ANativeWindow_fromSurface(env, surface);

    if (g_window == nullptr) {
        LOGE("Unable to acquire Android surface");
        return JNI_FALSE;
    }

    ANativeWindow_setBuffersGeometry(
        g_window,
        width,
        height,
        WINDOW_FORMAT_RGBA_8888
    );

    LOGI(
        "METMC display bridge started: %dx%d",
        width,
        height
    );

    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_linux_MetmcDisplayBridge_nativeStop(
        JNIEnv *env,
        jobject thiz) {

    if (g_window != nullptr) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }

    LOGI("METMC display bridge stopped");
}
