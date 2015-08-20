package com.lewa.themes.service;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import com.lewa.themes.service.IThemeServiceCallback;

/**
 * Created by ivonhoe on 15-3-6.
 */
interface IThemeService {
    void registerCallback(in String packageName,in IThemeServiceCallback cb);
    void unregisterCallback(in String packageName,in IThemeServiceCallback cb);
    Bitmap loadIconByResolveInfo(in ResolveInfo ri);
    Bitmap loadIconByApplicationInfo(in ApplicationInfo ai);
    void clearCustomizedIcons(String packageName);
    void reset();
    void checkModIcons();
    String getFancyIconRelativePath(in String packageName, in String className);
    //get current theme package name
    String getThemePackageName(String propertyName);
    //get current blur wallpaper
    Bitmap getBlurredWallpaper();
}
