
package com.lewa.themes.resource;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import lewa.util.ImageUtils;
import libcore.io.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//import static lewa.content.res.ThemeConstants.*;
import static com.lewa.themes.ThemeManager.THEME_ICON_CONFIG;
import static com.lewa.themes.ThemeManager.THEME_ICON_DEFAULT;
import static lewa.os.Process.IS_SYSTEM;

public class ThemeResources {
    private static final String LOG_TAG = "ThemeResources";

    private static ThemeResources sThemeResources = null;

    private boolean mIconV4;

    private ZipFile mIconFile;

    private static ArrayList<ZipFile> mIconFileBackups = new  ArrayList<ZipFile>();

    private ZipFile mLockStyleFile;

    private ZipFile mFancyWallpaperFile;

    private String mLockWallpaperPath;

    private HashSet<String> mFancyIcons;

    private static String sDensityName;

    private static WeakReference<Bitmap> sWallpaper;

    private static final Resources sResources = Resources.getSystem();

    private static final int sDendityDpi = sResources.getDisplayMetrics().densityDpi;

    public final static boolean DEBUG = SystemProperties.getBoolean("debug.IconCustomizer", false);

    static {
        // create face dir for Lewa Theme System
        final File CONFIG_THEME_CUSTOM_DIR = new File("/data/system/face");
        if (IS_SYSTEM && !CONFIG_THEME_CUSTOM_DIR.exists()) {
            try {
                CONFIG_THEME_CUSTOM_DIR.mkdirs();
                FileUtils.setPermissions(CONFIG_THEME_CUSTOM_DIR.getAbsolutePath(), 0755, -1, -1);
            } catch (Exception e) {
            }
        }
        switch (sDendityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                sDensityName = "ldpi";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                sDensityName = "mdpi";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                sDensityName = "hdpi";
                break;
            case 480/* DisplayMetrics.DENSITY_XXHIGH */:
                sDensityName = "xxhdpi";
                break;
            case 640/* DisplayMetrics.DENSITY_XXXHIGH */:
                sDensityName = "xxxhdpi";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
            default:
                sDensityName = "xhdpi";
                break;
        }
    }

    public static synchronized ThemeResources getSystem() {
        if (sThemeResources == null)
            sThemeResources = new ThemeResources();
        return sThemeResources;
    }

    public static void reset() {
        sThemeResources = null;
        sWallpaper = null;
        IconCustomizer.clearCustomizedIcons(null);
    }

    private ThemeResources() {
        init();
    }

    public static final String CONFIG_ICON_RES_SUBFOLDER = "res/";
    public static final String CONFIG_LOCKSTYLE_SUBFOLDER = "advance/";
    private void init() {
        mIconFile = getZipIconFile(THEME_ICON_CONFIG,THEME_ICON_DEFAULT, getExternalAvaliablePath("icon"));
/*        mLockStyleFile = getZipFile(CONFIG_LOCKSTYLE_CUSTOM_PATH,CONFIG_LOCKSTYLE_STANDALONE_PATH,
                CONFIG_LOCKSTYLE_DEFAULT_PATH, getExternalAvaliablePath(CONFIG_LOCKSTYLE_NAME));
        mFancyWallpaperFile = getZipFile(CONFIG_FANCYWALLPAPER_CUSTOM_PATH,
                CONFIG_FANCYWALLPAPER_STANDALONE_PATH,CONFIG_FANCYWALLPAPER_DEFAULT_PATH,
                getExternalAvaliablePath(CONFIG_FANCYWALLPAPER_NAME));
        mLockWallpaperPath = getAvaliablePath(CONFIG_LOCKWALLPAPER_CUSTOM_PATH,
                CONFIG_LOCKWALLPAPER_STANDALONE_PATH,CONFIG_LOCKWALLPAPER_DEFAULT_PATH,
                getExternalAvaliablePath(CONFIG_LOCKWALLPAPER_NAME));*/
        mFancyIcons = null;
        mIconV4 = mIconFile != null && mIconFile.getEntry(CONFIG_ICON_RES_SUBFOLDER) == null;
    }

