
package com.lewa.support.graphics;

import android.graphics.Bitmap;

public class BitmapInfo {

    int width;

    int height;

    int pixels[];

    public BitmapInfo(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new int[width * height];
    }

    public static BitmapInfo getBitmapInfo(Bitmap bitmap) {
        BitmapInfo info = new BitmapInfo(bitmap.getWidth(), bitmap.getHeight());
        bitmap.getPixels(info.pixels, 0, info.width, 0, 0, info.width, info.height);
        return info;
    }

    public static Bitmap getBitmap(BitmapInfo info) {
        return Bitmap.createBitmap(info.pixels, info.width, info.height,
                android.graphics.Bitmap.Config.ARGB_8888);
    }
}
