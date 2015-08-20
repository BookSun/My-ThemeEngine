package com.lewa.themechooser.adapters.online;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.custom.preview.online.OnLineDeskTopWallpaperPreview;
import com.lewa.themechooser.pojos.ThemeBase;

import java.io.Serializable;
import java.util.ArrayList;

import util.ThemeUtil;

public class OnlineWallPaperAdapter extends ThumbnailOnlineAdapter {
    public OnlineWallPaperAdapter(Context context, ArrayList<ThemeBase> themeBases) {
        super(context, themeBases, com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER);
    }

    @Override
    protected void initPath(ThemeBase themeBase) {
        try {
            url = themeBase.thumbnailpath;
            String start = url.substring(0, url.lastIndexOf("/") + 1);
            String end = url.substring(url.lastIndexOf("/"), url.length());
            url = start + "_" + ThemeUtil.thumbnailWidth + "-" + ThemeUtil.thumbnailHeight + end;
            thumbnailPath = new StringBuilder().append(ThemeConstants
                    .THEME_ONLINE_WALLPAPER_THUMBNAIL).append("/").append(ThemeConstants
                    .THEME_THUMBNAIL_WALLPAPER_PREFIX).append(ThemeUtil
                    .getNameNoBuffix(themeBase.getPkg())).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        super.onClick(v);
        Intent intent = new Intent();
        intent.setClass(mContext, OnLineDeskTopWallpaperPreview.class);
        tempThemeBases = mThemeBases;
//        intent.putExtra("themeBases", (Serializable) mThemeBases);
        intent.putExtra(ThemeConstants.THEMEBASE, (Serializable) v.getTag());
        comingSource.startActivityForResult(intent, REQUESTCODE);
    }
}