    public static boolean isIconV4() {
        return getSystem().mIconV4;
    }
    public static boolean isIconV4(ZipFile iconFile) {
        return iconFile != null && iconFile.getEntry(CONFIG_ICON_RES_SUBFOLDER) == null;
    }
    public boolean hasFancyIcon(String name) {
        ensureFancyIcons();
        return mFancyIcons.contains(name);
    }

    public String[] getFancyIcons() {
        ensureFancyIcons();
        return mFancyIcons.toArray(new String[] {});
    }

    private void ensureFancyIcons() {
        if (mFancyIcons == null) {
            mFancyIcons = new HashSet<String>();
            if (mIconFile != null) {
                String inner = IconCustomizer.FANCY_ICONS_INNER_PATH;
                int innerLength = inner.length();
                @SuppressWarnings("unchecked")
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) mIconFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    String z = e.getName();
                    if (e.isDirectory() && z.startsWith(inner) && isSubFolder(z)) {
                        mFancyIcons.add(z.substring(innerLength, z.length() - 1));
                    }
                }
            }
        }
    }

    private boolean isSubFolder(String path) {
        int split = 0;
        for (int i = path.length() - 1; i >= 0; i--) {
            if (path.charAt(i) == '/')
                split++;
            if (split > 2)
                return false;
        }
        if (split == 2)
            return true;
        return false;
    }

    private ZipFile getZipIconFile(String... paths) {
        try {
            mIconPath = getAvaliableIconPath(paths);
            return new ZipFile(mIconPath);
        } catch (Exception e) {
            return null;
        }
    }

    private ZipFile getZipFile(String... paths) {
        try {
            return new ZipFile(getAvaliablePath(paths));
        } catch (Exception e) {
            return null;
        }
    }

    public static Drawable getLockWallpaper(Context mContext){
        try {
            String path = getSystem().mLockWallpaperPath;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);
            int width;
            int height;
            switch (sDendityDpi) {
                case DisplayMetrics.DENSITY_LOW:
                    width = 240;
                    height = 320;
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                    width = 320;
                    height = 480;
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                    width = 540;
                    height = 960;
                    break;
                case 480/* DisplayMetrics.DENSITY_XXHIGH */:
                case DisplayMetrics.DENSITY_XHIGH:
                default:
                    width = 720;
                    height = 1280;
                    break;
            }
            opts.inSampleSize = ImageUtils.computeSampleSize(opts, width, height);
            opts.inJustDecodeBounds = false;
            Bitmap bmp = BitmapFactory.decodeFile(path, opts);
            if (bmp != null) {
                sWallpaper = new WeakReference<Bitmap>(bmp);
                return new BitmapDrawable(sResources, bmp);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "get wallpaper error", e);
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "get wallpaper error", e);
        }
        return null;
    }
    public static Drawable getLockWallpaperCache(Context mContext) {
        try {
            if (sWallpaper != null) {
                Bitmap bmp = sWallpaper.get();
                if (bmp != null)
                    return new BitmapDrawable(sResources, bmp);
            }
            return getLockWallpaper(mContext);
        } catch (Exception e) {
            Log.e(LOG_TAG, "get wallpaper error", e);
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "get wallpaper error", e);
        }
        return null;
    }
    public static void clearLockWallpaperCache(){
        if(null!=sWallpaper){
            sWallpaper.clear();
            sWallpaper=null;
        }
    }
    private String mIconPath = null;

    private static String getAvaliablePath(String... paths) {
        for (String p : paths) {
            if (new File(p).exists()) {
                return p;
            }
        }
        return null;
    }

    private static String getAvaliableIconPath(String... paths) {
        try {
            for(ZipFile iconFile : mIconFileBackups){
                iconFile.close();
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        mIconFileBackups.clear();
        String bestMatch = null;
        for (String p : paths) {
            if (new File(p).exists()) {
                if(bestMatch == null) {
                    bestMatch = p;
                }
                try {
                    mIconFileBackups.add(new ZipFile(p));
                    if(DEBUG) {
                        Log.d(LOG_TAG, "iconfile  : " + p);                        
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "", e);
                }
            }
        }
        return bestMatch;
    }

    private static String getExternalAvaliablePath(String name) {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + name;
    }

    public InputStream getIconStream(String path, long[] size) {
        if (mIconFile != null && path != null) {
            try {
                ZipEntry entry = mIconFile.getEntry(path);
                if (entry != null) {
                    if (size != null)
                        size[0] = entry.getSize();
                    return mIconFile.getInputStream(entry);
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    public boolean hasIcon(String path) {
        return mIconFile != null && path != null && mIconFile.getEntry(path) != null;
    }

    public InputStream getFancyWallpaperFileStream(String path, long[] size) {
        if (mFancyWallpaperFile != null) {
            try {
                ZipEntry entry = mFancyWallpaperFile.getEntry(path);
                if (entry != null) {
                    if (size != null)
                        size[0] = entry.getSize();
                    return mFancyWallpaperFile.getInputStream(entry);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public boolean containsFancyWallpaperEntry(String path) {
        return mFancyWallpaperFile != null && mFancyWallpaperFile.getEntry(path) != null;
    }

    public InputStream getFancyLockscreenFileStream(String path, long[] size) {
        if (mLockStyleFile != null) {
            try {
                ZipEntry entry = mLockStyleFile.getEntry(CONFIG_LOCKSTYLE_SUBFOLDER + path);
                if (entry != null) {
                    if (size != null)
                        size[0] = entry.getSize();
                    return mLockStyleFile.getInputStream(entry);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public boolean containsFancyLockscreenEntry(String path) {
        return mLockStyleFile != null
                && mLockStyleFile.getEntry(CONFIG_LOCKSTYLE_SUBFOLDER + path) != null;
    }

    protected void finalize() throws Throwable {
        if (mIconFile != null) {
            try {
                mIconFile.close();
            } catch (IOException ioexception) {
            }
            mIconFile = null;
        }
        if (mLockStyleFile != null) {
            try {
                mLockStyleFile.close();
            } catch (IOException ioexception) {
            }
            mLockStyleFile = null;
        }
// mIconFileBackups is static,use here will affect other ThemeResources object
//        for(ZipFile iconFile : mIconFileBackups) {
//             try {
//                 iconFile.close();
//            } catch (IOException ioexception) {
//            }
//            mIconFileBackups.remove(iconFile);
//        }
        super.finalize();
    }

    public Bitmap getIcon(Resources res, String path) {
        Bitmap icon = null;
        for(ZipFile zipFile : mIconFileBackups) {
            icon = getIcon(zipFile, res, path);
            if(icon != null) {
                break;
            }
        }
        return icon;
    }
 
    public Bitmap getIcon(ZipFile iconFile,  Resources res, String path) {
        if(DEBUG) {
            Log.d(LOG_TAG, "icon : " + iconFile + "   path "  + path);            
        }
        if (iconFile != null) {
            InputStream is = null;
            try {
                ZipEntry entry = null;
                if (isIconV4(iconFile)) {
                    entry = iconFile.getEntry(path);
                } else {
                    entry = iconFile.getEntry(CONFIG_ICON_RES_SUBFOLDER + path);
                }
                if (entry != null) {
                    is = iconFile.getInputStream(entry);
                    return BitmapFactory.decodeStream(is);
                }
            } catch (OutOfMemoryError e) {
            } catch (Exception e) {
            } finally {
                IoUtils.closeQuietly(is);
            }
        }
        return null;
    }


    public InputStream getIconStream(Resources res, String path) {
        return getIconStream(mIconFile, res, path);
    }

     public InputStream getIconStream(ZipFile zipFile, Resources res, String path) {
        if (zipFile != null) {
            InputStream is = null;
            try {
                ZipEntry entry = zipFile.getEntry(CONFIG_ICON_RES_SUBFOLDER + path);
                if (entry != null) {
                    is = zipFile.getInputStream(entry);
                    return is;
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static String getDensityName() {
        return sDensityName;
    }
}
