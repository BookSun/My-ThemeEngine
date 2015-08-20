
package com.lewa.support.theme;

import static com.lewa.support.util.Process.IS_SYSTEM;
import android.app.Application;
import android.content.Context;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;

import com.lewa.support.laml.RenderThread;
import com.lewa.support.laml.util.RendererCoreCache;
import com.lewa.support.laml.util.RendererCoreCache.RendererCoreInfo;

public class AppIconsHelper {

    public static final int TIME_MIN = 60 * 1000;

    public static final int TIME_HOUR = TIME_MIN * 60;

    public static final int TIME_DAY = TIME_HOUR * 24;

    private static RendererCoreCache mRendererCoreCache;

    private static int mThemeChanged = -1;

    private AppIconsHelper() {
        throw new AssertionError();
    }

    private static void checkVersion(Context context) {
        int version = SystemProperties.getInt("sys.lewa.themeChanged", -1);
        if (version > mThemeChanged) {
            clearCache();
            mThemeChanged = version;
        }
    }

    public static void cleanUp() {
        RenderThread.globalThreadStop();
    }

    public static void clearCache() {
        if (mRendererCoreCache != null)
            mRendererCoreCache.clear();
    }

    public static Drawable getIconDrawable(Context context, PackageItemInfo info, PackageManager pm) {
        return getIconDrawable(context, info, pm, 0L);
    }

    public static Drawable getIconDrawable(Context context, PackageItemInfo info,
            PackageManager pm, long cacheTime) {
        String packageName = info.packageName;
        String activityName = info.name;
        Drawable d = getIconDrawable(context, packageName, activityName, cacheTime);
        return d == null ? info.loadIcon(pm) : d;
    }

    public static Drawable getIconDrawable(Context context, ResolveInfo info, PackageManager pm) {
        return getIconDrawable(context, info, pm, 0);
    }

    public static Drawable getIconDrawable(Context context, ResolveInfo info, PackageManager pm,
            long cacheTime) {
        ComponentInfo ci = info.activityInfo != null ? info.activityInfo : info.serviceInfo;
        return getIconDrawable(context, ci, pm, cacheTime);
    }

    public static Drawable getIconDrawable(Context context, String name) {
        return getIconDrawable(context, name, 0);
    }

    public static Drawable getIconDrawable(Context context, String packageName, String activityName) {
        return getIconDrawable(context, packageName, activityName, 0);
    }

    public static Drawable getIconDrawable(Context context, String name, long cacheTime) {
        context = ensureContext(context);
        return getIconDrawable(context, name, cacheTime, new Handler(context.getMainLooper()));
    }

    public static Drawable getIconDrawable(Context context, String packageName,
            String activityName, long cacheTime) {
        context = ensureContext(context);
        return getIconDrawable(context, packageName, activityName, cacheTime,
                new Handler(context.getMainLooper()));
    }

    public static Drawable getIconDrawable(Context context, String packageName,
            String activityName, long cacheTime, Handler h) {
        String fancyIconRelativePath = IconCustomizer.getFancyIconRelativePath(packageName,
                activityName);
        return getIconDrawable(context, fancyIconRelativePath, cacheTime, h);
    }

    public static Drawable getIconDrawable(Context context, String name, long cacheTime, Handler h) {
        RendererCoreCache.RendererCoreInfo ri;
        if (mRendererCoreCache == null)
            mRendererCoreCache = new RendererCoreCache(h);
        try {
            if (IS_SYSTEM)
                checkVersion(context);
            ri = mRendererCoreCache.get(name, cacheTime);
            if (ri == null || ri.r == null) {
                ri = mRendererCoreCache.get(name, context, cacheTime, new FancyIconResourceLoader(
                        name), h);
            }
            return ri.r == null ? null : new FancyDrawable(ri.r);
        } catch (Exception e) {
            Log.e("LAML AppIconsHelper", "getIconDrawable", e);
        }
        return null;
    }

    private static Context ensureContext(Context context) {
        if (context == null) {
            try {
                context = (Context) Application.class.getMethod("getInstance").invoke(null);
            } catch (Exception e) {
            }
        }
        return context;
    }
}
