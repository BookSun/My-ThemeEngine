package com.lewa.support.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Method;

/**
 * A helper class to snapshot the screen or a view's content into a Bitmap.
 * <p/>
 * The Android framework has Z order window surface system. From the bottom-most Z order to the top-most Z order, the
 * surfaces are:
 * <p/>
 * <ul>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_WALLPAPER}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_PHONE}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_SEARCH_BAR}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_SYSTEM_DIALOG}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_TOAST}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_PRIORITY_PHONE}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_SYSTEM_ALERT}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_INPUT_METHOD}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_INPUT_METHOD_DIALOG}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_KEYGUARD}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_KEYGUARD_DIALOG}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_STATUS_BAR}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_STATUS_BAR_PANEL}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_SYSTEM_OVERLAY}</li>
 * <li>{@link android.view.WindowManager.LayoutParams#TYPE_SYSTEM_ERROR}</li></p>
 * </ul>
 * <p/>
 * The {@link android.view.WindowManager.LayoutParams#TYPE_WALLPAPER} type surface is for wallpaper drawing, it's the
 * bottom of the whole window system. Most of applications are drawn in the {@link
 * android.view.WindowManager.LayoutParams#TYPE_PHONE} type surface. The other types of surface are usually for system
 * ui controlling by the Android framework. Snapshot class can snapshot the whole window surfaces, or some of
 * them by indicating the specific surface type.
 * <p/>
 * The Snapshot class can also snapshot a View. The caller should make sure the View has been layout before the snapshot
 * or it will get a zero size Bitmap.
 * <p/>
 * <b>Notice:</b> Snapshot screen requires the caller to hold the permission {@link
 * android.Manifest.permission#READ_FRAME_BUFFER} and to have this permission the caller should declare
 * "LOCAL_CERTIFICATE :=platform" in its Android.mk.
 * <p/>
 * <b>Notice:</b> The caller should manage the returned bitmap self. Make sure to call {@link
 * android.graphics.Bitmap#recycle()} as soon as possible, once its content is not needed anymore.
 */
public class Snapshot
{
    private static final String TAG = "Snapshot";
    private static final boolean DEBUG_PERFORMANCE = true;

    private static final int TYPE_LAYER_MULTIPLIER = 10000;

    private static final String SURFACE_CLASS_NAME = "android.view.Surface";
    private static final String SURFACE_CONTROL_CLASS_NAME = "android.view.SurfaceControl";

    private static Method mSurfaceScreenshotMethod;
    private static Method mSurfaceScreenshotLayerMethod;
    private static Method mSurfaceControlScreenshotMethod;
    private static Method mSurfaceControlScreenshotLayerMethod;

    private DisplayMetrics mDisplayMetrics;

    private final int mScreenWidth;
    private final int mScreenHeight;

    private int mSampleSize = 1;

    private long mT0;
    private long mT1;
    private String mMessage;

    /**
     * Simple constructor to create a Snapshot.
     */
    public Snapshot() {
        mDisplayMetrics = Resources.getSystem().getDisplayMetrics();
        mScreenWidth = mDisplayMetrics.widthPixels;
        mScreenHeight = mDisplayMetrics.heightPixels;
    }

    /**
     * The sample size is the number of pixels in either dimension that correspond to a single pixel in the result
     * Bitmap.
     * <p/>
     * For example, sample size 4 returns an image that is 1/4 the width/height of the original, Sample size 8 return an
     * image that is 1/8 the width/height of the original. A big sample size's snapshot-ing costs much less time than
     * the small one. It's suggested that uses as big as possible sample size, once the quality is satisfied.
     *
     * @param sampleSize The sample size value. Any value <=1 is treated the same as 1. Note: The final sample size is
     *                   based on powers of 2, any other value will be rounded down to the nearest power of 2.
     * @see #getSampleSize()
     */
    public void setSampleSize(int sampleSize) {
        mSampleSize = 1;
        sampleSize = (sampleSize < 1) ? 1 : sampleSize;
        while ((sampleSize >>= 1) != 0) {
            mSampleSize *= 2;
        }
    }

    /**
     * Get the current sample size.
     *
     * @return The sample size value. The default sample size is 1.
     * @see #setSampleSize(int)
     */
    public int getSampleSize() {
        return mSampleSize;
    }

    /**
     * Snapshot all the surfaces of the screen framebuffer into a bitmap.
     * <p/>
     * This is the same as calling {@code snapshotScreen(0, 0)}.
     *
     * @see Snapshot#snapshotScreen(int, int)
     */
    public Bitmap snapshotScreen() {
        return snapshotScreen(0, 0);
    }

