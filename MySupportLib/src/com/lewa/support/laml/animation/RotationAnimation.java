
package com.lewa.support.laml.animation;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

public class RotationAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Rotation";

    public static final String TAG_NAME = "RotationAnimation";

    private float mCurrentAngle;

    public RotationAnimation(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, INNER_TAG_NAME, root);
    }

    public final float getAngle() {
        return mCurrentAngle;
    }

    @Override
    protected AnimationItem onCreateItem() {
        return new AnimationItem(new String[] {
            "angle"
        }, mRoot);
    }

    @Override
    protected void onTick(AnimationItem item1, AnimationItem item2, float ratio) {
        if (item1 == null && item2 == null) {
            mCurrentAngle = 0.0F;
            return;
        }
        double a1 = item1 != null ? item1.get(0) : 0;
        mCurrentAngle = (float) (a1 + (item2.get(0) - a1) * ratio);
    }
}
