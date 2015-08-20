package com.lewa.support.graphics;

/**
 * A helper factory for create a BitmapFilter.
 * <p/>
 * It's suggested to use this factory to create a BitmapFilter but not to create directly.
 */
public class BitmapFilterFactory
{
    public static final int GAUSSIAN_BLUR = 0;

    /**
     * Create a BitmapFilter by type name.
     *
     * @return Return {@code NULL} when type mismatching.
     */
    public static BitmapFilter createFilter(int type) {
        switch (type) {
            case GAUSSIAN_BLUR:
                return new GaussianBlurBitmapFilter();
            default:
                return null;
        }
    }
}
