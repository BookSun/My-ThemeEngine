package com.lewa.themes.resource;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import com.lewa.themes.ThemeManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import lewa.os.Shell;
import lewa.util.ImageUtils;

import static android.os.ParcelFileDescriptor.MODE_CREATE;
import static android.os.ParcelFileDescriptor.MODE_READ_WRITE;

/**
 * Created by ivonhoe on 15-3-19.
 */
public class BlurWallpaperUtils {

    private static final boolean DEBUG = ThemeManager.DEBUG;

    private static final String TAG = "BlurWallpaperUtils";

    public static Bitmap loadBlurWallpaper(Context context) {
        Bitmap bitmap = getBlurWallpaperFromStaticCache();
        if (bitmap == null) {
            Bitmap[] bitmaps = new Bitmap[1];
            getBlurredWallpaper(context, bitmaps);
            if (bitmaps[0] != null) {
                bitmap = bitmaps[0];
            }
        }
        return bitmap;
    }

    public static Bitmap getBlurWallpaperFromStaticCache() {
        File file = new File(ThemeManager.BLUR_WALLPAPER_PATH);
        if (file.exists()) {
            if (DEBUG)
                Log.d(TAG, "load blur wallpaper from static cache.");
            return BitmapFactory.decodeFile(file.getPath());
        }
        return null;
    }

    public static void getBlurredWallpaper(Context context, Bitmap[] blurWallpaper) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        if (wallpaperManager != null && wallpaperManager.getWallpaperInfo() == null) {
            BitmapDrawable d = (BitmapDrawable) wallpaperManager.getDrawable();

            if (d != null) {
                Shell.remove(ThemeManager.CUSTOMIZED_WALLPAPER_PATH);
                Shell.mkdirs(ThemeManager.CUSTOMIZED_WALLPAPER_PATH, 0777);
                Bitmap wallpaper = ((BitmapDrawable) d).getBitmap();
                File f = new File(ThemeManager.BLUR_WALLPAPER_PATH);
                if (blurWallpaper != null)
                    blurWallpaper[0] = buildBlurredBitmap(wallpaper, f);
            }
            wallpaperManager.forgetLoadedWallpaper();
        }
    }

    private static Bitmap buildBlurredBitmap(Bitmap inputBitmap, File blurFile) {
        if (inputBitmap == null || blurFile == null) {
            throw new NullPointerException();
        }
        Bitmap blurredWallpaper = null;
        try {
            FileOutputStream fos = new FileOutputStream(blurFile);

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 8;
            // Can you guess that why is 64*113? HaHa...
            Bitmap scaledBm = Bitmap.createScaledBitmap(inputBitmap, 64, 113, true);
            blurredWallpaper = Bitmap.createBitmap(64, 113, Bitmap.Config.ARGB_8888);
            ImageUtils.fastBlur(scaledBm, blurredWallpaper, 12);

            blurredWallpaper.compress(Bitmap.CompressFormat.PNG, 0, fos);
            fos.flush();
            fos.close();
            Shell.chmod(blurFile.getPath(), 0644);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return blurredWallpaper;
    }

    public static Bitmap cropBitmap(Bitmap source, int x, int y, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(source, x, y, width, height, null, false);
        if (source != null && bmp != source) {
            source.recycle();
        }
        return bmp;
    }

    public static Bitmap generateBitmap(Context context, Bitmap bm, int width, int height) {
        if (bm == null) {
            return null;
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        bm.setDensity(metrics.noncompatDensityDpi);

        if (width <= 0 || height <= 0
                || (bm.getWidth() == width && bm.getHeight() == height)) {
            return bm;
        }

        // This is the final bitmap we want to return.
        try {
            Log.d(TAG, "createBitmap begin width = " + width + " , height = " + height);
            Bitmap newbm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Log.d(TAG, "createBitmap end");
            newbm.setDensity(metrics.noncompatDensityDpi);

            Canvas c = new Canvas(newbm);
            Rect targetRect = new Rect();
            targetRect.right = bm.getWidth();
            targetRect.bottom = bm.getHeight();

            int deltaw = width - targetRect.right;
            int deltah = height - targetRect.bottom;

            if (deltaw > 0 || deltah > 0) {
                // We need to scale up so it covers the entire area.
                float scale;
                if (deltaw > deltah) {
                    scale = width / (float) targetRect.right;
                } else {
                    scale = height / (float) targetRect.bottom;
                }
                targetRect.right = (int) (targetRect.right * scale);
                targetRect.bottom = (int) (targetRect.bottom * scale);
                deltaw = width - targetRect.right;
                deltah = height - targetRect.bottom;
            }

            targetRect.offset(deltaw / 2, deltah / 2);

            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            Log.d(TAG, "drawBitmap begin targetRect = " + targetRect);
            c.drawBitmap(bm, null, targetRect, paint);
            Log.d(TAG, "drawBitmap end");
            bm.recycle();
            c.setBitmap(null);
            return newbm;
        } catch (OutOfMemoryError e) {
            Log.w(TAG, "Can't generate default bitmap", e);
            return bm;
        }
    }
}
