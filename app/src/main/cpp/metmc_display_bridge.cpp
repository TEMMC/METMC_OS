#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <thread>
#include <atomic>
#include <vector>
#include <cstring>

#define LOG_TAG "METMC_DISPLAY"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static ANativeWindow *g_window = nullptr;
static std::thread g_thread;
static std::atomic<bool> g_running(false);

static int g_width = 0;
static int g_height = 0;

static void renderLoop() {
    while (g_running) {
        if (g_window == nullptr || g_width <= 0 || g_height <= 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
            continue;
        }

        ANativeWindow_Buffer buffer;

        if (ANativeWindow_lock(g_window, &buffer, nullptr) == 0) {

            uint8_t *dst =
                static_cast<uint8_t *>(buffer.bits);

            for (int y = 0; y < buffer.height; ++y) {
                uint32_t *row =
                    reinterpret_cast<uint32_t *>(
                        dst + y * buffer.stride * 4
                    );

                for (int x = 0; x < buffer.width; ++x) {
                    /*
                     * Temporary desktop test pattern.
                     * This proves the Android Surface pipeline works.
                     * Linux X11 pixels will replace this framebuffer.
                     */
                    uint8_t r =
                        static_cast<uint8_t>((x * 255) /
                        (buffer.width > 1 ? buffer.width - 1 : 1));

                    uint8_t g =
                        static_cast<uint8_t>((y * 255) /
                        (buffer.height > 1 ? buffer.height - 1 : 1));

                    uint8_t b = 35;

                    row[x] =
                        (0xFFu << 24) |
                        (static_cast<uint32_t>(b) << 16) |
                        (static_cast<uint32_t>(g) << 8) |
                        r;
                }
            }

            ANativeWindow_unlockAndPost(g_window);
        }

        std::this_thread::sleep_for(
            std::chrono::milliseconds(33)
        );
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_metmc_os_linux_MetmcDisplayBridge_nativeStart(
        JNIEnv *env,
        jobject,
        jobject surface,
        jint width,
        jint height) {

    if (!surface) {
        LOGE("Surface is null");
        return JNI_FALSE;
    }

    if (g_running) {
        g_running = false;

        if (g_thread.joinable())
            g_thread.join();
    }

    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }

    g_window =
        ANativeWindow_fromSurface(env, surface);

    if (!g_window) {
        LOGE("Unable to acquire Android Surface");
        return JNI_FALSE;
    }

    g_width = width;
    g_height = height;

    ANativeWindow_setBuffersGeometry(
        g_window,
        width,
        height,
        WINDOW_FORMAT_RGBA_8888
    );

    g_running = true;
    g_thread = std::thread(renderLoop);

    LOGI(
        "METMC Surface bridge started %dx%d",
        width,
        height
    );

    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_linux_MetmcDisplayBridge_nativeStop(
        JNIEnv *,
        jobject) {

    g_running = false;

    if (g_thread.joinable())
        g_thread.join();

    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }

    LOGI("METMC Surface bridge stopped");
}
