package com.lewa.support.laml.tween;

import com.lewa.support.laml.tween.easing.IEasing;

import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * Animation.java:
 * 
 * @author yljiang@lewatek.com 2014-8-12
 */
public final class Animation {

    private float             mBegin;
    private float             mEnd;
    private float             mChange;
    private long              mDuration;
    private boolean           mInAnimation;
    private boolean           mIsEnd;

    private long              mSatartTime;

    private IEasing           mEasing;
    private Interpolator      mInterpolator;

    private AnimationListener mListener;

    public Animation(float begin, float end, long duration){
        mBegin = begin;
        mEnd = end;
        mChange = mEnd - mBegin;
        mDuration = duration;
        mInterpolator = new LinearInterpolator();
    }

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    public void setIEasing(IEasing ease) {
        mEasing = ease;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public void setAnimatorListener(AnimationListener listener) {
        mListener = listener;
    }

    public boolean inAnimation() {
        return mInAnimation;
    }

    public void start() {
        mSatartTime = SystemClock.elapsedRealtime();
        mInAnimation = true;
        mIsEnd = false;
    }

    public void end() {
        doTick(mSatartTime + mDuration);
    }

    public float doTick(long currentTime) {
        if (currentTime >= mSatartTime + mDuration) {
            if (!mInAnimation && !mIsEnd && mListener != null) {
                mIsEnd = true;
                mListener.onStateChange(AnimationListener.END);
            }
            mInAnimation = false;
            return mEnd;
        }
        long time = currentTime - mSatartTime;
        if (mEasing != null) {
            return (float)mEasing.tick(time, mBegin, mChange, mDuration);
        } else {
            return mBegin + mChange * mInterpolator.getInterpolation((float)time / mDuration);
        }
    }

    public static interface AnimationListener {

        public static final int END = -1;

        public void onStateChange(int state);

    }
}
