package com.lewa.themechooser.appwidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.lewa.themechooser.R;
import com.lewa.themechooser.appwidget.util.WallpaperUtils;

public class OnekeyWallpaperShortcut extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String aciton = getIntent().getAction();
        if (Intent.ACTION_CREATE_SHORTCUT.equals(aciton)) {
            generateShortcut();
        } else {
            WallpaperUtils.startWallpaperService(getApplicationContext());
        }
        finish();
    }

    private void generateShortcut() {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                getResources().getString(R.string.onekey_wallpaper));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,
                R.drawable.onekey_wallpaper_normal);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        Intent shortcutIntent = new Intent(this, OnekeyWallpaperShortcut.class);

        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        setResult(RESULT_OK, intent);
    }
}
