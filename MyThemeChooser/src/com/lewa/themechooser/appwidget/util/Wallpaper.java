package com.lewa.themechooser.appwidget.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Wallpaper {
    private static final String TAG = "Wallpaper";
    private Integer id;
    private String themeID;
    private String uri;
    private String fileName;
    private String packageName;
    private String size;

    public Wallpaper(JSONObject wallpaerJson) {
        try {
            id = wallpaerJson.getInt("id");
            themeID = wallpaerJson.getString("Themeid");
            uri = wallpaerJson.getString("url");
            fileName = wallpaerJson.getString("Filename");
            packageName = wallpaerJson.getString("Packagename");
            size = wallpaerJson.getString("Size");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    public Integer getId() {
        return id;
    }

    public String getThemeID() {
        return themeID;
    }

    public String getUri() {
        return uri;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "Wallpaper [id=" + id + ", themeID=" + themeID + ", uri=" + uri + ", fileName="
                + fileName + ", packageName=" + packageName + ", size=" + size + "]";
    }

}
