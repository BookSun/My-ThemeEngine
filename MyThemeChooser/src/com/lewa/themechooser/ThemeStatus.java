package com.lewa.themechooser;

import android.app.DownloadManager;
import android.app.LewaDownloadManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Downloads;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.lewa.themechooser.newmechanism.Globals;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;

import java.io.File;
import java.util.*;

import lewa.os.FileUtilities;

import static com.lewa.themes.ThemeManager.THEME_LOCKSCREEN2_PACKAGE;
import static com.lewa.themes.ThemeManager.STANDALONE;
import static com.lewa.themes.ThemeManager.THEME_ELEMENTS_PACKAGE;

public final class ThemeStatus {
    public static final int THEME_TYPE_ALL = -1;
    public static final int THEME_TYPE_PACKAGE = 0;
    public static final int THEME_TYPE_WALLPAPER = 1;
    public static final int THEME_TYPE_LOCK_WALLPAPER = 2;
    public static final int THEME_TYPE_LOCK_SCREEN = 3;
    public static final int THEME_TYPE_ICONS = 4;
    public static final int THEME_TYPE_FONT = 5;
    public static final int THEME_TYPE_STYLE = 6;
    public static final int THEME_TYPE_BOOT_ANIM = 7;
    public static final int THEME_TYPES_COUNT = 9;
    public static final int THEME_TYPE_LIVEWALLPAPER = 8;
    public static final int STATUS_UNAVAILABLE = -1;
    public static final int STATUS_INSTALLED = 0;
    public static final int STATUS_DOWNLOADED = 1;
    public static final int STATUS_DOWNLOADING = 2;
    public static final int STATUS_OUTDATED = 3;
    public static final int STATUS_APPLIED = 4;
    private final static boolean DBG = false;
    private final static String TAG = "ThemeStatus";
    private final static String PREFS_NAME = "theme_applied";
    private final static String PREF_PACKAGE = "package";
    private final static String PREF_LOCK_SCREEN = "lock_screen";
    private final static String PREF_LOCK_WALLPAPER = "lock_wallpaper";
    private final static String PREF_ICONS = "icons";
    private final static String PREF_WALLPAPER = "wallpaper";
    private final static String PREF_FONT = "font";
    private final static String PREF_URI_LOCK_SCREEN = "uri_lock_screen";
    private final static String PREF_URI_LOCK_WALLPAPER = "uri_lock_wallpaper";
    private final static String PREF_URI_ICONS = "uri_icons";
    private final static String PREF_URI_WALLPAPER = "uri_wallpaper";
    private final static String PREF_URI_FONT = "uri_font";
    private final static String PREF_URI_STYLE = "uri_style";
    private final static int UNKNOWN_VERSION = Integer.MAX_VALUE;
    private static final String THEME_ROOT_PATH = "/LEWA/theme";
    private static final String THEME_PACKAGE_PATH = THEME_ROOT_PATH + "/lwt";
    private static final String THEME_WALLPAPER_PATH = THEME_ROOT_PATH + "/wallpaper";
    private static final String THEME_WALLPAPER_DOWNLOAD_PATH = THEME_ROOT_PATH + "/deskwallpaper";
    private static final String THEME_LOCK_WALLPAPER_PATH = THEME_ROOT_PATH + "/lock_wallpaper";
    private static final String THEME_PACKAGE_PREFIX = "LEWA_LWT_";
    private static final String THEME_WALLPAPER_PREFIX = "LEWA_WP_";
    private static final String THEME_LOCK_WALLPAPER_PREFIX = "LEWA_LWP_";
    private final static int MAX_CHANGES_BEFORE_PERSISTENCE = 10;
    private static int sChangesCount = 0;
    private static InitStatusThread sThread;
    private Context mContext;
    private HashMap<String, Status> mStatus;
    private HashMap<Long, String> mDownloading;
    private String[] mAppliedPkgNames;
    private String[] mAppliedThumbUris;
    private List<String> mDownloadedWallpapers;
    private OnStatusChangeListener mListener;
    private byte[] mLock = new byte[0];
    private Handler mHandler;

    public ThemeStatus(Context context) {
        mContext = context;
        mAppliedPkgNames = new String[THEME_TYPES_COUNT];
        mAppliedThumbUris = new String[THEME_TYPES_COUNT];
        mDownloadedWallpapers = new ArrayList<String>();
        mStatus = new HashMap<String, Status>();
        mDownloading = new HashMap<Long, String>();
        mHandler = new Handler();
        initStatus(this);
    }

    private static void updateStatus(ThemeStatus status) {
        if (sThread != null && !sThread.isInterrupted())
            sThread.interrupt();
        (sThread = new InitStatusThread(status)).start();
    }

