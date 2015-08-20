package com.lewa.themes.provider;

import java.io.Serializable;

import com.android.internal.app.ThemeUtils;
import com.lewa.themes.Manifest;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.Utils;
import com.lewa.themes.provider.Themes.ThemeColumns;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.CustomTheme;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import static com.lewa.themes.ThemeManager.STANDALONE;;

/**
 * Simple data access object designed to wrap a cursor returned from any of the
 * Themes class APIs. Can be used efficiently with a custom CursorAdapter.
 *
 * <h2>Usage</h2>
 * <p>
 * Here is an example of looping through a Cursor with ThemeItem:
 * </p>
 *
 * <pre class="prettyprint">
 * ThemeItem item = new ThemeItem(Themes.listThemes(myContext));
 * try {
 *     while (c.moveToNext()) {
 *         // Do something with the item
 *     }
 * } finally {
 *     item.close();
 * }
 * </pre>
 */

public class ThemeItem extends AbstractDAOItem {
    private static final Uri CONTENT_URI
            = Uri.parse("content://" + PackageResources.AUTHORITY);
    private int mColumnId;
    private int mColumnThemeId;
    private int mColumnThemePackage;
    private int mColumnName;
    private int mColumnStyleName;
    private int mColumnAuthor;
    private int mColumnIsDRM;
    private int mColumnWallpaperName;
    private int mColumnWallpaperUri;
    private int mColumnLockWallpaperUri;
    private int mColumnRingtoneName;
    private int mColumnRingtoneUri;
    private int mColumnNotifRingtoneName;
    private int mColumnNotifRingtoneUri;
    private int mColumnThumbnailUri;
    private int mColumnIsSystem;
    private int mColumnIsApplied;
    private int mColumnPreviewUri;
    private int mColumnHasHostDensity;
    private int mColumnHasThemePackageScope;

    private int mColumnBootAnimationUri;
    private int mColumnFontUri;
    private int mColumnLockscreenUril;
    private int mColumnIconsUril;

    private int mColumnSize;
    private int mColumnVersionCode;
    private int mColumnVersionName;

    private int mColumnIsImageFile;
    private int mColumnDownloadPath;

    private List<Uri> mPreviewUris;
    private List<List<Uri>> mPreviewUrisByTypes;

    private List<Uri> mThumbnailUris;
    private List<List<Uri>> mThumbnailUrisByTypes;
    
 // Begin, added by yljiang@lewatek.com 2013-11-19 
    private int mColumnMechanismVersion;
    private int mColumnApplyPackages;
    private int mColumnInCallStyle;
    private int mColumnMessageRingtoneName;
    private int mColumnMessageRingtoneUri;
 // End

    private static final AbstractDAOItem.Creator<ThemeItem> MCREATOR
            = new AbstractDAOItem.Creator<ThemeItem>() {
        @Override
        public ThemeItem init(Cursor c) {
            return new ThemeItem(c);
        }
    };

    /**
     * @see AbstractDAOItem.Creator#newInstance(Context, Uri)
     */
    public static ThemeItem getInstance(Context context, Uri uri) {
        return MCREATOR.newInstance(context, uri);
    }

    /**
     * @see AbstractDAOItem.Creator#newInstance(Cursor)
     */
    public static ThemeItem getInstance(Cursor c) {
        return MCREATOR.newInstance(c);
    }


