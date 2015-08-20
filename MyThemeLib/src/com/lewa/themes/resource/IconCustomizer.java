
package com.lewa.themes.resource;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import com.lewa.themes.Manifest;
import com.lewa.themes.ThemeManager;
import lewa.graphics.BitmapInfo;
import lewa.graphics.IBitmapFilter;
import lewa.os.FileUtilities;
import lewa.os.Shell;
import lewa.os.SuExecuter;
import lewa.util.ImageUtils;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static lewa.os.Process.IS_SYSTEM;
import static lewa.os.Process.PACKAGE_NAME;

public class IconCustomizer {
    public static final String CUSTOMIZED_ICON_PATH = /*IS_SYSTEM ?*/ "/data/system/customized_icons/";
/*            : "/data/data/" + PACKAGE_NAME + "/files/customized_icons/";*/

    public static final String CUSTOMIZED_ICON_STAMP = CUSTOMIZED_ICON_PATH + ".stamp";

    private static final String ICON_ALIAS = "/system/etc/icon_alias.config";

    private static final String MOD_ICONS_NAME = "mod_icons";

    private static final String MOD_BUILT_IN_ICONS = "/system/media/theme/" + MOD_ICONS_NAME;

    private static final String MOD_EXTRA_ICONS_DIR = "/data/data/com.lewa.market/files/"
            + MOD_ICONS_NAME + "/";

    public static final String FANCY_ICONS_INNER_PATH = "fancy_icons/";

    private static final String ICON_TRANSFORM_CONFIG = "icon_transform.config";

    private static final String LOG_TAG = "IconCustomizer";

    private static final int ALPHA_MIN = 230;

    private static final int ALPHA_ATOP = 150;

    private static HashMap<String, String> sIconMapping = new HashMap<String, String>();

    private static final int sDensity = Resources.getSystem().getDisplayMetrics().densityDpi;

    private static final int sScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

    private static final int sScaledDensity = scaleDensity(sDensity);

    private static ZipFile sModIcons;

    // Begin, added by zhumeiquan, for scaling App Icon, 20131205
    // if it is a 480P phone icon will scale to 85px so it will be multiplied with 0.884. 
    public static float mIconScale = 1.0f;
    // it is a raw value without scaling by 0.884
    public static float mRawIconScale = 1.0f;
    // End

    public static int sCustomizedIconWidth = (int) (
            Resources.getSystem().getDisplayMetrics().density * 58 + 0.5f);
    //Resources.getSystem().getDimensionPixelSize(lewa.R.dimen.custom_icon_size);

    public static int sCustomizedIconHeight = sCustomizedIconWidth;

    public static Rect sCustomizedIconRect = new Rect(0, 0, sCustomizedIconWidth,
            sCustomizedIconHeight);

    private static final HashMap<String, SoftReference<Bitmap>> sRawIconCache = new HashMap<String, SoftReference<Bitmap>>();

    private static final HashMap<String, WeakReference<Bitmap>> sIconCache = new HashMap<String, WeakReference<Bitmap>>();

    private static Resources sSystemResource = Resources.getSystem();

    private static final Canvas sCanvas = new Canvas();

    private static final Canvas sCanvasForTransformBitmap = new Canvas();

