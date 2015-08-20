package com.lewa.themechooser.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;

import util.ThemeUtil;

public class ApplyThemeForOtherAppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int type = intent.getIntExtra("theme_type", -1);
        String pkgName = intent.getStringExtra("pkgName");
        String themeId = intent.getStringExtra("themeId");
        ThemeItem mThemeItem = Themes.getTheme(context, pkgName, themeId);
        if (type == CustomType.DESKTOP_WALLPAPER_TYPE) {
            try {
                context.setWallpaper(ThemeUtil.CreateCropBitmap(context.getContentResolver()
                        .openInputStream(mThemeItem.getWallpaperUri(context))));
                ThemeApplication.sThemeStatus.setAppliedThumbnail(mThemeItem.getPackageName()
                        , com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (type == CustomType.LOCKSCREEN_WALLPAPER_TYPE) {
            ThemeItem appliedTheme = Themes.getAppliedTheme(context);
            if (null == appliedTheme) {
                return;
            }
            Uri uri = Themes.getThemeUri(context, appliedTheme.getPackageName(), appliedTheme.getThemeId());
            ThemeUtil.supportsLockWallpaper(context);
            Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
            i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
            i.putExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI, PackageResources.convertFilePathUri(mThemeItem.getLockWallpaperUri(context)));
            i.putExtra(CustomType.EXTRA_NAME, CustomType.LOCKSCREEN_WALLPAPER_TYPE);
            ApplyThemeHelp.changeTheme(context, i);
            ThemeApplication.sThemeStatus.setAppliedPkgName(mThemeItem.getPackageName()
                    , com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);

        } else {
            Uri uri = mThemeItem.getUri(context);
            Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
            if (mThemeItem.hasThemePackageScope()) {
                i.putExtra(ThemeManager.EXTRA_SYSTEM_APP, true);
            }
            if (mThemeItem.getFontUril() != null) {
                ThemeUtil.isKillProcess = true;
            }
            if (mThemeItem.getIconsUri() != null) {
                ThemeUtil.isChangeIcon = true;
            }
            ApplyThemeHelp.changeTheme(context, i);

            ThemeApplication.sThemeStatus.setAppliedPkgName(mThemeItem.getPackageName(),
                    ThemeStatus.THEME_TYPE_PACKAGE);
        }
    }

}
