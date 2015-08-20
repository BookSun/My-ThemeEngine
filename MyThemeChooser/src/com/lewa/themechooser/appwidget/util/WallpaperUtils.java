package com.lewa.themechooser.appwidget.util;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Environment;
import android.util.Log;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.appwidget.CoveringPageActivity;
import com.lewa.themechooser.appwidget.service.WallpaperDownloadService;
import com.lewa.themes.provider.Themes.ThemeColumns;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import util.ThemeUtil;

public class WallpaperUtils {
    public static final int EXPECTED_DOWNLOADED_AMOUNT = 300;
    public static final int WALLPAPER_CHANGING_TIMEOUT = 1500;
    public static final String RANDOM_WIDGET_SETTING = "Random_Widget";
    private static final boolean DEBUG = false;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SO_TIMEOUT = 5000;
    public static boolean isWriting;
    public static boolean isChanging;
    public static int currentID;
    public static long startTime;
    private String mDefaultWallpaperLocale = "/system/media/wallpapers/";
    private String mDownloadedWallpaperLocale = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/LEWA/theme/deskwallpaper/";
    private String mRequestUri = "http://admin.lewatek.com/themeapi2/update_wall_paper";
    private ArrayList<String> mDefaultWallpaperPathList = new ArrayList<String>();
    private ArrayList<String> mDownloadedWallpaperPathList = new ArrayList<String>();
    private ArrayList<String> mPreDownloadedWallpaperPathList = new ArrayList<String>();
    private ContentResolver mContentResolver;
    private String mPreviousWallpaperPath;
    private SharedPreferences sp;
    private String tag;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mDensity;
    private String mDpi;
    private int currentIndex;
    private boolean onlineDownload;
    private boolean changeInOrder;

    public WallpaperUtils(Context context) {
        mContentResolver = context.getContentResolver();
        onlineDownload = context.getResources().getBoolean(R.bool.config_onekeywallpaper_online_download);
        changeInOrder = context.getResources().getBoolean(R.bool.config_onekeywallpaper_change_in_order);
        sp = context.getSharedPreferences(RANDOM_WIDGET_SETTING, Context.MODE_PRIVATE);
        getDefaultWallpaperPathList(context);
        if (onlineDownload) {
            tag = "WallpaperDownloadService";
            getDownloadedWallpaperPathList();
        } else {
            tag = "WallpaperChangeService";
            currentIndex = sp.getInt("currentIndex", 0);
        }
        CommonUtils commonUtils = new CommonUtils(context);
        Point point = commonUtils.getScreenSize();
        mDensity = commonUtils.getDensity();
        mDpi = commonUtils.getDpi();
        if (mDensity < 480) {
            mDisplayWidth = point.x * 2;
            mDisplayHeight = point.y;
        } else {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            mDisplayWidth = wallpaperManager.getDesiredMinimumWidth();
            mDisplayHeight = wallpaperManager.getDesiredMinimumHeight();
        }
    }

    public static void startWallpaperService(Context context) {
        boolean onlineDownload = context.getResources().getBoolean(R.bool.config_onekeywallpaper_online_download);
        Intent service = null;
        if (onlineDownload) {
            service = new Intent("com.lewa.themechooser.OnekeyWallpaperDownloadService");
        } else {
            service = new Intent("com.lewa.themechooser.OnekeyWallpaperChangeService");
        }
        context.startService(service);
    }

