package com.lewa.support.laml.shader;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;

import org.w3c.dom.Element;

import android.graphics.LinearGradient;
import android.graphics.Matrix;
/**
 * 
 * LinearGradientElement.java:
 * @author yljiang@lewatek.com 2014-7-8
 */
public class LinearGradientElement extends ShaderElement {

    public static final String TAG_NAME = "LinearGradient";
    private float              mEndX;
    private Expression         mEndXExp;
    private float              mEndY;
    private Expression         mEndYExp;
    private float              mInitEndX;
    private float              mInitEndY;
    private float              mInitX;
    private float              mInitY;

    public LinearGradientElement(Element ele, ScreenElementRoot root){
        super(ele, root);
        mEndXExp = Expression.build(ele.getAttribute("x1"));
        mEndYExp = Expression.build(ele.getAttribute("y1"));
        mGradientStops.update();
    }

    private final float getEndX() {
        return mEndXExp != null ? (float)(mEndXExp.evaluate(mRoot.getVariables()) * mRoot.getScale()):0;
    }

    private final float getEndY() {
        return mEndYExp != null ? (float)(mEndYExp.evaluate(mRoot.getVariables()) * mRoot.getScale()):0;
    }

    public void onGradientStopsChanged() {
        float xExp = getX();
        mInitX = xExp;
        mX = xExp;

        float yExp = getY();
        mInitY = yExp;
        mY = yExp;

        float endXExp = getEndX();
        mInitEndX = endXExp;
        mEndX = endXExp;

        float endYExp = getEndY();
        mInitEndY = endYExp;
        mEndY = endYExp;
        mShader = new LinearGradient(mX, mY, mEndX, mEndY, mGradientStops.getColors(), mGradientStops.getPositions(), mTileMode);
    }

    public boolean updateShaderMatrix() {
        float xExp = getX();
        float yExp = getY();
        float endXExp = getEndX();
        float endYExp = getEndY();
        if (xExp != mX || yExp != mY ||  endXExp != mEndX || endYExp != getEndY()){
            mX =  xExp;
            mY = yExp;
            mEndX =  endXExp;
            mEndY = endYExp;
            mShaderMatrix.reset();
            Matrix localMatrix = mShaderMatrix;
            float[] arrayOfFloat = new float[4];
            arrayOfFloat[0] = mInitX;
            arrayOfFloat[1] = mInitY;
            arrayOfFloat[2] = mInitEndX;
            arrayOfFloat[3] = mInitEndY;
            localMatrix.setPolyToPoly(arrayOfFloat, 0, new float[] { xExp, yExp,  endXExp, endYExp }, 0, 2);
            return true ;
        }
        return false ;
    }
}

