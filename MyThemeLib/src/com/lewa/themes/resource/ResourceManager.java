package com.lewa.themes.resource;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.MemoryFile;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.HashSet;

/**
 * Created by ivonhoe on 15-3-9.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class ResourceManager {

    private static final String LOG_TAG = "ResourceManager";

    private final ResourceLoader mResourceLoader;

    private final HashSet<String> mFailedBitmaps = new HashSet<String>();

    protected final LruCache<String, BitmapInfo> mBitmapsCache = new LruCache<String, BitmapInfo>(
            35 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, BitmapInfo value) {
            if (null == value.mBitmap) {
                return 0;
            }
            final int bitmapSize = (int) getBitmapSize(value.mBitmap);
            return bitmapSize;
        }
    };

    private int mExtraResourceDensity;

    private String mExtraResourceFolder;

    private int mExtraResourceScreenWidth;

    private int mResourceDensity;

    private int mTargetDensity;

    public ResourceManager(ResourceLoader resourceLoader) {
        mResourceLoader = resourceLoader;
    }

    public InputStream getInputStream(String path) {
        return mResourceLoader.getInputStream(path);
    }

    public InputStream getInputStream(String path, long[] size) {
        return mResourceLoader.getInputStream(path, size);
    }

    public String getPath() {
        return mResourceLoader.toString();
    }

    public long getBitmapSize(final Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    private BitmapInfo getBitmapInfo(String src) {
        if (TextUtils.isEmpty(src))
            return null;
        BitmapInfo info;
        synchronized (mBitmapsCache) {
            info = mBitmapsCache.get(src);
            if (info != null) {
                info.mLastVisitTime = System.currentTimeMillis();
                return info;
            }
        }
        if (mFailedBitmaps.contains(src))
            return null;

        Log.i(LOG_TAG, "load image " + src);
        boolean useDefaultResource = true;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = !src.endsWith(".9.png");
        opts.inTargetDensity = mTargetDensity;
        if (mExtraResourceScreenWidth != 0) {
            opts.inDensity = mExtraResourceDensity;
            info = mResourceLoader.getBitmapInfo(mExtraResourceFolder + "/" + src, opts);
            if (info != null)
                useDefaultResource = false;
        }

        if (info == null) {
            opts.inDensity = mResourceDensity;
            info = mResourceLoader.getBitmapInfo(src, opts);
        }
        if (info != null) {
            if (!useDefaultResource)
                Log.i(LOG_TAG, "load image from extra resource: " + mExtraResourceFolder);
            info.mBitmap.setDensity(mTargetDensity);
            info.mLastVisitTime = System.currentTimeMillis();
            synchronized (mBitmapsCache) {
                mBitmapsCache.put(src, info);
            }
            return info;
        }
        mFailedBitmaps.add(src);
        Log.e(LOG_TAG, "fail to load image: " + src);
        return info;
    }

    public void clear() {
        synchronized (mBitmapsCache) {
            mBitmapsCache.evictAll();
        }
    }

    public void clear(String param) {
        if (mBitmapsCache != null) {
            mBitmapsCache.remove(param);
        }
    }

    public void finish(boolean keepResource) {
        if (!keepResource) {
            clear();
        }
    }

    public Bitmap getBitmap(String src) {
        BitmapInfo info = getBitmapInfo(src);
        if (info != null) {
            return info.mBitmap;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public Drawable getDrawable(String src) {
        BitmapInfo info = getBitmapInfo(src);
        if (info != null && info.mBitmap != null) {
            Bitmap bm = info.mBitmap;
            if (bm.getNinePatchChunk() != null) {
                NinePatchDrawable ninePatchDrawable = new NinePatchDrawable(bm,
                        bm.getNinePatchChunk(), info.mPadding,
                        src);

                ninePatchDrawable.setTargetDensity(mTargetDensity);
                return ninePatchDrawable;
            }

            BitmapDrawable d = new BitmapDrawable(bm);
            d.setTargetDensity(mTargetDensity);
            return d;
        }
        return null;
    }

    public MemoryFile getFile(String src) {
        return mResourceLoader.getFile(src);
    }

    public Element getManifestRoot() {
        return mResourceLoader.getManifestRoot();
    }

    public Bitmap getMaskBufferBitmap(int width, int height, String key) {
        synchronized (mBitmapsCache) {
            BitmapInfo info = mBitmapsCache.get(key);
            Bitmap bm = null;
            if (info != null) {
                info.mLastVisitTime = System.currentTimeMillis();
                bm = info.mBitmap;
            }
            if (bm == null || bm.getHeight() < height || bm.getWidth() < width) {
                if (bm != null) {
                    width = Math.max(bm.getWidth(), width);
                    height = Math.max(bm.getHeight(), height);
                    bm.recycle();
                }
                bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bm.setDensity(mResourceDensity);
                mBitmapsCache.put(key, new BitmapInfo(bm, new Rect()));
            }
            return bm;
        }

    }

    public NinePatch getNinePatch(String src) {
        BitmapInfo info = getBitmapInfo(src);
        if (info != null) {
            return info.mNinePatch;
        }
        return null;
    }

    public void pause() {
    }

    public void resume() {
    }

    public void setExtraResourceDensity(int den) {
        mExtraResourceFolder = "den" + den;
        mExtraResourceDensity = translateDensity(den);
    }

    public void setExtraResourceScreenWidth(int sw) {
        mExtraResourceFolder = "sw" + sw;
        mExtraResourceScreenWidth = sw;
    }

    public void setResourceDensity(int density) {
        mResourceDensity = density;
    }

    public void setTargetDensity(int density) {
        mTargetDensity = density;
    }

    public static int translateDensity(int density) {
        return density;
    }

    public static class BitmapInfo {

        public long mLastVisitTime;
        public final Rect mPadding;
        public final NinePatch mNinePatch;
        public final Bitmap mBitmap;

        public BitmapInfo(Bitmap bm, Rect padding) {
            mBitmap = bm;
            mPadding = padding;
            mLastVisitTime = System.currentTimeMillis();
            mNinePatch = (bm != null && bm.getNinePatchChunk() != null) ?
                    new NinePatch(bm, bm.getNinePatchChunk(),
                            null) :
                    null;
        }
    }
}
