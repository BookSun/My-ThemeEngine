package com.lewa.themes.provider;

import com.lewa.themes.ThemeManager;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.CustomTheme;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lewa.os.FileUtilities;
import com.lewa.themes.CustomType;

/**
 * A simple helper class the provides an easy way of working with themes.
 *
 * @author T-Mobile USA
 */
public class Themes {
    public static final String AUTHORITY = ThemeManager.THEME_AUTHORITY;
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String KEY_ORIENTATION = "orientation";
    public static final String TAG = "ThemeManager";

    private Themes() {
    }

    /**
     * Creates a Uri for the given theme ids.
     *
     * @param context the context of the caller.
     * @param themeIds the ids of the themes.
     * @return
     */
    public static Uri getThemesUri(Context context, String[] themeIds) {
        StringBuilder themeId = new StringBuilder();
        for (int i = 0; i < themeIds.length; ++i) {
            themeId.append('"').append(themeIds[i]).append('"').append(',');
        }
        return ThemeColumns.CONTENT_URI.buildUpon().appendPath("WOODY@LEWA")
                .appendPath(themeId.substring(0, themeId.length() - 1)).build();
    }

    /**
     * Creates a theme {@link Uri} for the given theme package and id.
     *
     * @param context the context of the caller.
     * @param packageName the package of the theme.
     * @param themeId the id of the theme.
     * @return
     */
    public static Uri getThemeUri(Context context, String packageName, String themeId) {
        if (TextUtils.isEmpty(packageName) && TextUtils.isEmpty(themeId)) {
            return ThemeColumns.CONTENT_URI.buildUpon().appendEncodedPath("system").build();
        } else {
            return ThemeColumns.CONTENT_URI.buildUpon()
                    .appendPath(packageName).appendPath(themeId).build();
        }
    }

    /**
     * Gets a {@link Cursor} for all themes in the provider. Uses the default
     * Projection.
     *
     * @param context the context of the caller.
     * @return a {@link Cursor} for all themes in the provider or null if provider is empty.
     */
    public static Cursor listThemes(Context context) {
        return listThemes(context, null);
    }

    /**
     * Gets a {@link Cursor} for all themes in the provider using the specified
     * projection.
     *
     * @param context the context of the caller.
     * @param projection the Projection for the {@link Cursor}.
     * @return a {@link Cursor} for all themes in the provider using the
     *         specified projection or null if provider is empty.
     */
    public static Cursor listThemes(Context context, String[] projection) {
        return context.getContentResolver().query(
                ThemeColumns.CONTENT_PLURAL_URI, projection, null, null, null);
    }

    /**
     * Gets a {@link Cursor} for themes in the provider filter by the specified
     * package name.
     *
     * @param context the context of the caller.
     * @param packageName the package for which to filter.
     * @return a {@link Cursor} for themes in the provider filter by the
     *         pecified package name or null if provider is empty.
     */
    public static Cursor listThemesByPackage(Context context, String packageName) {
        return context.getContentResolver().query(
                ThemeColumns.CONTENT_PLURAL_URI, null,
                ThemeColumns.THEME_PACKAGE + " = ?",
                new String[] { packageName }, null);
    }

    /**
     * Gets the currently appled theme {@link ThemeItem}
     *
     * @param context the context of the caller.
     * @return a {@link ThemeItem}
     */
    public static ThemeItem getAppliedTheme(Context context) {
        if(ThemeManager.STANDALONE) {
            return ThemeItem.getInstance(context.getContentResolver().query(
                    ThemeColumns.CONTENT_PLURAL_URI, null, ThemeColumns.DOWNLOAD_PATH + " IS NOT NULL", null, null));
        } else {
            return ThemeItem.getInstance(context.getContentResolver().query(
                    ThemeColumns.CONTENT_PLURAL_URI, null,
                    ThemeColumns.IS_APPLIED + "=1", null, null));
        }
    }

