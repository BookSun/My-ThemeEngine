package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

import android.graphics.Canvas;
import android.util.Log;

public class MirrorScreenElement extends AnimatedScreenElement {

    private boolean             mMirrorTranslation;
    private ScreenElement       mTarget;
    private String              mTargetName;

    public static final String  TAG_NAME = "Mirror";
    private static final String LOG_TAG  = "MirrorScreenElement";

    public MirrorScreenElement(Element node, ScreenElementRoot root) throws ScreenElementLoadException{
        super(node, root);
        mTargetName = node.getAttribute("target");
        mMirrorTranslation = Boolean.parseBoolean(node.getAttribute("mirrorTranslation"));
    }

    public void doRender(Canvas canvas) {
        if (mTarget == null) {
            return;
        }
        if (mMirrorTranslation && mTarget instanceof AnimatedScreenElement) {
            ((AnimatedScreenElement)mTarget).doRenderWithTranslation(canvas);
        }
        mTarget.doRender(canvas);
    }

    public void init() {
        super.init();
        mTarget = mRoot.findElement(mTargetName);
        if (mTarget == null) {
            Log.e(LOG_TAG, "the target does not exist: " + mTargetName);
        }
    }
}
