package com.lewa.themechooser.receiver;

import android.app.IntentService;
import android.app.WallpaperInfo;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ThemeInfo;
import android.database.Cursor;
import android.net.Uri;
import android.service.wallpaper.WallpaperService;
import android.text.TextUtils;
import android.util.Log;

import com.lewa.themechooser.custom.preview.local.FontsPreview;
import com.lewa.themechooser.custom.preview.local.LiveWallpaperPreview;
import com.lewa.themechooser.preview.slide.local.LocalPreviewIconsActivity;
import com.lewa.themes.Utils;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import util.ThemeUtil;

import static com.lewa.themes.ThemeManager.STANDALONE;

public class ThemeInstallService extends IntentService {
    private final static boolean DEBUG = false;
    private final static String TAG = "ThemeInstallService";


    private PackageManager mPm = null;

    public ThemeInstallService() {
        super("ThemeInstallService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String themeFile = intent.getStringExtra("THEME_PACKAGE");
        if (null != themeFile && themeFile.startsWith("file://")) {
            themeFile = themeFile.substring(7, themeFile.length());
        }
        boolean isFromFileManager = intent.getBooleanExtra("isFromFileManager", false);
        boolean apply = intent.getBooleanExtra("APPLY", false);

        if (TextUtils.isEmpty(themeFile)) {
            Cursor c = this.getContentResolver().query(
                    intent.getData(), new String[]{"_data"}, null, null, null);

            if (c.moveToFirst()) {
                themeFile = c.getString(0);
            }
            c.close();
        }
        if (DEBUG) {
            Log.d(TAG, "installing/updating theme: " + themeFile);
        }

        if (null == mPm) mPm = this.getPackageManager();
        PackageInfo pi;
        if (STANDALONE) {
            pi = mPm.getPackageArchiveInfo(themeFile, PackageManager.GET_CONFIGURATIONS);
            if (null == pi || !Themes.isThemePackage(pi)) {
                if (DEBUG) {
                    Log.d(TAG, themeFile + " is NOT a theme: " + pi);
                }
            }
        } else {
            pi = mPm.getPackageArchiveInfo(themeFile, 0);
            if (null == pi) {
                // Return if it's not an APK or it's not a theme
                if (DEBUG) {
                    Log.d(TAG, themeFile + " is NOT a theme");
                }
            } else {
                // int N = pi.themeInfos.size();
                // N must be equal or larger than 1
                // For simplicity, always assume N is 1
                if (DEBUG) {
                    Log.d(TAG, "theme to be installed: "
                                    + ", Package: " + pi.packageName
                                    + ", Version: " + pi.versionCode
                    );
                }
            }
        }

        if (null == pi) {
            Uri newUri = ThemeUtil.installExternalTheme(this, themeFile, false);

            if(isFromFileManager) {
                // #69132 Add by Fan.Yang
                startPreviewActivity(isFromFileManager, newUri);
            }
            return;
        }
        PackageInfo piInstalled = null;
        try {
            piInstalled = mPm.getPackageInfo(pi.packageName, 0);
        } catch (Exception e) {
            // ignore
        }
        if (!STANDALONE) {
            //Delete for standalone by Fan.Yang
            ThemeItem mThemeItem = null;//Themes.getTheme(this, pi.packageName, pi.themeInfos != null ? pi.themeInfos[0].themeId : null);
            if (null != piInstalled && piInstalled.versionCode >= pi.versionCode && mThemeItem != null) {
                // Newer version exists
                if (DEBUG) {
                    Log.d(TAG, "latest version of theme " + pi.packageName + " already installed on your device");
                }
                if (STANDALONE) {
                    boolean notify = intent.getBooleanExtra("NOTIFY", false);
                    if (notify) {
                        android.content.SharedPreferences.Editor editor =
                                getSharedPreferences("DOWNLOADED", android.content.Context.MODE_PRIVATE).edit();
                        editor.putLong(pi.packageName, 1);
                        editor.commit();
                    }
                }
                if (isFromFileManager) {
                    startPreviewActivity(isFromFileManager, pi, themeFile);
                }
                return;
            }
        }

        /*
         * pm install: installs a package to the system.  Options:
         *     -l: install the package with FORWARD_LOCK.
         *     -r: reinstall an exisiting app, keeping its data.
         *     -t: allow test .apks to be installed.
         *     -i: specify the installer package name.
         *     -s: install package on sdcard.
         *     -f: install package on internal flash.
         *
         * pm set-install-location: changes the default install location.
         *   NOTE: this is only intended for debugging; using this can cause
         *   applications to break and other undersireable behavior.
         *     0 [auto]: Let system decide the best location
         *     1 [internal]: Install on internal device storage
         *     2 [external]: Install on external media
         */

        if (DEBUG) {
            Log.d(TAG, "installing theme " + pi.packageName + " ...");
        }
        ThemeInfo ti = Utils.parsePackage(themeFile);
        if (ti == null) {
            Themes.installTheme(this, themeFile);
        }
        ThemeUtil.installExternalTheme(this, themeFile, false);
        getContentResolver().notifyChange(ThemeColumns.CONTENT_PLURAL_URI, null);
        if (apply) {
            Intent i = new Intent("com.lewa.apply");
            sendBroadcast(i);
        }
        /*
         * try {
         *     // TODO: Install a theme to external media by default?
         *     Runtime.getRuntime().exec("pm set-install-location 2 ; pm install " + themeFile).waitFor();
         * } catch (Exception e) {
         *     e.printStackTrace();
         * }
         */

        // Delete the lwt in /data/data/<themechooser>/files/ if it exists
        if (!STANDALONE) {
            //Delete for standalone by Fan.Yang
/*            if (null != pi.themeInfos && null != pi.themeInfos[0] && null != pi.themeInfos[0].themeId) {
                File shareFile = new File(getFilesDir(), pi.themeInfos[0].themeId + ".lwt");
                if (shareFile.exists()) {
                    if (DEBUG) {
                        Log.d(TAG, "deleting previously shared theme ...");
                    }
                    shareFile.delete();
                }
            }*/
        }
        if (isFromFileManager) {
            startPreviewActivity(isFromFileManager, pi, themeFile);
        }
    }

    //#69132 Add by Fan.Yang
    private void startPreviewActivity(boolean isFromFileManager, Uri uri) {
        if (uri == null)
            return;
        ThemeItem mThemeItem = ThemeItem.getInstance(this, uri);
        Intent i;
        if (mThemeItem.getFontUril() != null && mThemeItem.getWallpaperUri(this) == null &&
                mThemeItem.getLockscreenUri() == null &&
                mThemeItem.getLockWallpaperUri(this) == null
                && mThemeItem.getIconsUri() == null) {
            i = new Intent(this, FontsPreview.class);
        } else {
            i = new Intent(this, LocalPreviewIconsActivity.class);
        }
        i.setData(uri);
        i.putExtra("isFromFileManager", isFromFileManager);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(i);
    }

    private void startPreviewActivity(boolean isFromFileManager, PackageInfo pi, String pkgPath) {
        ThemeInfo themeInfo = null;
        if (STANDALONE) {
            themeInfo = Utils.parsePackage(pkgPath);
        } else {
            //Delete for standalone by Fan.Yang
/*            if (pi.themeInfos != null) {
                themeInfo = pi.themeInfos[0];
            }*/
        }


        if (themeInfo != null) {
            ThemeItem mThemeItem = Themes.getTheme(this, pi.packageName, themeInfo.themeId);
            Intent i;
            if (mThemeItem.getFontUril() != null && mThemeItem.getWallpaperUri(this) == null &&
                    mThemeItem.getLockscreenUri() == null &&
                    mThemeItem.getLockWallpaperUri(this) == null
                    && mThemeItem.getIconsUri() == null) {
                i = new Intent(this, FontsPreview.class);
            } else {
                i = new Intent(this, LocalPreviewIconsActivity.class);
            }
            i.setData(Themes.getThemeUri(this, pi.packageName, themeInfo.themeId));
            i.putExtra("isFromFileManager", isFromFileManager);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(i);
        } else {
            if (!STANDALONE) {
                Intent i = new Intent(this, LiveWallpaperPreview.class);
                Intent livewallpaper_intent = new Intent(WallpaperService.SERVICE_INTERFACE);
                List<ResolveInfo> list = getPackageManager().queryIntentServices(
                        new Intent(WallpaperService.SERVICE_INTERFACE),
                        PackageManager.GET_META_DATA);
                WallpaperInfo info = null;
                for (ResolveInfo resolveInfo : list) {
                    try {
                        info = new WallpaperInfo(this, resolveInfo);
                        if (resolveInfo.serviceInfo.packageName.equals(pi.packageName)) {
                            break;
                        }
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                livewallpaper_intent.setClassName(pi.packageName, info.getServiceName());
                i.putExtra("android.live_wallpaper.intent", livewallpaper_intent);
                i.putExtra("android.live_wallpaper.settings", info.getSettingsActivity());
                i.putExtra("android.live_wallpaper.package", info.getPackageName());
                i.putExtra("isFromFileManager", isFromFileManager);
                i.putExtra("live_wallpaper_info_intent", info);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            }
        }


    }

    private void readStream(InputStream is) {
        try {
            int i = is.read();
            while (i != -1) {
                i = is.read();
            }
        } catch (IOException e) {
        }
    }

}
