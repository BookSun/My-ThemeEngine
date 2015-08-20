package com.lewa.themechooser;

import android.app.Application;
import android.os.Environment;

public interface ThemeConstants {
    public static final boolean DEBUG = true;
    public static final String TAG = "ThemeChooser";
    public static final String URL = "http://static.lewatek.com/theme";
    public static final String LEWA = "LEWA";
    public static final int THUMBNAIL_WVGA_HEIGHT = 348;
    public static final int THUMBNAIL_WVGA_WIDTH = 240;

    public static final int THUMBNAIL_HVGA_HEIGHT = 232;
    public static final int THUMBNAIL_HVGA_WIDTH = 159;
    public static final String THEMEBASE = "themeBase";
    public static final int SECOND = 1;
    public static final String applyingName = "applyName";


    public static final String THEME_THUMBNAIL_LOCKSCREEN_PREFIX = "lockscreen_";
    public static final String THEME_THUMBNAIL_THUMBNAIL = "thumbnail_";
    public static final String THEME_THUMBNAIL_ICONS_PREFIX = "icons_";
    public static final String THEME_THUMBNAIL_LIVEWALLPAPER_PREFIX = "live_wallpaper_";
    public static final String THEME_THUMBNAIL_PIM_PREFIX = "pim_";
    public static final String THEME_THUMBNAIL_PHONE_PREFIX = "phone_";
    public static final String THEME_THUMBNAIL_SETTING_PREFIX = "setting_";
    public static final String THEME_THUMBNAIL_LAUNCHER_PREFIX = "launcher_";

    public static final String THEME_THUMBNAIL_WALLPAPER_PREFIX = "wallpaper_";
    public static final String THEME_THUMBNAIL_LOCKSCREEN_WALLPAPER_PREFIX = "lockscreen_wallpaper_";
    public static final String THEME_THUMBNAIL_STYLE_PREFIX = "style_";
    public static final String THEME_THUMBNAIL_BOOTS_PREFIX = "boots_";
    public static final String THEME_THUMBNAIL_SYSTEMUI_PREFIX = "notification_";
    public static final String THEME_THUMBNAIL_FONTS_PREFIX = "fonts_";
    public static final String THEME_THUMBNAIL_OTHERS_PREFIX = "other_";

    public static final String MODEL_PREVIEW_LOCKSCREEN = "lockscreen";
    public static final String MODEL_PREVIEW_WALLPAPER = "wallpaper";
    public static final String MODEL_PREVIEW_LOCKSCREEN_WALLPAPER = "lock_screen_wallpaper";
    public static final String MODEL_PREVIEW_PIM = "PIM_";
    public static final String MODEL_PREVIEW_PHONE = "phone_";
    public static final String MODEL_PREVIEW_SETTING = "setting_";
    public static final String MODEL_PREVIEW_ICONS = "icons";
    public static final String MODEL_PREVIEW_LAUNCHER = "launcher";
    public static final String MODEL_PREVIEW_BOOTANIMATION = "bootanimation";
    public static final String MODEL_PREVIEW_FONTS = "fonts";
    public static final String MODEL_PREVIEW_NOTIFY = "notification_";
    public static final String MODEL_PREVIEW_OTHERS = "other_";
    public static final String PREVIEW_ICONS = "icons_";
    public static final String PREVIEW_LOCKSCREEN_STYLE = "lockscreen_";
    public static final String PREVIEW_LAUNCHER_STYLE = "launcher_";
    public static final String PREVIEW_BOOTS = "bootanimation_";
    public static final String PREVIEW_FONTS = "fonts_";
    public static final String PREVIEW_NOTIFICATION = "notification_";

    public static final String PREVIEW_SETTING = "setting_";
    public static final String PREVIEW_LOCKSCREEN_WALLPAPER = "lock_screen_wallpaper_";
    public static final String PREVIEW_WALLPAPER = "wallpaper_";
    public static final String PREVIEW_PIM = "pim_";
    public static final String PREVIEW_PHONE = "phone_";
    public static final String PREVIEW_OTHER = "other_";

    public static final String THEME_PATH = "/LEWA/theme/lwt/";
    public static final String WALLPAPER_PATH = "/LEWA/theme/deskwallpaper/";
    public static final String LOCKWALLPAPER_PATH = "/LEWA/theme/lockwallpaper/";
    public static final String SDCARD_ROOT_PATH = Environment.getExternalStorageDirectory().getPath();

    public static final String LEWA_PATH = SDCARD_ROOT_PATH + "/LEWA";

    public static final String LEWA_THEME_PATH = LEWA_PATH + "/theme";

    public static final String THEME_LWT = LEWA_THEME_PATH + "/lwt";

    public static final String THEME_WALLPAPER = LEWA_THEME_PATH + "/deskwallpaper";

