package com.lewa.themechooser.adapters.online;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.custom.preview.online.OnLineLiveWallpaperPreview;
import com.lewa.themechooser.pojos.ThemeBase;

import java.io.Serializable;
import java.util.ArrayList;

import util.ThemeUtil;

public class OnlineLiveWallpaperAdapter extends ThumbnailOnlineAdapter {

    public OnlineLiveWallpaperAdapter(Context context, ArrayList<ThemeBase> themeBases) {
        super(context, themeBases, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
    }

    @Override
    protected void initPath(ThemeBase themeBase) {
        try {
            url = themeBase.thumbnailpath;
            thumbnailPath = new StringBuilder().append(ThemeConstants.THEME_ONLINE_THUMBNAIL)
                    .append("/").append(ThemeConstants.THEME_THUMBNAIL_LIVEWALLPAPER_PREFIX)
                    .append(ThemeUtil.getNameNoBuffix(themeBase.getPkg())).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        super.onClick(v);
        Intent intent = new Intent();
        intent.setClass(mContext, OnLineLiveWallpaperPreview.class);
        tempThemeBases = mThemeBases;
//        intent.putExtra("themeBases", (Serializable) mThemeBases);
        intent.putExtra(ThemeConstants.THEMEBASE, (Serializable) v.getTag());
        comingSource.startActivityForResult(intent, REQUESTCODE);
    }

}
