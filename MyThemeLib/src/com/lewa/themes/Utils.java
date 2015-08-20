package com.lewa.themes;

import static com.lewa.themes.ThemeManager.STANDALONE;

import android.graphics.BitmapFactory;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.Themes.ThemeColumns;

import org.xmlpull.v1.XmlPullParser;

import android.app.ActivityThread;
import android.app.WallpaperInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ThemeInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore.Audio;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.io.File;

public class Utils {
    private static final String TAG = "Utils";
    private Utils(){}

    public static String getPackageName(){
        String pkg = null;
        try {
            pkg = ActivityThread.currentPackageName();
            int i = pkg.lastIndexOf(':');
            if(i != -1){
                pkg = pkg.substring(0, i);
            }
        } catch (Exception e) {
        }
        return pkg == null ? Manifest.PACKAGE_NAME : pkg;
    }
    public static Resources getFileResources(Context context, String pkgPath){
        Resources res = null;
        try {
            AssetManager assmgr = new AssetManager();
            assmgr.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    Build.VERSION.RESOURCES_SDK_INT);

            int cookie = assmgr.addAssetPath(pkgPath);
            if (cookie == 0) {
                return null;
            }

            final DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            res = new Resources(assmgr, metrics, null);
        } catch (Exception e) {
        }
        return res;
    }
    /** File name in an APK for the Android manifest. */
    private static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    public static boolean isLiveWallpaperPackage(String pkgPath){

        XmlResourceParser parser = null;
        final Resources res;
        AssetManager assmgr = null;
        try {
            assmgr = new AssetManager();
            try {
                assmgr.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        Build.VERSION.RESOURCES_SDK_INT);
            } catch (NoSuchMethodError e) {
            }

            int cookie = assmgr.addAssetPath(pkgPath);
            if (cookie == 0) {
                return false;
            }

            final DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            res = new Resources(assmgr, metrics, null);
            parser = assmgr.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);

            AttributeSet attrs = parser;

            int type;
            int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }
                String tagName = parser.getName();
                if (!TextUtils.isEmpty(tagName) && tagName.equals("action")) {
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String value = parser.getAttributeValue(i);
                        if (!TextUtils.isEmpty(value)
                                && value.equals("android.service.wallpaper.WallpaperService")) {
                            return true;
                        }
                    }
                }
            }
        } catch (org.xmlpull.v1.XmlPullParserException e1) {

        } catch (java.io.IOException e2) {

        }
        /*
         * } catch (Exception e) {
         *     Log.w(Constants.TAG, "Unable to read AndroidManifest.xml of " + pi.applicationInfo.sourceDir + " " + e);
         * } finally {
         *     if (assmgr != null) assmgr.close();
         *     if (parser != null) parser.close();
         * }
         */

        return false;

    }
    public static ThemeInfo parsePackage(String pkgPath) {
        XmlResourceParser parser = null;
        final Resources res;
        AssetManager assmgr = null;
        try {
            assmgr = new AssetManager();
            try {
                assmgr.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        Build.VERSION.RESOURCES_SDK_INT);
            } catch (NoSuchMethodError e) {
            }

            int cookie = assmgr.addAssetPath(pkgPath);
            if (cookie == 0) {
                return null;
            }

            final DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            res = new Resources(assmgr, metrics, null);
            parser = assmgr.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);

            AttributeSet attrs = parser;

            int type;
            int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                String tagName = parser.getName();
                if (!TextUtils.isEmpty(tagName) && tagName.equals("theme")) {
                    // this is a theme apk.
                    return new ThemeInfo(parser, res, attrs);
                }
            }
        } catch (org.xmlpull.v1.XmlPullParserException e1) {

        } catch (java.io.IOException e2) {

        }
        /*
         * } catch (Exception e) {
         *     Log.w(Constants.TAG, "Unable to read AndroidManifest.xml of " + pi.applicationInfo.sourceDir + " " + e);
         * } finally {
         *     if (assmgr != null) assmgr.close();
         *     if (parser != null) parser.close();
         * }
         */

        return null;
    }

    public static void populateContentValues(Context context, ContentValues outValues,
            PackageInfo pi, ServiceInfo si, boolean isCurrentTheme) {
        outValues.put(ThemeColumns.IS_APPLIED, 0);
        outValues.put(ThemeColumns.THEME_ID, si.name);
        outValues.put(ThemeColumns.THEME_PACKAGE, pi.packageName);
        outValues.put(ThemeColumns.STYLE_NAME, si.name);
        outValues.put(ThemeColumns.IS_DRM, 0);
        outValues.put(ThemeColumns.IS_SYSTEM,
                ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? 1 : 0);
        outValues.put(ThemeColumns.HAS_HOST_DENSITY, 1);
        outValues.put(ThemeColumns.HAS_THEME_PACKAGE_SCOPE, 0);
        outValues.put(ThemeColumns.WALLPAPER_NAME, (String) null);
        outValues.put(ThemeColumns.WALLPAPER_URI , (String) null);
        outValues.put(ThemeColumns.RINGTONE_NAME, (String) null);
        outValues.put(ThemeColumns.RINGTONE_URI , (String) null);
        outValues.put(ThemeColumns.RINGTONE_NAME_KEY , (String) null);
        outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME, (String) null);
        outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_URI , (String) null);
        outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME_KEY, (String) null);
        outValues.put(ThemeColumns.BOOT_ANIMATION_URI, (String) null);
        outValues.put(ThemeColumns.FONT_URI , (String) null);
        outValues.put(ThemeColumns.LOCKSCREEN_URI, (String) null);
        outValues.put(ThemeColumns.ICONS_URI , (String) null);
        outValues.put(ThemeColumns.LOCK_WALLPAPER_URI, (String) null);
        outValues.put(ThemeColumns.LOCK_WALLPAPER_NAME , (String) null);
        outValues.put(ThemeColumns.PREVIEW_URI , (String) null);
        outValues.put(ThemeColumns.LIVE_WALLPAPER_URI , "1");
        outValues.put(ThemeColumns.IS_IMAGE_FILE , 0);

        File pkgFile = new File(pi.applicationInfo.sourceDir);
        if (pkgFile.exists()) {
            outValues.put(ThemeColumns.SIZE, pkgFile.length());
        }
        outValues.put(ThemeColumns.VERSION_CODE, pi.versionCode);
        outValues.put(ThemeColumns.VERSION_NAME, pi.versionName);

        final ResolveInfo ri = new ResolveInfo();
        ri.serviceInfo = si;
        WallpaperInfo wi = null;
        PackageManager pm = context.getPackageManager();
        try {
            wi = new WallpaperInfo(context, ri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate WallpaperInfo for " + pi.packageName + ": " + e);
        }
        try {
            outValues.put(ThemeColumns.AUTHOR, wi.loadAuthor(pm).toString());
        } catch (Exception e) {
            outValues.put(ThemeColumns.AUTHOR, "");
        }
        try {
            outValues.put(ThemeColumns.NAME, wi.loadLabel(pm).toString());
        } catch (Exception e) {
            outValues.put(ThemeColumns.NAME, "");
        }
        try {
            java.lang.reflect.Field field = wi.getClass().getDeclaredField("mThumbnailResource");
            field.setAccessible(true);
            outValues.put(ThemeColumns.THUMBNAIL_URI
                    , PackageResources.makeResourceIdUri(pi.packageName
                    , field.getInt(wi)).toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve thumbnail of livewallpaper " + pi.packageName + ": " + e);
            outValues.put(ThemeColumns.THUMBNAIL_URI, (String) null);
        }
    }
    public static void populateContentValues(Context context, ContentValues outValues,
            PackageInfo pi, ThemeInfo ti, boolean isCurrentTheme) {
        populateContentValues(context, outValues, pi, ti, pi.applicationInfo == null ? null : pi.applicationInfo.sourceDir, isCurrentTheme);
    }
    public static void populateContentValues(Context context, ContentValues outValues,
            PackageInfo pi, ThemeInfo ti, String path, boolean isCurrentTheme) {
        outValues.put(ThemeColumns.IS_APPLIED, isCurrentTheme ? 1 : 0);
        outValues.put(ThemeColumns.THEME_ID, ti.themeId);
        outValues.put(ThemeColumns.THEME_PACKAGE, pi.packageName);
        outValues.put(ThemeColumns.NAME, ti.name);
        outValues.put(ThemeColumns.STYLE_NAME,
                ti.themeStyleName != null ? ti.themeStyleName : ti.name);
        outValues.put(ThemeColumns.AUTHOR, ti.author);
        outValues.put(ThemeColumns.IS_DRM, ti.isDrmProtected ? 1 : 0);
        outValues.put(ThemeColumns.IS_SYSTEM,
                ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? 1 : 0);
        outValues.put(ThemeColumns.HAS_HOST_DENSITY, STANDALONE ? 0 : (hasHostDensity(context, pi, ti) ? 1 : 0));
        // outValues.put(ThemeColumns.HAS_THEME_PACKAGE_SCOPE, hasThemePackageScope(context, pi, ti) ? 1 : 0);
        outValues.put(ThemeColumns.HAS_THEME_PACKAGE_SCOPE, !STANDALONE && ti.hasRedirection ? 1 : 0);
        if (ti.wallpaperResourceId != 0) {
            /* TODO: wallpaper name is theme name for now. */
            outValues.put(ThemeColumns.WALLPAPER_NAME, ti.name);
            outValues.put(ThemeColumns.WALLPAPER_URI
                    , PackageResources.makeResourceIdUri(pi.packageName
                    , ti.wallpaperResourceId).toString());
        } else {
            outValues.put(ThemeColumns.WALLPAPER_NAME, (String) null);
            outValues.put(ThemeColumns.WALLPAPER_URI , (String) null);
        }
        if (ti.ringtoneFileName != null) {
            outValues.put(ThemeColumns.RINGTONE_NAME, ti.ringtoneName);
            outValues.put(ThemeColumns.RINGTONE_NAME_KEY, Audio.keyFor(ti.ringtoneName));
            outValues.put(ThemeColumns.RINGTONE_URI
                    , PackageResources.makeAssetPathUri(pi.packageName, ti.ringtoneFileName).toString());
        } else {
            outValues.put(ThemeColumns.RINGTONE_NAME, (String) null);
            outValues.put(ThemeColumns.RINGTONE_URI , (String) null);
            outValues.put(ThemeColumns.RINGTONE_NAME_KEY , (String) null);
        }
        if (ti.notificationRingtoneFileName != null) {
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME, ti.notificationRingtoneName);
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME_KEY, Audio.keyFor(ti.notificationRingtoneName));
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_URI
                    , PackageResources.makeAssetPathUri(pi.packageName
                    , ti.notificationRingtoneFileName).toString());
        } else {
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME, (String) null);
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_URI , (String) null);
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME_KEY, (String) null);
        }
        if (ti.thumbnailResourceId != 0) {
            outValues.put(ThemeColumns.THUMBNAIL_URI
                    , PackageResources.makeResourceIdUri(pi.packageName
                    , ti.thumbnailResourceId).toString());
        } else {
            outValues.put(ThemeColumns.THUMBNAIL_URI, (String) null);
        }
        if (ti.previewResourceId != 0) {
            outValues.put(ThemeColumns.PREVIEW_URI
                    , PackageResources.makeResourceIdUri(pi.packageName
                    , ti.previewResourceId).toString());
        } else {
            outValues.put(ThemeColumns.PREVIEW_URI , (String) null);
        }

        if (!STANDALONE && ti.bootAnimationName != null) {
            outValues.put(ThemeColumns.BOOT_ANIMATION_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.bootAnimationName).toString());
        } else {
            outValues.put(ThemeColumns.BOOT_ANIMATION_URI, (String) null);
        }

        if (!STANDALONE && ti.fontName != null) {
            outValues.put(ThemeColumns.FONT_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.fontName).toString());
        } else {
            outValues.put(ThemeColumns.FONT_URI , (String) null);
        }

        if (ti.lockscreenName != null) {
            outValues.put(ThemeColumns.LOCKSCREEN_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.lockscreenName).toString());
        } else {
            outValues.put(ThemeColumns.LOCKSCREEN_URI, (String) null);
        }

        if (ti.iconsName != null) {
            outValues.put(ThemeColumns.ICONS_URI,
                    PackageResources.makeAssetPathUri(pi.packageName, ti.iconsName).toString());
        } else {
            outValues.put(ThemeColumns.ICONS_URI , (String) null);
        }

        if (ti.lockscreenWallpaperResourceId != 0) {
            /* TODO: wallpaper name is theme name for now. */
            outValues.put(ThemeColumns.LOCK_WALLPAPER_NAME, ti.name);
            outValues.put(ThemeColumns.LOCK_WALLPAPER_URI
                    , PackageResources.makeResourceIdUri(pi.packageName
                    , ti.lockscreenWallpaperResourceId).toString());
        } else {
            outValues.put(ThemeColumns.LOCK_WALLPAPER_URI, (String) null);
            outValues.put(ThemeColumns.LOCK_WALLPAPER_NAME , (String) null);
        }
