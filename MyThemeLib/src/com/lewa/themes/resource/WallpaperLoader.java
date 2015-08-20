package com.lewa.themes.resource;

import android.graphics.Bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import static com.lewa.themes.ThemeManager.THEME_WALLPAPER_CONFIG;
import static com.lewa.themes.ThemeManager.THEME_WALLPAPER_DEFAULT;

/**
 * Created by ivonhoe on 15-3-5.
 */
public class WallpaperLoader extends ResourceLoader {

    private static final String TAG = "WallpaperResourceLoader";

    private static final String WALLPAPER_NAME = "wallpaper";
    private static WeakReference<Bitmap> wallpaper;

    @Override
    public String getAvailablePath() {
        if (mAvailablePath == null) {
            mAvailablePath = getAvailablePath(THEME_WALLPAPER_CONFIG, THEME_WALLPAPER_DEFAULT,
                    getExternalAvailablePath(WALLPAPER_NAME));
        }

        return mAvailablePath;
    }

    @Override
    public InputStream getInputStream(String path, long[] size) {
        try {
            return getWallpaperStream(getAvailablePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean resourceExists(String path) {
        return false;
    }

    @Override
    public void clearCache() {
        if (null != wallpaper) {
            wallpaper.clear();
            wallpaper = null;
        }
        mAvailablePath = null;
    }

/*    public Drawable getWallpaperCache() {
        try {
            if (wallpaper != null) {
                Bitmap bmp = wallpaper.get();
                if (bmp != null)
                    return new BitmapDrawable(sResources, bmp);
            } else {
                Bitmap bitmap = getBitmapFile(getAvailablePath());
                if (bitmap != null) {
                    wallpaper = new WeakReference<Bitmap>(bitmap);
                    return new BitmapDrawable(sResources, bitmap);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "get wallpaper error", e);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "get wallpaper error", e);
        }
        return null;
    }*/
}
