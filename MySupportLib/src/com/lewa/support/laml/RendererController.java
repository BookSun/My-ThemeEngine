
package com.lewa.support.laml;

import android.util.Log;
import android.view.MotionEvent;

import java.util.LinkedList;

import com.lewa.support.laml.FramerateTokenList.FramerateChangeListener;
import com.lewa.support.laml.FramerateTokenList.FramerateToken;

public class RendererController implements FramerateChangeListener {
    private static final String LOG_TAG = "RendererController";

    private static final int MAX_MSG_COUNT = 5;

    private float mCurFramerate;

    private int mFrameTime = Integer.MAX_VALUE;

    private FramerateTokenList mFramerateTokenList;

    private boolean mInited;

    private long mLastUpdateSystemTime;

    private Listener mListener;

    private Object mLock = new Object();

    private LinkedList<MotionEvent> mMsgQueue;

    private boolean mPaused;

    private boolean mPendingRender;

    protected RenderThread mRenderThread;

    protected boolean mGlobal;

    private boolean mSelfPaused;

    private boolean mShouldUpdate;

    private float mTouchX = -1;

    private float mTouchY = -1;

    public RendererController(Listener l) {
        setListener(l);
        mFramerateTokenList = new FramerateTokenList(this);
    }

    public FramerateToken createToken(String name) {
        return mFramerateTokenList.createToken(name);
    }

    public void doRender() {
        if (mListener != null) {
            mPendingRender = true;
            mListener.doRender();
        }
    }

    public void doneRender() {
        mPendingRender = false;
        if (mRenderThread != null)
            mRenderThread.signal();
    }

    public synchronized void finish() {
        if (!mInited)
            return;
        try {
            if (mListener != null) {
                mListener.finish();
                mInited = false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
    }

    public float getCurFramerate() {
        return mCurFramerate;
    }

    public int getFrameTime() {
        return mFrameTime;
    }

    public float getFramerate() {
        return mFramerateTokenList.getFramerate();
    }

    public long getLastUpdateTime() {
        return mLastUpdateSystemTime;
    }

    public synchronized MotionEvent getMessage() {
        return mMsgQueue == null ? null : mMsgQueue.poll();
    }

    public boolean hasInited() {
        return mInited;
    }

    public synchronized boolean hasMessage() {
        return mMsgQueue == null ? false : mMsgQueue.size() > 0;
    }

    public synchronized void init() {
        if (mInited)
            return;
        try {
            if (mListener != null) {
                mListener.init();
                mInited = true;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
    }

    public boolean isSelfPaused() {
        return mSelfPaused;
    }

    public void onFrameRateChage(float old, float cur) {
        if (mRenderThread != null && cur > 0)
            mRenderThread.signal();
    }

    public void onTouch(MotionEvent event) {
        if (mListener != null)
            mListener.onTouch(event);
    }

    public void pause() {
        if (!mInited)
            return;
        synchronized (mLock) {
            mPaused = true;
            if (!mSelfPaused && mListener != null)
                mListener.pause();
        }
        mPendingRender = false;
    }

    public boolean pendingRender() {
        return mPendingRender;
    }

    public synchronized void postMessage(MotionEvent e) {
        if (mMsgQueue == null)
            mMsgQueue = new LinkedList<MotionEvent>();

        if (e.getActionMasked() != MotionEvent.ACTION_MOVE || e.getX() != mTouchX
                || e.getY() != mTouchY) {
            mMsgQueue.add(e);
            mTouchX = e.getX();
            mTouchY = e.getY();
        }

        if (mMsgQueue.size() > MAX_MSG_COUNT)
            mMsgQueue.poll().recycle();

        if (mRenderThread != null)
            mRenderThread.signal();
    }

    public void requestUpdate() {
        mShouldUpdate = true;
        if (mRenderThread != null)
            mRenderThread.signal();
        if (mGlobal) {
            mSelfPaused = false;
            mRenderThread.setPaused(false);
        }
    }

    public void resume() {
        if (!mInited)
            return;
        synchronized (mLock) {
            mPaused = false;
            if (!mSelfPaused && mListener != null)
                mListener.resume();
        }
        mPendingRender = false;
    }

    public void selfPause() {
        if (!mInited || mSelfPaused)
            return;
        synchronized (mLock) {
            if (!mSelfPaused) {
                mSelfPaused = true;
                if (!mPaused && mListener != null) {
                    mListener.pause();
                }
            }
        }
        mPendingRender = false;
    }

    public void selfResume() {
        if (!mInited || !mSelfPaused)
            return;
        synchronized (mLock) {
            if (mSelfPaused) {
                mSelfPaused = false;
                if (!mPaused && mListener != null)
                    mListener.resume();
            }
        }
        if (mRenderThread != null) {
            mRenderThread.signal();
            mRenderThread.setPaused(false);
        }
        mPendingRender = false;
    }

    public void setCurFramerate(float f) {
        mCurFramerate = f;
    }

    public void setFrameTime(int frameTime) {
        mFrameTime = frameTime;
    }

    public void setLastUpdateTime(long t) {
        mLastUpdateSystemTime = t;
    }

    public void setListener(Listener l) {
        mListener = l;
    }

    public void setRenderThread(RenderThread renderThread) {
        mRenderThread = renderThread;
        mGlobal = RenderThread.isGlobal(renderThread);
    }

    public boolean shouldUpdate() {
        return mShouldUpdate;
    }

    public void tick(long currentTime) {
        mShouldUpdate = false;

        if (mListener != null)
            mListener.tick(currentTime);
    }

    public long updateFramerate(long time) {
        long nextUpdateInterval;
        if (mListener != null) {
            nextUpdateInterval = mListener.updateFramerate(time);
            if (mGlobal && nextUpdateInterval == Long.MAX_VALUE && (mShouldUpdate || !mInited)) {
                nextUpdateInterval = 30;
            }
        } else {
            nextUpdateInterval = Long.MAX_VALUE;
        }
        return nextUpdateInterval;
    }

    public static abstract interface IRenderable {
        public abstract void doRender();
    }

    public static abstract interface Listener extends IRenderable {
        public abstract void finish();

        public abstract void init();

        public abstract void onTouch(MotionEvent event);

        public abstract void pause();

        public abstract void resume();

        public abstract void tick(long time);

        public abstract long updateFramerate(long time);
    }
}
