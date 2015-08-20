package com.lewa.themes;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.*;
import android.content.pm.*;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.resource.BlurWallpaperUtils;
import com.lewa.themes.resource.IconCustomizer;
import com.lewa.themes.service.IThemeService;
import com.lewa.themes.service.IThemeServiceCallback;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import lewa.laml.util.AppIconsHelper;

/**
 * @author Fan.Yang
 *         ThemeManager的主要功能：
 *         1.为平台应用提供主题引擎接口，通过loadIcon(...)方法获得当前的主题的主题图标
 *         2.为平台应用提供图标处理接口，通过getCustomizedIcon(Drawable)方法获取默认主题的主题图标。
 *         3.为平台应用提供模糊壁纸接口，通过getBlurredWallpaper()方法活取当前壁纸的模糊效果。
 *         4.为平台应用提供当前主题的状态，如当前选择的壁纸、锁屏引擎的信息。
 *         5.响应ThemeService（AIDL callback和Broadcast）的处理，完成安全重绘主题图标和壁纸等效果。
 */

public class ThemeManager extends BroadcastReceiver {
    public static final boolean DEBUG = true;
    private static final String TAG = "ThemeManager";

    // Begin, added by yljiang@lewatek.com 2013-12-03
    public static final String OTHER = "com.";
    public static final String FRAMEWORK = "framework-res";
    public static final String LEWA = "lewa-res";
    public static final String SPLIT = "/";
    // End

    // yixiao add theme filter for independent Launcher
    public static final boolean INDEPENDENT = false;
    public static final boolean STANDALONE = Manifest.STANDALONE;
    public static final String RESOURCE_AUTHORITY = Manifest.PACKAGE_NAME + ".packageresources";
    public static final String THEME_AUTHORITY = Manifest.PACKAGE_NAME + ".themes";

    public final static String DATA_DIR = Environment.getDataDirectory().getAbsolutePath();
    public final static String ROOT_DIR = Environment.getRootDirectory().getAbsolutePath();
    public final static String THEME_ELEMENTS_PACKAGE = Utils.getPackageName();
    public final static String THEME_ELEMENTS_DATA_PATH =
            DATA_DIR + "/data/" + THEME_ELEMENTS_PACKAGE + "/files";
    public final static String THEME_ELEMENTS_PATH = DATA_DIR + "/system/face";

    public final static String THEME_ELEMENT_LOCKSCREEN = THEME_ELEMENTS_PATH + "/lockstyle";
    public final static String THEME_ELEMENT_LOCKSCREEN_DIR = THEME_ELEMENTS_PATH + "/lockstyle";
    public final static String THEME_ELEMENT_LOCKSCREEN_CONFIG = THEME_ELEMENTS_PATH + "/main.xml";
    public final static String THEME_ELEMENT_LOCKSCREEN_OVERRIDE_DIR = THEME_ELEMENTS_PATH;
    public final static String THEME_ELEMENT_LOCKSCREEN_WALLPAPER =
            THEME_ELEMENT_LOCKSCREEN_OVERRIDE_DIR + "/lockwallpaper";
    public final static Uri THEME_ELEMENT_LOCKSCREEN_WALLPAPER_URI = Uri
            .parse("file://" + THEME_ELEMENT_LOCKSCREEN_WALLPAPER);
    public final static String THEME_ELEMENT_BOOT_ANIMATION =
            THEME_ELEMENTS_PATH + "/bootanimation.zip";
    public final static String THEME_ELEMENT_FONTS = THEME_ELEMENTS_PATH + "/fonts";
    public final static String THEME_ELEMENT_ICONS = THEME_ELEMENTS_PATH + "/icons";
    public final static String THEME_ELEMENT_INCALLSYLE = THEME_ELEMENTS_PATH + "/incallstyle";
    public final static String THEME_LOCKSCREEN_FILE_OVERRIDE =
            THEME_ELEMENTS_PATH + "/.file_lockscreen";

    /**
     * Commonly passed between activities.
     *
     * @see com.lewa.thememanager.provider.ThemeItem
     */
    public static final String EXTRA_THEME_ITEM = "theme_item";

    /**
     * Permission required to send a broadcast to the ThemeManager requesting
     * theme change. This permission is not required to fire a chooser for
     * {@link #ACTION_SET_THEME}, which presents the ThemeManager's normal UI.
     */
    public static final String PERMISSION_CHANGE_THEME = "com.lewa.permission.CHANGE_THEME";

