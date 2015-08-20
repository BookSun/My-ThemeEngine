package com.lewa.support.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {
    private static byte[] PNG_HEAD_FORMAT = {
            -119, 80, 78, 71, 13, 10, 26, 10
    };

    public static int computeSampleSize(InputStreamLoader streamLoader, int pixelSize) {
        int roundedSize = 1;
        if (pixelSize > 0) {
            BitmapFactory.Options options = getBitmapSize(streamLoader);

            double size = Math.sqrt(options.outWidth * options.outHeight / pixelSize);
            while (roundedSize * 2 <= size)
                roundedSize <<= 1;
        }

        return roundedSize;
    }

    public static int computeSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static boolean cropBitmapToAnother(Bitmap srcBmp, Bitmap destBmp, boolean recycleSrcBmp) {
        if (srcBmp != null && destBmp != null) {
            int srcWidth = srcBmp.getWidth();
            int srcHeight = srcBmp.getHeight();
            int destWidth = destBmp.getWidth();
            int destHeight = destBmp.getHeight();

            float ratio = Math.max((float) destWidth / srcWidth, (float) destHeight / srcHeight);

            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            Canvas canvas = new Canvas(destBmp);

            canvas.translate((destWidth - ratio * srcWidth) / 2,
                    (destHeight - ratio * srcHeight) / 2);
            canvas.scale(ratio, ratio);
            canvas.drawBitmap(srcBmp, 0, 0, paint);
            if (recycleSrcBmp)
                srcBmp.recycle();

            return true;
        }
        return false;
    }

    public static final Bitmap getBitmap(InputStreamLoader streamLoader, int pixelSize) {
        BitmapFactory.Options options = getDefaultOptions();
        options.inSampleSize = computeSampleSize(streamLoader, pixelSize);
        int retry = 0;
        while (retry < 3) {
            try {
                return BitmapFactory.decodeStream(streamLoader.get(), null, options);
            } catch (OutOfMemoryError e) {
                System.gc();
                options.inSampleSize *= 2;
            } catch (Exception ex) {
                return null;
            } finally {
                streamLoader.close();
            }
            retry++;
        }
        return null;
    }

    public static Bitmap getBitmap(InputStreamLoader streamLoader, int destWidth, int destHeight) {
        int pixelSize = 2 * (destWidth * destHeight);
        if (destWidth <= 0 || destHeight <= 0) {
            pixelSize = -1;
        }
        Bitmap destBmp = getBitmap(streamLoader, pixelSize);
        if (pixelSize > 0)
            destBmp = scaleBitmapToDesire(destBmp, destWidth, destHeight, true);

        return destBmp;
    }

    public static Bitmap getBitmap(InputStreamLoader streamLoader, int destWidth, int destHeight,
            Bitmap reusedBitmap) {
        Bitmap srcBitmap = null;
        if (reusedBitmap != null) {
            if (!reusedBitmap.isRecycled()) {
                Options sizeOp = getBitmapSize(streamLoader);
                try {
                    if (sizeOp.outHeight == reusedBitmap.getHeight()
                            && sizeOp.outHeight == reusedBitmap.getHeight()) {
                        BitmapFactory.Options op = getDefaultOptions();
                        op.inBitmap = reusedBitmap;
                        op.inSampleSize = 1;
                        srcBitmap = BitmapFactory.decodeStream(streamLoader.get(), null, op);
                        if (srcBitmap != null && destWidth > 0 && destHeight > 0) {
                            return scaleBitmapToDesire(srcBitmap, destWidth, destHeight, true);
                        }
                    }
                } catch (Exception e) {
                } finally {
                    if (srcBitmap != null)
                        reusedBitmap.recycle();
                    streamLoader.close();
                }
            }
        }
        return getBitmap(streamLoader, destWidth, destHeight);
    }

    public static final BitmapFactory.Options getBitmapSize(String filePath) {
        return getBitmapSize(new InputStreamLoader(filePath));
    }

    public static final BitmapFactory.Options getBitmapSize(InputStreamLoader streamLoader) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(streamLoader.get(), null, options);
            return options;
        } catch (Exception e) {
        } finally {
            streamLoader.close();
        }
        return options;
    }

    public static BitmapFactory.Options getDefaultOptions() {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inDither = false;
        opt.inJustDecodeBounds = false;
        opt.inSampleSize = 1;
        opt.inScaled = false;
        return opt;
    }

    public static boolean isPngFormat(InputStreamLoader streamLoader) {
        try {
            InputStream is = streamLoader.get();
            byte[] head = new byte[PNG_HEAD_FORMAT.length];
            int n = is.read(head);
            int i = head.length;
            return n >= i ? isPngFormat(head) : false;
        } catch (Exception e) {
            return false;
        } finally {
            if (streamLoader != null)
                streamLoader.close();
        }
    }

    public static boolean isPngFormat(byte[] pngHead) {
        if (pngHead != null && pngHead.length >= PNG_HEAD_FORMAT.length) {
            for (int i = 0; i < PNG_HEAD_FORMAT.length; i++)
                if (pngHead[i] != PNG_HEAD_FORMAT[i])
                    return false;
        }
        return true;
    }

