
package com.lewa.support.graphics;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

public class BlendFilter implements IBitmapFilter {

    static final String TAG = "BlendFilter";

    static final int BLEND_TYPE_NORMAL = 0;

    static final int BLEND_TYPE_MULTIPLY = 1;

    static final int BLEND_TYPE_SCREEN = 2;

    static final int BLEND_TYPE_DARKEN = 3;

    static final int BLEND_TYPE_LIGHTEN = 4;

    static final int BLEND_TYPE_DIFFERENCE = 5;

    static final int BLEND_TYPE_LINEAR_DODGE = 6;

    static final int BLEND_TYPE_LINEAR_BURN = 7;

    static final int BLEND_TYPE_OVERLAY = 8;

    static final int BLEND_TYPE_COLOR_DODGE = 9;

    static final int BLEND_TYPE_COLOR_BURN = 10;

    static final int BLEND_TYPE_OPACITY = 11;

    static final int BLEND_TYPE_HARD_LIGHT = 12;

    static final int BLEND_TYPE_SOFT_LIGHT = 13;

    static final int BLEND_TYPE_VIVID_LIGHT = 14;

    static final int BLEND_TYPE_LINEAR_LIGHT = 15;

    static final int BLEND_TYPE_PIN_LIGHT = 16;

    static final int BLEND_TYPE_HARD_MIX = 17;

    static final int BLEND_TYPE_EXCLUSION = 18;

    static final int BLEND_TYPE_HUE = 19;

    static final int BLEND_TYPE_SATURATION = 20;

    static final int BLEND_TYPE_COLOR = 21;

    static final int BLEND_TYPE_LUMINOSITY = 22;

    static final int BLEND_TYPE_DIVIDE = 23;

    static final int BLEND_TYPE_SUBTRACT = 24;

    private int mBlendType = 0;

    private BitmapInfo mInput;

    private SoftReference<SimpleImmutableEntry<Integer, BitmapInfo>> mInputImageCache;

    private boolean mIsInputImageOnTop = true;

    private PorterDuff.Mode mPorterDuffMode = PorterDuff.Mode.SRC_ATOP;

    abstract class Blender {

        public abstract int blendColor(int i, int j);
    }

    abstract class BlenderPerChannel extends Blender {

        public abstract float blendChannel(float f, float f1);

        public int blendColor(int dstArgb, int srcArgb) {
            int srcR = 0xff & srcArgb >>> 16;
            int srcG = 0xff & srcArgb >>> 8;
            int srcB = srcArgb & 0xff;
            int dstR = 0xff & dstArgb >>> 16;
            int dstG = 0xff & dstArgb >>> 8;
            int dstB = dstArgb & 0xff;
            int srcBlendedR = (int) (255F * ColorFilterUtils.clamp(0,
                    blendChannel((float) dstR / 255F, (float) srcR / 255), 1));
            int srcBlendedG = (int) (255 * ColorFilterUtils.clamp(0,
                    blendChannel((float) dstG / 255, (float) srcG / 255), 1));
            int srcBlendedB = (int) (255 * ColorFilterUtils.clamp(0,
                    blendChannel((float) dstB / 255, (float) srcB / 255), 1));
            return srcBlendedB | (0xff000000 & srcArgb | srcBlendedR << 16 | srcBlendedG << 8);
        }
    }

    abstract class PorterDuffBlender {

        public abstract int blendFinal(int i, int j);
    }

    abstract class PorterDuffBlenderPerChannel extends PorterDuffBlender {

        public abstract float blendAlpha(float f, float f1);

        public abstract float blendChannel(float f, float f1, float f2, float f3);

