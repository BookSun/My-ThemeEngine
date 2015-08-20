package com.lewa.themes.provider;

import com.lewa.themes.ThemeManager;

import android.net.Uri;
import android.text.TextUtils;

public class PackageResources {
    public static final String AUTHORITY = ThemeManager.RESOURCE_AUTHORITY;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    protected PackageResources() {}

    private static void checkPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName must not be null or empty.");
        }
    }

    public static Uri makeResourceIdUri(String packageName, long resId) {
        checkPackage(packageName);
        return CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendEncodedPath("res")
            .appendPath(String.valueOf(resId))
            .build();
    }

    public static Uri makeResourceEntryUri(String packageName, String defType, String name) {
        checkPackage(packageName);
        return CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendEncodedPath("res")
            .appendPath(defType)
            .appendPath(name)
            .build();
    }

    public static Uri makeAssetPathUri(String packageName, String assetPath) {
        checkPackage(packageName);
        return CONTENT_URI.buildUpon()
            .appendPath(packageName)
            .appendEncodedPath("assets")
            .appendPath(assetPath)
            .build();
    }

    /**
     * make file URI to read file through PackageResourcesProvider
     * @param uri File Uri
     * @return
     */
    public static Uri convertFilePathUri(Uri uri) {
        if(uri==null || !uri.getScheme().equals("file")){
            return uri;
        }
        return Uri.parse("content://" + AUTHORITY + '/' + uri.toString());
    }
}
