/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lewa.themechooser.server.intf;

import android.os.Build;
import android.util.Log;

import com.lewa.themes.ThemeManager;

import java.util.Locale;

import util.ThemeUtil;

/**
 * @author Administrator
 */
public class NetBaseParam {
    public static final String Host = "http://admin.lewatek.com";
    public static final String Port = "";
    public static final String Path = "/themeapi2";
    public static final String PrefixUrl = Host + Port + Path;
    public static final String B2B = "business";
    public static final String BRAND = "brand";
    public static final String DEVICE = "device";
    public static final String PAGE = "page";
    public static final String THEME_PLATEFORM = "theme_plateform";
    public static final String WALLPAPER = "wallpaper";
    public static final String THEMEPACKAGE = "themepackage";
    public static final String LOCKSCREEN = "lockscreen";
    public static final String BOOTANIMATION = "animation";
    public static final String FONT = "font";
    public static final String ICONSTYLE = "ICONSTYLE";
    public static final String FAVORITEBAR = "FAVORITEBAR";
    public static final String SYSTEMUI = "SYSTEMUI";
    public static final String SYSTEM = "system_application";
    public static final String LIVE_WALLPAPER = "liveWallpapr";
    public static final String DPI = "dpi";
    public static final String SCREENSCHEMA_WVGA = "WVGA";
    public static final String SCREENSCHEMA_HVGA = "HVGA";
    public static final String SCREENSCHEMA = "resolution";
    public static final String TID = "Tid";
    public static final String ThEMEID = "Themeid";
    public static final String PACKAGENAME = "Packagename";
    public static final String NAME_ZH = "Name_zh";
    public static final String NAME_EN = "Name_en";
    public static final String MODULE_NAME = "Module_name";
    public static final String PKG_NAME = "Filename";
    public static final String AUTHOR = "Author";
    public static final String AUTHOR_EN = "Author_en";
    public static final String RESOLUTION = "Resolution";
    public static final String CONST_SYS_VERSION = "system_version";
    public static final String INTERNALVERSION = "Internalversion";
    public static final String THEME_SIZE = "Size";
    public static final String ATTACHMENT = "Attachment";
    public static final String PREVIEW = "Previewpath";
    public static final String THUMB = "Thumbnailpath";
    public static final String MODULE_NUM = "Module_num";
    public static final String PKG_VERSION = "Theme_version";
    public static final String DATELINE = "Dateline";
    public static final String DOWNLOAD = "Downloads";
    public static final String LANG = "lang";
    public static final String VERSION_2_3_7 = "2.3.7";
    public static final String VERSION_4_0 = "4.0";
    // Begin, added by yljiang@lewatek.com 2013-12-25
    public static final String VERSION_5_0 = "5.0";
    public static final String VERSION_6_0 = "6.0";
    public static String SYS_VERSION = Build.VERSION.SDK_INT < 14 ? VERSION_2_3_7 : VERSION_6_0;
    // End
    public static final String PLATEFORM = "2";
    private final static String TAG = "NetBaseParam";
    private final static boolean DBG = false;
    public String screenSchema = "";
    public String actualtype = "";
    public String mType = "";
    public String screenDpi = "";
    public int typeId;
    public boolean isGetTheme = false;
    public String themeId;
    // public static String SYS_VERSION = VERSION_4_0;
    public int combineModelInt = -1;


    public NetBaseParam(String type) {
        this.mType = actualtype = type;
        if (type.equals(ICONSTYLE)) {
            combineModelInt = 5;
        } else if (type.equals(FAVORITEBAR)) {
            combineModelInt = 3;
        }
    }

    public static boolean isPackgeResource(String type) {
        return type.equalsIgnoreCase(ICONSTYLE)
                || type.equalsIgnoreCase(FAVORITEBAR);
    }

    public static boolean isThemeResource(String type) {
        return type.equalsIgnoreCase(THEMEPACKAGE);
    }

    public static boolean isGetTheme(boolean getTheme) {
        return getTheme;
    }

    public static String getCurrentScreenSchema() {
        return ThemeUtil.isWVGA ? SCREENSCHEMA_WVGA : SCREENSCHEMA_HVGA;
    }

    public int getCombineModelInt() {
        return combineModelInt;
    }

    public String changeString(int page) {
        if (isGetTheme) {
            Log.d("windy", "toThemeUrl");
            return toThemeUrl(themeId, page);
        }
        if (!isThemeResource(mType)) {
            Log.d("windy", "toAllUrl");
            return toAllUrl(mType, typeId, page);
        }
        Log.d("windy", "toGenrericModuleUrl");
        return toGenrericModuleUrl(page);
    }

