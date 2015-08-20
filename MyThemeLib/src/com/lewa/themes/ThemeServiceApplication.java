package com.lewa.themes;

import android.app.Application;
import android.content.Intent;

/**
 * Created by ivonhoe on 15-4-15.
 */
public class ThemeServiceApplication extends Application {

    ThemeManager mThemeManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mThemeManager = ThemeManager.getInstance(getApplicationContext());
        mThemeManager.notifiedOnThemeChanged(false);
        mThemeManager.notifiedOnWallpaperChanged(false);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mThemeManager.unBindService();
    }
}
