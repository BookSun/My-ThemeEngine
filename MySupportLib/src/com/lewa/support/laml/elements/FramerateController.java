
package com.lewa.support.laml.elements;

import android.graphics.Canvas;

import java.util.ArrayList;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class FramerateController extends ScreenElement {
    public static final String INNER_TAG = "ControlPoint";

    public static final String TAG_NAME = "FramerateController";

    private ArrayList<ControlPoint> mControlPoints = new ArrayList<ControlPoint>();

    private float mCurFramerate;

    private long mLastUpdateTime;

    private Object mLock = new Object();

    private boolean mLoop;

    private long mNextUpdateInterval;

    private long mStartTime;

    private boolean mStopped;

    private long mTimeRange;

    public FramerateController(Element ele, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(ele, root);
        mRoot.addFramerateController(this);
        mLoop = Boolean.parseBoolean(ele.getAttribute("loop"));
        NodeList nodeList = ele.getElementsByTagName(INNER_TAG);

        for (int i = 0, N = nodeList.getLength(); i < N; i++) {
            Element e = (Element) nodeList.item(i);
            mControlPoints.add(new ControlPoint(e));
        }
        mTimeRange = mControlPoints.get(mControlPoints.size() - 1).mTime;
    }

    public void doRender(Canvas c) {
    }

    protected void onVisibilityChange(boolean visible) {
        super.onVisibilityChange(visible);
        if (visible) {
            requestFramerate(mCurFramerate);
        } else {
            mCurFramerate = getFramerate();
            requestFramerate(0);
        }
    }

    public void reset(long time) {
        synchronized (mLock) {
            mStartTime = time;
            mStopped = false;
            mLastUpdateTime = 0;
            mNextUpdateInterval = 0;
            requestUpdate();
        }
    }

    public long updateFramerate(long currentTime) {
        updateVisibility();
        if (isVisible()) {
            synchronized (mLock) {
                if (!mStopped) {
                    long elapsedTime = currentTime
                            - (mLastUpdateTime > 0 ? mLastUpdateTime : mStartTime);
                    if (elapsedTime >= 0 && elapsedTime < mNextUpdateInterval) {
                        mLastUpdateTime = currentTime;
                        return mNextUpdateInterval -= elapsedTime;
                    } else if (elapsedTime < 0) {
                        elapsedTime = 0;
                    }
                    long time = mLoop ? elapsedTime % mTimeRange : elapsedTime;
                    int size = mControlPoints.size() - 1;
                    long nextUpdateTime = 0;
                    for (int i = size; i >= 0; i--) {
                        ControlPoint cp = mControlPoints.get(i);
                        if (time >= cp.mTime) {
                            requestFramerate(cp.mFramerate);

                            if (!mLoop && i == size)
                                mStopped = true;

                            mLastUpdateTime = currentTime;
                            return mNextUpdateInterval = mStopped ? Long.MAX_VALUE : nextUpdateTime
                                    - time;
                        }
                        nextUpdateTime = cp.mTime;
                    }
                }
            }
        }
        return Long.MAX_VALUE;
    }

    public static class ControlPoint {
        public int mFramerate;

        public long mTime;

        public ControlPoint(Element node) throws ScreenElementLoadException {
            mTime = Utils.getAttrAsLongThrows(node, "time");
            mFramerate = Utils.getAttrAsInt(node, "frameRate", -1);
        }
    }
}
