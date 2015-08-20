package com.lewa.themechooser.receiver;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.os.Process;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeChooser;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.Themes.ThemeColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerifyThemesCompletedReceiver extends BroadcastReceiver {
    private static final String THEME_ROOT_PATH = "/LEWA/theme";
    private static final String THEME_WALLPAPER_PATH = THEME_ROOT_PATH + "/deskwallpaper";
    private static final String THEME_LOCK_WALLPAPER_PATH = THEME_ROOT_PATH + "/lockwallpaper";
    private static final byte[] JPEG_HEAD_FORMAT1 = {-1, -40, -1, -32}; // "ffd8ffe0";
    private static final byte[] JPEG_HEAD_FORMAT2 = {-1, -40, -1, -31}; // "ffd8ffe1";

    private boolean isTopActivity(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
        List<RecentTaskInfo> appTask = activityManager.getRecentTasks(Integer.MAX_VALUE, 1);
        String packageName = "com.lewa.themechooser";
        if (appTask == null) {
            return false;
        }

        if (appTask.size() > 0 && appTask.get(0).baseIntent.toString().contains(packageName)) {
            return true;
        }

        return false;
//        if (tasksInfo.size() > 0) {
//            if (packageName.equals(tasksInfo.get(0).topActivity.getPackageName())) {
//                return true;
//            }
//        }
//        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.MEDIA_MOUNTED")) {
            VerifyDownloadedImagesThread.start(context);
        } else if (!ThemeManager.STANDALONE
                && !Environment.MEDIA_MOUNTED.equals(intent.getAction())) {
            if (isTopActivity(context)) {
                ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.Theme);
                Toast t = Toast.makeText(themeWrapper, R.string.theme_sd_not_avaliable,
                        Toast.LENGTH_SHORT);
                t.show();				
                //#65004 deleted by bin.dong 
               /* Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
                mHomeIntent.addCategory(Intent.CATEGORY_HOME);
                mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(mHomeIntent);*/
            }
        }
    }

    public static class VerifyDownloadedImagesThread extends Thread {
        private static final Pattern sPattern = Pattern.compile("[0-9]{2,}");
        private static VerifyDownloadedImagesThread sInstance;
        private static Context mContext;
        private File mSdcardRoot = Environment.getExternalStorageDirectory();
        private ContentResolver mContentResolver;

        private VerifyDownloadedImagesThread() {
            throw new AssertionError();
        }

        private VerifyDownloadedImagesThread(Context context) {
            mSdcardRoot = Environment.getExternalStorageDirectory();
            mContentResolver = context.getContentResolver();
        }

        public static void start(Context context) {
            mContext = context;
            if (sInstance != null && !Thread.interrupted())
                sInstance.interrupt();
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                sInstance = new VerifyDownloadedImagesThread(context);
                sInstance.setPriority(Thread.MIN_PRIORITY);
                sInstance.start();
            }
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            verifyWallpapers(THEME_WALLPAPER_PATH, ThemeColumns.WALLPAPER_URI);
            verifyWallpapers(THEME_LOCK_WALLPAPER_PATH, ThemeColumns.LOCK_WALLPAPER_URI);
        }

        private void verifyWallpapers(String path, String field) {
            verifyDirtyWallpaper(path);

            if (!isAppUpgraded() & !isExternalWallpaperChanged(field)) {
                return;
            }

            verifyDatabase(path, field);
            verifyLocaleWallpaper(path, field);
        }

        private void verifyDirtyWallpaper(String path) {
            String localPath = mSdcardRoot.getPath() + path;
            File file = new File(localPath);
            String[] imageNameList = file.list();
            if (imageNameList != null) {
                for (String imageName : imageNameList) {
                    if (!imageName.startsWith("com.lewa.pkg")) {
                        File tempFile = new File(localPath, imageName);
                        if (tempFile.exists() && tempFile.isFile()) {
                            tempFile.delete();
                        }
                    }
                }
            }
        }

        private void verifyDatabase(String path, String field) {
            ArrayList<String> imagePathList = new ArrayList<String>();
            ArrayList<String> dirtyImageUriList = new ArrayList<String>();
            String localPath = mSdcardRoot.getPath() + path;
            File file = new File(localPath);
            String[] imageNameList = file.list();
            if (imageNameList != null) {
                for (String imageName : imageNameList) {
                    imagePathList.add(localPath + "/" + imageName);
                }
            }

            Cursor query = mContentResolver.query(ThemeColumns.CONTENT_PLURAL_URI, new String[]{
                    field }, ThemeColumns.IS_SYSTEM + "=0 AND " + ThemeColumns.IS_IMAGE_FILE + "=-1 AND " + field
                    + " IS NOT NULL ", null, null);
            int startIndex = "file://".length();
            while (query.moveToNext()) {
                String imageUri = query.getString(0);
                if (!imagePathList.contains(imageUri.substring(startIndex))) {
                    dirtyImageUriList.add(imageUri);
                }
            }
            query.close();

            for (String dirtyImageUri : dirtyImageUriList) {
                mContentResolver.delete(ThemeColumns.CONTENT_PLURAL_URI, field + "=?",
                        new String[]{
                                dirtyImageUri
                        }
                );
            }
        }

        private void verifyLocaleWallpaper(String path, String field) {
            ArrayList<String> imageUriList = new ArrayList<String>();
            ArrayList<String> freshImagePathList = new ArrayList<String>();
            Cursor query = mContentResolver.query(ThemeColumns.CONTENT_PLURAL_URI, new String[]{
                    field
            }, ThemeColumns.IS_SYSTEM + "=0 AND " + ThemeColumns.IS_IMAGE_FILE + "=-1 AND " + field
                    + " IS NOT NULL ", null, null);
            while (query.moveToNext()) {
                imageUriList.add(query.getString(0));
            }
            query.close();

            String localPath = mSdcardRoot.getPath() + path;
            File file = new File(localPath);
            String[] imageNameList = file.list();
            if (imageNameList != null) {
                for (String imageName : imageNameList) {
                    String imagePath = localPath + "/" + imageName;
                    if (!(imageUriList.contains("file://" + imagePath) || imageName
                            .equals(".nomedia"))) {
                        if (checkJPEG(imagePath)) {
                            freshImagePathList.add(imagePath);
                        }
                    }
                }
            }

            for (String freshImagePath : freshImagePathList) {
                String imageID = getImageID(freshImagePath);
                if (imageID != null) {
                    insertFreshImage(imageID, freshImagePath, new File(freshImagePath).length(),
                            field);
                }
            }
        }

        private String getImageID(String str) {
            Matcher matcher = sPattern.matcher(str);
            if (matcher.find()) {
                return matcher.group();
            }
            return null;
        }

        private void insertFreshImage(String imageID, String path, long size, String field) {
            ContentValues values = new ContentValues();
            values.put(ThemeColumns.THEME_ID, "com.lewa.themeid." + imageID);
            values.put(ThemeColumns.NAME, "com.lewa.pkg." + imageID + ".jpg");
            values.put(ThemeColumns.THEME_PACKAGE, "com.lewa.pkg." + imageID);
            values.put(ThemeColumns.SIZE, (float) size / 1000 + "kb");
            values.put(ThemeColumns.AUTHOR, "");
            values.put(ThemeColumns.STYLE_NAME, "");
            values.put(ThemeColumns.IS_IMAGE_FILE, -1);
            values.put(field, "file://" + path);
            mContentResolver.insert(ThemeColumns.CONTENT_PLURAL_URI, values);
        }

        private boolean isAppUpgraded() {
            String TAG_VERSION = "app_version";
            boolean upgraded = false;
            try {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
                int curVersion = mContext.getPackageManager().getPackageInfo(
                        mContext.getPackageName(), 0).versionCode;
                int perVersion = sp.getInt(TAG_VERSION, -1);
                upgraded = curVersion != perVersion;
                if (upgraded)
                    sp.edit().putInt(TAG_VERSION, curVersion).commit();
            } catch (Exception e) {
            }
            return upgraded;
        }

        private boolean isExternalWallpaperChanged(String field) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                return false;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            String hashName = null;
            String endPath = null;
            if (ThemeColumns.WALLPAPER_URI.equals(field)) {
                hashName = "external_deskwallpaper_hash";
                endPath = "/LEWA/theme/deskwallpaper";
            } else {
                hashName = "external_lockwallpaper_hash";
                endPath = "/LEWA/theme/lockwallpaper";
            }
            int hash = sp.getInt(hashName, -1);
            File file = new File(Environment.getExternalStorageDirectory(), endPath);
            int newHash = Arrays.hashCode(file.list());
            if (hash == newHash)
                return false;
            sp.edit().putInt(hashName, newHash).commit();
            return true;
        }

        private boolean checkJPEG(String imagePath) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(imagePath);
                byte[] buf = new byte[4];
                if (fis.read(buf) != -1) {
                    return isJPEGFormat(buf, JPEG_HEAD_FORMAT1)
                            || isJPEGFormat(buf, JPEG_HEAD_FORMAT2);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean isJPEGFormat(byte[] srcJPEGHead, byte[] dstJPEGHead) {
            if (srcJPEGHead != null && srcJPEGHead.length >= dstJPEGHead.length) {
                for (int i = 0; i < dstJPEGHead.length; i++)
                    if (srcJPEGHead[i] != dstJPEGHead[i])
                        return false;
            }
            return true;
        }

    }
}
