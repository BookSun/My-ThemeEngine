package com.lewa.support.laml.shader;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;

import org.w3c.dom.Element;

import android.graphics.SweepGradient;
/**
 * 
 * SweepGradientElement.java:
 * @author yljiang@lewatek.com 2014-7-8
 */
public class SweepGradientElement extends ShaderElement {

    public static final String TAG_NAME = "SweepGradient";
    private float              mAngle;
    private Expression         mAngleExp;

    public SweepGradientElement(Element ele, ScreenElementRoot root){
        super(ele, root);
        mAngleExp = Expression.build(ele.getAttribute("rotation"));
        mGradientStops.update();
    }

    private final float getAngle() {
        return mAngleExp != null ? (float)mAngleExp.evaluate(mRoot.getVariables()) :0;
    }

    public void onGradientStopsChanged() {
        mX = 0;
        mY = 0;
        mAngle = 0;
        mShader = new SweepGradient(getX(), getY(), mGradientStops.getColors(), mGradientStops.getPositions());
    }

    private static final boolean OPEN = false ;
    public boolean updateShaderMatrix() {
        float x = getX();
        float y = getY();
        float angle = getAngle();
        if (x != mX || y != mY || angle != mAngle) {
            mX = x;
            mY = y;
            mAngle = angle;
            if(OPEN){
                mShaderMatrix.reset();
                mShaderMatrix.preTranslate(-x, -y);
                mShaderMatrix.setRotate(angle);
                mShaderMatrix.postTranslate(x, y);
            }
            return true;
        }
        return false;
    }
}
