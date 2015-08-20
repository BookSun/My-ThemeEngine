package com.lewa.themechooser.custom.main;

import android.app.Fragment;

import com.lewa.themechooser.custom.CustomBase;
import com.lewa.themechooser.custom.fragment.local.LiveWallpaperFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineLiveWallpaperFragment;

public class LiveWallpaper extends CustomBase {

    @Override
    public Fragment getLocalFragment() {
        return new LiveWallpaperFragment();
    }

    @Override
    public Fragment getOnLineFragment() {
        return new OnLineLiveWallpaperFragment();
    }
}
