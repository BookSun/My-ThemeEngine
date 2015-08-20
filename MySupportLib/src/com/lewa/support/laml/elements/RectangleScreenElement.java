package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

/**
 * RectangleScreenElement.java:
 * 
 * @author yljiang@lewatek.com 2014-7-8
 */
public class RectangleScreenElement extends GeometryScreenElement {

    private float               mCornerRadiusX;
    private float               mCornerRadiusY;
    private static final String LOG_TAG  = "RectangleScreenElement";
    public static final String  TAG_NAME = "Rectangle";

    public RectangleScreenElement(Element ele, ScreenElementRoot root) throws ScreenElementLoadException{
        super(ele, root);
        resolveCornerRadius(ele);
    }

    private void resolveCornerRadius(Element ele) {
        String[] strings = ele.getAttribute("cornerRadius").split(",");
        try {
            if (strings == null || strings.length < 1) 
                return;
            if (strings.length == 1) {
                float radius = scale(Float.parseFloat(strings[0]));
                mCornerRadiusY = radius;
                mCornerRadiusX = radius;
                return;
            }
        } catch (NumberFormatException localNumberFormatException) {
            Log.w(LOG_TAG, "illegal number format of cornerRadius.");
            return;
        }
        mCornerRadiusX = scale(Float.parseFloat(strings[0]));
        mCornerRadiusY = scale(Float.parseFloat(strings[1]));
    }

    protected void onDraw(Canvas canvas, DrawMode mode) {
        float width = getWidth();
        float height = getHeight();
        float left = getX();
        float top = getY();
        float drawWidth = left + width;
        float drawHeight = top + height;
        float offset = mWeight / 2f;

        if (mode == DrawMode.STROKE_OUTER) {
            left -= offset;
            top -= offset;
            drawWidth += offset;
            drawHeight += offset;
        }
        if (mode == DrawMode.STROKE_INNER) {
            left += offset;
            top += offset;
            drawWidth -= offset;
            drawHeight -= offset;
        }
        if (mCornerRadiusX > 0 && mCornerRadiusY > 0) {
            canvas.drawRoundRect(new RectF(left, top, drawWidth, drawHeight), mCornerRadiusX, mCornerRadiusY, mPaint);
        } else {
            canvas.drawRect(left, top, drawWidth, drawHeight, mPaint);
        }
    }
}
