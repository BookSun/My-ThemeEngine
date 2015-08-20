package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * EllipseScreenElement.java:
 * 
 * @author yljiang@lewatek.com 2014-7-8
 */
public class EllipseScreenElement extends GeometryScreenElement {

    public static final String TAG_NAME = "Ellipse";

    public EllipseScreenElement(Element ele, ScreenElementRoot root) throws ScreenElementLoadException{
        super(ele, root);
    }

    protected void onDraw(Canvas canvas, DrawMode mode) {
        float width = getWidth();
        float height = getHeight();
        if (width < 0 || height < 0)
            return;
        if (mode == DrawMode.STROKE_OUTER) {
            width += mWeight;
            height += mWeight;
        }
        if (mode == DrawMode.STROKE_INNER) {
            width -= mWeight;
            height -= mWeight;
        }
        float left = getX() - width / 2;
        float top = getY() - height / 2;
        canvas.drawOval(new RectF(left, top, left + width, top + height), mPaint);
    }
}