    public static final String THEME_LOCK_SCREEN_WALLPAPER = LEWA_THEME_PATH + "/lockwallpaper";

    public static final String THEME_ICONS = LEWA_THEME_PATH + "/icons";

    public static final String THEME_PREVIEW = LEWA_THEME_PATH + "/preview";
    
    public static final String THEME_NEW_WALLPAPER = LEWA_THEME_PATH + "/wallpaper";

    public static final String THEME_LOCAL_PREVIEW = THEME_PREVIEW + "/local";

    public static final String THEME_ONLINE_PREVIEW = THEME_PREVIEW + "/online";

    public static final String THEME_ONLINE_WALLPAPRE = THEME_ONLINE_PREVIEW + "/wallpaper";

    public static final String THEME_THUMBNAIL = LEWA_THEME_PATH + "/thumbnail";

    public static final String THEME_LOCAL_THUMBNAIL = THEME_THUMBNAIL + "/local";
    public static final String THEME_LOCAL_WALLPAPER_THUMBNAIL = THEME_LOCAL_THUMBNAIL + "/wallpaper";
    public static final String THEME_ONLINE_THUMBNAIL = THEME_THUMBNAIL + "/online";
    public static final String THEME_ONLINE_WALLPAPER_THUMBNAIL = THEME_ONLINE_THUMBNAIL + "/wallpaper";

    public static final String THEME_RINGTONE = LEWA_THEME_PATH + "/ringtone";

    public static final String THEME_MODEL = LEWA_THEME_PATH + "/model";

    public static final String THEME_MODEL_LOCKSCREEN_STYLE = THEME_MODEL + "/lockscreen";

    public static final String THEME_MODEL_ICONS_STYLE = THEME_MODEL + "/icons";

    public static final String THEME_MODEL_LAUNCHER_STYLE = THEME_MODEL + "/launcher";

    public static final String THEME_MODEL_BOOTS_STYLE = THEME_MODEL + "/boots";

    public static final String THEME_MODEL_FONTS_STYLE = THEME_MODEL + "/fonts";

    public static final String LIVEWALLPAPER_INTENT = "android.live_wallpaper.intent";
    public static final String LIVEWALLPAPER_INFO_INTENT = "live_wallpaper_info_intent";

    public static final int THEMEPKG = 0;
    public static final int ICONS = 1;
    public static final int LOCKSCREEN = 2;
    public static final int LAUNCHER = 3;
    public static final int WALLPAPER = 4;
    public static final int LSWALLPAPER = 5;
    public static final int BOOTS = 6;
    public static final int FONTS = 7;

    public static final int LOCKSCREEN_STYLE = 1;
    public static final int LOCKSCREEN_WALLPAPER = 2;
    public static final int DESKTOP_STYLE = 3;
    public static final int DESKTOP_WALLPAPER = 4;
    public static final int ICON = 5;
    public static final int BOOTANIMATION = 9;
    public static final int FONT = 10;
    public static final int LIVE_WALLPAPER = 12;

    public static final int THEME_LOCAL = 0;
    public static final int THEME_ONLINE = 1;


    public static final int B2B = 2;
    public static final int NOTB2B = 1;

    public static final int DEFAULT_PAGE_SIZE = 18;

    public static String PREFS_NAME = "CUSTOM_URI";
    /**
     * 下载过程中Notification id
     */
    public static final int DOWNLOAD_NOTIFICATION_ID = 1;

    /**
     * 下载完成且成功
     */
    public static final int DOWNLOADED = 0;
    /**
     * 下载中
     */
    public static final int DOWNLOADING = 1;
    /**
     * 下载失败
     */
    public static final int DOWNLOADFAIL = 2;
    /**
     * 服务器上有新的版本
     */
    public static final int DOWNNEWVERSION = 3;
    /**
     * 正在使用
     */
    public static final int DOWNUSING = 4;

    /**
     * 发送Intent标志
     */
    public static final String KEY_LEWA_SEND_FLAG = "com.lewa.filemgr.SEND_FLAG";
    public static final int VALUE_LEWA_SEND_FLAG = 1;
    public static final int VALUE_LEWA_MULTY_SEND_FLAG = 2;
    public static final String IMAGE_CACHE_DIR = "thumbs";
    public static Application APPLICATION = null;
    public static ThemeStatus sThemeStatus = null;
    public static boolean sWallpaperChanged = false;

    public static final class GoToInvokeLWT {

        public static final String ACTION_INVOKE_LWT = "com.lewa.lwt.action";
        public static final String INVOKE_LWT_FILE_FIELD = "com.lewa.lwt.field.filepath";
        public static final String FLAG_INVOKE_LWT = "filemgr";
        public static final String FLAG_KEY_INVOKE_LWT = "from";
        public static final String ACTION_DELETE_LWT = "com.lewa.lwt.delete.action";
    }

}