        public int blendFinal(int dstArgb, int srcArgb) {
            int srcA = 0xff & srcArgb >>> 24;
            int srcR = 0xff & srcArgb >>> 16;
            int srcG = 0xff & srcArgb >>> 8;
            int srcB = 0xff & srcArgb;
            int dstA = 0xff & dstArgb >>> 24;
            int dstR = 0xff & dstArgb >>> 16;
            int dstG = 0xff & dstArgb >>> 8;
            int dstB = 0xff & dstArgb;
            float Da = (float) dstA / 255;
            float Sa = (float) srcA / 255;
            int resultA = (int) (255 * ColorFilterUtils.clamp(0, blendAlpha(Da, Sa), 1));
            Da = (float) dstR / 255;
            Sa = (float) srcR / 255;
            int resultR = (int) (255 * ColorFilterUtils.clamp(0, blendChannel(Da, Sa, Da, Sa), 1));
            Da = (float) dstG / 255;
            Sa = (float) srcG / 255;
            int resultG = (int) (255 * ColorFilterUtils.clamp(0, blendChannel(Da, Sa, Da, Sa), 1));
            Da = (float) dstB / 255;
            Sa = (float) srcB / 255;
            int resultB = (int) (255 * ColorFilterUtils.clamp(0, blendChannel(Da, Sa, Da, Sa), 1));
            return resultB | (resultA << 24 | resultR << 16 | resultG << 8);
        }
    }

    private Blender getCurrentBlender() {
        switch (mBlendType) {
            case BLEND_TYPE_OPACITY:
                return new Blender() {

                    private float blendChannel(float a, float b, float alpha) {
                        return alpha * b + a * (1 - alpha);
                    }

                    public int blendColor(int dstArgb, int srcArgb) {
                        int srcA = 0xff & srcArgb >>> 24;
                        int srcR = 0xff & srcArgb >>> 16;
                        int srcG = 0xff & srcArgb >>> 8;
                        int srcB = 0xff & srcArgb;
                        int dstR = 0xff & dstArgb >>> 16;
                        int dstG = 0xff & dstArgb >>> 8;
                        int dstB = 0xff & dstArgb;
                        int srcBlendedR = (int) (255 * ColorFilterUtils.clamp(
                                0,
                                blendChannel((float) dstR / 255, (float) srcR / 255,
                                        (float) srcA / 255), 1));
                        int srcBlendedG = (int) (255 * ColorFilterUtils.clamp(
                                0,
                                blendChannel((float) dstG / 255, (float) srcG / 255,
                                        (float) srcA / 255), 1));
                        int srcBlendedB = (int) (255 * ColorFilterUtils.clamp(
                                0,
                                blendChannel((float) dstB / 255, (float) srcB / 255,
                                        (float) srcA / 255), 1));
                        return srcBlendedB
                                | (0xff000000 & srcArgb | srcBlendedR << 16 | srcBlendedG << 8);
                    }
                };

            case BLEND_TYPE_LUMINOSITY:
                return new Blender() {

                    public int blendColor(int dstArgb, int srcArgb) {
                        float srcHsl[] = new float[3];
                        float dstHsl[] = new float[3];
                        ColorFilterUtils.RgbToHsl(srcArgb, srcHsl);
                        ColorFilterUtils.RgbToHsl(dstArgb, dstHsl);
                        int blendedRgb = ColorFilterUtils.HslToRgb(dstHsl[0], dstHsl[1], srcHsl[2]);
                        return 0xff000000 & srcArgb | 0xffffff & blendedRgb;
                    }
                };

            case BLEND_TYPE_COLOR:
                return new Blender() {

                    public int blendColor(int dstArgb, int srcArgb) {
                        float srcHsl[] = new float[3];
                        float dstHsl[] = new float[3];
                        ColorFilterUtils.RgbToHsl(srcArgb, srcHsl);
                        ColorFilterUtils.RgbToHsl(dstArgb, dstHsl);
                        int blendedRgb = ColorFilterUtils.HslToRgb(srcHsl[0], srcHsl[1], dstHsl[2]);
                        return 0xff000000 & srcArgb | 0xffffff & blendedRgb;
                    }
                };

            case BLEND_TYPE_SATURATION:
                return new Blender() {

                    public int blendColor(int dstArgb, int srcArgb) {
                        float srcHsl[] = new float[3];
                        float dstHsl[] = new float[3];
                        ColorFilterUtils.RgbToHsl(srcArgb, srcHsl);
                        ColorFilterUtils.RgbToHsl(dstArgb, dstHsl);
                        int blendedRgb = ColorFilterUtils.HslToRgb(dstHsl[0], srcHsl[1], dstHsl[2]);
                        return 0xff000000 & srcArgb | 0xffffff & blendedRgb;
                    }
                };

            case BLEND_TYPE_HUE:
                return new Blender() {

                    public int blendColor(int dstArgb, int srcArgb) {
                        float srcHsl[] = new float[3];
                        float dstHsl[] = new float[3];
                        ColorFilterUtils.RgbToHsl(srcArgb, srcHsl);
                        ColorFilterUtils.RgbToHsl(dstArgb, dstHsl);
                        int blendedRgb = ColorFilterUtils.HslToRgb(srcHsl[0], dstHsl[1], dstHsl[2]);
                        return 0xff000000 & srcArgb | 0xffffff & blendedRgb;
                    }
                };

            case BLEND_TYPE_EXCLUSION:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return (b + a) - a * (2 * b);
                    }
                };

