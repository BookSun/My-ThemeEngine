package com.lewa.themechooser.appwidget.service;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.appwidget.util.WallpaperUtils;
import com.lewa.themes.ThemeManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class WallpaperChangeService extends Service {
    private static final String TAG = "WallpaperChangeService";
    private static Context mContext;
    private static WallpaperUtils sWallpaperUtils;
    private static WallpaperManager sWallpaperManager;

    private static void changeWallpaper() {
        String wallpaperPath = sWallpaperUtils.getWallpaperPath();
        InputStream is = null;
        if (wallpaperPath != null) {
            //sWallpaperUtils.showCoveringPage(mContext);
            try {
                is = sWallpaperUtils.getCalculateStream(wallpaperPath);
                sWallpaperManager.setStream(is);
                String wallpaperPackageName = sWallpaperUtils
                        .getWallpaperPackageName(wallpaperPath);
                ThemeApplication.sThemeStatus.setAppliedPkgName(wallpaperPackageName,
                        ThemeStatus.THEME_TYPE_WALLPAPER);
                ThemeApplication.sThemeStatus.setWallpaperIsUsingFlag(wallpaperPackageName);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException：" + e.toString(), e);
                try {
                    sWallpaperManager.clear();
                } catch (IOException e1) {
                    Log.e(TAG, "IOException：" + e.toString(), e);
                }
                WallpaperController.getInstance().doCancel();
            } catch (IOException e) {
                Log.e(TAG, "IOException：" + e.toString(), e);
                try {
                    sWallpaperManager.clear();
                } catch (IOException e1) {
                    Log.e(TAG, "IOException：" + e.toString(), e);
                }
                WallpaperController.getInstance().doCancel();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "IOException：" + e.toString(), e);
                        WallpaperController.getInstance().doCancel();
                    }
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initParams();
    }

    private void initParams() {
        mContext = getApplicationContext();
        sWallpaperUtils = new WallpaperUtils(mContext);
        sWallpaperManager = WallpaperManager.getInstance(mContext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        WallpaperController.start();
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static class WallpaperController extends AsyncTask<Void, Object, Void> {
        private static WallpaperController controller;
        private static long startTime = System.currentTimeMillis();

        private WallpaperController() {
        }

        public static void start() {
            if (controller != null && controller.getStatus() != AsyncTask.Status.FINISHED) {
                return;
            }
            if (WallpaperUtils.isChanging
                    && ((System.currentTimeMillis() - startTime) < WallpaperUtils.WALLPAPER_CHANGING_TIMEOUT)) {
                return;
            }
            startTime = System.currentTimeMillis();
            WallpaperUtils.startTime = startTime;
            WallpaperUtils.isChanging = true;
            controller = (WallpaperController) new WallpaperController()
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        public static WallpaperController getInstance() {
            return controller;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {
                changeWallpaper();
                cancel(true);
            }
            return null;
        }

        public boolean doCancel() {
            if (controller != null) {
                return cancel(true);
            }
            return false;
        }
    }
}
