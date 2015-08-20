package com.lewa.thememanager.utils;

import com.lewa.thememanager.Constants;
import com.lewa.themes.ThemeManager;

import lewa.os.FileUtilities;

import android.app.IWallpaperManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import lewa.provider.ExtraSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WallpaperUtilities {
    /**
     * Lock held while the temporary lock wallpaper is being written to disk.
     * This is used to prevent a possible race condition if multiple events
     * occur in quick succession to apply a specific theme with a lock paper.
     * <p>
     * TODO: This should be a file lock to avoid breaking down when carried over
     * multiple processes.
     */
    private static Object sLockWallpaperLock = new Object();

    private WallpaperUtilities() {}

    public static void setWallpaper(Context context, Uri uri) {
        InputStream in = null;
        try {
           in = context.getContentResolver().openInputStream(uri);
           WallpaperManager.getInstance(context).setStream(in);
        } catch (Exception e) {
            Log.w(Constants.TAG, "Could not set wallpaper", e);
        } finally {
            if (in != null) {
                FileUtilities.close(in);
            }
        }
    }

    /**
     * Tests if the HTC lockscreen facility is on the current platform. If true,
     * it is safe to call {@link #setLockWallpaper}.
     */
    public static synchronized boolean supportsLockWallpaper(Context context) {
        return true;
    }

    /**
     * Sets an HTC lockscreen wallpaper. It is necessary that you only invoke this method
     * if {@link #supportsLockWallpaper} yields true.
     */
    public static void setLockWallpaper(Context context, Uri uri,boolean isDefaultLockScreenWallpaper) {
        if (Constants.DEBUG) {
            Log.d(Constants.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " - uri=" + (null == uri ? "null" : uri.toString()));
        }
        if(isDefaultLockScreenWallpaper){
            File f=new File(ThemeManager.THEME_ELEMENT_LOCKSCREEN_WALLPAPER);
            if(f.exists()){
                f.delete();
                android.os.SystemProperties.set("sys.lewa.themeChanged", String.valueOf(android.os.SystemClock.elapsedRealtime() / 1000));
                return;
            }
        }
        synchronized (sLockWallpaperLock) {
            InputStream in = null;
            OutputStream out = null;
            try {
                if (null != uri) {
                    in = context.getContentResolver().openInputStream(uri);
                    // lockscreenManager_setStream.invoke(null, in, context);
                    out = FileUtilities.openOutputStream(ThemeManager.THEME_ELEMENT_LOCKSCREEN_WALLPAPER);
                    FileUtilities.connectIO(in, out);
                    FileUtilities.setPermissions(ThemeManager.THEME_ELEMENT_LOCKSCREEN_DIR);
//                    FileUtilities.setPermissions(ThemeUtilities.THEME_ELEMENT_LOCKSCREEN_DIR+"/face");
                    FileUtilities.setPermissions(ThemeManager.THEME_ELEMENT_LOCKSCREEN_WALLPAPER);
                    Settings.System.putInt(context.getContentResolver(), ExtraSettings.System.LOCKSCREEN_CHANGED, 2);
                    android.os.SystemProperties.set("sys.lewa.themeChanged", String.valueOf(android.os.SystemClock.elapsedRealtime() / 1000));

                /*
                 * } else {
                 *     FileUtilities.forceDelete(new File(ThemeUtilities.THEME_ELEMENT_LOCKSCREEN_WALLPAPER));
                 */
                }
            } catch (Exception e) {
                if ((e instanceof InvocationTargetException) || (e instanceof IOException)) {
                    Log.w(Constants.TAG, "Unable to set lock screen wallpaper (uri=" + uri + "): " + e);
                } else {
                    /* Explode on unexpected reflection errors... */
//                    throw new RuntimeException(e);
                }
            } finally {
                if (in != null) {
                    FileUtilities.close(in);
                }
                if (out != null) {
                    FileUtilities.close(out);
                }
            }
        }
    }

    public static void setLiveWallpaper(Context context, ComponentName component) {
        try {
            getWallpaperService(context).setWallpaperComponent(component);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Failure setting wallpaper", e);
        }
    }

    private static IWallpaperManager getWallpaperService(Context context) {
        return WallpaperManager.getInstance(context).getIWallpaperManager();
    }
}