    public void downloadWallpaper(Context context, Wallpaper wallpaper)
            throws ClientProtocolException, IOException {
        String wallpaperName = wallpaper.getPackageName();
        if (DEBUG)
            Log.i(tag, "开始请求下载图片：" + wallpaperName);
        currentID = wallpaper.getId();
        ThemeApplication.sThemeStatus.setDownloading(currentID, wallpaper.getPackageName(),
                wallpaper.getFileName(), ThemeStatus.THEME_TYPE_WALLPAPER);
        HttpEntity httpEntity = getHttpEntity(getActualWallpaperUri(wallpaper.getUri()));
        InputStream content = httpEntity.getContent();
        isWriting = true;
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(
                mDownloadedWallpaperLocale + wallpaperName + ".jpg"));
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = content.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        out.close();
        content.close();
        isWriting = false;
        WallpaperDownloadService.achievedDownloadedAmount++;
        WallpaperDownloadService.downloadCompleted = true;
        if (DEBUG)
            Log.i(tag, "开始将图片写进数据库：" + wallpaperName);
        insertImageInfo(context, wallpaper.getThemeID(), wallpaper.getPackageName(),
                wallpaper.getSize());
    }

    public boolean deleteWallpaper(Wallpaper wallpaper) {
        String wallpaperName = wallpaper.getPackageName();
        File file = new File(mDownloadedWallpaperLocale + wallpaperName + ".jpg");
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }

    public boolean isWallpaperDownloaded(Wallpaper wallpaper) {
        String wallpaperName = wallpaper.getPackageName();
        File file = new File(mDownloadedWallpaperLocale);
        if (file.exists()) {
            String[] strFiles = file.list();
            if (strFiles != null) {
                for (String strFile : strFiles) {
                    if ((wallpaperName + ".jpg").equals(strFile)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private HttpEntity getHttpEntity(String uri) throws ClientProtocolException, IOException {
        HttpEntity httpEntity = null;
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(uri);
        HttpParams httparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httparams, CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httparams, SO_TIMEOUT);
        httpGet.addHeader("User-Agent", ThemeUtil.userAgent);
        httpGet.setParams(httparams);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        httpEntity = httpResponse.getEntity();
        return httpEntity;
    }

    public ArrayList<Wallpaper> getWallpaperList(String uri) throws ClientProtocolException,
            IOException {
        ArrayList<Wallpaper> wallpaperList = new ArrayList<Wallpaper>();
        try {
            HttpEntity httpEntity = getHttpEntity(uri);
            JSONArray jsonArray = new JSONArray(EntityUtils.toString(httpEntity));
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    wallpaperList.add(new Wallpaper((JSONObject) jsonArray.opt(i)));
                }
            }
        } catch (ParseException e) {
            Log.e(tag, e.toString());
        } catch (JSONException e) {
            Log.e(tag, e.toString());
        }
        return wallpaperList;
    }

    private ArrayList<String> getDefaultWallpaperPathList(Context context) {
        mDefaultWallpaperPathList.clear();
        File file = new File(mDefaultWallpaperLocale);
        if (file.exists()) {
            String[] strFiles = file.list();
            if (strFiles != null) {
                String defaultWallpaperLocale = mDefaultWallpaperLocale;
                for (String strFile : strFiles) {
                    mDefaultWallpaperPathList.add(defaultWallpaperLocale + strFile);
                }
            }
        } else {
            if (DEBUG)
                Log.d(tag, "没有创建默认壁纸文件夹");
        }
        return mDefaultWallpaperPathList;
    }

    private ArrayList<String> getDownloadedWallpaperPathList() {
        if (isWriting) {
            return mDownloadedWallpaperPathList;
        }
        mDownloadedWallpaperPathList.clear();
        if (CommonUtils.isSdCardMounted()) {
            File file = new File(mDownloadedWallpaperLocale);
            if (file.exists()) {
                String[] strFiles = file.list();
                if (strFiles != null) {
                    if (onlineDownload) {
                        WallpaperDownloadService.achievedDownloadedAmount = strFiles.length;
                    }
                    String downloadedWallpaperLocale = mDownloadedWallpaperLocale;
                    if (!changeInOrder) {
                        for (String strFile : strFiles) {
                            mDownloadedWallpaperPathList.add(downloadedWallpaperLocale + strFile);
                        }
                    } else {
                        mDownloadedWallpaperPathList.addAll(mPreDownloadedWallpaperPathList);
                        for (String strFile : strFiles) {
                            String wallpaperPath = downloadedWallpaperLocale + strFile;
                            if (!mDownloadedWallpaperPathList.contains(wallpaperPath))
                                mDownloadedWallpaperPathList.add(wallpaperPath);
                        }
                        mPreDownloadedWallpaperPathList.clear();
                        mPreDownloadedWallpaperPathList.addAll(mDownloadedWallpaperPathList);
                    }
                }
            } else {
                file.mkdirs();
            }
        }
        return mDownloadedWallpaperPathList;
    }

    public String getWallpaperPath() {
        if (changeInOrder) {
            return getOrderedWallpaerPath();
        } else {
            return getRandomWallpaperPath();
        }
    }

    private String getOrderedWallpaerPath() {
        synchronized (WallpaperUtils.class) {
            int defaultSize = mDefaultWallpaperPathList.size();
            ArrayList<String> downloadedWallpaperPathList = getDownloadedWallpaperPathList();
            int downloadedSize = downloadedWallpaperPathList.size();
            int totalSize = defaultSize + downloadedSize;
            if (totalSize == 0 || totalSize == 1) {
                return null;
            }

            String orderedWallpaperPath = null;
            if (currentIndex >= totalSize) {
                currentIndex = 0;
                if (mDefaultWallpaperPathList.size() > 0) {
                    orderedWallpaperPath = mDefaultWallpaperPathList.get(0);
                } else {
                    orderedWallpaperPath = downloadedWallpaperPathList.get(0);
                }
            } else {
                if (currentIndex < defaultSize) {
                    orderedWallpaperPath = mDefaultWallpaperPathList.get(currentIndex);
                } else {
                    orderedWallpaperPath = downloadedWallpaperPathList.get(currentIndex
                            - defaultSize);
                }
            }
            if (onlineDownload) {
                currentIndex++;
            } else {
                sp.edit().putInt("currentIndex", ++currentIndex).commit();
            }
            return orderedWallpaperPath;
        }

    }

    private String getRandomWallpaperPath() {
        synchronized (WallpaperUtils.class) {
            int defaultSize = mDefaultWallpaperPathList.size();
            ArrayList<String> downloadedWallpaperPathList = getDownloadedWallpaperPathList();
            int downloadedSize = downloadedWallpaperPathList.size();
            int totalSize = defaultSize + downloadedSize;
            if (totalSize == 0 || totalSize == 1) {
                return null;
            }

            int randomIndex = (int) (Math.random() * totalSize);
            String randomWallpaperPath = null;
            if (randomIndex >= defaultSize) {
                randomWallpaperPath = downloadedWallpaperPathList.get(randomIndex - defaultSize);
            } else {
                randomWallpaperPath = mDefaultWallpaperPathList.get(randomIndex);
            }
            if (randomWallpaperPath.equals(mPreviousWallpaperPath)) {
                return getRandomWallpaperPath();
            } else {
                mPreviousWallpaperPath = randomWallpaperPath;
            }
            return randomWallpaperPath;
        }
    }

    private void insertImageInfo(Context context, String themeID, String packageName, String size) {
        Cursor query = mContentResolver.query(ThemeColumns.CONTENT_PLURAL_URI, null,
                ThemeColumns.THEME_ID + "=?", new String[]{
                        themeID
                }, null
        );
        if (query.moveToNext()) {
            query.close();
            return;
        }
        query.close();
        ContentValues values = new ContentValues();
        values.put(ThemeColumns.THEME_ID, themeID);
        values.put(ThemeColumns.NAME, packageName + ".jpg");
        values.put(ThemeColumns.THEME_PACKAGE, packageName);
        values.put(ThemeColumns.SIZE, size);
        values.put(ThemeColumns.AUTHOR, "");
        values.put(ThemeColumns.STYLE_NAME, "");
        values.put(ThemeColumns.IS_IMAGE_FILE, -1);
        String wallpaperUri = "file://" + mDownloadedWallpaperLocale + packageName + ".jpg";
        values.put(ThemeColumns.WALLPAPER_URI, wallpaperUri);
        mContentResolver.insert(ThemeColumns.CONTENT_PLURAL_URI, values);
    }

    public String getWallpaperPackageName(String wallpaperPath) {
        Cursor query = mContentResolver.query(ThemeColumns.CONTENT_PLURAL_URI, null,
                ThemeColumns.WALLPAPER_URI + "=?", new String[]{
                        "file://" + wallpaperPath
                }, null
        );
        String wallpaperPackageName = null;
        if (query != null) {
            if (query.moveToNext()) {
                wallpaperPackageName = query.getString(query
                        .getColumnIndex(ThemeColumns.THEME_PACKAGE));
            }
            query.close();
        }
        return wallpaperPackageName;
    }

    public String getActualWallpaperUri(String uri) {
        int index = uri.lastIndexOf("/");
        return uri.substring(0, index + 1) + "_" + mDisplayWidth + "-" + mDisplayHeight
                + uri.substring(index);
    }

    public String getJsonArrayUri() {
        return mRequestUri + "?" + "screen=" + mDisplayWidth + "-" + mDisplayHeight + "&density="
                + mDensity + "&dpi=" + mDpi + "&wifi="
                + (WallpaperDownloadService.isWifiOn ? 1 : 0) + "&finished="
                + WallpaperDownloadService.achievedDownloadedAmount;
    }

    public InputStream getCalculateStream(String wallpaperPath) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(wallpaperPath, options);
        options.inSampleSize = ThemeUtil.calculateInSampleSize(options, mDisplayWidth,
                mDisplayHeight);
        if (options.inSampleSize <= 1) {
            return new FileInputStream(wallpaperPath);
        } else {
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(wallpaperPath, options);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            baos.close();
            return is;
        }
    }

    public void showCoveringPage(Context context) {
        if (mDensity >= 480) {
            Intent activity = new Intent(context, CoveringPageActivity.class);
            activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activity);
        }
    }
}
