package com.lewa.themechooser.provider;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
//import android.provider.DrmStore;
import android.text.TextUtils;
import android.util.Log;

import com.lewa.themes.ThemeManager;
import com.lewa.themes.Utils;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lewa.content.ExtraIntent;
import lewa.os.FileUtilities;

/**
 * Proxy uri-based access to assets from theme packages. DRM security is upheld
 * at this layer which is why we can't just use the built-in android.resource://
 * uri scheme.
 */
public class PackageResourcesProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int TYPE_RESOURCE_ID = 0;
    private static final int TYPE_RESOURCE_ENTRY = 1;
    private static final int TYPE_ASSET_PATH = 2;

    private static final int DEFAULT_ORIENTATION = Configuration.ORIENTATION_PORTRAIT;
    private static final String INTERNAL_FILE_URI_PERFIX = "/file://";
    /* Cache AssetManager objects to speed up ringtone manipulation. */
    private final Map<PackageKey, SoftReference<Resources>> mResourcesTable =
            new HashMap<PackageKey, SoftReference<Resources>>();
    private final BroadcastReceiver mThemePackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String pkg = intent.getData().getSchemeSpecificPart();
            deleteResourcesForTheme(pkg);
        }
    };

    private static AssetManager createAssetManager(String packageFileName,
                                                   String packageLockedZipFile) {
        AssetManager assets = new AssetManager();
        assets.addAssetPath(packageFileName);
        if (packageLockedZipFile != null) {
            assets.addAssetPath(packageLockedZipFile);
        }
        return assets;
    }

    @Override
    public boolean onCreate() {
        /*
         * Detect package removal for the purpose of clearing the resource table
         * cache. Even though it's a soft ref cache it is important to
         * immediately clear on removal so that updated theme packages
         * immediately reflect updated media assets.
         */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addCategory(ExtraIntent.CATEGORY_THEME_PACKAGE_INSTALLED_STATE_CHANGE);
        filter.addDataScheme("package");
        getContext().registerReceiver(mThemePackageReceiver, filter);

        return true;
    }

    private synchronized Resources getResourcesForTheme(String packageName,
                                                        int orientation) throws NameNotFoundException {
        final PackageKey key = new PackageKey(packageName, orientation);
        SoftReference<Resources> ref = mResourcesTable.get(key);
        Resources res = ref != null ? ref.get() : null;
        if (res != null) {
            return res;
        }

        PackageInfo pi = null;
        try {
            pi = getContext().getPackageManager().getPackageInfo(packageName, 0);
        } catch (Exception e) {
        }
        if (pi == null || pi.applicationInfo == null) {
            if (ThemeManager.THEME_ELEMENTS_PACKAGE.equals(packageName)) {
                return getContext().getResources();
            } else {
                Cursor c = null;
                try {
                    c = getContext().getContentResolver().query(ThemeColumns.CONTENT_PLURAL_URI, new String[]{ThemeColumns.DOWNLOAD_PATH}, ThemeColumns.THEME_PACKAGE + "='" + packageName + "'", null, null);
                    if (c.moveToFirst()) {
                        String path = c.getString(0);
                        res = Utils.getFileResources(getContext(), path);
                        mResourcesTable.put(key, new SoftReference<Resources>(res));
                        return res;
                    }
                    c.close();
                } catch (Exception e) {
                    Log.e("getFileResources", e.getMessage());
                } finally {
                    if (c != null && !c.isClosed()) {
                        c.close();
                    }
                }
            }
        } else {
            if (ThemeManager.STANDALONE) {
                res = Utils.getFileResources(getContext(), pi.applicationInfo.publicSourceDir);
                mResourcesTable.put(key, new SoftReference<Resources>(res));
                return res;
            } else {
                return null;
                //Delete for standalone by Fan.Yang
                //return createResourcesForTheme(key, pi.applicationInfo.publicSourceDir, pi.getLockedZipFilePath());
            }
        }
        return null;
    }

    private synchronized Resources createResourcesForTheme(final PackageKey key,
                                                           String packageFileName, String packageLockedZipFile) {
        AssetManager assets = createAssetManager(packageFileName, packageLockedZipFile);
        Configuration config = new Configuration(getContext().getResources().getConfiguration());
        config.orientation = key.orientation;
        Resources r = new Resources(assets, Resources.getSystem().getDisplayMetrics(), config);

        mResourcesTable.put(key, new SoftReference<Resources>(r));
        return r;
    }

    private synchronized void deleteResourcesForTheme(String packageName) {
        // When a theme package is uninstalled, remove all cached
        // resource packages with the theme's package name.
        try {
            for (PackageKey key : mResourcesTable.keySet()) {
                if (packageName.equals(key.packageName)) {
                    mResourcesTable.remove(key);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        /*
         * When the C++ side (usually through MediaPlayerService) tries to open
         * the file, it uses ActivityManagerService#openContentUri which differs
         * in implementation from ContentResolver#openFileDescriptor(Uri, ...).
         * Specifically, openFile is called directly instead of openAssetFile,
         * with no logic in place to wrap the AssetFileDescriptor returned.
         */
        String path = uri.getPath();
        if (path.startsWith("file://")) {
            return super.openFile(Uri.parse(path), mode);
        } else {
            AssetFileDescriptor afd = openAssetFile(uri, mode);
            if (afd == null) {
                return null;
            }
            return afd.getParcelFileDescriptor();
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        String path = uri.getPath();
        if (path.startsWith(INTERNAL_FILE_URI_PERFIX)) {
            File tmp = new File(path.substring(INTERNAL_FILE_URI_PERFIX.length()));
            if (tmp.exists()) {
                AssetFileDescriptor fd = new AssetFileDescriptor(ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY), 0, tmp.length());
                return fd;
            } else {
                return null;
            }
        }

        int type = URI_MATCHER.match(uri);
        /*
         * if (Constants.DEBUG) {
         *     Log.d(Constants.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " - Uri: " + uri.toString());
         * }
         */

        List<String> segments = uri.getPathSegments();
        if (segments.size() < 3) {
            throw new IllegalArgumentException("Can't handle URI: " + uri);
        }

        String packageName = segments.get(0);
        Resources packageRes = null;
        try {
            packageRes = getResourcesForTheme(packageName,
                    getOrientation(uri));
        } catch (NameNotFoundException e) {
            throw new FileNotFoundException(e.toString());
        }
        if (packageRes == null) {
            throw new FileNotFoundException("Unable to access package: " + packageName);
        }

        switch (type) {
            case TYPE_RESOURCE_ID:
            case TYPE_RESOURCE_ENTRY:
                int resId;
                if (type == TYPE_RESOURCE_ID) {
                    resId = (int) ContentUris.parseId(uri);
                } else {
                    resId = packageRes.getIdentifier(segments.get(3), segments.get(2),
                            packageName);
                }
                if (resId == 0) {
                    throw new IllegalArgumentException("No resource found for URI: " + uri);
                }
                try {
                    return packageRes.openRawResourceFd(resId);
                } catch (Exception e) {
                    InputStream in = null;
                    FileOutputStream out = null;
                    File tmp = new File(getContext().getFilesDir(), "thumb_" + packageName + '_' + String.valueOf(resId));
                    try {
                        if (!tmp.exists()) {
                            in = packageRes.openRawResource(resId);
                            out = new FileOutputStream(tmp);
                            FileUtilities.connectIO(in, out);
                        }
                        AssetFileDescriptor fd = new AssetFileDescriptor(ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY), 0, tmp.length());
                        return fd;
                    } catch (Exception ex) {
                    } finally {
                        if (in != null)
                            FileUtilities.close(in);
                        if (out != null)
                            FileUtilities.close(out);
                    }
                }

            case TYPE_ASSET_PATH:
                final String assets = "/assets/";
                String assetPath = uri.getPath();
                assetPath = assetPath.substring(assetPath.lastIndexOf(assets) + assets.length());
                if (assetPath.contains("/locked/")) {
                    /* Make sure the caller has DRM access permission.  This should basically
                     * only be the media service process.  This call technically checks whether
                     * our own process holds this permission as well so it's extremely important
                     * the ThemeManager never requests this in the manifest. */
                    //DrmStore.enforceAccessDrmPermission(getContext());
                }
                try {
                    return packageRes.getAssets().openFd(assetPath);
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private int getOrientation(Uri uri) {
        int orientation = DEFAULT_ORIENTATION;
        String o = uri.getQueryParameter(Themes.KEY_ORIENTATION);
        if (!TextUtils.isEmpty(o)) {
            orientation = Integer.valueOf(o);
        }
        return orientation;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    static {
        /* See PackageResources#makeRingtoneUri. */
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "*/res/#", TYPE_RESOURCE_ID);
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "*/res/*/*", TYPE_RESOURCE_ENTRY);
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "*/assets/*", TYPE_ASSET_PATH);
        URI_MATCHER.addURI(PackageResources.AUTHORITY, "*/assets/*/*", TYPE_ASSET_PATH);
    }

    private class PackageKey {
        public String packageName;
        public int orientation;

        public PackageKey(String packageName, int orientation) {
            this.packageName = packageName;
            this.orientation = orientation;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PackageKey)) {
                return false;
            }
            PackageKey other = (PackageKey) obj;
            return (orientation == other.orientation &&
                    packageName.equals(other.packageName));
        }

        @Override
        public int hashCode() {
            return 37 * packageName.hashCode() + 421 * orientation + 7789;
        }
    }
}
