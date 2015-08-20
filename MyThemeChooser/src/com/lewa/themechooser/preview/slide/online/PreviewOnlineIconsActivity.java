package com.lewa.themechooser.preview.slide.online;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.preview.slide.adapter.OnlinePreviewIcons;
import com.lewa.themechooser.receiver.ThemeInstallService;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;

import java.io.File;

import util.ThemeUtil;

public class PreviewOnlineIconsActivity extends OnlinePreviewIcons {
    @Override
    protected ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeBase);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return super.onCreateDialog(id, args);
    }

    @Override
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

    private void applyThemeInternal(ThemeItem item) {
        if (item == null) {
            if (new File(new StringBuilder().append(ThemeConstants.THEME_LWT)
                    .append("/").append(mThemeBase.getPkg()).toString()).exists()) {
                Intent intent = new Intent(this, ThemeInstallService.class);
                intent.putExtra("THEME_PACKAGE", new StringBuilder().append(
                        ThemeConstants.THEME_LWT).append("/").append(mThemeBase.getPkg()).toString());
                intent.putExtra("APPLY", true);
                this.startService((intent));
                Toast.makeText(this, getString(R.string.init_install_theme), Toast.LENGTH_SHORT).show();
            }
        } else {
            doApply(item);

            mThemeStatus.setAppliedPkgName(mThemeBase.getPackageName(), ThemeStatus.THEME_TYPE_PACKAGE);
            if (item.getWallpaperUri(this) != null) {
                ThemeApplication.sThemeStatus.setAppliedPkgName(
                        null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
            }

            if (item.getIconsUri() != null) {
                mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(this
                        , item, PreviewsType.LAUNCHER_ICONS), ThemeStatus.THEME_TYPE_ICONS);
            }
            if (item.getWallpaperUri(this) != null) {
                mThemeStatus.setAppliedThumbnail(item.getWallpaperUri(this)
                        , ThemeStatus.THEME_TYPE_WALLPAPER);
            }
            if (item.getFontUril() != null) {
                mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getThumbnails(this
                        , item, PreviewsType.FONTS), ThemeStatus.THEME_TYPE_FONT);
            }
            if (item.getLockscreenUri() != null) {
                mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getThumbnails(this
                        , item, PreviewsType.LOCKSCREEN), ThemeStatus.THEME_TYPE_LOCK_SCREEN);
                mThemeStatus.setAppliedThumbnail("", ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
            }
            if (item.getLockWallpaperUri(this) != null) {
                mThemeStatus.setAppliedThumbnail(item.getLockWallpaperUri(this)
                        , ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
            }
            if (item.hasThemePackageScope()) {
                mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getThumbnails(this
                        , item, PreviewsType.FRAMEWORK_APPS), ThemeStatus.THEME_TYPE_STYLE);
            }
        }
    }

    public void doApply(ThemeItem bean) {
        Uri uri = bean.getUri(this);
        mChangeHelper.beginChange(bean.getName());
        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        if (bean.hasThemePackageScope()) {
            i.putExtra(ThemeManager.EXTRA_SYSTEM_APP, true);
        }
        ThemeUtil.updateCurrentThemeInfo(bean);
        Settings.System.putString(getContentResolver(),"lewa_wallpaper_path", "");
        ApplyThemeHelp.changeTheme(this, i);
    }

    @Override
    protected String getLoadPath(ThemeBase themeBase, Context context) {
        return ThemeConstants.THEME_PATH;
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(String packageName, int returnCode) {
            Message msg = mHandler.obtainMessage(DELETE_THEME);
            mHandler.sendMessage(msg);
        }
    }
}