    public static boolean isThemePackage(final PackageInfo pi) {
        if (pi == null || pi.reqFeatures == null || pi.reqFeatures.length < 1) {
            return false;
        }

        for (FeatureInfo fi : pi.reqFeatures) {
            if (fi.name == null) {
               return false;
            }
            
            if (fi.name.equals("com.lewa.software.themes")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Deletes a non system theme with the specified package and id.
     *
     * @param context the context of the caller.
     * @param packageName
     * @param themeId
     */
    public static void deleteTheme(Context context, String packageName, String themeId) {
        context.getContentResolver().delete(
                ThemeColumns.CONTENT_PLURAL_URI,
                ThemeColumns.THEME_PACKAGE + " = ? AND " + ThemeColumns.THEME_ID + " = ?",
                new String[] { packageName, themeId });
    }

    /**
     * Deletes non system themes with the specified package.
     *
     * @param context the context of the caller.
     * @param packageName the package for the themes to be deleted.
     */
    public static void deleteThemesByPackage(Context context, String packageName) {
        context.getContentResolver().delete(ThemeColumns.CONTENT_PLURAL_URI,
                ThemeColumns.THEME_PACKAGE + " = ?",
                new String[] { packageName });
    }

    /**
     * Marks a theme as being the applied theme.
     *
     * @param context the context of the caller.
     * @param packageName the package of the theme to apply.
     * @param themeId the id of the theme to apply.
     */
    public static void markAppliedTheme(Context context, String packageName,
            String themeId) {
        ContentValues values = new ContentValues();
        values.put(ThemeColumns.IS_APPLIED, 0);
        context.getContentResolver().update(ThemeColumns.CONTENT_PLURAL_URI, values, null, null);
        values.put(ThemeColumns.IS_APPLIED, 1);
        context.getContentResolver().update(
                ThemeColumns.CONTENT_PLURAL_URI,
                values,
                ThemeColumns.THEME_PACKAGE + " = ? AND " + ThemeColumns.THEME_ID + " = ?",
                new String[] { packageName, themeId });
    }

    /**
     * Request a theme change by broadcasting to the ThemeManager. Must hold
     * permission {@link ThemeConstants#PERMISSION_CHANGE_THEME}.
     */
    public static void changeTheme(Context context, Uri themeUri) {
        changeTheme(context, new Intent(ThemeManager.ACTION_CHANGE_THEME, themeUri));
    }

    /**
     * Changes to the style of the given style {@link Uri}.
     *
     * @param context the context of the caller.
     * @param styleUri the {@link Uri} of the style to apply.
     */
    public static void changeStyle(Context context, Uri styleUri) {
        changeTheme(context
                , new Intent(ThemeManager.ACTION_CHANGE_THEME).setDataAndType(
                styleUri, ThemeColumns.STYLE_CONTENT_ITEM_TYPE));
    }

    /**
     * Alternate API to {@link #changeTheme(Context, Uri)} which allows you to
     * customize the intent that is delivered. This is used to access more
     * advanced functionality like conditionalizing certain parts of the theme
     * that is going to be applied.
     *
     * @param context the context of the caller.
     * @param intent the Intent with extras the specify the conditions to apply.
     */
    public static void changeTheme(Context context, Intent intent) {
        ApplyThemeThread.start(context, intent);
    }
    private static void applyTheme(Context context, Intent intent) {
        ThemeItem item = ThemeItem.getInstance(context, intent.getData());
        int type = intent.getIntExtra(CustomType.EXTRA_NAME , CustomType.THEME_TYPE);
        Uri fontUri = intent.getParcelableExtra(ThemeManager.EXTRA_FONT_URI);
        if(fontUri == null)
            fontUri = item.getFontUril();
        if ((item.getMechanismVersion() <= 0) &&
                (type == CustomType.FONT_TYPE || type == CustomType.THEME_TYPE ||
                        type == CustomType.THEME_DETAIL && fontUri != null)) {
            if (fontUri != null) {
                InputStream in = null;
                OutputStream out = null;
                File tmpFile = null;
                try {
                    File tmpDir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + context.getPackageName() + "/cache");
                    if(!tmpDir.exists())
                        FileUtilities.forceMkdir(tmpDir, true);
                    tmpFile = new File(tmpDir, "tmpFonts");
                    FileUtilities.deleteIfExists(tmpFile);
                    in = context.getContentResolver().openInputStream(fontUri);
                    out = FileUtilities.openOutputStream(tmpFile);
                    FileUtilities.connectIO(in, out);
                    intent.putExtra(ThemeManager.EXTRA_FONT_URI, PackageResources.convertFilePathUri(Uri.parse("file://" + tmpFile.getAbsolutePath())));
                } catch (Exception e) {
                    if (null != tmpFile)
                        try {
                            FileUtilities.deleteIfExists(tmpFile);
                        } catch (Exception ex) {
                        }
                } finally {
                    if (null != in)
                        FileUtilities.close(in);
                    if (null != out)
                        FileUtilities.close(out);
                }
            }
        }
     // Begin, added by yljiang@lewatek.com 2013-11-26 
        if((item.getMechanismVersion() <= 0 ) &&(type ==  CustomType.SYSTEM_APP || type == CustomType.THEME_TYPE || type == CustomType.THEME_DETAIL && intent.getBooleanExtra(ThemeManager.EXTRA_SYSTEM_APP, false))){
            
            if(item.hasThemePackageScope()){
                PackageInfo info;
                try {
                    info = context.getPackageManager().getPackageInfo(item.getPackageName(), 0);
                    if(info.versionCode != item.getVersionCode())
                        info = null;
                } catch (Exception e) {
                    info = null;
                }
                String path = item.getDownloadPath();
                if(info == null && !TextUtils.isEmpty(path) && !path.startsWith(Environment.getDataDirectory().getAbsolutePath())){
                    installTheme(context, path);
                }
            }
        }
     // End
        if(ThemeManager.STANDALONE){
            context.sendOrderedBroadcast(intent, null);
        } else {
            context.sendOrderedBroadcast(intent, Manifest.permission.CHANGE_CONFIGURATION);
        }
    }

    private static class ApplyThemeThread extends Thread{
        private static ApplyThemeThread sInstance;
        private Context mContext;
        private Intent mIntent;
        public static void start(Context context, Intent intent){
            if(sInstance != null && !Thread.interrupted())
                sInstance.interrupt();
            sInstance = new ApplyThemeThread(context, intent);
            sInstance.setPriority(Thread.MIN_PRIORITY);
            sInstance.start();
        }
        private ApplyThemeThread(){
            throw new AssertionError();
        }
        private ApplyThemeThread(Context context, Intent intent){
            mContext = context;
            mIntent = intent;
        }
        @Override
        public void run() {
            applyTheme(mContext, mIntent);
        }
    }
    
    public static boolean installTheme(Context context, String path){
        IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        PackageInstallObserver obs = new PackageInstallObserver();
        try {
            File dir = context.getCacheDir();
            if(!dir.exists())
                FileUtilities.forceMkdir(dir, true);
            File tmp = File.createTempFile("theme", null, dir);
            FileUtils.copyFile(new File(path), tmp);
            FileUtilities.setPermissions(tmp);
            // pm.installPackage(Uri.fromFile(tmp), obs, PackageManager.INSTALL_REPLACE_EXISTING, context.getPackageName());
            synchronized (obs) {
                while(!obs.finished){
                    try {
                        obs.wait();
                    } catch (Exception e) {
                    }
                }
            }
            return obs.result == 1;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }/* catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }*/
        return false;
    }

    private static class PackageInstallObserver extends IPackageInstallObserver.Stub {
        boolean finished;
        int result;

        @Override
        public void packageInstalled(String name, int status) throws RemoteException {
            synchronized (this) {
                finished = true;
                result = status;
                notifyAll();
            }
        }
        
    }

    /**
     * @return Status of a theme
     *
     * @param context the context of the caller.
     * @param themeId the ID of a theme.
     * @param versionCode the version of the online theme
     */
    public static ThemeItem.Status getStatus(Context context, String themeId, String versionCode) {
        return getStatus(context, new String[] { themeId }, new String[] { versionCode })[0];
    }

    /**
     * Check the status of the online themes
     *
     * @param context the context of the caller.
     * @param themeIds the IDs of themes.
     * @param versionCode the versions of the online themes
     *
     * @return An array of theme Status
     */
    public static ThemeItem.Status[] getStatus(
            Context context, String themeIds[], String versionCodes[]) {
        Uri uri = getThemesUri(context, themeIds);
        Cursor c = context.getContentResolver().query(uri,
                new String[] { ThemeColumns.THEME_ID,
                ThemeColumns.VERSION_CODE }, null, null, null);
        ThemeItem.Status[] results = new ThemeItem.Status[themeIds.length];
        java.util.Arrays.fill(results, ThemeItem.Status.NOT_INSTALLED);
        if (null == c) {
            return results;
        }
        if (c.moveToFirst()) {
            int index = -1;
            int start = 0;
            int end = themeIds.length;
            int[] versions = new int[versionCodes.length];
            for (int i = 0; i < versionCodes.length; i++) {
                if(versionCodes[i]==null){
                    versions[i]=-1;
                }else{
                    versions[i] = Integer.parseInt(versionCodes[i]);
                }
            }
            do {
                for(int i=0;i<themeIds.length;i++){
                    if(c.getString(0).equals(themeIds[i])){
                        index=i;
                        break;
                    }
                }
//                index = java.util.Arrays.binarySearch(themeIds, start, end,
//                        c.getString(0));
                if (index >= 0) {
                    // start = index + 1;
                    results[index] = (c.getInt(1) < versions[index]) ? ThemeItem.Status.OUTDATED
                            : ThemeItem.Status.INSTALLED;
                }
            } while (c.moveToNext());
        }
        c.close();
        return results;
    }

    public static ThemeItem getTheme(Context context, String packageName, String themeId) {
        return ThemeItem.getInstance(context, getThemeUri(context, packageName, themeId));
    }

    public interface ThemeColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/theme");

        public static final Uri CONTENT_PLURAL_URI = Uri.parse("content://" + AUTHORITY + "/themes");

        public static final String CONTENT_TYPE = "vnd.lewa.cursor.dir/theme";
        public static final String CONTENT_ITEM_TYPE = "vnd.lewa.cursor.item/theme";

        public static final String STYLE_CONTENT_TYPE = "vnd.lewa.cursor.dir/style";
        public static final String STYLE_CONTENT_ITEM_TYPE = "vnd.lewa.cursor.item/style";

        public static final String _ID = "_id";
        public static final String THEME_ID = "theme_id";
        public static final String THEME_PACKAGE = "theme_package";

        public static final String SIZE = "size";
        public static final String VERSION_CODE = "version_code";
        public static final String VERSION_NAME = "version_name";

        public static final String IS_APPLIED = "is_applied";

        public static final String NAME = "name";
        public static final String STYLE_NAME = "style_name";
        public static final String AUTHOR = "author";
        public static final String IS_DRM = "is_drm";

        public static final String WALLPAPER_NAME = "wallpaper_name";
        public static final String WALLPAPER_URI = "wallpaper_uri";

        public static final String LOCK_WALLPAPER_NAME = "lock_wallpaper_name";
        public static final String LOCK_WALLPAPER_URI = "lock_wallpaper_uri";

        public static final String RINGTONE_NAME = "ringtone_name";
        public static final String RINGTONE_NAME_KEY = "ringtone_name_key";
        public static final String RINGTONE_URI = "ringtone_uri";
        public static final String NOTIFICATION_RINGTONE_NAME = "notif_ringtone_name";
        public static final String NOTIFICATION_RINGTONE_NAME_KEY = "notif_ringtone_name_key";
        public static final String NOTIFICATION_RINGTONE_URI = "notif_ringtone_uri";

        public static final String THUMBNAIL_URI = "thumbnail_uri";
        public static final String PREVIEW_URI = "preview_uri";

        public static final String IS_SYSTEM = "system";

        public static final String FONT_URI = "font_uri";
        public static final String LOCKSCREEN_URI = "lockscreen_uri";
        public static final String BOOT_ANIMATION_URI = "boot_animation_uri";
        public static final String ICONS_URI = "icons_uri";

        // If it's value is -1, it's a single image file which can be set as either wallpaper or lockscreen wallpaper.
        // If the value is 0, the wallpaper is bundled within a LWT package.
        // If the value is larger than 0, the image file is currently being downloaded.
        public static final String IS_IMAGE_FILE = "is_image_file";

        public static final String LIVE_WALLPAPER_URI = "live_wallpaper_uri";

        public static final String DOWNLOAD_PATH = "download_path";

        /**
         * Flag indicating whether this theme has been compiled with assets for
         * the current host system's display density.
         * <p>
         * Because the platform build system by default excludes assets for
         * densities other than the platform target a lot of themes being
         * produced and published in the market lack mdpi assets and would.
         * simply crash on mdpi handsets. This flag was introduced to provide a
         * meaningful error message when the user attempts to apply such a
         * theme.
         */
        public static final String HAS_HOST_DENSITY = "has_host_density";

        /**
         * Flag indicating whether this theme has bene compiled with the
         * expected 0x0a package scope with the special modified aapt provided
         * in the current build system.
         * <p>
         * If themes are compiled using the standard SDK they will have the 0x7f
         * package scope and will create conflicts with regular app packages
         * (ultimately causing crashes after the theme is applied).
         */
        public static final String HAS_THEME_PACKAGE_SCOPE = "has_theme_package_scope";
        
     // Begin, added by yljiang@lewatek.com 2013-11-19 
        public static final String MECHANISM_VERSION = "mechanismVersion";
        
        public static final String APPLY_PACKAGES = "applyPackages";

        public static final String INCALL_STYLE = "inCallStyle";
        public static final String MESSAGE_RINGTONE_NAME = "message_ringtone_name";
        public static final String MESSAGE_RINGTONE_NAME_KEY = "message_ringtone_name_key";
        public static final String MESSAGE_RINGTONE_URI = "message_ringtone_uri";

     // End
    }
}
