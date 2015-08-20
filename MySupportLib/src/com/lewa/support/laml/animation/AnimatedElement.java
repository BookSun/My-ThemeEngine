
package com.lewa.support.laml.animation;

import java.util.ArrayList;

import com.lewa.support.laml.ScreenContext;
import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.elements.ElementGroup;
import com.lewa.support.laml.util.TextFormatter;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.text.TextUtils;
import android.util.Log;

public class AnimatedElement {

    private static final boolean DEBUG = false;

    private static final String LOG_TAG = "AnimatedElement";

    private boolean mAlignAbsolute;

    private Expression mAlphaExpression;

    private AlphaAnimation mAlphas;

    private ArrayList<BaseAnimation> mAnimations = new ArrayList<BaseAnimation>();

    protected Expression mBaseXExpression;

    protected Expression mBaseYExpression;

    protected Expression mCenterXExpression;

    protected Expression mCenterYExpression;

    protected Expression mHeightExpression;

    private PositionAnimation mPositions;

    private ScreenElementRoot mRoot;

    protected Expression mRotationExpression;

    private RotationAnimation mRotations;

    private SizeAnimation mSizes;

    private SourcesAnimation mSources;

    private TextFormatter mSrcFormatter;

    protected Expression mSrcIdExpression;

    protected Expression mWidthExpression;