    /**
     * {@inheritDoc}
     */
    public ThemeItem(Cursor c) {
        super(c);
        mColumnId = c.getColumnIndex(ThemeColumns._ID);
        mColumnThemeId = c.getColumnIndex(ThemeColumns.THEME_ID);
        mColumnThemePackage = c.getColumnIndex(ThemeColumns.THEME_PACKAGE);
        mColumnName = c.getColumnIndex(ThemeColumns.NAME);
        mColumnStyleName = c.getColumnIndex(ThemeColumns.STYLE_NAME);
        mColumnAuthor = c.getColumnIndex(ThemeColumns.AUTHOR);
        mColumnIsDRM = c.getColumnIndex(ThemeColumns.IS_DRM);
        mColumnWallpaperName = c.getColumnIndex(ThemeColumns.WALLPAPER_NAME);
        mColumnWallpaperUri = c.getColumnIndex(ThemeColumns.WALLPAPER_URI);
        mColumnLockWallpaperUri = c.getColumnIndex(ThemeColumns.LOCK_WALLPAPER_URI);
        mColumnRingtoneName = c.getColumnIndex(ThemeColumns.RINGTONE_NAME);
        mColumnRingtoneUri = c.getColumnIndex(ThemeColumns.RINGTONE_URI);
        mColumnNotifRingtoneName = c.getColumnIndex(ThemeColumns.NOTIFICATION_RINGTONE_NAME);
        mColumnNotifRingtoneUri = c.getColumnIndex(ThemeColumns.NOTIFICATION_RINGTONE_URI);
        mColumnThumbnailUri = c.getColumnIndex(ThemeColumns.THUMBNAIL_URI);
        mColumnIsSystem = c.getColumnIndex(ThemeColumns.IS_SYSTEM);
        mColumnIsApplied = c.getColumnIndex(ThemeColumns.IS_APPLIED);
        mColumnPreviewUri = c.getColumnIndex(ThemeColumns.PREVIEW_URI);
        mColumnHasHostDensity = c.getColumnIndex(ThemeColumns.HAS_HOST_DENSITY);
        mColumnHasThemePackageScope = c.getColumnIndex(ThemeColumns.HAS_THEME_PACKAGE_SCOPE);

        mColumnBootAnimationUri = c.getColumnIndex(ThemeColumns.BOOT_ANIMATION_URI);
        mColumnFontUri = c.getColumnIndex(ThemeColumns.FONT_URI);
        mColumnLockscreenUril = c.getColumnIndex(ThemeColumns.LOCKSCREEN_URI);
        mColumnIconsUril = c.getColumnIndex(ThemeColumns.ICONS_URI);

        mColumnSize = c.getColumnIndex(ThemeColumns.SIZE);
        mColumnVersionCode = c.getColumnIndex(ThemeColumns.VERSION_CODE);
        mColumnVersionName = c.getColumnIndex(ThemeColumns.VERSION_NAME);

     // Begin, added by yljiang@lewatek.com 2013-11-19 
        mColumnMechanismVersion  = c.getColumnIndex(ThemeColumns.MECHANISM_VERSION);
        mColumnApplyPackages  = c.getColumnIndex(ThemeColumns.APPLY_PACKAGES);
        mColumnInCallStyle  = c.getColumnIndex(ThemeColumns.INCALL_STYLE);
        mColumnMessageRingtoneName = c.getColumnIndex(ThemeColumns.MESSAGE_RINGTONE_NAME);
        mColumnMessageRingtoneUri = c.getColumnIndex(ThemeColumns.MESSAGE_RINGTONE_URI);
     // End
        
        mColumnIsImageFile = c.getColumnIndex(ThemeColumns.IS_IMAGE_FILE);
        mColumnDownloadPath = c.getColumnIndex(ThemeColumns.DOWNLOAD_PATH);
       
        mPreviewUrisByTypes = new ArrayList<List<Uri>>(8);
        for (int i = 0; i < 8; ++i) {
            mPreviewUrisByTypes.add(new ArrayList<Uri>());
        }
        mThumbnailUrisByTypes = new ArrayList<List<Uri>>(8);
        for (int i = 0; i < 8; ++i) {
            mThumbnailUrisByTypes.add(new ArrayList<Uri>());
        }
    }

    /**
     * @return the id for this item's row in the provider
     */
    public long getId() {
        return mCursor.getLong(mColumnId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uri getUri(Context context) {
        return Themes.getThemeUri(context, getPackageName(), getThemeId());
    }

    /**
     * @return the Theme name
     */
    public String getName() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnName);
    }

