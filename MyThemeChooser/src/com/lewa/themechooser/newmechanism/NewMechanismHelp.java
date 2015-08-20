package com.lewa.themechooser.newmechanism;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.util.Log;

import com.lewa.themechooser.ThemeApplication;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes.ThemeColumns;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lewa.os.FileUtilities;

/**
 * NewMechanismHelp.java:
 *
 * @author yljiang@lewatek.com 2013-12-2
 */
public class NewMechanismHelp {
    public static final String TAG = "NewMechanismHelp";
    public static String fromAPK = "";
    public static Uri installExtTheme(Context context, String path, boolean updateLocale) throws Exception {
        final File file = new File(path);
        Uri result = null;
        if (!file.exists())
            return result;
        ZipFile mZipFile = new ZipFile(file);
        if (mZipFile == null) {
            Log.d(TAG, "NewMechanismHelp installExtTheme error, not zip file");
            return result;
        }
        ZipEntry entryZipEntry = mZipFile.getEntry(Globals.ZIP_DESCRIPTION);
        if (entryZipEntry == null)
            return result;
        final ThemeDescription description = new ThemeDescription();
        description.setFileSize(file.length());
        description.setDownloadUrl(file.getAbsolutePath());
        description.setZipName(file.getName());
        InputStream input = mZipFile.getInputStream(entryZipEntry);
        ParseThemeVersion.getParseData(input, description);
        input.close();
        if (description.getName() == null)
            return result;
        try {
            zipLwtThumbnailForDB(file, mZipFile, description);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Enumeration<?> entries = mZipFile.entries();
        ArrayList<String> applyPackages = new ArrayList<String>();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry != null) {
                final String entryName = entry.getName();
                if (entryName.startsWith(Globals.OTHER) || Globals.FRAMEWORK.equals(entryName) || Globals.LEWA.equals(entryName)) {
                    if (applyPackages.isEmpty()) {
                        applyPackages.add(path);
                    }
                    applyPackages.add(entryName);
                } else if (Globals.ICON.equals(entryName)) {
                    description.setIconUrl(FileContentProvider.makePath(path, Globals.ICON));
                } else if (Globals.FONT.equals(entryName)) {
                    description.setFrontUrl(FileContentProvider.makePath(path, Globals.FONT));
                } else if (Globals.LOCKSCREAN.equals(entryName)) {
                    description.setLockscreenUrl(FileContentProvider.makePath(path, Globals.LOCKSCREAN));
                } else if (Globals.ZIP_WALLPAPER.equals(entryName)) {
                    description.setWallpaperUrl(FileContentProvider.makePath(path, Globals.ZIP_WALLPAPER));
                } else if (Globals.ZIP_WALLPAPER_LOCKSCREEN.equals(entryName)) {
                    description.setLockWallpaperUrl(FileContentProvider.makePath(path, Globals.ZIP_WALLPAPER_LOCKSCREEN));
                } else if (Globals.INCALL_STYLE.equals(entryName)) {
                    description.setInCallStyleUrl(FileContentProvider.makePath(path, Globals.INCALL_STYLE));
                } else if (Globals.BOOT_ANIMATION.equals(entryName)) {
                    description.setBootAnimationUri(FileContentProvider.makePath(
                            Globals.SD_THEME_RES, Globals.BOOT_ANIMATION));
                } else if (Globals.ZIP_RINGTONE_INCALL.equals(entryName)) {
                    description.setInCallRingtoneUrl(FileContentProvider.makePath(
                            Globals.SD_THEME_RES, Globals.INCALL_RINGTONE));
                } else if (Globals.ZIP_RINGTONE_MESSAGE.equals(entryName)) {
                    description.setMessageRingtoneUrl(FileContentProvider.makePath(
                            Globals.SD_THEME_RES, Globals.MESSAGE_RINGTONE));
                } else if (Globals.ZIP_RINGTONE_NOTIFATION.equals(entryName)) {
                    description.setNotifRingtoneUrl(FileContentProvider.makePath(
                            Globals.SD_THEME_RES, Globals.NOTIFICATION_RINGTONE));
                }
            }
        }
        if (applyPackages.size() > 0) {
            description.setApplyPackages(applyPackages);
        }
        mZipFile.close();
        result = updateDB(context, path, updateLocale, description);
        return result;
    }

    private static void zipLwtThumbnailForDB(File lwtFile, ZipFile mZipFile, ThemeDescription description) throws Exception {

        String fileName = NewMechanismUtils.appendJpgEnd(
                description.getPackageName() + Globals.ZIP_THUMBNAI_THEME);
        File f = new File(Globals.SD_THEME_THUMBNAIL, fileName);
        if (f.exists() && f.lastModified() >= lwtFile.lastModified()) {
            description.setThumbnailUrl(NewMechanismUtils.appendFileStart(f.getAbsolutePath()));
        } else {
            ZipEntry thumbnailEntry = mZipFile.getEntry(Globals.ZIP_THUMBNAI_THEME);
            if (thumbnailEntry != null) {
                InputStream inputStream = mZipFile.getInputStream(thumbnailEntry);
                if (inputStream != null) {
                    NewMechanismUtils.writeStreamToFile(inputStream, Globals.SD_THEME_THUMBNAIL,
                            fileName);
                    description.setThumbnailUrl(NewMechanismUtils.appendFileStart(f.getAbsolutePath()));
                }
            }
        }
    }

    public static Uri updateDB(Context context, String path, boolean updateLocale, ThemeDescription description) {
        ContentResolver ct = context.getContentResolver();
        Uri uri = null;
        if (ct == null || description == null)
            return uri;
        final String selection = ThemeColumns.THEME_PACKAGE + "='" + description.getPackageName() + "'";
        Cursor cursor = null;
        try {
            cursor = ct.query(ThemeColumns.CONTENT_PLURAL_URI,
                    new String[] { ThemeColumns.DOWNLOAD_PATH, ThemeColumns.VERSION_CODE,
                            ThemeColumns.THEME_ID }, selection, null, null);
            if (cursor.moveToFirst()) {
                String download = cursor.getString(0);
                int olderVersionCode = cursor.getInt(1);
                // Add by Fan.Yang
                String themeId = cursor.getString(2);
                int newVersionCode = description.getVersionCode();
                if (newVersionCode < olderVersionCode) {
                    FileUtilities.deleteIfExists(path);
                } else {
                    uri = ThemeColumns.CONTENT_URI.buildUpon()
                            .appendPath(description.getPackageName()).appendPath(themeId).build();
                    if (updateLocale) {
                        ContentValues values = new ContentValues();
                        values.put(ThemeColumns.NAME, description.getName());
                        values.put(ThemeColumns.AUTHOR, description.getAuthor());
                        ct.update(ThemeColumns.CONTENT_PLURAL_URI, values, selection, null);
                    } else if (!path.equals(download)) {
                        FileUtilities.deleteIfExists(download);
                        ct.delete(ThemeColumns.CONTENT_PLURAL_URI, selection, null);
                        uri = insertThemeToDB(context, path, updateLocale, description);
                    }
                }
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                    cursor = null;
                }
            } else {
                uri = insertThemeToDB(context, path, updateLocale, description);
            }
        } catch (Exception e) {
            Log.e(Globals.TAG, "updateDB--error:" + e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            cursor = null;
            return uri;
        }
    }

    public static Uri insertThemeToDB(Context context, String path, boolean updateLocale, ThemeDescription description) {
        ContentValues values = getContentValues(context, description, path, false);
        Uri uri = context.getContentResolver().insert(ThemeColumns.CONTENT_PLURAL_URI, values);
        ThemeApplication.sThemeStatus.setDownloaded(description.getPackageName());
        return uri;
    }

    public static ContentValues getContentValues(Context context, ThemeDescription description, String path, boolean isCurrentTheme) {
        final ContentValues outValues = new ContentValues();
        outValues.put(ThemeColumns.IS_APPLIED, isCurrentTheme ? 1 : 0);
        outValues.put(ThemeColumns.THEME_ID, description.getPackageName());
        outValues.put(ThemeColumns.THEME_PACKAGE, description.getPackageName());
        outValues.put(ThemeColumns.NAME, description.getName());
        outValues.put(ThemeColumns.AUTHOR, description.getAuthor());
        outValues.put(ThemeColumns.STYLE_NAME, description.getPackageName());
        outValues.put(ThemeColumns.MECHANISM_VERSION, description.getMechanismVersion());
        outValues.put(ThemeColumns.DOWNLOAD_PATH, description.getDownloadUrl());
        outValues.put(ThemeColumns.SIZE, description.getFileSize());
        outValues.put(ThemeColumns.THUMBNAIL_URI, description.getThumbnailUrl());
        outValues.put(ThemeColumns.VERSION_CODE, description.getVersionCode());
        outValues.put(ThemeColumns.VERSION_NAME, description.getVersionName());
        final String applyPackages = description.getApplyPackagesStr();
        outValues.put(ThemeColumns.HAS_THEME_PACKAGE_SCOPE, NewMechanismUtils.isBlankStr(applyPackages) ? 0 : 1);
        outValues.put(ThemeColumns.APPLY_PACKAGES, applyPackages);
        if (description.getBootAnimationUri() != null) {
            outValues.put(ThemeColumns.BOOT_ANIMATION_URI, description.getBootAnimationUri());
        }
        if (description.getWallpaperUrl() != null) {
            outValues.put(ThemeColumns.WALLPAPER_URI, description.getWallpaperUrl());
//            outValues.put(ThemeColumns.IS_IMAGE_FILE, -1);
        }
        if (description.getFrontUrl() != null) {
            outValues.put(ThemeColumns.FONT_URI, description.getFrontUrl());
        }
        if (description.getLockWallpaperUrl() != null) {
            outValues.put(ThemeColumns.LOCK_WALLPAPER_URI, description.getLockWallpaperUrl());
//            outValues.put(ThemeColumns.IS_IMAGE_FILE, -1);
        }
        if (description.getIconUrl() != null) {
            outValues.put(ThemeColumns.ICONS_URI, description.getIconUrl());
        }
        if (description.getLockscreenUrl() != null) {
            outValues.put(ThemeColumns.LOCKSCREEN_URI, description.getLockscreenUrl());
        }
        if (description.getInCallRingtoneUrl() != null) {
            outValues.put(ThemeColumns.RINGTONE_NAME, description.getName());
            outValues.put(ThemeColumns.RINGTONE_NAME_KEY, Audio.keyFor(description.getName()));
            outValues.put(ThemeColumns.RINGTONE_URI, description.getInCallRingtoneUrl());
        }
        if (description.getMessageRingtoneUrl() != null) {
            outValues.put(ThemeColumns.MESSAGE_RINGTONE_NAME, description.getName());
            outValues.put(ThemeColumns.MESSAGE_RINGTONE_NAME_KEY, Audio.keyFor(description.getName()));
            outValues.put(ThemeColumns.MESSAGE_RINGTONE_URI, description.getMessageRingtoneUrl());
        }
        if (description.getNotifRingtoneUrl() != null) {
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME, description.getName());
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME_KEY, Audio.keyFor(description.getName()));
            outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_URI, description.getNotifRingtoneUrl());
        }
        if (description.getInCallStyleUrl() != null) {
            outValues.put(ThemeColumns.INCALL_STYLE, description.getInCallStyleUrl());
        }
        return outValues;
    }

    public static List<Uri> getPreview(ThemeItem themeItem, Context context) {
        List<Uri> list = themeItem.getPreviews(context);
        if ((list == null || list.size() <= 0) && themeItem.getMechanismVersion() > 0) {
            try {
                final ArrayList<Uri> arrayList = new ArrayList<Uri>();
                //yixiao add theme filter for independent launcher
                if(!ThemeManager.INDEPENDENT){
                    addToList(NewMechanismHelp.getPreviews(context, themeItem, PreviewsType.LOCKSCREEN), arrayList);
                }
                addToList(NewMechanismHelp.getPreviews(context, themeItem,
                        PreviewsType.LAUNCHER_ICONS), arrayList);
                // TCL Evoque 937553 add by Fan.Yang
                if (!ThemeManager.STANDALONE) {
                    addToList(NewMechanismHelp.getPreviews(context, themeItem,
                            PreviewsType.FRAMEWORK_APPS), arrayList);
                    addToList(NewMechanismHelp.getPreviews(context, themeItem, PreviewsType.FONTS),
                            arrayList);
                }
                list = arrayList;
            } catch (Exception e) {
                Log.e(Globals.TAG, "NewMechanismHelp.getPreviewList--:" + e.toString());
            }
            if (list != null)
                themeItem.setPreviewUris(list);
        }
        return list;
    }

    private static void addToList(List<Uri> arrayList, List<Uri> arrayList2) {
        if (arrayList != null && arrayList.size() > 0) {
            arrayList2.addAll(arrayList);
        }
    }

    public static List<Uri> getPreviews(Context context, ThemeItem themeItem, PreviewsType type) {
        List<Uri> list = themeItem.getPreviews(context, type);
        if ((list == null || list.size() <= 0) && themeItem.getMechanismVersion() >= 0) {
            isDefaultTheme(themeItem);
            try {
                list = NewMechanismHelp.getPreviewList(themeItem, type);
                if (list.size() == 0) {
                    if (type == PreviewsType.DEFAULT_THEME_WALLPAPER &&
                            isFile(themeItem.getWallpaperUri(context))) {
                        list.add(themeItem.getWallpaperUri(context));
                    } else if (type == PreviewsType.LOCKWALLPAPER &&
                            isFile(themeItem.getLockWallpaperUri(
                                    context))) {
                        list.add(themeItem.getLockWallpaperUri(context));
                    }
                }
            } catch (Exception e) {
                Log.e(Globals.TAG, "NewMechanismHelp.getPreviewList--:" + e.toString());
            }
            if (list != null) {
                themeItem.putPreviewUris(type, list);
            }
        }
/*       //#64909 add begin by bin.dong
       if((list == null || list.size() <= 0) && themeItem.getMechanismVersion() == 0){
          PACKAGE_WALLPAPER = true;
           try {
               list = NewMechanismHelp.getPreviewList(themeItem, type);
           } catch (Exception e) {
               Log.e(Globals.TAG, "NewMechanismHelp.getPreviewList--:" + e.toString());
           }
           PACKAGE_WALLPAPER = false;
       }
       //#64909 add end by bin.dong*/
        return list;
    }

    private static boolean isFile(Uri uri) {
        if (uri != null && uri.toString().startsWith("file://")) {
            return true;
        }
        return false;
    }

    private static boolean isDefaultTheme(ThemeItem item) {
        String packageName = item.getPackageName();
        if (item.getMechanismVersion() == 0) {
            if (ThemeManager.THEME_ELEMENTS_PACKAGE.equals(packageName)) {
                fromAPK = "assets/";
            } else if (ThemeManager.THEME_LOCKSCREEN2_PACKAGE.equals(packageName)) {
                fromAPK = "assets/lockscreen2/";
            } else {
                fromAPK = "res/drawable/";
            }
            return true;
        }
        return false;
    }

    public static List<Uri> getPreviewList(ThemeItem item, PreviewsType type) throws Exception {
        if (item == null || item.getDownloadPath() == null) {
            return null;
        }
        File lwtFile = new File(item.getDownloadPath());
        ZipFile mZipFile = new ZipFile(lwtFile);
        if (mZipFile != null) {
            long lwtLastModified = lwtFile.lastModified();
            final ArrayList<Uri> arrayList = new ArrayList<Uri>();
            final Enumeration<?> entries = mZipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = (ZipEntry) entries.nextElement();
                String entryName = entry.getName();
                boolean isTrue = false;
               if(type == PreviewsType.DEFAULT_THEME_WALLPAPER&&item.getMechanismVersion() > 0){
                 isTrue = entryName.equals(getZipNameForPreview(type));
               }else{
                 isTrue = entryName.startsWith(getZipNameForPreview(type));
               }
                if (isTrue && (entryName.endsWith(Globals.JPG) || entryName.endsWith(Globals.PNG))) {
                    String fileName = NewMechanismUtils.appendJpgEnd(
                            item.getPackageName() + entryName);
                    File imgFile = new File(Globals.SD_THEME_PREVIEW, fileName);
                    if (NewMechanismUtils.isBlankFile(imgFile) || lwtLastModified > imgFile.lastModified()) {
                        InputStream inputStream = mZipFile.getInputStream(entry);
                        if (inputStream != null) {
                            NewMechanismUtils.writeStreamToFile(inputStream,
                                    Globals.SD_THEME_PREVIEW, fileName);
                            arrayList.add(Uri.parse(NewMechanismUtils.appendFileStart(imgFile.getAbsolutePath())));
                        }
                    } else {
                        arrayList.add(Uri.parse(NewMechanismUtils.appendFileStart(imgFile.getAbsolutePath())));
                    }
                }
            }
            mZipFile.close();
            return arrayList;
        }
        return null;
    }

    private static String getZipNameForPreview(PreviewsType type) {
        if (type == null)
            return Globals.ZIP_PREVIEW;
        switch (type) {
            case LAUNCHER_ICONS:
                return Globals.ZIP_PREVIEW_ICON;
            case LOCKSCREEN:
                return fromAPK + Globals.ZIP_PREVIEW_LOCKSCREEN;
            //TCL937553 add by Fan.Yang
            case LOCKWALLPAPER:
                if (fromAPK.startsWith("res/")) {
                    return fromAPK + "lockscreen_wallpaper.png";
                }
                return fromAPK + Globals.ZIP_PREVIEW_LOCKSCREEN_WALLPAPER;
            case FRAMEWORK_APPS:
                return Globals.ZIP_PREVIEW_SYSTEMAPP;
            case FONTS:
                return Globals.ZIP_PREVIEW_FONT;
            case DEFAULT_THEME_WALLPAPER:
                // TCL964445 add by Fan.Yang
                if (fromAPK.startsWith("res/")) {
                    return fromAPK + "wallpaper.jpg";
                }
                //TCL937553 add by Fan.Yang
                return fromAPK+ Globals.ZIP_PREVIEW_WALLPAPER;
            default:
                return Globals.ZIP_PREVIEW;
        }
    }

    public static Uri getThumbnails(Context context, ThemeItem item, PreviewsType type) {
        try {
            if (!NewMechanismUtils.isNewMechanism(item)) {
                return item.getThumbnails(context, type);
            } else {
                return getThumbnails(item, type);
            }
        } catch (Exception e) {
            Log.e(Globals.TAG, "NewMechanismHelp.getThumbnails--:" + e.toString());
        }
        return null;
    }

    public static Uri getApplyThumbnails(Context context, ThemeItem item, PreviewsType type) {
        try {
            if (!NewMechanismUtils.isNewMechanism(item)) {
                return item.getThumbnails(context, type);
            } else {
                return Uri.parse(item.getPackageName());
            }
        } catch (Exception e) {
            Log.e(Globals.TAG, "NewMechanismHelp.getThumbnails--:" + e.toString());
        }
        return null;
    }

    public static Uri getThumbnails(ThemeItem item, PreviewsType type) throws Exception {
        String zipName = getZipNameForThumbnails(type);
        if (zipName == null) {
            return null;
        }
        File lwtFile = new File(item.getDownloadPath());
        String fileName = NewMechanismUtils.appendJpgEnd(item.getPackageName() + zipName);
        File f = new File(Globals.SD_THEME_THUMBNAIL, fileName);
        if (NewMechanismUtils.isBlankFile(f) || lwtFile.lastModified() >= f.lastModified()) {
            ZipFile mZipFile = new ZipFile(lwtFile);
            if (mZipFile != null) {
                ZipEntry thumbnailEntry = mZipFile.getEntry(zipName);
                if (thumbnailEntry != null) {
                    InputStream inputStream = mZipFile.getInputStream(thumbnailEntry);
                    if (inputStream != null) {
                        NewMechanismUtils.writeStreamToFile(inputStream, Globals.SD_THEME_THUMBNAIL, fileName);
                        return Uri.parse(NewMechanismUtils.appendFileStart(f.getAbsolutePath()));
                    }
                }
                mZipFile.close();
            }
        } else {
            return Uri.parse(NewMechanismUtils.appendFileStart(f.getAbsolutePath()));
        }
        return Uri.parse("");
    }

    /**
     * LOCKSCREEN(0), BOOT_ANIMATION(1), LAUNCHER_ICONS(2), FRAMEWORK_APPS(3), FONTS(4)
     * , OTHER(5), DESKWALLPAPER(6), LOCKWALLPAPER(7), LIVE_WALLPAPER(8);
     *
     * @param type
     * @return
     */
    private static String getZipNameForThumbnails(PreviewsType type) {
        if (type == null)
            return Globals.ZIP_THUMBNAIL_LAUNCHER;
        switch (type) {
            case LAUNCHER_ICONS:
                return Globals.ZIP_THUMBNAIL_LAUNCHER;
            case LOCKSCREEN:
                return Globals.ZIP_THUMBNAIL_LOCKSCREEN;
            case FRAMEWORK_APPS:
                return Globals.ZIP_THUMBNAIL_SYSTEMAPP;
            case FONTS:
                return Globals.ZIP_THUMBNAIL_FONT;
            default:
                return Globals.ZIP_THUMBNAIL_LAUNCHER;
        }

    }
}
