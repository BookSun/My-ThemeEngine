package com.lewa.themechooser.adapters.online;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.preview.slide.online.PreviewOnlineIconsActivity;

import java.io.Serializable;
import java.util.ArrayList;

import util.ThemeUtil;

public class OnlineThemePkgAdapter extends ThumbnailOnlineAdapter {
    public OnlineThemePkgAdapter(Context context, ArrayList<ThemeBase> themeBases) {
        super(context, themeBases, com.lewa.themechooser.ThemeStatus.THEME_TYPE_PACKAGE);
    }

    @Override
    protected void initPath(ThemeBase themeBase) {
        try {
            url = themeBase.thumbnailpath;
            thumbnailPath = new StringBuilder().append(ThemeConstants.THEME_ONLINE_THUMBNAIL)
                    .append("/").append(ThemeConstants.THEME_THUMBNAIL_THUMBNAIL)
                    .append(ThemeUtil.getNameNoBuffix(themeBase.getPkg())).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        super.onClick(v);
        Intent intent = new Intent();
        intent.setClass(mContext, PreviewOnlineIconsActivity.class);
        intent.putExtra(ThemeConstants.THEMEBASE, (Serializable) v.getTag());
        tempThemeBases = mThemeBases;
//        intent.putExtra("themeBases", (Serializable)mThemeBases);
        comingSource.startActivityForResult(intent, REQUESTCODE);
    }
}
