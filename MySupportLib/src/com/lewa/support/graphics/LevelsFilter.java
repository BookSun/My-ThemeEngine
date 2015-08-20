
package com.lewa.support.graphics;

public class LevelsFilter implements IBitmapFilter {

    private float mInputMiddle = 1;

    private float mInputMax = 255;

    private float mInputMin = 0;

    private float mOutputMax = 255;

    private float mOutputMin = 0;

    private boolean mIsFilterR = true;

    private boolean mIsFilterG = true;

    private boolean mIsFilterB = true;

    private static int interpolate(float start, float gamma, float end, float outStart,
            float outEnd, int inputValue) {
        if ((float) inputValue <= start)
            return (int) outStart;
        if ((float) inputValue >= end)
            return (int) outEnd;
        if (gamma == 1) {
            return (int) (outStart + (((float) inputValue - start) * (outEnd - outStart))
                    / (end - start));
        } else {
            float inputRange = end - start;
            float outRange = outEnd - outStart;
            float factor = ((float) inputValue - start) / inputRange;
            return (int) (outStart + outRange * (1 - (float) Math.pow(1 - factor, gamma)));
        }
    }

    public void process(BitmapInfo imgData) {
        int width = imgData.width;
        int height = imgData.height;
        int pixels[] = imgData.pixels;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int colorIndex = x + y * width;
                int argb = pixels[colorIndex];
                int r = (0xff0000 & argb) >>> 16;
                int g = (0xff00 & argb) >>> 8;
                int b = 0xff & argb;
                if (mIsFilterR)
                    r = interpolate(mInputMin, mInputMiddle, mInputMax, mOutputMin, mOutputMax, r);
                if (mIsFilterG)
                    g = interpolate(mInputMin, mInputMiddle, mInputMax, mOutputMin, mOutputMax, g);
                if (mIsFilterB)
                    b = interpolate(mInputMin, mInputMiddle, mInputMax, mOutputMin, mOutputMax, b);
                pixels[colorIndex] = b | (r << 16 | g << 8) | 0xff000000 & argb;
            }
        }
    }

    public void setChannel(String channel) {
        mIsFilterR = false;
        mIsFilterG = false;
        mIsFilterB = false;
        if ("r".equalsIgnoreCase(channel) || "red".equalsIgnoreCase(channel)) {
            mIsFilterR = true;
        } else if ("g".equalsIgnoreCase(channel) || "green".equalsIgnoreCase(channel)) {
            mIsFilterG = true;
        } else if ("b".equalsIgnoreCase(channel) || "blue".equalsIgnoreCase(channel)) {
            mIsFilterB = true;
        } else {
            mIsFilterR = true;
            mIsFilterG = true;
            mIsFilterB = true;
        }
    }

    public void setInputMax(float value) {
        mInputMax = ColorFilterUtils.clamp(2, value, 255);
    }

    public void setInputMiddle(float value) {
        mInputMiddle = ColorFilterUtils.clamp(0.0001F, value, 9.9999F);
    }

    public void setInputMin(float value) {
        mInputMin = ColorFilterUtils.clamp(0, value, 253F);
    }

    public void setOutputMax(float value) {
        mOutputMax = ColorFilterUtils.clamp(0, value, 255);
    }

    public void setOutputMin(float value) {
        mOutputMin = ColorFilterUtils.clamp(0, value, 255);
    }
}
