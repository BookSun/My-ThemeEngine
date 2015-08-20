package com.lewa.support.graphics;

import android.graphics.Bitmap;
import android.util.Log;

import com.lewa.support.util.ImageUtils;

// TODO: Use stackblur implements.

/**
 * A gaussian blur image filter.
 */
public class GaussianBlurBitmapFilter extends BitmapFilter
{
    private static final String TAG = "GaussianBlurBitmapFilter";
    private static final boolean DEBUG_PERFORMANCE = true;

    private static final float LOW_STRENGTH_BLUR_RADIUS = 14.f;
    private static final float MODERATE_STRENGTH_BLUR_RADIUS = 30.f;
    private static final float HIGH_STRENGTH_BLUR_RADIUS = 62.f;

    // Magic numbers to do the downsize sample and scale the blur radius. This makes the blur effect is same as PS.
    private static final int BLUR_SAMPLE_SIZES[] = {1, 2, 2, 2, 4, 8, 8, 16};
    private static final float BLUR_RADIUS_LEVELS[] = {1, 2, 4, 8, 16, 32, 64, 128};
    private static final float NATIVE_BLUR_RADIUS[] = {0, 1, 3, 6, 7, 8, 14, 16};

    private boolean mIsBlurAlpha = true;

    private float mRadius = 0;
    private float mNativeBlurRadius = 0;
    private int mBlurSampleSize = 1;

    private long mT0;
    private long mT1;
    private String mMessage;

    /**
     * Create a gaussian blur filter with default blur strength.
     * It's suggested to use {@link lewa.graphics.BitmapFilterFactory} to create a BitmapFilter not this method.
     *
     * @see lewa.graphics.BitmapFilterFactory
     */
    public GaussianBlurBitmapFilter() {
        setFilterStrength(getFilterStrength());
    }

    /**
     * Create a gaussian blur filter with specified radius.
     * It's suggested to use {@link lewa.graphics.BitmapFilterFactory} to create a BitmapFilter not this method.
     *
     * @param radius The blur radius. It's has the same effect when using this radius in Photoshop's gaussian blur
     *               filter. The suggest value is [0 ~ 128].
     * @see lewa.graphics.BitmapFilterFactory
     */
    public GaussianBlurBitmapFilter(float radius) {
        setBlurRadius(radius);
    }

    /**
     * Set blur radius.
     *
     * @param radius The blur radius. It's has the similar effect when using this radius in Photoshop's gaussian blur
     *               filter. The suggest value is [0 ~ 128].
     * @see #getBlurRadius()
     */
    public void setBlurRadius(float radius) {
        mRadius = (radius < 0) ? 0 : radius;

        int index = 0;
        int iRadius = (int) mRadius;
        while ((iRadius >>= 1) != 0) {
            index++;
        }
        index = (index >= BLUR_SAMPLE_SIZES.length) ? BLUR_SAMPLE_SIZES.length - 1 : index;

        if (index != 0) {
            // Calculate downsize sample size and scale radius.
            float prevLevel = BLUR_RADIUS_LEVELS[index - 1];
            float currentLevel = BLUR_RADIUS_LEVELS[index];
            mNativeBlurRadius = (mRadius - prevLevel) / (currentLevel - prevLevel) * NATIVE_BLUR_RADIUS[index] +
                    NATIVE_BLUR_RADIUS[index];
            mBlurSampleSize = BLUR_SAMPLE_SIZES[index];
        } else {
            // No blur
            mBlurSampleSize = 1;
            mNativeBlurRadius = 0;
        }
    }

    /**
     * Get current blur radius.
     *
     * @see #setBlurRadius(float)
     */
    public float getBlurRadius() {
        return mRadius;
    }

    /**
     * Set whether blur alpha channel. If not,  the result alpha channel is 255.
     *
     * @param blurAlpha {@code TRUE} enable blur alpha channel; {@code FALSE} disable blur alpha channel.
     * @see #isEnableBlurAlpha()
     */
    public void setEnableBlurAlpha(boolean blurAlpha) {
        mIsBlurAlpha = blurAlpha;
    }

    /**
     * Get whether is blur alpha.
     *
     * @return The default is {@code TRUE}.
     * @see #setEnableBlurAlpha(boolean)
     */
    public boolean isEnableBlurAlpha() {
        return mIsBlurAlpha;
    }

    /**
     * {@inheritDoc}
     *
     * Don't specialized the blur radius directly, but use strength. This is the suggested method.
     */
    @Override
    public void setFilterStrength(int strength) {
        super.setFilterStrength(strength);
        switch (strength) {
            case FILTER_STRENGTH_LOW:
                setBlurRadius(LOW_STRENGTH_BLUR_RADIUS);
                break;
            case FILTER_STRENGTH_MODERATE:
                setBlurRadius(MODERATE_STRENGTH_BLUR_RADIUS);
                break;
            case FILTER_STRENGTH_HIGH:
                setBlurRadius(HIGH_STRENGTH_BLUR_RADIUS);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDownsizeSampleSize() {
        return mBlurSampleSize;
    }

    /**
     * @see BitmapFilter#doFilter(android.graphics.Bitmap, int)
     */
    @Override
    protected Bitmap doFilter(Bitmap source, int preSampleSize) {
        Bitmap result = null;

        if (mBlurSampleSize == 1 && mNativeBlurRadius == 0) {
            result = Bitmap.createBitmap(source); // Return a copy of source Bitmap.
        } else {
            float scaleFactor = (float) preSampleSize / mBlurSampleSize;
            if (scaleFactor == 1) {
                // Don't need downsize sample.
                performanceTickBegin("gaussian blur create result bitmap w=" + source.getWidth() + " h=" + source.getHeight() + ": ");
                result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
                performanceTickEnd();

                performanceTickBegin("gaussian blur filter radius=" + mNativeBlurRadius + ": ");
                //native_gaussianBlur(source, result, mNativeBlurRadius, mIsBlurAlpha, source.isPremultiplied());
                ImageUtils.fastBlur(source, result, (int) mNativeBlurRadius);
                performanceTickEnd();
            } else {
                // Downsize first
                performanceTickBegin("gaussian blur downsize bitmap w=" + source.getWidth() + " h=" + source.getHeight() + ": ");
                int w = (int) (source.getWidth() * scaleFactor);
                int h = (int) (source.getHeight() * scaleFactor);
                w = w > 1 ? w : 1;
                h = h > 1 ? h : 1;
                Bitmap scaledSource = Bitmap.createScaledBitmap(source, w, h, true);
                performanceTickEnd();

                performanceTickBegin("gaussian blur create result bitmap w=" + scaledSource.getWidth() + " h=" + scaledSource.getHeight() + ": ");
                result = Bitmap.createBitmap(scaledSource.getWidth(), scaledSource.getHeight(),
                        Bitmap.Config.ARGB_8888);
                performanceTickEnd();

                performanceTickBegin("gaussian blur filter radius=" + mNativeBlurRadius + ": ");
                //native_gaussianBlur(scaledSource, result, mNativeBlurRadius, mIsBlurAlpha, scaledSource.isPremultiplied());
                ImageUtils.fastBlur(scaledSource, result, (int) mNativeBlurRadius);
                performanceTickEnd();

                scaledSource.recycle();
            }
        }

        result.setHasAlpha(mIsBlurAlpha);
        return result;
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

    /*
    private static native int native_gaussianBlur(Bitmap src, Bitmap dst, float radius, boolean blurAlpha, boolean isPremultiplied);

    static {
        System.loadLibrary("lewa_runtime");
    }
    */
}