    /**
     * Broadcast intent to use to change theme without going through the normal
     * ThemeManager UI.  Requires {@link #PERMISSION_SET_THEME}.
     */
    public static final String ACTION_CHANGE_THEME = "com.lewa.intent.action.CHANGE_THEME";
    public static final String ACTION_KILL_PROCESS_FINISH = "com.lewa.aciton.KILL_PROCESS_SUCCESS";

    /**
     * Broadcast intent fired on theme change.
     */
    public static final String ACTION_THEME_CHANGED = "com.lewa.intent.action.THEME_CHANGED";

    /**
     * Broadcast intent fired on theme change.
     */
    public static final String ACTION_THEME_DOWNLOADED = "com.lewa.intent.action.THEME_DOWNLOADED";

    /**
     * Similar to {@link Intent#ACTION_SET_WALLPAPER}.
     */
    public static final String ACTION_SET_THEME = "com.lewa.intent.action.SET_THEME";

    /**
     * For Lewa Colorful View, wallpaper has changed
     */
    public static final String ACTION_COLORFUL_WALLPAPER_CHANGED = "com.lewa.intent.action.COLORFUL_WALLPAPER_CHANGED";

    public static final String SET_NETWORK = "android.settings.SETTINGS";
    /**
     * URI for the item which should be checked in both the theme and style
     * choosers. If null, will use the current global theme.
     */
    public static final String EXTRA_THEME_EXISTING_URI = "com.lewa.intent.extra.theme.EXISTING_URI";

    /**
     * URI for the profile in which the current theme is associated. If null, we will apply against the
     * currently applied profile.
     */
    public static final String EXTRA_THEME_PROFILE_URI = "com.lewa.intent.extra.theme.THEME_PROFILE_URI";

    /**
     * Boolean indicating whether the "extended" theme change API should be
     * supported. This API is a convenience for profile change and is not used
     * during normal theme or style changes.
     * <p/>
     * Without this set, {@link #EXTRA_WALLPAPER_URI},
     * {@link #EXTRA_DONT_SET_LOCK_WALLPAPER}, {@link #EXTRA_LOCK_WALLPAPER_URI},
     * {@link #EXTRA_RINGTONE_URI}, and {@link #EXTRA_NOTIFICATION_RINGTONE_URI}
     * will not be observed.
     */
    public static final String EXTRA_EXTENDED_THEME_CHANGE = "com.lewa.intent.extra.theme.EXTENDED_THEME_CHANGE";

    public static final String EXTRA_INCALLSTYLE_URI = "com.lewa.intent.extra.theme.INCALLSTYLE_URI";

    public static final String EXTRA_WALLPAPER_URI = "com.lewa.intent.extra.theme.WALLPAPER_URI";
    public static final String EXTRA_DONT_SET_LOCK_WALLPAPER = "com.lewa.intent.extra.theme.DONT_SET_LOCK_WALLPAPER";
    public static final String EXTRA_LOCK_WALLPAPER_URI = "com.lewa.intent.extra.theme.LOCK_WALLPAPER_URI";
    public static final String EXTRA_LIVE_WALLPAPER_COMPONENT = "com.lewa.intent.extra.theme.LIVE_WALLPAPER_COMPONENT";
    public static final String EXTRA_RINGTONE_URI = "com.lewa.intent.extra.theme.RINGTONE_URI";
    public static final String EXTRA_NOTIFICATION_RINGTONE_URI = "com.lewa.intent.extra.theme.NOTIFICATION_RINGTONE_URI";

    public static final String EXTRA_LOCKSCREEN_URI = "com.lewa.intent.extra.theme.LOCKSCREEN_URI";
    public static final String EXTRA_BOOT_ANIMATION_URI = "com.lewa.intent.extra.theme.BOOT_ANIMATION_URI";
    public static final String EXTRA_FONT_URI = "com.lewa.intent.extra.theme.FONT_URI";
    public static final String EXTRA_ICONS_URI = "com.lewa.intent.extra.theme.ICONS_URI";
    public static final String EXTRA_SYSTEM_APP = "system_app";
    public static final String DEFAULT_LOCKSCREEN_STYLE = "default_lockscreen_style";
    public static final String DEFAULT_FONT = "default_font";
    public static final String DEFAULT_LOCKSCREEN_WALLPAPER = "default_lockscreen_wallpaper";
    public static final String DEFAULT_ICON = "default_icon";

    /**
     * The Android ringtone manager returns a null Uri for silent.
     * Use this Uri in the local provider to better track silent.
     */
    public static final Uri SILENT_RINGTONE_URI = Uri
            .parse("content://com.lewa.thememanager/ringtone/silent");

