package com.lewa.themechooser.custom.preview.local;

import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.appwidget.util.WallpaperUtils;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.preview.slide.local.PreviewIconsActivity;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;

import java.io.InputStream;

public class DeskTopWallpaperPreview extends PreviewIconsActivity {
    private static WallpaperUtils sWallpaperUtils;
    private ProgressDialog dialog;
    private boolean wallpaperChanged = false;
    public static final String TAG = "DeskTopWallpaperPreview";
    public static final String DEFAULT_THEME_PACKAGE = "com.lewa.theme.LewaDefaultTheme";
    public static final String DEFAULT_THEME_UTI = "/system/media/wallpapers/default.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sWallpaperUtils = new WallpaperUtils(this);
    }

    public void showHome(String pkg) {
        //RC48063-jianwu.gao delete begin
        //fix bug : reset wallpaper to default after set font

        /*
            ThemeAppliction.sThemeStatus.setAppliedThumbnail(pkg,
                    com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
            ThemeAppliction.sThemeStatus.setAppliedPkgName(
                    null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
        */

        //RC48063-jianwu.gao delete end

        dialog.cancel();
        Toast.makeText(DeskTopWallpaperPreview.this,
                getString(R.string.theme_change_dialog_title_success), Toast.LENGTH_SHORT).show();

        DeskTopWallpaperPreview.this.finish();

        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(mHomeIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wallpaperChanged = false;
    }

    @Override
    protected void onPause() {
        WallpaperUpdater.cancel();
        super.onPause();
    }

    public void setWallpaper() {
        InputStream is = null;
        try {
            WallpaperManager.getInstance(this)
                    .setStream(sWallpaperUtils.getCalculateStream(mThemeItem != null ?
                            mThemeItem.getWallpaperUri(DeskTopWallpaperPreview.this).getPath() :
                            mThemeUri.getPath()));

            //RC48063-jianwu.gao add begin
            //fix bug : reset wallpaper to default after set font
            ThemeApplication.sThemeStatus.setAppliedThumbnail(mThemeItem != null ? mThemeItem
                    .getWallpaperUri(DeskTopWallpaperPreview.this).getPath() : mThemeUri.getPath()
                    , ThemeStatus.THEME_TYPE_WALLPAPER);
            ThemeApplication.sThemeStatus.setAppliedPkgName(
                    null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
            Settings.System.putString(getContentResolver(), "lewa_wallpaper_path", mThemeItem
                    .getWallpaperUri(DeskTopWallpaperPreview.this).getPath());
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
    }

    @Override
    protected void doApply(ThemeItem bean) {
        wallpaperChanged = true;

        if (bean == null || bean.isImageFile()) {
            dialog = new ProgressDialog(this);
            dialog.setTitle(getString(R.string.theme_change_dialog_title));
            dialog.setMessage(getString(R.string.switching_to_wallpaper, getThemeName()));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            dialog.show();
            WallpaperUpdater.start(this);
            return;
        }
        ThemeItem appliedTheme = Themes.getAppliedTheme(this);
        if (null == appliedTheme) {
            return;
        }
        Uri uri = Themes
                .getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
        appliedTheme.close();
        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
        i.putExtra(ThemeManager.EXTRA_WALLPAPER_URI, bean.getWallpaperUri(this));
        i.putExtra(CustomType.EXTRA_NAME, CustomType.DESKTOP_WALLPAPER_TYPE);

        Settings.System.putString(getContentResolver(), "lewa_wallpaper_path",
                bean.getWallpaperUri(this).getPath().toString());
        ApplyThemeHelp.changeTheme(this, i);
        ThemeApplication.sThemeStatus.setAppliedThumbnail((bean.getWallpaperUri(this).toString())
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
        ThemeApplication.sThemeStatus.setAppliedPkgName(null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
    }

    @Override
    public ImageAdapter initAdapter() {
        if (mThemeItem == null) {
            if (!isThemeUriAvailable(mThemeUri)) {
                return null;
            }
            return new ImageAdapter(this, mThemeUri);
        }
        // Add by Fan.Yang, check url list first, make sure the preview file is exist
        PreviewsType type = mThemeItem.isImageFile() ?
                PreviewsType.DESKWALLPAPER :
                PreviewsType.DEFAULT_THEME_WALLPAPER;
        if (!isThemeItemAvailable(mThemeItem, type)) {
            return null;
        }
        return new ImageAdapter(this, mThemeItem, type);

    }

    @Override
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

    @Override
    protected boolean isThemeApplied() {
        return ThemeApplication.sThemeStatus.isWallpaperApplied(mThemeItem != null ? mThemeItem
                .getWallpaperUri(DeskTopWallpaperPreview.this).getPath() : mThemeUri.getPath()
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
    }

    @Override
    protected String getDeleteToast() {
        return getString(R.string.deskwallpaper_delete_success);
    }

    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_wallpaper);
    }

    @Override
    protected void doDeleteTheme() {
        if (null != mThemeItem) {
            super.doDeleteTheme();
            String SuffixStr = "";
            try {
                SuffixStr = mThemeItem.getWallpaperUri(this).toString().split("\\.")[1];

            } catch (Exception e) {
                Log.d(ThemeConstants.TAG, "e.error=" + e.getMessage());
                e.printStackTrace();
            }
            mThemeStatus.setDeleted(mThemeItem.getPackageName(), mThemeItem.getName(),
                    ThemeStatus.THEME_TYPE_WALLPAPER);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        themeinfo.setVisible(false);
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        //#63893 Add by Fan.Yang
        if (wallpaperChanged) {
            wallpaperChanged = false;
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    private static class WallpaperUpdater extends AsyncTask<Void, Void, Void> {
        private static WallpaperUpdater sUpdater;
        private DeskTopWallpaperPreview mActivity;
        private String mPackageName;

        public WallpaperUpdater(DeskTopWallpaperPreview activity) {
            mActivity = activity;
            mPackageName = mActivity.getThemePackageName();
        }

        static void start(DeskTopWallpaperPreview activity) {
            if (sUpdater != null) {
                sUpdater.cancel(false);
            }
            (sUpdater = new WallpaperUpdater(activity))
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        static void cancel() {
            if (sUpdater != null) {
                sUpdater.cancel(false);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            mActivity.setWallpaper();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mActivity.showHome(mPackageName);
        }
    }
}
