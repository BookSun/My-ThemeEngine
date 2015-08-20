
package com.lewa.support.graphics;

public class HslFilter implements IBitmapFilter {

    private Filter mHsl;

    private Filter mHsv;

    private void ensureHsl() {
        if (mHsl == null) {
            mHsl = new Filter();
            mHsl.useHsv = false;
        }
    }

    private void ensureHsv() {
        if (mHsv == null) {
            mHsv = new Filter();
            mHsv.useHsv = true;
        }
    }

    public void process(BitmapInfo imgData) {
        if (mHsl != null)
            mHsl.process(imgData);
        if (mHsv != null)
            mHsv.process(imgData);
    }

    public void setHueAdjust(float hue) {
        ensureHsl();
        mHsl.setHueAdjust(hue);
    }

    public void setHueModify(float hue) {
        ensureHsl();
        mHsl.setHueModify(hue);
    }

    public void setLightnessAdjust(float lightness) {
        if (lightness > 0) {
            ensureHsl();
            mHsl.setLightnessAdjust(lightness);
        } else {
            ensureHsv();
            mHsv.setLightnessAdjust(lightness);
        }
    }

    public void setLightnessModify(float lightness) {
        if (lightness > 0) {
            ensureHsl();
            mHsl.setLightnessModify(lightness);
        } else {
            ensureHsv();
            mHsv.setLightnessModify(lightness);
        }
    }

    public void setSaturationAdjust(float saturation) {
        ensureHsl();
        mHsl.setSaturationAdjust(saturation);
    }

    public void setSaturationModify(float saturation) {
        ensureHsl();
        mHsl.setSaturationModify(saturation);
    }

    public static class Filter implements IBitmapFilter {

        private static float sTempTriple[] = new float[3];

        private float mHueAdjust;

        private float mHueModify;

        private float mLightnessAdjust;

        private float mLightnessModify;

        private float mSaturationAdjust;

        private float mSaturationModify;

        public boolean useHsv;

        public void process(BitmapInfo imgData) {
            int width = imgData.width;
            int height = imgData.height;
            int pixels[] = imgData.pixels;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int colorIndex = x + y * width;
                    int rgb = pixels[colorIndex];
                    if (useHsv)
                        ColorFilterUtils.RgbToHsv(rgb, sTempTriple);
                    else
                        ColorFilterUtils.RgbToHsl(rgb, sTempTriple);
                    float h = sTempTriple[0];
                    float s = sTempTriple[1];
                    float l = sTempTriple[2];
                    if (!Float.isNaN(mHueModify))
                        h = mHueModify;
                    else if (!Float.isNaN(mHueAdjust)) {
                        h += mHueAdjust;
                        if (h >= 360)
                            h -= 360;
                        else if (h < 0)
                            h += 360;
                    }
                    if (!Float.isNaN(mSaturationModify))
                        s = mSaturationModify;
                    else if (!Float.isNaN(mSaturationAdjust)) {
                        float f;
                        if (mSaturationAdjust <= 0) {
                            f = s * (1 + mSaturationAdjust);
                        } else {
                            float multipleFactor = Math.min(1, 2F * mSaturationAdjust);
                            float additionFactor = 2 * (mSaturationAdjust - 0.5F);
                            f = s * (1 + multipleFactor);
                            if (additionFactor > 0)
                                f += additionFactor;
                        }
                        s = ColorFilterUtils.clamp(0, f, 1);
                    }
                    if (!Float.isNaN(mLightnessModify))
                        l = mLightnessModify;
                    else if (!Float.isNaN(mLightnessAdjust))
                        if (mLightnessAdjust <= 0)
                            l *= 1 + mLightnessAdjust;
                        else
                            l = 1 - (1 - l) * (1 - mLightnessAdjust);
                    int newRgb = useHsv ? ColorFilterUtils.HsvToRgb(h, s, l) : ColorFilterUtils
                            .HslToRgb(h, s, l);
                    pixels[colorIndex] = 0xffffff & newRgb | 0xff000000 & pixels[colorIndex];
                }

            }

        }

        public void setHueAdjust(float hue) {
            mHueAdjust = ColorFilterUtils.clamp(-180, hue, 180);
        }

        public void setHueModify(float hue) {
            mHueModify = ColorFilterUtils.clamp(0, hue, 359.9999F);
        }

        public void setLightnessAdjust(float lightness) {
            mLightnessAdjust = ColorFilterUtils.clamp(-1, lightness / 100, 1);
        }

        public void setLightnessModify(float lightness) {
            mLightnessModify = ColorFilterUtils.clamp(0, lightness / 100, 1);
        }

        public void setSaturationAdjust(float saturation) {
            mSaturationAdjust = ColorFilterUtils.clamp(-1, saturation / 100, 1);
        }

        public void setSaturationModify(float saturation) {
            mSaturationModify = ColorFilterUtils.clamp(0, saturation / 100, 1);
        }

    }

}
