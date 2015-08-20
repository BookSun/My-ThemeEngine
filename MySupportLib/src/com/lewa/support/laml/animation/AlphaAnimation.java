
package com.lewa.support.laml.animation;

import android.text.TextUtils;
import android.util.Log;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

public class AlphaAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Alpha";

    public static final String TAG_NAME = "AlphaAnimation";

    public static final String LOG_TAG = TAG_NAME;

    private int mCurrentAlpha;

    private int mDelayValue;

    public AlphaAnimation(Element node, String tagName, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, tagName, root);
        mCurrentAlpha = 255;
        String delayValue = node.getAttribute("delayValue");
        if (!TextUtils.isEmpty(delayValue)) {
            try {
                mDelayValue = Integer.parseInt(delayValue);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, e.toString());
            }
        } else {
            AnimationItem ai = getItem(0);
            mDelayValue = (int) ai.get(0);
        }
    }

    public AlphaAnimation(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        this(node, INNER_TAG_NAME, root);
        Utils.asserts(node.getNodeName().equalsIgnoreCase(TAG_NAME),
                "wrong tag name:" + node.getNodeName());
    }

    public final int getAlpha() {
        return mCurrentAlpha;
    }

    @Override
    protected AnimationItem onCreateItem() {
        return new AnimationItem(new String[] {
            "a"
        }, mRoot);
    }

    @Override
    protected void onTick(AnimationItem item1, AnimationItem item2, float ratio) {
        if (item1 == null && item2 == null)
            return;
        double a1 = item1 == null ? 255 : item1.get(0);
        mCurrentAlpha = (int) Math.round(a1 + (item2.get(0) - a1) * ratio);
    }

    @Override
    public void reset(long time) {
        super.reset(time);
        mCurrentAlpha = mDelayValue;
    }
}