    public void setOnStatusChangeListener(OnStatusChangeListener listener) {
        mListener = listener;
    }

    public OnStatusChangeListener getStatusChangeListener() {
        return mListener;
    }

    public void setDeleted(String name) {
        if (DBG) {
            Log.d(TAG, "Deleted " + name);
        }
        if (null != mStatus.remove(name)) {
            mDownloadedWallpapers.remove(
                    Environment.getExternalStorageDirectory().getAbsolutePath()
                            + ThemeConstants.WALLPAPER_PATH + name
            );
            countChange();
        }
    }

    public void removeDownloaded(String name) {
        if (name != null) {
            mStatus.remove(name);
        }
    }

    public void setDeleted(String name, String file, int type) {
        if (DBG) {
            Log.d(TAG, "Deleted " + name + " " + file);
        }
        boolean deleted1 = null != mStatus.remove(name);
        boolean deleted2 = null != mStatus.remove(typedName(file, type));
        if (type == THEME_TYPE_WALLPAPER) {
            mDownloadedWallpapers.remove(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + ThemeConstants.WALLPAPER_PATH + file);
        }
        if (deleted2 || deleted1) {
            countChange();
        }
    }

    // Parameter name is not currently used
    public void setDownloading(long id, String name, String file, int type) {
        mDownloading.put(id, typedName(file, type));
        mStatus.put(typedName(file, type), new Status(STATUS_DOWNLOADING, id));
        countChange();
        if (DBG) {
            Log.d(TAG, "Start downloading " + name + ",file=" + file + " " + id);
        }
    }

    public long getDownloadId(String name, String file, int type) {
        Status s = mStatus.get(typedName(file, type));
        return null != s ? s.versionCode : -1;
    }

    public void setDownloaded(long id) {
        String name = mDownloading.remove(id);
        if (DBG) {
            Log.d(TAG, "Finished downloading " + name);
        }
        if (null != name) {
            mStatus.put(name, new Status(STATUS_DOWNLOADED, UNKNOWN_VERSION));
            /*
             * if (name.startsWith(THEME_WALLPAPER_PREFIX)) {
             *     mDownloadedWallpapers.add(0
             *             , Environment.getExternalStorageDirectory().getAbsolutePath()
             *             + ThemeConstants.WALLPAPER_PATH
             *             + name.substring(THEME_WALLPAPER_PREFIX.length(), name.length()));
             * }
             */
            countChange();
        }
    }

    public void setDownloaded(String name) {
        if (null != name) {
            mStatus.put(name, new Status(STATUS_DOWNLOADED, UNKNOWN_VERSION));
            /*
             * if (name.startsWith(THEME_WALLPAPER_PREFIX)) {
             *     mDownloadedWallpapers.add(0
             *             , Environment.getExternalStorageDirectory().getAbsolutePath()
             *             + ThemeConstants.WALLPAPER_PATH
             *             + name.substring(THEME_WALLPAPER_PREFIX.length(), name.length()));
             * }
             */
            countChange();
        }
    }

    public void setDownloadingCancelled(long id) {
        if (DBG) {
            Log.d(TAG, "Cancel downloading " + mDownloading.get(id));
        }
        String name = mDownloading.remove(id);
        if (null != name) {
            mStatus.remove(name);
            countChange();
        }
    }

    public boolean isDownloaded(String name) {
        return mStatus.containsKey(name) && !mDownloading.containsValue(name);
    }

    public boolean isDownloading(long id) {
        return mDownloading.containsKey(id);
    }

    public boolean isDownloaded(String name, String fileName, int type) {
        return isDownloaded(name) || isDownloaded(typedName(fileName, type))
                //TODO: Someday we should remove the following since the downloaded
                //wallpaper will have the suffix in the file name
                || (THEME_TYPE_WALLPAPER == type && fileName.lastIndexOf('.') > 0
                && isDownloaded(typedName(fileName.substring(0, fileName.lastIndexOf('.')), type)));
    }

