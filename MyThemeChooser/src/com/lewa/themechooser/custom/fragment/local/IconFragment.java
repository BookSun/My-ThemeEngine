package com.lewa.themechooser.custom.fragment.local;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.custom.preview.local.IconPreview;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;


/**
 * @author xufeng
 */
public class IconFragment extends LocalBase {
    @Override
    protected int getType() {
        return DESKTOP_ICON_TYPE;
    }

    @Override
    public void startActivity(View v) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), IconPreview.class);
        Bundle mExtras = new Bundle();
        mExtras.putStringArrayList("themes_uri", uriList);
        intent.putExtra("extras_themes_uri", mExtras);
        intent.setData((Uri) v.getTag());
        getActivity().startActivity(intent);
    }

    @Override
    public boolean isApplied(ThemeItem themeItem) {
        return ThemeApplication.sThemeStatus.isApplied(themeItem.getPackageName()
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_ICONS);
    }

    @Override
    public PreviewsType getThumbnailType() {
        return PreviewsType.LAUNCHER_ICONS;
    }
}
