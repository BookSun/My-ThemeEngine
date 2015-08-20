#include <jni.h>
#include <string.h>
#include <math.h>
#include <stdio.h>
#include <android/log.h>
#include <android/bitmap.h>

#define LOG_TAG "ImageUtils"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define ALPHA_MIN 230
#define TRUE 1
#define FALSE 0

typedef struct {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t alpha;
} rgba;

static jfieldID rLeft;
static jfieldID rTop;
static jfieldID rRight;
static jfieldID rBottom;

jint native_find_side(JNIEnv* env, jclass clazz, jobject bitmap, jboolean isMin)
{
    AndroidBitmapInfo  info;
    void*              pixels;
    int ret;
    int w, h, s;
    int edge = -1;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return edge;
    }

    // Check image
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        LOGE("==> %d %d", info.format, info.format);
        return edge;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    w = info.width;
    h = info.height;
    s = info.stride;

    int top = getPadding(pixels, s, w, h, TRUE, FALSE);
    if (top >= 0) {
        int bottom = getPadding(pixels, s, w, h, TRUE, TRUE);
        if (bottom >= 0) {
            int left = getPadding(pixels, s, w, h, FALSE, FALSE);
            if (left >= 0) {
                int right = getPadding(pixels, s, w, h, FALSE, TRUE);
                if (right >= 0)
                    edge = isMin ? min(right - left, bottom - top) : max(right - left, bottom - top);
            }
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return edge;
}


void setRect(JNIEnv* env, jobject rect, int l, int t, int r, int b)
{
    (*env)->SetIntField(env,rect,rLeft,l);
    (*env)->SetIntField(env,rect,rTop,t);
    (*env)->SetIntField(env,rect,rRight,r);
    (*env)->SetIntField(env,rect,rBottom,b);
}

void native_find_edge(JNIEnv* env, jclass clazz, jobject bitmap,jobject rect)
{
    AndroidBitmapInfo  info;
    void*              pixels;
    int ret;
    int w, h, s;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        LOGE("==> %d %d", info.format, info.format);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    w = info.width;
    h = info.height;
    s = info.stride;

    setRect(env,
            rect,
            getPadding(pixels, s, w, h, FALSE, FALSE),
            getPadding(pixels, s, w, h, TRUE, FALSE),
            getPadding(pixels, s, w, h, FALSE, TRUE),
            getPadding(pixels, s, w, h, TRUE, TRUE)
            );

    AndroidBitmap_unlockPixels(env, bitmap);
}

int getPadding(void* pixels, int s, int w, int h, int vertical, int reverse)
{
    int x, y;
    rgba * line;
    if (vertical) {
        if (reverse) {
            for (y = h - 1; y > h / 3; y--) {
                line = (rgba*)((char*)pixels + y * s);
                for (x = w - 1; x >= 0; x--) {
                    if (line[x].alpha > ALPHA_MIN) {
                        return y + 1;
                    }
                }
            }
            return h;
        } else {
            for (y = 0; y < h / 3; y++) {
                line = (rgba*)((char*)pixels + y * s);
                for (x = 0; x < w; x++) {
                    if (line[x].alpha > ALPHA_MIN) {
                        return y;
                    }
                }
            }
        }
    } else {
        if (reverse) {
            for (x = w - 1; x > w / 3; x--) {
                for (y = h - 1; y >= 0; y--) {
                    line = (rgba*)((char*)pixels + y * s);
                    if (line[x].alpha > ALPHA_MIN) {
                        return x + 1;
                    }
                }
            }
            return w;
        } else {
            for (x = 0; x < w / 3; x++) {
                for (y = 0; y < h; y++) {
                    line = (rgba*)((char*)pixels + y * s);
                    if (line[x].alpha > ALPHA_MIN) {
                        return x;
                    }
                }
            }
        }
    }
    return 0;
}

void native_blur(JNIEnv* env, jclass clazz, jobject bitmapIn, jobject bitmapOut, jint radius) {
    LOGI("Blurring bitmap...");

    // Properties
    AndroidBitmapInfo   infoIn;
    void*               pixelsIn;
    AndroidBitmapInfo   infoOut;
    void*               pixelsOut;

    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, bitmapIn, &infoIn)) < 0 || (ret = AndroidBitmap_getInfo(env, bitmapOut, &infoOut)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoIn.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        LOGE("==> %d %d", infoIn.format, infoOut.format);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmapIn, &pixelsIn)) < 0 || (ret = AndroidBitmap_lockPixels(env, bitmapOut, &pixelsOut)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    int h = infoIn.height;
    int w = infoIn.width;

    LOGI("Image size is: %i %i", w, h);

    rgba* input = (rgba*) pixelsIn;
    rgba* output = (rgba*) pixelsOut;

    int wm = w - 1;
    int hm = h - 1;
    int wh = w * h;
    int whMax = max(w, h);
    int div = radius + radius + 1;

    uint8_t r[wh];
    uint8_t g[wh];
    uint8_t b[wh];
    int rsum, gsum, bsum, x, y, i, yp, yi, yw;
    rgba p;
    int vmin[whMax];

    int divsum = (div + 1) >> 1;
    divsum *= divsum;
    int dv[256 * divsum];
    for (i = 0; i < 256 * divsum; i++) {
        dv[i] = (i / divsum);
    }

    yw = yi = 0;

    uint8_t stack[div][3];
    int stackpointer;
    int stackstart;
    int rbs;
    int ir;
    int ip;
    int r1 = radius + 1;
    int routsum, goutsum, boutsum;
    int rinsum, ginsum, binsum;

    for (y = 0; y < h; y++) {
        rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
        for (i = -radius; i <= radius; i++) {
            p = input[yi + min(wm, max(i, 0))];

            ir = i + radius; // same as sir

            stack[ir][0] = p.red;
            stack[ir][1] = p.green;
            stack[ir][2] = p.blue;
            rbs = r1 - abs(i);
            rsum += stack[ir][0] * rbs;
            gsum += stack[ir][1] * rbs;
            bsum += stack[ir][2] * rbs;
            if (i > 0) {
                rinsum += stack[ir][0];
                ginsum += stack[ir][1];
                binsum += stack[ir][2];
            } else {
                routsum += stack[ir][0];
                goutsum += stack[ir][1];
                boutsum += stack[ir][2];
            }
        }
        stackpointer = radius;

        for (x = 0; x < w; x++) {

            r[yi] = dv[rsum];
            g[yi] = dv[gsum];
            b[yi] = dv[bsum];

            rsum -= routsum;
            gsum -= goutsum;
            bsum -= boutsum;

            stackstart = stackpointer - radius + div;
            ir = stackstart % div; // same as sir

            routsum -= stack[ir][0];
            goutsum -= stack[ir][1];
            boutsum -= stack[ir][2];

            if (y == 0) {
                vmin[x] = min(x + radius + 1, wm);
            }
            p = input[yw + vmin[x]];

            stack[ir][0] = p.red;
            stack[ir][1] = p.green;
            stack[ir][2] = p.blue;

            rinsum += stack[ir][0];
            ginsum += stack[ir][1];
            binsum += stack[ir][2];

            rsum += rinsum;
            gsum += ginsum;
            bsum += binsum;

            stackpointer = (stackpointer + 1) % div;
            ir = (stackpointer) % div; // same as sir

            routsum += stack[ir][0];
            goutsum += stack[ir][1];
            boutsum += stack[ir][2];

            rinsum -= stack[ir][0];
            ginsum -= stack[ir][1];
            binsum -= stack[ir][2];

            yi++;
        }
        yw += w;
    }
    for (x = 0; x < w; x++) {
        rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
        yp = -radius * w;
        for (i = -radius; i <= radius; i++) {
            yi = max(0, yp) + x;

            ir = i + radius; // same as sir

            stack[ir][0] = r[yi];
            stack[ir][1] = g[yi];
            stack[ir][2] = b[yi];

            rbs = r1 - abs(i);

            rsum += r[yi] * rbs;
            gsum += g[yi] * rbs;
            bsum += b[yi] * rbs;

            if (i > 0) {
                rinsum += stack[ir][0];
                ginsum += stack[ir][1];
                binsum += stack[ir][2];
            } else {
                routsum += stack[ir][0];
                goutsum += stack[ir][1];
                boutsum += stack[ir][2];
            }

            if (i < hm) {
                yp += w;
            }
        }
        yi = x;
        stackpointer = radius;
        for (y = 0; y < h; y++) {
            output[yi].red = dv[rsum];
            output[yi].green = dv[gsum];
            output[yi].blue = dv[bsum];
            output[yi].alpha = 255;

            rsum -= routsum;
            gsum -= goutsum;
            bsum -= boutsum;

            stackstart = stackpointer - radius + div;
            ir = stackstart % div; // same as sir

            routsum -= stack[ir][0];
            goutsum -= stack[ir][1];
            boutsum -= stack[ir][2];

            if (x == 0) vmin[y] = min(y + r1, hm) * w;
            ip = x + vmin[y];

            stack[ir][0] = r[ip];
            stack[ir][1] = g[ip];
            stack[ir][2] = b[ip];

            rinsum += stack[ir][0];
            ginsum += stack[ir][1];
            binsum += stack[ir][2];

            rsum += rinsum;
            gsum += ginsum;
            bsum += binsum;

            stackpointer = (stackpointer + 1) % div;
            ir = stackpointer; // same as sir

            routsum += stack[ir][0];
            goutsum += stack[ir][1];
            boutsum += stack[ir][2];

            rinsum -= stack[ir][0];
            ginsum -= stack[ir][1];
            binsum -= stack[ir][2];

            yi += w;
        }
    }

    // Unlocks everything
    AndroidBitmap_unlockPixels(env, bitmapIn);
    AndroidBitmap_unlockPixels(env, bitmapOut);

    LOGI("Bitmap blurred.");
}

