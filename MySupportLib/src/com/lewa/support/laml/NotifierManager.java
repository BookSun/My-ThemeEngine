
package com.lewa.support.laml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public final class NotifierManager {
    private static final boolean DBG = true;

    private static final String LOG_TAG = "NotifierManager";

    public static String TYPE_MOBILE_DATA = "MobileData";

    public static String TYPE_WIFI_STATE = "WifiState";

    private static NotifierManager sInstance;

    private Context mContext;

    private HashMap<String, BaseNotifier> mNotifiers = new HashMap<String, BaseNotifier>();

    private NotifierManager(Context c) {
        mContext = c;
    }

    private static BaseNotifier createNotifier(String t, Context c) {
        if (DBG)
            Log.i(LOG_TAG, "createNotifier:" + t);
        if (TYPE_MOBILE_DATA.equals(t))
            return new MobileDataNotifier(c);

        if (TYPE_WIFI_STATE.equals(t))
            return new MultiBroadcastNotifier(c, new String[] {
                    "android.net.wifi.WIFI_STATE_CHANGED", "android.net.wifi.SCAN_RESULTS",
                    "android.net.wifi.STATE_CHANGE"
            });
        return new BroadcastNotifier(c, t);
    }

    public synchronized static NotifierManager getInstance(Context c) {
        if (sInstance == null)
            sInstance = new NotifierManager(c);
        return sInstance;
    }

    public synchronized void acquireNotifier(String type, OnNotifyListener l) {
        if (DBG)
            Log.i(LOG_TAG, "acquireNotifier:" + type + "  " + l.toString());
        BaseNotifier notifier = mNotifiers.get(type);
        if (notifier == null) {
            notifier = createNotifier(type, mContext);
            if (notifier != null) {
                notifier.init();
                mNotifiers.put(type, notifier);
                notifier.addListener(l);
                notifier.addRef();
                notifier.addActiveRef();
            }
        }
    }

    public synchronized void pause(String t, OnNotifyListener l) {
        BaseNotifier notifier = mNotifiers.get(t);
        if (notifier != null) {
            notifier.removeListener(l);
            if (notifier.releaseActiveRef() == 0)
                notifier.pause();
        }
    }

    public synchronized void releaseNotifier(String type, OnNotifyListener l) {
        if (DBG)
            Log.i(LOG_TAG, "releaseNotifier:" + type + "  " + l.toString());
        BaseNotifier notifier = mNotifiers.get(type);
        if (notifier != null) {
            notifier.releaseActiveRef();
            notifier.removeListener(l);
            if (notifier.releaseRef() == 0) {
                notifier.finish();
                mNotifiers.remove(type);
            }
        }
    }

    public synchronized void resume(String type, OnNotifyListener l) {
        BaseNotifier notifier = mNotifiers.get(type);
        if (notifier != null) {
            notifier.addListener(l);
            if (notifier.addActiveRef() == 1)
                notifier.resume();
        }
    }

    public static abstract class BaseNotifier {
        private int mActiveReference;

        protected Context mContext;

        private ArrayList<OnNotifyListener> mListeners = new ArrayList<OnNotifyListener>();

        private int mReference;

        private boolean mRegistered;

        public BaseNotifier(Context c) {
            mContext = c;
        }

        public final int addActiveRef() {
            return ++mActiveReference;
        }

        public final int addRef() {
            return ++mReference;
        }

        public final void addListener(OnNotifyListener l) {
            synchronized (mListeners) {
                mListeners.add(l);
            }
        }

        public void finish() {
            unregister();
        }

        public void init() {
            register();
        }

        protected void onNotify(Context context, Intent intent, Object o) {
            synchronized (mListeners) {
                for (OnNotifyListener l : mListeners) {
                    l.onNotify(context, intent, o);
                }
            }
        }

        protected abstract void onRegister();

        protected abstract void onUnregister();

        public void pause() {
            unregister();
        }

        protected void register() {
            if (!mRegistered) {
                onRegister();
                mRegistered = true;
                if (DBG)
                    Log.i(LOG_TAG, "onRegister: " + toString());
            }
        }

        public final int releaseActiveRef() {
            if (mActiveReference > 0) {
                return --mActiveReference;
            }
            return 0;
        }

        public final int releaseRef() {
            if (mReference > 0) {
                return --mReference;
            }
            return 0;
        }

        public final void removeListener(OnNotifyListener l) {
            synchronized (mListeners) {
                mListeners.remove(l);
            }
        }

        public void resume() {
            register();
        }

        protected void unregister() {
            if (mRegistered) {
                try {
                    onUnregister();
                } catch (IllegalArgumentException e) {
                    Log.w(LOG_TAG, e.toString());
                }
                mRegistered = false;
                if (DBG)
                    Log.i("NotifierManager", "onUnregister: " + toString());
            }
        }
    }

    public static class BroadcastNotifier extends BaseNotifier {
        private String mAction;

        private IntentFilter mIntentFilter;

        private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (DBG)
                    Log.i(LOG_TAG, "onNotify: " + toString());
                onNotify(context, intent, null);
            }
        };

        public BroadcastNotifier(Context c) {
            super(c);
        }

        public BroadcastNotifier(Context c, String action) {
            super(c);
            mAction = action;
        }

        protected IntentFilter createIntentFilter() {
            String action = getIntentAction();
            if (action == null)
                return null;
            return new IntentFilter(action);
        }

        protected String getIntentAction() {
            return mAction;
        }

        protected void onRegister() {
            if (mIntentFilter == null)
                mIntentFilter = createIntentFilter();

            mContext.registerReceiver(mIntentReceiver, mIntentFilter);
        }

        protected void onUnregister() {
            mContext.unregisterReceiver(mIntentReceiver);
        }
    }

    public static class MobileDataNotifier extends BaseNotifier {
        private final ContentObserver mMobileDataEnableObserver = new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                if (DBG)
                    Log.i(LOG_TAG, "onNotify: " + toString());
                onNotify(null, null, Boolean.valueOf(selfChange));
            }

        };

        public MobileDataNotifier(Context c) {
            super(c);
        }

        protected void onRegister() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor("mobile_data"), false, mMobileDataEnableObserver);
        }

        protected void onUnregister() {
            mContext.getContentResolver().unregisterContentObserver(mMobileDataEnableObserver);
        }
    }

    public static class MultiBroadcastNotifier extends BroadcastNotifier {
        private String[] mIntents;

        public MultiBroadcastNotifier(Context c, String[] intents) {
            super(c);
            mIntents = intents;
        }

        protected IntentFilter createIntentFilter() {
            IntentFilter filter = new IntentFilter();
            for (String s : mIntents) {
                filter.addAction(s);
            }
            return filter;
        }
    }

    public static abstract interface OnNotifyListener {
        public abstract void onNotify(Context paramContext, Intent paramIntent, Object paramObject);
    }
}
