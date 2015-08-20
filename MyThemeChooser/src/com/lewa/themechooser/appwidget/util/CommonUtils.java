package com.lewa.themechooser.appwidget.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.lewa.themechooser.appwidget.CoveringPageActivity;

import java.util.List;

public class CommonUtils {
    public static final String ACTION_ONEKEY_WALLPAPER = "com.lewa.themechooser.OnekeyWallpaper";
    public static final String ACTION_ONEKEY_FONT = "com.lewa.themechooser.OnekeyFont";
    public static final String ACTION_ONEKEY_THEME = "com.lewa.themechooser.OnekeyTheme";
    private Display display;
    private DisplayMetrics outMetrics;
    private int density;

    public CommonUtils(Context context) {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = outMetrics.densityDpi;
    }

    public static boolean isWifiOn(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI
                && activeNetInfo.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

    public static boolean isSdCardMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static boolean isCoveringPageOn(Context context) {
        List<RunningTaskInfo> runningTasks = ((ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(50);
        for (RunningTaskInfo taskInfo : runningTasks) {
            if (taskInfo.topActivity.getClassName().equals(CoveringPageActivity.class.getName())) {
                return true;
            }
        }
        return false;
    }

    public Point getScreenSize() {
        Point outSize = new Point();
        display.getSize(outSize);
        return outSize;
    }

    public int getDensity() {
        return density;
    }

    public String getDpi() {
        String densityDpi = null;
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
                densityDpi = "ldpi";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                densityDpi = "mdpi";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                densityDpi = "hdpi";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                densityDpi = "xdpi";
                break;
            case 480/* DisplayMetrics.DENSITY_XXHIGH */:
                densityDpi = "xxdpi";
                break;
            default:
                densityDpi = "xdpi";
                break;
        }
        return densityDpi;
    }

}
