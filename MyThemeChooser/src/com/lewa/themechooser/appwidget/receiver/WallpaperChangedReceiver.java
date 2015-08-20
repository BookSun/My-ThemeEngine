package com.lewa.themechooser.appwidget.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lewa.themechooser.appwidget.CoveringPageActivity;
import com.lewa.themechooser.appwidget.util.CommonUtils;
import com.lewa.themechooser.appwidget.util.WallpaperUtils;

public class WallpaperChangedReceiver extends BroadcastReceiver {
    private static final long TIMELIMITS = 1200;

    @Override
    public void onReceive(Context context, Intent intent) {
        CommonUtils commonUtils = new CommonUtils(context);
        int density = commonUtils.getDensity();
        if (density >= 480 && CommonUtils.isCoveringPageOn(context)) {
            finishCoveringPage(context);
            return;
        }
        WallpaperUtils.isChanging = false;
    }

    private void finishCoveringPage(final Context context) {
        new Thread() {
            public void run() {
                long timePast = System.currentTimeMillis() - WallpaperUtils.startTime;
                if (timePast < TIMELIMITS) {
                    try {
                        Thread.sleep(TIMELIMITS - timePast);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                context.sendBroadcast(new Intent(
                        CoveringPageActivity.ACTION_FINISH_COERINGPAGE));
                return;
            }

            ;
        }.start();
    }
}
