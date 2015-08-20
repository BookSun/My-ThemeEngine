
package com.lewa.support.laml.animation;

import java.util.Iterator;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

public class SizeAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Size";

    public static final String TAG_NAME = "SizeAnimation";

    private double mCurrentH;

    private double mCurrentW;

    private double mDelayH;

    private double mDelayW;

    private double mMaxH;

    private double mMaxW;

    public SizeAnimation(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        super(node, INNER_TAG_NAME, root);
        Utils.asserts(node.getNodeName().equalsIgnoreCase(TAG_NAME),
                "wrong tag name:" + node.getNodeName());
        Iterator<AnimationItem> i = mItems.iterator();
        while (i.hasNext()) {
            AnimationItem ai = i.next();
            if (ai.get(0) > mMaxW)
                mMaxW = ai.get(0);
            if (ai.get(1) > mMaxH)
                mMaxH = ai.get(1);
        }
        AnimationItem ai = getItem(0);
        mDelayW = ai.get(0);
        mDelayH = ai.get(1);
    }

    public final double getHeight() {
        return mCurrentH;
    }

    public final double getMaxHeight() {
        return mMaxH;
    }

    public final double getMaxWidth() {
        return mMaxW;
    }

    public final double getWidth() {
        return mCurrentW;
    }

    @Override
    protected AnimationItem onCreateItem() {
        return new AnimationItem(new String[] {
                "w", "h"
        }, mRoot);
    }

    @Override
    protected void onTick(AnimationItem item1, AnimationItem item2, float ratio) {
        double y1 = item1 != null ? item1.get(1) : 0;
        double x1 = item1 != null ? item1.get(0) : 0;
        mCurrentW = x1 + (item2.get(0) - x1) * ratio;
        mCurrentH = y1 + (item2.get(1) - y1) * ratio;
    }

    @Override
    public void reset(long time) {
        super.reset(time);
        mCurrentW = mDelayW;
        mCurrentH = mDelayH;
    }
}
