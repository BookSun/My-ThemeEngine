package com.lewa.support.util;

import android.view.animation.Interpolator;
/**
 * 
 * DampingInterpolator.java:
 * @author yljiang@lewatek.com 2014-8-4
 */
public class DampingInterpolator implements Interpolator {

    private final double mAtanValue;
    private final float  mFactor;

    public DampingInterpolator(float factor){
        mFactor = factor;
        mAtanValue = Math.atan(mFactor);
    }

    public float getInterpolation(float input) {
        return (float)(Math.atan(input * mFactor) / mAtanValue);
    }
}
