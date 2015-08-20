
package com.lewa.support.graphics;

import android.graphics.Color;

public class ColorFilterUtils {

    static final float COLOR_TO_GRAYSCALE_FACTOR_B = 0.114F;

    static final float COLOR_TO_GRAYSCALE_FACTOR_G = 0.587F;

    static final float COLOR_TO_GRAYSCALE_FACTOR_R = 0.299F;

    private ColorFilterUtils() {
    }

    public static int HsvToRgb(float h, float s, float v) {
        return Color.HSVToColor(new float[] {
                h, s, v
        });
    }

    public static void RgbToHsv(int red, int green, int blue, float hsv[]) {
        Color.colorToHSV(Color.rgb(red, green, blue), hsv);
    }

    public static void RgbToHsv(int color, float hsv[]) {
        Color.colorToHSV(color, hsv);
    }

    public static int HslToRgb(float h, float s, float l) {
        int r;
        int g;
        int b;
        if (s == 0) {
            b = (int) (255 * l);
            g = b;
            r = b;
        } else {
            float q;
            if (l < 0.5)
                q = l * (1 + s);
            else
                q = (l + s) - l * s;
            float p = 2 * l - q;
            float f = h / 360;
            double T[] = new double[] {
                    0.3333333F + f, f, f - 0.3333333F
            };
            for (int i = 0; i < 3; i++) {
                if (T[i] < 0)
                    T[i] = 1 + T[i];
                else if (T[i] > 1)
                    T[i] = T[i] - 1;
                if (6 * T[i] < 1) {
                    T[i] = p + 6 * (q - p) * T[i];
                    continue;
                }
                if (2 * T[i] < 1) {
                    T[i] = q;
                    continue;
                }
                if (3 * T[i] < 2)
                    T[i] = p + 6 * ((q - p) * (0.6666667F - T[i]));
                else
                    T[i] = p;
            }

            r = (int) (255 * T[0]);
            g = (int) (255 * T[1]);
            b = (int) (255 * T[2]);
        }
        return Color.rgb(r, g, b);
    }

    public static int HslToRgb(float hsl[]) {
        return HslToRgb(hsl[0], hsl[1], hsl[2]);
    }

    public static void RgbToHsl(int red, int green, int blue, float hsl[]) {
        float r = (float) red / 255;
        float g = (float) green / 255;
        float b = (float) blue / 255;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float f;
        if (max == min)
            f = 0;
        else if (max == r && g >= b)
            f = (60 * (g - b)) / (max - min);
        else if (max == r && g < b)
            f = 360 + (60 * (g - b)) / (max - min);
        else if (max == g)
            f = 120 + (60 * (b - r)) / (max - min);
        else if (max == b)
            f = 240F + (60F * (r - g)) / (max - min);
        else
            f = 0;
        float f1 = (max + min) / 2;
        float f2;
        if (f1 != 0 && max != min) {
            if (0 < f1 && f1 <= 0.5) {
                f2 = (max - min) / (max + min);
            } else if (f1 == 0.5) {
                f2 = (max - min) / (2.0F - (max + min));
            } else {
                f2 = 0;
            }
        } else {
            f2 = 0.0F;
        }
        hsl[0] = f;
        hsl[1] = f2;
        hsl[2] = f1;
    }

    public static void RgbToHsl(int color, float hsl[]) {
        RgbToHsl(0xff & color >>> 16, 0xff & color >>> 8, color & 0xff, hsl);
    }

    public static float clamp(float min, float value, float max) {
        return value > min ? (value >= max ? max : value) : min;
    }

    public static int clamp(int min, int value, int max) {
        return value > min ? (value >= max ? max : value) : min;
    }

    public static int convertColorToGrayscale(int color) {
        return (int) (COLOR_TO_GRAYSCALE_FACTOR_R * (float) ((0xff0000 & color) >>> 16)
                + COLOR_TO_GRAYSCALE_FACTOR_G * (float) ((0xff00 & color) >>> 8) + COLOR_TO_GRAYSCALE_FACTOR_B
                * (float) (color & 0xff));
    }

    public static int interpolate(int inMin, int inMax, int outMin, int outMax, int value) {
        return (int) ((float) outMin + ((float) value * (float) (outMax - outMin))
                / (float) (inMax - inMin));
    }

    public static void interpolate(float hsl1[], float hsl2[], float amount, float hslOut[]) {
        int size = Math.min(hsl1.length, hsl2.length);
        for (int i = 0; i < size; i++)
            hslOut[i] = hsl1[i] + amount * (hsl2[i] - hsl1[i]);
    }
}
