package com.lewa.thememanager.receiver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.lewa.thememanager.R;
import com.lewa.thememanager.utils.WallpaperUtilities;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.Themes.ThemeColumns;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.WallpaperManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import lewa.os.FileUtilities;

public class KillProcessReceiver extends BroadcastReceiver{
    private static final boolean DEBUG = false ;
    private static final String TAG="KillProcessReceiver";
    private static final int SYSTEM_UI_RETRY = 50;
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String[] FORCE_STOP_PACKAGES = new String[]{
        "com.android.browser"
    };
    private static final String NETMGR_PACKAGES = "com.lewa.netmgr" ;
    private static final String PLAYER_PACKAGES = "com.lewa.player";
    private static final String MEDIA_PACKAGES = "android.process.media" ;
    @Override
    public void onReceive(final Context context, Intent intent) {
        final Uri wallpaperUri=(Uri)intent.getParcelableExtra("wallpaperUri");
        (new Thread(){
            public void run() {
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        || context.getResources().getBoolean(R.bool.config_change_english_font)){
                    IActivityManager iam = ActivityManagerNative.getDefault();
                    final String pkg = context.getPackageName();
                    try {
                        List<RunningAppProcessInfo> infos = iam.getRunningAppProcesses();
                        String wallpaperPkg=null;
                        if(WallpaperManager.getInstance(context).getWallpaperInfo()!=null){
                            wallpaperPkg=WallpaperManager.getInstance(context).getWallpaperInfo().getPackageName();
                        }
                        int suid = 1000;
                        String chooser = "com.lewa.themechooser";

                        for (RunningAppProcessInfo info : infos) {
                                String processName = info.processName;
                                int uid = info.uid;
                                if(processName.equals(SYSTEM_UI_PACKAGE))
                                    suid = uid;
                            if ((uid >= 10000 && (!processName.equals(pkg) &&
                                    !processName.equals(wallpaperPkg) &&
                                    !processName.equals(chooser)) &&
                                    !MEDIA_PACKAGES.equals(processName) &&
                                    !PLAYER_PACKAGES.equals(processName))
                                    || processName.equals("com.android.settings") ||
                                    NETMGR_PACKAGES.equals(processName)) {
                                try {
                                    Log.d(TAG, "uid=" + uid + ",processName=" + processName);
                                    iam.killApplicationProcess(processName, uid);
                                } catch (Exception e) {
                                    Log.d(TAG, "cannot kill " + e.getMessage());
                                }
                            }
                        }
                        //fix bug #58066 杀掉systemui连带会清理掉theme wallpaper|| processName.equals("com.android.systemui")
//                        int retry = 0;
//                        while(retry < SYSTEM_UI_RETRY){
//                            if(!isSystemUIRunning(context))
//                                break;
//                            iam.killApplicationProcess(SYSTEM_UI_PACKAGE, suid);
//                            Thread.sleep(200);
//                            retry++;
//                        }
                        Intent wallpaper = new Intent();
                        wallpaper.setClassName(SYSTEM_UI_PACKAGE, SYSTEM_UI_PACKAGE + ".ImageWallpaper");
                        context.startService(wallpaper);

                        Intent systemui = new Intent();
                        systemui.setClassName(SYSTEM_UI_PACKAGE, SYSTEM_UI_PACKAGE + ".SystemUIService");
                        context.startService(systemui);

                    } catch (Exception e) {
                        Log.e(TAG, "kill process error" + e.toString());
                    }
                } else {
                    ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
                    for(String pkg : FORCE_STOP_PACKAGES) {
                        try {
                            am.forceStopPackage(pkg);
                        } catch (Exception e) {
                            Log.e(TAG, "kill process error" + e.toString());
                        }
                    }
                }
                setWallpaper(context,wallpaperUri);
                killProcessFinish(context);
            }
        }).start();
    }

    public static void killProcessFinish(Context context){
        Intent intent=new Intent(ThemeManager.ACTION_KILL_PROCESS_FINISH);
        intent.setType(ThemeColumns.CONTENT_ITEM_TYPE);
        context.sendBroadcast(intent);
    }

    private static void setWallpaper(Context context, Uri uri) {
        if(DEBUG)
            Log.e("JYL", "setWallpaperStream--");
        if(uri == null || uri.toString().isEmpty())
            return ;
        int counts = 0;
        int MAX_COUNTS = 10 ;
        long SLEEEP_TIME = 2000 ;
        while(counts < MAX_COUNTS){
            SystemClock.sleep(SLEEEP_TIME);
            if(isSystemUIRunning(context)) {
                setWallpaperStream(context, uri);
                break;
            }
            counts++;
        }
    }
    public static void setWallpaperStream(Context context, Uri uri) {
        if(DEBUG) {
            Log.e("JYL", "setWallpaperStream--:context == null :"+(context == null)+" uri:"+uri);
        }
        Long start  = SystemClock.currentThreadTimeMillis() ;
        InputStream in = null;

        //RC48063-jianwu.gao modify begin
        //fix bug : reset wallpaper to default after set font
        Uri wallpaperUri;
        try {
           wallpaperUri = PackageResources.convertFilePathUri(uri);
           in = context.getContentResolver().openInputStream(wallpaperUri);
           if(in != null ) {
               WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
               wallpaperManager.setStream(in);
               if(DEBUG)
                   Log.e("JYL", " wallpaperManager.setStream  time:"+(SystemClock.currentThreadTimeMillis()  - start));
               //RC48063-jianwu.gao modify end
           }
        } catch (Exception e) {
            Log.e("JYL", "Could not set wallpaper", e);
        } finally {
            if (in != null) {
                FileUtilities.close(in);
            }
        }
    }
    public static boolean isSystemUIRunning(Context context) {
        IActivityManager iam = ActivityManagerNative.getDefault();
        try {
            List<RunningAppProcessInfo> infos = iam.getRunningAppProcesses();
            for (RunningAppProcessInfo service : infos) {
                if (SYSTEM_UI_PACKAGE.equals(service.processName)) {
                    return true;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "fetch process info error" + e.toString());
        }
        return false;
    }
}
