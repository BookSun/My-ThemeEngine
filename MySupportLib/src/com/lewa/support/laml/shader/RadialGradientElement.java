package com.lewa.support.laml.shader;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;

import org.w3c.dom.Element;

import android.graphics.RadialGradient;
/**
 * 
 * RadialGradientElement.java:
 * @author yljiang@lewatek.com 2014-7-8
 */
public class RadialGradientElement extends ShaderElement {

    public static final String TAG_NAME = "RadialGradient";
    private float              mRx;
    private Expression         mRxExp;
    private float              mRy;
    private Expression         mRyExp;

    public RadialGradientElement(Element ele, ScreenElementRoot root){
        super(ele, root);
        mRxExp = Expression.build(ele.getAttribute("rX"));
        mRyExp = Expression.build(ele.getAttribute("rY"));
        mGradientStops.update();
    }

    private final float getRx() {
        return mRxExp != null ? (float)(mRxExp.evaluate(mRoot.getVariables()) * mRoot.getScale()):0;
    }

    private final float getRy() {
        return mRyExp != null ? (float)(mRyExp.evaluate(mRoot.getVariables()) * mRoot.getScale()):0;
    }

    public void onGradientStopsChanged() {
        mX = 0;
        mY = 0;
        mRx = 1;
        mRy = 1;
        mShader = new RadialGradient(0, 0, 1, mGradientStops.getColors(), mGradientStops.getPositions(), mTileMode);
    }

    public boolean updateShaderMatrix() {
        float xExp = getX();
        float yExp = getY();
        float rx = getRx();
        float ry = getRy();
        if (xExp != mX || yExp != mY || rx != mRx || ry != mRy) {
            mX = xExp;
            mY = yExp;
            mRx = rx;
            mRy = ry;
            mShaderMatrix.reset();
            mShaderMatrix.preTranslate(-xExp, -yExp);
            mShaderMatrix.setScale(rx, ry);
            mShaderMatrix.postTranslate(xExp, yExp);
            return true;
        }
        return false;
    }
}
