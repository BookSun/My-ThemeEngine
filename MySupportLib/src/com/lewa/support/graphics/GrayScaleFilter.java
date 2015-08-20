
package com.lewa.support.graphics;

import android.graphics.Color;

public class GrayScaleFilter implements IBitmapFilter {

    private int mBlackColor = Color.BLACK;

    private int mWhiteColor = Color.WHITE;

    public void process(BitmapInfo imgData) {
        int width = imgData.width;
        int height = imgData.height;
        int pixels[] = imgData.pixels;
        int aBlack = mBlackColor >>> 24;
        int aWhite = mWhiteColor >>> 24;
        int rBlack = 0xff & mBlackColor >>> 16;
        int rWhite = 0xff & mWhiteColor >>> 16;
        int gBlack = 0xff & mBlackColor >>> 8;
        int gWhite = 0xff & mWhiteColor >>> 8;
        int bBlack = 0xff & mBlackColor;
        int bWhite = 0xff & mWhiteColor;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int colorIndex = x + y * width;
                int argb = pixels[colorIndex];
                int luminance = ColorFilterUtils.convertColorToGrayscale(argb);
                int newA = ColorFilterUtils.interpolate(0, 255, aBlack, aWhite, luminance);
                int newR = ColorFilterUtils.interpolate(0, 255, rBlack, rWhite, luminance);
                int newG = ColorFilterUtils.interpolate(0, 255, gBlack, gWhite, luminance);
                int newB = ColorFilterUtils.interpolate(0, 255, bBlack, bWhite, luminance);
                pixels[colorIndex] = newB
                        | ((newA * (argb >>> 24)) / 255 << 24 | newR << 16 | newG << 8);
            }
        }
    }

    public void setBlackColor(String strColor) {
        mBlackColor = Color.parseColor(strColor);
    }

    public void setWhiteColor(String strColor) {
        mWhiteColor = Color.parseColor(strColor);
    }
}
