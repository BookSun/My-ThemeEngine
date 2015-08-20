
package com.lewa.support.laml;

import android.util.Log;
import java.util.ArrayList;

public class FramerateTokenList {
    private static final String LOG_TAG = "FramerateTokenList";

    private float mCurFramerate;

    private FramerateChangeListener mFramerateChangeListener;

    private ArrayList<FramerateToken> mList;

    public FramerateTokenList() {
        mList = new ArrayList<FramerateToken>();
    }

    public FramerateTokenList(FramerateChangeListener l) {
        mList = new ArrayList<FramerateToken>();
        mFramerateChangeListener = l;
    }

    private void onChange() {
        float r = 0;
        synchronized (mList) {
            for (FramerateToken t : mList) {
                if (t.mFramerate > r)
                    r = t.mFramerate;
            }
            mCurFramerate = r;
        }
    }

    public FramerateToken createToken(String name) {
        Log.d(LOG_TAG, "createToken: " + name);
        FramerateToken token = new FramerateToken(name);

        synchronized (mList) {
            mList.add(token);
            return token;
        }
    }

    public float getFramerate() {
        return mCurFramerate;
    }

    public static abstract interface FramerateChangeListener {
        public abstract void onFrameRateChage(float old, float cur);
    }

    public class FramerateToken {
        public float mFramerate;

        public String mName;

        public FramerateToken(String name) {
            mName = name;
        }

        public float getFramerate() {
            return mFramerate;
        }

        public void requestFramerate(float f) {
            if (mFramerate != f) {
                Log.d(LOG_TAG, "requestFramerate: " + f + " by:" + mName);
                if (mFramerateChangeListener != null)
                    mFramerateChangeListener.onFrameRateChage(mFramerate, f);

                mFramerate = f;
                onChange();
            }
        }
    }
}