    /**
     * Snapshot sub surfaces of the screen framebuffer into a bitmap.
     * <p>
     * If the minLayer and maxLayer are both zero, it will sample all the surfaces of the screen.
     * </p>
     *
     * @param minLayer The lowest (bottom-most Z order) window surface to include in the screen snapshot. The possible
     *                 values are defined in the {@link android.view.WindowManager.LayoutParams}. Any other values are
     *                 treated as {@link android.view.WindowManager.LayoutParams#TYPE_WALLPAPER}.
     * @param maxLayer The highest (top-most z order) window surface to include in the screen snapshot. The possible
     *                 values are defined in the {@link android.view.WindowManager.LayoutParams}. Any other values are
     *                 treated as {@link android.view.WindowManager.LayoutParams#TYPE_WALLPAPER}.
     * @return Returns a Bitmap containing the screen contents, or {@code null} if an error occurs. Make sure to call
     * {@link android.graphics.Bitmap#recycle()} as soon as possible, once its content is not needed anymore.
     */
    public Bitmap snapshotScreen(int minLayer, int maxLayer) {
        int sampleWidth = mScreenWidth / mSampleSize;
        int sampleHeight = mScreenHeight / mSampleSize;

        sampleWidth = sampleWidth > 0 ? sampleWidth : 1;
        sampleHeight = sampleHeight > 0 ? sampleHeight : 1;

        int minZ = windowTypeToLayerZ(minLayer) * TYPE_LAYER_MULTIPLIER;
        int maxZ = windowTypeToLayerZ(maxLayer) * TYPE_LAYER_MULTIPLIER;

        Bitmap result = null;
        try {
            performanceTickBegin("snapshotScreen(" + sampleWidth + ", " + sampleHeight + "," + minZ + ", " + maxZ + "): ");

            if (Build.VERSION.SDK_INT >= 18) {
                if (minZ == 0 && maxZ == 0) {
                    result = surfaceControlScreenshot(sampleWidth, sampleHeight);
                } else {
                    result = surfaceControlScreenshotLayer(sampleWidth, sampleHeight, minZ, maxZ);
                }
            } else {
                if (minZ == 0 && maxZ == 0) {
                    result = surfaceScreenshot(sampleWidth, sampleHeight);
                } else {
                    result = surfaceScreenshotLayer(sampleWidth, sampleHeight, minZ, maxZ);
                }
            }

            result.setDensity(mDisplayMetrics.densityDpi);

            performanceTickEnd();

        } catch (Exception e) {
            // throw new RuntimeException(e.getMessage());
            result = Bitmap.createBitmap(sampleWidth, sampleHeight, Bitmap.Config.ARGB_8888);
            result.eraseColor(Color.WHITE);
            Log.e(TAG, "snapshotScreen failed width exception: " + e.getMessage());
        }

        return result;
    }

    /**
     * Snapshot a view into a Bitmap, and keep the transparency areas transparent.
     * <p>
     * This is the same as calling {@code snapshotView(view, Color.TRANSPARENT)}.
     * </p>
     *
     * @see Snapshot#snapshotView(android.view.View, int)
     */
    public Bitmap snapshotView(View view) {
        return snapshotView(view, Color.TRANSPARENT);
    }

    /**
     * Snapshot a view into a Bitmap with the indicated background color.
     *
     * @param view            The target {@link android.view.View}.
     * @param backgroundColor The background color of the Bitmap. If the view has transparency areas, these areas will
     *                        be erased using this color.
     * @return Returns a Bitmap containing the view content, or {@code null} if any error occurs. Make sure to call
     * {@link android.graphics.Bitmap#recycle()} as soon as possible, once its content is not needed anymore.
     */
    public Bitmap snapshotView(View view, int backgroundColor) {
        if (view == null) {
            return null;
        }

        int viewWidth = view.getRight() - view.getLeft();
        int viewHeight = view.getBottom() - view.getTop();

        int sampleWidth = viewWidth / mSampleSize;
        int sampleHeight = viewHeight / mSampleSize;
        float canvasScale = 1.f / mSampleSize;

        sampleWidth = sampleWidth > 0 ? sampleWidth : 1;
        sampleHeight = sampleHeight > 0 ? sampleHeight : 1;

        performanceTickBegin("snapshotView " + view +
                " sampleSize = " + mSampleSize +
                " (" + sampleWidth + ", " + sampleHeight + "): ");

        Bitmap bitmap = Bitmap.createBitmap(sampleWidth, sampleHeight, Bitmap.Config.ARGB_8888);
        if (bitmap == null) {
            throw new OutOfMemoryError();
        }
        if ((backgroundColor & 0xff000000) != 0) {
            bitmap.eraseColor(backgroundColor);
        }

        bitmap.setDensity(mDisplayMetrics.densityDpi);

        Canvas canvas = new Canvas(bitmap);
        canvas.scale(view.getScaleX() * canvasScale, view.getScaleY() * canvasScale);
        canvas.translate(-view.getScrollX(), -view.getScrollY());

        view.computeScroll();
        view.draw(canvas);

        canvas.setBitmap(null);

        performanceTickEnd();

        return bitmap;
    }

