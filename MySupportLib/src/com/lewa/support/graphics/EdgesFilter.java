
package com.lewa.support.graphics;

import java.lang.reflect.Array;

public class EdgesFilter implements IBitmapFilter {

    private static float sTempHsl[] = new float[3];

    public void process(BitmapInfo imgData) {
        int width = imgData.width;
        int height = imgData.height;
        int pixels[] = imgData.pixels;
        int ai[] = {
                width, height
        };
        int luminance[][] = (int[][]) Array.newInstance(Integer.TYPE, ai);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = pixels[x + y * width];
                luminance[x][y] = ColorFilterUtils.convertColorToGrayscale(color);
            }
        }

        for (int i = 1; i < height - 1; i++) {
            for (int x = 1; x < width - 1; x++) {
                int colorIndex = x + i * width;
                int grayX = ((((-luminance[x - 1][i - 1] + luminance[x - 1][2 + (i - 1)]) - 2 * luminance[1 + (x - 1)][i - 1]) + 2 * luminance[1 + (x - 1)][2 + (i - 1)]) - luminance[2 + (x - 1)][i - 1])
                        + luminance[2 + (x - 1)][2 + (i - 1)];
                int grayY = (luminance[x - 1][i - 1] + 2 * luminance[x - 1][1 + (i - 1)] + luminance[x - 1][2 + (i - 1)])
                        - luminance[2 + (x - 1)][i - 1]
                        - 2
                        * luminance[2 + (x - 1)][1 + (i - 1)]
                        - luminance[2 + (x - 1)][2 + (i - 1)];
                int magnitude = 255 - ColorFilterUtils.clamp(0, Math.abs(grayX) + Math.abs(grayY),
                        255);
                ColorFilterUtils.RgbToHsl(pixels[colorIndex], sTempHsl);
                sTempHsl[2] = (float) magnitude / 255F;
                int newRgb = ColorFilterUtils.HslToRgb(sTempHsl);
                pixels[colorIndex] = 0xffffff & newRgb | 0xff000000 & pixels[colorIndex];
            }

        }

    }

}