    public AnimatedElement(Element node, ScreenElementRoot root) {
        mRoot = root;
        try {
            load(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Expression createExp(Element node, String name, String compatibleName) {
        Expression exp = Expression.build(node.getAttribute(name));
        if (exp == null && !TextUtils.isEmpty(compatibleName))
            exp = Expression.build(node.getAttribute(compatibleName));
        return exp;
    }

    private ScreenContext getContext() {
        return mRoot.getContext();
    }

    private void loadAlphaAnimations(Element node) throws ScreenElementLoadException {
        Element ele = Utils.getChild(node, AlphaAnimation.TAG_NAME);
        if (ele != null) {
            mAlphas = new AlphaAnimation(ele, mRoot);
            mAnimations.add(mAlphas);
        }
    }

    private void loadPositionAnimations(Element node) throws ScreenElementLoadException {
        Element ele = Utils.getChild(node, PositionAnimation.TAG_NAME);
        if (ele != null) {
            mPositions = new PositionAnimation(ele, mRoot);
            mAnimations.add(mPositions);
        }
    }

    private void loadRotationAnimations(Element node) throws ScreenElementLoadException {
        Element ele = Utils.getChild(node, RotationAnimation.TAG_NAME);
        if (ele != null) {
            mRotations = new RotationAnimation(ele, mRoot);
            mAnimations.add(mRotations);
        }
    }

    private void loadSizeAnimations(Element node) throws ScreenElementLoadException {
        Element ele = Utils.getChild(node, SizeAnimation.TAG_NAME);
        if (ele != null) {
            mSizes = new SizeAnimation(ele, mRoot);
            mAnimations.add(mSizes);
        }
    }

    private void loadSourceAnimations(Element node) throws ScreenElementLoadException {
        Element ele = Utils.getChild(node, SourcesAnimation.TAG_NAME);
        if (ele != null) {
            mSources = new SourcesAnimation(ele, mRoot);
            mAnimations.add(mSources);
        }
    }

    public int getAlpha() {
        int alpha = mAlphaExpression != null ? (int) mAlphaExpression
                .evaluate(getContext().mVariables) : 255;
        int alpha1 = mAlphas != null ? mAlphas.getAlpha() : 255;
        return Utils.mixAlpha(alpha, alpha1);
    }

    public float getHeight() {
        if (mSizes != null)
            return (float) mSizes.getHeight();
        return (float) (mHeightExpression != null ? mHeightExpression
                .evaluate(getContext().mVariables) : -1);
    }

    public float getMaxHeight() {
        if (mSizes != null)
            return (float) mSizes.getMaxHeight();
        return (float) (mHeightExpression != null ? mHeightExpression
                .evaluate(getContext().mVariables) : -1);
    }

    public float getMaxWidth() {
        if (mSizes != null)
            return (float) mSizes.getMaxWidth();
        return (float) (mWidthExpression != null ? mWidthExpression
                .evaluate(getContext().mVariables) : -1);
    }

    public float getPivotX() {
        return (float) (mCenterXExpression != null ? mCenterXExpression
                .evaluate(getContext().mVariables) : 0);
    }

    public float getPivotY() {
        return (float) (mCenterYExpression != null ? mCenterYExpression
                .evaluate(getContext().mVariables) : 0);
    }

    public float getRotationAngle() {
        double angle = mRotationExpression != null ? mRotationExpression
                .evaluate(getContext().mVariables) : 0;
        float f = mRotations != null ? mRotations.getAngle() : 0;
        return (float) (angle + f);
    }

    public String getSrc() {
        String src = mSrcFormatter.getText(getContext().mVariables);

        if (mSources != null)
            src = mSources.getSrc();
        if (src != null && mSrcIdExpression != null) {
            long id = (long) mSrcIdExpression.evaluate(getContext().mVariables);
            src = Utils.addFileNameSuffix(src, String.valueOf(id));
        }
        return src;
    }

    public float getWidth() {
        if (mSizes != null)
            return (float) mSizes.getWidth();
        return (float) (mWidthExpression != null ? mWidthExpression
                .evaluate(getContext().mVariables) : -1);
    }

    public float getX() {
        double x = mBaseXExpression != null ? mBaseXExpression.evaluate(getContext().mVariables)
                : 0;
        if (mSources != null)
            x += mSources.getX();
        if (mPositions != null)
            x += mPositions.getX();
        return (float) x;
    }

    public float getY() {
        double y = mBaseYExpression != null ? mBaseYExpression.evaluate(getContext().mVariables)
                : 0;
        if (mSources != null)
            y += mSources.getY();
        if (mPositions != null)
            y += mPositions.getY();
        return (float) y;
    }

    public float getOffsetX() {
        float x = 0;
        if (mPositions != null)
            x = (float) mPositions.getX();
        return x;
    }

    public float getOffsetY() {
        float y = 0;
        if (mPositions != null)
            y = (float) mPositions.getY();
        return y;
    }

    public void init() {
        int N = mAnimations.size();
        for (int i = 0; i < N; i++)
            mAnimations.get(i).init();

    }

    public boolean isAlignAbsolute() {
        return mAlignAbsolute;
    }

    public void load(Element node) throws ScreenElementLoadException {
        if (node == null) {
            if (DEBUG){
                Log.e(LOG_TAG, "node is null");
            }
            return;
        }
        mBaseXExpression = createExp(node, "x", "left");
        mBaseYExpression = createExp(node, "y", "top");
        mWidthExpression = createExp(node, "w", "width");
        mHeightExpression = createExp(node, "h", "height");
        mRotationExpression = createExp(node, "angle", "rotation");
        mCenterXExpression = createExp(node, "centerX", "pivotX");
        mCenterYExpression = createExp(node, "centerY", "pivotY");
        mSrcIdExpression = createExp(node, "srcid", null);
        mAlphaExpression = createExp(node, "alpha", null);
        mSrcFormatter = TextFormatter.fromElement(node, "src", "srcFormat", "srcParas", "srcExp",
                "srcFormatExp");
        String align = node.getAttribute("align");
        if (align.equalsIgnoreCase("absolute"))
            mAlignAbsolute = true;
        loadSourceAnimations(node);
        loadPositionAnimations(node);
        loadRotationAnimations(node);
        loadSizeAnimations(node);
        loadAlphaAnimations(node);
    }

    public void reset(long time) {
        int N = mAnimations.size();
        for (int i = 0; i < N; i++)
            mAnimations.get(i).reset(time);

    }

    public void tick(long currentTime) {
        int N = mAnimations.size();
        for (int i = 0; i < N; i++)
            mAnimations.get(i).tick(currentTime);

    }

    private Expression.NumberExpression mYExpression;
    private Expression.NumberExpression mXExpression;
    private Expression.NumberExpression mHExpression;
    private Expression.NumberExpression mAExpression;


    public void setAlpha(int alpha) {
        if (mAExpression == null) {
            mAExpression = new Expression.NumberExpression(String.valueOf(alpha));
            mAlphaExpression = mAExpression;
        }
        mAExpression.setValue(alpha);
    }

    public void setX(float x) {
        if (mXExpression == null) {
            mXExpression = new Expression.NumberExpression(String.valueOf(x));
            mBaseXExpression = mXExpression;
        }
        mXExpression.setValue(x);
    }

    public void setY(float y) {
        if (mYExpression == null) {
            mYExpression = new Expression.NumberExpression(String.valueOf(y));
            mBaseYExpression = mYExpression;
        }
        mYExpression.setValue(y);
    }

    public void setH(float paramDouble) {
        if (mHExpression == null) {
            mHExpression = new Expression.NumberExpression(String.valueOf(paramDouble));
            mHeightExpression = mHExpression;
        }
        mHExpression.setValue(paramDouble);
    }

    public int evaluateAlpha(ElementGroup mParent) {
        int alpha = 0;
        if (mAlphaExpression != null) {
            alpha = (int)mAlphaExpression.evaluate(getContext().mVariables);
            if (mAlphas == null)
                return alpha;
        }
        if(mAlphas != null){
            alpha = Utils.mixAlpha(mAlphas.getAlpha(), alpha);
            if(mParent == null)
                return alpha;
        }
        return Utils.mixAlpha(alpha, mParent.getAlpha());
    }

    public void setSrc(String string) {
        if (mSrcFormatter != null)
            mSrcFormatter.setText(string);
    }

}
