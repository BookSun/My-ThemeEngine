package com.lewa.themechooser.newmechanism;

import com.lewa.themechooser.ThemeConstants;
import com.lewa.themes.ThemeManager;

/**
 * 定义的全局的变量
 * Globals.java:
 *
 * @author yljiang@lewatek.com 2013-12-2
 */
public class Globals {

    public static final String ACTION_THEME_CHANGED = "com.lewa.intent.action.THEME_CHANGED";
    public static final String PROVIDER_PATH = "content://com.lewa.themechooser.fileprovider";
    public static final String TAG = "JYL";
    public static final String JPG = ".jpg";
    public static final String PNG = ".png";
    public static final String ZIP = ".zip";
    public static final String FILE_TITLE = "file://";
    public static final String ROOT = "su0";
    public static final int NEW_MECHANISM = 1;
    public static final String SPLIT = ThemeManager.SPLIT;
    public static final String OTHER = ThemeManager.OTHER;
    public static final String FRAMEWORK = ThemeManager.FRAMEWORK;
    public static final String LEWA = ThemeManager.LEWA;
    public static final String BOOT_ANIMATION = "bootanimation";
    public static final String BOOT_ANIMATION_lOCAL = " /data/local/";
    public static final String ICON = "icons";
    public static final String FONT = "fonts";
    public static final String LOCKSCREAN = "lockscreen";
    public static final String WALLPAPER_LOCKSCREEN = "wallpaper_lockscreen.jpg";
    public static final String ZIP_WALLPAPER_LOCKSCREEN = "wallpaper/" + WALLPAPER_LOCKSCREEN;
    public static final String INCALL_RINGTONE = "incall_ringtone";
    public static final String MESSAGE_RINGTONE = "message_ringtone";
    public static final String NOTIFICATION_RINGTONE = "notif_ringtone";
    public static final String INCALL_STYLE = "incallstyle";
    public static final String WALLPAPER = "wallpaper.jpg";
    ;
    public static final String ZIP_WALLPAPER = "wallpaper/" + WALLPAPER;
    public static final String SD_THEME_RES = ThemeConstants.LEWA_THEME_PATH + "/res";
    public static final String SD_THEME_THUMBNAIL = ThemeConstants.THEME_THUMBNAIL;
    public static final String SD_THEME_PREVIEW = ThemeConstants.THEME_PREVIEW;
    public static final String SD_THEME_WALLPAPER = ThemeConstants.THEME_NEW_WALLPAPER;
    
    public static final String ZIP_DESCRIPTION = "description.xml";
    public static final String ZIP_THUMBNAI = "thumbnail/thumbnail";
    public static final String ZIP_THUMBNAI_THEME = ZIP_THUMBNAI + JPG;
    public static final String ZIP_THUMBNAIL_LAUNCHER = ZIP_THUMBNAI + "_launcher.jpg";
    public static final String ZIP_THUMBNAIL_LOCKSCREEN = ZIP_THUMBNAI + "_lockscreen.jpg";
    public static final String ZIP_THUMBNAIL_SYSTEMAPP = ZIP_THUMBNAI + "_systemapp.jpg";
    public static final String ZIP_THUMBNAIL_FONT = ZIP_THUMBNAI + "_fonts.jpg";
    public static final String ZIP_PREVIEW = "preview/preview";
    public static final String ZIP_PREVIEW_WALLPAPER = "wallpaper/wallpaper.jpg";
    public static final String ZIP_PREVIEW_ICON = ZIP_PREVIEW + "_launcher";
    public static final String ZIP_PREVIEW_LOCKSCREEN = ZIP_PREVIEW + "_lockscreen";
    public static final String ZIP_PREVIEW_LOCKSCREEN_WALLPAPER = "wallpaper/wallpaper_lockscreen.jpg";
    public static final String ZIP_PREVIEW_SYSTEMAPP = ZIP_PREVIEW + "_systemapp";
    public static final String ZIP_PREVIEW_FONT = ZIP_PREVIEW + "_fonts";
    private static final String ZIP_RINGTONE = "ringtone/";
    public static final String ZIP_RINGTONE_INCALL = ZIP_RINGTONE + INCALL_RINGTONE;
    public static final String ZIP_RINGTONE_MESSAGE = ZIP_RINGTONE + MESSAGE_RINGTONE;
    public static final String ZIP_RINGTONE_NOTIFATION = ZIP_RINGTONE + NOTIFICATION_RINGTONE;
}
