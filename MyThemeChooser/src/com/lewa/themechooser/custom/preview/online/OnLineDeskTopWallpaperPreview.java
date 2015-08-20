package com.lewa.themechooser.custom.preview.online;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.provider.Settings;
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
import java.io.InputStream;

import util.ThemeUtil;

public class OnLineDeskTopWallpaperPreview extends OnlinePreviewIcons {
    private static final double SCALE = 1.5;
    private ProgressDialog dialog;

    public OnLineDeskTopWallpaperPreview() {
        super();
        mThemeType = com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER;

    }

    private void showHome() {
        //RC48063-jianwu.gao delete begin
        //fix bug : reset wallpaper to default after set font
        /*
            ThemeAppliction.sThemeStatus.setAppliedThumbnail(mThemeBase.getPackageName()
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
            ThemeAppliction.sThemeStatus.setAppliedPkgName(
                null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
        */
        //RC48063-jianwu.gao delete end
        try {
            dialog.cancel();
        } catch (Exception e) {
        }
        Toast.makeText(OnLineDeskTopWallpaperPreview.this
                , getString(R.string.theme_change_dialog_title_success), Toast.LENGTH_SHORT)
                .show();

        OnLineDeskTopWallpaperPreview.this.finish();

        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(mHomeIntent);
    }

    private void setWallpaper() {
        try {
            ThemeItem mThemeBean = Themes.getTheme(this
                    , mThemeBase.getPackageName(), mThemeBase.getThemeId());
            InputStream is = null;
            try {
                is = OnLineDeskTopWallpaperPreview.this.getContentResolver()
                        .openInputStream(mThemeBean.getWallpaperUri(this));
                setWallpaper(is);
                //RC48063-jianwu.gao add begin
                //fix bug : reset wallpaper to default after set font
                ThemeApplication.sThemeStatus.setAppliedThumbnail(mThemeBean.getWallpaperUri(this)
                        , com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
                ThemeApplication.sThemeStatus.setAppliedPkgName(
                        null, com.lewa.themechooser.ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
                Settings.System.putString(getContentResolver(), "lewa_wallpaper_path",mThemeBean.getWallpaperUri(this).getPath());
                //RC48063-jianwu.gao add end

            } catch (Exception e) {
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception ex) {
                    }
                }
            }
            mHandler.sendMessage(Message.obtain());
        } catch (Exception e) {
        }
    }

    @Override
    protected ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeBase);
    }

    @Override
    protected String getFitAttachment() {
        String start = mThemeBase.attachment.substring(0,
                mThemeBase.attachment.lastIndexOf("/") + 1);
        String end = mThemeBase.attachment.substring(mThemeBase.attachment.lastIndexOf("/"),
                mThemeBase.attachment.length());
        if (ThemeManager.STANDALONE && ThemeUtil.screenDPI.equals("XXHDPI")) {
            mThemeBase.attachment = start + "_" + (int) (ThemeUtil.screen_width * 2 / (SCALE))
                    + "-"
                    + (int) (ThemeUtil.previewHeight / (SCALE)) + end;
        } else {
            String str = "_" + ThemeUtil.screen_width * 2 + "-"
                    + ThemeUtil.screen_height;
            if (!mThemeBase.attachment.contains(str)) {
                mThemeBase.attachment = start + "_" + ThemeUtil.screen_width * 2 + "-"
                        + ThemeUtil.screen_height + end;
            }
        }
        return mThemeBase.attachment;
    }

    @Override
    protected void applyTheme() {
        if (!mThemeBase.getPkg().endsWith(".lwt")) {
            dialog = new ProgressDialog(this);
            dialog.setTitle(getString(R.string.theme_change_dialog_title));
            dialog.setMessage(getString(R.string.switching_to_wallpaper, getString(R.string.theme_model_wallpaper)));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            dialog.show();
            WallpaperUpdater.start(this);
            return;
        }
        if (Themes.getTheme(this, mThemeBase.getPackageName(), mThemeBase.getThemeId()) == null) {
            if (new File(new StringBuilder().append(ThemeConstants.THEME_LWT).append("/")
                    .append(mThemeBase.getPkg()).toString()).exists()) {
                Intent intent = new Intent(this, ThemeInstallService.class);
                intent.putExtra("THEME_PACKAGE", new StringBuilder()
                        .append(ThemeConstants.THEME_LWT).append("/")
                        .append(mThemeBase.getPkg()).toString());
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
            i.putExtra(ThemeManager.EXTRA_WALLPAPER_URI, mThemeBean.getWallpaperUri(this));
            i.putExtra(CustomType.EXTRA_NAME, CustomType.DESKTOP_WALLPAPER_TYPE);
            mChangeHelper.beginChange(mThemeBean.getName());
            ApplyThemeHelp.changeTheme(this, i);

            ThemeApplication.sThemeStatus.setAppliedThumbnail(mThemeBean.getWallpaperUri(
                    this), com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
            if (mThemeBean.getWallpaperUri(this) != null) {
                ThemeApplication.sThemeStatus.setAppliedPkgName(
                        null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
            }
        }
    }

    @Override
    protected String getLoadPath(ThemeBase themeBase, Context context) {
        if (mThemeBase.getPkg().endsWith(".lwt")) {
            return ThemeConstants.THEME_PATH;
        } else {
            return ThemeConstants.WALLPAPER_PATH;
        }
    }

    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_wallpaper);
    }

    private static class WallpaperUpdater extends AsyncTask<Void, Void, Void> {
        private static WallpaperUpdater sUpdater;
        private OnLineDeskTopWallpaperPreview mActivity;

        public WallpaperUpdater(OnLineDeskTopWallpaperPreview activity) {
            mActivity = activity;
        }

        static void start(OnLineDeskTopWallpaperPreview activity) {
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
            mActivity.showHome();
        }
    }
}
