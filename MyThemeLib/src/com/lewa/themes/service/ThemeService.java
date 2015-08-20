package com.lewa.themes.service;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.resource.BlurWallpaperUtils;
import com.lewa.themes.resource.IconCustomizer;
import com.lewa.themes.resource.ThemeResources;

import java.util.*;

import lewa.graphics.drawable.BitmapBlurDrawable;
import lewa.graphics.drawable.BlurOptions;
import lewa.os.Shell;
import lewa.util.ImageUtils;

/**
 * Created by ivonhoe on 15-3-6.
 */
public class ThemeService extends Service {

    private static final boolean DEBUG = true;
    private static final String TAG = "ThemeService";
    private static final String SETTINGS_APP_ICON_SCALE = "app_icon_scale";

    private float mIconScale;

    private Context mContext;

    private WallpaperChangedReceiver mWallpaperReceiver;

    private WallpaperManager mWallpaperManager;

    private Bitmap blurredWallpaper;

    private RemoteCallbackList<IThemeServiceCallback> mCallbacks = new ThemeClientCallbacks<IThemeServiceCallback>();

    private HashMap<String, IThemeServiceCallback> mCallbacksMap = new HashMap<String, IThemeServiceCallback>();

    private static final int MSG_NOTIFY_WALLPAPER_CHANGED = 0;

    private AppIconTask mAppIconTask;

