package com.lewa.themechooser.custom.main;

import android.app.Fragment;
import android.content.Intent;

import com.lewa.themechooser.custom.CustomBase;
import com.lewa.themechooser.custom.fragment.local.LocalLockScreenWallpaperFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineLockScreenWallpaperFragment;

/**
 * @author xufeng
 */
public class LockScreenWallpaper extends CustomBase {

    @Override
    public Fragment getLocalFragment() {
        return new LocalLockScreenWallpaperFragment();
    }

    @Override
    public Fragment getOnLineFragment() {
        return new OnLineLockScreenWallpaperFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}