    public String toThemeUrl(String themeId, int page) {
        String dpiParam = "";
        String systemVersionParam = "";
        String param = "";
        dpiParam = resolveDPIParam();
        systemVersionParam = "&" + CONST_SYS_VERSION + "=" + NetBaseParam.SYS_VERSION;
        param += dpiParam.startsWith("&") ? dpiParam : "&" + dpiParam + systemVersionParam;
        param += "&" + B2B + "=" + (ThemeUtil.CONFIG_IS_B2B ? "2" : "1");
        param += "&" + BRAND + "=" + ThemeUtil.CONFIG_BRAND;
        param += "&" + DEVICE + "=" + ThemeUtil.CONFIG_DEVICE;
        param += "&" + PAGE + "=" + page;
        param += "&" + THEME_PLATEFORM + "=" + PLATEFORM;
        if (ThemeUtil.CONFIG_IS_B2B) {
            param += "&" + LANG + "=" + Locale.getDefault().getLanguage();
        } else {
            param += "&" + LANG + "=zh";
        }
        return NetBaseParam.PrefixUrl + "/" + "gettheme?themeid=" + themeId + param;
    }

    public String toAllUrl(String customType, int typeId, int page) {
        String MODULE = "moduleid=";
        String type = "";
        int moduleId = -1;
        String param = "";
        String moduleParam = "";
        String dpiParam = "";
        String systemVersionParam = "";
        String resolutionParam = "";
        if (DBG) Log.d(TAG, "==> " + this.mType);
        if (mType.equals(WALLPAPER)) {
            resolutionParam = resolveResolutionParam();
        }
        dpiParam = resolveDPIParam();
        systemVersionParam = "&" + CONST_SYS_VERSION + "="
                + NetBaseParam.SYS_VERSION;
        String pagingStr = "";
        String pagingParam = "";
        if (DBG) Log.d(TAG, "mType=== " + mType);

        type = "getmodule";
        moduleId = typeId;
        moduleParam += MODULE + moduleId;
        param += moduleParam
                + (dpiParam.startsWith("&") ? dpiParam : "&" + dpiParam)
                + systemVersionParam
        ;
        param += "&" + B2B + "=" + (ThemeUtil.CONFIG_IS_B2B ? "2" : "1");
        param += "&" + BRAND + "=" + ThemeUtil.CONFIG_BRAND;
        param += "&" + DEVICE + "=" + ThemeUtil.CONFIG_DEVICE;
        param += "&" + THEME_PLATEFORM + "=" + PLATEFORM;
        param += "&" + PAGE + "=" + page;
        if (ThemeUtil.CONFIG_IS_B2B) {
            param += "&" + LANG + "=" + Locale.getDefault().getLanguage();
        } else {
            param += "&" + LANG + "=zh";
        }
        if (mType.equals(WALLPAPER)) {
            param += "&" + resolutionParam;
        }
        if (!(dpiParam.trim().equals("")
                && pagingParam.trim().equals("") && systemVersionParam.trim()
                .equals(""))) {
            param = "?" + param;
        }
        String result = NetBaseParam.PrefixUrl + "/" + type + pagingStr + param;
        if (DBG) Log.d(TAG, "==" + result);
        return result;
    }

    /**
     * 分辨率参数  当是字体时无需分辨率
     *
     * @return
     */
    public String resolveResolutionParam() {
        String resolutionParam;
        if (mType.equalsIgnoreCase(FONT)) {
            resolutionParam = "";
        } else {
            resolutionParam = SCREENSCHEMA + "=" + screenSchema;
        }
        return resolutionParam;
    }

    public String resolveDPIParam() {
        String dpi;
        if (mType.equalsIgnoreCase(FONT)) {
            dpi = "";
        } else {
            dpi = DPI + "=" + screenDpi;
        }
        return dpi;
    }

    public String toGenrericModuleUrl(int page) {
        String dpiParam;
        String systemVersionParam;
        if (DBG) Log.d(TAG, "==> " + mType);
        if (mType.equals(WALLPAPER)) {
            dpiParam = "";
            systemVersionParam = "";
        } else {
            dpiParam = resolveDPIParam();
            systemVersionParam = "&" + CONST_SYS_VERSION + "="
                    + NetBaseParam.SYS_VERSION;
        }
        String pagingStr = "";
        String pagingParam = "";
        if (mType.equals(ICONSTYLE) || mType.equals(FAVORITEBAR)) {
            mType = THEMEPACKAGE;
        }
        String param = dpiParam + pagingParam;
        if (!mType.equals(BOOTANIMATION)) {
            param += systemVersionParam;
        }
        param += "&" + B2B + "=" + (ThemeUtil.CONFIG_IS_B2B ? "2" : "1");
        param += "&" + BRAND + "=" + ThemeUtil.CONFIG_BRAND;
        param += "&" + DEVICE + "=" + ThemeUtil.CONFIG_DEVICE;
        param += "&" + THEME_PLATEFORM + "=" + PLATEFORM;
        param += "&" + PAGE + "=" + page;
        if (ThemeUtil.CONFIG_IS_B2B) {
            param += "&" + LANG + "=" + Locale.getDefault().getLanguage();
        } else {
            param += "&" + LANG + "=zh";
        }
        if (!(dpiParam.trim().equals("")
                && pagingParam.trim().equals("") && systemVersionParam.trim()
                .equals(""))) {
            param = "?" + param;
        }
        return NetBaseParam.PrefixUrl + "/" + mType + pagingStr + param;
    }

}