/*
 *         try {
 *             [> Try to find theme attributes by convention, like HTC lock screen wallpaper. <]
 *             Resources themeRes = context.createPackageContext(pi.packageName, 0).getResources();
 *
 *             int lockWallpaperResId =
 *                 themeRes.getIdentifier("com_htc_launcher_lockscreen_wallpaper", "drawable",
 *                         pi.packageName);
 *             if (lockWallpaperResId != 0) {
 *                 int nameId = themeRes.getIdentifier("com_htc_launcher_lockscreen_wallpaper_name",
 *                         "string", pi.packageName);
 *                 if (nameId != 0) {
 *                     outValues.put(ThemeColumns.LOCK_WALLPAPER_NAME,
 *                             themeRes.getString(nameId));
 *                 }
 *                 outValues.put(ThemeColumns.LOCK_WALLPAPER_URI,
 *                         PackageResources.makeResourceIdUri(pi.packageName, lockWallpaperResId)
 *                         .toString());
 *             }
 *         } catch (NameNotFoundException e) {
 *             // Unlikely-as-hell race condition.
 *         }
 */

        // Theme package size and version
        if(path != null) {
            File pkgFile = new File(path);
            if (pkgFile.exists()) {
                outValues.put(ThemeColumns.SIZE, pkgFile.length());
            }
            outValues.put(ThemeColumns.DOWNLOAD_PATH, path);
        } else {
            outValues.put(ThemeColumns.SIZE, 0);
            outValues.put(ThemeColumns.DOWNLOAD_PATH, (String) null);
        }
        outValues.put(ThemeColumns.VERSION_CODE, pi.versionCode);
        outValues.put(ThemeColumns.VERSION_NAME, pi.versionName);
        outValues.put(ThemeColumns.LIVE_WALLPAPER_URI , (String) null);
        outValues.put(ThemeColumns.IS_IMAGE_FILE , 0);
    }

    private static boolean hasHostDensity(Context context, PackageInfo pi, ThemeInfo ti) {
        try {
            Resources res = context.createPackageContext(pi.packageName, 0).getResources();

            /*
             * We don't need to actually read the bitmap, only look up the entry
             * in the resources table and examine the density with which the
             * AssetManager responded.
             */
            TypedValue outValue = new TypedValue();
            res.getValue(ti.previewResourceId, outValue, true);
            int density = (outValue.density == TypedValue.DENSITY_DEFAULT) ?
                    DisplayMetrics.DENSITY_DEFAULT : outValue.density;
            return density == res.getDisplayMetrics().densityDpi;
        } catch (NotFoundException e) {
            Log.w(TAG, "Missing required resource in package " + pi.packageName + ": " + e.getMessage());
            return false;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Possible package manager race condition detected?", e);
            return false;
        }
    }

    public static int computeSampleSize(BitmapFactory.Options options, int rWidth, int rHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > rHeight || width > rWidth) {
            final int heightRatio = Math.round((float) height
                    / (float) height);
            final int widthRatio = Math.round((float) width / (float) rWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        //RC 48764 jianwu gao begin
        if (reqHeight == 800) {
            return inSampleSize;
        }
        //RC 48764 jianwu gao end

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static String addFileNameSuffix(String src, String suffix) {
        return addFileNameSuffix(src, "_", suffix);
    }

    public static String addFileNameSuffix(String src, String separator, String suffix) {
        int dot = src.indexOf('.');
        StringBuilder sb = new StringBuilder(src.substring(0, dot));
        return sb.append(separator).append(suffix).append(src.substring(dot)).toString();
    }
}
