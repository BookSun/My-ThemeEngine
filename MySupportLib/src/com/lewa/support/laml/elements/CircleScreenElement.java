package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;

import org.w3c.dom.Element;

import android.graphics.Canvas;

/**
 * CircleScreenElement.java:
 * 
 * @author yljiang@lewatek.com 2014-7-8
 */
public class CircleScreenElement extends GeometryScreenElement {

    public static final String TAG_NAME = "Circle";
    private Expression         mRadiusExp;

    public CircleScreenElement(Element ele, ScreenElementRoot root) throws ScreenElementLoadException{
        super(ele, root);
        mRadiusExp = Expression.build(ele.getAttribute("r"));
    }

    private final float getRadius() {
        return mRadiusExp != null ? scale((float)mRadiusExp.evaluate(mRoot.getVariables())) : 0;
    }

    protected void onDraw(Canvas canvas, DrawMode mode) {
        float radius = getRadius();
        if (mode == DrawMode.STROKE_OUTER) {
            radius += mWeight / 2;
        } else if (mode == DrawMode.STROKE_INNER) {
            radius -= mWeight / 2;
        }
        canvas.drawCircle(getX(), getY(), radius, mPaint);
    }
}
