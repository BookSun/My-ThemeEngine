package util;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ThemeInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.Volley;
import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.newmechanism.Globals;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.Utils;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes.ThemeColumns;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import lewa.os.FileUtilities;

import static com.lewa.themes.ThemeManager.STANDALONE;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;


public class ThemeUtil {
    public static final String CONFIG_BRAND = Build.BRAND;
    public static final String CONFIG_DEVICE = lewa.os.Build.LEWA_DEVICE.equals("unknown") ? Build.DEVICE : lewa.os.Build.LEWA_DEVICE;
    public static final boolean CONFIG_IS_B2B = !lewa.os.Build.B2bVersion.equals("unknown");
    private static final boolean DEBUG = ThemeConstants.DEBUG;
    private static final String TAG = "ThemeUtil";
    // End
    private static final Runtime s_runtime = Runtime.getRuntime();
    private final static String DATA_PATH = Environment.getDataDirectory().getAbsolutePath();
    public static boolean isWVGA = false;
    public static String screenDPI = "";
    public static String THEME_INFO_URL;
    public static String THEME_URL;
    // Begin, When software is FC ,reset downloadThreads which are still
    // downloading, add by zjyu ,2012.5.15
    public static String dcontrolFlag = "true"; // 下载控制标签,为true表示可以加载
    public static boolean threadcontrolFlag = true; // 控制线程能否运行标签
    public static boolean startFlag = true; // 控制线程只启动一次
    public static ArrayList<ThemeBase> baselist = new ArrayList<ThemeBase>(); // 下载主题队列
    // End
    public static boolean isEN = false;
    public static boolean isUsingChanged = false;
    //    public static boolean isFontfinish=false;
    // Begin, added by yljiang@lewatek.com 2013-12-20
    public static boolean isKillProcess = false;
    //Only when the need to reboot to change the font,
    public static boolean isChangeFont = false;
    // End
    public static boolean isChangeIcon = false;
    public static int notifyId;
    public static int screen_width;
    public static int screen_height;
    public static int displayMetrics;
    public static float density;
    public static String userAgent = null;
    public static int thumbnailWidth;
    public static int thumbnailHeight;
    public static int previewWidth;
    public static int previewHeight;
    private static boolean sLockscreenTested = false;
    private static RequestQueue mRequestQueue = null;

    public static void updateCurrentThemeInfo(ThemeItem item) {
        ThemeUtil.isKillProcess = item.getFontUril() != null;
        ThemeUtil.isChangeIcon = item.getIconsUri() != null;
    }

