package com.lewa.themechooser.custom.preview.online;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
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

import java.io.File;

import util.ThemeUtil;

public class OnLineFontsPreview extends OnlinePreviewIcons {
    public OnLineFontsPreview() {
        super();
        mThemeType = com.lewa.themechooser.ThemeStatus.THEME_TYPE_FONT;
    }

    protected ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeBase);
    }

    protected void applyTheme() {
        boolean reboot = getResources().getBoolean(R.bool.config_font_reboot);
        final ThemeItem item = Themes.getTheme(this, mThemeBase.getPackageName(), mThemeBase.getThemeId());
        if (item != null && item.getFontUril() != null) {
            ThemeUtil.isChangeFont = true;
        }
        if (reboot && ThemeUtil.isChangeFont) {
            new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.font_change_reboot)
                    .setPositiveButton(R.string.apply_reboot, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            applyThemeInternal(item);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
        } else {
            applyThemeInternal(item);
        }
    }

    protected void applyThemeInternal(ThemeItem item) {
        if (item == null) {
            if (new File(new StringBuilder().append(ThemeConstants.THEME_LWT).append("/").append(mThemeBase.getPkg()).toString()).exists()) {
                Intent intent = new Intent(this, ThemeInstallService.class);
                intent.putExtra("THEME_PACKAGE", new StringBuilder().append(ThemeConstants.THEME_LWT).append("/").append(mThemeBase.getPkg()).toString());
                intent.putExtra("APPLY", true);
                this.startService((intent));
                Toast.makeText(this, getString(R.string.init_install_theme), Toast.LENGTH_SHORT).show();
            }
        } else {
            ThemeItem appliedTheme = Themes.getAppliedTheme(this);
            if (null == appliedTheme) {
                return;
            }
            if (!ThemeUtil.isDataHasSpace(30)) {
                removeDialog(0);
                Toast.makeText(this, getString(R.string.data_no_size), Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
            appliedTheme.close();
            ThemeItem mThemeBean = Themes.getTheme(this, mThemeBase.getPackageName(), mThemeBase.getThemeId());
            Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
            i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
            i.putExtra(ThemeManager.EXTRA_FONT_URI, mThemeBean.getFontUril());
            i.putExtra(CustomType.EXTRA_NAME, CustomType.FONT_TYPE);
            mChangeHelper.beginChange(mThemeBean.getName());
            ThemeUtil.isKillProcess = true;

            //RC48063-jianwu.gao add begin
            //fix bug : reset wallpaper to default after set font
            String wallpaperUriString = ThemeApplication.sThemeStatus.getAppliedThumbnail(ThemeStatus.THEME_TYPE_WALLPAPER);
            if(null != wallpaperUriString && !TextUtils.isEmpty(wallpaperUriString)) {
                File file;
                Uri wallpaperUri;
                if (!wallpaperUriString.startsWith("file")) {
                    file = new File(wallpaperUriString);
                    wallpaperUri = Uri.fromFile(file);
                } else {
                    wallpaperUri = Uri.parse(wallpaperUriString);
                }
                i.putExtra("wallpaperUri", wallpaperUri);
            }
            //RC48063-jianwu.gao add end

            ApplyThemeHelp.changeTheme(this, i);
            ThemeApplication.sThemeStatus.setAppliedPkgName(mThemeBean.getPackageName(), com.lewa.themechooser.ThemeStatus.THEME_TYPE_FONT);
        }
    }

    @Override
    protected String getLoadPath(ThemeBase themeBase, Context context) {
        return ThemeConstants.THEME_PATH;
    }


    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_font);
    }
}