    /**
     * Access the name to be displayed for the theme when packages sans
     * wallpaper and ringtone. For different parts of the UI.
     *
     * @return the style name
     */
    public String getStyleName() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnStyleName);
    }

    /**
     * @return the Theme author
     */
    public String getAuthor() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnAuthor);
    }

    /**
     * @return true if this theme contains DRM content
     */
    public boolean isDRMProtected() {
        return mCursor.getInt(mColumnIsDRM) != 0;
    }

    /**
     * @return the String Theme Id
     */
    public String getThemeId() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnThemeId);
    }

    /**
     * @return this theme's package
     */
    public String getPackageName() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnThemePackage);
    }

    /**
     * @return this theme's download path
     */
    public String getDownloadPath() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnDownloadPath);
    }

    /**
     * Requests a unique identifier for a wallpaper. Useful to distinguish
     * different wallpaper items contained in a single theme package. Though the
     * result appears to be a filename, it should never be treated in this way.
     * It is merely useful as a unique key to feed a BitmapStore surrounding
     * this theme package.
     *
     * @return the wallpaper identifier
     */
    public String getWallpaperIdentifier() {
        return mCursor.getString(mColumnWallpaperName);
    }

    /**
     * If this theme specifies a wallpaper, get the Uri.
     *
     * @param context the context of the caller
     * @return the wallpaper uri, or null if this theme doesn't specify one.
     */
    public Uri getWallpaperUri(Context context) {
        return parseUriNullSafe(mCursor.getString(mColumnWallpaperUri));
    }

    /**
     * If this theme specifies a lockscreen wallpaper, get the Uri.
     *
     * @param context the context of the caller
     * @return the lockscreen wallpaper uri, or null if this theme doesn't
     *         specify one.
     */
    public Uri getLockWallpaperUri(Context context) {
        return parseUriNullSafe(mCursor.getString(mColumnLockWallpaperUri));
    }

    /**
     * If this theme specifies a ringtone, get the Uri.
     *
     * @param context the context of the caller
     * @return the ringtone uri, or null if this theme doesn't specify one.
     */
    public Uri getRingtoneUri(Context context) {
        return parseUriNullSafe(mCursor.getString(mColumnRingtoneUri));
    }

    /**
     * @return the name of the ringtone or null if this theme doesn't specify one.
     */
    public String getRingtoneName() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnRingtoneName);
    }

    /**
     * If this theme specifies a notification ringtone, get the Uri.
     *
     * @param context the context of the caller
     * @return the notification ringtone uri, or null if this theme doesn't specify one.
     */
    public Uri getNotificationRingtoneUri(Context context) {
        return parseUriNullSafe(mCursor.getString(mColumnNotifRingtoneUri));
    }

    /**
     * @return the name of the notification ringtone or null if this theme
     *         doesn't specify one.
     */
    public String getNotificationRingtoneName() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnNotifRingtoneName);
    }

    /**
     * A theme may specify a thumbnail to represent a theme.
     *
     * @return the thumbnail uri, or null if this theme doesn't specify one.
     */
    public Uri getThumbnailUri() {
        if(isClosed())
            return null ;
        return parseUriNullSafe(mCursor.getString(mColumnThumbnailUri));
    }

    public Uri getPreviewUri() {
        if(isClosed())
            return null ;
        return parseUriNullSafe(mCursor.getString(mColumnPreviewUri));
    }

    /**
     * A theme may specify a preview image to represent a theme.
     *
     * @param orientation
     *            the screen orientation for which a preview image is desired.
     *            Orientation values come from
     *            {@link android.content.res.Configuration}
     * @return the preview image uri, or null if this theme doesn't specify one.
     */
    public Uri getPreviewUri(int orientation) {
        Uri uri = parseUriNullSafe(mCursor.getString(mColumnPreviewUri));
        if (null != uri) {
            uri = uri
                    .buildUpon()
                    .appendQueryParameter(Themes.KEY_ORIENTATION,
                            String.valueOf(orientation)).build();
        }
        return uri;
    }

    /**
     * @return the Uri of the boot animation
     */
    public Uri getBootAnimationUri() {
        if(isClosed())
            return null ;
        return parseUriNullSafe(mCursor.getString(mColumnBootAnimationUri));
    }

    /**
     * @return the Uri of the font
     */
    public Uri getFontUril() {
        if(isClosed())
            return null ;
        return parseUriNullSafe(mCursor.getString(mColumnFontUri));
    }

    /**
     * @return the Uri of the lockscreen
     */
    public Uri getLockscreenUri() {
        if(isClosed())
            return null ;
        return parseUriNullSafe(mCursor.getString(mColumnLockscreenUril));
    }

    /**
     * @return the Uri of the icons package
     */
    public Uri getIconsUri() {
        if(isClosed())
            return null ;
        return parseUriNullSafe(mCursor.getString(mColumnIconsUril));
    }

    /** @deprecated */
    public String getSoundPackName() {
        return null;
    }

    /**
     * Tests whether the theme item can be uninstalled. This condition is true
     * for all theme APKs not part of the system image.
     *
     * @return Returns true if the theme can be uninstalled.
     */
    public boolean isRemovable() {
        return mCursor.getInt(mColumnIsSystem) == 0;
    }

    /**
     * @return true if this theme is currently applied
     */
    public boolean isApplied() {
        return mCursor.getInt(mColumnIsApplied) != 0;
    }

    /**
     * @return true if this theme has assets compiled for the current host's
     *         display ensity.
     */
    public boolean hasHostDensity() {
        return mCursor.getInt(mColumnHasHostDensity) != 0;
    }

    /**
     * @return true if this theme has assets compiled in the theme package
     * scope (0x0a as opposed to 0x7f).
     *
     * Woody Guo @ 2012/08/20: return true if there are resource redirections defined
     * in the theme package. Return false if this packages doesn't contain any redirections.
     */
    public boolean hasThemePackageScope() {
        return mCursor.getInt(mColumnHasThemePackageScope) != 0;
    }

    /**
     * Compares the internal T-Mobile theme object to this ThemeItem. For internal use.
     *
     * @param theme the CustomTheme object to compare
     * @return
     */
    public boolean equals(CustomTheme theme) {
        if (theme == null) {
            return false;
        }
        if (getPackageName().equals(theme.getThemePackageName()) == false) {
            return false;
        }
        return theme.getThemeId().equals(getThemeId());
    }

    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append('{');
        b.append("pkg=").append(getPackageName()).append("; ");
        b.append("themeId=").append(getThemeId()).append("; ");
        b.append("name=").append(getName()).append("; ");
        b.append("drm=").append(isDRMProtected());
        b.append('}');

        return b.toString();
    }

    public Uri getThumbnails(Context context, PreviewsType type) {
        String pkgName = getPackageName();
        if(pkgName.startsWith("com.lewa.font.inner.")){
            return Uri.parse("file:///system/fonts/" + getFontUril().toString() + ".jpg");
        }
        int resId;
        switch (type) {
        case DESKWALLPAPER:
            if (isImageFile()) {
                return getWallpaperUri(context);
            }
            return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("res")
                    .appendPath("drawable").appendPath("wallpaper").build();
        case LOCKWALLPAPER:
            if (isImageFile()) {
                return getLockWallpaperUri(context);
            }
            return null;
        case LOCKSCREEN:
            //add by Fan.Yang for standalone default theme
            if (Manifest.STANDALONE && Manifest.PACKAGE_NAME.equals(getPackageName())) {
                return CONTENT_URI.buildUpon().appendPath(Manifest.PACKAGE_NAME).appendEncodedPath(
                        "assets").appendPath("thumbnail").appendPath("thumbnail_lockscreen.jpg").build();
            } else if (Manifest.STANDALONE && ThemeManager.THEME_LOCKSCREEN2_PACKAGE.equals(getPackageName())) {
                return CONTENT_URI.buildUpon().appendPath(Manifest.PACKAGE_NAME).appendEncodedPath(
                        "assets").appendPath("lockscreen2").appendPath("thumbnail_lockscreen.png").build();
            }
            return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("res")
                    .appendPath("drawable").appendPath("thumbnail_lockscreen").build();
        case BOOT_ANIMATION:
            return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("res")
                    .appendPath("drawable").appendPath("thumbnail_bootanimation").build();
        case LAUNCHER_ICONS:
            //add by Fan.Yang for standalone default theme
            if (Manifest.STANDALONE && Manifest.PACKAGE_NAME.equals(getPackageName())) {
                return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("assets")
                        .appendPath("thumbnail").appendPath("thumbnail_launcher.jpg").build();
            }
            return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("res")
                    .appendPath("drawable").appendPath("thumbnail_launcher").build();
        case FRAMEWORK_APPS:
            return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("res")
                    .appendPath("drawable").appendPath("thumbnail_systemapp").build();
        case FONTS:
            return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("res")
                    .appendPath("drawable").appendPath("thumbnail_fonts").build();
        case OTHER:
            return CONTENT_URI.buildUpon().appendPath(pkgName).appendEncodedPath("res")
                    .appendPath("drawable").appendPath("thumbnail_other").build();
        case LIVE_WALLPAPER:
            return getThumbnailUri();
        default:
            return null;
        }
    }
    // Woody Guo @ 2012/07/09: Return uris of preview images
    // content://PackageResources.AUTHORITY/com.lewa.theme.Dream/assets/preview/preview-2.png
    /**
     * @return A list of Uris of the preview pictures, or null if no preview picutre exists.
     */
    public List<Uri> getPreviews(Context context) {
        if (null != mPreviewUris) {
            return mPreviewUris;
        }
        String pkgName = getPackageName();
        String previewFile = "preview/";
        // add by Fan.Yang, add lockscreen2 lock style
        // 如果是lockscreen2的锁屏样式，把资源包定向到themechooser的lockscreen2文件夹下面
        if (pkgName.equals(ThemeManager.THEME_LOCKSCREEN2_PACKAGE)) {
            previewFile = "lockscreen2/";
            pkgName = ThemeManager.THEME_ELEMENTS_PACKAGE;
        }
        List<String> files = new ArrayList<String>();
        try {
            String[] lists;
            try {
                lists = context.createPackageContext(pkgName, 0)
                .getAssets().list("preview");
            } catch (Exception e) {
                lists = Utils.getFileResources(context, getDownloadPath()).getAssets().list("preview");
            }
            files = Arrays.asList(lists);
            mPreviewUris = new ArrayList<Uri>(files.size());
            if (files.size() > 0) {
                Collections.sort(files, sPreviewComparator);
                for (String f : files) {
                    mPreviewUris.add(CONTENT_URI.buildUpon()
                            .appendPath(pkgName)
                            .appendEncodedPath("assets")
                            .appendPath(previewFile + f).build());
                }
                int idxStart = mPreviewUris.get(0).toString().lastIndexOf('/') + 7;
                String s;
                int index = 0;
                for (Uri uri : mPreviewUris) {
                    s = uri.toString();
                    if (index == 0 && s.indexOf("lock", idxStart) >= 0||s.indexOf("icon", idxStart)>0) {
                        mPreviewUrisByTypes.get(index).add(uri);
                    } else if (index <= 1 && s.indexOf("boot", idxStart) >= 0) {
                        index = 1;
                        mPreviewUrisByTypes.get(index).add(uri);
                    } else if (index <= 2 && s.indexOf("launcher", idxStart) >= 0) {
                        index = 2;
                        mPreviewUrisByTypes.get(index).add(uri);
                    } else if (index <= 3 && s.indexOf("system", idxStart) >= 0) {
                        index = 3;
                        mPreviewUrisByTypes.get(index).add(uri);
                    } else if (index <= 4 && s.indexOf("font", idxStart) >= 0) {
                        index = 4;
                        mPreviewUrisByTypes.get(index).add(uri);
                    // Add for lock screen & launcher wallpaper preview
                    } else if (index <= 5 && s.indexOf("desktop", idxStart) >= 0) {
                        index = 5;
                        mPreviewUrisByTypes.get(index).add(uri);
                    } else {
                        break;
                    }
                }
                final int append = "/preview_".length();
                if(STANDALONE){//filter preview type for standalone version
                    List<Uri> temp = new ArrayList<Uri>();
                    for (Uri uri : mPreviewUris) {
                        String name = uri.getPath();
                        int i = name.lastIndexOf('/') + append;
                        if(name.length() > i){
                            name = name.substring(i);
                        }
                        if(name.startsWith("launcher") || name.startsWith("lock") || name.startsWith("icon")){
                            temp.add(uri);
                        }
                    }
                    mPreviewUris = temp;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return mPreviewUris;
    }
//    public List<Uri> getThumbnails(Context context, PreviewsType type) {
//        mThumbnailUris=null;
//        if (null == mThumbnailUris) {
//            getThumbnails(context);
//        }
//        if (mThumbnailUris==null||0 >= mThumbnailUris.size()) {
//            return null;
//        }
//        return mThumbnailUrisByTypes.get(type.index());
//    }
    /**
     * @return A list of Uris of the preview pictures of the given type,
     * or null if no preview picutre exists.
     */
    // Begin, added by yljiang@lewatek.com 2013-11-29
    private final HashMap<PreviewsType, List<Uri>> hashMapPreviewUris = new HashMap<PreviewsType, List<Uri>>() ;
    
    public void putPreviewUris(PreviewsType key,List<Uri> value) {
        hashMapPreviewUris.put(key, value);
    }

    public void setPreviewUris(List<Uri> mPreviewUris) {
        this. mPreviewUris = mPreviewUris;
    }
    // End

    public List<Uri> getPreviews(Context context, PreviewsType type) {
        
        // Begin, added by yljiang@lewatek.com 2013-11-29
        if(hashMapPreviewUris != null && getMechanismVersion() > 0 ) {
            List<Uri> list =  hashMapPreviewUris.get(type);
            if(list != null)
                return list;
        }
        // End
        String pkgName = getPackageName();
        if(pkgName.startsWith("com.lewa.font.inner.")){
            List<Uri> list = new ArrayList<Uri>();
            list.add(Uri.parse("file:///system/fonts/" + getFontUril().toString() + ".jpg"));
            return list;
        }
        // is_image_file字段表示资源不是从主题包中获取
        if (isImageFile()) {
            if (type == PreviewsType.DESKWALLPAPER) {
                List<Uri> prev = new ArrayList<Uri>(1);
                prev.add(getWallpaperUri(context));
                return prev;
            //add by Fan.Yang for standalone default theme
            } else if (type==PreviewsType.LOCKSCREEN || type==PreviewsType.LOCKWALLPAPER) {
                List<Uri> prev = new ArrayList<Uri>(1);
                prev.add(getLockWallpaperUri(context));
                return prev;
            }
            return null;
        }
        if (null == mPreviewUris) {
            getPreviews(context);
        }
        if (mPreviewUris == null || mPreviewUris.size() <= 0) {
            return null;
        }
        int index = type.index();
        if(type == PreviewsType.DEFAULT_THEME_WALLPAPER){
            index = 5;
        }
        return mPreviewUrisByTypes.get(index);
    }

    /**
     * Types of preview pictures
     *
     * Preview pictures should be showed in this order.
     */
    public enum PreviewsType {
        LOCKSCREEN(0), BOOT_ANIMATION(1), LAUNCHER_ICONS(2), FRAMEWORK_APPS(3), FONTS(4)
        , OTHER(5), DESKWALLPAPER(6), LOCKWALLPAPER(7), LIVE_WALLPAPER(8), DEFAULT_THEME_WALLPAPER(9);

        private final int index;

        PreviewsType(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    };

    public static void sortPreviews(List list) {
        Collections.sort(list, sPreviewComparator);
    }

    // Naming convention:
    // - Lockscreen:        preview_lockscreen_0        preview_lockscreen_1        ...
    // - Boot animation:    preview_bootanimation_0     preview_ bootanimation_1    ...
    // - Launcher icons:    preview_launcher_0          preview_ launcher_1         ...
    // - Sys/apps:          preview_systemapp_0         preview_systemapp_1         ...
    // - Font:              preview_fonts_0             preview_ fonts_1            ...
    // - Other:             preview_other_0             preview_other_1             ...
    private final static PreviewComparator sPreviewComparator = new PreviewComparator();
    private final static class PreviewComparator implements Comparator<String>{
        private int[] doCompare(String o1, String o2, String key) {
            // positive – o1 is greater than o2
            // zero – o1 equals to o2
            // negative – o1 is less than o2
            int[] result = new int[2];
            result[0] = 0;
            if (o1.indexOf(key) >= 0) {
                if (o2.indexOf(key) < 0) {
                    result[1] = -1;
                    return result;
                }
                result[1] = o1.compareTo(o2);
                return result;
            }

            if (o2.indexOf(key) >= 0) {
                result[1] = 1;
                return result;
            }

            result[0] = -1;
            return result;
        }

        public int compare(String o1, String o2) {
            int[] result = doCompare(o1, o2, "lock");
            if (result[0] >= 0) return result[1];

            result = doCompare(o1, o2, "boot");
            if (result[0] >= 0) return result[1];

            result = doCompare(o1, o2, "launcher");
            if (result[0] >= 0) return result[1];

            result = doCompare(o1, o2, "system");
            if (result[0] >= 0) return result[1];

            result = doCompare(o1, o2, "fonts");
            if (result[0] >= 0) return result[1];

            return o1.compareTo(o2);
        }
    }

    /**
     * @return the size of this theme package
     */
    public long getSize() {
        return mCursor.getLong(mColumnSize);
    }

    // Woody Guo @ 2012/07/13
    /**
     * @return the Uri of this theme package so it can be shared
     */
    public Uri getShareUri(Context context) {
        return Uri.fromFile(new File(context.getFilesDir(), getThemeId() + ".lwt"));
    }

    // Woody Guo @ 2012/07/21
    // Copy the theme package to /data/data/<app>/files/ for sharing purpose
    /**
     * @return true if the package is successfully prepared for sharing
     */
    public boolean prepareShare(Context context) {
        if ((new File(context.getFilesDir(), getThemeId() + ".lwt")).exists()) {
            // Do not re-copy the package if it already exists
            return true;
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        boolean success = false;
        try {
            File pkgFile = new File(context.getPackageManager().getPackageInfo(
                    getPackageName(), 0).applicationInfo.sourceDir);
            in = new FileInputStream(pkgFile);
            out = context.openFileOutput(getThemeId() + ".lwt", Context.MODE_WORLD_READABLE);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.getFD().sync();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception e1) {
            }
            try {
                out.close();
            } catch (Exception e1) {
            }
        }
        return success;
    }

    /**
     * @return the version number of this theme package, as specified by
     * the <manifest> tag's versionCode attribute.
     */
    public int getVersionCode() {
        return mCursor.getInt(mColumnVersionCode);
    }

    /**
     * @return the version name of this theme package, as specified by
     * the <manifest> tag's versionName attribute.
     */
    public String getVersionName() {
        return mCursor.getString(mColumnVersionName);
    }

    
 // Begin, added by yljiang@lewatek.com 2013-11-19 
    
    public boolean isClosed()
    {
        if(mCursor != null && !mCursor.isClosed())
        {
            return false ;
        }
        return true ;
    }
    public int getMechanismVersion() {
        if(isClosed())
            return  0 ;
        return mCursor.getInt(mColumnMechanismVersion);
    }
    
    public String getApplyPackages() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnApplyPackages);
    }
    public Uri getInCallStyleUri() {
        if(isClosed())
            return null ;
        return parseUriNullSafe(mCursor.getString(mColumnInCallStyle));
    }

    public Uri getMessagetRingtoneUri(Context context) {
        return parseUriNullSafe(mCursor.getString(mColumnMessageRingtoneUri));
    }

    public String getMessageRingtoneName() {
        if(isClosed())
            return null ;
        return mCursor.getString(mColumnMessageRingtoneName);
    }

 // End
    
    public boolean isImageFile() {
        return mCursor.getInt(mColumnIsImageFile) != 0;
    }

    /**
     * Status about an online theme
     *
     * - NOT_INSTALLED: The online theme is not installed on device
     * - INSTALLED:     The latest version of the online theme is installed on device
     * - OUTDATED:      The theme installed on device is outdated and can be updated
     * - DOWNLOADED:    The theme is downloaded but not installed. This status is only be decided
     *                  and set by ThemeChooser.
     */
    public enum Status { NOT_INSTALLED, INSTALLED, OUTDATED, DOWNLOADED };
}