    private final IThemeService.Stub mThemeBinder = new IThemeService.Stub() {

        @Override
        public void registerCallback(String packageName, IThemeServiceCallback callback) {
            if (callback != null && packageName != null) {
                mCallbacks.register(callback);
                try {
                    getCurrentBlurredWallpaper();
                    callback.onWallpaperChanged(blurredWallpaper);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                synchronized (mCallbacksMap) {
                    IThemeServiceCallback old = mCallbacksMap.put(packageName, callback);
                    if (old != null) {
                        mCallbacks.unregister(old);
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "register callback, packageName:" + packageName + ",callback:" +
                            callback);
                }
            }
        }

        @Override
        public void unregisterCallback(String packageName, IThemeServiceCallback callback) {
            if (callback != null && packageName != null) {
                mCallbacks.unregister(callback);
                synchronized (mCallbacksMap) {
                    mCallbacksMap.remove(packageName);
                }
                if (DEBUG) {
                    Log.d(TAG, "unregister callback, packageName:" + packageName);
                }
            }
        }

        @Override
        public Bitmap loadIconByResolveInfo(ResolveInfo ri) {
            BitmapDrawable d = IconCustomizer.getCustomizedIcon(mContext, ri);
            return d == null ? null : d.getBitmap();
        }

        @Override
        public Bitmap loadIconByApplicationInfo(ApplicationInfo ai) {
            BitmapDrawable d = IconCustomizer.getCustomizedIcon(mContext, ai);
            return d == null ? null : d.getBitmap();
        }

        @Override
        public void clearCustomizedIcons(String packageName) {
            IconCustomizer.clearCustomizedIcons(packageName);
        }

        @Override
        public void reset() {
            ThemeResources.reset();
        }

        @Override
        public void checkModIcons() {
            IconCustomizer.checkModIcons();
        }

        @Override
        public String getFancyIconRelativePath(String packageName, String className) {
            return IconCustomizer.getFancyIconRelativePath(packageName, className);
        }

        // unfinished interface
        @Override
        public String getThemePackageName(String propertyName) {
            if (propertyName != null && propertyName.equals("lockscreen")) {
                return "com.lewa.lockscreen";
            }
            return "com.lewa.lockscreen";
        }

        @Override
        public Bitmap getBlurredWallpaper() {
            return BlurWallpaperUtils.loadBlurWallpaper(mContext);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            Log.d(TAG, "Theme service onCreate");
        }

        mContext = getApplicationContext();
        mWallpaperManager = WallpaperManager.getInstance(mContext);
        mWallpaperReceiver = new WallpaperChangedReceiver();
        float iconScale = Settings.System.getFloat(getContentResolver(), SETTINGS_APP_ICON_SCALE,
                1.0f);
        mIconScale = iconScale;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(mWallpaperReceiver, filter);
        registerIconScaleSetting(mContext);
        IconCustomizer.checkModIcons();

        mAppIconTask = new AppIconTask(mContext);
        mAppIconTask.execute();
        // 如果ThemeService进程被kill掉，那么下次启动后必须准备好上次的断点，处理好模糊壁纸
        mHandler.sendEmptyMessage(MSG_NOTIFY_WALLPAPER_CHANGED);
        // tell clients, OK，I am back, reconnect me, register the callback and redraw the blur wallpaper
        sendBroadcast(new Intent(ThemeManager.ACTION_SERVICE_CREATED));
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (DEBUG) {
            Log.d(TAG, "Theme service onStart");
        }
        if (intent != null) {
            boolean checkMod = intent.getBooleanExtra(ThemeManager.EXTRA_ICON_CHANGED, false);
            if (checkMod) {
                IconCustomizer.clearCustomizedIcons(null);
                ThemeResources.reset();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            Log.d(TAG, "Theme service onStartCommand");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "Theme service onBind");
        }
        return mThemeBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "Theme service onUnbind,intent:" + intent);
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Log.d(TAG, "Theme service onDestroy");
        }
        mWallpaperManager = null;
        mCallbacks.kill();
        unregisterReceiver(mWallpaperReceiver);
        if (mAppIconTask != null) {
            mAppIconTask.cancel(true);
        }
    }

    private void notifyClientOnThemeChanged() {
        try {
            Set<Map.Entry<String, IThemeServiceCallback>> sets = mCallbacksMap.entrySet();
            for (Map.Entry<String, IThemeServiceCallback> entry : sets) {
                IThemeServiceCallback callback = (IThemeServiceCallback) entry.getValue();
                callback.onThemeChanged();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    class IconScaleObserver extends ContentObserver {
        private Handler mHandler;

        public IconScaleObserver(Handler handler) {
            super(handler);
            mHandler = handler;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    float iconScale = Settings.System
                            .getFloat(mContext.getContentResolver(), SETTINGS_APP_ICON_SCALE, 1.0f);
                    if (mIconScale != iconScale) {
                        ThemeResources.reset();
                        IconCustomizer.setIconScale(iconScale);
                        mIconScale = iconScale;
                    }
                }
            };
            mHandler.post(r);
        }
    }

    private void registerIconScaleSetting(Context context) {
        float iconScale = Settings.System
                .getFloat(mContext.getContentResolver(), SETTINGS_APP_ICON_SCALE, 1.0f);
        IconCustomizer.setIconScale(iconScale);
        IconScaleObserver observer = new IconScaleObserver(new Handler());
        context.getContentResolver().registerContentObserver(
                android.provider.Settings.System.getUriFor(SETTINGS_APP_ICON_SCALE), false,
                observer);
    }

    private long debugTime;

    class WallpaperChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {
                debugTime = System.currentTimeMillis();
                Log.d(TAG, "Theme service rebuild blur wallpaper on wallpaper changed BEGIN.");
            }
            mHandler.sendEmptyMessage(MSG_NOTIFY_WALLPAPER_CHANGED);
            if (DEBUG) {
                Log.d(TAG, "Theme service notify client wallpaper changed DONE, cost time:" +
                        (System.currentTimeMillis() - debugTime));
            }
        }
    }

    class ThemeClientCallbacks<E extends IInterface> extends RemoteCallbackList {
        @Override
        public void onCallbackDied(IInterface callback) {
            super.onCallbackDied(callback);
            removeCallback(callback);
        }

        @Override
        public void onCallbackDied(IInterface callback, Object cookie) {
            super.onCallbackDied(callback, cookie);
            removeCallback(callback);
        }

        @Override
        public void kill() {
            synchronized (mCallbacksMap) {
                mCallbacksMap.clear();
            }
            super.kill();
        }

        private void removeCallback(IInterface callback) {
            synchronized (mCallbacksMap) {
                IThemeServiceCallback caller = (IThemeServiceCallback) callback;
                Set<Map.Entry<String, IThemeServiceCallback>> sets = mCallbacksMap.entrySet();
                for (Map.Entry<String, IThemeServiceCallback> entry : sets) {

                    String key = (String) entry.getKey();
                    IThemeServiceCallback value = (IThemeServiceCallback) entry.getValue();
                    if (caller != null && caller == value) {
                        mCallbacksMap.remove(key);
                        return;
                    }
                }
            }
        }
    }

    private synchronized Bitmap getCurrentBlurredWallpaper() {
        if (blurredWallpaper == null) {
            Bitmap[] bitmaps = new Bitmap[1];
            BlurWallpaperUtils.getBlurredWallpaper(mContext, bitmaps);
            if (bitmaps[0] != null) {
                blurredWallpaper = bitmaps[0];
            }
        }
        return blurredWallpaper;
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_NOTIFY_WALLPAPER_CHANGED) {
                blurredWallpaper = null;
                getCurrentBlurredWallpaper();
                notifyClientOnWallpaperChanged();
            }
        }
    };

    private void notifyClientOnWallpaperChanged() {
        try {
            synchronized (mCallbacksMap) {
                try {
                    final int N = mCallbacks.beginBroadcast();
                    Set<Map.Entry<String, IThemeServiceCallback>> sets = mCallbacksMap.entrySet();
                    for (Map.Entry<String, IThemeServiceCallback> entry : sets) {

                        IThemeServiceCallback callback = (IThemeServiceCallback) entry.getValue();
                        callback.onWallpaperChanged(blurredWallpaper);
                    }
                } finally {
                    mCallbacks.finishBroadcast();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    static class AppIconTask extends AsyncTask<Void, Void, Void> {

        private Context context;
        private List<ApplicationInfo> appInfoList;
        private List<ResolveInfo> resolveInfoList;

        AppIconTask(Context context) {
            this.context = context;
            appInfoList = ThemeManager.queryApplicationInfo(context);
            resolveInfoList = ThemeManager.queryResolveInfo(context);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (appInfoList == null || resolveInfoList == null || context == null) {
                return null;
            }
            for (ApplicationInfo info : appInfoList) {
                //Log.d(TAG, "%%%%%%%% AppIconTask load icon at background, app info");
                IconCustomizer.getCustomizedIcon(context, info);
            }
            for (ResolveInfo info : resolveInfoList) {
                //Log.d(TAG, "%%%%%%%% AppIconTask load icon at background, resolve info");
                IconCustomizer.getCustomizedIcon(context, info);
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            context = null;
        }
    }
}
