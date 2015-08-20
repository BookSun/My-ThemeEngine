
package com.lewa.support.laml.animation;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

public class SourcesAnimation extends PositionAnimation {
    public static class Source extends AnimationItem {

        public static final String TAG_NAME = "Source";

        public String mSrc;

        @Override
        public AnimationItem load(Element node) throws ScreenElementLoadException {
            mSrc = node.getAttribute("src");
            return super.load(node);
        }

        public Source(String attrs[], ScreenElementRoot root) {
            super(attrs, root);
        }
    }

    public static final String TAG_NAME = "SourcesAnimation";

    private String mCurrentBitmap;

    public SourcesAnimation(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        super(node, Source.TAG_NAME, root);
    }

    public final String getSrc() {
        return mCurrentBitmap;
    }

    @Override
    protected AnimationItem onCreateItem() {
        return new Source(new String[] {
                "x", "y"
        }, mRoot);
    }

    @Override
    protected void onTick(AnimationItem item1, AnimationItem item2, float ratio) {
        if (item2 == null) {
            mCurrentX = 0;
            mCurrentY = 0;
        } else {
            mCurrentX = item2.get(0);
            mCurrentY = item2.get(1);
            mCurrentBitmap = ((Source) item2).mSrc;
        }
    }
}
