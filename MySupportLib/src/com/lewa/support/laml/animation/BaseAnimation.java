
package com.lewa.support.laml.animation;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import com.lewa.support.laml.*;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class BaseAnimation {

    private static final long INFINITE_TIME = 1000000000000L;

    private static final String LOG_TAG = "BaseAnimation";

    private long mDelay;

    protected ArrayList<AnimationItem> mItems = new ArrayList<AnimationItem>();

    private boolean mLastFrame;

    private long mRealTimeRange;

    protected ScreenElementRoot mRoot;

    private long mStartTime;

    private long mTimeRange;

    public BaseAnimation(Element node, String tag, ScreenElementRoot root)
            throws ScreenElementLoadException {
        mRoot = root;
        load(node, tag);
    }

    private void load(Element node, String tag) throws ScreenElementLoadException {
        mItems.clear();
        String strDelay = node.getAttribute("delay");
        NodeList nodeList;
        if (!TextUtils.isEmpty(strDelay))
            try {
                mDelay = Long.parseLong(strDelay);
            } catch (NumberFormatException e) {
                Log.w(LOG_TAG, "invalid delay attribute");
            }
        nodeList = node.getElementsByTagName(tag);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element ele = (Element) nodeList.item(i);
            mItems.add(onCreateItem().load(ele));
        }
        Utils.asserts(mItems.size() > 0, "BaseAnimation: empty items");
        mTimeRange = mItems.get(-1 + mItems.size()).mTime;
        if (mItems.size() > 1)
            mRealTimeRange = mItems.get(-2 + mItems.size()).mTime;
    }

    protected AnimationItem getItem(int index) {
        if (index >= 0 && index < mItems.size())
            return mItems.get(index);
        else
            return null;
    }

    public void init() {
    }

    protected abstract AnimationItem onCreateItem();

    protected abstract void onTick(AnimationItem pre, AnimationItem cur, float f);

    public void reset(long time) {
        mStartTime = time;
        mLastFrame = false;
    }

    public final void tick(long currentTime) {
        long elapsedTime = currentTime - mStartTime;
        if (elapsedTime < 0)
            elapsedTime = 0;
        if (elapsedTime < mDelay) {
            onTick(null, null, 0);
        } else {
            long l = elapsedTime - mDelay;
            if (mTimeRange < INFINITE_TIME || l <= mRealTimeRange || !mLastFrame) {
                long time = l % mTimeRange;
                AnimationItem pos1 = null;
                for (int i = 0, N = mItems.size(); i < N; i++) {
                    AnimationItem pos = mItems.get(i);
                    if (time <= pos.mTime) {
                        long base;
                        long range;
                        if (i == 0) {
                            range = pos.mTime;
                            base = 0;
                        } else {
                            pos1 = mItems.get(i - 1);
                            range = pos.mTime - pos1.mTime;
                            base = pos1.mTime;
                        }
                        mLastFrame = i == N - 1;
                        onTick(pos1, pos, range == 0 ? 1 : ((float) (time - base) / (float)range));
                        break;
                    }
                }
            }
        }
    }

    public static class AnimationItem {

        private String mAttrs[];

        public Expression mExps[];

        private ScreenElementRoot mRoot;

        public long mTime;

        public double get(int i) {
            if (i >= 0 && i < mExps.length && mExps != null) {
                if (mExps[i] != null)
                    return mExps[i].evaluate(mRoot.getContext().mVariables);
            } else {
                Log.e(LOG_TAG, "fail to get number in AnimationItem:" + i);
            }
            return 0;
        }

        public AnimationItem load(Element node) throws ScreenElementLoadException {
            try {
                mTime = Long.parseLong(node.getAttribute("time"));
            } catch (NumberFormatException numberformatexception) {
                Log.e(LOG_TAG, "fail to get time attribute");
                throw new ScreenElementLoadException("fail to get time attribute");
            }
            if (mAttrs != null) {
                mExps = new Expression[mAttrs.length];
                for (int i = 0; i < mAttrs.length; i++) {
                    mExps[i] = Expression.build(node.getAttribute(mAttrs[i]));

                }

            }
            return this;
        }

        public AnimationItem(String attrs[], ScreenElementRoot root) {
            mAttrs = attrs;
            mRoot = root;
        }
    }
}
