package com.lewa.themechooser.newmechanism;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import util.ThemeUtil;

/**
 * ApplyTeme.java
 *
 * @author yljiang@lewatek.com 2013-11-30
 */
public class ApplyThemeHelp {

    private static final String TAG = Globals.TAG;
    private static final int THREAD_MAX = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_MAX);

    public static void applyInternal(final Context context, final Intent intent, final ThemeItem item) {
        if (NewMechanismUtils.isNewMechanism(item)) {
            applyInternalForNewMechanism(context, intent, item);
        } else {
            Themes.changeTheme(context, intent);
        }
    }

    public static void applyInternalForNewMechanism(final Context context, final Intent intent, final ThemeItem item) {
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                Themes.changeTheme(context, intent);
            }
        });
    }

    public static void changeTheme(final Context context, final Intent intent) {
        final ThemeItem item = ThemeItem.getInstance(context, intent.getData());
        EXECUTOR_SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                final int type = intent.getIntExtra(CustomType.EXTRA_NAME, CustomType.THEME_TYPE);
                if (NewMechanismUtils.isNewMechanism(item)) {
                    if (android.os.Build.MODEL.equals("Nokia_X")) {
                        ThemeUtil.isChangeFont = true;
                    }
                    applyThemeForType(context, intent);
                } else if (CustomType.THEME_TYPE == type) {
                    deleteBootAnimation();
                    // The new mechanism is switched to the old theme of the mechanism ,some resources are not being replaced ,so you must Kill Process
                    if (NewMechanismUtils.isNewMechanism(Themes.getAppliedTheme(context))) {
                        ThemeUtil.isKillProcess = true;
                        intent.putExtra(ThemeManager.EXTRA_SYSTEM_APP, true);
                    }
                }
                Themes.changeTheme(context, intent);
            }
        });
    }

    private static void applyThemeForType(Context context, Intent intent) {
        ThemeItem item = ThemeItem.getInstance(context, intent.getData());
        final int type = intent.getIntExtra(CustomType.EXTRA_NAME, CustomType.THEME_TYPE);
        switch (type) {
            case CustomType.INCALL_RINGTONE:
                applyInCallRingtone(context, item);
                break;
            case CustomType.MESSAGE_RINGTONE:
                applyMessageRingtone(context, item);
                break;
            case CustomType.NOTIFICATION_RINGTONE:
                applyNoficationRingtone(context, item);
                break;
            case CustomType.THEME_TYPE:
                applyTheme(context, item);
                break;
        }
    }

    public static void applyTheme(Context context, ThemeItem item) {
        applyBootAnimation(item);
        applyInCallRingtone(context, item);
        applyMessageRingtone(context, item);
        applyNoficationRingtone(context, item);
    }

    public static void applyInCallStyle(ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.INCALL_STYLE);
        } catch (Exception e) {
            Log.e(TAG, " applyInCallStyle: " + e.toString());
        }
    }

    public static void applyInCallRingtone(Context context, ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.ZIP_RINGTONE_INCALL, Globals.SD_THEME_RES, Globals.INCALL_RINGTONE);
            if (item.getRingtoneUri(context) != null) {
                NewMechanismUtils.setFileToRingtone(context, new File(Globals.SD_THEME_RES, Globals.INCALL_RINGTONE));
            }
        } catch (Exception e) {
            Log.e(TAG, " applyRingtone: " + e.toString());
        }
    }

    public static void applyMessageRingtone(Context context, ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.ZIP_RINGTONE_MESSAGE, Globals.SD_THEME_RES, Globals.MESSAGE_RINGTONE);
            if (item.getMessagetRingtoneUri(context) != null) {
                NewMechanismUtils.setFileToMessageRingtone(context, new File(Globals.SD_THEME_RES, Globals.MESSAGE_RINGTONE));
            }
        } catch (Exception e) {
            Log.e(TAG, " applyMessageRingtone: " + e.toString());
        }
    }

    public static void applyNoficationRingtone(Context context, ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.ZIP_RINGTONE_NOTIFATION, Globals.SD_THEME_RES, Globals.NOTIFICATION_RINGTONE);
            if (item.getNotificationRingtoneUri(context) != null) {
                NewMechanismUtils.setFileToNotificationRingtone(context, new File(Globals.SD_THEME_RES, Globals.NOTIFICATION_RINGTONE));
            }
        } catch (Exception e) {
            Log.e(TAG, " applyMessageRingtone: " + e.toString());
        }
    }

    public static void applyBootAnimation(ThemeItem item) {
        try {
            if (item.getBootAnimationUri() == null) {
                return;
            }
            String path = NewMechanismUtils.zipFileToSD(item, Globals.BOOT_ANIMATION, Globals.SD_THEME_RES, Globals.BOOT_ANIMATION + Globals.ZIP);
            if (path != null) {
                NewMechanismUtils.exeRootCmd("cp -af " + path + Globals.BOOT_ANIMATION_lOCAL);
            }
        } catch (Exception e) {
            Log.e(TAG, " applyBootAnimation: " + e.toString());
        }
    }

    public static void unzipToDir(String zipPath, String entryName, String outPath) throws Exception {
        String strCmd = "unzip " + zipPath + " \"" + entryName + "\" " + " -d " + outPath;
        String olderName = outPath + entryName;
        String newName = olderName + Globals.ZIP;
        String mv = "mv " + olderName + " " + newName;
        NewMechanismUtils.exeRootCmd(strCmd + ";" + mv);
    }

    public static void deleteBootAnimation() {
        try {
            NewMechanismUtils.exeRootCmd("rm -rf " + Globals.BOOT_ANIMATION_lOCAL + Globals.BOOT_ANIMATION + Globals.ZIP);
        } catch (Exception e) {
            Log.e(TAG, " deleteBootAnimation(): " + e.toString());
        }
    }

    public static void applyWallpaper(ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.ZIP_WALLPAPER, Globals.SD_THEME_RES, Globals.WALLPAPER);
        } catch (Exception e) {
            Log.e(TAG, " applyWallpaper: " + e.toString());
        }
    }

    public static void applyWallpaperForLockScreen(ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.ZIP_WALLPAPER_LOCKSCREEN, Globals.SD_THEME_RES, Globals.WALLPAPER_LOCKSCREEN);
        } catch (Exception e) {
            Log.e(TAG, " applyWallpaperLockScreen : " + e.toString());
        }
    }

    public static void applyLockScreen(ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.LOCKSCREAN);
        } catch (Exception e) {
            Log.e(TAG, " applyLockScreen: " + e.toString());
        }
    }

    public static void applyIcon(ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.ICON);
        } catch (Exception e) {
            Log.e(TAG, "applyIcon: " + e.toString());
        }

    }

    public static void applyFont(ThemeItem item) {
        try {
            NewMechanismUtils.zipFileToSD(item, Globals.FONT);
        } catch (Exception e) {
            Log.e(TAG, " applyFont : " + e.toString());
        }
    }

    public static void applyOtherPackage(ThemeItem item) {
        try {
            zipOtherPackage(item);
        } catch (Exception e) {
            Log.e(TAG, " applyOtherPackage: " + e.toString());
        }
    }

    public static void zipOtherPackage(ThemeItem item) throws Exception {
        if (item == null || item.getDownloadPath() == null) {
            return;
        }
        ZipFile mZipFile = new ZipFile(item.getDownloadPath());
        if (mZipFile != null) {
            final Enumeration<?> entries = mZipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = (ZipEntry) entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(Globals.OTHER) || Globals.FRAMEWORK.equals(entryName) || Globals.LEWA.equals(entryName)) {
                    InputStream in = mZipFile.getInputStream(entry);
                    NewMechanismUtils.writeStreamToFile(in, Globals.SD_THEME_RES, entryName);
                }
            }
            mZipFile.close();
        }
    }
}
