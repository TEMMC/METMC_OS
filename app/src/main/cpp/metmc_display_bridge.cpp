#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>

#include <atomic>
#include <thread>
#include <mutex>

#define LOG_TAG "METMC-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class METMCDisplay {
public:
    ANativeWindow* window = nullptr;

    std::atomic<bool> running{false};

    std::thread renderThread;

    std::mutex windowMutex;

    int width = 1280;
    int height = 720;

    void setWindow(JNIEnv* env, jobject surface) {

        std::lock_guard<std::mutex> lock(windowMutex);

        if (window != nullptr) {
            ANativeWindow_release(window);
            window = nullptr;
        }

        if (surface != nullptr) {
            window =
                ANativeWindow_fromSurface(
                    env,
                    surface
                );
        }
    }

    void resize(
            int newWidth,
            int newHeight
    ) {
        width = newWidth;
        height = newHeight;
    }

    void start() {

        if (running.exchange(true)) {
            return;
        }

        renderThread =
            std::thread(
                [this]() {
                    renderLoop();
                }
            );
    }

    void stop() {

        if (!running.exchange(false)) {
            return;
        }

        if (renderThread.joinable()) {
            renderThread.join();
        }
    }

    void renderLoop() {

        LOGI("METMC native display started");

        while (running) {

            {
                std::lock_guard<std::mutex>
                    lock(windowMutex);

                if (window != nullptr) {

                    ANativeWindow_Buffer buffer;

                    if (ANativeWindow_lock(
                            window,
                            &buffer,
                            nullptr
                    ) == 0) {

                        uint32_t* pixels =
                            static_cast<uint32_t*>(
                                buffer.bits
                            );

                        int stride =
                            buffer.stride;

                        for (
                            int y = 0;
                            y < buffer.height;
                            y++
                        ) {

                            for (
                                int x = 0;
                                x < buffer.width;
                                x++
                            ) {

                                uint8_t r =
                                    (x * 255) /
                                    (buffer.width > 0
                                        ? buffer.width
                                        : 1);

                                uint8_t g =
                                    (y * 255) /
                                    (buffer.height > 0
                                        ? buffer.height
                                        : 1);

                                uint8_t b = 32;

                                pixels[
                                    y * stride + x
                                ] =
                                    0xFF000000 |
                                    (r << 16) |
                                    (g << 8) |
                                    b;
                            }
                        }

                        ANativeWindow_unlockAndPost(
                            window
                        );
                    }
                }
            }

            std::this_thread::sleep_for(
                std::chrono::milliseconds(16)
            );
        }

        LOGI("METMC native display stopped");
    }

    ~METMCDisplay() {

        stop();

        std::lock_guard<std::mutex>
            lock(windowMutex);

        if (window != nullptr) {

            ANativeWindow_release(window);

            window = nullptr;
        }
    }
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativeCreate(
        JNIEnv*,
        jclass
) {

    return reinterpret_cast<jlong>(
        new METMCDisplay()
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativeDestroy(
        JNIEnv*,
        jclass,
        jlong handle
) {

    delete reinterpret_cast<METMCDisplay*>(
        handle
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativeSetSurface(
        JNIEnv* env,
        jclass,
        jlong handle,
        jobject surface
) {

    auto* display =
        reinterpret_cast<METMCDisplay*>(
            handle
        );

    if (display != nullptr) {
        display->setWindow(
            env,
            surface
        );
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativeResize(
        JNIEnv*,
        jclass,
        jlong handle,
        jint width,
        jint height
) {

    auto* display =
        reinterpret_cast<METMCDisplay*>(
            handle
        );

    if (display != nullptr) {
        display->resize(
            width,
            height
        );
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativeStart(
        JNIEnv*,
        jclass,
        jlong handle
) {

    auto* display =
        reinterpret_cast<METMCDisplay*>(
            handle
        );

    if (display != nullptr) {
        display->start();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativeStop(
        JNIEnv*,
        jclass,
        jlong handle
) {

    auto* display =
        reinterpret_cast<METMCDisplay*>(
            handle
        );

    if (display != nullptr) {
        display->stop();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativePointer(
        JNIEnv*,
        jclass,
        jlong,
        jfloat,
        jfloat,
        jint
) {
}

extern "C"
JNIEXPORT void JNICALL
Java_com_metmc_os_x11_NativeDisplayView_nativeKey(
        JNIEnv*,
        jclass,
        jlong,
        jint,
        jboolean
) {
}