    public static final String START_FILEMANAGER_ACTION = "com.lewa.filemgr.path_start";

    public static final String THEME_LOCKSCREEN_PACKAGE = "com.lewa.lockscreen";

    public static final String THEME_LOCKSCREEN2_PACKAGE = "com.lewa.lockscreen2";

    public static final String SETTINGS_LOCKSCREEN = "lewa.theme.lockscreen";

    public static final String INTER_LOCKSCREEN2_PATH = "lockscreen2/lockscreen";

    public final static String THEME_DEFAULT_PATH = "/system/media/theme/";

    public final static String THEME_LOCKSCREEN_DEFAULT = THEME_DEFAULT_PATH + "lockstyle";

    public final static String THEME_WALLPAPER_DEFAULT = THEME_DEFAULT_PATH + "defaultwallpaper";

    public final static String THEME_ICON_DEFAULT = THEME_DEFAULT_PATH + "icons";

    public final static String THEME_LOCKWALLPAPER_DEFAULT = THEME_DEFAULT_PATH + "lockwallpaper";

    public final static String THEME_LOCKSCREEN_CONFIG = THEME_ELEMENTS_PATH + "/lockstyle";

    public final static String THEME_WALLPAPER_CONFIG = THEME_ELEMENTS_PATH + "/wallpaper";

    public final static String THEME_ICON_CONFIG = THEME_ELEMENTS_PATH + "/icons";

    public final static String THEME_LOCKWALLPAPER_CONFIG = THEME_ELEMENTS_PATH + "/lockwallpaper";

    public static final String THEME_LOCKSTYLE_SUBFOLDER = "advance/";

    public static final String THEME_ICON_RES_SUBFOLDER = "res/";

    public static final String THEME_EXTRA_PATH = THEME_ELEMENTS_PATH + "lockscreen.config";

    public static final String CUSTOMIZED_ICON_PATH = "/data/system/customized_icons/";

    public static final String CUSTOMIZED_WALLPAPER_PATH = "/data/system/customized_wallpaper/";

    public static final String BLUR_WALLPAPER_PATH = CUSTOMIZED_WALLPAPER_PATH + "blur_wallpaper";

    public static final String THEME_SERVICE_ACTION = "com.lewa.themes.ThemeService";

    public static final String ACTION_SERVICE_CREATED = "com.lewa.intent.action.SERVICE_CREATE";

    public static final String EXTRA_ICON_CHANGED = "com.lewa.intent.extra.theme.ICON_CHANGED";

    private static final int SERVICE_CONNECTION_MSG = 0;

    private static final int THEME_CHANGED_MSG = 1;

    private static final int WALLPAPER_CHANGED_MSG = 2;

    private static final HashMap<String, WeakReference<Bitmap>> sIconCache = new HashMap<String, WeakReference<Bitmap>>();

    private static final Hashtable<String, WeakReference<BitmapDrawable>> sIconDrawableCache
            = new Hashtable<String, WeakReference<BitmapDrawable>>();

    private static final Hashtable<String, ResolveInfo> resolveInfoMap =
            new Hashtable<String, ResolveInfo>();

    private static final Hashtable<String, ApplicationInfo> applicationInfoMap =
            new Hashtable<String, ApplicationInfo>();

    private Thread mInvalidateThread;