    private static Bitmap surfaceScreenshot(int width, int height) throws Exception {
        if (mSurfaceScreenshotMethod == null) {
            Class<?> clazz = Class.forName(SURFACE_CLASS_NAME);
            if (clazz != null)
                mSurfaceScreenshotMethod = clazz.getDeclaredMethod("screenshot", Integer.TYPE, Integer.TYPE);
        }

        Object object = mSurfaceScreenshotMethod.invoke(null, width, height);

        if (object != null) {
            return (Bitmap) object;
        }
        return null;

        //return Surface.screenshot(width, height);
    }

    private static Bitmap surfaceScreenshotLayer(int width, int height, int minLayer, int maxLayer) throws Exception {
        if (mSurfaceScreenshotLayerMethod == null) {
            Class<?> clazz = Class.forName(SURFACE_CLASS_NAME);
            if (clazz != null)
                mSurfaceScreenshotLayerMethod = clazz.getDeclaredMethod("screenshot", Integer.TYPE, Integer.TYPE,
                        Integer.TYPE, Integer.TYPE);
        }

        Object object = mSurfaceScreenshotLayerMethod.invoke(null, width, height, minLayer, maxLayer);

        if (object != null) {
            return (Bitmap) object;
        }
        return null;

        //return Surface.screenshot(width, height, minLayer, maxLayer);
    }

    private static Bitmap surfaceControlScreenshot(int width, int height) throws Exception {
        if (mSurfaceControlScreenshotMethod == null) {
            Class<?> clazz = Class.forName(SURFACE_CONTROL_CLASS_NAME);
            if (clazz != null)
                mSurfaceControlScreenshotMethod = clazz.getDeclaredMethod("screenshot", Integer.TYPE, Integer.TYPE);
        }

        Object object = mSurfaceControlScreenshotMethod.invoke(null, width, height);

        if (object != null) {
            return (Bitmap) object;
        }
        return null;
    }

    private static Bitmap surfaceControlScreenshotLayer(int width, int height, int minLayer, int maxLayer) throws Exception {
        if (mSurfaceControlScreenshotLayerMethod == null) {
            Class<?> clazz = Class.forName(SURFACE_CONTROL_CLASS_NAME);
            if (clazz != null)
                mSurfaceControlScreenshotLayerMethod = clazz.getDeclaredMethod("screenshot", Integer.TYPE, Integer.TYPE,
                        Integer.TYPE, Integer.TYPE);
        }

        Object object = mSurfaceControlScreenshotLayerMethod.invoke(null, width, height, minLayer, maxLayer);

        if (object != null) {
            return (Bitmap) object;
        }
        return null;
    }

    /**
     * From com.android.internal.policy.impl.PhoneWindowManager#windowTypeToLayerLw(int)
     */
    private static int windowTypeToLayerZ(int type) {
        switch (type) {
            case WindowManager.LayoutParams.TYPE_WALLPAPER:
                return 2;
            case WindowManager.LayoutParams.TYPE_PHONE:
                return 3;
            case WindowManager.LayoutParams.TYPE_SEARCH_BAR:
                return 4;
            case WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG:
                return 5;
            case WindowManager.LayoutParams.TYPE_TOAST:
                return 6;
            case WindowManager.LayoutParams.TYPE_PRIORITY_PHONE:
                return 7;
            case WindowManager.LayoutParams.TYPE_SYSTEM_ALERT:
                return 9;
            case WindowManager.LayoutParams.TYPE_INPUT_METHOD:
                return 10;
            case WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG:
                return 11;
            case WindowManager.LayoutParams.TYPE_KEYGUARD:
                return 13;
            case WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG:
                return 14;
            case WindowManager.LayoutParams.TYPE_STATUS_BAR:
                return 16;
            case WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL:
                return 17;
            case WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY:
                return 19;
            case WindowManager.LayoutParams.TYPE_SYSTEM_ERROR:
                return 22;
        }
        return 0;
    }

    private void performanceTickBegin(String message) {
        if (DEBUG_PERFORMANCE) {
            mT0 = System.nanoTime();
            mMessage = message;
        }
    }

    private void performanceTickEnd() {
        if (DEBUG_PERFORMANCE) {
            mT1 = System.nanoTime();
            Log.d(TAG, mMessage + (mT1 - mT0) / 1000000.f + "ms");
        }
    }
}