//    public static boolean saveBitmapToLocal(InputStreamLoader streamLoader, String path,
//            int destWidth, int destHeight) {
//        if (streamLoader != null && path != null && destWidth > 0 && destHeight > 0) {
//            BitmapFactory.Options options = getBitmapSize(streamLoader);
//            if (options.outWidth > 0 && options.outHeight > 0) {
//                if (options.outWidth == destWidth && options.outHeight == destHeight)
//                    return saveToFile(streamLoader, path);
//
//                Bitmap destBmp = getBitmap(streamLoader, destWidth, destHeight);
//                if (destBmp != null) {
//                    boolean result = saveToFile(destBmp, path, isPngFormat(streamLoader));
//                    destBmp.recycle();
//                    return result;
//                }
//            }
//        }
//
//        return false;
//    }

    public static boolean saveToFile(Bitmap bitmap, String path) {
        return saveToFile(bitmap, path, false);
    }

    public static boolean saveToFile(Bitmap bitmap, String path, boolean saveToPng) {
        if (bitmap != null) {
            try {
                FileOutputStream outputStream = new FileOutputStream(path);
                bitmap.compress(saveToPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                        100, outputStream);
                outputStream.close();
                return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

//    private static boolean saveToFile(InputStreamLoader streamLoader, String path) {
//        try {
//            FileOutputStream outputStream = new FileOutputStream(path);
//            InputStream inputStream = streamLoader.get();
//            Streams.copy(inputStream, outputStream);
//            outputStream.close();
//            streamLoader.close();
//            return true;
//        } catch (Exception e) {
//        }
//        return false;
//    }

    public static Bitmap scaleBitmapToDesire(Bitmap srcBmp, int destWidth, int destHeight,
            boolean recycleSrcBmp) {
        int srcWidth = srcBmp.getWidth();
        int srcHeight = srcBmp.getHeight();
        if (srcWidth == destWidth && srcHeight == destHeight)
            return srcBmp;
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        if (srcBmp.getConfig() != null)
            config = srcBmp.getConfig();
        Bitmap destBmp = Bitmap.createBitmap(destWidth, destHeight, config);
        if (cropBitmapToAnother(srcBmp, destBmp, recycleSrcBmp))
            return destBmp;
        else
            return srcBmp;
    }

    public static void fastBlur(Bitmap in, Bitmap out, int radius) {
        native_blur(in, out, radius);
    }

    public static void findEdge(Bitmap bmp, Rect rect) {
        native_find_edge(bmp, rect);
    }

    public static int findMinSide(Bitmap bmp) {
        return native_find_side(bmp, true);
    }

    public static int findMaxSide(Bitmap bmp) {
        return native_find_side(bmp, false);
    }

    private static native void native_blur(Bitmap in, Bitmap out, int radius);

    private static native void native_find_edge(Bitmap bmp, Rect rect);

    private static native int native_find_side(Bitmap bmp, boolean min);

    static {
        try {
            System.loadLibrary("lewa_imageutils");
        } catch (UnsatisfiedLinkError e) {
        }
    }
}
