package com.lewa.themechooser.custom.preview.online;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.preview.slide.adapter.OnlinePreviewIcons;
import com.lewa.themechooser.receiver.ThemeInstallService;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;

import java.io.File;

import util.ThemeUtil;

public class OnLineLockScreenWallpaperPreview extends OnlinePreviewIcons {
    ProgressDialog dialog;

    public OnLineLockScreenWallpaperPreview() {
        super();
        mThemeType = com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER;
    }

    public void showHome() {
        dialog.cancel();
        Toast.makeText(this, getString(R.string.theme_change_dialog_title_success),
                Toast.LENGTH_SHORT).show();
    }

    public void setWallpaper() {
        ThemeItem mThemeBean = Themes.getTheme(this
                , mThemeBase.getPackageName(), mThemeBase.getThemeId());
        Uri lock_wallpaper_uri = null;
        try {
//            lock_wallpaper_uri = Uri.fromFile(
//                    new File(ThemeConstants.THEME_WALLPAPER
//                    ,mThemeBase.getCombinedName()));
            lock_wallpaper_uri = mThemeBean.getLockWallpaperUri(this);

        } catch (Exception e) {
        }
        if (lock_wallpaper_uri == null) return;

        ThemeItem appliedTheme = Themes.getAppliedTheme(OnLineLockScreenWallpaperPreview.this);
        Uri uri = Themes.getThemeUri(OnLineLockScreenWallpaperPreview.this
                , appliedTheme.getPackageName(), appliedTheme.getThemeId());
        appliedTheme.close();
        ThemeUtil.supportsLockWallpaper(this);
        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
        i.putExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI, PackageResources.convertFilePathUri(lock_wallpaper_uri));
        i.putExtra(CustomType.EXTRA_NAME, CustomType.LOCKSCREEN_WALLPAPER_TYPE);
        mChangeHelper.beginChange(getString(R.string.lockscreen_wallpaper));
        ApplyThemeHelp.changeTheme(OnLineLockScreenWallpaperPreview.this, i);
        ThemeApplication.sThemeStatus.setAppliedPkgName(mThemeBase.getPackageName()
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        mHandler.sendMessage(Message.obtain());
    }

    @Override
    protected ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeBase);
    }

    @Override
    protected void applyTheme() {
        if (!mThemeBase.getPkg().endsWith(".lwt")) {
//            dialog = new ProgressDialog(new
//                    ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog));
//            dialog.setTitle(getString(R.string.theme_change_dialog_title));
//            dialog.setMessage(getString(R.string.switching_to_wallpaper,getString(R.string.lockscreen_wallpaper)));
//            dialog.setCancelable(false);
//            dialog.setIndeterminate(true);
//            dialog.show();
//            WallpaperUpdater.start(this);
            setWallpaper();
            return;
        }
        if (Themes.getTheme(this, mThemeBase.getPackageName(), mThemeBase.getThemeId()) == null) {
            if (new File(new StringBuilder().append(ThemeConstants.THEME_LWT).append("/")
                    .append(mThemeBase.getPkg()).toString()).exists()) {
                Intent intent = new Intent(this, ThemeInstallService.class);
                intent.putExtra("THEME_PACKAGE", new StringBuilder().append(
                        ThemeConstants.THEME_LWT).append("/").append(mThemeBase.getPkg()).toString());
                intent.putExtra("APPLY", true);
                this.startService((intent));
                Toast.makeText(this, getString(R.string.init_install_theme), Toast.LENGTH_SHORT).show();
            }
        } else {
            ThemeItem appliedTheme = Themes.getAppliedTheme(this);
            if (null == appliedTheme) {
                return;
            }
            Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
            appliedTheme.close();
            ThemeItem mThemeBean = Themes.getTheme(this
                    , mThemeBase.getPackageName(), mThemeBase.getThemeId());
            Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
            i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
            i.putExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI, mThemeBean.getLockWallpaperUri(this));
            i.putExtra(CustomType.EXTRA_NAME, CustomType.LOCKSCREEN_WALLPAPER_TYPE);
            mChangeHelper.beginChange(mThemeBean.getName());
            ApplyThemeHelp.changeTheme(this, i);
            ThemeApplication.sThemeStatus.setAppliedThumbnail(mThemeBean.getLockWallpaperUri(this)
                    , com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        }
    }

    @Override
    protected String getFitAttachment() {
        String start = mThemeBase.attachment.substring(0,
                mThemeBase.attachment.lastIndexOf("/") + 1);
        String end = mThemeBase.attachment.substring(mThemeBase.attachment.lastIndexOf("/"),
                mThemeBase.attachment.length());
        String str = "_" + ThemeUtil.screen_width + "-"
                + ThemeUtil.screen_height;
        if (!mThemeBase.attachment.contains(str)) {
            mThemeBase.attachment = start + "_" + ThemeUtil.screen_width + "-"
                    + ThemeUtil.screen_height + end;
        }

        return mThemeBase.attachment;

    }

    @Override
    protected String getLoadPath(ThemeBase themeBase, Context context) {
        if (mThemeBase.getPkg().endsWith(".lwt")) {
            return ThemeConstants.THEME_PATH;
        } else {
            return ThemeConstants.LOCKWALLPAPER_PATH;
        }
    }

    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_lock_screen_wallpaper);
    }

    private static class WallpaperUpdater extends AsyncTask<Void, Void, Void> {
        private static WallpaperUpdater sUpdater;
        private OnLineLockScreenWallpaperPreview mActivity;

        public WallpaperUpdater(OnLineLockScreenWallpaperPreview activity) {
            mActivity = activity;
        }

        static void start(OnLineLockScreenWallpaperPreview activity) {
            if (sUpdater != null) {
                sUpdater.cancel(false);
            }
            (sUpdater = new WallpaperUpdater(activity)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        protected Void doInBackground(Void... params) {
            mActivity.setWallpaper();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
//            mActivity.showHome();
        }
    }
}
