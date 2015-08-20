package com.lewa.thememanager.utils;

import com.lewa.thememanager.Constants;
import com.lewa.thememanager.R;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;

import lewa.os.FileUtilities;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ThemeInfo;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.wallpaper.WallpaperService;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.pm.ServiceInfo;
import lewa.content.ExtraIntent;
import lewa.provider.ExtraSettings;

public class ThemeUtilities {

    /**
     * Applies just the configuration portion of the theme. No wallpapers or
     * ringtones are set.
     */
    public static void applyStyle(Context context, ThemeItem theme) {
        applyStyleInternal(context, theme);

        /* Broadcast appearance/style change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), ThemeColumns.STYLE_CONTENT_ITEM_TYPE));
    }

    private static void applyStyleInternal(Context context, ThemeItem theme) {
        // New theme is applied, hence reset the count to 0.
        Intent intent = new Intent(ExtraIntent.ACTION_APP_LAUNCH_FAILURE_RESET, Uri.fromParts("package", "com.lewa.thememanager.activity", null));
        context.sendBroadcast(intent);

        Themes.markAppliedTheme(context, theme.getPackageName(), theme.getThemeId());

        /*
         * Trigger a configuration change so that all apps will update their UI.
         * This will also persist the theme for us across reboots.
         */
        updateConfiguration(context, theme);
    }

