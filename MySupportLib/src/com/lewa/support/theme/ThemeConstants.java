
package com.lewa.support.theme;

import static com.lewa.support.util.Process.PACKAGE_NAME;

public class ThemeConstants {

    public static final String CONFIG_THEME_DEFAULT_PATH = "/system/media/theme/";

    public static final String CONFIG_THEME_CUSTOM_PATH = "/data/system/face/";

    public static final String CONFIG_THEME_STANDALONE_PATH = "/data/data/" + PACKAGE_NAME
            + "/files/";

    public static final String CONFIG_LOCKSTYLE_NAME = "lockstyle";

    public static final String CONFIG_LOCKSTYLE_DEFAULT_PATH = CONFIG_THEME_DEFAULT_PATH
            + CONFIG_LOCKSTYLE_NAME;

    public static final String CONFIG_LOCKSTYLE_CUSTOM_PATH = CONFIG_THEME_CUSTOM_PATH
            + CONFIG_LOCKSTYLE_NAME;

    public static final String CONFIG_LOCKSTYLE_STANDALONE_PATH = CONFIG_THEME_STANDALONE_PATH
            + CONFIG_LOCKSTYLE_NAME;

    public static final String CONFIG_FANCYWALLPAPER_NAME = "fancywallpaper";

    public static final String CONFIG_FANCYWALLPAPER_DEFAULT_PATH = CONFIG_THEME_DEFAULT_PATH
            + CONFIG_FANCYWALLPAPER_NAME;

    public static final String CONFIG_FANCYWALLPAPER_CUSTOM_PATH = CONFIG_THEME_CUSTOM_PATH
            + CONFIG_FANCYWALLPAPER_NAME;

    public static final String CONFIG_FANCYWALLPAPER_STANDALONE_PATH = CONFIG_THEME_STANDALONE_PATH
            + CONFIG_FANCYWALLPAPER_NAME;

    public static final String CONFIG_LOCKWALLPAPER_NAME = "lockwallpaper";

    public static final String CONFIG_LOCKWALLPAPER_DEFAULT_PATH = CONFIG_THEME_DEFAULT_PATH
            + CONFIG_LOCKWALLPAPER_NAME;

    public static final String CONFIG_LOCKWALLPAPER_CUSTOM_PATH = CONFIG_THEME_CUSTOM_PATH
            + CONFIG_LOCKWALLPAPER_NAME;

    public static final String CONFIG_LOCKWALLPAPER_STANDALONE_PATH = CONFIG_THEME_STANDALONE_PATH
            + CONFIG_LOCKWALLPAPER_NAME;

    public static final String CONFIG_ICONS_NAME = "icons";

    public static final String CONFIG_ICONS_DEFAULT_PATH = CONFIG_THEME_DEFAULT_PATH
            + CONFIG_ICONS_NAME;

    public static final String CONFIG_ICONS_CUSTOM_PATH = CONFIG_THEME_CUSTOM_PATH
            + CONFIG_ICONS_NAME;

    public static final String CONFIG_ICONS_STANDALONE_PATH = CONFIG_THEME_STANDALONE_PATH
            + CONFIG_ICONS_NAME;

    public static final String CONFIG_LOCKSTYLE_SUBFOLDER = "advance/";

    public static final String CONFIG_ICON_RES_SUBFOLDER = "res/";

    public static final String CONFIG_EXTRA_PATH = CONFIG_THEME_CUSTOM_PATH + "lockscreen.config";

}