int min(int a, int b) {
    return a > b ? b : a;
}

int max(int a, int b) {
    return a > b ? a : b;
}

static JNINativeMethod gNativeMethods[] = {
    { "native_blur", "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;I)V", (void*) native_blur },
    { "native_find_edge", "(Landroid/graphics/Bitmap;Landroid/graphics/Rect;)V", (void*) native_find_edge },
    { "native_find_side", "(Landroid/graphics/Bitmap;Z)I", (void*) native_find_side },
};

static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static int fillFields(JNIEnv* env) {
    jclass clazz;
    clazz=(*env)->FindClass(env, "android/graphics/Rect");
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    rLeft=(*env)->GetFieldID(env,clazz,"left","I");
    rTop=(*env)->GetFieldID(env,clazz,"top","I");
    rRight=(*env)->GetFieldID(env,clazz,"right","I");
    rBottom=(*env)->GetFieldID(env,clazz,"bottom","I");
    if (rLeft == NULL || rTop == NULL || rRight == NULL || rBottom == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    if(!registerNativeMethods(env, "com/lewa/support/util/ImageUtils", gNativeMethods, sizeof(gNativeMethods) / sizeof(gNativeMethods[0]))) {
        return -1;
    }
    if(!fillFields(env)) {
        return -1;
    }
    return JNI_VERSION_1_4;
}
