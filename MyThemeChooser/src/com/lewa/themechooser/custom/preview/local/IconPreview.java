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

public class IconPreview extends PreviewIconsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void doApply(ThemeItem bean) {
        ThemeItem appliedTheme = Themes.getAppliedTheme(this);
        if (null == appliedTheme) {
            return;
        }
        Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
        appliedTheme.close();

        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
        i.putExtra(ThemeManager.EXTRA_ICONS_URI, bean.getIconsUri());
        if (bean.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                && bean.getThemeId().equals("LewaDefaultTheme")) {
            i.putExtra(ThemeManager.DEFAULT_ICON, true);
        }
        i.putExtra(CustomType.EXTRA_NAME, CustomType.DESKTOP_ICON_TYPE);
        ThemeUtil.isChangeIcon = true;
        ApplyThemeHelp.changeTheme(this, i);

        ThemeApplication.sThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(this
                , bean, PreviewsType.LAUNCHER_ICONS), com.lewa.themechooser.ThemeStatus.THEME_TYPE_ICONS);
    }

    private String parseUriNullSafe(Uri uri) {
        return (uri != null ? uri.toString() : null);
    }

    public ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeItem, PreviewsType.LAUNCHER_ICONS);
    }

    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_icon);
    }

    @Override
    protected String getDeleteToast() {
        return getString(R.string.icons_delete_success);
    }
}