    /**
     * check if this theme is exits
     *
     * @param file file name
     * @return true if this file is exits , false if this file is not exits
     */
    public boolean isNotExits(String file) {
        File sdcardRoot = Environment.getExternalStorageDirectory();
        String themeName = file.substring(0, file.length() - 4);
        File[] pkgs = (new File(sdcardRoot.getPath() + THEME_PACKAGE_PATH)).listFiles();
        if (pkgs != null) {
            for (File f : pkgs) {
                if (f.getName().contains(themeName)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isDownloading(String name, int onlineVersion) {
        StringBuffer fileName = new StringBuffer();
        String fileEnd = ".lwt";
        int index = name.indexOf(fileEnd);
        if (index > 0) {
            fileName = fileName.append(name.substring(0, index)).append("_v").append(onlineVersion)
                    .append(fileEnd);
        } else {
            fileName = fileName.append(name).append("_v").append(onlineVersion).append(fileEnd);
        }
        boolean isTrue =
                mDownloading.containsValue(fileName.toString()) || mDownloading.containsValue(name);
        return isTrue;
    }

    public boolean isDownloading(String name, String file, int type, int onlineVersion) {
        boolean isTrue = isDownloading(name, onlineVersion) ||
                isDownloading(typedName(file, type), onlineVersion);
        return isTrue;
    }

    public boolean isOutdated(String name, int onlineVersion) {
        Status s = mStatus.get(name);
        if (null == s || STATUS_DOWNLOADING == s.internalStatus
                || s.versionCode >= onlineVersion)
            return false;
        return true;
    }

    public boolean isOutdated(String name, String file, int type, int onlineVersion) {
        return isOutdated(name, onlineVersion) || isOutdated(typedName(file, type), onlineVersion);
    }

    public boolean isApplied(String name) {
        return isApplied(name, THEME_TYPE_PACKAGE) || isApplied(name, THEME_TYPE_LOCK_WALLPAPER)
                || isApplied(name, THEME_TYPE_WALLPAPER) || isApplied(name, THEME_TYPE_STYLE)
                || isApplied(name, THEME_TYPE_FONT) || isApplied(name, THEME_TYPE_LOCK_SCREEN)
                || isApplied(name, THEME_TYPE_ICONS) || isApplied(name, THEME_TYPE_LIVEWALLPAPER)
                ;
    }

    public boolean isApplied(String name, int type) {
        return !TextUtils.isEmpty(mAppliedPkgNames[type]) && mAppliedPkgNames[type].equals(name);
    }

    public boolean isWallpaperApplied(String name, int type) {
        if (null == mAppliedThumbUris[type]) {
            return false;
        }
        return !TextUtils.isEmpty(mAppliedThumbUris[type]) &&
                mAppliedThumbUris[type].contains(name);
    }

    public boolean isApplied(String name, String file, int type) {
        return isApplied(name, type == THEME_TYPE_LIVEWALLPAPER ? THEME_TYPE_WALLPAPER : type)
                || (THEME_TYPE_WALLPAPER == type && isApplied(typedName(file, type), type)
                || (THEME_TYPE_WALLPAPER == type && isWallpaperApplied(name, type))
                || (THEME_TYPE_WALLPAPER == type && isWallpaperSettedByPath(name))
        );
    }

    public boolean isWallpaperSettedByPath(String name) {
        String full_path = ThemeConstants.THEME_WALLPAPER + File.separator + name;
        String wallpaper_path = Settings.System.getString(
                mContext.getContentResolver(), "lewa_wallpaper_path");
        if (wallpaper_path == null) {
            return false;
        }
        return wallpaper_path.contains(full_path);
    }

    public String getAppliedPkgName(int type) {
        return mAppliedPkgNames[type];
    }

    public void setAppliedPkgName(String name, int type) {
        mAppliedPkgNames[type] = name;
        countChange();
    }

    public String getAppliedThumbnail(int type) {
        return mAppliedThumbUris[type];
    }

    public void setAppliedThumbnail(Uri uri, int type) {
        try {
            setAppliedThumbnail(null != uri ? uri.toString() : "", type);
            countChange();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAppliedThumbnail(String uri, int type) {
        if (DBG) {
            Log.d(TAG, "Set " + typeLabel(type) + " thumb to: " + uri);
        }
        mAppliedThumbUris[type] = uri;
        int pos = -1, pos2 = -1;
        switch (type) {
            case THEME_TYPE_LOCK_WALLPAPER:
                if (TextUtils.isEmpty(uri)) {
                    // Applied a lockscreen, so to clear info of lock wallpaper here
                    mAppliedPkgNames[type] = null;
                } else if (uri.startsWith("file")) {
                    // There is a wallpaper in the lockscreen itself
                    if (TextUtils.isEmpty(mAppliedPkgNames[type])
                            || !mAppliedPkgNames[type].equals("external_resource")) {
                        mAppliedPkgNames[type] = mAppliedPkgNames[THEME_TYPE_LOCK_WALLPAPER];
                    }
                } else {
                    pos = uri.indexOf("packageresources");
                    if (pos >= 0) {
                        pos += 17;
                        pos2 = uri.indexOf('/', pos);
                        mAppliedPkgNames[type] = uri.substring(pos, pos2);
                    } else {
                        mAppliedPkgNames[type] = "external_resource";
                        sChangesCount++;
                        persistPreferencesAsync();
                    }
                }
                break;
            case THEME_TYPE_WALLPAPER:
                if (TextUtils.isEmpty(uri)) {
                    mAppliedPkgNames[type] = null;
                    persistPreferencesAsync();
                    break;
                } else if (0 >= uri.indexOf('/')) {
                    //RC48063-jianwu.gao modify begin
                    //fix bug : reset wallpaper to default after set font
                    mAppliedThumbUris[type] = uri;
                    //RC48063-jianwu.gao modify end
                    mAppliedPkgNames[type] = typedName(uri, THEME_TYPE_WALLPAPER);
//                    mAppliedPkgNames[type] = uri;
                    persistPreferencesAsync();
                    break;
                    //RC48063-jianwu.gao modify begin
                    //fix bug : reset wallpaper to default after set font
                } else if (uri.startsWith("file")) {
                    mAppliedThumbUris[type] = uri;
                    mAppliedPkgNames[type] = uri;
                    //RC48063-jianwu.gao modify end
                }
            default:
                if (uri != null) {
                    pos = uri.indexOf("packageresources");
                    pos += 17;
                    pos2 = uri.indexOf('/', pos);
                    try {
                        String pkgName = uri.substring(pos, pos2);
                        if (pkgName.equals(THEME_ELEMENTS_PACKAGE) && uri.contains("lockscreen2")) {
                            mAppliedPkgNames[type] = THEME_LOCKSCREEN2_PACKAGE;
                        } else {
                            mAppliedPkgNames[type] = uri.substring(pos, pos2);
                        }
                    } catch (StringIndexOutOfBoundsException exception) {
                        exception.toString();
                    }
                    if (uri.startsWith("com"))
                        mAppliedPkgNames[type] = uri;
                }
        }
        if (DBG) {
            dumpApplied(type);
        }
        countChange();
    }

    public int getStatus(String name, String file, int type) {
        return getStatus(name, file, type, UNKNOWN_VERSION);
    }

    public int getStatus(String name, String file, int type, int onlineVersion) {
        if (isApplied(name, file, type)) {
            return STATUS_APPLIED;
        }
        if (isOutdated(name, file, type, onlineVersion)) {
            return STATUS_OUTDATED;
        }
        if (isDownloading(name, file, type, onlineVersion)) {
            return STATUS_DOWNLOADING;
        }
        if (type == THEME_TYPE_PACKAGE && isNotExits(file)) {
            return STATUS_UNAVAILABLE;
        }
        if (isDownloaded(name, file, type)&&isDownloadFileExist(name, type)) {
            return STATUS_DOWNLOADED;
        }
        return STATUS_UNAVAILABLE;
    }

    private boolean isDownloadFileExist(String fileName, int type) {
        String path = null;
        if (type == ThemeStatus.THEME_TYPE_WALLPAPER) {
            path = ThemeConstants.WALLPAPER_PATH;
        } else if (type == ThemeStatus.THEME_TYPE_LOCK_WALLPAPER) {
            path = ThemeConstants.LOCKWALLPAPER_PATH;
        } else {
            return true;
        }
        path = Environment.getExternalStorageDirectory().getAbsolutePath()
                + path + fileName + Globals.JPG;
        File file = new File(path);
        return file.exists() ? true : false;
    }

    public void persistPreferencesAsync() {
        if (DBG) {
            Log.d(TAG, "persistPreferencesAsync " + sChangesCount);
        }
        new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                persistPreferences();
            }
        }.start();
    }

    public void persistPreferences() {
        if (DBG) {
            Log.d(TAG, "persistPreferences " + sChangesCount);
        }
        synchronized (mLock) {
            if (sChangesCount <= 0) {
                return;
            }
            android.content.SharedPreferences.Editor editor = mContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE).edit();

            editor.putString(PREF_PACKAGE, mAppliedPkgNames[THEME_TYPE_PACKAGE]);
            editor.putString(PREF_LOCK_SCREEN, mAppliedPkgNames[THEME_TYPE_LOCK_SCREEN]);
            editor.putString(PREF_LOCK_WALLPAPER, mAppliedPkgNames[THEME_TYPE_LOCK_WALLPAPER]);
            editor.putString(PREF_ICONS, mAppliedPkgNames[THEME_TYPE_ICONS]);
            editor.putString(PREF_WALLPAPER, mAppliedPkgNames[THEME_TYPE_WALLPAPER]);
            editor.putString(PREF_FONT, mAppliedPkgNames[THEME_TYPE_FONT]);

            editor.putString(PREF_URI_STYLE, mAppliedThumbUris[THEME_TYPE_STYLE]);
            editor.putString(PREF_URI_LOCK_SCREEN, mAppliedThumbUris[THEME_TYPE_LOCK_SCREEN]);
            editor.putString(PREF_URI_LOCK_WALLPAPER, mAppliedThumbUris[THEME_TYPE_LOCK_WALLPAPER]);
            editor.putString(PREF_URI_ICONS, mAppliedThumbUris[THEME_TYPE_ICONS]);
            editor.putString(PREF_URI_WALLPAPER, mAppliedThumbUris[THEME_TYPE_WALLPAPER]);
            editor.putString(PREF_URI_FONT, mAppliedThumbUris[THEME_TYPE_FONT]);

            editor.commit();
            sChangesCount = 0;
        }
    }

    public List<String> getDownloadedWallpapers() {
        return mDownloadedWallpapers;
    }

    private void countChange() {
        if (null != mListener) {
            mListener.onStatusChange();
        }
        if (sChangesCount++ > MAX_CHANGES_BEFORE_PERSISTENCE) {
            persistPreferencesAsync();
        }
    }

    private void checkAppliedTheme() {
        mAppliedPkgNames[THEME_TYPE_STYLE] = SystemProperties.get(
                "persist.sys.themePackageName", "com.lewa.theme.LewaDefaultTheme");

        android.content.SharedPreferences prefs = mContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);
        mAppliedPkgNames[THEME_TYPE_PACKAGE] = prefs
                .getString(PREF_PACKAGE, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedPkgNames[THEME_TYPE_LOCK_SCREEN] = prefs
                .getString(PREF_LOCK_SCREEN, STANDALONE ? THEME_LOCKSCREEN2_PACKAGE : null);
        mAppliedPkgNames[THEME_TYPE_LOCK_WALLPAPER] = prefs
                .getString(PREF_LOCK_WALLPAPER, STANDALONE ? THEME_LOCKSCREEN2_PACKAGE : null);
        mAppliedPkgNames[THEME_TYPE_ICONS] = prefs
                .getString(PREF_ICONS, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedPkgNames[THEME_TYPE_WALLPAPER] = prefs
                .getString(PREF_WALLPAPER, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedPkgNames[THEME_TYPE_FONT] = prefs
                .getString(PREF_FONT, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        if (WallpaperManager.getInstance(mContext).getWallpaperInfo() != null) {
            mAppliedPkgNames[THEME_TYPE_LIVEWALLPAPER]
                    = WallpaperManager.getInstance(mContext).getWallpaperInfo().getServiceName();
        }
        mAppliedThumbUris[THEME_TYPE_STYLE] = prefs
                .getString(PREF_URI_STYLE, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedThumbUris[THEME_TYPE_LOCK_SCREEN] = prefs
                .getString(PREF_URI_LOCK_SCREEN, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedThumbUris[THEME_TYPE_LOCK_WALLPAPER] = prefs
                .getString(PREF_URI_LOCK_WALLPAPER, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedThumbUris[THEME_TYPE_ICONS] = prefs
                .getString(PREF_URI_ICONS, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedThumbUris[THEME_TYPE_WALLPAPER] = prefs
                .getString(PREF_URI_WALLPAPER, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);
        mAppliedThumbUris[THEME_TYPE_FONT] = prefs
                .getString(PREF_URI_FONT, STANDALONE ? THEME_ELEMENTS_PACKAGE : null);

        //TODO: Remove the following someday
        if (TextUtils.isEmpty(mAppliedPkgNames[THEME_TYPE_PACKAGE])) {
            android.content.SharedPreferences prefsDownloaded = mContext
                    .getSharedPreferences("DOWNLOADED", Context.MODE_PRIVATE);
            mAppliedPkgNames[THEME_TYPE_PACKAGE] = prefsDownloaded
                    .getString("applied_themePkgName", "com.lewa.theme.LewaDefaultTheme");
            for (int i = THEME_TYPE_PACKAGE + 1; i < THEME_TYPES_COUNT - 1; ++i) {
                mAppliedPkgNames[i] = mAppliedPkgNames[THEME_TYPE_PACKAGE];
            }
            mAppliedPkgNames[THEME_TYPE_LOCK_WALLPAPER] = "com.lewa.theme.LewaDefaultTheme";

            int pos = -1, pos2 = -1;
            android.content.SharedPreferences prefsCustom = mContext
                    .getSharedPreferences("CUSTOM_URI", Context.MODE_PRIVATE);
            String s = prefsCustom.getString("thumbnail_font_uri", null);
            if (!TextUtils.isEmpty(s)) {
                pos = s.indexOf("packageresources");
                pos += 17;
                pos2 = s.indexOf('/', pos);
                mAppliedThumbUris[THEME_TYPE_FONT] = s;
                mAppliedPkgNames[THEME_TYPE_FONT] = s.substring(pos, pos2);
            }

            s = prefsCustom.getString("thumbnail_lockscreen_style_uri", null);
            if (!TextUtils.isEmpty(s)) {
                pos = s.indexOf("packageresources");
                pos += 17;
                pos2 = s.indexOf('/', pos);
                mAppliedThumbUris[THEME_TYPE_LOCK_SCREEN] = s;
                mAppliedPkgNames[THEME_TYPE_LOCK_SCREEN] = s.substring(pos, pos2);
            }

            s = prefsCustom.getString("thumbnail_icon_uri", null);
            if (!TextUtils.isEmpty(s)) {
                pos = s.indexOf("packageresources");
                pos += 17;
                pos2 = s.indexOf('/', pos);
                mAppliedThumbUris[THEME_TYPE_ICONS] = s;
                mAppliedPkgNames[THEME_TYPE_ICONS] = s.substring(pos, pos2);
            }

            s = prefsCustom.getString("thumbnail_system_app_uri", null);
            /*
             * int pos = s.indexOf("packageresources");
             * pos += 17;
             * int pos2 = s.indexOf('/', pos);
             */
            mAppliedThumbUris[THEME_TYPE_STYLE] = s;
            // mAppliedPkgNames[THEME_TYPE_STYLE] = s.substring(pos, pos2);

            s = prefsCustom.getString("lockscreen_wallpaper_uri", null);
            if (!TextUtils.isEmpty(s)) {
                if (s.startsWith("file")) {
                    mAppliedPkgNames[THEME_TYPE_LOCK_WALLPAPER]
                            = mAppliedPkgNames[THEME_TYPE_LOCK_SCREEN];
                } else {
                    pos = s.indexOf("packageresources");
                    pos += 17;
                    pos2 = s.indexOf('/', pos);
                    mAppliedPkgNames[THEME_TYPE_LOCK_WALLPAPER] = s.substring(pos, pos2);
                }
                mAppliedThumbUris[THEME_TYPE_LOCK_WALLPAPER] = s;
            }

            s = prefsCustom.getString("desktop_wallpaper_uri", null);
            if (!TextUtils.isEmpty(s)) {
                pos = s.indexOf("packageresources");
                pos += 17;
                pos2 = s.indexOf('/', pos);
                mAppliedThumbUris[THEME_TYPE_WALLPAPER] = s;
                mAppliedPkgNames[THEME_TYPE_WALLPAPER] = s.substring(pos, pos2);
            }
        }
    }

    private void checkDownloadingThemes() {
        Cursor c = null;
        try {
            LewaDownloadManager dm = LewaDownloadManager
                    .getInstance(mContext.getContentResolver(), mContext.getPackageName());
            LewaDownloadManager.Query query = new LewaDownloadManager.Query();
            query.setFilterByStatus(DownloadManager.STATUS_RUNNING
                    | DownloadManager.STATUS_PENDING | DownloadManager.STATUS_PAUSED);
            c = dm.query(query);
            // c = mContext.getContentResolver().query(Downloads.Impl.CONTENT_URI
            // , new String[] { Downloads.Impl._ID, Downloads.Impl.COLUMN_FILE_NAME_HINT }
            // , Downloads.Impl.COLUMN_STATUS + " <> ? AND " + Downloads.Impl.COLUMN_STATUS
            // + " <> ?"[>  + " AND " + Downloads.Impl.COLUMN_FILE_NAME_HINT + " LIKE '%LEWA/theme%'" <]
            // , new String[] { String.valueOf(DownloadManager.STATUS_FAILED)
            // , String.valueOf(DownloadManager.STATUS_SUCCESSFUL) }, null);

            if (c.moveToFirst()) {
                String s;
                int pos;
                long id;
                int indexId = c.getColumnIndex(Downloads.Impl._ID);
                int indexHint = c.getColumnIndex(Downloads.Impl.COLUMN_FILE_NAME_HINT);
                if (DBG) {
                    Log.d(TAG, "Existing downloads: ");
                }
                do {
                    s = c.getString(indexHint);
                    if (!s.contains("/LEWA/theme")) {
                        continue;
                    }
                    pos = s.lastIndexOf('/') + 1;
                    if (pos <= 0) {
                        continue;
                    }
                    s = s.substring(pos, s.length());
                    s = (s.endsWith("lwt") ? THEME_PACKAGE_PREFIX : THEME_WALLPAPER_PREFIX) + s;
                    id = c.getLong(indexId);
                    mDownloading.put(id, s);
                    mStatus.put(s, new Status(STATUS_DOWNLOADING, id));
                    if (DBG) {
                        Log.d(TAG, "\t" + s + " " + id);
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Query downloading themes failed " + e);
        } finally {
            if (null != c)
                c.close();
        }
    }

    private void checkDownloadedThemes(int type) {
        File sdcardRoot = Environment.getExternalStorageDirectory();
        if (sdcardRoot.exists()) {
            try {
                FileUtilities.forceMkdir(
                        new File(sdcardRoot.getPath() + THEME_PACKAGE_PATH), true);
                FileUtilities.forceMkdir(
                        new File(sdcardRoot.getPath() + THEME_WALLPAPER_PATH), true);
                // FileUtilities.forceMkdir(
                // new File(sdcardRoot.getPath() + THEME_LOCK_WALLPAPER_PATH), true);
            } catch (Exception e) {
            }

            if (THEME_TYPE_ALL == type || THEME_TYPE_PACKAGE == type) {
                File[] pkgs = (new File(sdcardRoot.getPath() + THEME_PACKAGE_PATH)).listFiles();
                if (pkgs != null) {
                    for (File f : pkgs) {
                        mStatus.put(THEME_PACKAGE_PREFIX + f.getName()
                                , new Status(STATUS_DOWNLOADED, UNKNOWN_VERSION));
                    }
                }
            }

            /*
             * if (THEME_TYPE_ALL == type || THEME_TYPE_WALLPAPER == type) {
             *     File[] pkgs = (new File(sdcardRoot.getPath() + THEME_WALLPAPER_PATH)).listFiles();
             *     if (pkgs != null) {
             *         Arrays.sort(pkgs, new Comparator<File>() {
             *             public int compare(File f1, File f2) {
             *                 return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
             *             }
             *         });
             *         for (File f : pkgs) {
             *             mStatus.put(THEME_WALLPAPER_PREFIX + f.getName()
             *                     , new Status(STATUS_DOWNLOADED, UNKNOWN_VERSION));
             *             mDownloadedWallpapers.add(f.getAbsolutePath());
             *         }
             *     }
             * }
             */
        }
    }

    private void checkInstalledThemes() {
        Cursor c = null;
        try {
            c = Themes.listThemes(mContext, new String[]
                    { ThemeColumns.THEME_PACKAGE, ThemeColumns.VERSION_CODE, ThemeColumns.THEME_ID,
                            ThemeColumns.LIVE_WALLPAPER_URI
                    });
            if (c.moveToFirst()) {
                do {
                    if (c.getString(3) != null) {
                        mStatus.put(c.getString(0) + c.getString(2)
                                , new Status(STATUS_DOWNLOADED, c.getInt(1)));
                    } else {
                        mStatus.put(c.getString(0)
                                , new Status(STATUS_DOWNLOADED, c.getInt(1)));
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Query installed themes failed " + e);
        } finally {
            if (null != c)
                c.close();
        }
    }

    private void dumpStatus() {
        Log.d(TAG, "Theme Status: ");
        for (String s : mStatus.keySet()) {
            Status ss = mStatus.get(s);
            Log.d(TAG, "\t" + ss.versionCode
                    + "\t" + statusString(ss.internalStatus)
                    + "\t" + s);
        }
    }

    private void dumpApplied() {
        dumpApplied(THEME_TYPE_ALL);
    }

    private void dumpApplied(int type) {
        if (THEME_TYPE_ALL == type) {
            Log.d(TAG, "Applied theme: ");
            int i = 0;

            for (String s : mAppliedPkgNames) {
                Log.d(TAG, "\t " + typeLabel(i) + ":\t" + s + "\t" + mAppliedThumbUris[i]);
                i++;
            }
        } else {
            Log.d(TAG, typeLabel(type) + ":\t" + mAppliedPkgNames[type] + "\t" +
                    mAppliedThumbUris[type]);
        }
    }

    private String typeLabel(int type) {
        String[] labels = new String[] { "Package", "Wallpaper", "Lockscreen Wallpaper"
                , "Lockscreen", "Icons", "Font", "Style", "Boot animation", "Live Wallpaper"
        };
        return labels[type];
    }

    private String statusString(int statusCode) {
        String status = "";
        switch (statusCode) {
            case STATUS_APPLIED:
                status = "Applied";
                break;
            case STATUS_DOWNLOADED:
                status = "Downloaded";
                break;
            case STATUS_OUTDATED:
                status = "New version available";
                break;
            case STATUS_INSTALLED:
                status = "Installed";
                break;
            case STATUS_DOWNLOADING:
                status = "Downloading";
                break;
            default:
                status = "Unavailable";
        }
        return status;
    }

    public static String typedName(String name, int type) {
        String key;
        switch (type) {
            case THEME_TYPE_WALLPAPER:
                key = THEME_WALLPAPER_PREFIX + name;
                break;
            case THEME_TYPE_LOCK_WALLPAPER:
                key = THEME_LOCK_WALLPAPER_PREFIX + name;
                break;
            default:
                key = THEME_PACKAGE_PREFIX + name;
        }
        return key;
    }

    public void setWallpaperIsUsingFlag(String wallpaperPackageName) {
        mContext.getSharedPreferences(PREFS_NAME, mContext.MODE_WORLD_READABLE).edit()
                .putString(ThemeStatus.PREF_WALLPAPER, wallpaperPackageName).commit();
    }

    public interface OnStatusChangeListener {
        public void onStatusChange();
    }

    private static class Status {
        int internalStatus;
        long versionCode;

        public Status(int internalStatus, long versionCode) {
            this.internalStatus = internalStatus;
            this.versionCode = versionCode;
        }
    }

    // Add by Fan.Yang #65380
    private static final HandlerThread sStatusThread = new HandlerThread("Status-Thread");

    static {
        sStatusThread.start();
    }

    private static final Handler statusHandler = new Handler(sStatusThread.getLooper());

    private static void runOnStatusThread(Runnable r) {
        if (sStatusThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            statusHandler.post(r);
        }
    }

    private ArrayList<Long> mFailed = new ArrayList<Long>();

    private void initStatus(final ThemeStatus status) {
        runOnStatusThread(new Runnable() {
            @Override
            public void run() {
                // Clear
                status.mStatus.clear();

                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                status.checkAppliedTheme();

                // Check downloaded themes on SDCard
                status.checkDownloadedThemes(THEME_TYPE_ALL);

                // Query theme_item.db to get versions of installed themes
                status.checkInstalledThemes();

                status.checkDownloadingThemes();

                if (DBG) {
                    status.dumpStatus();
                    status.dumpApplied();
                }
            }
        });
    }

    public void checkDownloadStatus() {
        checkDownloadStatus(this);
    }

    private void checkDownloadStatus(final ThemeStatus status) {
        runOnStatusThread(new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                ArrayList<Long> ret = status.checkDownloadingStatus();
                if (!ret.isEmpty()) {
                    status.updateDownloadingStatus();
                }
            }
        });
    }

    // themes may deleted when downloading,check download status when resume
    private ArrayList<Long> checkDownloadingStatus() {
        synchronized (mFailed) {
            HashMap<Long, String> copy = new HashMap<Long, String>(mDownloading);
            mFailed.clear();
            LewaDownloadManager dm = LewaDownloadManager
                    .getInstance(mContext.getContentResolver(), mContext.getPackageName());
            if (dm instanceof LewaDownloadManager) {
                Iterator<Map.Entry<Long, String>> it = copy.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Long, String> entry = it.next();
                    long id = entry.getKey();
                    int status = dm.getStatusById(id);
                    if (DownloadManager.STATUS_FAILED == status) {
                        it.remove();
                        mFailed.add(id);
                    }
                }
            }
            return mFailed;
        }
    }

    private void updateDownloadingStatus() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                OnStatusChangeListener listener = getStatusChangeListener();
                for (Long id : mFailed) {
                    mDownloading.remove(id);
                }

                Iterator<Map.Entry<String, Status>> it = mStatus.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Status> entry = it.next();
                    Status status = entry.getValue();
                    if (status.internalStatus == STATUS_DOWNLOADING) {
                        it.remove();
                    }
                }
                if (listener != null && !mFailed.isEmpty()) {
                    listener.onStatusChange();
                }
            }
        });
    }

    // Abandoned by Fan.Yang
    private static class InitStatusThread extends Thread {
        private ThemeStatus mThemeStatus;

        public InitStatusThread(ThemeStatus status) {
            mThemeStatus = status;
            status.mStatus.clear();
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            mThemeStatus.checkAppliedTheme();

            // Check downloaded themes on SDCard
            mThemeStatus.checkDownloadedThemes(THEME_TYPE_ALL);

            // Query theme_item.db to get versions of installed themes
            mThemeStatus.checkInstalledThemes();

            mThemeStatus.checkDownloadingThemes();

            /*
             * if (null != mListener) {
             *     mListener.onStatusChange();
             * }
             */

            if (DBG) {
                mThemeStatus.dumpStatus();
                mThemeStatus.dumpApplied();
            }
        }

    }
}