    private static void applyStyleInternal(Context context, ThemeItem theme,boolean isSystemApp, boolean forcestopLauncher,boolean isOnekeyTheme) {
        // New theme is applied, hence reset the count to 0.
        Intent intent = new Intent(ExtraIntent.ACTION_APP_LAUNCH_FAILURE_RESET, Uri.fromParts("package", "com.lewa.thememanager.activity", null));
        context.sendBroadcast(intent);

        Themes.markAppliedTheme(context, theme.getPackageName(), theme.getThemeId());

        /*
         * Trigger a configuration change so that all apps will update their UI.
         * This will also persist the theme for us across reboots.
         */
        if (isSystemApp) {
            updateConfiguration(context, theme);
        }
        if (forcestopLauncher) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if(!isOnekeyTheme){
                reLauncherHome(context);
            }else{
                List<RunningTaskInfo> runningTasks = manager.getRunningTasks(30);
                for(RunningTaskInfo runningTask:runningTasks){
                    manager.forceStopPackage(runningTask.topActivity.getPackageName());
                }
            }
        }
    }

    static void applyCustom(Context context, ThemeItem theme, boolean isIcon, boolean isSystemApp) {
        Intent intent = new Intent(ExtraIntent.ACTION_APP_LAUNCH_FAILURE_RESET, Uri.fromParts("package", "com.lewa.thememanager.activity", null));
        context.sendBroadcast(intent);
        if (isSystemApp) {
            updateConfiguration(context, theme);
        }
        if (isIcon) {
            reLauncherHome(context);
        }
    }

    private static void reLauncherHome(Context context) {

        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = context.getPackageManager();
        Intent i = new Intent("android.intent.action.MAIN");
        i.addCategory("android.intent.category.HOME");
        List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
        if (lst != null) {
            for (ResolveInfo resolveInfo : lst) {
                manager.forceStopPackage(resolveInfo.activityInfo.packageName);
            }
            //manager.forceStopPackage("com.lewa.lockscreen2");
        }
    }

    private static void applyCustom(Context context, ThemeItem theme) {
        // New theme is applied, hence reset the count to 0.

        Intent intent = new Intent(ExtraIntent.ACTION_APP_LAUNCH_FAILURE_RESET, Uri.fromParts("package", "com.lewa.thememanager.activity", null));
        context.sendBroadcast(intent);

        // Themes.markAppliedTheme(context, theme.getPackageName(),
        // theme.getThemeId());

        /*
         * Trigger a configuration change so that all apps will update their UI.
         * This will also persist the theme for us across reboots.
         */
        updateConfiguration(context, theme);
    }

    /**
     * Applies a full theme. This is a superset of applyStyle.
     */
    public static void applyTheme(Context context, ThemeItem theme) {
        applyTheme(context, theme, new Intent().setType(ThemeColumns.CONTENT_ITEM_TYPE), true);
    }

    public static void applyTheme(Context context, ThemeItem theme, Intent request) {
        applyTheme(context, theme, request, true);
    }

    public static void applyTheme(Context context, ThemeItem theme, boolean forcestopLauncher) {
        applyTheme(context, theme, new Intent().setType(ThemeColumns.CONTENT_ITEM_TYPE), forcestopLauncher);
    }

    public static void applyForThemeDetail(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);
        boolean isSystemApp=request.getBooleanExtra(ThemeManager.EXTRA_SYSTEM_APP, false);
        boolean isDefaultFont=request.getBooleanExtra(ThemeManager.DEFAULT_FONT, false);
        boolean isDefaultLockScreen=request.getBooleanExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, false);
        boolean isDefaultIcon=request.getBooleanExtra(ThemeManager.DEFAULT_ICON, false);
        boolean isDefaultLockScreenWallpaper=request.getBooleanExtra(ThemeManager.DEFAULT_LOCKSCREEN_WALLPAPER, false);
        // Begin, added by yljiang@lewatek.com 2014-01-09
        if(isSystemApp) {
            NewMechanismHelp.applyTheme(context, theme ,request);
        }
        // End
        Uri wallpaperUri = null;
        Uri lockWallpaperUri = null;
        ComponentName liveWallPaperComponent = null;
        Uri ringtoneUri = null;
        Uri notificationRingtoneUri = null;

        Uri bootAnimationUri = null;
        Uri fontUri = null;
        Uri lockscreenUri = null;
        Uri iconsUri = null;

        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            wallpaperUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_WALLPAPER_URI);
            lockWallpaperUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI);
            ringtoneUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_RINGTONE_URI);
            notificationRingtoneUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_NOTIFICATION_RINGTONE_URI);
            liveWallPaperComponent = (ComponentName) request.getParcelableExtra(ThemeManager.EXTRA_LIVE_WALLPAPER_COMPONENT);

            bootAnimationUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_BOOT_ANIMATION_URI);
            fontUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_FONT_URI);
            lockscreenUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_LOCKSCREEN_URI);
            iconsUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_ICONS_URI);
        }

        if (Constants.DEBUG) {
            Log.i(Constants.TAG, "applyTheme: theme=" + theme.getUri(context) + ", wallpaperUri=" + wallpaperUri + ", lockWallpaperUri=" + lockWallpaperUri + ", liveWallPaperComponent=" + liveWallPaperComponent + ", ringtoneUri=" + ringtoneUri + ", notificationRingtoneUri=" + notificationRingtoneUri + ", bootAnimationUri=" + bootAnimationUri + ", fontUri=" + fontUri + ", lockscreenUri=" + lockscreenUri + ", dontSetLockWallpaper=" + dontSetLockWallpaper + ",iconsUri=" + iconsUri);
        }

        if (liveWallPaperComponent != null) {
            WallpaperUtilities.setLiveWallpaper(context, liveWallPaperComponent);
        } else {
            // if (wallpaperUri == null) {
            // wallpaperUri = theme.getWallpaperUri(context);
            // }
            if (wallpaperUri != null) {
                WallpaperUtilities.setWallpaper(context, wallpaperUri);
            }
        }

        // if (ringtoneUri == null) {
        // ringtoneUri = theme.getRingtoneUri(context);
        // }
        if (ringtoneUri != null) {
            /* Check for special silent uri */
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, ThemeManager.SILENT_RINGTONE_URI.equals(ringtoneUri) ? null : ringtoneUri);
        }

        // if (notificationRingtoneUri == null) {
        // notificationRingtoneUri = theme.getNotificationRingtoneUri(context);
        // }
        if (notificationRingtoneUri != null) {
            /* Check for special silent uri */
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION, ThemeManager.SILENT_RINGTONE_URI.equals(notificationRingtoneUri) ? null : notificationRingtoneUri);
        }

        // Woody Guo @ 2012/08/27: LockScreenResourceLoader will create this
        // folder if it doesn't exist
        // ensureThemeFolderExists();
        setBootAnimation(context, theme, bootAnimationUri, true);
        if(isDefaultFont||fontUri!=null){
            setFonts(context, theme, fontUri, true,isDefaultFont);
            if(!ThemeManager.STANDALONE) {
                reloadFont(context);
            }
        }
        setLockscreen(context, theme, lockscreenUri, true, isDefaultLockScreen);
        setIcons(context, theme, iconsUri, true,isDefaultIcon);

        if (!dontSetLockWallpaper) {
            // if (lockWallpaperUri == null) {
            // lockWallpaperUri = theme.getLockWallpaperUri(context);
            // }
            if (lockWallpaperUri != null) {
                if (WallpaperUtilities.supportsLockWallpaper(context)) {
                    WallpaperUtilities.setLockWallpaper(context, lockWallpaperUri,isDefaultLockScreenWallpaper);
                }
            }
        }

        if (!extendedThemeChange) {
            applyStyleInternal(context, theme);
        } else {
            applyCustom(context, theme, iconsUri != null ? true : false, isSystemApp);
        }
        Intent i = new Intent(ThemeManager.ACTION_THEME_CHANGED)
                .setDataAndType(theme.getUri(context), themeType);
        if (wallpaperUri != null) {
            i.putExtra("wallpaperUri", wallpaperUri);
        }
        i.putExtra("iconChanged", true);
        /* Broadcast theme change. */
        context.sendBroadcast(i);
    }

    /**
     * Extended API to {@link #applyTheme(Context,ThemeItem)} which allows the
     * caller to override certain components of a theme with user-supplied
     * values.
     */
    public static void applyTheme(Context context, ThemeItem theme, Intent request, boolean forcestopLauncher) {

        boolean isNewMechanism = NewMechanismHelp.isNewMechanism(theme) ;
        // Begin, added by yljiang@lewatek.com 2013-12-02
        NewMechanismHelp.applyTheme(context, theme ,request);
        NewMechanismHelp.applyInCallStyle(context,theme,request,false);
        // End
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);
        boolean isSystemApp=request.getBooleanExtra(ThemeManager.EXTRA_SYSTEM_APP, false);
        boolean isDefaultLockScreen=request.getBooleanExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, false);
        boolean isDefaultFont=request.getBooleanExtra(ThemeManager.DEFAULT_FONT, false);
        boolean isOnekeyTheme=request.getBooleanExtra("onekey_theme", false);
        boolean isDefaultIcon=request.getBooleanExtra(ThemeManager.DEFAULT_ICON, false);
        boolean isDefaultLockScreenWallpaper=request.getBooleanExtra(ThemeManager.DEFAULT_LOCKSCREEN_WALLPAPER, false);

        Uri wallpaperUri = null;
        Uri lockWallpaperUri = null;
        ComponentName liveWallPaperComponent = null;
        Uri ringtoneUri = null;
        Uri notificationRingtoneUri = null;

        Uri bootAnimationUri = null;
        Uri fontUri = null;
        Uri lockscreenUri = null;
        Uri iconsUri = null;

        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            wallpaperUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_WALLPAPER_URI);
            lockWallpaperUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI);
            ringtoneUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_RINGTONE_URI);
            notificationRingtoneUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_NOTIFICATION_RINGTONE_URI);
            liveWallPaperComponent = (ComponentName) request.getParcelableExtra(ThemeManager.EXTRA_LIVE_WALLPAPER_COMPONENT);

            bootAnimationUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_BOOT_ANIMATION_URI);
            fontUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_FONT_URI);
            lockscreenUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_LOCKSCREEN_URI);
            iconsUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_ICONS_URI);
        }

        if (Constants.DEBUG) {
            Log.i(Constants.TAG, "applyTheme: theme=" + theme.getUri(context) + ", wallpaperUri=" +
                    wallpaperUri + ", lockWallpaperUri=" + lockWallpaperUri +
                    ", liveWallPaperComponent=" + liveWallPaperComponent + ", ringtoneUri=" +
                    ringtoneUri + ", notificationRingtoneUri=" + notificationRingtoneUri +
                    ", bootAnimationUri=" + bootAnimationUri + ", fontUri=" + fontUri +
                    ", lockscreenUri=" + lockscreenUri + ", dontSetLockWallpaper=" +
                    dontSetLockWallpaper + ",iconsUri=" + iconsUri);
        }

        if (liveWallPaperComponent != null) {
            WallpaperUtilities.setLiveWallpaper(context, liveWallPaperComponent);
        } else {
            if (wallpaperUri == null) {
                wallpaperUri = theme.getWallpaperUri(context);
            }
            if (wallpaperUri != null) {
                Log.d(Constants.TAG,"wallpaperUri="+wallpaperUri);
                WallpaperUtilities.setWallpaper(context, wallpaperUri);
            }
        }

        if (ringtoneUri == null) {
            ringtoneUri = theme.getRingtoneUri(context);
        }
        if (ringtoneUri != null && ! isNewMechanism ) {
            /* Check for special silent uri */
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, ThemeManager.SILENT_RINGTONE_URI.equals(ringtoneUri) ? null : ringtoneUri);
        }

        if (notificationRingtoneUri == null) {
            notificationRingtoneUri = theme.getNotificationRingtoneUri(context);
        }
        if (notificationRingtoneUri != null& ! isNewMechanism)  {
            /* Check for special silent uri */
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION, ThemeManager.SILENT_RINGTONE_URI.equals(notificationRingtoneUri) ? null : notificationRingtoneUri);
        }

        // Woody Guo @ 2012/08/27: LockScreenResourceLoader will create this
        // folder if it doesn't exist
        // ensureThemeFolderExists();
        setBootAnimation(context, theme, bootAnimationUri, false);
        if(fontUri==null){
            fontUri=theme.getFontUril();
        }
        if(isDefaultFont||fontUri!=null){
            setFonts(context, theme, fontUri, false,isDefaultFont);
            if(!ThemeManager.STANDALONE) {
                reloadFont(context);
            }
        }
        setLockscreen(context, theme, lockscreenUri, false, isDefaultLockScreen);
        setIcons(context, theme, iconsUri, false,isDefaultIcon);

        if (!dontSetLockWallpaper) {
            if (lockWallpaperUri == null) {
                lockWallpaperUri = theme.getLockWallpaperUri(context);
            }
            // if (lockWallpaperUri != null) {
            if (WallpaperUtilities.supportsLockWallpaper(context)) {
                WallpaperUtilities.setLockWallpaper(context, lockWallpaperUri,isDefaultLockScreenWallpaper);
            }
            // }
        }
        if (!extendedThemeChange) {
            applyStyleInternal(context, theme,isSystemApp, forcestopLauncher,isOnekeyTheme);
        } else {
            applyCustom(context, theme);
        }
        /* Broadcast theme change. */
        Intent i=new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), themeType);
        if(wallpaperUri!=null){
            i.putExtra("wallpaperUri", wallpaperUri);
        }
        i.putExtra("iconChanged", true);
        context.sendBroadcast(i);
    }

    public static void applySystemApp(Context context, ThemeItem theme, Intent request) {

     // Begin, added by yljiang@lewatek.com 2013-12-02
      NewMechanismHelp.applyTheme(context, theme,request);
     // End
        String themeType = request.getType();
        applyCustom(context, theme,false,true);
        /* Broadcast theme change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), themeType));
    }

    public static void applyBootAnimation(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);

        Uri bootAnimationUri = null;
        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            bootAnimationUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_BOOT_ANIMATION_URI);
        }

        // Woody Guo @ 2012/08/27: LockScreenResourceLoader will create this
        // folder if it doesn't exist
        // ensureThemeFolderExists();
        setBootAnimation(context, theme, bootAnimationUri, false);
        applyCustom(context, theme,false,false);

        /* Broadcast theme change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), themeType));
    }

    /**
     * This function not be supported temporarily
     *
     * @param context
     * @param theme
     * @param request
     */
    public static void applyDeskTopStyle(Context context, ThemeItem theme, Intent request) {

    }

    public static void applyDeskTopWallpaper(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);

        Uri wallpaperUri = null;
        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            wallpaperUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_WALLPAPER_URI);
        }

        if (wallpaperUri == null) {
            wallpaperUri = theme.getWallpaperUri(context);
        }
        if (wallpaperUri != null) {
            WallpaperUtilities.setWallpaper(context, wallpaperUri);
        }

        applyCustom(context, theme, false, false);

        /* Broadcast theme change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), themeType));
    }

    public static void applyFonts(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);
        boolean isDefaultFont=request.getBooleanExtra(ThemeManager.DEFAULT_FONT, false);


        Uri fontUri = null;

        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            fontUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_FONT_URI);
        }

        setFonts(context, theme, fontUri, false, isDefaultFont);
        applyCustom(context, theme,false,false);
        Intent intent = new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), themeType) ;

        //RC48063-jianwu.gao modify begin
        //fix bug : reset wallpaper to default after set font
        Uri wallPaperUri = request.getParcelableExtra("wallpaperUri");
        intent.putExtra("wallpaperUri", wallPaperUri);
        //RC48063-jianwu.gao modify end
        context.sendBroadcast(intent);
        /* Broadcast theme change. */
        if(!ThemeManager.STANDALONE) {
            reloadFont(context);
        }
    }

    public static void applyIcon(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);
        boolean isDefaultIcon=request.getBooleanExtra(ThemeManager.DEFAULT_ICON, false);

        Uri iconsUri = null;
        Uri lockWallpaperUri = null;

        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            iconsUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_ICONS_URI);
            lockWallpaperUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI);
        }
        setIcons(context, theme, iconsUri, false,isDefaultIcon);

        applyCustom(context, theme, true,false);

        /* Broadcast theme change. */
        Intent intent = new Intent(ThemeManager.ACTION_THEME_CHANGED);
        intent.setDataAndType(theme.getUri(context), themeType);
        intent.putExtra("iconChanged", true);
        context.sendBroadcast(intent);
    }

    public static void applyLockScreenStyle(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);
        boolean isDefaultLockScreen=request.getBooleanExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, false);

        Uri lockscreenUri = null;

        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            lockscreenUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_LOCKSCREEN_URI);
        }

        setLockscreen(context, theme, lockscreenUri, true, isDefaultLockScreen);
        applyCustom(context, theme,false,false);

        /* Broadcast theme change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), themeType));
    }

    // Add by Fan.Yang, 默认主题设置lockscreen2，其他设置lockscreen
    private static void setLockscreen(Context context, boolean isFromThemeDetail, String uri) {
        String path = isFromThemeDetail ?
                ThemeManager.INTER_LOCKSCREEN2_PATH :
                ThemeManager.THEME_LOCKSCREEN_DEFAULT;
        if (uri.contains(path)) {
            Settings.System.putString(context.getContentResolver(),
                    ThemeManager.SETTINGS_LOCKSCREEN, ThemeManager.THEME_LOCKSCREEN2_PACKAGE);
        } else {
            Settings.System.putString(context.getContentResolver(),
                    ThemeManager.SETTINGS_LOCKSCREEN, ThemeManager.THEME_LOCKSCREEN_PACKAGE);
        }
    }

    public static void applyLiveWallpaper(Context context, Intent request){
        ComponentName liveWallPaperComponent = (ComponentName) request.getParcelableExtra(ThemeManager.EXTRA_LIVE_WALLPAPER_COMPONENT);
        if (liveWallPaperComponent != null) {
            WallpaperUtilities.setLiveWallpaper(context, liveWallPaperComponent);
        }
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setType(ThemeColumns.STYLE_CONTENT_ITEM_TYPE));
    }

    public static void applyLockScreenWallpaper(Context context, ThemeItem theme, Intent request) {
        String themeType = request.getType();
        boolean extendedThemeChange = request.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false);
        boolean dontSetLockWallpaper = request.getBooleanExtra(ThemeManager.EXTRA_DONT_SET_LOCK_WALLPAPER, false);
        boolean isDefaultLockScreenWallpaper=request.getBooleanExtra(ThemeManager.DEFAULT_LOCKSCREEN_WALLPAPER, false);

        Uri lockWallpaperUri = null;
        /*
         * Extended API is used by profile switch to supply theme "overrides".
         */
        if (extendedThemeChange) {
            lockWallpaperUri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI);
        }

        if (!dontSetLockWallpaper) {
            if (lockWallpaperUri == null) {
                lockWallpaperUri = theme.getLockWallpaperUri(context);
            }
            // if (lockWallpaperUri != null) {
            if (WallpaperUtilities.supportsLockWallpaper(context)) {
                WallpaperUtilities.setLockWallpaper(context, lockWallpaperUri,isDefaultLockScreenWallpaper);
            }
            // }
        }

        applyCustom(context, theme,false,false);

        /* Broadcast theme change. */
        context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(theme.getUri(context), themeType));
    }

    private static void ensureThemeFolderExists() {
        File f = new File(ThemeManager.THEME_ELEMENTS_PATH);
        if (!f.exists()) {
            try {
                FileUtilities.forceMkdir(f, true);
                FileUtilities.setPermissions(f, "755");
            } catch (Exception e) {
                // ignore
            }
        }
        File ready = new File(ThemeManager.THEME_ELEMENTS_PATH + ".ready");
        if (!ready.exists()) {
            try {
                FileUtilities.setPermissions(f);
                ready.createNewFile();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static boolean OPEN_FUNCTION = false ;

    private static void setBootAnimation(Context context
            , ThemeItem theme, Uri bootAnimationUri, boolean isForThemeDetail) {
        // We currently do not support chaning boot animation
        if(!OPEN_FUNCTION)
            return ;
        if (null == bootAnimationUri && !isForThemeDetail) {
            bootAnimationUri = theme.getBootAnimationUri();
        }
        if (null != bootAnimationUri) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = context.getContentResolver().openInputStream(bootAnimationUri);
                out = FileUtilities.openOutputStream(ThemeManager.THEME_ELEMENT_BOOT_ANIMATION);
                FileUtilities.connectIO(in, out);
                FileUtilities.setPermissions(ThemeManager.THEME_ELEMENT_BOOT_ANIMATION);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Could not set boot animation: ", e);
                try {
                    FileUtilities.deleteIfExists(ThemeManager.THEME_ELEMENT_BOOT_ANIMATION);
                } catch (Exception ex) {
                    // ignore
                }
            } finally {
                if (in != null) FileUtilities.close(in);
                if (null != out) FileUtilities.close(out);
            }
        } else {
            // Remove previously applied boot animation if it exists
            if (!isForThemeDetail) {
                try {
                    // FileUtilities.deleteIfExists(THEME_ELEMENT_BOOT_ANIMATION);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    // Save new fonts to a temporarily file
    // Fonts will be unziped on device shutdown
    private static void setFonts(final Context context
            , ThemeItem theme, Uri fontUri, boolean isForThemeDetail,boolean isDefaultFont) {
        if (null == fontUri && !isForThemeDetail&&!isDefaultFont) {
            fontUri = theme.getFontUril();
        }
        InnerFontUtil.resetFontConfig();
        if(fontUri != null && !fontUri.toString().startsWith("content:")){
            if(InnerFontUtil.setFont(fontUri.toString()))
                return;
        }
        boolean success = false;
        File fontDir = new File(ThemeManager.THEME_ELEMENT_FONTS);

        //create default font override directory
        try {
            if (fontDir.exists()) {
                FileUtilities.cleanDirectory(fontDir);
            } else {
                FileUtilities.forceMkdir(fontDir);
                FileUtilities.setPermissions(fontDir);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not remove fonts: ", e);
        }

        if(isDefaultFont || fontUri == null){
//            if(fontUri == null){
//                File fallback = new File("/system/fonts/DroidSansFallback.ttf");
//                File roboto = new File("/system/fonts/Roboto-Regular.ttf");
//                try {
//                    (new File(THEME_ELEMENT_FONTS, ".original")).createNewFile();
//                    if(fallback.exists())
//                        Runtime.getRuntime().exec("ln -s " + fallback.getAbsolutePath() + ' ' + THEME_ELEMENT_FONTS + "/DroidSansFallback.ttf");
//                    if(roboto.exists())
//                        Runtime.getRuntime().exec("ln -s " + roboto.getAbsolutePath() + ' ' + THEME_ELEMENT_FONTS + "/Roboto-Regular.ttf");
//                } catch (Exception e) {
//                }
//            }
            return;
        }

        //create temporary directory and extract temporary font archive
        if(!fontUri.toString().endsWith("tmpFonts")){
            File tmpDir = context.getCacheDir();
            File tmpFile = new File(tmpDir, "tmpFonts");
            InputStream in = null;
            OutputStream out = null;
            try {
                FileUtilities.forceMkdir(tmpDir, true);
                in = context.getContentResolver().openInputStream(fontUri);
                out = FileUtilities.openOutputStream(tmpFile);
                FileUtilities.connectIO(in, out);
            } catch (IOException e) {
                Log.e(Constants.TAG, e.toString());
                if (null != tmpFile)
                try {
                    FileUtilities.deleteIfExists(tmpFile);
                } catch (Exception ex) {
                }
                tmpFile = null;
            } finally {
                if (null != in)
                    FileUtilities.close(in);
                if (null != out)
                    FileUtilities.close(out);
            }
            // apply new fonts
            if(tmpFile != null){
                try {
                    FileUtilities.unzip(tmpFile, ThemeManager.THEME_ELEMENT_FONTS, "755");
                    FileUtilities.deleteIfExists(tmpFile);
                    correctFont(context);
                    success = true;
                } catch (IOException e) {
                    Log.e(Constants.TAG, e.toString());
                } finally {
                    if (null != tmpFile)
                    try {
                        FileUtilities.deleteIfExists(tmpFile);
                    } catch (Exception ex) {
                    }
                }
            }
        } else {
            //get font archive from stream
            InputStream in = null;
            ZipInputStream zin = null;
            OutputStream out = null;
            try {
                in = context.getContentResolver().openInputStream(fontUri);
                zin = new ZipInputStream(in);
                ZipEntry ze;
                while((ze = zin.getNextEntry()) != null){
                    if (ze.isDirectory()) {
                        continue;
                    }
                    File f = new File(fontDir, ze.getName());
                    out = new BufferedOutputStream(new FileOutputStream(f));
                    int n;
                    byte[] buf = new byte[1024];
                    while ((n = zin.read(buf)) != -1)
                        out.write(buf, 0, n);
                    zin.closeEntry();
                    out.flush();
                    out.close();
                    FileUtilities.setPermissions(f, "755");
                }
                correctFont(context);
                success = true;
            } catch (Exception e) {
                Log.e(Constants.TAG, e.toString());
            } finally {
                if (null != zin)
                    FileUtilities.close(zin);
                if (null != in)
                    FileUtilities.close(in);
                if (null != out)
                    FileUtilities.close(out);
            }
        }

        if(!success){
            try {
                FileUtilities.cleanDirectory(fontDir);
            } catch (Exception ex) {
            }
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), R.string.apply_font_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private static void correctFont(Context context){
        try {
            if(Build.VERSION.SDK_INT == 16/*Build.VERSION_CODES.JELLY_BEAN*/ && context.getResources().getBoolean(R.bool.config_change_english_font)){
                File fallback = new File(ThemeManager.THEME_ELEMENT_FONTS, "DroidSansFallback.ttf");
                File roboto = new File(ThemeManager.THEME_ELEMENT_FONTS, "Roboto-Regular.ttf");
                if(fallback.exists() && !roboto.exists()){
                    Runtime.getRuntime().exec("ln -s " + fallback.getAbsolutePath() + ' ' + roboto.getAbsolutePath());
                }
            }
        } catch (Exception e) {
        }
    }

    private static void reloadFont(Context context) {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            //Delete for standalone by Fan.Yang
/*            Configuration config = am.getConfiguration();
            config.setFontChanged((int)System.currentTimeMillis());
            am.updateConfiguration(config);*/
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not reload font", e);
        }
    }

    private static String getCurrentDensityLabel(Context context){
        switch (context.getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return "ldpi";
            case DisplayMetrics.DENSITY_MEDIUM:
                return "mdpi";
            case DisplayMetrics.DENSITY_XHIGH:
                return "xhdpi";
            case 480/*DisplayMetrics.DENSITY_XXHIGH*/:
                return "xxhdpi";
            case DisplayMetrics.DENSITY_HIGH:
            default:
                return "hdpi";
        }
    }

    private static void setIcons(Context context, ThemeItem theme, Uri iconsUri,
            boolean isForThemeDetail, boolean isDefaultIcon) {
        if (null == iconsUri && !isForThemeDetail) {
            iconsUri = theme.getIconsUri();
        }
        // add by Fan.Yang make theme service feel theme changed
        ThemeManager.getInstance(context).reset();

        if (isDefaultIcon) {
            File iconFile = new File(ThemeManager.THEME_ELEMENT_ICONS);
            if (iconFile.exists()) {
                iconFile.delete();
            }
            android.os.SystemProperties.set("sys.lewa.themeChanged",
                    String.valueOf(android.os.SystemClock.elapsedRealtime() / 1000));
            return;
        }
        if (null != iconsUri) {
            // Apply new icons
            InputStream in = null;
            OutputStream out = null;

            // check dpi icon
            final String defName = ".zip";
            String uriStr = iconsUri.toString();
            if (uriStr.endsWith(defName)) {
                try {
                    uriStr = uriStr.substring(0, uriStr.length() - defName.length()) + '_' +
                            getCurrentDensityLabel(context) + defName;
                    in = context.getContentResolver().openInputStream(Uri.parse(uriStr));
                } catch (Exception e) {
                }
            }

            try {
                if(in == null)
                    in = context.getContentResolver().openInputStream(iconsUri);
                out = FileUtilities.openOutputStream(ThemeManager.THEME_ELEMENT_ICONS);
                FileUtilities.connectIO(in, out);
                FileUtilities.setPermissions(ThemeManager.THEME_ELEMENT_ICONS);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Could not set icons", e);
            } finally {
                if (in != null)
                    FileUtilities.close(in);
                if (null != out)
                    FileUtilities.close(out);
            }
        } else {
            // Remove previously applied icons if it exists
            if (!isForThemeDetail) {
                // File iconsFile = new File(THEME_ELEMENT_ICONS);
                // if (iconsFile.exists()) {
                // try {
                // FileUtilities.forceDelete(iconsFile);
                // } catch (Exception e) {
                // Log.e(Constants.TAG, "Could not remove lockscreen", e);
                // }
                // }
            }
        }
    }

    private static void setLockscreen(Context context, ThemeItem theme, Uri lockscreenUri,
            boolean isForThemeDetail, boolean isDefaultLockScreen) {
        if (null == lockscreenUri && !isForThemeDetail) {
            lockscreenUri = theme.getLockscreenUri();
        }
        InputStream in = null;
        OutputStream out = null;
        File lsFile = null;
        lsFile = new File(ThemeManager.THEME_ELEMENT_LOCKSCREEN);
        if(lsFile.exists()){
            lsFile.delete();
        }
        if (null != lockscreenUri) {
            // Extract lockscreen from the applied theme
            try {
                if(!lsFile.exists()){
                    lsFile.createNewFile();
                }
                in = context.getContentResolver().openInputStream(lockscreenUri);
                out = FileUtilities.openOutputStream(ThemeManager.THEME_ELEMENT_LOCKSCREEN);
                FileUtilities.connectIO(in, out);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Can not copy lockStyle file,e:" + e);
            } finally {
                if (in != null)
                    FileUtilities.close(in);
                if (null != out)
                    FileUtilities.close(out);
            }
        } else {
            if (!isDefaultLockScreen) {
                return;
            }
        }
        try {
            android.os.SystemProperties.set("sys.lewa.themeChanged", String.valueOf(android.os.SystemClock.elapsedRealtime() / 1000));
            Settings.System.putInt(context.getContentResolver(),
                    ExtraSettings.System.LOCKSCREEN_CHANGED, 1);
            setLockscreen(context, isForThemeDetail, lockscreenUri.toSafeString());
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not set lockscreen", e);
        } finally {
            if (in != null)
                FileUtilities.close(in);
            if (null != out)
                FileUtilities.close(out);
        }
    }

    public static void updateConfiguration(Context context, CustomTheme theme) {
        updateConfiguration(context, theme.getThemePackageName(), theme.getThemeId());
    }

    public static void updateConfiguration(Context context, ThemeItem theme) {
        updateConfiguration(context, theme.getPackageName(), theme.getThemeId());
    }

    public static void updateConfiguration(Context context, PackageInfo pi, ThemeInfo ti) {
        updateConfiguration(context, pi.packageName, ti.themeId);
    }

    private static void updateConfiguration(Context context, String packageName, String themeId) {
        if(!ThemeManager.STANDALONE) {
            //Delete for standalone by Fan.Yang
/*            Configuration currentConfig = ExtraActivityManager.getConfiguration();

            currentConfig.setCustomTheme(new CustomTheme(themeId, packageName));
            ExtraActivityManager.updateConfiguration(currentConfig);*/
        }
    }

   private static void checkFaceDir() {
        File face = new File("/data/system/face");
        if (!face.exists()) {
            try {
                FileUtilities.forceMkdir(face, true);
                FileUtilities.setPermissions(face);
                Log.v(Constants.TAG, "face folder created!!!!!");
            } catch (Exception e) {
                Log.e(Constants.TAG, "create face folder failed!!!!!,e+" + e);
            }
        }
    }

    public static CustomTheme getAppliedTheme(Context context) {
        // Add by Fan.Yang, face dir must be created in system uid
        checkFaceDir();
        if(!ThemeManager.STANDALONE) {
            //Delete for standalone by Fan.Yang
/*            Configuration config = ExtraActivityManager.getConfiguration();
            return (config.getCustomTheme() != null ? config.getCustomTheme() : CustomTheme.getBootTheme());*/
            return null;
        } else {
            CustomTheme Theme = new CustomTheme(SystemProperties.get("persist.sys.themeId"), SystemProperties.get("persist.sys.themePackageName"));
            return TextUtils.isEmpty(Theme.getThemeId()) ? CustomTheme.getBootTheme() : Theme;
        }
    }

    public static int compareTheme(ThemeItem item, PackageInfo pi, ThemeInfo ti) {
        int cmp = item.getPackageName().compareTo(pi.packageName);
        if (cmp != 0) {
            return cmp;
        }
        return item.getThemeId().compareTo(ti.themeId);
    }

    public static int compareTheme(ThemeItem item, PackageInfo pi, ServiceInfo si) {
        int cmp = item.getPackageName().compareTo(pi.packageName);
        if (cmp != 0) {
            return cmp;
        }
        return item.getThemeId().compareTo(si.name);
    }

    public static boolean themeEquals(PackageInfo pi, ThemeInfo ti, CustomTheme current) {
        if (!pi.packageName.equals(current.getThemePackageName())) {
            return false;
        }
        if (!ti.themeId.equals(current.getThemeId())) {
            return false;
        }
        return true;
    }
}
