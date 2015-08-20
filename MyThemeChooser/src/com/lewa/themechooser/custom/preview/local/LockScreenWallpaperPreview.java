package com.lewa.themechooser.custom.preview.local;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.preview.slide.local.PreviewIconsActivity;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;

import util.ThemeUtil;

public class LockScreenWallpaperPreview extends PreviewIconsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void doApply(ThemeItem bean) {
        Uri lockWallpaperUri = null;
        ThemeItem appliedTheme = Themes.getAppliedTheme(this);
        if (null == appliedTheme) {
            return;
        }
        Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
        boolean isDefault = false;
        if (null == bean) {
            lockWallpaperUri = mThemeUri;
        } else {
            lockWallpaperUri = bean.getLockWallpaperUri(this);
            if(bean.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                    && bean.getThemeId().equals("LewaDefaultTheme")){
                isDefault = true;
            }
        }
        appliedTheme.close();
        ThemeUtil.supportsLockWallpaper(this);
        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
        i.putExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI, PackageResources.convertFilePathUri(lockWallpaperUri));
        i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_WALLPAPER, isDefault);
        i.putExtra(CustomType.EXTRA_NAME, CustomType.LOCKSCREEN_WALLPAPER_TYPE);
        ApplyThemeHelp.changeTheme(this, i);
        ThemeApplication.sThemeStatus.setAppliedPkgName(getThemePackageName()
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
    }

    public ImageAdapter initAdapter() {
        if (null != mThemeItem) {
            //TCL937553 add by Fan.Yang
            return new ImageAdapter(this, mThemeItem, PreviewsType.LOCKWALLPAPER);
        } else {
            return new ImageAdapter(this, mThemeUri);
        }
    }

    protected String getThemePackageName() {
        if (null != mThemeItem) {
            return super.getThemePackageName();
        } else {
            String name = mThemeUri.toString();
            int lastSlash = name.lastIndexOf('/');
            name = name.substring(++lastSlash, name.length());
            try {
                name = java.net.URLDecoder.decode(name, "UTF-8");
            } catch (Exception e) {
            }
            return name;
        }
    }

    protected boolean isThemeApplied() {
        if (null != mThemeItem) {
            return super.isThemeApplied() || mThemeStatus.isApplied(null
                    , getThemePackageName(), com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        } else {
            return mThemeStatus.isApplied(null
                    , getThemePackageName(), com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        }
    }

    @Override
    protected String getDeleteToast() {
        return getString(R.string.lockwallpaper_delete_success);
    }

    protected void doDeleteTheme() {
        if (null != mThemeItem) {
            super.doDeleteTheme();
            String SuffixStr = "";
            try {
                SuffixStr = mThemeItem.getLockWallpaperUri(this).toString().split("\\.")[1];

            } catch (Exception e) {
                Log.d(ThemeConstants.TAG, "e.error=" + e.getMessage());
                e.printStackTrace();
            }
            mThemeStatus.setDeleted(mThemeItem.getPackageName(), mThemeItem.getName()
                    , com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        } else {
            try {
                new java.io.File(new java.net.URI(mThemeUri.toString())).delete();
            } catch (Exception e) {
            }
            mThemeStatus.setDeleted(null, getThemePackageName()
                    , com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
        }
    }

    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_lock_screen_wallpaper);
    }
}
