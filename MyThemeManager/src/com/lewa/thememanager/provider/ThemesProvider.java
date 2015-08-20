package com.lewa.thememanager.provider;

import com.lewa.thememanager.Constants;
import com.lewa.thememanager.utils.DatabaseUtilities;
import com.lewa.thememanager.utils.InnerFontUtil;
import com.lewa.thememanager.utils.ThemeUtilities;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.Utils;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;

import android.app.WallpaperInfo;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ThemeInfo;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Environment;
import android.service.wallpaper.WallpaperService;
import lewa.content.ExtraIntent;

import static com.lewa.themes.ThemeManager.STANDALONE;
import static com.lewa.themes.Utils.parsePackage;

/**
 * Provider
 */
public class ThemesProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final File INTERNAL_WALLPAPER = new File(Environment.getRootDirectory(), "/media/wallpapers/");
    private static final File INTERNAL_LOCKWALLPAPER = new File(Environment.getRootDirectory(), "/media/lockwallpapers/");

    public static final String TABLE_NAME = "themeitem_map";

    private static final int TYPE_THEMES = 0;
    private static final int TYPE_THEME = 1;
    private static final int TYPE_THEME_SYSTEM = 2;

    private static final String sDefaultThemeSelection
            // = "LENGTH(" + ThemeColumns.THEME_PACKAGE + ") = 0";
            = ThemeColumns.THEME_ID + " = 'LewaDefaultTheme'";

    private SQLiteOpenHelper mOpenHelper;

    private static final String THEME_ROOT_PATH = "/LEWA/theme";
    public static final String THEME_PACKAGE_PATH = THEME_ROOT_PATH + "/lwt";

    private final Handler mHandler = new Handler();

    public static class OpenDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "theme_item.db";
        private static final int DATABASE_VERSION = 21 ;

        public OpenDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Creates database the first time we try to open it.
         */
        @Override
        public void onCreate(final SQLiteDatabase db) {
            createTables(db);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldVer, int newVer) {
            dropTables(db);
            createTables(db);
        }

        private void createTables(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE themeitem_map (" +
                    ThemeColumns._ID + " INTEGER PRIMARY KEY, " +
                    ThemeColumns.THEME_PACKAGE + " TEXT NOT NULL, " +
                    ThemeColumns.THEME_ID + " TEXT, " +
                    ThemeColumns.IS_APPLIED + " INTEGER DEFAULT 0, " +
                    ThemeColumns.AUTHOR + " TEXT NOT NULL, " +
                    ThemeColumns.IS_DRM + " INTEGER DEFAULT 0, " +
                    ThemeColumns.IS_SYSTEM + " INTEGER DEFAULT 0, " +
                    ThemeColumns.NAME + " TEXT NOT NULL, " +
                    ThemeColumns.STYLE_NAME + " TEXT NOT NULL, " +
                    ThemeColumns.WALLPAPER_NAME + " TEXT, " +
                    ThemeColumns.WALLPAPER_URI + " TEXT, " +
                    ThemeColumns.LOCK_WALLPAPER_NAME + " TEXT, " +
                    ThemeColumns.LOCK_WALLPAPER_URI + " TEXT, " +
                    ThemeColumns.RINGTONE_NAME + " TEXT, " +
                    ThemeColumns.RINGTONE_NAME_KEY + " TEXT, " +
                    ThemeColumns.RINGTONE_URI + " TEXT, " +
                    ThemeColumns.NOTIFICATION_RINGTONE_NAME + " TEXT, " +
                    ThemeColumns.NOTIFICATION_RINGTONE_NAME_KEY + " TEXT, " +
                    ThemeColumns.NOTIFICATION_RINGTONE_URI + " TEXT, " +
                    ThemeColumns.THUMBNAIL_URI + " TEXT, " +
                    ThemeColumns.PREVIEW_URI + " TEXT, " +
                    ThemeColumns.HAS_HOST_DENSITY + " INTEGER DEFAULT 1, " +
                    ThemeColumns.HAS_THEME_PACKAGE_SCOPE + " INTEGER DEFAULT 0, " +
                    ThemeColumns.BOOT_ANIMATION_URI + " TEXT, " +
                    ThemeColumns.FONT_URI + " TEXT, " +
                    ThemeColumns.LOCKSCREEN_URI + " TEXT, " +
                    ThemeColumns.SIZE + " INTEGER DEFAULT 0, " +
                    ThemeColumns.VERSION_CODE + " INTEGER DEFAULT 1, " +
                    ThemeColumns.VERSION_NAME + " TEXT, " +
                    ThemeColumns.ICONS_URI + " TEXT, " +
                    ThemeColumns.IS_IMAGE_FILE + " INTEGER DEFAULT 0, " +
                 // Begin, added by yljiang@lewatek.com 2013-11-19
                    ThemeColumns.MECHANISM_VERSION + " INTEGER DEFAULT 0, " +
                    ThemeColumns.APPLY_PACKAGES + " TEXT, " +
                    ThemeColumns.INCALL_STYLE + " TEXT, " +
                    ThemeColumns.MESSAGE_RINGTONE_NAME + " TEXT, " +
                    ThemeColumns.MESSAGE_RINGTONE_NAME_KEY + " TEXT, " +
                    ThemeColumns.MESSAGE_RINGTONE_URI + " TEXT, " +
                 // End
                    ThemeColumns.LIVE_WALLPAPER_URI + " TEXT, " +
                    ThemeColumns.DOWNLOAD_PATH + " TEXT " +
                    ")");
            db.execSQL("CREATE INDEX themeitem_map_package ON themeitem_map (theme_package)");
            db.execSQL("CREATE UNIQUE INDEX themeitem_map_key ON themeitem_map (theme_package, theme_id)");
            db.execSQL("CREATE INDEX themeitem_map_is_image_file ON themeitem_map (is_image_file)");
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS themeitem_map");
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new OpenDatabaseHelper(getContext());

        /*
         * Detect theme package changes while the provider (that is, our
         * process) is alive. This is the more likely catch for theme package
         * changes as the user is somehow interacting with the theme manager
         * application when these events occur.
         */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
//        filter.addAction(ThemeManager.ACTION_THEME_DOWNLOADED);
        filter.addCategory(ExtraIntent.CATEGORY_THEME_PACKAGE_INSTALLED_STATE_CHANGE);
        filter.addDataScheme("package");
        getContext().registerReceiver(mThemePackageReceiver, filter);

        IntentFilter filterConfigChanged = new IntentFilter();
        filterConfigChanged .addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                Configuration currentConfig = context.getResources().getConfiguration();
                if (currentConfig.userSetLocale) {
                    context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_DOWNLOADED));
                    UpdateLocaleThread.start(context, mOpenHelper);
                }
            }
        }, filterConfigChanged);

        /**
         * Start a background task to make sure the database is in sync with the
         * package manager. This will also detect inserted or deleted themes
         * which didn't come through our application, and that occurred while
         * this provider was not alive.
         * <p>
         * This is not the common case for users, but it is possible and must be
         * supported. Development invokes this feature often when restarting the
         * emulator with changes to theme packages, or by using "adb sync".
         */
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                new VerifyInstalledThemesThread().start();
            }
        });

        return true;
    }

    private static class UpdateLocaleThread extends Thread {
        private static UpdateLocaleThread sInstance;
        private Context mContext;
        private SQLiteOpenHelper mHelper;
        public static void start(Context context, SQLiteOpenHelper helper){
            if(sInstance != null && !Thread.interrupted())
                sInstance.interrupt();
            sInstance = new UpdateLocaleThread(context, helper);
            sInstance.setPriority(Thread.MIN_PRIORITY);
            sInstance.start();
        }
        private UpdateLocaleThread(Context context, SQLiteOpenHelper helper){
            mContext = context;
            mHelper = helper;
        }

        @Override
        public void run() {
            Context context = mContext;
            SQLiteDatabase db = mHelper.getWritableDatabase();
            Cursor c = null;
            try {
                PackageManager pm = context.getPackageManager();
                String where = ThemeColumns._ID + "=?";
                c = db.query(TABLE_NAME, new String[]{ThemeColumns._ID, ThemeColumns.THEME_PACKAGE, ThemeColumns.THEME_ID}, ThemeColumns.LIVE_WALLPAPER_URI + "=1", null, null, null, null);
                while(c.moveToNext()){
                    long id = c.getLong(0);
                    String packageName = c.getString(1);
                    String className = c.getString(2);
                    Intent i = new Intent();
                    i.setClassName(packageName, className);
                    ResolveInfo ri = pm.resolveService(i, PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
                    if(ri != null && ri.serviceInfo != null){
                        WallpaperInfo wi = null;
                        try {
                            wi = new WallpaperInfo(context, ri);
                        } catch (Exception e) {
                        }
                        if(wi != null){
                            ContentValues values = new ContentValues();
                            values.put(ThemeColumns.NAME, wi.loadLabel(pm).toString());
                            values.put(ThemeColumns.AUTHOR, wi.loadAuthor(pm).toString());
                            db.update(TABLE_NAME, values, where, new String[]{String.valueOf(id)});
                        }
                    }
                }
            } catch (Exception e) {
            } finally {
                if(c != null && !c.isClosed())
                    c.close();
            }
        }
    }

    private class VerifyInstalledThemesThread extends Thread {
        private final SQLiteDatabase mDb;
        private final boolean mForceReparsePackages;

        public VerifyInstalledThemesThread() {
            this(false);
        }

        public VerifyInstalledThemesThread(boolean force) {
            mDb = mOpenHelper.getWritableDatabase();
            mForceReparsePackages = force;
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            long start;

            if (Constants.DEBUG) {
                start = System.currentTimeMillis();
            }

            SQLiteDatabase db = mDb;
            db.beginTransaction();
            try {
                verifyPackages();
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();

                if (Constants.DEBUG) {
                    Log.i(Constants.TAG, "VerifyInstalledThemesThread took " + (System.currentTimeMillis() - start) + " ms.");
                }

                getContext().sendBroadcast(new Intent("com.lewa.theme.VerifyThemesCompleted")
                        .putExtra("force", mForceReparsePackages));
            }
        }

        /**
         * Determine if the system default theme needs to be modified based on
         * current runtime conditions, and then modify it.
         *
         * @param db
         * @param appliedTheme Currently applied theme (as detected by querying the system, not this database).
         * @return True if the system default row changed; false otherwise.
         */
        private boolean detectSystemDefaultChange(SQLiteDatabase db, CustomTheme appliedTheme) {
            boolean appliedInDb = DatabaseUtilities.cursorToBoolean(db.query(TABLE_NAME,
                    new String[] { ThemeColumns.IS_APPLIED },
                    sDefaultThemeSelection, null, null, null, null), false);
            boolean appliedToSystem = TextUtils.isEmpty(appliedTheme.getThemePackageName());
            if (appliedToSystem != appliedInDb) {
                if (Constants.DEBUG) {
                    Log.i(Constants.TAG, "ThemesProvider out of sync: updating system default, is_applied=" + appliedToSystem);
                }

                ContentValues values = new ContentValues();
                values.put(ThemeColumns.IS_APPLIED, appliedToSystem);
                db.update(TABLE_NAME, values, sDefaultThemeSelection, null);
                return true;
            } else {
                return false;
            }
        }

        private List<PackageInfo> getInstalledThemePackages() {
            List<PackageInfo> themes = new ArrayList<PackageInfo>();
            if(!STANDALONE){
/*                for(PackageInfo info : android.app.LewaThemeHelper.getInstalledThemePackages(getContext()))
                    if((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        themes.add(info);*/
            } else {
                File lwts = new File(Environment.getExternalStorageDirectory(), ThemesProvider.THEME_PACKAGE_PATH);
                String[] pkgs = lwts.list();
                PackageManager pm = getContext().getPackageManager();
                if (pkgs != null) {
                    Arrays.sort(pkgs);
                    final String lwtp = lwts.getAbsolutePath();
                    for(String p : pkgs){
                        String path = lwtp + '/' + p;
                        PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
                        if(pi != null){
                            pi.applicationInfo.sourceDir = path;
                            themes.add(pi);
                        }
                    }
                }
                //add default theme
                String path = getContext().getPackageResourcePath();
                PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
                if(pi != null){
                    pi.applicationInfo.sourceDir = path;
                    themes.add(pi);
                }
            }
            return themes;
        }

        private void verifyPackages() {
            File file = INTERNAL_WALLPAPER;
            if (file.exists()) {
                File[] imageFiles = file.listFiles();
                String imageName="";
                Long imageSize;
                if (imageFiles != null) {
                    for (File imageFile : imageFiles) {
                        try {
                            imageName=imageFile.getName();
                            imageSize=imageFile.length();
                            ContentValues values = new ContentValues();
                            values.put(ThemeColumns.THEME_PACKAGE,"com.lewa.pkg.rw" + imageName.split("\\.")[0]);
                            values.put(ThemeColumns.THEME_ID, "com.lewa.themeid.rw"+imageName.split("\\.")[0]);
                            values.put(ThemeColumns.NAME, "com.lewa.pkg.rw"+imageName);
                            values.put(ThemeColumns.SIZE, imageSize);
                            values.put(ThemeColumns.AUTHOR, "");
                            values.put(ThemeColumns.STYLE_NAME, "");
                            values.put(ThemeColumns.IS_IMAGE_FILE, -1);
                            String wallpaperUri = null;
                            if(!imageName.split("\\.")[0].equals("default")){
                                wallpaperUri = "file://" + INTERNAL_WALLPAPER +"/"+ imageName;
                                values.put(ThemeColumns.WALLPAPER_URI, wallpaperUri);
                                values.put(ThemeColumns.IS_SYSTEM, 1);
                            }
                            mDb.insert(ThemesProvider.TABLE_NAME, ThemeColumns._ID, values);
                        } catch (Exception e) {
//                            e.printStackTrace();
                        }
                    }
                }
            }
            File lockfile = INTERNAL_LOCKWALLPAPER;
            if (lockfile.exists()) {
                File[] imageFiles = lockfile.listFiles();
                String imageName="";
                Long imageSize;
                if (imageFiles != null) {
                    for (File imageFile : imageFiles) {
                        try {
                            imageName=imageFile.getName();
                            imageSize=imageFile.length();
                            ContentValues values = new ContentValues();
                            values.put(ThemeColumns.THEME_PACKAGE,"com.lewa.pkg.rw" + imageName.split("\\.")[0]);
                            values.put(ThemeColumns.THEME_ID, "com.lewa.themeid.rw"+imageName.split("\\.")[0]);
                            values.put(ThemeColumns.NAME, "com.lewa.pkg.rw"+imageName);
                            values.put(ThemeColumns.SIZE, imageSize);
                            values.put(ThemeColumns.AUTHOR, "");
                            values.put(ThemeColumns.STYLE_NAME, "");
                            values.put(ThemeColumns.IS_IMAGE_FILE, -1);
                            String wallpaperUri = null;
                            wallpaperUri = "file://" + INTERNAL_LOCKWALLPAPER +"/"+ imageName;
                            values.put(ThemeColumns.LOCK_WALLPAPER_URI, wallpaperUri);
                            values.put(ThemeColumns.IS_SYSTEM, 1);
                            mDb.insert(ThemesProvider.TABLE_NAME, ThemeColumns._ID, values);
                        } catch (Exception e) {
//                            e.printStackTrace();
                        }
                    }
                }
            }
            CustomTheme appliedTheme = ThemeUtilities.getAppliedTheme(getContext());

            /*
             * Tracks whether any actual modifications to the database occurred.
             * If true, we must notify content observers when the package
             * verification phase completes.
             */
            boolean notifyChanges = false;

            /*
             * Handle the "default" special case outside the main loop involving
             * actual theme packages.
             */
            // Woody Guo @ 2012/08/20: No longer needed as we have a real default theme pre-installed
            /*
             * boolean invalidatedSystemDefault = detectSystemDefaultChange(mDb, appliedTheme);
             * if (invalidatedSystemDefault) {
             *     notifyChanges = true;
             * }
             */

            PackageManager pm = getContext().getPackageManager();

            /* List all currently installed theme packages. */
            List<PackageInfo> themePackages = getInstalledThemePackages();

            Set<PackageInfo> liveWallpaperPkgs = new HashSet<PackageInfo>();
            List<ResolveInfo> ris = pm.queryIntentServices(new Intent(
                    WallpaperService.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
            String pkgName = null;
            String prevPkgName = null;
            for (ResolveInfo rs : ris) {
                pkgName = rs.serviceInfo.packageName;
                if (pkgName.equals(prevPkgName)||"com.mediatek.vlw".equals(pkgName)) {
                    continue;
                }
                prevPkgName = pkgName;
                try {
                    liveWallpaperPkgs.add(pm.getPackageInfo(pkgName
                            , PackageManager.GET_META_DATA | PackageManager.GET_SERVICES));
                } catch (Exception e) {
                }
            }

            themePackages.addAll(liveWallpaperPkgs);

            /*
             * Get a sorted cursor of all currently known themes. We'll walk
             * this cursor along with the package managers sorted output to
             * determine changes. This cursor intentionally excludes the
             * "special" case system default theme (which has THEME_PACKAGE set
             * to a blank string).
             */
            // Woody Guo @ 2012/08/21: Now every theme is a real theme,
            // the name of the default theme will never be empty.
            Cursor current = mDb.query(TABLE_NAME,
                    null, ThemeColumns.IS_IMAGE_FILE + " = 0", null, null, null,
                    ThemeColumns.THEME_PACKAGE + ", " + ThemeColumns.THEME_ID);
            ThemeItem currentItem = ThemeItem.getInstance(current);

            Collections.sort(themePackages, new Comparator<PackageInfo>() {
                @Override
                public int compare(PackageInfo a, PackageInfo b) {
                    return a.packageName.compareTo(b.packageName);
                }
            });


            if(!STANDALONE) {
                //Delete for standalone by Fan.Yang
/*                if (mForceReparsePackages) {
                    for (PackageInfo pi: themePackages) {
                        PackageInfo piNew = pm.getPackageArchiveInfo(pi.applicationInfo.sourceDir, 0);
                        int N = (piNew.themeInfos == null ? 0 : piNew.themeInfos.length);
                        for (int i = 0; i < N; i++) {
                            pi.themeInfos[i] = piNew.themeInfos[i];
                            // pi.setDrmProtectedThemeApk(pi.isDrmProtectedThemeApk() || pi.themeInfos[i].isDrmProtected);
                        }
                        *//*
                         * if (pi.isDrmProtectedThemeApk()) {
                         *     pi.setLockedZipFilePath(PackageParser.getLockedZipFilePath(p.mPath));
                         * }
                         *//*
                    }
                }*/
            }

            try {
                for (PackageInfo pi: themePackages) {
                    ThemeInfo tis[];
                    if(STANDALONE) {
                        ThemeInfo ti = parsePackage(pi.applicationInfo.sourceDir);
                        tis = ti == null ? null : new ThemeInfo[]{ ti };
                    } else {
                        //Delete for standalone by Fan.Yang
                        tis = null;//pi.themeInfos;
                    }
                    if (tis == null && pi.services == null) {
                        continue;
                    }

                    /*
                     * Deal with potential package change, moving `current'
                     * along to efficiently detect differences. This method
                     * handles insert, delete, and modify returning with
                     * `current' positioned ahead of the theme matching the last
                     * of `pi's ThemeInfo objects (or passed the last
                     * entry if the cursor is exhausted).
                     */
                    boolean invalidated = detectPackageChange(
                            getContext(), mDb, pi, current, currentItem, appliedTheme);
                    if (invalidated) {
                        notifyChanges = true;
                    }

                    mDb.yieldIfContendedSafely();
                }

                /*
                 * Delete any items left-over that were not found in
                 * `themePackages'.
                 */
                while (current.moveToNext()) {
                    deleteTheme(mDb, currentItem);
                    notifyChanges = true;
                }
                InnerFontUtil.InsertInnerFonts(mDb);
                notifyChanges = true;

                /*
                 * // Deal with downloaded images
                 * current.moveToFirst();
                 * File sdcardRoot = Environment.getExternalStorageDirectory();
                 * File[] pkgs = (new File(sdcardRoot.getPath() + THEME_WALLPAPER_PATH)).listFiles();
                 * if (pkgs != null) {
                 *     Arrays.sort(pkgs, new Comparator<File>() {
                 *         public int compare(File f1, File f2) {
                 *             return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                 *         }
                 *     });
                 *     insertWallpapers(mDb, pkgs);
                 * }
                 */
            } finally {
                if (currentItem != null) {
                    currentItem.close();
                }
                if (notifyChanges) {
                    notifyChanges();
                }
            }
        }
    }

    private static boolean detectPackageChange(Context context, SQLiteDatabase db,
            PackageInfo pi, Cursor current, ThemeItem currentItem, CustomTheme appliedTheme) {
        boolean notifyChanges = false;

        ThemeInfo[] tis;
        if(STANDALONE) {
            ThemeInfo ti = parsePackage(pi.applicationInfo.sourceDir);
            tis = ti == null ? null : new ThemeInfo[]{ ti };
        } else {
            //Delete for standalone by Fan.Yang
            tis = null;//pi.themeInfos;
        }
        if (null != tis) {
            Arrays.sort(tis, new Comparator<ThemeInfo>() {
                @Override
                public int compare(ThemeInfo a, ThemeInfo b) {
                    return a.themeId.compareTo(b.themeId);
                }
            });

            for (ThemeInfo ti: tis) {
                String currPackageName = null;
                String currThemeId = null;

                /*
                 * The local cursor is sorted to require only 1 iteration
                 * through to detect inserts, updates, and deletes.
                 */
                while (!current.isAfterLast()) {
                    String packageName = currentItem.getPackageName();
                    String themeId = currentItem.getThemeId();

                    /* currentItem less than, equal to, or greater than pi/ti? */
                    int cmp = ThemeUtilities.compareTheme(currentItem, pi, ti);

                    if (cmp < 0) {
                        /*
                         * This theme isn't in the package list, delete and
                         * go to the next; rinse, lather, repeat.
                         */
                        deleteTheme(db, currentItem);
                        notifyChanges = true;
                        current.moveToNext();
                        continue;
                    }

                    /*
                     * Either we need to verify this entry in the database or we
                     * need to insert a new one. Either way, the current cursor
                     * is correctly positioned so we should break out of this
                     * loop to do the real work.
                     */
                    if (cmp == 0) {
                        currPackageName = packageName;
                        currThemeId = themeId;
                    } else /* if (cmp > 0) */ {
                        currPackageName = null;
                        currThemeId = null;
                    }

                    /* Handle either an insert or verify/update. */
                    break;
                }

                boolean isCurrentTheme = ThemeUtilities.themeEquals(pi, ti, appliedTheme);

                if (currPackageName != null && currThemeId != null) {
                    boolean invalidated = verifyOrUpdateTheme(
                            context, db, pi, ti, currentItem, isCurrentTheme);
                    if (invalidated) {
                        notifyChanges = true;
                    }
                    current.moveToNext();
                } else {
                    insertTheme(context, db, pi, ti, isCurrentTheme);
                    notifyChanges = true;
                }
            }
        } else if (null != pi.services) {
            List<ResolveInfo> services = context.getPackageManager().queryIntentServices(
                    new Intent(WallpaperService.SERVICE_INTERFACE).setPackage(pi.packageName)
                    , PackageManager.GET_META_DATA);
            /*
             * List<ServiceInfo> services = new ArrayList<ServiceInfo>(pi.services.length);
             * for (ServiceInfo si: pi.services) {
             *     if (si.metaData.containsKey(WallpaperService.SERVICE_META_DATA)) {
             *         services.add(si);
             *     }
             * }
             */
            Collections.sort(services , new Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo a, ResolveInfo b) {
                    return a.serviceInfo.name.compareTo(b.serviceInfo.name);
                }
            });

            for (ResolveInfo ri : services) {
                String currPackageName = null;
                String currThemeId = null;

                /*
                 * The local cursor is sorted to require only 1 iteration
                 * through to detect inserts, updates, and deletes.
                 */
                while (!current.isAfterLast()) {
                    String packageName = currentItem.getPackageName();
                    String themeId = currentItem.getThemeId();

                    /* currentItem less than, equal to, or greater than pi/ti? */
                    int cmp = ThemeUtilities.compareTheme(currentItem, pi, ri.serviceInfo);

                    if (cmp < 0) {
                        /*
                         * This theme isn't in the package list, delete and
                         * go to the next; rinse, lather, repeat.
                         */
                        deleteTheme(db, currentItem);
                        notifyChanges = true;
                        current.moveToNext();
                        continue;
                    }

                    /*
                     * Either we need to verify this entry in the database or we
                     * need to insert a new one. Either way, the current cursor
                     * is correctly positioned so we should break out of this
                     * loop to do the real work.
                     */
                    if (cmp == 0) {
                        currPackageName = packageName;
                        currThemeId = themeId;
                    } else /* if (cmp > 0) */ {
                        currPackageName = null;
                        currThemeId = null;
                    }

                    /* Handle either an insert or verify/update. */
                    break;
                }

                boolean isCurrentTheme = false;

                if (currPackageName != null && currThemeId != null) {
                    boolean invalidated = verifyOrUpdateTheme(
                            context, db, pi, ri.serviceInfo, currentItem, isCurrentTheme);
                    if (invalidated) {
                        notifyChanges = true;
                    }
                    current.moveToNext();
                } else {
                    insertTheme(context, db, pi, ri.serviceInfo, isCurrentTheme);
                    notifyChanges = true;
                }
            }
        }

        return notifyChanges;
    }

    private static boolean hasThemePackageScope(Context context, PackageInfo pi, ThemeInfo ti) {
        if ((ti.previewResourceId >>> 24) == 0x0a) {
            return true;
        }
        if ((ti.styleResourceId >>> 24) == 0x0a) {
            return true;
        }
        if ((ti.wallpaperResourceId >>> 24) == 0x0a) {
            return true;
        }
        if ((ti.thumbnailResourceId >>> 24) == 0x0a) {
            return true;
        }
        return false;
    }

/*
 *     private static void populateContentValues(ContentValues outValues, File wallpaper) {
 *         String fileNameWithoutExt = wallpaper.getName();
 *         int lastIndexOfSlash = fileNameWithoutExt.lastIndexOf(File.pathSeparator);
 *         if (lastIndexOfSlash > 0) {
 *             fileNameWithoutExt = fileNameWithoutExt.substring(0, lastIndexOfSlash);
 *         }
 * 
 *         String name = fileNameWithoutExt;
 *         int indexOfUnderlines = fileNameWithoutExt.indexOf("___");
 *         if (0 < indexOfUnderlines) {
 *             if (Locale.getDefault().getLanguage().equals("zh")) {
 *                 name = fileNameWithoutExt.substring(0, indexOfUnderlines);
 *             } else {
 *                 name = fileNameWithoutExt.substring(indexOfUnderlines+3, fileNameWithoutExt.length());
 *             }
 *         }
 *         Uri wallpaperUri = Uri.parse("file://" + wallpaper.getAbsolutePath());
 * 
 *         outValues.put(ThemeColumns.IS_APPLIED, 0);
 *         outValues.put(ThemeColumns.THEME_ID, "");
 *         outValues.put(ThemeColumns.THEME_PACKAGE, "");
 *         outValues.put(ThemeColumns.STYLE_NAME, "");
 *         outValues.put(ThemeColumns.IS_DRM, 0);
 *         outValues.put(ThemeColumns.IS_SYSTEM, 0);
 *         outValues.put(ThemeColumns.HAS_HOST_DENSITY, 1);
 *         outValues.put(ThemeColumns.HAS_THEME_PACKAGE_SCOPE, 0);
 *         outValues.put(ThemeColumns.WALLPAPER_NAME, name);
 *         outValues.put(ThemeColumns.WALLPAPER_URI , wallpaperUri);
 *         outValues.put(ThemeColumns.RINGTONE_NAME, (String) null);
 *         outValues.put(ThemeColumns.RINGTONE_URI , (String) null);
 *         outValues.put(ThemeColumns.RINGTONE_NAME_KEY , (String) null);
 *         outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME, (String) null);
 *         outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_URI , (String) null);
 *         outValues.put(ThemeColumns.NOTIFICATION_RINGTONE_NAME_KEY, (String) null);
 *         outValues.put(ThemeColumns.BOOT_ANIMATION_URI, (String) null);
 *         outValues.put(ThemeColumns.FONT_URI , (String) null);
 *         outValues.put(ThemeColumns.LOCKSCREEN_URI, (String) null);
 *         outValues.put(ThemeColumns.ICONS_URI , (String) null);
 *         outValues.put(ThemeColumns.LOCK_WALLPAPER_URI, (String) null);
 *         outValues.put(ThemeColumns.LOCK_WALLPAPER_NAME , (String) null);
 *         outValues.put(ThemeColumns.PREVIEW_URI , (String) null);
 *         outValues.put(ThemeColumns.LIVE_WALLPAPER_URI , (String) null);
 *         outValues.put(ThemeColumns.VERSION_CODE, 0);
 *         outValues.put(ThemeColumns.VERSION_NAME, "");
 *         outValues.put(ThemeColumns.AUTHOR, "");
 *         outValues.put(ThemeColumns.NAME, name);
 *         outValues.put(ThemeColumns.SIZE, pkgFile.length());
 *     }
 */


    private static void deleteTheme(SQLiteDatabase db, ThemeItem item) {
        //add for keep fake internal default theme
        if(ThemeManager.THEME_ELEMENTS_PACKAGE.equals(item.getPackageName()) || !TextUtils.isEmpty(item.getDownloadPath()))
            return;
        if (Constants.DEBUG) {
            Log.i(Constants.TAG, "ThemesProvider out of sync: removing "
                    + item.getPackageName() + "/" + item.getThemeId());
        }
        db.delete(TABLE_NAME, ThemeColumns._ID + " = " + item.getId(), null);
    }

    private static void insertTheme(Context context
            , SQLiteDatabase db, PackageInfo pi, ServiceInfo si, boolean isCurrentTheme) {
        if (Constants.DEBUG) {
            Log.i(Constants.TAG, "ThemesProvider out of sync: inserting " + pi.packageName + "/" + si.name);
        }

        ContentValues values = new ContentValues();
        Utils.populateContentValues(context, values, pi, si, isCurrentTheme);
        if(shouldInsertTheme(values))
        db.insert(TABLE_NAME, ThemeColumns._ID, values);
    }

    private static void insertTheme(Context context
            , SQLiteDatabase db, PackageInfo pi, ThemeInfo ti, boolean isCurrentTheme) {
        if (Constants.DEBUG) {
            Log.i(Constants.TAG, "ThemesProvider out of sync: inserting " + pi.packageName + "/" + ti.themeId);
        }

        ContentValues values = new ContentValues();
        Utils.populateContentValues(context, values, pi, ti, isCurrentTheme);
        db.insert(TABLE_NAME, ThemeColumns._ID, values);
    }

    /*
     * private static void insertWallpapers(SQLiteDatabase db, File[] wallpapers) {
     *     if (Constants.DEBUG) {
     *         Log.i(Constants.TAG, "ThemesProvider out of sync: inserting " + wallpapers.length + " wallpapers");
     *     }
     *     ContentValues values = new ContentValues();
     *     for (File f : wallpapers) {
     *         values.clear();
     *         populateContentValues(values, f);
     *         db.insert(TABLE_NAME, ThemeColumns._ID, values);
     *     }
     * }
     */

    private static boolean verifyOrUpdateTheme(Context context, SQLiteDatabase db
            , PackageInfo pi, ServiceInfo si, ThemeItem existing, boolean isCurrentTheme) {
        boolean invalidated = false;

        ContentValues values = new ContentValues();
        Utils.populateContentValues(context, values, pi, si, isCurrentTheme);

        invalidated = !equalContentValuesAndCursor(values, existing.getCursor());

        if (invalidated) {
            if (Constants.DEBUG) {
                Log.i(Constants.TAG, "ThemesProvider out of sync: updating "
                        + existing.getPackageName() + "/" + existing.getThemeId());
            }

            db.update(TABLE_NAME, values, ThemeColumns._ID + " = " + existing.getId(), null);
        }

        return invalidated;
    }

    private static boolean verifyOrUpdateTheme(Context context, SQLiteDatabase db
            , PackageInfo pi, ThemeInfo ti, ThemeItem existing, boolean isCurrentTheme) {
        boolean invalidated = false;

        /*
         * Pretend we would insert this record fresh, then compare the
         * resulting ContentValues with the actual database row. If any
         * differences are found, adjust them with an update query.
         */
        ContentValues values = new ContentValues();
        Utils.populateContentValues(context, values, pi, ti, isCurrentTheme);

        invalidated = !equalContentValuesAndCursor(values, existing.getCursor());

        if (invalidated) {
            if (Constants.DEBUG) {
                Log.i(Constants.TAG, "ThemesProvider out of sync: updating "
                        + existing.getPackageName() + "/" + existing.getThemeId());
            }

            db.update(TABLE_NAME, values, ThemeColumns._ID + " = " + existing.getId(), null);
        }

        return invalidated;
    }

    private static boolean equalContentValuesAndCursor(ContentValues values, Cursor cursor) {
        int n = cursor.getColumnCount();
        while (n-- > 0) {
            String columnName = cursor.getColumnName(n);
            if (columnName.equals("_id")) {
                continue;
            }
            if (cursor.isNull(n)) {
                if (values.getAsString(columnName) != null) {
                    return false;
                }
            } else if (!cursor.getString(n).equals(values.getAsString(columnName))) {
                return false;
            }
        }
        return true;
    }
    //#65947 add begin by bin.dong
    private static boolean shouldInsertTheme(ContentValues values){
        String thumbUriString = (String)values.get(ThemeColumns.THUMBNAIL_URI);
        if(null != thumbUriString)
            return true;
        return false;
    }
    //#65947 add end by bin.dong
    private final BroadcastReceiver mThemePackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            if(Intent.ACTION_PACKAGE_ADDED.equals(action)){
                //insert live wallpaper package
                Uri uri = intent.getData();
                if(uri != null){
                    String pkg = uri.getEncodedSchemeSpecificPart();
                    if(pkg != null){
                        PackageManager pm = getContext().getPackageManager();
                        PackageInfo pi;
                        try {
                            pi = pm.getPackageInfo(pkg
                                    , PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
                        } catch (Exception e) {
                            pi = null;
                        }
                        if (pi != null && pi.services != null) {
                            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                            List<ResolveInfo> services = pm.queryIntentServices(new Intent(
                                    WallpaperService.SERVICE_INTERFACE).setPackage(pi.packageName)
                                    , PackageManager.GET_META_DATA);
                            if(services.size() > 0){
                                for (ResolveInfo ri : services) {
                                    ContentValues values = new ContentValues();
                                    Utils.populateContentValues(getContext(), values, pi, ri.serviceInfo, false);
                                    if(shouldInsertTheme(values))
                                    db.insert(TABLE_NAME, ThemeColumns._ID, values);
                                }
                                notifyChanges();
                            }
                        }
                    }
                }

            } else if(Intent.ACTION_PACKAGE_REMOVED.equals(action)){
                Uri uri = intent.getData();
                if(uri != null){
                    String pkg = uri.getEncodedSchemeSpecificPart();
                    if(pkg != null){
                        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                        if(db.delete(TABLE_NAME, ThemeColumns.THEME_PACKAGE + "='" + pkg + "' AND " + ThemeColumns.LIVE_WALLPAPER_URI + "=1", null) > 0)
                            notifyChanges();
                    }
                }
            }
            /*new Thread() {
                public void run() {
                    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                    db.beginTransaction();
                    try {
                        handlePackageEvent(context, db, intent);
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
            }.start();*/
        }

        private void handlePackageEvent(Context context, SQLiteDatabase db, Intent intent) {
            String action = intent.getAction();
            String pkg = intent.getData().getSchemeSpecificPart();

            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            if (Constants.DEBUG) {
                Log.d(Constants.TAG, "ThemesProvider.handlePackageEvent: action=" + action
                        + "; package=" + pkg + "; replacing=" + isReplacing);
            }

            try {
                if (isReplacing) {
                    if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                        if (Constants.DEBUG) {
                            Log.i(Constants.TAG, "Handling replaced theme package: " + pkg);
                        }
                        // TODO: At the moment, we think a package is a livewallpaper by checking this:
                        // <meta-data android:name="android.service.wallpaper" ... />
                        // Ideally, we should do this by checking this:
                        // <action android:name="android.service.wallpaper.WallpaperService" />
                        PackageInfo pi = context.getPackageManager().getPackageInfo(pkg
                                , PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
                        Cursor cursor = db.query(TABLE_NAME, null,
                                ThemeColumns.THEME_PACKAGE + " = ?",
                                new String[] { pkg }, null, null, ThemeColumns.THEME_ID);
                        ThemeItem dao = ThemeItem.getInstance(cursor);
                        boolean invalidated = detectPackageChange(context
                                , db, pi, cursor, dao, ThemeUtilities.getAppliedTheme(context));
                        if (invalidated) {
                            notifyChanges();
                        }
                        if (null != cursor) cursor.close();
                    }
                } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                    if (Constants.DEBUG) {
                        Log.i(Constants.TAG, "Handling new theme package: " + pkg);
                    }
                    PackageInfo pi = context.getPackageManager().getPackageInfo(pkg
                            , PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
                    if (pi != null) {
                        ThemeInfo[] tis;
                        if(STANDALONE) {
                            ThemeInfo ti = parsePackage(pi.applicationInfo.sourceDir);
                            tis = ti == null ? null : new ThemeInfo[]{ ti };
                        } else {
                            //Delete for standalone by Fan.Yang
                            tis = null;//pi.themeInfos;
                        }
                        if (tis != null) {
                            for (ThemeInfo ti: tis) {
                                insertTheme(context, db, pi, ti, false);
                            }
                        } else if (pi.services != null) {
                            // for (ServiceInfo si : pi.services) {
                                // if (si.metaData.containsKey(WallpaperService.SERVICE_META_DATA)) {
                                    // insertTheme(context, db, pi, si, false);
                                // }
                            // }
                            List<ResolveInfo> services = context.getPackageManager()
                                    .queryIntentServices(new Intent(
                                    WallpaperService.SERVICE_INTERFACE).setPackage(pi.packageName)
                                    , PackageManager.GET_META_DATA);
                            for (ResolveInfo ri : services) {
                                insertTheme(context, db, pi, ri.serviceInfo, false);
                            }
                        }
                    }
                    notifyChanges();
                } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                    if (Constants.DEBUG) {
                        Log.i(Constants.TAG, "Handling removed theme package: " + pkg);
                    }
                    db.delete(TABLE_NAME, ThemeColumns.THEME_PACKAGE + " = ?", new String[] { pkg });
                    notifyChanges();
                }
            } catch (NameNotFoundException e) {
                if (Constants.DEBUG) {
                    Log.d(Constants.TAG, "Unexpected package manager inconsistency detected", e);
                }
            }
        }
    };

    private Cursor queryThemes(int type, Uri uri, SQLiteDatabase db, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        if (type == TYPE_THEMES) {
            if (sortOrder == null) {
                sortOrder = ThemeColumns.NAME;
            }
        } else if (type == TYPE_THEME) {
            List<String> segments = uri.getPathSegments();
            int n = segments.size();
            if (n == 3) {
                String packageName = segments.get(1);
                String themeId = segments.get(2);
                if (packageName.equals("WOODY@LEWA")) {
                    // Woody Guo @ 2012/07/30: Obtain a list of themes
                    // whose IDs are given in themeId in the format of "id1","id2","id3"...
                    selection = DatabaseUtilities.appendSelection(selection,
                            ThemeColumns.THEME_ID + " in (" + themeId + ")");
                } else {
                    selection = DatabaseUtilities.appendSelection(selection,
                            ThemeColumns.THEME_PACKAGE + "=? AND " +
                            ThemeColumns.THEME_ID + "=?");
                    selectionArgs = DatabaseUtilities.appendSelectionArgs(selectionArgs,
                            packageName, themeId);
                }
            } else {
                throw new IllegalArgumentException("Can't parse URI: " + uri);
            }
        } else if (type == TYPE_THEME_SYSTEM) {
            selection = DatabaseUtilities.appendSelection(selection,
                    ThemeColumns.THEME_PACKAGE + " = ? AND " +
                    ThemeColumns.THEME_ID + " = ?");
            CustomTheme defaultTheme = CustomTheme.getSystemTheme();
            selectionArgs = DatabaseUtilities.appendSelectionArgs(selectionArgs
                    , defaultTheme.getThemePackageName(), defaultTheme.getThemeId());
        }
        return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public Cursor query(Uri uri, String[] projection
            , String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        int type = URI_MATCHER.match(uri);
        Cursor c;
        switch (type) {
            case TYPE_THEMES:
            case TYPE_THEME:
            case TYPE_THEME_SYSTEM:
                c = queryThemes(type, uri, db, projection, selection, selectionArgs, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        int type = URI_MATCHER.match(uri);
        switch (type) {
            case TYPE_THEMES:
                return ThemeColumns.CONTENT_TYPE;
            case TYPE_THEME:
            case TYPE_THEME_SYSTEM:
                return ThemeColumns.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private void checkForRequiredArguments(ContentValues values) {
        if (!values.containsKey(ThemeColumns.THEME_PACKAGE)) {
            throw new IllegalArgumentException(
                    "Required argument missing: " + ThemeColumns.THEME_PACKAGE);
        }
        if (!values.containsKey(ThemeColumns.THEME_ID)) {
            throw new IllegalArgumentException("Required argument missing: " + ThemeColumns.THEME_ID);
        }

    }

    private void notifyChanges() {
        getContext().getContentResolver()
                .notifyChange(Themes.ThemeColumns.CONTENT_PLURAL_URI, null);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * if (Binder.getCallingPid() != android.os.Process.myPid()) {
         *     throw new SecurityException("Cannot insert into this provider");
         * }
         */

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri newUri = null;
        long _id;

        switch (URI_MATCHER.match(uri)) {
            case TYPE_THEMES:
                checkForRequiredArguments(values);
                String packageName = values.getAsString(ThemeColumns.THEME_PACKAGE);
                String themeId = values.getAsString(ThemeColumns.THEME_ID);
                _id = db.insert(TABLE_NAME, ThemeColumns._ID, values);
                if (_id >= 0) {
                    newUri = ThemeColumns.CONTENT_URI.buildUpon()
                            .appendPath(packageName)
                            .appendPath(themeId).build();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (newUri != null) {
            notifyChanges();
        }

        return newUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        /*
         * if (Binder.getCallingPid() != android.os.Process.myPid()) {
         *     throw new SecurityException("Cannot update this provider");
         * }
         */

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;

        int type = URI_MATCHER.match(uri);
        switch (type) {
            case TYPE_THEMES:
            case TYPE_THEME:
                if (type == TYPE_THEME) {
                    List<String> segments = uri.getPathSegments();
                    int n = segments.size();
                    if (n == 3) {
                        String packageName = segments.get(1);
                        String themeId = segments.get(2);
                        selection = DatabaseUtilities.appendSelection(selection,
                                ThemeColumns.THEME_PACKAGE + "=? AND " +
                                ThemeColumns.THEME_ID + "=?");
                        selectionArgs = DatabaseUtilities.appendSelectionArgs(selectionArgs,
                                packageName, themeId);
                    } else {
                        throw new IllegalArgumentException("Can't parse URI: " + uri);
                    }
                }
                try {
                    count = db.update(TABLE_NAME, values, selection, selectionArgs);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Failed to update " + TABLE_NAME + " " + e);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (count > 0) {
            notifyChanges();
        }

        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        /*
         * if (Binder.getCallingPid() != android.os.Process.myPid()) {
         *     throw new SecurityException("Cannot delete from this provider");
         * }
         */

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case TYPE_THEMES:
                count = db.delete(TABLE_NAME, selection, selectionArgs);
                break;
            case TYPE_THEME:
                List<String> segments = uri.getPathSegments();
                int n = segments.size();
                if (n == 3) {
                    String packageName = segments.get(1);
                    String themeId = segments.get(2);
                    selection
                            = ThemeColumns.THEME_PACKAGE + "=? AND "
                            + ThemeColumns.THEME_ID + "=?";
                    selectionArgs = new String[] { packageName, themeId };
                    count = db.delete(TABLE_NAME, selection, selectionArgs);
                    break;
                }
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (count > 0) {
            notifyChanges();
        }

        return count;
    }

    static {
        URI_MATCHER.addURI(Themes.AUTHORITY, "themes", TYPE_THEMES);
        URI_MATCHER.addURI(Themes.AUTHORITY, "theme/system", TYPE_THEME_SYSTEM);
        URI_MATCHER.addURI(Themes.AUTHORITY, "theme/*/*", TYPE_THEME);
    }


}
