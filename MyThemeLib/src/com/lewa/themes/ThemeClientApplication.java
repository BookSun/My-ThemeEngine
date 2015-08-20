package com.lewa.themes;

import android.app.Application;

/**
 * Created by ivonhoe on 15-3-16.
 */
public class ThemeClientApplication extends Application {

    ThemeManager mThemeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mThemeManager = ThemeManager.getInstance(getApplicationContext(), null);
        mThemeManager.notifiedOnThemeChanged(true);
        mThemeManager.notifiedOnWallpaperChanged(true);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mThemeManager.unBindService();
    }
}
