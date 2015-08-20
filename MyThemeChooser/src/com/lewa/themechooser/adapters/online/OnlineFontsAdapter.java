package com.lewa.themechooser.adapters.online;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.custom.preview.online.OnLineFontsPreview;
import com.lewa.themechooser.pojos.ThemeBase;

import java.io.Serializable;
import java.util.ArrayList;

import util.ThemeUtil;

public class OnlineFontsAdapter extends ThumbnailOnlineAdapter {

    public OnlineFontsAdapter(Context context, ArrayList<ThemeBase> themeBases) {
        super(context, themeBases, com.lewa.themechooser.ThemeStatus.THEME_TYPE_FONT);
        mLayoutRes = com.lewa.themechooser.R.layout.theme_fonts;
    }

    public void onClick(View v) {
        super.onClick(v);
        Intent intent = new Intent();
        intent.setClass(mContext, OnLineFontsPreview.class);
        intent.putExtra("themeBases", (Serializable) mThemeBases);
        intent.putExtra(ThemeConstants.THEMEBASE, (Serializable) v.getTag());
        comingSource.startActivityForResult(intent, REQUESTCODE);
    }

    @Override
    protected void initPath(ThemeBase themeBase) {
        try {
            url = themeBase.thumbnailpath;
            thumbnailPath = new StringBuilder().append(ThemeConstants.THEME_ONLINE_THUMBNAIL)
                    .append("/").append(ThemeConstants.THEME_THUMBNAIL_FONTS_PREFIX)
                    .append(ThemeUtil.getNameNoBuffix(themeBase.getPkg())).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
        super.onClick(view);
        Intent intent = new Intent();
        intent.setClass(mContext, OnLineFontsPreview.class);
        tempThemeBases = mThemeBases;
        intent.putExtra(ThemeConstants.THEMEBASE
                , (Serializable) ((ViewCache) view.getTag()).thumbnail.getTag());
        comingSource.startActivityForResult(intent, REQUESTCODE);
    }
}