    private static Paint sPaintForBitmap = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);

    private static Paint sPaintForBackground = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);

    private static Paint sPaintForMask = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private static Context sContext;

    private static IconConfig sIconConfig = null;

    private static Matrix sIconTransformMatrix = null;

    public static final int DEFAULT_ICON_SIZE = 96; // HDPI icon size;

    public static final float ICON_SCALE_480P =
            480f / 720f * 128f / DEFAULT_ICON_SIZE;// 480 / 720 * 128 = 85

    public final static boolean DEBUG = SystemProperties.getBoolean("debug.IconCustomizer", false);

    private final static int TRY_AGAIN_COUNT = 3;//for some time get mask bitmap is null
    private static boolean isMaskNormal;

    static {
        IconAlias.loadAlias(sIconMapping, ICON_ALIAS);
        sCanvas.setDrawFilter(
                new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
        sPaintForBackground.setXfermode(new PorterDuffXfermode(Mode.DST_OVER));
        sPaintForMask.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        if (Shell.sLoaded) {
            Shell.mkdirs(CUSTOMIZED_ICON_PATH, 0777);
        }
    }

    /**
     * load icon alias configuration file form custom path
     *
     * @param config
     */
    public static void loadIconAlias(String config) {
        sIconMapping.clear();
        IconAlias.loadAlias(sIconMapping, config);
    }

    /**
     * add icon alias
     *
     * @param key
     * @param value
     */
    public static void addIconMapping(String key, String value) {
        sIconMapping.put(key, value);
    }

    private static int scaleDensity(int densityDpi) {
        switch (densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return DisplayMetrics.DENSITY_MEDIUM;
            case DisplayMetrics.DENSITY_MEDIUM:
                return DisplayMetrics.DENSITY_HIGH;
            case DisplayMetrics.DENSITY_HIGH:
                return DisplayMetrics.DENSITY_XHIGH;
            case DisplayMetrics.DENSITY_XHIGH:
            case 480/* DisplayMetrics.DENSITY_XXHIGH */:
                return 480/* DisplayMetrics.DENSITY_XXHIGH */;
            default:
                return (int) (1.5 * densityDpi);
        }
    }

    // Begin, modified by zhumeiquan, for scaling App Icon, 20131205
    public static void setIconScale(float scale) {
        mRawIconScale = scale;
        if (sDensity == 240 && sScreenWidth == 480) {
            scale = ICON_SCALE_480P * scale;
            //MODIFY FOR MX2 BEGIN
        } else if (sDensity == DisplayMetrics.DENSITY_XHIGH && sScreenWidth == 800) {
            mRawIconScale = scale * (720f / sScreenWidth);
        }
        //MODIFY FOR MX2 END
        if (mIconScale != scale) {
            mIconScale = scale;
            sCustomizedIconHeight = sCustomizedIconWidth = scalePixel(96);
            sCustomizedIconRect = new Rect(0, 0, sCustomizedIconWidth, sCustomizedIconHeight);
        }
    }
    // End

    private static int scalePixel(int px) {
        int density = sDensity;
        int i = (px * density) / 240;
        // Begin, modified by zhumeiquan, for scaling App Icon, 20131205
        return (int) ((i + i % 2) * mIconScale);
        // End
    }

    private static String LAST_MODIFIED_ICONS_TIME = "last_modified_icons_time";

    /**
     * should call by IconService
     */
    public static void checkModIcons() {
        try {
            File iconsFile = new File(ThemeManager.THEME_ICON_CONFIG);
            if (iconsFile.exists()) {
                if(DEBUG){
                    Log.d(LOG_TAG, "icons file exist");
                }
                long modifiedTime = iconsFile.lastModified();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(sContext);
                long lastModifiedTime = sp.getLong(LAST_MODIFIED_ICONS_TIME, 0);

                if (modifiedTime > lastModifiedTime) {
                    sp.edit().putLong(LAST_MODIFIED_ICONS_TIME, modifiedTime).commit();
                    clearCustomizedIcons(null);
                }
            }

            File file = new File(CUSTOMIZED_ICON_PATH);
            if (file.exists()) {
                long stamp = Libcore.os.lstat(MOD_BUILT_IN_ICONS).st_size;
                long modifiedTime = 0;
                File extra = new File(MOD_EXTRA_ICONS_DIR);
                if (extra.exists()) {
                    modifiedTime = extra.lastModified();
                }
                long createdTime = Libcore.os.lstat(CUSTOMIZED_ICON_PATH).st_ctime;
                if (modifiedTime <= createdTime) {
                    File stampFile = new File(CUSTOMIZED_ICON_STAMP);
                    if (stampFile.exists()) {
                        BufferedReader reader = new BufferedReader(new FileReader(stampFile));
                        long stampLast = Long.valueOf(reader.readLine());
                        reader.close();
                        if (stamp == stampLast) {
                            return;
                        }
                    } else {
                        Shell.write(CUSTOMIZED_ICON_STAMP, String.valueOf(stamp));
                        Shell.chmod(CUSTOMIZED_ICON_STAMP, 0777);
                        return;
                    }
                    clearCustomizedIcons(null);
                }
            } else {
                Shell.mkdirs(CUSTOMIZED_ICON_PATH, 0777);
            }
            long stamp = Libcore.os.lstat(MOD_BUILT_IN_ICONS).st_size;
            Shell.write(CUSTOMIZED_ICON_STAMP, String.valueOf(stamp));
            Shell.chmod(CUSTOMIZED_ICON_STAMP, 0777);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    public static void clearCache() {
        synchronized (sRawIconCache) {
            sRawIconCache.clear();
        }
        synchronized (sIconCache) {
            sIconCache.clear();
        }
        sIconConfig = null;
        sIconTransformMatrix = null;
    }

    public static void clearCustomizedIcons(String packageName) {
        if (!Shell.sLoaded) {
            return;
        }
        if (DEBUG)
            Log.d(LOG_TAG, "clearCustomizedIcons");
        if (TextUtils.isEmpty(packageName)) {
            Shell.remove(CUSTOMIZED_ICON_PATH);
            Shell.mkdirs(CUSTOMIZED_ICON_PATH, 0777);
            clearCache();
        } else {
            Shell.remove(CUSTOMIZED_ICON_PATH + packageName + "*");
        }
    }

    private static void ensureIconConfigLoaded() {
        if (sIconConfig == null) {
            sIconConfig = loadIconConfig();
            sIconTransformMatrix = makeIconMatrix();
        }
    }

    public static IconConfig getIconConfig() {
        ensureIconConfigLoaded();
        return sIconConfig;
    }

    private static IconConfig loadIconConfig() {
        IconConfig config = new IconConfig();
        InputStream is = null;
        try {
            is = ThemeResources.getSystem().getIconStream(ICON_TRANSFORM_CONFIG, null);
            if (is == null) {
                return config;
            }
            Element doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is)
                    .getDocumentElement();
            if (config != null) {
                NodeList configs = doc.getChildNodes();
                for (int i = 0, N = configs.getLength(); i < N; i++) {
                    Node node = configs.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element ele = (Element) node;
                        String name = ele.getTagName();
                        if ("IconFilters".equals(name)) {
                            config.filters = loadIconFilters(ele.getChildNodes());
                        } else if ("IconShadow".equals(name)) {
                            String radius = ele.getAttribute("radius");
                            String color = ele.getAttribute("color");
                            if (radius != null) {
                                config.shadowRadius = Integer.parseInt(radius)
                                        * sSystemResource.getDisplayMetrics().density;
                            }
                            if (color != null && color.startsWith("#")) {
                                config.shadowColor = Color.parseColor(color);
                            }
                        } else if ("Config".equals(name)) {
                            String configName = ele.getAttribute("name");
                            String configValue = ele.getAttribute("value");
                            if (configName != null && configValue != null) {
                                if ("UseModIcon".equalsIgnoreCase(configName)) {
                                    config.isUseModIcon = Boolean.parseBoolean(configValue);
                                } else if ("ComposeThemeIcon".equalsIgnoreCase(configName)) {
                                    config.isComposeThemeIcon = Boolean.parseBoolean(configValue);
                                } else if ("BaseScale".equalsIgnoreCase(configName)) {
                                    config.baseScale = Float.parseFloat(configValue);
                                }
                            }
                        } else if ("PointsMapping".equals(name)) {
                            ArrayList<Float> pointsMappingFrom = new ArrayList<Float>();
                            ArrayList<Float> pointsMappingTo = new ArrayList<Float>();
                            NodeList points = ele.getChildNodes();
                            for (int j = 0, M = points.getLength(); j < M; j++) {
                                node = points.item(j);
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    ele = (Element) node;
                                    name = ele.getTagName();
                                    if ("Point".equals(name)) {
                                        pointsMappingFrom.add(hdpiSizeToCurrent(Float
                                                .parseFloat(ele.getAttribute("fromX"))));
                                        pointsMappingFrom.add(hdpiSizeToCurrent(Float
                                                .parseFloat(ele.getAttribute("fromY"))));
                                        pointsMappingTo.add(hdpiSizeToCurrent(Float.parseFloat(ele
                                                .getAttribute("toX"))));
                                        pointsMappingTo.add(hdpiSizeToCurrent(Float.parseFloat(ele
                                                .getAttribute("toY"))));
                                    }
                                }
                            }
                            config.pointsMappingTo = new float[pointsMappingTo.size()];
                            for (int j = pointsMappingTo.size() - 1; j >= 0; j--) {
                                config.pointsMappingTo[j] = pointsMappingTo.get(j);
                            }
                            config.pointsMappingFrom = new float[pointsMappingFrom.size()];
                            for (int j = pointsMappingTo.size() - 1; j >= 0; j--) {
                                config.pointsMappingFrom[j] = pointsMappingFrom.get(j);
                            }
                        } else if ("ScaleX".equals(name)) {
                            config.scaleX = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("ScaleY".equals(name)) {
                            config.scaleY = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("ScaleX".equals(name)) {
                            config.scaleX = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("SkewX".equals(name)) {
                            config.skewX = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("SkewY".equals(name)) {
                            config.skewY = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("TransX".equals(name)) {
                            config.transX = hdpiSizeToCurrent(Float.parseFloat(ele
                                    .getAttribute("value")));
                        } else if ("TransY".equals(name)) {
                            config.transY = hdpiSizeToCurrent(Float.parseFloat(ele
                                    .getAttribute("value")));
                        } else if ("RotateX".equals(name)) {
                            config.rotateX = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("RotateY".equals(name)) {
                            config.rotateY = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("RotateZ".equals(name)) {
                            config.rotateZ = Float.parseFloat(ele.getAttribute("value"));
                        } else if ("CameraX".equals(name)) {
                            config.cameraX = hdpiSizeToCurrent(Float.parseFloat(ele
                                    .getAttribute("value")));
                        } else if ("CameraY".equals(name)) {
                            config.cameraY = hdpiSizeToCurrent(Float.parseFloat(ele
                                    .getAttribute("value")));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString(), e);
        } finally {
            IoUtils.closeQuietly(is);
        }
        return config;
    }

    private static List<IBitmapFilter> loadIconFilters(NodeList configs) {
        List<IBitmapFilter> list = new ArrayList<IBitmapFilter>();
        for (int i = 0, N = configs.getLength(); i < N; i++) {
            if (configs.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element ele = (Element) configs.item(i);
                String name = ele.getTagName();
                if ("Filter".equals(name)) {
                    NodeList filterChildren = ele.getChildNodes();
                    List<Entry<String, String>> paramEntrys = new ArrayList<Entry<String, String>>();
                    for (int j = 0, O = filterChildren.getLength(); j < O; j++) {
                        if (filterChildren.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element paramEle = (Element) filterChildren.item(j);
                            if ("Param".equals(paramEle.getNodeName())) {
                                Entry<String, String> param = new SimpleEntry<String, String>(
                                        paramEle.getAttribute("name"),
                                        paramEle.getAttribute("value"));
                                paramEntrys.add(param);
                            }
                        }
                    }
                    String filterName = ele.getAttribute("name");
                    IBitmapFilter filter = IBitmapFilter.Factory.create(filterName, paramEntrys);
                    if (filter != null) {
                        list.add(filter);
                    }
                }
            }
        }
        return list;
    }

    private static float hdpiSizeToCurrent(float pixelSize) {
        return pixelSize * ((float) sDensity / 240);
    }

    private static Bitmap drawableToBitmap(Drawable icon, float ratio) {
        int targetWidth = sCustomizedIconWidth;
        int targetHeight = sCustomizedIconHeight;
        int sourceWidth;
        int sourceHeight;
        if (icon instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap.getDensity() == 0) {
                bitmapDrawable.setTargetDensity(sSystemResource.getDisplayMetrics());
            }
            sourceWidth = bitmap.getWidth();
            sourceHeight = bitmap.getHeight();
        } else {
            sourceWidth = targetWidth;
            sourceHeight = targetHeight;
        }
        icon.setBounds(0, 0, sourceWidth, sourceHeight);
        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight,
                android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = sCanvas;
        canvas.setBitmap(bitmap);
        canvas.save();
        canvas.translate((targetWidth - ratio * sourceWidth) / 2.0F, (targetHeight - ratio
                * sourceHeight) / 2.0F);
        canvas.scale(ratio, ratio);
        icon.draw(canvas);
        canvas.restore();
        return bitmap;
    }

    private static Bitmap composeIcon(Bitmap base, String modMask, String modBackground,
            String modCover) {
        Bitmap mask = getRawIcon(modMask);
        Bitmap background = getRawIcon(modBackground);
        Bitmap cover = getRawIcon(modCover);
        return composeIcon(base, mask, background, cover);
    }

    private static Bitmap composeIconWithTransform(Bitmap base, String modMask,
            String modBackground, String modCover) {
        ensureIconConfigLoaded();
        if (sIconConfig.filters != null && sIconConfig.filters.size() > 0) {
            BitmapInfo imgData = BitmapInfo.getBitmapInfo(base);
            for (IBitmapFilter filter : sIconConfig.filters) {
                filter.process(imgData);
            }
            base = BitmapInfo.getBitmap(imgData);
        }
        if (sIconTransformMatrix != null) {
            base = transformBitmap(base, sIconTransformMatrix);
        }
        //lqwang modify-pr58062-begin
        Bitmap mask = getMaskIcon(modMask);
        Bitmap background = getMaskIcon(modBackground);
        Bitmap cover = getMaskIcon(modCover);
        isMaskNormal = mask != null && background != null && cover != null;
        //lqwang modify-pr58062-end

        return composeIcon(base, mask, background, cover);
    }

    /**
     * when get mask failed,try more times.
     *
     * @param filename
     * @return
     */
    private static Bitmap getMaskIcon(String filename) {
        Bitmap b = null;
        for (int i = 0; i < TRY_AGAIN_COUNT; i++) {
            b = getRawIcon(filename);
            if (b != null) {
                break;
            }
        }
        return b;
    }

    private static Bitmap transformBitmap(Bitmap base, Matrix matrix) {
        Bitmap outBitmap = Bitmap.createBitmap(base.getWidth(), base.getHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        outBitmap.setDensity(base.getDensity());
        sCanvasForTransformBitmap.setBitmap(outBitmap);
        sCanvasForTransformBitmap.drawBitmap(base, matrix, sPaintForBitmap);
        return outBitmap;
    }

    private static Bitmap composeIcon(Bitmap base, Bitmap modMask, Bitmap modBackground,
            Bitmap modCover) {
        int baseWidth = base.getWidth();
        int baseHeight = base.getHeight();
        int basePixels[] = new int[baseWidth * baseHeight];
        Canvas canvas = sCanvas;
        canvas.setBitmap(base);

        if (modCover != null) {
            canvas.drawBitmap(modCover, null, sCustomizedIconRect, sPaintForBitmap);
        }

        if (modMask != null) {
            canvas.drawBitmap(modMask, null, sCustomizedIconRect, sPaintForMask);
        }
        base.getPixels(basePixels, 0, baseWidth, 0, 0, baseWidth, baseHeight);
        if (modBackground != null) {
            Paint p = sPaintForBackground;
            int bgColor = getAverageColor(baseWidth, baseHeight, basePixels);
            if ((bgColor & 0xffffff) != Color.TRANSPARENT) {
                p.setColorFilter(new PorterDuffColorFilter(bgColor, PorterDuff.Mode.SRC_ATOP));
            } else {
                p.setColorFilter(null);
            }
            canvas.drawBitmap(modBackground, null, sCustomizedIconRect, p);
        }
        return base;
    }

    public static void checkSystemUi() {
        final String sysuiIconPath = "/data/system/customized_icons/com_android_systemui.png";
        File f = new File(sysuiIconPath);
        if (f.exists()) {
            lewa.os.SuExecuter.runCommandForResult("rm " + sysuiIconPath, true);
        }
    }

    public static BitmapDrawable generateIconDrawable(Drawable base) {
        return generateIconDrawable(base, false);
    }

    public static Bitmap generateIconBitmap(Drawable base, boolean cropOutside) {
        float scaleRatio = getScaleRatio(base, cropOutside);
        if (DEBUG) {
            Log.d(LOG_TAG, "scaleRatio : " + scaleRatio);
        }
        Bitmap icon = drawableToBitmap(base, scaleRatio);
        return composeIcon(icon, "icon_mask.png", "icon_background.png",
                "icon_cover.png");
    }

    public static BitmapDrawable generateIconDrawable(Drawable base, boolean cropOutside) {
        return getDrawble(generateIconBitmap(base, cropOutside));
    }

    public static BitmapDrawable generateIconDrawable(Drawable base, Bitmap mask,
            Bitmap background, Bitmap cover, boolean cropOutside) {
        Bitmap icon = drawableToBitmap(base, getScaleRatio(base, cropOutside));
        return getDrawble(composeIcon(icon, mask, background, cover));
    }

    public static BitmapDrawable generateShortcutDrawable(Drawable base) {
        Bitmap icon = drawableToBitmap(base, getScaleRatio(base, false));
        return getDrawble(composeIconWithTransform(icon, "icon_mask.png",
                "icon_shortcut_background.png", "icon_shortcut_cover.png"));
    }

    public static BitmapDrawable generateShortcutDrawable(Resources res, int resId) {
        Drawable base = getDrawable(res, resId);
        Bitmap icon = drawableToBitmap(base, getScaleRatio(base, false));
        return getDrawble(composeIconWithTransform(icon, "icon_mask.png",
                "icon_shortcut_background.png", "icon_shortcut_cover.png"));
    }

    public static Drawable getDrawable(Context context, ResolveInfo info) {
        return getDrawable(context, info.activityInfo.packageName, info.icon);
    }

    public static Drawable getDrawable(Context context, String packageName, int resId) {
        try {
            Resources res = context.getPackageManager().getResourcesForApplication(packageName);
            return getDrawable(res, resId);
        } catch (Exception e) {
            Log.e(LOG_TAG, "getDrawable failed. packageName:" + packageName + " resId:" + resId);
            return context.getPackageManager().getDefaultActivityIcon();
        }
    }

    public static Drawable getDrawable(Context context, ApplicationInfo appInfo) {
        try {
            Drawable d = context.getPackageManager()
                    .getDrawable(appInfo.packageName, appInfo.icon, appInfo);
            return d;
        } catch (Exception e) {
            Log.e(LOG_TAG, "getDrawable failed. packageName:" + appInfo.packageName + " resId:" +
                    appInfo.icon);
            return context.getPackageManager().getDefaultActivityIcon();
        }
    }

    private static Drawable getDrawable(Resources res, int resId) {
        return getDrawableForDensity(res, resId, sScaledDensity);
    }

    public static Drawable getDrawableForDensity(Resources res, int resId, int density) {
        try {
            TypedValue value = new TypedValue();
            res.getValueForDensity(resId, density, value, true);
            boolean isColorDrawable = false;
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                isColorDrawable = true;
            }
            if (!isColorDrawable) {
                String file = value.string.toString();
                if (!file.endsWith(".xml")) {
                    InputStream is = res.getAssets().openNonAsset(value.assetCookie, file,
                            AssetManager.ACCESS_STREAMING);
                    Drawable dr = Drawable.createFromResourceStream(res, value, is, file, null);
                    is.close();
                    return dr;
                }
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }
        return res.getDrawable(resId);
    }

    public static BitmapDrawable getCustomizedIcon(Context context, ApplicationInfo info) {
        return getCustomizedIcon(context, info, true);
    }

    public static BitmapDrawable getCustomizedIcon(Context context, ResolveInfo info) {
        String packageName;
        String className;
        if (info.activityInfo != null) {
            packageName = info.activityInfo.packageName;
            className = info.activityInfo.name;
        } else if (info.serviceInfo != null) {
            packageName = info.serviceInfo.packageName;
            className = info.serviceInfo.name;
        } else {
            packageName = null;
            className = null;
        }
        return getCustomizedIcon(context, packageName, className, info.getIconResource(), true);
    }

    private static BitmapDrawable getCustomizedIcon(Context context, String packageName,
            String className, int resId, boolean useAppIcon) {
        List<String> names = getIconNames(packageName, className, useAppIcon);
        if (resId == 0) {
            names.add("lewa.png");
        }

        BitmapDrawable drawable = getCustomizedIconFromCache(names);
        if (drawable == null)
            drawable = getCustomizedIconInner(context, names, getDrawable(context, packageName, resId));
        return drawable;
    }

    private static BitmapDrawable getCustomizedIcon(Context context, ApplicationInfo appInfo,
            boolean useAppIcon) {
        List<String> names = getIconNames(appInfo.packageName, appInfo.className, useAppIcon);
        if (appInfo.icon == 0) {
            names.add("lewa.png");
        }
        BitmapDrawable drawable = getCustomizedIconFromCache(names);
        if (drawable == null)
            drawable = getCustomizedIconInner(context, names, getDrawable(context, appInfo));
        return drawable;
    }

    public static BitmapDrawable getCustomizedIcon(Context context, String packageName,
            String className, Drawable original) {
        List<String> names = getIconNames(packageName, className, true);
        BitmapDrawable drawable = getCustomizedIconFromCache(names);
        if (drawable == null) {
            drawable = getCustomizedIconInner(context, names, original);
        }
        return drawable;
    }

    public static BitmapDrawable getCustomizedIcon(Context context, String filename) {
        List<String> names = new ArrayList<String>();
        names.add(filename);
        BitmapDrawable drawable = getCustomizedIconFromCache(names);
        if (drawable == null) {
            drawable = getCustomizedIconInner(context, names, null);
        }
        return drawable;
    }

    public static BitmapDrawable getCustomizedIconFromCache(List<String> names) {
        //lqwang-PR51690-modify begin
        String filename = getCachedFileName(names);
        //lqwang-PR51690-modify end
        BitmapDrawable drawable = getDrawableFromMemoryCache(filename);
        if (drawable == null) {
            drawable = getDrawableFromStaticCache(filename);
        }
        return drawable;
    }

    //lqwang-PR51690-add begin
    public static String getCachedFileName(List<String> names) {
        String filename = names.get(0);
        if (names.size() > 1) {
            filename = filename.concat("-").concat(names.get(1));
        }
        return filename;
    }

    //lqwang-PR51690-add end
    public static BitmapDrawable getCustomizedIconFromCache(String packageName, String className) {
        return getCustomizedIconFromCache(getIconNames(packageName, className, true));
    }

    private static synchronized BitmapDrawable getCustomizedIconInner(Context context,
            List<String> names, Drawable original) {
        ensureIconConfigLoaded();
        //lqwang-PR51690-modify begin
        String filename = getCachedFileName(names);
        //lqwang-PR51690-modify end
        BitmapDrawable drawable = null;
        for (int i = 0; drawable == null && i < names.size(); i++) {
            drawable = getDrawble(getIconFromTheme(names.get(i), sIconConfig.isComposeThemeIcon));
        }
        if (drawable == null) {
            Bitmap bitmap = null;
            if (sIconConfig.isUseModIcon) {
                for (int j = 0; bitmap == null && j < names.size(); j++) {
                    bitmap = getModIcon(names.get(j));
                }
            }
            if (bitmap == null && original != null) {
                bitmap = drawableToBitmap(original, getScaleRatio(original, false));
            }
            if (bitmap != null) {
                if (DEBUG)
                    Log.d(LOG_TAG, "Generate customized icon for " + filename);
                bitmap = composeIconWithTransform(bitmap, "icon_mask.png", "icon_background.png",
                        "icon_cover.png");
                boolean saveCache = context.getPackageName().equals(Manifest.SERVICE_PACKAGE_NAME);
                if (isMaskNormal && saveCache) {
                    saveCustomizedIconBitmap(filename, bitmap, context);
                }
            }
            drawable = getDrawble(bitmap);
        }
        if (drawable != null) {
            synchronized (sIconCache) {
                sIconCache.put(filename, new WeakReference<Bitmap>(drawable.getBitmap()));
            }
        }
        return drawable;
    }

    private static FileOutputStream getFileOutputStream(String path) {
        try {
            File file = new File(path);
            file.createNewFile();
            Shell.chmod(path, 0644);
            return new FileOutputStream(file);
        } catch (IOException e) {
            return null;
        }
    }

    private static void saveCustomizedIconBitmap(String fileName, Bitmap icon, Context context) {
        FileOutputStream os = null;
        try {
            String path = (IS_SYSTEM ? "/cache/" : context.getCacheDir().getAbsolutePath() + '/')
                    + fileName;
            os = getFileOutputStream(path);
            if (os != null) {
                icon.compress(CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                String target = CUSTOMIZED_ICON_PATH + fileName;
                Shell.copy(path, target);
                Shell.chmod(target, 0644);
                Shell.remove(path);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            IoUtils.closeQuietly(os);
        }
    }

    public static void saveCustomizedIconBitmap(String packageName, String className, Drawable dr,
            Context context) {
        if (dr instanceof BitmapDrawable) {
            saveCustomizedIconBitmap(getFileName(packageName, className),
                    ((BitmapDrawable) dr).getBitmap(), context);
        }
    }

    private static BitmapDrawable getDrawble(Bitmap bitmap) {
        return bitmap == null ? null : new BitmapDrawable(sSystemResource, bitmap);
    }

    private static float getScaleRatio(Drawable icon, boolean cropOutside) {
        return getScaleRatioRaw(icon, cropOutside) * sIconConfig.baseScale;
    }

    private static float getScaleRatioRaw(Drawable icon, boolean cropOutside) {
        ensureIconConfigLoaded();
        if (!(icon instanceof PaintDrawable)) {
            int sourceWidth;
            int sourceHeight;
            if (icon instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                sourceWidth = bitmap.getWidth();
                sourceHeight = bitmap.getHeight();
            } else {
                sourceWidth = icon.getIntrinsicWidth();
                sourceHeight = icon.getIntrinsicHeight();
            }
            if (sourceWidth > 0 && sourceWidth > 0) {
                float ratioW = (float) sCustomizedIconWidth / (float) sourceWidth;
                float ratioH = (float) sCustomizedIconHeight / (float) sourceHeight;
                if (cropOutside) {
                    return Math.max(ratioW, ratioH);
                }
                float contentRatio = getContentRatio(icon);
                if (contentRatio > 0 && contentRatio <= 2) {
                    return .9f * contentRatio;
                } else {
                    return Math.min(1, Math.min(ratioW, ratioH));
                }
            }
        }
        return 1;
    }

    public static Bitmap getRawIcon(String filename) {
        Bitmap bitmap = null;
        if (!TextUtils.isEmpty(filename)) {
            bitmap = getIconFromMemoryCache(filename);
            if (bitmap == null) {
                bitmap = getIconFromTheme(filename, false);
                if (bitmap == null) {
                    bitmap = getModIcon(filename);
                }
            }
        }
        if (bitmap != null) {
            synchronized (sRawIconCache) {
                sRawIconCache.put(filename, new SoftReference<Bitmap>(bitmap));
            }
        }
        return bitmap;
    }

    private static Bitmap getModIcon(String fileName) {
        File iconFile = new File(MOD_EXTRA_ICONS_DIR + fileName);
        if (iconFile.exists()) {
            try {
                return scaleBitmap(BitmapFactory.decodeFile(iconFile.getAbsolutePath()));
            } catch (OutOfMemoryError e) {

            } catch (Exception e) {

            }
        }
        if (sModIcons == null) {
            iconFile = new File(MOD_BUILT_IN_ICONS);
            if (iconFile.exists()) {
                try {
                    sModIcons = new ZipFile(MOD_BUILT_IN_ICONS);
                } catch (IOException e) {
                }
            }
        }
        if (sModIcons != null) {
            InputStream is = null;
            try {
                ZipEntry entry = sModIcons.getEntry(fileName);
                if (entry != null) {
                    is = sModIcons.getInputStream(entry);
                    return scaleBitmap(BitmapFactory.decodeStream(is));
                }
            } catch (OutOfMemoryError e) {
            } catch (Exception e) {
            } finally {
                IoUtils.closeQuietly(is);
            }
        }
        if (sContext != null) {
            InputStream is = null;
            try {
                is = sContext.getAssets().open(MOD_ICONS_NAME + '/' + fileName);
                if (is != null) {
                    Bitmap bmp = scaleBitmap(BitmapFactory.decodeStream(is));
                    is.close();
                    return bmp;
                }
            } catch (Exception e) {
            } finally {
                IoUtils.closeQuietly(is);
            }
        }
        return null;
    }

    public static void setContext(Context context) {
        sContext = context;
        checkModIcons();
    }

    private static BitmapDrawable getDrawableFromStaticCache(String filename) {
        String pathName = CUSTOMIZED_ICON_PATH + filename;
        File iconFile = new File(pathName);
        if (iconFile.exists()) {
            try {
                return getDrawble(BitmapFactory.decodeFile(pathName));
            } catch (OutOfMemoryError e) {

            } catch (Exception e) {
                iconFile.delete();
            }
        }
        return null;
    }

    private static BitmapDrawable getDrawableFromMemoryCache(String name) {
        synchronized (sIconCache) {
            WeakReference<Bitmap> ref = sIconCache.get(name);
            if (ref != null) {
                return getDrawble(ref.get());
            }
        }
        return null;
    }

    private static Bitmap getIconFromMemoryCache(String name) {
        synchronized (sRawIconCache) {
            SoftReference<Bitmap> ref = sRawIconCache.get(name);
            if (ref != null) {
                return ref.get();
            }
        }
        return null;
    }

    private static Bitmap getIconFromTheme(String filename, boolean compose) {
        Bitmap icon = ThemeResources.getSystem().getIcon(sSystemResource,
                ThemeResources.getDensityName() + '/' + filename);
        if (icon != null) {
            icon.setDensity(sDensity);
        } else {
            icon = ThemeResources.getSystem().getIcon(sSystemResource, filename);
            if (icon != null) {
                icon.setDensity(DisplayMetrics.DENSITY_XHIGH);
            }
        }
        if (icon != null) {
            icon = scaleBitmap(icon);
            return compose ? composeIconWithTransform(icon, "icon_mask_theme.png",
                    "icon_background_theme.png", "icon_cover_theme.png") : icon;
        }
        return null;
    }

    private static Bitmap scaleBitmap(Bitmap icon) {
        if (icon != null
                && (icon.getWidth() != sCustomizedIconWidth ||
                icon.getHeight() != sCustomizedIconHeight)) {
            Bitmap bitmap = Bitmap.createScaledBitmap(icon, sCustomizedIconWidth,
                    sCustomizedIconHeight, true);
            bitmap.setDensity(sDensity);
            return bitmap;
        }
        return icon;
    }

    private static float getContentRatio(Drawable icon) {
        if (icon instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) icon).getBitmap();
            int side = ImageUtils.findMaxSide(bmp);
            if (side > 0) {
                return (float) sCustomizedIconWidth / side;
            }
        }
        return -1;
    }

    private static int getAverageColor(int height, int width, int[] pixels) {

        // Alpha range is 0...255
        final int minAlpha = ALPHA_MIN;

        final int stride = 10;

        // Saturation range is 0...1
        float minSaturation = 0.2f;

        // Number of pixels to sample
        int hSamples = width / stride;
        int vSamples = height / stride;

        // Holds temporary sum of HSV values
        float[] sampleTotals = {
                0, 0, 0
        };

        // Loop through pixels horizontally
        float[] hsv = new float[3];
        int sample;
        int sampleSize = 0;
        for (int j = vSamples, sV = height / vSamples; j < height; j += sV) {
            // Loop through pixels horizontal
            int s = j * width;
            for (int i = hSamples, sH = width / hSamples; i < width; i += sH) {
                // Get pixel & convert to HSV format
                sample = pixels[s + i];
                // Check pixel matches criteria to be included in sample
                if ((Color.alpha(sample) > minAlpha)) {
                    Color.colorToHSV(sample, hsv);
                    if (hsv[1] >= minSaturation) {
                        // Add sample values to total
                        sampleTotals[0] += hsv[0]; // H
                        sampleTotals[1] += hsv[1]; // S
                        sampleTotals[2] += hsv[2]; // V
                        sampleSize++;
                    }
                }
            }
        }
        if (sampleSize == 0)
            return Color.TRANSPARENT;

        sampleTotals[0] /= sampleSize;
        sampleTotals[1] /= sampleSize;
        sampleTotals[2] /= sampleSize;

        // Return average tuplet as RGB color
        return Color.HSVToColor(ALPHA_ATOP, sampleTotals);
    }

    public static String getFancyIconRelativePath(String packageName, String className) {
        List<String> names = getIconNames(packageName, className, true);
        ThemeResources res = ThemeResources.getSystem();
        for (String name : names) {
            if (name.endsWith(".png")) {
                name = name.substring(0, name.length() - ".png".length());
            }
            if (res.hasFancyIcon(name)) {
                String relativePath = FANCY_ICONS_INNER_PATH + name + '/';
                if (res.hasIcon(relativePath + "manifest.xml")) {
                    return relativePath;
                }
            }
        }
        return null;
    }

    public static String getFancyIconRelativePath(ResolveInfo info) {
        String packageName;
        String className;
        if (info.activityInfo != null) {
            packageName = info.activityInfo.packageName;
            className = info.activityInfo.name;
        } else if (info.serviceInfo != null) {
            packageName = info.serviceInfo.packageName;
            className = info.serviceInfo.name;
        } else {
            return null;
        }
        return getFancyIconRelativePath(packageName, className);
    }

    private static String getV4IconName(String name) {
        return name.toLowerCase(Locale.US).replace('.', '_') + ".png";
    }

    public static List<String> getIconNames(String packageName, String className,
            boolean mapPackagename) {
        ArrayList<String> paths = new ArrayList<String>();
        if (packageName == null) {
            return paths;
        }
        if (ThemeResources.isIconV4()) {
            if (className != null) {
                paths.add(className = getV4IconName(className));
                String mappingName = sIconMapping.get(className);
                if (mappingName != null) {
                    paths.add(mappingName);
                }
            }
            if (mapPackagename) {
                paths.add(packageName = getV4IconName(packageName));
                String mappingName = sIconMapping.get(packageName);
                if (mappingName != null) {
                    paths.add(mappingName);
                }
            }
        } else {
            String fileName = getFileName(packageName, className);
            String mappingName = sIconMapping.get(fileName);
            if (mappingName != null) {
                paths.add(mappingName);
            }
            paths.add(fileName);
            if (className != null) {
                if (!className.startsWith(packageName)) {
                    paths.add(className + ".png");
                }
            }
            if (mapPackagename) {
                paths.add(packageName += ".png");
                mappingName = sIconMapping.get(packageName);
                if (mappingName != null) {
                    paths.add(mappingName);
                }
            }
        }
        return paths;
    }

    private static String getFileName(String packageName, String className) {
        if (className == null) {
            return packageName + ".png";
        }

        if (className.startsWith(packageName)) {
            return className + ".png";
        } else {
            return packageName + '#' + className + ".png";
        }
    }

    public static void prepareCustomizedIcons(Context context) {
        prepareCustomizedIcons(context, null);
    }

    public static void prepareCustomizedIcons(Context context, CustomizedIconsListener l) {
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = context.getPackageManager();
        boolean processIcon = IS_SYSTEM;
        List<ResolveInfo> list = pm.queryIntentActivities(launcherIntent, 0);
        if (l != null) {
            l.beforePrepareIcon(list.size());
        }
        for (int i = 0, N = list.size(); i < N; i++) {
            ActivityInfo info = list.get(i).activityInfo;
            Drawable icon = info.loadIcon(pm);
            if (processIcon) {
                String packageName = info.packageName;
                String className = info.name;
                if (getCustomizedIconFromCache(packageName, className) == null) {
                    getCustomizedIcon(context, packageName, className, icon);
                }
            }
            if (l != null) {
                l.finishPrepareIcon(i);
            }
        }
        if (l != null) {
            l.finishAllIcons();
        }
    }

    public static interface CustomizedIconsListener {

        public abstract void beforePrepareIcon(int i);

        public abstract void finishAllIcons();

        public abstract void finishPrepareIcon(int i);
    }

    private static Matrix makeIconMatrix() {
        Matrix matrix = new Matrix();
        if (sIconConfig.pointsMappingFrom != null) {
            matrix.setPolyToPoly(sIconConfig.pointsMappingFrom, 0, sIconConfig.pointsMappingTo, 0,
                    sIconConfig.pointsMappingFrom.length / 2);
        } else {
            Camera camera = new Camera();
            camera.rotateX(sIconConfig.rotateX);
            camera.rotateY(sIconConfig.rotateY);
            camera.rotateZ(sIconConfig.rotateZ);
            camera.getMatrix(matrix);
            matrix.preTranslate((-sCustomizedIconWidth) / 2.0F - sIconConfig.cameraX,
                    (-sCustomizedIconHeight) / 2.0F - sIconConfig.cameraY);
            matrix.postTranslate(sCustomizedIconWidth / 2.0F + sIconConfig.cameraX,
                    sCustomizedIconHeight / 2.0F + sIconConfig.cameraY);
            matrix.postScale(sIconConfig.scaleX, sIconConfig.scaleY);
            matrix.postSkew(sIconConfig.skewX, sIconConfig.skewY);
        }
        return matrix;
    }

    public static class IconConfig {
        List<IBitmapFilter> filters;

        public float cameraX;

        public float cameraY;

        public float pointsMappingFrom[];

        public float pointsMappingTo[];

        public float rotateX;

        public float rotateY;

        public float rotateZ;

        public float scaleX = 1;

        public float scaleY = 1;

        public float skewX;

        public float skewY;

        public float transX;

        public float transY;

        public float shadowRadius = 5 * Resources.getSystem().getDisplayMetrics().density;

        public int shadowColor = 0x60000000;

        public float baseScale = 1;

        public boolean isUseModIcon = true;

        public boolean isComposeThemeIcon = false;

    }
}
