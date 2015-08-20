
package com.lewa.support.laml.animation;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

public class PositionAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Position";

    public static final String TAG_NAME = "PositionAnimation";

    protected double mCurrentX;

    protected double mCurrentY;

    protected double mDelayX;

    protected double mDelayY;

    public PositionAnimation(Element node, String tagName, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, tagName, root);
        AnimationItem ai = getItem(0);
        mDelayX = ai.get(0);
        mDelayY = ai.get(1);
    }

    public PositionAnimation(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, INNER_TAG_NAME, root);
        Utils.asserts(node.getNodeName().equalsIgnoreCase(TAG_NAME),
                "wrong tag name:" + node.getNodeName());
    }

    public final double getX() {
        return mCurrentX;
    }

    public final double getY() {
        return mCurrentY;
    }

    @Override
    protected AnimationItem onCreateItem() {
        return new AnimationItem(new String[] {
                "x", "y"
        }, mRoot);
    }

    @Override
    protected void onTick(AnimationItem item1, AnimationItem item2, float ratio) {
        if (item1 == null && item2 == null)
            return;
        double x1;
        double y1;
        if (item1 != null) {
            x1 = item1.get(0);
            y1 = item1.get(1);
        } else {
            x1 = 0;
            y1 = 0;
        }
        mCurrentX = x1 + (item2.get(0) - x1) * ratio;
        mCurrentY = y1 + (item2.get(1) - y1) * ratio;
    }

    @Override
    public void reset(long time) {
        super.reset(time);
        mCurrentX = mDelayX;
        mCurrentY = mDelayY;
    }
}
