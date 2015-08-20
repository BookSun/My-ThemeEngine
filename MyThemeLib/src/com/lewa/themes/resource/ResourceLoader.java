package com.lewa.themes.resource;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.MemoryFile;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import com.lewa.themes.Utils;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.zip.ZipFile;

/**
 * Created by ivonhoe on 15-3-5.
 */
public abstract class ResourceLoader {

    private static final String IMAGES_FOLDER_NAME = "images";

    private static final String TAG = "ResourceLoader";

    protected static final Resources sResources = Resources.getSystem();

    protected static final int sDendityDpi = sResources.getDisplayMetrics().densityDpi;

    protected static final String MANIFEST_FILE_NAME = "manifest.xml";

    protected String mLanguageCountrySuffix;

    protected String mLanguageSuffix;

    protected String mManifestName = MANIFEST_FILE_NAME;

    protected static Context mContext;

    protected String mAvailablePath;

    /*
    * 根据不同路径的优先级获取资源
    * */
    public abstract String getAvailablePath();

    public abstract InputStream getInputStream(String path, long[] size);

    public abstract boolean resourceExists(String path);

    public abstract void clearCache();

    public String getAvailablePath(String... paths) {
        for (String p : paths) {
            if (new File(p).exists()) {
                return p;
            }
        }
        return null;
    }

    public ZipFile getZipFile(String... paths) {
        try {
            return new ZipFile(getAvailablePath(paths));
        } catch (Exception e) {
            return null;
        }
    }

    protected final InputStream getInputStream(String path) {
        return getInputStream(path, null);
    }

/*    public static Drawable getDrawable(String path) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);
            int width;
            int height;
            switch (sDendityDpi) {
                case DisplayMetrics.DENSITY_LOW:
                    width = 240;
                    height = 320;
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                    width = 320;
                    height = 480;
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                    width = 540;
                    height = 960;
                    break;
                case 480*//* DisplayMetrics.DENSITY_XXHIGH *//*:
                case DisplayMetrics.DENSITY_XHIGH:
                default:
                    width = 720;
                    height = 1280;
                    break;
            }
            opts.inSampleSize = Utils.computeSampleSize(opts, width, height);
            opts.inJustDecodeBounds = false;
            Bitmap bmp = BitmapFactory.decodeFile(path, opts);
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "get wallpaper error", e);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "get wallpaper error", e);
        }
        return null;
    }*/

    private String getPathForLanguage(String src, String folder) {
        if (!TextUtils.isEmpty(mLanguageCountrySuffix)) {
            String path = folder + "_" + mLanguageCountrySuffix + "/" + src;
            if (resourceExists(path))
                return path;
        }
        if (!TextUtils.isEmpty(mLanguageSuffix)) {
            String path = folder + "_" + mLanguageSuffix + "/" + src;
            if (resourceExists(path))
                return path;
        }
        if (!TextUtils.isEmpty(folder)) {
            String path = folder + "/" + src;
            if (resourceExists(path))
                return path;
        }
        if (!resourceExists(src))
            return null;
        return src;
    }

    public ResourceManager.BitmapInfo getBitmapInfo(String src, BitmapFactory.Options opts) {
        String path = getPathForLanguage(src, IMAGES_FOLDER_NAME);
        InputStream is = null;
        try {
            is = getInputStream(path);
            Rect padding = new Rect();
            Bitmap bm = BitmapFactory.decodeStream(is, padding, opts);
            if (bm == null)
                return null;
            return new ResourceManager.BitmapInfo(bm, padding);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, e.toString(), e);
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    public MemoryFile getFile(String src) {
        final int COUNT = 4096;
        InputStream is = null;
        try {
            long[] length = new long[1];
            is = getInputStream(src, length);
            if (is == null)
                return null;
            MemoryFile mf = new MemoryFile(null, (int) length[0]);
            OutputStream os = mf.getOutputStream();
            int read;
            byte[] buffer = new byte[COUNT];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            os.flush();
            os.close();
            return mf;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, e.toString(), e);
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    public Element getManifestRoot() {
        String manifestName = null;
        if (!TextUtils.isEmpty(mLanguageCountrySuffix)) {
            manifestName = Utils.addFileNameSuffix(mManifestName, mLanguageCountrySuffix);
            if (!resourceExists(manifestName))
                manifestName = null;
        }
        if (manifestName == null && !TextUtils.isEmpty(mLanguageSuffix)) {
            manifestName = Utils.addFileNameSuffix(mManifestName, mLanguageSuffix);
            if (!resourceExists(manifestName))
                manifestName = null;
        }

        if (manifestName == null)
            manifestName = mManifestName;

        InputStream is = null;
        try {
            is = getInputStream(manifestName);
            if (is != null)
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is)
                        .getDocumentElement();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } catch (OutOfMemoryError e) {
            Log.e(TAG, e.toString());
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    protected InputStream getWallpaperStream(String path) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int[] size = getDisplaySize();
        options.inSampleSize = Utils.calculateInSampleSize(options, size[0], size[1]);
        if (options.inSampleSize <= 1) {
            return new FileInputStream(path);
        } else {
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            baos.close();
            return is;
        }
    }

    protected int[] getDisplaySize() {
        int[] size = new int[2];
        switch (sDendityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                size[0] = 240;
                size[1] = 320;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                size[0] = 320;
                size[1] = 480;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                size[0] = 540;
                size[1] = 960;
                break;
            case 480 /* DisplayMetrics.DENSITY_XXHIGH */:
            case DisplayMetrics.DENSITY_XHIGH:
            default:
                size[0] = 720;
                size[1] = 1280;
                break;
        }
        return size;
    }

    protected String getExternalAvailablePath(String name) {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + name;
    }
}
