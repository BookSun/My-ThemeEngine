package com.lewa.themechooser.custom.main;

import android.app.Fragment;

import com.lewa.themechooser.custom.CustomBase;
import com.lewa.themechooser.custom.fragment.local.DeskTopWallpaperFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineDeskTopWallpaperFragment;

public class DeskTopWallpaper extends CustomBase {

    @Override
    public Fragment getLocalFragment() {
        return new DeskTopWallpaperFragment();
    }

    @Override
    public Fragment getOnLineFragment() {
        return new OnLineDeskTopWallpaperFragment();
    }
}