            case BLEND_TYPE_HARD_MIX:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return b < 1 - a ? 0 : 1;
                    }
                };

            case BLEND_TYPE_PIN_LIGHT:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        if (a < 2 * b - 1)
                            return 2 * b - 1;
                        else if (a >= 2 * b)
                            return 2 * b;
                        else
                            return a;
                    }
                };

            case BLEND_TYPE_LIGHTEN:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return Math.max(a, b);
                    }
                };

            case BLEND_TYPE_DARKEN:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return Math.min(a, b);
                    }
                };

            case BLEND_TYPE_DIFFERENCE:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return Math.abs(a - b);
                    }
                };

            case BLEND_TYPE_SUBTRACT:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return a - b;
                    }
                };

            case BLEND_TYPE_DIVIDE:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return a / b;
                    }
                };

            case BLEND_TYPE_LINEAR_LIGHT:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return (a + 2 * b) - 1;
                    }
                };

            case BLEND_TYPE_VIVID_LIGHT:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        if (b <= 0.5F)
                            return 1 - (1 - a) / (2 * b);
                        else
                            return a / (2 * (1 - b));
                    }
                };

            case BLEND_TYPE_LINEAR_BURN:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return b + a - 1;
                    }
                };

            case BLEND_TYPE_COLOR_BURN:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return 1 - (1 - a) / b;
                    }
                };

            case BLEND_TYPE_LINEAR_DODGE:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return b + a;
                    }
                };

            case BLEND_TYPE_COLOR_DODGE:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return a / (1 - b);
                    }
                };

            case BLEND_TYPE_HARD_LIGHT:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        if (b < 0.5F)
                            return b * (2 * a);
                        else
                            return 1 - 2 * (1 - a) * (1 - b);
                    }
                };

            case BLEND_TYPE_SOFT_LIGHT:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        if (b < 0.5F)
                            return b * (2 * a) + a * a * (1 - 2 * b);
                        else
                            return (float) (2 * a * (1 - b) + Math.sqrt(a) * (2 * b - 1));
                    }
                };

            case BLEND_TYPE_OVERLAY:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        if (a < 0.5F)
                            return b * (2 * a);
                        else
                            return 1 - 2 * (1 - a) * (1 - b);
                    }
                };

            case BLEND_TYPE_SCREEN:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return 1 - (1 - a) * (1 - b);
                    }
                };

            case BLEND_TYPE_MULTIPLY:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return a * b;
                    }
                };

            case BLEND_TYPE_NORMAL:
                return new BlenderPerChannel() {

                    public float blendChannel(float a, float b) {
                        return b;
                    }
                };
        }
        Log.w(TAG, "unknown blender type:" + mBlendType);
        return null;
    }

    private PorterDuffBlender getCurrentPorterDuffBlender() {
        if (mPorterDuffMode == PorterDuff.Mode.CLEAR)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return 0;
                }

                public float blendChannel(float Dc, float Sc, float Da, float f) {
                    return 0;
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.DST)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Da;
                }

                public float blendChannel(float Dc, float Sc, float Da, float f) {
                    return Dc;
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.DST_ATOP)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Sa;
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sa * Dc + Sc * (1 - Da);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.DST_IN)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Sa * Da;
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sa * Dc;
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.DST_OUT)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Da * (1 - Sa);
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Dc * (1 - Sa);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.DST_OVER)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Sa + Da * (1 - Sa);
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Dc + Sc * (1 - Da);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.SRC)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Sa;
                }

                public float blendChannel(float Dc, float Sc, float Da, float f) {
                    return Sc;
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.SRC_ATOP)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Da;
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sc * Da + Dc * (1 - Sa);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.SRC_IN)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Sa * Da;
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sc * Da;
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.SRC_OUT)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Sa * (1 - Da);
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sc * (1 - Da);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.SRC_OVER)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return Sa + Da * (1 - Sa);
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sc + Dc * (1 - Sa);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.XOR)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return (Sa + Da) - Da * (2 * Sa);
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sc * (1 - Da) + Dc * (1 - Sa);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.SCREEN)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return (Sa + Da) - Sa * Da;
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return (Sc + Dc) - Sc * Dc;
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.LIGHTEN)
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return (Sa + Da) - Sa * Da;
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sc * (1 - Da) + Dc * (1 - Sa) + Math.max(Sc, Dc);
                }
            };
        if (mPorterDuffMode == PorterDuff.Mode.DARKEN) {
            return new PorterDuffBlenderPerChannel() {

                public float blendAlpha(float Da, float Sa) {
                    return (Sa + Da) - Sa * Da;
                }

                public float blendChannel(float Dc, float Sc, float Da, float Sa) {
                    return Sc * (1 - Da) + Dc * (1 - Sa) + Math.min(Sc, Dc);
                }
            };
        } else {
            Log.w(TAG, "unsupport porter duff mode:" + mPorterDuffMode);
            return null;
        }
    }

    private int mergeWidthHeight(int width, int height) {
        if (width <= Short.MAX_VALUE && height <= Short.MAX_VALUE)
            return height | width << 16;
        else
            throw new RuntimeException("image's width or height to large:" + width + 'x' + height);
    }

    private BitmapInfo obtainInputImageBySize(int width, int height) {
        int widthHeight = mergeWidthHeight(width, height);
        Entry<Integer, BitmapInfo> entry = mInputImageCache == null ? null : mInputImageCache.get();
        if (entry != null && ((Integer) entry.getKey()).intValue() == widthHeight)
            return entry.getValue();
        int originWidthHeight = mergeWidthHeight(mInput.width, mInput.height);
        if (originWidthHeight == widthHeight) {
            return mInput;
        } else {
            Bitmap originBitmap = BitmapInfo.getBitmap(mInput);
            Bitmap newBitmap = Bitmap.createScaledBitmap(originBitmap, width, height, true);
            BitmapInfo newImageData = BitmapInfo.getBitmapInfo(newBitmap);
            mInputImageCache = new SoftReference<SimpleImmutableEntry<Integer, BitmapInfo>>(
                    new SimpleImmutableEntry<Integer, BitmapInfo>(Integer.valueOf(widthHeight),
                            newImageData));
            return newImageData;
        }
    }

    public void process(BitmapInfo imgData) {
        if (mInput != null) {
            Blender blendable = getCurrentBlender();
            if (blendable != null) {
                PorterDuffBlender porterDuffBlender = getCurrentPorterDuffBlender();
                if (porterDuffBlender != null) {
                    int width = imgData.width;
                    int height = imgData.height;
                    int pixels[] = imgData.pixels;
                    int dstPixels[] = pixels;
                    int srcPixels[] = obtainInputImageBySize(width, height).pixels;
                    if (!mIsInputImageOnTop) {
                        dstPixels = srcPixels;
                        srcPixels = pixels;
                    }
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            int colorIndex = x + y * width;
                            int dstArgb = dstPixels[colorIndex];
                            int srcArgb = srcPixels[colorIndex];
                            int blendedSrcArgb = blendable.blendColor(dstArgb, srcArgb);
                            pixels[colorIndex] = porterDuffBlender.blendFinal(dstArgb,
                                    blendedSrcArgb);
                        }

                    }

                }
            }
        }
    }

    public void setBlendType(int blendType) {
        mBlendType = blendType;
    }

    public void setBlendTypeName(String typeName) {
        if (!TextUtils.isEmpty(typeName) && !typeName.equalsIgnoreCase("Normal")) {
            if (typeName.equalsIgnoreCase("Multiply")) {
                mBlendType = BLEND_TYPE_MULTIPLY;
            }
            if (typeName.equalsIgnoreCase("Screen")) {
                mBlendType = BLEND_TYPE_SCREEN;
            }
            if (typeName.equalsIgnoreCase("Darken")) {
                mBlendType = BLEND_TYPE_DARKEN;
            }
            if (typeName.equalsIgnoreCase("Lighten")) {
                mBlendType = BLEND_TYPE_LIGHTEN;
            }
            if (typeName.equalsIgnoreCase("Difference")) {
                mBlendType = BLEND_TYPE_DIFFERENCE;
            }
            if (typeName.equalsIgnoreCase("LinearDodge")) {
                mBlendType = BLEND_TYPE_LINEAR_DODGE;
            }
            if (typeName.equalsIgnoreCase("LinearBurn")) {
                mBlendType = BLEND_TYPE_LINEAR_BURN;
            }
            if (typeName.equalsIgnoreCase("Overlay")) {
                mBlendType = BLEND_TYPE_OVERLAY;
            }
            if (typeName.equalsIgnoreCase("ColorDodge")) {
                mBlendType = BLEND_TYPE_COLOR_DODGE;
            }
            if (typeName.equalsIgnoreCase("ColorBurn")) {
                mBlendType = BLEND_TYPE_COLOR_BURN;
            }
            if (typeName.equalsIgnoreCase("Opacity")) {
                mBlendType = BLEND_TYPE_OPACITY;
            }
            if (typeName.equalsIgnoreCase("HardLight")) {
                mBlendType = BLEND_TYPE_HARD_LIGHT;
            }
            if (typeName.equalsIgnoreCase("SoftLight")) {
                mBlendType = BLEND_TYPE_SOFT_LIGHT;
            }
            if (typeName.equalsIgnoreCase("VividLight")) {
                mBlendType = BLEND_TYPE_VIVID_LIGHT;
            }
            if (typeName.equalsIgnoreCase("LinearLight")) {
                mBlendType = BLEND_TYPE_LINEAR_LIGHT;
            }
            if (typeName.equalsIgnoreCase("PinLight")) {
                mBlendType = BLEND_TYPE_PIN_LIGHT;
            }
            if (typeName.equalsIgnoreCase("HardMix")) {
                mBlendType = BLEND_TYPE_HARD_MIX;
            }
            if (typeName.equalsIgnoreCase("Exclusion")) {
                mBlendType = BLEND_TYPE_EXCLUSION;
            }
            if (typeName.equalsIgnoreCase("Hue")) {
                mBlendType = BLEND_TYPE_HUE;
            }
            if (typeName.equalsIgnoreCase("Saturation")) {
                mBlendType = BLEND_TYPE_SATURATION;
            }
            if (typeName.equalsIgnoreCase("Color")) {
                mBlendType = BLEND_TYPE_COLOR;
            }
            if (typeName.equalsIgnoreCase("Luminosity")) {
                mBlendType = BLEND_TYPE_LUMINOSITY;
            }
            if (typeName.equalsIgnoreCase("Divide")) {
                mBlendType = BLEND_TYPE_DIVIDE;
            }
            if (typeName.equalsIgnoreCase("Subtract")) {
                mBlendType = BLEND_TYPE_SUBTRACT;
            } else {
                Log.d(TAG, "unknown blend type name: " + typeName);
            }
        } else {
            mBlendType = BLEND_TYPE_NORMAL;
        }
    }

    public void setInputImage(Bitmap bitmap) {
        mInput = BitmapInfo.getBitmapInfo(bitmap);
    }

    public void setIsInputImageOnTop(boolean value) {
        mIsInputImageOnTop = value;
    }

    public void setPorterDuffMode(PorterDuff.Mode value) {
        mPorterDuffMode = value;
    }
}
