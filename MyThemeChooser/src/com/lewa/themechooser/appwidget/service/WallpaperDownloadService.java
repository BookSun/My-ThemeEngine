package com.lewa.themechooser.appwidget.service;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.appwidget.util.CommonUtils;
import com.lewa.themechooser.appwidget.util.Wallpaper;
import com.lewa.themechooser.appwidget.util.WallpaperUtils;
import com.lewa.themes.ThemeManager;

import org.apache.http.client.ClientProtocolException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class WallpaperDownloadService extends Service {
    private static final boolean DEBUG = false;
    private static final String TAG = "WallpaperDownloadService";
    public static int achievedDownloadedAmount;
    public static int currentDownloadIndex = 0;
    public static boolean isWifiOn;
    public static boolean isSdCardMounted;
    public static boolean downloadCompleted;
    private static Context mContext;
    private static ContextThemeWrapper mThemeWrapper;
    private static WallpaperUtils sWallpaperUtils;
    private static WallpaperManager sWallpaperManager;
    private static ArrayList<Wallpaper> sWallpaperList = new ArrayList<Wallpaper>();
    private NetworkStateReceiver mNetworkStateReceiver;
    private SdCardStateReceiver mSdCardStateReceiver;

    public static void startDownload() {
        if (sWallpaperList.isEmpty() || currentDownloadIndex >= sWallpaperList.size()) {
            getWallpaperList();
        }
        downloadCompleted = false;
        if (!sWallpaperList.isEmpty() && currentDownloadIndex < sWallpaperList.size()) {
            Wallpaper wallpaper = sWallpaperList.get(currentDownloadIndex++);
            if (sWallpaperUtils.isWallpaperDownloaded(wallpaper)) {
                if (DEBUG)
                    Log.i(TAG, "Image：" + (currentDownloadIndex - 1) + " already downloaded!");
                if (currentDownloadIndex < sWallpaperList.size()) {
                    startDownload();
                } else {
                    getWallpaperList();
                }
            } else {
                downloadWallpaper(wallpaper);
            }
        }
        DownloadController.getInstance().doCancel();
    }

    public static void getWallpaperList() {
        if (DEBUG)
            Log.i(TAG, "Requesting uri list!");
        currentDownloadIndex = 0;
        sWallpaperList.clear();
        try {
            sWallpaperList = sWallpaperUtils.getWallpaperList(sWallpaperUtils.getJsonArrayUri());
        } catch (ClientProtocolException e) {
            Log.e(TAG, "ClientProtocolException：" + e.toString(), e);
            DownloadController.getInstance().doCancel();
            return;
        } catch (IOException e) {
            Log.e(TAG, "IOException：" + e.toString(), e);
            DownloadController.getInstance().doCancel();
            return;
        }

        if (sWallpaperList.size() > 0) {
            if (DEBUG)
                Log.i(TAG, "Uri list acquired!");
        }
    }

    public static void downloadWallpaper(Wallpaper wallpaper) {
        try {
            sWallpaperUtils.downloadWallpaper(mContext, wallpaper);
        } catch (ClientProtocolException e) {
            ThemeApplication.sThemeStatus.setDownloadingCancelled(WallpaperUtils.currentID);
            sWallpaperUtils.deleteWallpaper(wallpaper);
            currentDownloadIndex--;
            Log.e(TAG, "ClientProtocolException：" + e.toString(), e);
            DownloadController.getInstance().doCancel();
        } catch (IOException e) {
            ThemeApplication.sThemeStatus.setDownloadingCancelled(WallpaperUtils.currentID);
            sWallpaperUtils.deleteWallpaper(wallpaper);
            currentDownloadIndex--;
            Log.e(TAG, "IOException：" + e.toString(), e);
            DownloadController.getInstance().doCancel();
        }
    }

    private static void changeWallpaper() {
        String wallpaperPath = sWallpaperUtils.getWallpaperPath();
        InputStream is = null;
        if (wallpaperPath != null) {
            //sWallpaperUtils.showCoveringPage(mContext);
            try {
                is = sWallpaperUtils.getCalculateStream(wallpaperPath);
                sWallpaperManager.setStream(is);
                Settings.System.putString(mContext.getContentResolver(),"lewa_wallpaper_path", wallpaperPath);
                String wallpaperPackageName = sWallpaperUtils
                        .getWallpaperPackageName(wallpaperPath);
                ThemeApplication.sThemeStatus.setAppliedPkgName(wallpaperPackageName,
                        ThemeStatus.THEME_TYPE_WALLPAPER);
                ThemeApplication.sThemeStatus.setWallpaperIsUsingFlag(wallpaperPackageName);
                WallpaperController.getInstance().doPublishProgress(
                        mContext.getResources().getString(R.string.theme_change_dialog_title_success),
                        Toast.LENGTH_SHORT);
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
        } else if (!isWifiOn) {
            WallpaperController.getInstance().doPublishProgress(
                    mContext.getResources().getString(R.string.wifi_off_no_wallpaper),
                    Toast.LENGTH_LONG);
            return;
        } else if (!isSdCardMounted) {
            WallpaperController.getInstance().doPublishProgress(
                    mContext.getResources().getString(R.string.sdcard_unmounted_no_wallpaper),
                    Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initParams();
        registerReceiver();
    }

    private void initParams() {
        mContext = getApplicationContext();
        mThemeWrapper = new ContextThemeWrapper(mContext, R.style.Theme);
        if (CommonUtils.isSdCardMounted()) {
            isSdCardMounted = true;
        } else {
            Log.d(TAG, "SD card abnormal!");
        }
        if (CommonUtils.isWifiOn(mContext)) {
            isWifiOn = true;
        } else {
            Log.d(TAG, "Wifi disconnected!");
        }
        sWallpaperUtils = new WallpaperUtils(mContext);
        sWallpaperManager = WallpaperManager.getInstance(mContext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        if (isSdCardMounted && isWifiOn
                && achievedDownloadedAmount <= WallpaperUtils.EXPECTED_DOWNLOADED_AMOUNT) {
            if (DEBUG)
                Log.i(TAG, "currentDownloadIndex：" + currentDownloadIndex + "  isWriting："
                        + WallpaperUtils.isWriting);
            DownloadController.start();
        }
        WallpaperController.start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerReceiver() {
        registerSdCardStateReceiver();
        registerNetworkStateReceiver();
    }

    private void registerNetworkStateReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        mNetworkStateReceiver = new NetworkStateReceiver();
        mContext.registerReceiver(mNetworkStateReceiver, intentFilter);
    }

    private void registerSdCardStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_REMOVED");
        intentFilter.addAction("android.intent.action.ACTION_MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.ACTION_MEDIA_BAD_REMOVAL");
        intentFilter.addAction("android.intent.action.MEDIA_SHARED");
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addDataScheme("file");
        mSdCardStateReceiver = new SdCardStateReceiver();
        mContext.registerReceiver(mSdCardStateReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mNetworkStateReceiver);
        mContext.unregisterReceiver(mSdCardStateReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static class DownloadController extends AsyncTask<Void, String, Void> {
        private static DownloadController controller;

        private DownloadController() {
        }

        public static void start() {
            if (controller != null && controller.getStatus() != Status.FINISHED) {
                return;
            }
            controller = (DownloadController) new DownloadController()
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        public static DownloadController getInstance() {
            return controller;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {
                startDownload();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(mThemeWrapper, values[0], Toast.LENGTH_SHORT).show();
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled() {
            if (downloadCompleted) {
                ThemeApplication.sThemeStatus.setDownloaded(WallpaperUtils.currentID);
            }
            super.onCancelled();
        }

        public boolean doCancel() {
            if (controller != null) {
                return cancel(true);
            }
            return false;
        }

        public void doPublishProgress(String values) {
            if (controller != null) {
                publishProgress(values);
            }
        }

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
            if ((System.currentTimeMillis() - startTime) < WallpaperUtils.WALLPAPER_CHANGING_TIMEOUT) {
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

        @Override
        protected void onProgressUpdate(Object... values) {
            Toast.makeText(mThemeWrapper, (String) values[0], (Integer) values[1]).show();
            super.onProgressUpdate(values);
        }

        public boolean doCancel() {
            if (controller != null) {
                return cancel(true);
            }
            return false;
        }

        public void doPublishProgress(Object... values) {
            if (controller != null) {
                publishProgress(values);
            }
        }
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        private static final String TAG = "NetworkStateReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (DEBUG)
                    Log.d(TAG, "Wifi connected!");
                WallpaperDownloadService.isWifiOn = true;
            } else {
                if (DEBUG)
                    Log.d(TAG, "Wifi disconnected!");
                WallpaperDownloadService.isWifiOn = false;
            }
        }
    }

    private class SdCardStateReceiver extends BroadcastReceiver {
        private static final String TAG = "SdCardStateReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG)
                Log.i(TAG, action);
            if (action.equals("android.intent.action.MEDIA_MOUNTED")) {
                if (DEBUG)
                    Log.d(TAG, "SD mounted!");
                WallpaperDownloadService.isSdCardMounted = true;
            } else {
                if (DEBUG)
                    Log.d(TAG, "SD card abnormal!");
                WallpaperDownloadService.isSdCardMounted = false;
            }
        }
    }
}
