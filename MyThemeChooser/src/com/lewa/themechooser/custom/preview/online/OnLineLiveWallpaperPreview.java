package com.lewa.themechooser.custom.preview.online;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.service.wallpaper.WallpaperService;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.preview.slide.adapter.OnlinePreviewIcons;
import com.lewa.themechooser.receiver.ThemeInstallService;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OnLineLiveWallpaperPreview extends OnlinePreviewIcons {
    private WallpaperManager mWallpaperManager;

    public OnLineLiveWallpaperPreview() {
        super();
        mThemeType = com.lewa.themechooser.ThemeStatus.THEME_TYPE_LIVEWALLPAPER;
    }

    protected void doDeleteTheme() {
        super.doDeleteTheme();
    }

    @Override
    protected ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeBase);
    }

    protected void applyTheme() {
        boolean isExistFlag = false;
        List<ResolveInfo> list = getPackageManager().queryIntentServices(
                new Intent(WallpaperService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        WallpaperInfo info = null;
        for (ResolveInfo resolveInfo : list) {
            try {
                info = new WallpaperInfo(this, resolveInfo);
                if (info.getPackageName().equals(mThemeBase.getPackageName()) && info.getServiceName().equals(mThemeBase.getThemeId())) {
                    isExistFlag = true;
                    break;
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isExistFlag) {
            mChangeHelper.beginChange(mThemeBase.getNameByLocale());
            ThemeItem appliedTheme = Themes.getAppliedTheme(this);
            if (null == appliedTheme) {
                return;
            }
            Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
            appliedTheme.close();

            Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
            ThemeItem mThemeBean = Themes.getTheme(this, mThemeBase.getPackageName(), mThemeBase.getThemeId());
            i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
            i.putExtra(ThemeManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(mThemeBase.getPackageName(), mThemeBase.getThemeId()));
            i.putExtra(CustomType.EXTRA_NAME, CustomType.LIVE_WALLPAPER);
            ApplyThemeHelp.changeTheme(this, i);
            ThemeApplication.sThemeStatus.setAppliedPkgName(mThemeBase.getPackageName()+mThemeBase.getThemeId(), ThemeStatus.THEME_TYPE_WALLPAPER);
            ThemeApplication.sThemeStatus.setAppliedPkgName(mThemeBase.getPackageName()+mThemeBase.getThemeId(), ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
        } else {
            if (new File(new StringBuilder().append(ThemeConstants.THEME_LWT)
                    .append("/").append(mThemeBase.getPkg()).toString()).exists()) {
                Intent intent = new Intent(this, ThemeInstallService.class);
                intent.putExtra("THEME_PACKAGE", new StringBuilder().append(
                        ThemeConstants.THEME_LWT).append("/").append(mThemeBase.getPkg()).toString());
                intent.putExtra("APPLY", true);
                this.startService((intent));
                Toast.makeText(this, getString(R.string.init_install_theme), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected String getLoadPath(ThemeBase themeBase, Context context) {
        return ThemeConstants.THEME_PATH;
    }


    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_live_wallpaper);
    }
}