    private Handler mInvalidateHandler;

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case THEME_CHANGED_MSG:
                    sIconCache.clear();
                    break;
                case SERVICE_CONNECTION_MSG:
                    mConnection.connect();
                    break;
                case WALLPAPER_CHANGED_MSG:
                    Bitmap bm = (Bitmap) msg.obj;
                    if (bm == null) {
                        loadBlurWallpaperFromRemote();
                    }
                    drawBlurWallpaper(bm);
                    break;
                default:
                    break;
            }
        }
    };

    private static boolean FANCY_ICON = true;

    private static final Object mLock = new Object();

    private static ThemeManager mThemeManager;

    private final ThemeConnection mConnection;

    private Context mContext;

    private boolean careWallpaperChange = true;

    private boolean careThemeChange = true;

    private Drawable mBlurWallpaper;

    private ThemeReceiver mThemeReceiver;

    public static ThemeManager getInstance(Context context) {
        return getInstance(context, null);
    }

    public static ThemeManager getInstance(Context context, OnInitListener listener) {
        if (context == null) {
            return mThemeManager;
        }
        synchronized (mLock) {
            if (mThemeManager == null) {
                if (context instanceof Activity || context instanceof Service) {
                    throw new RuntimeException(
                            "Can not init theme connection with activity or service");
                } else {
                    mThemeManager = new ThemeManager(context.getApplicationContext(), listener);
                }
            }
        }
        return mThemeManager;
    }

    private ThemeManager(Context context, OnInitListener initListener) {
        mContext = context;
        mConnection = new ThemeConnection(context, initListener);
        mConnection.connect();

        registerServiceCreatedReceiver();
    }

    public void unBindService() {
        mContext.unbindService(mConnection);

        notifiedOnThemeChanged(false);
    }

    // 设置一个flag，是否关注壁纸的变化
    public void notifiedOnWallpaperChanged(boolean isCare) {
        careWallpaperChange = isCare;
    }

    public void notifiedOnThemeChanged(boolean isCareThemeChange) {
        careThemeChange = isCareThemeChange;
        if (isCareThemeChange) {
            mInvalidateThread = new InvalidateDrawableThread();
            mInvalidateThread.start();

            if (mThemeReceiver == null) {
                mThemeReceiver = new ThemeReceiver();
            }
            registerThemeChangedReceiver();
        } else {
            if (mThemeReceiver != null) {
                mContext.unregisterReceiver(mThemeReceiver);
                mThemeReceiver = null;
            }
        }
    }

    private void registerThemeChangedReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_THEME_CHANGED);
        try {
            filter.addDataType(Themes.ThemeColumns.CONTENT_ITEM_TYPE);
            filter.addDataType(Themes.ThemeColumns.STYLE_CONTENT_ITEM_TYPE);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        mContext.registerReceiver(mThemeReceiver, filter);
    }

    private void registerServiceCreatedReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_SERVICE_CREATED);
        mContext.registerReceiver(this, filter);
    }

    public void reset() {
        if (mConnection.isConnected()) {
            try {
                IThemeService service = mConnection.getService();
                service.clearCustomizedIcons(null);
                service.reset();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Intent intent = new Intent(ThemeManager.THEME_SERVICE_ACTION);
            intent.putExtra(EXTRA_ICON_CHANGED, true);
            mContext.startService(intent);
        }
    }

    public Drawable loadIcon(ApplicationInfo ai) {
        if (ai == null) {
            return null;
        }
        try {
            if (!mConnection.isConnected()) {
                mConnection.connect();
                return loadIconAtLocal(ai);
            }
            IThemeService service = mConnection.getService();
            String packageName = ai.packageName;
            String className = ai.className;
            if (FANCY_ICON) {
                String path = service.getFancyIconRelativePath(packageName, className);
                if (path != null) {
                    Drawable d = getFancyIcon(path);
                    if (d != null)
                        return d;
                }
            }
            String key = packageName + className;

            BitmapDrawable d = getDrawableFromMemoryCache(key);
            if (d == null) {
                d = getCustomizedIconFromStaticCache(packageName, className, true);
                if (d == null && ai.icon == 0) {
                    d = getDrawableFromStaticCache("lewa.png");
                }
                if (d == null) {
                    Bitmap bmp = service.loadIconByApplicationInfo(ai);
                    if (null != bmp) {
                        d = getDrawable(key, bmp);
                    }
                }
                if (d != null) {
                    synchronized (sIconCache) {
                        sIconCache.put(key, new WeakReference<Bitmap>(d.getBitmap()));
                    }
                    synchronized (sIconDrawableCache) {
                        ApplicationInfo appInfo = applicationInfoMap.get(key);
                        if (appInfo == null) {
                            appInfo = new ApplicationInfo(ai);
                            applicationInfoMap.put(key, appInfo);
                        }
                        sIconDrawableCache.put(key, new WeakReference<BitmapDrawable>(d));
                    }
                }
            }
            return d;
        } catch (Exception e) {
            Log.e(TAG, "Dead object in loadIcon", e);
        }
        return null;
    }

    public Drawable loadIcon(ResolveInfo ri) {
        try {
            String packageName;
            String className;
            if (ri.activityInfo != null) {
                packageName = ri.activityInfo.packageName;
                className = ri.activityInfo.name;
            } else if (ri.serviceInfo != null) {
                packageName = ri.serviceInfo.packageName;
                className = ri.serviceInfo.name;
            } else {
                return null;
            }
            if (!mConnection.isConnected()) {
                mConnection.connect();
                return loadIconAtLocal(ri);
            }
            IThemeService service = mConnection.getService();
            if (FANCY_ICON) {
                String path = service.getFancyIconRelativePath(packageName, className);
                if (path != null) {
                    Drawable d = getFancyIcon(path);
                    if (d != null)
                        return d;
                }
            }

            String key = packageName + className;
            BitmapDrawable d = getDrawableFromMemoryCache(key);
            if (d == null) {
                d = getCustomizedIconFromStaticCache(packageName, className, false);
                if (d == null && ri.getIconResource() == 0) {
                    d = getDrawableFromStaticCache("lewa.png");
                }
                if (d == null) {
                    Bitmap bmp = service.loadIconByResolveInfo(ri);
                    if (null != bmp) {
                        d = getDrawable(key, bmp);
                    }
                }
                if (d != null) {
                    synchronized (sIconCache) {
                        sIconCache.put(key, new WeakReference<Bitmap>(d.getBitmap()));
                    }
                    synchronized (sIconDrawableCache) {
                        ResolveInfo resolveInfo = resolveInfoMap.get(key);
                        if (resolveInfo == null) {
                            resolveInfo = new ResolveInfo(ri);
                            resolveInfoMap.put(key, resolveInfo);
                        }
                        sIconDrawableCache.put(key, new WeakReference<BitmapDrawable>(d));
                    }
                }
            }
            return d;
        } catch (Exception e) {
            Log.e(TAG, "Dead object in loadIcon", e);
        }
        return null;
    }

    public Drawable loadIcon(PackageItemInfo pi) {
        Class<?> classType = pi.getClass();
        Method method = null;
        ApplicationInfo appInfo = null;
        try {
            method = classType.getDeclaredMethod("getApplicationInfo");
            method.setAccessible(true);
            appInfo = (ApplicationInfo) method.invoke(pi);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (appInfo != null) {
            return loadIcon(appInfo);
        }
        return getDrawableFromStaticCache("lewa.png");
    }

    public static Drawable getCustomizedIcon(Drawable drawable) {
        return IconCustomizer.generateIconDrawable(drawable);
    }

    private BitmapDrawable loadIconAtLocal(ApplicationInfo ai) {
        if (DEBUG) {
            Log.d(TAG, "loadIconAtLocal");
        }
        return IconCustomizer.getCustomizedIcon(mContext, ai);
    }

    private BitmapDrawable loadIconAtLocal(ResolveInfo ri) {
        if (DEBUG) {
            Log.d(TAG, "loadIconAtLocal");
        }
        return IconCustomizer.getCustomizedIcon(mContext, ri);
    }

    private BitmapDrawable getDrawableFromMemoryCache(String name) {
        synchronized (sIconDrawableCache) {
            WeakReference<BitmapDrawable> ref = sIconDrawableCache.get(name);
            if (ref != null) {
                return ref.get();
            }
        }
        synchronized (sIconCache) {
            WeakReference<Bitmap> ref = sIconCache.get(name);
            if (ref != null) {
                if (DEBUG)
                    Log.d(TAG, "get drawable from memory cache, name:" + name);
                return getDrawable(name, ref.get());
            }
        }
        return null;
    }

    private BitmapDrawable getDrawable(String key, Bitmap bitmap) {
        BitmapDrawable drawable = null;
        if (bitmap != null) {
            drawable = new BitmapDrawable(mContext.getResources(), bitmap);
        }
        return drawable;
    }

    public BitmapDrawable getCustomizedIconFromStaticCache(String packageName,
            String className, boolean usePackageName) {
        BitmapDrawable d = getDrawableFromStaticCache(getFileName(packageName, className));
        if (d == null && usePackageName)
            d = getDrawableFromStaticCache(packageName + ".png");
        if (d == null && className != null)
            d = getDrawableFromStaticCache(className.replace('.', '_') + ".png");
        if (d == null)
            d = getDrawableFromStaticCache(packageName.replace('.', '_') + ".png");
        return d;
    }

    private BitmapDrawable getDrawableFromStaticCache(String filename) {
        String pathName = CUSTOMIZED_ICON_PATH + filename;
        File iconFile = new File(pathName);
        if (iconFile.exists()) {
            try {
                if (DEBUG)
                    Log.d(TAG, "get drawable from static cache,file name:" + filename);
                return getDrawable(filename, BitmapFactory.decodeFile(pathName));
            } catch (OutOfMemoryError e) {
            } catch (Exception e) {
                iconFile.delete();
            }
        }
        return null;
    }

    private String getFileName(String packageName, String className) {
        List<String> names = IconCustomizer.getIconNames(packageName, className, true);
        return IconCustomizer.getCachedFileName(names);
    }

    private Drawable getFancyIcon(String name) {
        try {
            return (Drawable) AppIconsHelper.getIconDrawable(mContext, name);
        } catch (Exception e) {
            Log.e(TAG, "getFancyIcon", e);
        }
        return null;
    }

    private void invalidateCachedDrawable() {
        if (!mConnection.isConnected()) {
            mConnection.connect();
            return;
        }
        List<ResolveInfo> resolveInfos = queryResolveInfo(mContext);
        List<ApplicationInfo> applicationInfos = queryApplicationInfo(mContext);
        IThemeService service = mConnection.getService();
        Set<Map.Entry<String, WeakReference<BitmapDrawable>>> sets = sIconDrawableCache.entrySet();
        try {
            for (Map.Entry<String, WeakReference<BitmapDrawable>> entry : sets) {
                if (entry != null) {
                    final String key = entry.getKey();
                    final BitmapDrawable drawable = (entry.getValue()).get();
                    ApplicationInfo aInfo = getApplicationInfo(applicationInfos, key);
                    if (aInfo != null) {
                        final Bitmap bmp = service.loadIconByApplicationInfo(aInfo);
                        postInvalidate(drawable, bmp, key);
                    } else {
                        ResolveInfo rInfo = resolveInfoMap.get(key);
                        if (rInfo != null) {
                            final Bitmap bmp = service.loadIconByResolveInfo(rInfo);
                            postInvalidate(drawable, bmp, key);
                        } else {
                            Log.d("simply", "没有找到这个drawable，没有重绘，key:" + key);
                            // dumpResolveInfo(resolveInfos);
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void postInvalidate(final BitmapDrawable drawable, final Bitmap bmp, final String key) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (drawable != null) {
                    if (DEBUG)
                        Log.d(TAG, "postInvalidate, key:" + key);
                    setDrawableBitmap(drawable, bmp);
                    synchronized (sIconCache) {
                        sIconCache.put(key, new WeakReference<Bitmap>(drawable.getBitmap()));
                    }
                }
            }
        });
    }

    // redraw cached bitmap drawable
    private void setDrawableBitmap(BitmapDrawable drawable, Bitmap bitmap) {
        if (drawable == null || bitmap == null) {
            return;
        }
        Class<?> drawableType = drawable.getClass();
        Method method = null;
        try {
            method = drawableType.getDeclaredMethod("setBitmap", Bitmap.class);
            method.setAccessible(true);
            method.invoke(drawable, bitmap);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static List<ApplicationInfo> queryApplicationInfo(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> list = pm.getInstalledPackages(0);
        List<ApplicationInfo> applicationInfos = new ArrayList<ApplicationInfo>();
        for (PackageInfo pi : list) {
            ApplicationInfo ai = null;
            try {
                ai = pm.getApplicationInfo(pi.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            applicationInfos.add(ai);
        }
        return applicationInfos;
    }

    private ApplicationInfo getApplicationInfo(List<ApplicationInfo> appInfos, String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        String packageName = null;
        String className = null;
        for (ApplicationInfo ai : appInfos) {
            packageName = ai.packageName;
            className = ai.className;
            if (key.equals(packageName + className)) {
                return ai;
            }
        }
        return null;
    }

    public static List<ResolveInfo> queryResolveInfo(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // 通过查询，获得所有ResolveInfo对象.
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfos;
    }

    private ResolveInfo getResolveInfo(List<ResolveInfo> resolveInfos, String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }

        String packageName = null;
        String className = null;
        for (ResolveInfo ri : resolveInfos) {
            if (ri.activityInfo != null) {
                packageName = ri.activityInfo.packageName;
                className = ri.activityInfo.name;
            } else if (ri.serviceInfo != null) {
                packageName = ri.serviceInfo.packageName;
                className = ri.serviceInfo.name;
            }

            if (key.equals(packageName + className)) {
                return ri;
            }
        }
        return null;
    }

    private void dumpResolveInfo(List<ResolveInfo> resolveInfos) {
        String packageName = null;
        String className = null;
        for (ResolveInfo ri : resolveInfos) {
            if (ri.activityInfo != null) {
                packageName = ri.activityInfo.packageName;
                className = ri.activityInfo.name;
            } else if (ri.serviceInfo != null) {
                packageName = ri.serviceInfo.packageName;
                className = ri.serviceInfo.name;
            }
        }
    }

    public void reconnection() {
        if (mConnection != null) {
            mConnection.reconnection();
        }
    }

    public Drawable getBlurredWallpaper() {
        if (mBlurWallpaper == null) {
            // Check connection fist;
            if (!mConnection.isConnected()) {
                mConnection.connect();
                // If connection is NOT OK,load blur wallpaper at client application
                return loadBlurWallpaperAtLocal(mContext);
            }

            Bitmap bitmap = loadBlurWallpaperFromRemote();
            mBlurWallpaper = drawBlurWallpaper(bitmap);
        }
        return mBlurWallpaper;
    }

    /**
     * @author Fan.Yang
     * 优先从文件中读取模糊壁纸，然后再使用远端服务处理
     */
    private Bitmap loadBlurWallpaperFromRemote() {
        Bitmap bitmap = BlurWallpaperUtils.getBlurWallpaperFromStaticCache();
        if (bitmap == null) {
            // get blur wallpaper from remote service
            IThemeService service = mConnection.getService();
            try {
                bitmap = service.getBlurredWallpaper();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * @author Fan.Yang
     * 如果远程服务没有链接，需要本地处理模糊壁纸
     */
    private Drawable loadBlurWallpaperAtLocal(Context context) {
        Bitmap bitmap = BlurWallpaperUtils.loadBlurWallpaper(context);
        return drawBlurWallpaper(bitmap);
    }

    private Drawable drawBlurWallpaper(Bitmap bitmap) {
        if (bitmap == null) {
            return mBlurWallpaper;
        }
        int sWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int sHeight = mContext.getResources().getDisplayMetrics().heightPixels;

        Log.d(TAG, "drawBlurWallpaper, packageName:" + mContext.getPackageName());
        Bitmap bm = BlurWallpaperUtils.generateBitmap(mContext, bitmap, sWidth, sHeight);
        Bitmap cropBitmap = BlurWallpaperUtils.cropBitmap(bm, 0, 0, bm.getWidth(), 168);
        if (mBlurWallpaper == null) {
            if (bm != null) {
                mBlurWallpaper = new BitmapDrawable(cropBitmap);
            }
        } else {
            setDrawableBitmap((BitmapDrawable) mBlurWallpaper, cropBitmap);
        }
        return mBlurWallpaper;
    }

    private Context mThemeContext = null;

    public boolean isWeatherLockScreenEnable() {
        if (getThemeChooserContext() != null) {
            SharedPreferences sharedPreferences = getThemeChooserContext()
                    .getSharedPreferences("theme_applied", Context.MODE_MULTI_PROCESS);
            String packageName = sharedPreferences.getString("lock_screen", "");
            if (packageName.equals(Manifest.PACKAGE_NAME)) {
                return true;
            } else if (packageName.equals(ThemeManager.THEME_LOCKSCREEN2_PACKAGE)) {
                return false;
            }
        }

        // face路径下存在lockstyle文件，不使用天气动画
        return !lockStyleExists();
    }

    private Context getThemeChooserContext() {
        if (mThemeContext == null) {
            try {
                mThemeContext = mContext.createPackageContext("com.lewa.themechooser",
                        Context.CONTEXT_IGNORE_SECURITY);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return mThemeContext;
    }

    public int allowChangeLockScreenWallpaper() {
        if (getThemeChooserContext() != null) {
            SharedPreferences sharedPreferences = getThemeChooserContext()
                    .getSharedPreferences("theme_applied", Context.MODE_MULTI_PROCESS);
            boolean isEnable = Settings.Secure.getInt(mContext.getContentResolver(),
                    "lock_screen_fancy_weather_enabled", 1) == 1 ? true : false;
            String packageName = sharedPreferences.getString("lock_screen", "");
            if (isEnable) {
                // fancy wallpaper
                return 1;
            } else if (packageName.equals(ThemeManager.THEME_LOCKSCREEN2_PACKAGE)) {
                // can not change wallpaper for this lockscreen
                return 2;
            }
        }
        return 0;
    }

    private String getLockScreenFilename() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/lockstyle";
    }

    private boolean lockStyleExists() {
        boolean lockExistsOne = new File(getLockScreenFilename()).length() > 0;
        boolean lockExistsTwo = new File(THEME_LOCKSCREEN_CONFIG).length() > 0;
        return (lockExistsOne || lockExistsTwo) ? true : false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // ThemeService created after connected.
        if (intent.getAction().equals(ACTION_SERVICE_CREATED)) {
            if (!mConnection.isConnected()) {
                mMainHandler.sendEmptyMessageDelayed(SERVICE_CONNECTION_MSG, 1000);
            }
        }
    }

    class InvalidateDrawableThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            mInvalidateHandler = new Handler();
            Looper.loop();
        }
    }

    private IThemeServiceCallback.Stub mCallback = new IThemeServiceCallback.Stub() {

        @Override
        public void onThemeChanged() throws RemoteException {
            mMainHandler.sendEmptyMessage(THEME_CHANGED_MSG);
        }

        @Override
        public void onWallpaperChanged(Bitmap blurredWallpaper) throws RemoteException {
            if (careWallpaperChange) {
                Message msg = new Message();
                msg.what = WALLPAPER_CHANGED_MSG;
                msg.obj = blurredWallpaper;
                mMainHandler.sendMessage(msg);
            }
        }
    };

    class ThemeConnection implements ServiceConnection {

        /**
         * Denotes a successful operation.
         */
        public static final int SUCCESS = 0;
        /**
         * Denotes a generic operation failure.
         */
        public static final int ERROR = -1;

        private String PACKAGE_NAME = "com.lewa.thememanager";
        private String CLASS_NAME = "com.lewa.themes.service.ThemeService";

        private long debugTime;

        private final Object mStartLock = new Object();

        private Context mContext;
        private IThemeService mService;
        private boolean mConnected = false;
        private OnInitListener mListener;

        ThemeConnection(Context context, OnInitListener initListener) {
            mContext = context;
            mListener = initListener;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mStartLock) {
                if (DEBUG) {
                    debugTime = System.currentTimeMillis() - debugTime;
                    Log.d(TAG, "ThemeConnection onServiceConnected.Connect time:" + debugTime +
                            ", packageName:" + mContext.getPackageName());
                }
                mConnected = true;
                mService = IThemeService.Stub.asInterface(service);

                try {
                    if (mService != null) {
                        mService.registerCallback(getCurProcessName(mContext), mCallback);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if (mListener != null) {
                    mListener.onInit(SUCCESS);
                }
            }
        }

        String getCurProcessName(Context context) {
            int pid = android.os.Process.myPid();
            ActivityManager mActivityManager = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager
                    .getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    return appProcess.processName;
                }
            }
            return context.getPackageName();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mStartLock) {
                if (DEBUG) {
                    debugTime = System.currentTimeMillis() - debugTime;
                    Log.d(TAG, "ThemeConnection onServiceDisconnected,Disconnection time:" +
                            debugTime + ", packageName:" + mContext.getPackageName());
                }
                mService = null;
                mConnected = false;
                if (mListener != null) {
                    mListener.onInit(ERROR);
                    mListener = null;
                }
            }
        }

        public void connect() {
            synchronized (mStartLock) {
                if (!mConnected) {
                    if (DEBUG) {
                        debugTime = System.currentTimeMillis();
                        Log.d(TAG, "ThemeConnection connect, package:" + mContext.getPackageName());
                    }
                    Intent intent = new Intent(THEME_SERVICE_ACTION);
                    intent.setComponent(new ComponentName(PACKAGE_NAME, CLASS_NAME));
                    mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
                }
            }
        }

        public void disconnect() {
            synchronized (mStartLock) {
                if (mConnected) {
                    try {
                        if (DEBUG) {
                            debugTime = System.currentTimeMillis();
                            Log.d(TAG, "ThemeConnection disconnect.");
                        }
                        mContext.unbindService(this);
                    } catch (IllegalArgumentException ex) {
                        Log.v(TAG, "disconnect failed: " + ex);
                    }
                    mConnected = false;
                }
            }
        }

        public void reconnection() {
            synchronized (mStartLock) {
                disconnect();
                mMainHandler.sendEmptyMessageDelayed(SERVICE_CONNECTION_MSG, 1000);
            }
        }

        public synchronized boolean isConnected() {
            return (mService != null && mConnected);
        }

        public IThemeService getService() {
            return mService;
        }

        public Bitmap loadIconByResolveInfo(ResolveInfo resolveInfo) {
            try {
                if (isConnected()) {
                    return mService.loadIconByResolveInfo(resolveInfo);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }

        public Bitmap loadIconByApplicationInfo(ApplicationInfo applicationInfo) {
            try {
                if (isConnected()) {
                    return mService.loadIconByApplicationInfo(applicationInfo);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    public interface OnInitListener {
        public void onInit(int status);
    }

    class ThemeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isIconChanged = intent.getBooleanExtra("iconChanged", false);
            if (DEBUG) {
                Log.d(TAG, "ThemeManager:onReceive, isIconChanged:" + isIconChanged +
                        ", packageName:" + context.getPackageName() + ",careThemeChange:" +
                        careThemeChange);
            }
            if (isIconChanged && careThemeChange) {
                synchronized (sIconCache) {
                    sIconCache.clear();
                }

                if (mInvalidateHandler != null) {
                    mInvalidateHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            invalidateCachedDrawable();
                        }
                    });
                }
            }
        }
    }
}
