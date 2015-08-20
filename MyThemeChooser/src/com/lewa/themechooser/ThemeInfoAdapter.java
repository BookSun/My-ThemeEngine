package com.lewa.themechooser;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lewa.themechooser.widget.ImageViewCheck;
import com.lewa.themes.provider.ThemeItem;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.HashMap;

public class ThemeInfoAdapter extends BaseAdapter implements OnClickListener {
    public HashMap<String, Uri> mHashMap;
    public int theme_info_width;
    public int theme_info_height;
    protected ImageLoader imageLoader = ImageLoader.getInstance();
    private ArrayList<String> mNameList;
    private ArrayList<Uri> mUriList;
    private Context mContext;
    private ThemeItem mThemeItem;
    private ArrayList<Uri> mThumbnailList;
    private ArrayList<Integer> mCropPreviews;
    private int width;
    private int height;

    public ThemeInfoAdapter(ArrayList mNameList, ArrayList mUriList, Context mContext
            , ThemeItem themeItem, ArrayList mThumbnailList, ArrayList mCropPreviews) {
        this.mContext = mContext;
        this.mNameList = mNameList;
        this.mUriList = mUriList;
        mHashMap = new HashMap();
        this.mThemeItem = themeItem;
        this.mThumbnailList = mThumbnailList;
        this.mCropPreviews = mCropPreviews;
        for (int i = 0; i < this.mNameList.size(); i++) {
            mHashMap.put(this.mNameList.get(i), this.mUriList.get(i));
        }
        width = mContext.getResources().getDimensionPixelOffset(R.dimen.screen_width) / 2;
        height = mContext.getResources().getDimensionPixelOffset(R.dimen.screen_height) / 2;
        theme_info_width = mContext.getResources().getDimensionPixelOffset(R.dimen.thumbnail_deskwallpaper_width);
        theme_info_height = mContext.getResources().getDimensionPixelOffset(R.dimen.thumbnail_deskwallpaper_height);
    }

    @Override
    public int getCount() {
        return this.mNameList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 3;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewCache viewCache = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.theme_info_adapter, null);
            viewCache = new ViewCache();
            viewCache.thumbnail = (ImageViewCheck) convertView.findViewById(R.id.thumbnail);
            viewCache.style_name = (TextView) convertView.findViewById(R.id.name);
            viewCache.check_icon = (View) convertView.findViewById(R.id.check_icon);
            viewCache.container = (ViewGroup) convertView;
            convertView.setTag(viewCache);
        } else {
            viewCache = (ViewCache) convertView.getTag();
        }
        viewCache.style_name.setText(mNameList.get(position));
        if (this.mCropPreviews.get(position) == 0) {
            viewCache.thumbnail.setTag(Integer.valueOf(0));
        } else if (this.mCropPreviews.get(position) == 1) {
            viewCache.thumbnail.setTag(Integer.valueOf(1));
        } else if (this.mCropPreviews.get(position) == 2) {
            viewCache.thumbnail.setTag(Integer.valueOf(2));
        }
        if (mThumbnailList.get(position) != null) {
            imageLoader.displayImage(mThumbnailList.get(position).toString(), viewCache.thumbnail);
        } else {
            viewCache.thumbnail.setImageResource(R.drawable.theme_detail_default);
        }
        viewCache.thumbnail.setOnClickListener(this);
        viewCache.thumbnail.setId(position);
        viewCache.thumbnail.setTag(R.id.tag_button, viewCache.check_icon);
        viewCache.thumbnail.setChecked(true);
        viewCache.check_icon.setVisibility(View.VISIBLE);
        viewCache.thumbnail.setTag(R.id.tag_button + 1, viewCache.container);
        return convertView;
    }

    //    @Override
    public void onClick(View v) {
        ImageViewCheck tvc = (ImageViewCheck) v;
        View iv = (View) v.getTag(R.id.tag_button);
        ViewGroup con = (ViewGroup) v.getTag(R.id.tag_button + 1);
        if (tvc.isChecked()) {
            tvc.setChecked(false);
            iv.setVisibility(View.GONE);
            con.setBackgroundColor(Color.TRANSPARENT);
            mHashMap.remove(mNameList.get(v.getId()));
        } else {
            tvc.setChecked(true);
            iv.setVisibility(View.VISIBLE);
            mHashMap.put(mNameList.get(v.getId()), mUriList.get(v.getId()));
        }
    }

    private class ViewCache {
        public ImageViewCheck thumbnail;
        public TextView style_name;
        public View check_icon;
        public ViewGroup container;
    }

}
