package com.lewa.themechooser.newmechanism;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import com.lewa.themes.provider.ThemeItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NewMechanismUtils {

    public static final String KEY_SMS_NOTIFICATION_RINGTONE = "pref_key_ringtone";

    public static boolean isNewMechanism(ThemeItem item) {
        try {
            if (item != null && item.getMechanismVersion() >= Globals.NEW_MECHANISM) {
                return true;
            }
        } catch (Exception e) {
            Log.e(Globals.TAG, "isNewMechanism---e:" + e.toString());
        }
        return false;
    }

    public static boolean isBlankFile(File file) {
        if (file == null || !file.exists() || file.length() <= 0 || !file.isFile())
            return true;
        return false;
    }

    public static void mkDirFile(String strDir) {
        File dirFile = new File(strDir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        } else if (!dirFile.isDirectory()) {
            dirFile.delete();
            dirFile.mkdirs();
        }
    }

    public static String appendFileStart(String path) {
        if (isBlankStr(path))
            return null;
        return Globals.FILE_TITLE + path;
    }

    public static boolean isBlankStr(String str) {
        return (str == null || str.length() == 0 || str.trim().equals(""));
    }

    public static String formatString(String path) {
        if (isBlankStr(path))
            return null;
        final int MAX_LENGTH = 128;
        final int length = path.length();
        if (length > MAX_LENGTH)
            path = path.substring(length - MAX_LENGTH);
        return path.replaceAll("[\\W]", "").replaceAll("_", "");
    }

    public static String appendJpgEnd(String path) {
        return formatString(path) + Globals.JPG;
    }

    public static String writeStreamToFile(InputStream in, String dirPath, String fileName) throws Exception {
        if (in != null) {
            mkDirFile(dirPath);
            File outFile = new File(dirPath, fileName);
            FileUtils.copyToFile(in, outFile);
            in.close();
            return outFile.getAbsolutePath();
        }
        return null;
    }

    public static String zipFileToSD(ThemeItem item, String fileName) throws Exception {
        return zipFileToSD(item, fileName, Globals.SD_THEME_RES, fileName);
    }

    public static String zipFileToSD(ThemeItem item, String zipEntryName, String dirPath, String fileName) throws Exception {
        ZipFile mZipFile = new ZipFile(item.getDownloadPath());
        String path = null;
        if (mZipFile != null) {
            ZipEntry zipEntry = mZipFile.getEntry(zipEntryName);
            if (zipEntry != null) {
                path = writeStreamToFile(mZipFile.getInputStream(zipEntry), dirPath, fileName);
            }
            mZipFile.close();
        }
        return path;
    }

    public static void exeRootCmd(String str) throws Exception {
        java.lang.Process process = Runtime.getRuntime().exec(Globals.ROOT);
        InputStream inputStream = process.getInputStream();
        OutputStream outputStream = process.getOutputStream();
        outputStream.write(str.getBytes());
        outputStream.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        reader.close();
        inputStream.close();
    }

    public static Uri getRingtoneUri(Context context, File file, String key) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, file.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
        if (key != null)
            values.put(key, true);
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
        Uri newUri = contentResolver.insert(uri, values);
        return newUri;
    }

    public static void setFileToMessageRingtone(Context context, File file) {
        Uri newUri = getRingtoneUri(context, file, null);
        Settings.System.putString(context.getContentResolver(), KEY_SMS_NOTIFICATION_RINGTONE, newUri.toString());
    }

    public static void setFileToNotificationRingtone(Context context, File file) {
        Uri newUri = getRingtoneUri(context, file, MediaStore.Audio.Media.IS_NOTIFICATION);
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION, newUri);
    }

    public static void setFileToRingtone(Context context, File file) {
        Uri newUri = getRingtoneUri(context, file, MediaStore.Audio.Media.IS_RINGTONE);
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);
        //for mtk two sim card
        try {
            Method[] methods = Settings.System.class.getMethods();
            Method method = Settings.System.class.getDeclaredMethod("putString", ContentResolver.class, String.class, String.class);
            method.invoke(null, context.getContentResolver(), "ringtone_2", newUri.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
