package com.lewa.themechooser.custom.preview.local;


import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themechooser.preview.slide.local.PreviewIconsActivity;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;

public class LockScreenStylePreview extends PreviewIconsActivity {

    private static final String TAG = "LockScreenStylePreview";
    private static final Boolean DBG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    protected void doApply(ThemeItem bean) {
        ThemeItem appliedTheme = Themes.getAppliedTheme(this);
        if (null == appliedTheme) {
            return;
        }
        Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
        appliedTheme.close();
        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
        i.putExtra(ThemeManager.EXTRA_LOCKSCREEN_URI, bean.getLockscreenUri());
        if (bean.getLockWallpaperUri(this) != null) {
            i.putExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI, bean.getLockWallpaperUri(this));
        }
        if (bean.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                && bean.getThemeId().equals("LewaDefaultTheme")) {
            i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, true);
        }
        //i.putExtra(CustomType.EXTRA_NAME,ThemeConstants.LOCKSCREEN_STYLE_TYPE);
        i.putExtra(CustomType.EXTRA_NAME, CustomType.THEME_DETAIL);
        ApplyThemeHelp.changeTheme(this, i);

        ThemeApplication.sThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(this
                , bean, PreviewsType.LOCKSCREEN), com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_SCREEN);
        if (bean.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                && bean.getThemeId().equals("LewaDefaultTheme")) {
            ThemeApplication.sThemeStatus.setAppliedPkgName(getThemePackageName(),
                    com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        } else {
            ThemeApplication.sThemeStatus.setAppliedPkgName(bean.getPackageName(),
                    com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        }
    }

    @Override
    public ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeItem, PreviewsType.LOCKSCREEN);
    }

    @Override
    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_lock_screen_style);
    }

    @Override
    protected String getDeleteToast() {
        return getString(R.string.lockscreen_style_delete_success);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        if("com.lewa.lockscreen2".equals(mThemeItem.getPackageName())){
            MenuItem item = menu.findItem(R.id.setting);
            item.setVisible(true);
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.setting) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.lewa.lockscreen2",
                    "com.lewa.lockscreen2.LockscreenSettings"));
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
