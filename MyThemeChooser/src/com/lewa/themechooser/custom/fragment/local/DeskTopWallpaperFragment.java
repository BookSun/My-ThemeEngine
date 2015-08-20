package com.lewa.themechooser.custom.fragment.local;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.custom.preview.local.DeskTopWallpaperPreview;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;

public class DeskTopWallpaperFragment extends LocalBase {
    public static final String TAG = "DeskTopWallpaperFragment";
    public static final String DEFAULT_THEME_PACKAGE ="com.lewa.theme.LewaDefaultTheme"; 
    public static final String DEFAULT_THEME_UTI = "/system/media/wallpapers/default.png";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayoutRes = R.layout.theme_local_wallpaper_main;
        mItemLayoutResource = R.layout.theme_desktop_grid_item_thumbnail;
    }

    @Override
    protected int getType() {
        return DESKTOP_WALLPAPER_TYPE;
    }

    @Override
    public void startActivity(View v) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), DeskTopWallpaperPreview.class);
        Bundle mExtras = new Bundle();
        mExtras.putStringArrayList("themes_uri", uriList);
        intent.putExtra("extras_themes_uri", mExtras);
        intent.setData((Uri) v.getTag());
        getActivity().startActivity(intent);
    }

    @Override
    public boolean isApplied(Uri themeUri) {
        String uri = themeUri.toString();
        int lastSlash = uri.lastIndexOf('/');
        uri = uri.substring(++lastSlash, uri.length());
        try {
            uri = java.net.URLDecoder.decode(uri, "UTF-8");
        } catch (Exception e) {
        }

        return (ThemeApplication.sThemeStatus
                .isWallpaperApplied(themeUri.toString(), ThemeStatus.THEME_TYPE_WALLPAPER) ||
                ThemeApplication.sThemeStatus.isApplied(uri
                        , uri, com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER));
    }

    @Override
    public boolean isApplied(ThemeItem themeItem) {
        WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(getActivity())
                .getWallpaperInfo();
        if (wallpaperInfo == null) {
            String themeUri = themeItem.getWallpaperUri(getActivity()).getPath().toString();
            String themeSystemUri = Settings.System.getString(getActivity().getContentResolver(),
                    "lewa_wallpaper_path");
            if (TextUtils.isEmpty(themeSystemUri)) {
                return ThemeApplication.sThemeStatus
                        .isApplied(themeItem.getPackageName(), ThemeStatus.THEME_TYPE_WALLPAPER);
            }
            return themeUri.equals(themeSystemUri);
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_PICTRUE_FOR_WALLPAPER);
        return true;
    }

    @Override
    public PreviewsType getThumbnailType() {
        return PreviewsType.DESKWALLPAPER;
    }

    /*
     * @Override
     * public List<String> getAdditionalThemes(com.lewa.themechooser.ThemeStatus status) {
     *     return status.getDownloadedWallpapers();
     * }
     */
}
