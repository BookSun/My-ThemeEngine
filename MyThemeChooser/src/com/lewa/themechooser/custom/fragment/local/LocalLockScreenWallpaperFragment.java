package com.lewa.themechooser.custom.fragment.local;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.custom.preview.local.LockScreenWallpaperPreview;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;

public class LocalLockScreenWallpaperFragment extends LocalBase {
    @Override
    protected int getType() {
        return LOCKSCREEN_WALLPAPER_TYPE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayoutRes = R.layout.theme_local_screen_wallpaper_main;
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
////          intent.putExtra("scale", true);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
//            intent.putExtra("crop", "true");
//            intent.putExtra("outputX", 425);
//            intent.putExtra("outputY", 265);
//            intent.putExtra("aspectX", ThemeUtil.screen_width);
//            intent.putExtra("aspectY", ThemeUtil.screen_height);
//            intent.putExtra("noFaceDetection", true);
//            intent.putExtra("scaleUpIfNeeded", true);
//            intent.putExtra("setWallpaper", false);
//            intent.putExtra("scale", true);
//            intent.putExtra("return-data", false);
        startActivityForResult(intent, SELECT_PICTRUE_FOR_LOCKSCREEN);
        return true;
    }

    @Override
    public void startActivity(View v) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), LockScreenWallpaperPreview.class);
        Bundle mExtras = new Bundle();
        mExtras.putStringArrayList("themes_uri", uriList);
        intent.putExtra("extras_themes_uri", mExtras);
        intent.setData((Uri) v.getTag());
        startActivity(intent);
    }

    @Override
    public boolean isApplied(ThemeItem themeItem) {
        return ThemeApplication.sThemeStatus.isApplied(themeItem.getPackageName()
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
    }

    @Override
    public PreviewsType getThumbnailType() {
        return PreviewsType.LOCKSCREEN;
    }
}
