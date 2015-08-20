package com.lewa.themechooser.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.lewa.themes.ThemeManager;

import util.ThemeUtil.UpdateThemeInfoThread;

public class ThemeFileReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.lewa.intent.action.THEME_FILE_ADDED".equals(action) || "com.lewa.intent.action.THEME_FILE_CHANGED".equals(action)) {
//            context.startService(
//                    (new Intent(context, ThemeInstallService.class)).setData(intent.getData())
//                    .putExtra("THEME_PACKAGE", intent.getStringExtra("THEME_PACKAGE")));
        } else if (ThemeManager.ACTION_THEME_DOWNLOADED.equals(action)) {
            Uri uri = intent.getData();
            UpdateThemeInfoThread.start(context, uri == null ? null : uri.getEncodedSchemeSpecificPart(), false);
        }
    }
}
