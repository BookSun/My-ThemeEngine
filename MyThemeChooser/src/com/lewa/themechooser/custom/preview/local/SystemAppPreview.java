package com.lewa.themechooser.custom.preview.local;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themechooser.preview.slide.local.PreviewIconsActivity;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;

import util.ThemeUtil;

public class SystemAppPreview extends PreviewIconsActivity {

    private static final String TAG = "SystemAppPreview";
    private static final Boolean DBG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void doApply(ThemeItem bean) {
        ThemeItem appliedTheme = Themes.getAppliedTheme(this);
        if (null == appliedTheme) {
            return;
        }
        Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
        Uri default_deskTopWallpaper_uri;
        Uri default_font_uri;
        Uri default_icon_uri;
        Uri default_LockScreenWallpaper_uri;
        default_deskTopWallpaper_uri = appliedTheme.getWallpaperUri(this);
        default_font_uri = appliedTheme.getFontUril();
        default_icon_uri = appliedTheme.getIconsUri();
        default_LockScreenWallpaper_uri = appliedTheme.getLockWallpaperUri(this);
        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, bean.getUri(this));
        i.putExtra(CustomType.EXTRA_NAME, CustomType.SYSTEM_APP);
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
        ThemeUtil.isKillProcess = true;
        if (bean.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                && bean.getThemeId().equals("LewaDefaultTheme")) {
            if (ThemeApplication.sThemeStatus.isApplied("com.lewa.theme.LewaDefaultTheme"
                    , com.lewa.themechooser.ThemeStatus.THEME_TYPE_STYLE)) {
                ThemeUtil.isKillProcess = false;
            } else {
                ThemeUtil.isKillProcess = true;
            }
        }
        ApplyThemeHelp.changeTheme(this, i);
        ThemeApplication.sThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(this
                , bean, PreviewsType.FRAMEWORK_APPS), com.lewa.themechooser.ThemeStatus.THEME_TYPE_STYLE);
        appliedTheme.close();
    }

    @Override
    public ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeItem, PreviewsType.FRAMEWORK_APPS);
    }

    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_system_app);
    }

    @Override
    protected String getDeleteToast() {
        return getString(R.string.system_style_delete_success);
    }
}
