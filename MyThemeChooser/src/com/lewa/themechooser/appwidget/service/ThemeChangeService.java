package com.lewa.themechooser.appwidget.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes.ThemeColumns;

import util.ThemeUtil;

public class ThemeChangeService extends Service {
    private static Context mContext;
    private static ThemeStatus mThemeStatus;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        mThemeStatus = ThemeApplication.sThemeStatus;
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        ThemeController.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static class ThemeController extends AsyncTask<Void, String, Void> {
        private static ThemeController mThemeController;
        private static int lastThemeIndex = -1;

        private ThemeController() {
        }

        public static void start() {
            if (mThemeController != null
                    && mThemeController.getStatus() != AsyncTask.Status.FINISHED) {
                return;
            }
            mThemeController = (ThemeController) new ThemeController()
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ThemeItem themeItem = getThemeItem(mContext);
            if (themeItem != null) {
                changeTheme(themeItem);
                publishProgress("changing");
            } else {
                publishProgress("errorChange");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if ("changing".equals(values[0])) {
                Toast.makeText(mContext,
                        mContext.getResources().getString(R.string.onekey_theme_changing),
                        Toast.LENGTH_SHORT).show();
            } else if ("errorChange".equals(values[0])) {
                Toast.makeText(mContext,
                        mContext.getResources().getString(R.string.onekey_font_no_theme),
                        Toast.LENGTH_SHORT).show();
            }
        }

        private void changeTheme(ThemeItem themeItem) {
            Uri uri = themeItem.getUri(mContext);
            Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
            i.putExtra("onekey_theme", true);
            ThemeUtil.updateCurrentThemeInfo(themeItem);
            if (themeItem.hasThemePackageScope()) {
                i.putExtra(ThemeManager.EXTRA_SYSTEM_APP, true);
            }
            if (themeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                    && themeItem.getThemeId().equals("LewaDefaultTheme")) {
                i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, true);
                i.putExtra(ThemeManager.DEFAULT_FONT, true);
                ThemeUtil.isKillProcess = true;
            }
            ApplyThemeHelp.changeTheme(mContext, i);

            mThemeStatus.setAppliedPkgName(
                    themeItem.getPackageName(), ThemeStatus.THEME_TYPE_PACKAGE);
            if (themeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
                mThemeStatus.setAppliedPkgName(themeItem.getPackageName(),
                        ThemeStatus.THEME_TYPE_ICONS);
                mThemeStatus.setAppliedPkgName(themeItem.getPackageName(),
                        ThemeStatus.THEME_TYPE_FONT);
                mThemeStatus.setAppliedPkgName(themeItem.getPackageName(),
                        ThemeStatus.THEME_TYPE_LOCK_SCREEN);
                mThemeStatus.setAppliedPkgName(themeItem.getPackageName(),
                        ThemeStatus.THEME_TYPE_STYLE);
                mThemeStatus.setAppliedPkgName(themeItem.getPackageName(),
                        ThemeStatus.THEME_TYPE_WALLPAPER);
                mThemeStatus.setAppliedPkgName(themeItem.getPackageName(),
                        ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
            } else {
                if (themeItem.getIconsUri() != null) {
                    mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(
                            mContext, themeItem, PreviewsType.LAUNCHER_ICONS), ThemeStatus.THEME_TYPE_ICONS);
                }
                if (themeItem.getWallpaperUri(mContext) != null) {
                    mThemeStatus.setAppliedThumbnail(themeItem.getWallpaperUri(mContext)
                            , ThemeStatus.THEME_TYPE_WALLPAPER);
                }
                if (themeItem.getFontUril() != null) {
                    mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getThumbnails(
                            mContext, themeItem, PreviewsType.FONTS), ThemeStatus.THEME_TYPE_FONT);
                }
                if (themeItem.getLockscreenUri() != null) {
                    mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getThumbnails(
                            mContext, themeItem, PreviewsType.LOCKSCREEN), ThemeStatus.THEME_TYPE_LOCK_SCREEN);
                    mThemeStatus.setAppliedThumbnail("", ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
                }
                if (themeItem.getLockWallpaperUri(mContext) != null) {
                    mThemeStatus.setAppliedThumbnail("", ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
                }
                if (themeItem.hasThemePackageScope()) {
                    mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getThumbnails(
                            mContext, themeItem, PreviewsType.FRAMEWORK_APPS), ThemeStatus.THEME_TYPE_STYLE);
                }
            }
        }

        private ThemeItem getThemeItem(Context context) {
            Cursor cursor = context
                    .getContentResolver()
                    .query(ThemeColumns.CONTENT_PLURAL_URI,
                            null,
                            "((case when wallpaper_uri is null then 0 else 1 end) +(case when lock_wallpaper_uri is null then 0 else 1 end) +(case when font_uri is null then 0 else 1 end)+(case when lockscreen_uri is null then 0 else 1 end) +(case when boot_animation_uri is null then 0 else 1 end) +(case when icons_uri is null then 0 else 1 end) ) >= 2",
                            null, ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            if (!cursor.moveToNext()) {
                return null;
            }
            ThemeItem daoItem = new ThemeItem(cursor);
            int themeCount = daoItem.getCount();
            if (themeCount <= 1) {
                return null;
            }
            daoItem.setPosition(getRandomThemeIndex(themeCount));
            return daoItem;
        }

        private int getRandomThemeIndex(int themeCount) {
            int randomIndex = (int) (Math.random() * themeCount);
            if (randomIndex == lastThemeIndex) {
                return getRandomThemeIndex(themeCount);
            }
            lastThemeIndex = randomIndex;
            return randomIndex;
        }
    }
}
