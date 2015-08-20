/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lewa.themechooser.server.intf.jsonimpl;

import android.util.Log;

import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.server.intf.NetBaseParam;
import com.lewa.themechooser.server.intf.NetHelper;
import com.lewa.themechooser.server.intf.UrlParam;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes.ThemeColumns;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LewaServerJsonParser {
    private final static String TAG = "LewaServerJsonParser";
    private final static Boolean DBG = false;


    public static int parseCount(String str) {
        try {
            return Integer.parseInt(str.replaceAll("\"", "").trim());
        } catch (Exception e) {
            Log.w(TAG, "parseCount failed");
            // e.printStackTrace();
        }
        return -1;
    }

    public static ThemeBase appendThemeBase(String str, ThemeBase tb) {
        JSONObject jsonobj = null;
        if (str == null) {
            return null;
        }
        try {
            jsonobj = new JSONObject(str);
            tb.thumbnailpath = java.net.URLDecoder.decode(jsonobj.getString(NetBaseParam.THUMB), "UTF-8");
        } catch (Exception ex) {
        }

        try {
            String[] previews = jsonobj.getString(NetBaseParam.PREVIEW).split(",");
            for (int i = 0; i < previews.length; i++) {
                tb.previewpath.add(java.net.URLDecoder.decode(previews[i], "UTF-8"));
            }

            if (tb.previewpath.size() > 0) {
                ThemeItem.sortPreviews(tb.previewpath);
            }
        } catch (Exception ex) {
        }
        if (DBG) Log.d(TAG, "-----> tb.thumbnailpath " + tb.thumbnailpath);
        if (DBG) Log.d(TAG, "-----> tb.previewpath " + tb.previewpath);
        return tb;
    }

    public static List<ThemeBase> parseListThemeBase(String str, boolean parsePackage, Object... obj) {
        try {
            if (DBG) Log.d(TAG, "-------------- parseListThemeBase " + parsePackage + " "
                    + str);
            List<ThemeBase> themes = new ArrayList<ThemeBase>(0);
            if (str == null) {
                return null;
            }
            if (str.trim().contains("nothing:dpi_error") || str.trim().contains("nothing:themeid_error") || str.trim().contains("nothing:version_error")) {
                ThemeBase themeBase = new ThemeBase(null, ThemeConstants.LEWA, null, false);
                themeBase.setPackageName("nothing:error");
                themes.add(themeBase);
                return themes;
            }
            if (str.trim().contains("nothing")) {
                ThemeBase themeBase = new ThemeBase(null, ThemeConstants.LEWA, null, false);
                themeBase.setPackageName("nothing");
                themes.add(themeBase);
                return themes;
            }
            if (str.trim().contains("wrong")) {
                return themes;
            }

            JSONArray array = new JSONArray(str);
            int count = array.length();

            for (int i = 0; i < count; i++) {
                JSONObject jo = (JSONObject) array.get(i);
                ThemeBase themeBase = new ThemeBase(null, ThemeConstants.LEWA, null, false);
                themeBase.setCnName(jo.getString(NetBaseParam.NAME_ZH));
                themeBase.setEnName(jo.getString(NetBaseParam.NAME_EN));
                if (jo.has(NetBaseParam.INTERNALVERSION)) {
                    themeBase.setInternal_version(jo.getString(NetBaseParam.INTERNALVERSION));
                }
                // Begin, added by yljiang@lewatek.com 2013-11-19
                if (jo.has(ThemeColumns.MECHANISM_VERSION)) {
                    themeBase.setmMechanismVersion(jo.getInt(ThemeColumns.MECHANISM_VERSION));
                }
                // End
                if (jo.has(NetBaseParam.DATELINE)) {
                    themeBase.setCreateDate(jo.getString(NetBaseParam.DATELINE).split(" ")[0]);
                }
                if (jo.has(NetBaseParam.DOWNLOAD)) {
                    themeBase.setDownloads(jo.getString(NetBaseParam.DOWNLOAD));
                }
                if (jo.has(NetBaseParam.TID)) {
                    themeBase.setThemeId(jo.getString(NetBaseParam.TID));
                }
                if (jo.has(NetBaseParam.PREVIEW)) {
                    String[] previews = jo.getString(NetBaseParam.PREVIEW).split(",");
                    for (int j = 0; j < previews.length; j++) {
                        try {
                            themeBase.previewpath.add((java.net.URLDecoder.decode(previews[j], "UTF-8")));
                        } catch (UnsupportedEncodingException e) {
                            Log.w(TAG, "Unsupported encoding");
                            // e.printStackTrace();
                        }
                    }
                    if (themeBase.previewpath.size() > 0) {
                        ThemeItem.sortPreviews(themeBase.previewpath);
                    }
                }
                if (jo.has(NetBaseParam.THUMB)) {
                    try {
                        themeBase.thumbnailpath = java.net.URLDecoder.decode(jo.getString(NetBaseParam.THUMB), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.w(TAG, "Unsupported encoding");
                    }
                }
                if (jo.has(NetBaseParam.PACKAGENAME)) {
                    themeBase.setPackageName(jo.getString(NetBaseParam.PACKAGENAME));
                }
                if (jo.has(NetBaseParam.PKG_VERSION)) {
                    themeBase.setVersion(jo.getString(NetBaseParam.PKG_VERSION));
                }
                try {

                    if (jo.has(NetBaseParam.ThEMEID)) {
                        themeBase.setId(jo.getString(NetBaseParam.ThEMEID));
                    }
                    themeBase.setCnAuthor(jo.getString(NetBaseParam.AUTHOR));
                    themeBase.setEnAuthor(jo.getString(NetBaseParam.AUTHOR_EN));
                    themeBase.setPkg(jo.getString(NetBaseParam.PKG_NAME));

                    if (jo.has(NetBaseParam.ATTACHMENT)) {
                        themeBase.attachment = jo.getString(NetBaseParam.ATTACHMENT);
                    }
                    // 临时行为
                    themeBase.setSize(jo.getString(NetBaseParam.THEME_SIZE));
                    if (parsePackage) {
                        themeBase.setVersion(jo.getString(NetBaseParam.PKG_VERSION));//
                        if (jo.getString(NetBaseParam.LOCKSCREEN).equals("0")) {
                        } else {
                            themeBase.setContainLockScreen(true);
                        }
                        themeBase.setModelNum(jo.getString(NetBaseParam.MODULE_NUM));

                        String regetUrl = NetBaseParam.PrefixUrl + "/modulepreview?themeid=" + themeBase.getThemeId() + "&moduleid=" + ((UrlParam) obj[0]).getCombineModelInt();

                        appendThemeBase(NetHelper.getNetString(regetUrl), themeBase);
                    } else {
                        if (jo.has(NetBaseParam.THUMB)) {
                            if (DBG)
                                Log.d(TAG, "----------------- THUMB" + jo.getString(NetBaseParam.THUMB));
                            try {
                                themeBase.thumbnailpath = java.net.URLDecoder.decode(jo.getString(NetBaseParam.THUMB), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                Log.w(TAG, "Unsupported encoding");
                                // e.printStackTrace();
                            }
                        }
                        if (jo.has(NetBaseParam.PREVIEW)) {
                            themeBase.previewpath = Arrays.asList(jo.getString(NetBaseParam.PREVIEW).split(","));
                            if (themeBase.previewpath.size() > 0) {
                                ThemeItem.sortPreviews(themeBase.previewpath);
                            }
                            if (DBG)
                                Log.d(TAG, "----------------- PREVIEW" + jo.getString(NetBaseParam.PREVIEW));
                        }
                    }
                } catch (JSONException js) {
                    Log.d(TAG, "js.toString()=" + js.toString());

                }
                themes.add(themeBase);
            }
            return themes;
        } catch (JSONException se) {
            Log.w(TAG, "JSONException: " + se.toString());
            // se.printStackTrace();
        }
        return null;
    }


    public static List<ThemeBase> parseListThemeBase(JSONArray jsonArray
            , boolean parsePackage
            , NetBaseParam url) {
        try {

            List<ThemeBase> themes = new ArrayList<ThemeBase>(0);
            if (jsonArray == null || jsonArray.length() == 0) {
                return null;
            }
//            if(str.trim().contains("nothing:dpi_error")||str.trim().contains("nothing:themeid_error")||str.trim().contains("nothing:version_error")){
//                ThemeBase themeBase = new ThemeBase(null, ThemeConstants.LEWA, null, false);
//                themeBase.setPackageName("nothing:error");
//                themes.add(themeBase);
//                return themes;
//            }
//            if (str.trim().contains("nothing") ) {
//                ThemeBase themeBase = new ThemeBase(null, ThemeConstants.LEWA, null, false);
//                themeBase.setPackageName("nothing");
//                themes.add(themeBase);
//                return themes;
//            }
//            if (str.trim().contains("wrong")){
//                return themes;
//            }

            int count = jsonArray.length();

            for (int i = 0; i < count; i++) {
                JSONObject jo = (JSONObject) jsonArray.get(i);
                ThemeBase themeBase = new ThemeBase(null, ThemeConstants.LEWA, null, false);
                themeBase.setCnName(jo.getString(NetBaseParam.NAME_ZH));
                themeBase.setEnName(jo.getString(NetBaseParam.NAME_EN));
                if (jo.has(NetBaseParam.INTERNALVERSION)) {
                    themeBase.setInternal_version(jo.getString(NetBaseParam.INTERNALVERSION));
                }
                if (jo.has(ThemeColumns.MECHANISM_VERSION)) {
                    themeBase.setmMechanismVersion(jo.getInt(ThemeColumns.MECHANISM_VERSION));
                }
                // End
                if (jo.has(NetBaseParam.DATELINE)) {
                    themeBase.setCreateDate(jo.getString(NetBaseParam.DATELINE).split(" ")[0]);
                }
                if (jo.has(NetBaseParam.DOWNLOAD)) {
                    themeBase.setDownloads(jo.getString(NetBaseParam.DOWNLOAD));
                }
                if (jo.has(NetBaseParam.TID)) {
                    themeBase.setThemeId(jo.getString(NetBaseParam.TID));
                }
                if (jo.has(NetBaseParam.PREVIEW)) {
                    String[] previews = jo.getString(NetBaseParam.PREVIEW).split(",");
                    for (int j = 0; j < previews.length; j++) {
                        try {
                            themeBase.previewpath.add((java.net.URLDecoder.decode(previews[j], "UTF-8")));
                        } catch (UnsupportedEncodingException e) {
                            Log.w(TAG, "Unsupported encoding");
                            // e.printStackTrace();
                        }
                    }
                    if (themeBase.previewpath.size() > 0) {
                        ThemeItem.sortPreviews(themeBase.previewpath);
                    }
                }
                if (jo.has(NetBaseParam.THUMB)) {
                    try {
                        themeBase.thumbnailpath = java.net.URLDecoder.decode(jo.getString(NetBaseParam.THUMB), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.w(TAG, "Unsupported encoding");
                    }
                }
                if (jo.has(NetBaseParam.PACKAGENAME)) {
                    themeBase.setPackageName(jo.getString(NetBaseParam.PACKAGENAME));
                }
                if (jo.has(NetBaseParam.PKG_VERSION)) {
                    themeBase.setVersion(jo.getString(NetBaseParam.PKG_VERSION));
                }
                try {

                    if (jo.has(NetBaseParam.ThEMEID)) {
                        themeBase.setId(jo.getString(NetBaseParam.ThEMEID));
                    }
                    themeBase.setCnAuthor(jo.getString(NetBaseParam.AUTHOR));
                    themeBase.setEnAuthor(jo.getString(NetBaseParam.AUTHOR_EN));
                    themeBase.setPkg(jo.getString(NetBaseParam.PKG_NAME));

                    if (jo.has(NetBaseParam.ATTACHMENT)) {
                        themeBase.attachment = jo.getString(NetBaseParam.ATTACHMENT);
                    }
                    // 临时行为
                    themeBase.setSize(jo.getString(NetBaseParam.THEME_SIZE));
                    if (parsePackage) {
                        themeBase.setVersion(jo.getString(NetBaseParam.PKG_VERSION));//
                        if (jo.getString(NetBaseParam.LOCKSCREEN).equals("0")) {
                        } else {
                            themeBase.setContainLockScreen(true);
                        }
                        themeBase.setModelNum(jo.getString(NetBaseParam.MODULE_NUM));

                        String regetUrl = NetBaseParam.PrefixUrl + "/modulepreview?themeid=" + themeBase.getThemeId() + "&moduleid=" + url.getCombineModelInt();

                        appendThemeBase(NetHelper.getNetString(regetUrl), themeBase);
                    } else {
                        if (jo.has(NetBaseParam.THUMB)) {
                            if (DBG)
                                Log.d(TAG, "----------------- THUMB" + jo.getString(NetBaseParam.THUMB));
                            try {
                                themeBase.thumbnailpath = java.net.URLDecoder.decode(jo.getString(NetBaseParam.THUMB), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                Log.w(TAG, "Unsupported encoding");
                                // e.printStackTrace();
                            }
                        }
                        if (jo.has(NetBaseParam.PREVIEW)) {
                            themeBase.previewpath = Arrays.asList(jo.getString(NetBaseParam.PREVIEW).split(","));
                            if (themeBase.previewpath.size() > 0) {
                                ThemeItem.sortPreviews(themeBase.previewpath);
                            }
                            if (DBG)
                                Log.d(TAG, "----------------- PREVIEW" + jo.getString(NetBaseParam.PREVIEW));
                        }
                    }
                } catch (JSONException js) {
                    Log.d(TAG, "js.toString()=" + js.toString());

                }
                themes.add(themeBase);
            }
            return themes;
        } catch (JSONException se) {
            Log.w(TAG, "JSONException: " + se.toString());
            // se.printStackTrace();
        }
        return null;
    }
}