    /**
     * the network is work well?
     *
     * @param context
     * @return
     */
    public static boolean isNetWorkEnable(Context context) {
        boolean isNetworkAvailable = false;
        try {
            ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager.getActiveNetworkInfo() != null) {
                isNetworkAvailable = connManager.getActiveNetworkInfo()
                        .isAvailable();
            }
        } catch (Exception e) {
            // ignore
        }
        return isNetworkAvailable;
    }

    /**
     * the network type,is wifi or gprs?
     *
     * @param context
     * @return
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .getState();
        if (state == State.CONNECTED || state == State.CONNECTING) {
            return "wifi";
        }

        state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .getState();
        if (state == State.CONNECTED || state == State.CONNECTING) {
            return "mobile";
        }
        return "none";

    }

    /**
     * 初如化当前语言环境
     */
    public static void initLocale(Context context) {
        boolean isChn = context.getResources().getConfiguration().locale
                .getLanguage().equals("zh");
        if (isChn) {
            ThemeUtil.isEN = false;
        } else {
            ThemeUtil.isEN = true;
        }
    }

    public static String fileLengthToSize(long length) {

        StringBuilder sb = new StringBuilder();
        long file_B = length % 1024;
        long file_KB = length / 1024;
        long file_KB_B = file_B;
        long file_MB = file_KB / 1024;
        long file_MB_KB = file_KB % 1024;
        // The order can't change
        if (file_MB > 0) {
            sb.append(file_MB).append(".")
                    .append(String.valueOf(file_MB_KB).substring(0, 1))
                    .append("MB");
        } else if (file_KB > 0) {
            sb.append(file_KB).append(".")
                    .append(String.valueOf(file_KB_B).substring(0, 1))
                    .append("KB");
        } else if (file_B > 0) {
            sb.append(length).append("B");
        }
        return sb.toString();
    }

    public static void initURL() {
        if (isWVGA) {
            THEME_URL = new StringBuilder().append(ThemeConstants.URL)
                    .append("/wvga/theme").toString();
            THEME_INFO_URL = new StringBuilder().append(ThemeConstants.URL)
                    .append("/wvga/theme/Themes.xls").toString();
            // WALLPAPER_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/wallpaper").toString();
            // WALLPAPER_PREVIEW_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/wallpaper/preview").toString();
            // WALLPAPER_THUMBNAIL_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/wallpaper/thumbnail").toString();
            // WALLPAPER_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/wallpaper/thumbnail/Wallpaper.xls").toString();
            // //LOCKSCREEN_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/theme").toString();
            // LOCKSCREEN_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/theme/Lockscreen.xls").toString();
            // //FONTS_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/theme").toString();
            // FONTS_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/theme/Fonts.xls").toString();
            // //BOOTS_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/theme").toString();
            // BOOTS_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/wvga/theme/BootAnimation.xls").toString();
        } else {

            THEME_URL = new StringBuilder().append(ThemeConstants.URL)
                    .append("/hvga/theme").toString();
            THEME_INFO_URL = new StringBuilder().append(ThemeConstants.URL)
                    .append("/hvga/theme/Themes.xls").toString();
            // WALLPAPER_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/wallpaper").toString();
            // WALLPAPER_PREVIEW_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/wallpaper/preview").toString();
            // WALLPAPER_THUMBNAIL_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/wallpaper/thumbnail").toString();
            // WALLPAPER_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/wallpaper/thumbnail/Wallpaper.xls").toString();
            // //LOCKSCREEN_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/theme").toString();
            // LOCKSCREEN_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/theme/Lockscreen.xls").toString();
            // //FONTS_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/theme").toString();
            // FONTS_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/theme/Fonts.xls").toString();
            // //BOOTS_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/theme").toString();
            // BOOTS_INFO_URL = new
            // StringBuilder().append(ThemeConstants.URL).append("/hvga/theme/BootAnimation.xls").toString();
        }
    }

    public static String getNameNoBuffix(String pkg) {
        String name = null;
        if (pkg != null) {
            if (pkg.lastIndexOf(".") != -1) {
                name = pkg.substring(0, pkg.lastIndexOf("."));
            } else {
                name = pkg;
            }
        }
        return name;
    }

    // Begin, When software is FC ,reset downloadThreads which are still
    // downloading, add by zjyu ,2012.5.15
    public static void resetDownloadThread() {
        // OnlineSlideBase osb = new ThemePkg();
        // Context context = osb;
        // SharedPreferences.Editor editor =
        // context.getSharedPreferences("DOWNLOADED",
        // Context.MODE_PRIVATE).edit();
        // while (!baselist.isEmpty()) {
        // editor.putLong(baselist.get(0).getPkg(),
        // ThemeConstants.DOWNLOADFAIL);
        // baselist.remove(0);
        // }
        // editor.commit();
    }

    public static void runGC() {
        long usedMem1 = usedMemory(), usedMem2 = Long.MAX_VALUE;
        for (int i = 0; (usedMem1 < usedMem2) && (i < 20); ++i) {
            s_runtime.runFinalization();
            s_runtime.gc();
            Thread.currentThread();
            Thread.yield();
            usedMem2 = usedMem1;
            usedMem1 = usedMemory();
        }
    }

    private static long usedMemory() {
        return s_runtime.totalMemory() - s_runtime.freeMemory();
    }

    public static Options getOptions(String filePath, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeFile(filePath, options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize2(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
        } catch (Exception e) {
        }
        return options;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options An options object with out* params already populated (run through a decode*
     *            method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    // porting from lewa.util.ImageResizer.java by Fan.Yang
    public static int calculateInSampleSize2(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    public static Options getOptions(String filePath) {
        Options options = new Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        final int minSideLength = Math.min(options.outWidth, options.outHeight);
        options.inSampleSize = computeSampleSize(options, minSideLength,
                options.outWidth * options.outHeight);
        options.inJustDecodeBounds = false;
        options.inInputShareable = true;
        options.inPurgeable = true;
        return options;
    }

    public static int computeSampleSize(BitmapFactory.Options options,
                                        int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
                                                int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
                .sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math
                .floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    /**
     * the sdcard space
     *
     * @param sizeMb
     * @return
     */
    public static boolean sdcardHasSpace(int sizeMb) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {

            String sdcard = Environment.getExternalStorageDirectory().getPath();
            StatFs statFs = new StatFs(sdcard);
            long blockSize = statFs.getBlockSize();
            long blocks = statFs.getAvailableBlocks();
            long availableSpare = (blocks * blockSize) / (1024 * 1024);
            if (sizeMb > availableSpare) {
                return false;
            } else {
                return true;
            }
        }
        return false;

    }

    public static boolean isDataHasSpace(long sizeMb) {
        StatFs statFs = new StatFs(DATA_PATH);
        long blockSize = statFs.getBlockSize();
        long blocks = statFs.getAvailableBlocks();
        long availableSpare = (blocks * blockSize) / (1024 * 1024);
        if (sizeMb > availableSpare) {
            Log.e(TAG, String.format("data has no space %d / %d", availableSpare, sizeMb));
            return false;
        } else {
            return true;
        }
    }

    /**
     * 可以删除指定路径的文件和目录
     *
     * @param filePath
     */
    public static void deleteFile(String filePath) {
        try {
            FileUtilities.deleteIfExists(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void deleteLiveWallpaper(Activity activity, ThemeItem mThemeItem) {
        String apkFileName = mThemeItem.getName() + "_v" + mThemeItem.getVersionCode() + ".lwt";

        ThemeUtil.deleteFile(new StringBuilder()
                .append(ThemeConstants.THEME_LWT).append("/").append(apkFileName)
                .toString());
        activity.getContentResolver().delete(ThemeColumns.CONTENT_PLURAL_URI, ThemeColumns.THEME_PACKAGE + "='" + mThemeItem.getPackageName() + "'", null);
        activity.finish();
    }

    /**
     * 本地主题删除调用的接口
     *
     * @param activity
     * @param themeItem
     */

    public static void deleteThemeInfo(Activity activity, ThemeItem themeItem) {
        String themePkg = themeItem.getName() + ".lwt";
        String themeName = themeItem.getName();

        ThemeApplication.sThemeStatus.setDeleted(
                themeItem.getPackageName(), themePkg, ThemeStatus.THEME_TYPE_PACKAGE);

        ThemeUtil.deleteFile(new StringBuilder()
                .append(ThemeConstants.THEME_LWT).append("/").append(themePkg)
                .toString());

        ThemeUtil.deleteFile(new StringBuilder()
                .append(ThemeConstants.THEME_LOCAL_PREVIEW).append("/")
                .append(themeName).toString());

        /**
         * 因为以lockscreen_pkgname作为首页缩略图，但可能此主题不包含锁屏，所以在此删除
         */
        ThemeUtil.deleteFile(new StringBuilder()
                .append(ThemeConstants.THEME_LOCAL_THUMBNAIL).append("/")
                .append(ThemeConstants.THEME_THUMBNAIL_LOCKSCREEN_PREFIX)
                .append(themeName).toString());

        if (themeItem.isImageFile()) {
            activity.getContentResolver().delete(themeItem.getUri(activity), null, null);
            Uri uri = themeItem.getWallpaperUri(activity);
            if (null == uri) {
                uri = themeItem.getLockWallpaperUri(activity);
            }
            String s = null;
            try {
                s = uri.toString();
                s = s.substring(7, s.length());
                (new File(s)).delete();
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete file: " + s + " " + e);
            }
        } else {
            //delete external theme file log
            activity.getContentResolver().delete(themeItem.getUri(activity), null, null);
            //delete external theme package
            try {
                FileUtilities.deleteIfExists(themeItem.getDownloadPath());
            } catch (IOException e) {
            }
        }

        activity.finish();
    }

    /**
     * 删除主题以及与之有关的所有资源和信息(在线主题删除调用的接口)
     *
     * @param activity
     * @param themeBase
     */
    public static void deleteThemeInfo(Activity activity, ThemeBase themeBase) {
        /**
         * 如果主题对象中的lwtPath不为空，则表示此主题包文件不在/theme/lwt内
         */
        if (themeBase.getLwtPath() != null) {
            /**
             * 删除.lwt源文件
             */
            ThemeUtil.deleteFile(themeBase.getLwtPath());
            /**
             * 删除此lwt文件对应的预览图和信息
             */
            ThemeUtil.deleteFile(new StringBuilder()
                    .append(ThemeConstants.THEME_LOCAL_PREVIEW).append("/")
                    .append(themeBase.getName()).toString());
            activity.finish();
            return;
        }
        ContentResolver cr = activity.getContentResolver();
        cr.delete(ThemeColumns.CONTENT_PLURAL_URI, ThemeColumns.THEME_PACKAGE + "='" + themeBase.getPackageName() + "'", null);

        String themePkg = themeBase.getPkg();
        String themeName = themeBase.getName();

//        ((ThemeAppliction) activity.getApplication()).themeStatus.setDeleted(
//                themeBase.getPackageName(), themePkg, ThemeStatus.THEME_TYPE_PACKAGE);

        if (themePkg.endsWith("lwt")) {
            themePkg = themePkg.substring(0, themePkg.length() - 4) + "_v" + themeBase.getVersionCode() + ".lwt";
        }
        ThemeUtil.deleteFile(new StringBuilder()
                .append(ThemeConstants.THEME_LWT).append("/").append(themePkg)
                .toString());

        ThemeUtil.deleteFile(new StringBuilder()
                .append(ThemeConstants.THEME_LOCAL_PREVIEW).append("/")
                .append(themeName).toString());

        /**
         * 因为以lockscreen_pkgname作为首页缩略图，但可能此主题不包含锁屏，所以在此删除
         */
        ThemeUtil.deleteFile(new StringBuilder()
                .append(ThemeConstants.THEME_LOCAL_THUMBNAIL).append("/")
                .append(ThemeConstants.THEME_THUMBNAIL_LOCKSCREEN_PREFIX)
                .append(themeName).toString());

//        activity.finish();
    }

    /**
     * 从其他程序中选择图片裁剪，并作为锁屏壁纸或者桌面
     *
     * @param activity
     * @param arb
     * @param aspectX     宽比例
     * @param aspectY     高比例
     * @param outputX     宽像素
     * @param outputY     高像素
     * @param requestCode
     */
    public static void cropImageFromGallery(Fragment f, int aspectX, int aspectY, int outputX, int outputY, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", aspectX);
        intent.putExtra("aspectY", aspectY);
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("return-data", true);

        // The following lines are provided and maintained by Mediatek inc.
        intent.putExtra("scaleUpIfNeeded", true);
        // The following lines are provided and maintained by Mediatek inc.
        f.startActivityForResult(intent, requestCode);
    }

    /**
     * create thumbnail with the screen size(WVGA or HVGA)
     *
     * @param is
     * @param fos
     * @param quality    压缩质量
     * @param changeSize 是否生成缩略图，如果为true，则根据屏幕分辨率生成对应的缩略图
     * @return
     */
    public static boolean createThumbnail(Context context, InputStream source, File target,
                                          int quality, boolean changeSize, int type) {

        Bitmap bitmap = null;
        FileOutputStream fos = null;

        try {
            if (source == null) {
                return false;
            }
            if (!(ThemeStatus.THEME_TYPE_LIVEWALLPAPER == type)) {
                bitmap = BitmapFactory.decodeStream(source, null,
                        getOptions(target.getAbsolutePath(),
                                (int) (context.getResources().getDimension(R.dimen.thumbnail_width)),
                                (int) (context.getResources().getDimension(R.dimen.thumbnail_height)))
                );
            } else {
                bitmap = BitmapFactory.decodeStream(source, null,
                        getOptions(target.getAbsolutePath(),
                                (int) (context.getResources().getDimension(R.dimen.thumbnail_width)),
                                (int) (context.getResources().getDimension(R.dimen.dynamic_wallpaper_image_height)))
                );
            }

            if (bitmap == null) {
                if (DEBUG) Log.e(TAG, "Can't decode to Bitmap from IO!");
                return false;
            }

            if (changeSize) {
                boolean isWVGA = ThemeUtil.isWVGA;
                if (isWVGA) {
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap,
                            ThemeConstants.THUMBNAIL_WVGA_WIDTH,
                            ThemeConstants.THUMBNAIL_WVGA_HEIGHT,
                            ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                } else {
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap,
                            ThemeConstants.THUMBNAIL_HVGA_WIDTH,
                            ThemeConstants.THUMBNAIL_HVGA_HEIGHT,
                            ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                }
            }
            File parent = target.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            fos = new FileOutputStream(target);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);

            fos.flush();
            return true;
        } catch (OutOfMemoryError e) {
            Log.w(TAG, "System is low on memory. Please consider running garbage collection");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                    fos = null;
                }
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    bitmap = null;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }

        }

        return false;
    }

    public static String parseSafeUri(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    public static String isValidUri(Uri uri, Context mContext) {
        try {
            if (MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri) == null)
                return null;
        } catch (Exception e) {
            return null;
        }
        return uri.toString();
    }

    public static void getScreenSize(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        screen_width = displayMetrics.widthPixels;
        screen_height = displayMetrics.heightPixels;
    }

    public static void initUserAgent(Context context) {
        userAgent = HttpUtils.getUserAgent(context);
    }

    public static void initIconSize(Context context) {
        thumbnailWidth = (int) (context.getResources().getDimension(R.dimen.thumbnail_width));
        thumbnailHeight = (int) (context.getResources().getDimension(R.dimen.thumbnail_height));
        previewWidth = (int) (context.getResources().getDimension(R.dimen.preview_icon_selected_width));
        previewHeight = (int) (context.getResources().getDimension(R.dimen.preview_icon_selected_height));
    }

    public static Bitmap CreateCropBitmap(InputStream is) {
        Bitmap bm = BitmapFactory.decodeStream(is);
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scale = ((float) (screen_width * 2) / width) < ((float) screen_height / height) ? ((float) screen_height / height) : ((float) (screen_width * 2) / width);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, (int) (screen_width * 2 / scale), (int) (screen_height / scale), matrix, true);
        return newbm;
    }

    public static synchronized boolean supportsLockWallpaper(Context context) {
        return true;
    }

    public static void initApplication(final Application app) {
        ThemeApplication.APPLICATION = app;
        DisplayImageOptions options;

        //config default displayImageOptions
        options = new DisplayImageOptions.Builder().showImageOnFail(R.drawable.theme_no_default)
                .showImageForEmptyUri(R.drawable.theme_no_default)
                .showImageOnLoading(R.drawable.theme_no_default)
                .showImageOnFail(R.drawable.theme_no_default).cacheInMemory(true).cacheOnDisc(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY)
                .build();

        //config ImageLoaderConfiguration
        File cacheDir = StorageUtils.getCacheDirectory(app);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(app)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .threadPoolSize(3)
                .denyCacheImageMultipleSizesInMemory()
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .memoryCacheExtraOptions(480, 800)
                .memoryCache(new LruMemoryCache(20 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .memoryCacheSizePercentage(13) // default
                .discCache(new UnlimitedDiskCache(cacheDir)) // default
                .discCacheSize(50 * 1024 * 1024)
                .discCacheFileCount(500)
                .discCacheFileNameGenerator(new Md5FileNameGenerator())
                .defaultDisplayImageOptions(options)
                //.writeDebugLogs() // Remove for release app
                .build();
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config);
        ThemeApplication.sThemeStatus = new ThemeStatus(app);
        ThemeUtil.initUserAgent(app);
        ThemeUtil.initIconSize(app);
        ThemeUtil.getScreenSize(app);

        initVolley(app);
    }

    private static void initVolley(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
        VolleyLog.setTag("Volley");
        VolleyLog.DEBUG = true;
    }

    public static RequestQueue getInstence() {
        return (mRequestQueue != null) ? mRequestQueue
                : Volley.newRequestQueue(ThemeApplication.APPLICATION);
    }

    private static boolean isAppUpgraded(Context context) {
        final String TAG_VERSION = "version";
        boolean upgraded = false;
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int curVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            ;
            int perVersion = sp.getInt(TAG_VERSION, -1);
            upgraded = curVersion != perVersion;
            if (upgraded)
                sp.edit().putInt(TAG_VERSION, curVersion).commit();
        } catch (Exception e) {
        }
        return upgraded;
    }

    private static void clearExternalCacheDir(Context context) {
        final String[] dirs = {
                ThemeConstants.THEME_ONLINE_PREVIEW,
                ThemeConstants.THEME_ONLINE_THUMBNAIL,
                ThemeConstants.THEME_LOCAL_PREVIEW,
                ThemeConstants.THEME_LOCAL_THUMBNAIL,
        };
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File dir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + context.getPackageName() + "/cache");
                if (dir.exists())
                    FileUtilities.cleanDirectory(dir);
                for (String p : dirs) {
                    File d = new File(p);
                    if (d.exists()) {
                        FileUtilities.cleanDirectory(d);
                    }
                }
            }
            FileUtilities.cleanDirectory(context.getCacheDir());
        } catch (Exception e) {
        }
    }

    public static void onLowMemory() {

    }

    /**
     * clear unnecessary packages
     *
     * @param context
     * @return changed
     */
    public static boolean clearTheme(Context context) {
        boolean changed = false;
        IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        String applied = ThemeApplication.sThemeStatus.getAppliedPkgName(ThemeStatus.THEME_TYPE_STYLE);
        final String data = Environment.getRootDirectory().getAbsolutePath();
        for (PackageInfo info : android.app.LewaThemeHelper.getInstalledThemePackages(context)) {
            String pkg = info.packageName;
            if (!pkg.equals(applied) && !info.applicationInfo.sourceDir.startsWith(data)) {
                PackageDeleteObserver obs = new PackageDeleteObserver();
                //Delete for standalone by Fan.Yang
                // try {
                    obs.finished = true;//Add for standalone by Fan.Yang
                    //pm.deletePackage(pkg, obs, 0);
                    synchronized (obs) {
                        while (!obs.finished) {
                            try {
                                obs.wait();
                            } catch (Exception e) {
                            }
                        }
                    }
                    changed = true;
                    Log.d(TAG, "deletedPackage:" + info.applicationInfo.sourceDir);
                /*} catch (RemoteException e) {
                    Log.e(TAG, "deletePackage:" + pkg);
                }*/
            }
        }
        return changed;
    }

    public static void updateThemeInfo(Context context, boolean uninstallUnused, boolean killProcess, boolean fileHash) {
        boolean uninstalled = false;
        String pkgName = null;
        String lockwallpaper = null;
        String wallpaper = null;
        int isImageFile = 0;
        String themePkg = null;
        if (!STANDALONE) {
            if (uninstallUnused) {
                uninstalled = clearTheme(context);
            }

        }
        boolean localeChanged = isLocaleChanged(context);
        HashSet<String> fileSet = new HashSet<String>();
        File lwts = new File(Environment.getExternalStorageDirectory(), "/LEWA/theme/lwt");
        File[] files = lwts.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".lwt");
            }
        });
        if (files != null) {
            for (File f : files) {
                if (f.exists()) {
                    String path = f.getAbsolutePath();
                    installExternalTheme(context, path, localeChanged);
                    fileSet.add(path);
                }
            }
        }
        File lockwallpapers = new File(Environment.getExternalStorageDirectory(), "/LEWA/theme/lockwallpaper");
        File[] lockwallpaperfiles = lockwallpapers.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jpg") || name.endsWith(".png");
            }
        });
        if (lockwallpaperfiles != null) {
            for (File f : lockwallpaperfiles) {
                if (f.exists()) {
                    String path = f.getAbsolutePath();
                    fileSet.add(path);
                }
            }
        }
        File wallpapers = new File(Environment.getExternalStorageDirectory(), "/LEWA/theme/deskwallpaper");
        File[] wallpaperfiles = wallpapers.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jpg") || name.endsWith(".png");
            }
        });
        if (wallpaperfiles != null) {
            for (File f : wallpaperfiles) {
                if (f.exists()) {
                    String path = f.getAbsolutePath();
                    fileSet.add(path);
                }
            }
        }
        Cursor c = null;
        try {
            ContentResolver cr = context.getContentResolver();
            PackageManager pm = context.getPackageManager();
            List<Long> dirties = new ArrayList<Long>();
            c = cr.query(ThemeColumns.CONTENT_PLURAL_URI,
                    new String[] { ThemeColumns._ID, ThemeColumns.DOWNLOAD_PATH, ThemeColumns.NAME,
                            ThemeColumns.THEME_PACKAGE, ThemeColumns.IS_IMAGE_FILE,
                            ThemeColumns.LOCK_WALLPAPER_URI, ThemeColumns.WALLPAPER_URI
                    }, ThemeColumns.DOWNLOAD_PATH + " IS NOT NULL AND " + ThemeColumns.IS_SYSTEM +
                    "=0", null, null);
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String path = c.getString(1);
                if (!fileSet.contains(path) && !path.startsWith(DATA_PATH)) {
                    pkgName = c.getString(3);
                    lockwallpaper = c.getString(5);
                    wallpaper = c.getString(6);
                    isImageFile = c.getInt(4);
                    themePkg = null;
                    if (isImageFile == 0) {
                        themePkg = c.getString(2) + ".lwt";
                    } else {
                        themePkg = c.getString(2);
                    }

                    dirties.add(id);
                    Log.d(TAG, "delete archive " + path);
                }
            }
            c.close();
            c = cr.query(ThemeColumns.CONTENT_PLURAL_URI, new String[]{ThemeColumns._ID, ThemeColumns.THEME_PACKAGE}, ThemeColumns.LIVE_WALLPAPER_URI + "=1 AND " + ThemeColumns.IS_SYSTEM + "=0", null, null);
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String pkg = c.getString(1);
                try {
                    pm.getPackageGids(pkg);
                } catch (NameNotFoundException e) {
                    dirties.add(id);
                    Log.d(TAG, "delete package " + pkg);
                }
            }
            if (dirties.size() > 0) {
                for (long id : dirties) {
                    cr.delete(ThemeColumns.CONTENT_PLURAL_URI, ThemeColumns._ID + "=" + id, null);
                    ThemeApplication.sThemeStatus.setDeleted(
                            pkgName, themePkg, isImageFile == 0 ? ThemeStatus.THEME_TYPE_PACKAGE : (lockwallpaper == null && wallpaper != null ? ThemeStatus.THEME_TYPE_WALLPAPER : ThemeStatus.THEME_TYPE_LOCK_WALLPAPER));
                }
            }
            if (STANDALONE) {
                updateDefaultTheme(context);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
        if (uninstalled || !fileHash || isExternalThemeChanged(context) && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            ImageLoader loader = ImageLoader.getInstance();
            loader.stop();
            loader.clearMemoryCache();
            loader.clearDiscCache();
            loader.resume();
            if (!STANDALONE) {
                if (localeChanged) {
                    for (PackageInfo info : android.app.LewaThemeHelper.getInstalledThemePackages(context)) {
                        if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            String path = info.applicationInfo.sourceDir;
                            if (!path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()))
                                installExternalTheme(context, info, path, localeChanged);
                        }
                    }
                }
            }
        }
        if (false)
            context.sendBroadcast(new Intent("android.intent.action.killProcess"));
    }

    public static boolean isExternalThemeChanged(Context context) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return false;
        final String hashName = "external_lwt_hash";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int hash = sp.getInt(hashName, -1);
        File lwts = new File(Environment.getExternalStorageDirectory(), "/LEWA/theme/lwt");
        int newHash = Arrays.hashCode(lwts.list());
        if (hash == newHash)
            return false;
        sp.edit().putInt(hashName, newHash).commit();
        return true;
    }

    public static boolean isLocaleChanged(Context context) {
        final String localeName = "external_lwt_local";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String locale = sp.getString(localeName, null);
        String newLocale = Locale.getDefault().toString();
        if (locale != null && locale.equals(newLocale))
            return false;
        sp.edit().putString(localeName, newLocale).commit();
        return true;
    }

    public static Uri installExternalTheme(Context context, String path, boolean updateLocale) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
        Uri result = null;
        if (pi == null) {
            try {
                result = NewMechanismHelp.installExtTheme(context, path, updateLocale);
            } catch (Exception e) {
                Log.e(Globals.TAG, e.toString());
            }
        } else {
            installExternalTheme(context, pi, path, updateLocale);
        }
        return result;
    }

    public static void installExternalTheme(Context context, PackageInfo pi, String path, boolean updateLocale) {
        ContentResolver ct = context.getContentResolver();
        if (pi == null) {
            return;
        }
        final boolean data = path.startsWith(DATA_PATH);
        ThemeInfo ti = Utils.parsePackage(path);
        if (pi != null && ti != null && !data) {
            Cursor c = null;
            try {
                String selection = ThemeColumns.THEME_PACKAGE + "='" + pi.packageName + "'";
                c = ct.query(ThemeColumns.CONTENT_PLURAL_URI, new String[]{ThemeColumns.DOWNLOAD_PATH, ThemeColumns.VERSION_CODE}, selection, null, null);
                if (c.moveToFirst()) {
                    String download = c.getString(0);
                    int versionCode = c.getInt(1);
                    if (pi.versionCode >= versionCode) {
                        if (updateLocale && pi.versionCode == versionCode) {
                            //partly update mode, only update theme name and author
                            ContentValues values = new ContentValues();
                            values.put(ThemeColumns.NAME, ti.name);
                            values.put(ThemeColumns.AUTHOR, ti.author);
                            ct.update(ThemeColumns.CONTENT_PLURAL_URI, values, selection, null);
                        } else if (!path.equals(download)) {
                            if (pi.versionCode >= versionCode) {
                                //Target file is newer so update to target info and delete the old one
                                ContentValues values = new ContentValues();
                                Utils.populateContentValues(context, values, pi, ti, path, false);
                                ct.update(ThemeColumns.CONTENT_PLURAL_URI, values, selection, null);
                                FileUtilities.deleteIfExists(download);
                            } else {
                                //Target file is old version so delete it
                                if (!download.startsWith(DATA_PATH)) {
                                    FileUtilities.deleteIfExists(path);
                                }
                            }
                        }
                    } else {
                        //Target file is old version so delete it
                        FileUtilities.deleteIfExists(path);
                    }
                } else {
                    //insert the new theme file
                    ContentValues values = new ContentValues();
                    Utils.populateContentValues(context, values, pi, ti, path, false);
                    ct.insert(ThemeColumns.CONTENT_PLURAL_URI, values);
                    ThemeApplication.sThemeStatus.setDownloaded(pi.packageName);
                }
                c.close();
            } catch (Exception e) {
                Log.e(TAG, "installExternalTheme: " + path + ", " + e.getMessage());
            } finally {
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            }
        } else if (ti == null && pi != null && !data) {
            Cursor c = null;
            String selection = ThemeColumns.THEME_PACKAGE + "='"
                    + pi.packageName + "'";
            c = ct.query(ThemeColumns.CONTENT_PLURAL_URI, new String[]{
                            ThemeColumns.NAME, ThemeColumns.THEME_PACKAGE}, selection,
                    null, null
            );
            if (c != null && c.getCount() > 0) {
                if (!c.isClosed()) {
                    c.close();
                }
            } else {
                boolean isLivewallpaper = Utils.isLiveWallpaperPackage(path);
                
                if (isLivewallpaper&&!isNullWallpaperInfo(context)) {
                    Intent i = new Intent(
                            context,
                            com.lewa.themechooser.receiver.ThemeInstallService.class);
                    i.putExtra("THEME_PACKAGE", path);
                    i.putExtra("isFromFileManager", false);
                    context.startService((i));
                }else{
                    String appliedLiveWallpaper = ThemeApplication.sThemeStatus.getAppliedPkgName(ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
                    String appliedWallpaper = ThemeApplication.sThemeStatus.getAppliedPkgName(ThemeStatus.THEME_TYPE_WALLPAPER);
                    if(appliedLiveWallpaper != null && appliedLiveWallpaper.contains(pi.packageName)
                            ||appliedWallpaper != null && appliedWallpaper.contains(pi.packageName)) {
                            ThemeApplication.sThemeStatus.setAppliedPkgName(null, ThemeStatus.THEME_TYPE_WALLPAPER);
                            ThemeApplication.sThemeStatus.setAppliedPkgName(null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
                            ThemeUtil.deleteFile(path);
                            context.getContentResolver().delete(ThemeColumns.CONTENT_PLURAL_URI, ThemeColumns.THEME_PACKAGE + "='" + pi.packageName + "'", null);
                            int start = path.lastIndexOf("/") + 1; 
                            int end = path.indexOf("_"); 
                            String themeName = path.substring(start, end) + ".lwt";
                            ThemeApplication.sThemeStatus.removeDownloaded(ThemeStatus.typedName(themeName, ThemeStatus.THEME_TYPE_LIVEWALLPAPER));
                            ThemeApplication.sThemeStatus.persistPreferences();
                    }
                }
            }
        }
    }
    public static boolean isNullWallpaperInfo(Context context){
        WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(context)
                .getWallpaperInfo();
        if (wallpaperInfo == null) 
           return true;
        return false;
    }
    
    public static void updateDefaultTheme(Context context) {
        ContentResolver ct = context.getContentResolver();
        Cursor c = null;
        try {
            final String pkg = ThemeManager.THEME_ELEMENTS_PACKAGE;
            String name = context.getString(R.string.theme_name_os6);
            c = ct.query(ThemeColumns.CONTENT_PLURAL_URI, new String[] { ThemeColumns._ID },
                    ThemeColumns.THEME_PACKAGE + "='" + pkg + "'", null, null);
            ContentValues values = new ContentValues();
            values.put(ThemeColumns.NAME, name);
            values.put(ThemeColumns.AUTHOR, context.getString(R.string.author));
            values.put(ThemeColumns.WALLPAPER_URI, "file:///system/media/wallpapers/1.jpg");
            values.put(ThemeColumns.LOCK_WALLPAPER_URI, "file:///system/media/theme/lockwallpaper");
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                c.close();
                ct.update(ThemeColumns.CONTENT_PLURAL_URI, values, ThemeColumns._ID + "=" + id,
                        null);
            } else {
                c.close();
                values.put(ThemeColumns.IS_APPLIED, 1);
                values.put(ThemeColumns.THEME_ID, "LewaDefaultTheme");
                values.put(ThemeColumns.THEME_PACKAGE, pkg);
                values.put(ThemeColumns.STYLE_NAME, "lewadefault");
                values.put(ThemeColumns.IS_DRM, 0);
                values.put(ThemeColumns.IS_SYSTEM, 1);
                values.put(ThemeColumns.HAS_HOST_DENSITY, 0);
                values.put(ThemeColumns.HAS_THEME_PACKAGE_SCOPE, 0);
                values.put(ThemeColumns.THUMBNAIL_URI, PackageResources.makeAssetPathUri(pkg, "preview/preview_launcher_0.png").toString());
                values.put(ThemeColumns.LOCKSCREEN_URI, "file:///" + ThemeManager.THEME_LOCKSCREEN_DEFAULT);
                values.put(ThemeColumns.ICONS_URI, "file:///system/media/theme/icons");
                values.put(ThemeColumns.WALLPAPER_URI, "file:///system/media/wallpapers/1.jpg");
                values.put(ThemeColumns.WALLPAPER_NAME, name);
                values.put(ThemeColumns.LOCK_WALLPAPER_URI, "file:///system/media/theme/lockwallpaper");
                //values.put(ThemeColumns.LOCK_WALLPAPER_URI, PackageResources.makeAssetPathUri(pkg, "wallpaper/wallpaper_lockscreen.jpg").toString());
                values.put(ThemeColumns.LOCK_WALLPAPER_NAME, name);
                File pkgFile = new File(context.getPackageResourcePath());
                if (pkgFile.exists()) {
                    values.put(ThemeColumns.SIZE, pkgFile.length());
                }
                values.put(ThemeColumns.DOWNLOAD_PATH, pkgFile.getAbsolutePath());
                values.put(ThemeColumns.VERSION_CODE, "1");
                values.put(ThemeColumns.VERSION_NAME, "1.2");
                values.put(ThemeColumns.LIVE_WALLPAPER_URI, (String) null);
                values.put(ThemeColumns.IS_IMAGE_FILE, 0);
                ct.insert(ThemeColumns.CONTENT_PLURAL_URI, values);

                // insert lockscreen2 style
                ct.insert(ThemeColumns.CONTENT_PLURAL_URI, insertLockScreen2(context));
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    private static ContentValues insertLockScreen2(Context context){
        ContentValues values = new ContentValues();
        String name = context.getString(R.string.theme_name_lockscreen2);
        values.put(ThemeColumns.NAME, name);
        values.put(ThemeColumns.AUTHOR, context.getString(R.string.author));
        values.put(ThemeColumns.IS_APPLIED, 0);
        values.put(ThemeColumns.THEME_ID, "LewaDefaultLockScreen");
        values.put(ThemeColumns.THEME_PACKAGE, ThemeManager.THEME_LOCKSCREEN2_PACKAGE);
        values.put(ThemeColumns.STYLE_NAME, name);
        values.put(ThemeColumns.IS_DRM, 0);
        values.put(ThemeColumns.IS_SYSTEM, 1);
        values.put(ThemeColumns.HAS_HOST_DENSITY, 0);
        values.put(ThemeColumns.HAS_THEME_PACKAGE_SCOPE, 0);
        //values.put(ThemeColumns.THUMBNAIL_URI, PackageResources.makeAssetPathUri(pkg, "preview/preview_launcher_0.jpg").toString());
        values.put(ThemeColumns.LOCKSCREEN_URI, PackageResources
                .makeAssetPathUri(ThemeManager.THEME_ELEMENTS_PACKAGE, ThemeManager.INTER_LOCKSCREEN2_PATH).toString());
        //values.put(ThemeColumns.ICONS_URI, "file:///system/media/theme/icons");
        //values.put(ThemeColumns.WALLPAPER_URI, PackageResources.makeAssetPathUri(pkg, "wallpaper/wallpaper.jpg").toString());
        values.put(ThemeColumns.WALLPAPER_NAME, name);
        //values.put(ThemeColumns.LOCK_WALLPAPER_URI, PackageResources.makeAssetPathUri(pkg, "wallpaper/wallpaper_lockscreen.jpg").toString());
        values.put(ThemeColumns.LOCK_WALLPAPER_NAME, name);
        File pkgFile = new File(context.getPackageResourcePath());
        values.put(ThemeColumns.DOWNLOAD_PATH, pkgFile.getAbsolutePath());
        values.put(ThemeColumns.VERSION_CODE, "0.1");
        values.put(ThemeColumns.VERSION_NAME, "0.1");
        values.put(ThemeColumns.LIVE_WALLPAPER_URI, (String) null);
        values.put(ThemeColumns.IS_IMAGE_FILE, 0);
        return values;
    }
    /**
     * Reboot device
     *
     * @param context
     * @return reboot success
     */
    public static boolean reboot(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (null != pm) {
            pm.reboot("change font");
            return true;
        }
        return false;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                            int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        //RC 48764 jianwu gao begin
        if (reqHeight == 800) {
            return inSampleSize;
        }
        //RC 48764 jianwu gao end

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static class UpdateThemeInfoThread extends Thread {
        private static UpdateThemeInfoThread sInstance;
        private Context mContext;
        private boolean mClear, mFileHash;
        private String mPackage;

        private UpdateThemeInfoThread() {
            throw new AssertionError();
        }

        private UpdateThemeInfoThread(Context context, String pkg, boolean clear, boolean fileHash) {
            mContext = context;
            mClear = clear;
            mFileHash = fileHash;
            mPackage = pkg;
        }

        public static void start(Context context, boolean clear) {
            start(context, null, clear, false);
        }

        public static void start(Context context, boolean clear, boolean fileHash) {
            start(context, null, clear, fileHash);
        }

        public static void start(Context context, String pkg, boolean clear) {
            start(context, pkg, clear, false);
        }

        public static void start(Context context, String pkg, boolean clear, boolean fileHash) {
            Log.d(TAG, "updateThemeInfo");
            if (sInstance != null && !Thread.interrupted())
                sInstance.interrupt();
            sInstance = new UpdateThemeInfoThread(context, pkg, clear, fileHash);
            sInstance.setPriority(Thread.MIN_PRIORITY);
            sInstance.start();
        }

        @Override
        public void run() {
            String pkg = mPackage;
            if (pkg == null) {
                updateThemeInfo(mContext, mClear, false, mFileHash);
            }
        }
    }

    private static class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        boolean finished;

        @Override
        public void packageDeleted(String name, int status) throws RemoteException {
            synchronized (this) {
                finished = true;
                notifyAll();
            }
        }
    }

    public static boolean isVersionChanged(){
        return false;
    }
}
