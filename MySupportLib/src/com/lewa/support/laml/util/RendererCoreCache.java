
package com.lewa.support.laml.util;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.*;
import java.util.Map.Entry;

import com.lewa.support.laml.RendererCore;
import com.lewa.support.laml.ResourceLoader;

public class RendererCoreCache implements com.lewa.support.laml.RendererCore.OnReleaseListener {
    private static final String LOG_TAG = "RendererCoreCache";

    public static final int TIME_MIN = 60000;

    public static final int TIME_HOUR = TIME_MIN * 60;

    public static final int TIME_DAY = TIME_HOUR * 24;

    private HashMap<Object, RendererCoreInfo> mCaches = new HashMap<Object, RendererCoreInfo>();

    private Handler mHandler;

    public RendererCoreCache() {
        mHandler = new Handler();
    }

    public RendererCoreCache(Handler h) {
        mHandler = h;
    }

    private synchronized void checkCache(Object key) {
        Log.d(LOG_TAG, "checkCache: " + key);
        RendererCoreInfo ri = mCaches.get(key);
        if (ri == null) {
            Log.d(LOG_TAG, "checkCache: the key does not exist, " + key);
        } else if (ri.accessTime != Long.MAX_VALUE) {
            long t = System.currentTimeMillis() - ri.accessTime;
            if (t >= ri.cacheTime) {
                mCaches.remove(key);
                Log.d(LOG_TAG, "checkCache removed: " + key);
            } else {
                if (t < 0) {
                    ri.accessTime = System.currentTimeMillis();
                    t = 0;
                }
                mHandler.postDelayed(ri.checkCache, ri.cacheTime - t);
                Log.d(LOG_TAG, "checkCache resheduled: " + key + " after " + (ri.cacheTime - t));
            }
        }
    }

    public synchronized void OnRendererCoreReleased(RendererCore rc) {
        Log.d(LOG_TAG, "OnRendererCoreReleased: " + rc);
        for (Entry<Object, RendererCoreInfo> en : mCaches.entrySet()) {
            if (en.getValue().r == rc) {
                release(en.getKey());
                return;
            }
        }
    }

    public synchronized void clear() {
        mCaches.clear();
    }

    public synchronized RendererCoreInfo get(Object key, Context context, long cacheTime,
            String path) {
        return get(key, context, cacheTime, path, new Handler());
    }

    public synchronized RendererCoreInfo get(Object key, Context context, long cacheTime,
            String path, Handler h) {
        return get(key, context, cacheTime, null, path, h);
    }

    public synchronized RendererCoreInfo get(Object key, Context context, long cacheTime,
            ResourceLoader loader, Handler h) {
        return get(key, context, cacheTime, loader, null, h);
    }

    public synchronized RendererCoreInfo get(Object key, Context context, long cacheTime,
            ResourceLoader loader) {
        return get(key, context, cacheTime, loader, null, new Handler());
    }

    public synchronized RendererCoreInfo get(Object key, long cacheTime) {
        RendererCoreInfo ri = mCaches.get(key);
        if (ri != null) {
            ri.accessTime = Long.MAX_VALUE;
            ri.cacheTime = cacheTime;
            mHandler.removeCallbacks(ri.checkCache);
        }
        return ri;
    }

    private synchronized RendererCoreInfo get(Object key, Context context, long cacheTime,
            ResourceLoader loader, String path, Handler h) {
        RendererCoreInfo ri = get(key, cacheTime);
        if (ri != null)
            return ri;
        RendererCore r = loader == null ? RendererCore.createFromZipFile(context, path, h)
                : RendererCore.create(context, loader, h);
        ri = new RendererCoreInfo(r);
        ri.accessTime = Long.MAX_VALUE;
        ri.cacheTime = cacheTime;
        if (r != null) {
            r.setOnReleaseListener(this);
            ri.checkCache = new CheckCacheRunnable(key);
        }
        mCaches.put(key, ri);
        return ri;
    }

    public synchronized void release(Object key) {
        Log.d(LOG_TAG, "release: " + key);
        RendererCoreInfo ri = mCaches.get(key);
        if (ri != null) {
            ri.accessTime = System.currentTimeMillis();
            if (ri.cacheTime == 0) {
                mCaches.remove(key);
                Log.d(LOG_TAG, "removed: " + key);
            } else {
                Log.d(LOG_TAG, "scheduled release: " + key + " after " + ri.cacheTime);
                mHandler.removeCallbacks(ri.checkCache);
                mHandler.postDelayed(ri.checkCache, ri.cacheTime);
            }
        }
    }

    private class CheckCacheRunnable implements Runnable {

        private Object mKey;

        public CheckCacheRunnable(Object key) {
            mKey = key;
        }

        public void run() {
            checkCache(mKey);
        }
    }

    public static class RendererCoreInfo {

        public long accessTime = Long.MAX_VALUE;

        public long cacheTime;

        public CheckCacheRunnable checkCache;

        public RendererCore r;

        public RendererCoreInfo(RendererCore rc) {
            r = rc;
        }
    }
}
