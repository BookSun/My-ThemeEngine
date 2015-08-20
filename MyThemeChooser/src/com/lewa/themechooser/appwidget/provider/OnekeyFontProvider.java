package com.lewa.themechooser.appwidget.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.lewa.themechooser.R;
import com.lewa.themechooser.appwidget.util.CommonUtils;

public class OnekeyFontProvider extends AppWidgetProvider {
    private static final String TAG = "OnekeyFontProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.onekey_font_remoteview);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                CommonUtils.ACTION_ONEKEY_FONT), 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_icon, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
}
