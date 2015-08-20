
package com.lewa.support.laml;

import java.util.ArrayList;
import java.util.Set;

import android.util.Log;

public class LifecycleResourceManager extends ResourceManager {
    private static final String LOG_TAG = "LifecycleResourceManager";

    public static final int TIME_HOUR = 60 * 60 * 1000;

    public static final int TIME_DAY = 24 * TIME_HOUR;

    private static long mLastCheckCacheTime;

    private long mCheckTime;

    private long mInactiveTime;

    public LifecycleResourceManager(ResourceLoader resourceLoader, long inactiveTime, long checkTime) {
        super(resourceLoader);
        mInactiveTime = inactiveTime;
        mCheckTime = checkTime;
    }

    public void checkCache() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mLastCheckCacheTime < mCheckTime)
            return;

        Log.d(LOG_TAG, "beging check cache... ");
        synchronized (mBitmapsCache){
            final ArrayList<String> mToBeRemoved = new ArrayList<String>();
            final Set<String> strings = mBitmapsCache.snapshot().keySet() ;
            for (String key : strings) {
                BitmapInfo bi = mBitmapsCache.get(key);
                if (currentTimeMillis - bi.mLastVisitTime > mInactiveTime)
                    mToBeRemoved.add(key);

            }
            for(String s : mToBeRemoved){
                Log.d(LOG_TAG, "remove cache: " + s);
                mBitmapsCache.remove(s);
            }
        }
        mLastCheckCacheTime = currentTimeMillis;
    }

    public void finish(boolean keepResource) {
        if (keepResource)
            checkCache();

        super.finish(keepResource);
    }

    public void pause() {
        checkCache();
    }
}
