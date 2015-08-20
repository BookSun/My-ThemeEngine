package com.lewa.support.graphics;

import android.graphics.Bitmap;

/**
 * Abstract class for any image filters.
 * <p/>
 * A BitmapFilter filter a Bitmap. It's will not edit the source Bitmap. The caller should take notice of the filtering
 * performance. Most of filters are resource constrained on both CPU and IO bandwidth.
 * <p/>
 * <b>Notice:</b> The image filter don't make any promise of that the result Bitmap has the same size of the source
 * Bitmap.
 * <p/>
 */
public abstract class BitmapFilter
{
    /**
     * Hint value that suggests the BitmapFilter doing low strength filtering. It's a suggesting value, an BitmapFilter
     * may don't take this value into account.
     */
    public static final int FILTER_STRENGTH_LOW = 1;
    /**
     * Hint value that suggests the BitmapFilter doing moderate strength filtering. It's a suggesting value, an
     * BitmapFilter may don't take this value into account. This strength is the default value.
     */
    public static final int FILTER_STRENGTH_MODERATE = 2;
    /**
     * Hint value that suggests the BitmapFilter doing high strength filtering. It's a suggesting value, an BitmapFilter
     * may don't take this value into account.
     */
    public static final int FILTER_STRENGTH_HIGH = 3;

    private int mStrength = FILTER_STRENGTH_MODERATE;

    /**
     * Set the BitmapFilter's filtering strength.
     *
     * @see #FILTER_STRENGTH_LOW
     * @see #FILTER_STRENGTH_MODERATE
     * @see #FILTER_STRENGTH_HIGH
     * @see #getFilterStrength()
     */
    public void setFilterStrength(int strength) {
        mStrength = strength;
    }

    /**
     * Get the filtering strength.
     *
     * @return The default value is {@link #FILTER_STRENGTH_MODERATE}
     * @see #FILTER_STRENGTH_LOW
     * @see #FILTER_STRENGTH_MODERATE
     * @see #FILTER_STRENGTH_HIGH
     * @see #setFilterStrength(int)
     */
    public int getFilterStrength() {
        return mStrength;
    }

    /**
     * Get the filter downsizing sample size if it has.
     * <p/>
     * Same filters have downsize sampling. The method return they downsizing sample size. If not, it returns 1.
     *
     * @return The filter's downsizing sample size. The default value is 1.
     * @see lewa.graphics.Snapshot#setSampleSize(int)
     */
    public abstract int getDownsizeSampleSize();

    /**
     * Create a new Bitmap contains the filter result of the source Bitmap.
     * <p/>
     * <b>Notice:</b> The caller should manage the return bitmap self. Make sure to call
     * {@link android.graphics.Bitmap#recycle()} as soon as possible, once its content is not needed anymore.
     *
     * @param source The input source Bitmap. The pixel format of the source Bitmap can only be
     *               {@code Bitmap.Config.ARGB_8888}.
     * @return Returns a filtered Bitmap, or {@code null} is any error occurs.
     */
    public Bitmap filterBitmap(Bitmap source) {
        return filterBitmap(source, 1);
    }

    /**
     * Create a new Bitmap contains the filter result of the pre-downsized source Bitmap.
     * <p/>
     * For performance reasons, some source Bitmap may already has downsized sampling from the origin Bitmap. And,
     * some filters downsize sampling themself. If lets the filter known the source Bitmap previous downsizing
     * sample size, it may reduce unnecessary downsizing and improving performance especially when the two sample
     * sizes are the same. When you don't known whether the source Bitmap has downsized of not, let's
     * {@code preSampleSize=1} or use {@link #filterBitmap(android.graphics.Bitmap)}.
     * <p/>
     * <b>Notice:</b> The caller should manage the return bitmap self. Make sure to call
     * {@link android.graphics.Bitmap#recycle()} as soon as possible, once its content is not needed anymore.
     *
     * @param source        The input source Bitmap. The pixel format of the source Bitmap can only be
     *                      {@code Bitmap.Config.ARGB_8888}.
     * @param preSampleSize Source Bitmap previous downsizing sample size. Any value <=1 is treated the same as 1. Note:
     *                      The final sample size is based on powers of 2, any other value will be rounded down to the
     *                      nearest power of 2.
     * @return Returns a filtered Bitmap, or {@code null} is any error occurs.
     * @see lewa.graphics.Snapshot#setSampleSize(int)
     */
    public Bitmap filterBitmap(Bitmap source, int preSampleSize) {
        if (source == null || source.getConfig() != Bitmap.Config.ARGB_8888
                || source.getWidth() < 1 || source.getHeight() < 1) {
            return null;
        }

        return doFilter(source, preSampleSize);
    }

    /**
     * Implement this method to do the filter work.
     *
     * @param source        The same as {@link #filterBitmap(android.graphics.Bitmap, int)}.
     * @param preSampleSize The same as {@link #filterBitmap(android.graphics.Bitmap, int)}.
     * @return The same as {@link #filterBitmap(android.graphics.Bitmap, int)}.
     */
    protected abstract Bitmap doFilter(Bitmap source, int preSampleSize);
}
