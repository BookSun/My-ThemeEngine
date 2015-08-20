
package com.lewa.support.laml.animation;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

public class VariableAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "AniFrame";

    public static final String TAG_NAME = "VariableAnimation";

    private double mCurrentValue;

    private double mDelayValue;

    public VariableAnimation(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, INNER_TAG_NAME, root);
        mDelayValue = getItem(0).get(0);
    }

    public final double getValue() {
        return mCurrentValue;
    }

    @Override
    protected AnimationItem onCreateItem() {
        return new AnimationItem(new String[] {
            "value"
        }, mRoot);
    }

    @Override
    protected void onTick(AnimationItem item1, AnimationItem item2, float ratio) {
        if (item1 == null && item2 == null)
            return;
        double a1 = item1 == null ? 0 : item1.get(0);
        mCurrentValue = a1 + (item2.get(0) - a1) * ratio;
    }

    @Override
    public void reset(long time) {
        super.reset(time);
        mCurrentValue = mDelayValue;
    }
}
