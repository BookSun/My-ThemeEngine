
package com.lewa.support.laml.elements;

import org.w3c.dom.Element;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.text.TextUtils;
import android.util.Log;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.util.ColorParser;

public class PathScreenElement extends AnimatedScreenElement {

    private static final String LOG_TAG = "PathScreenElement";

    public static final String TAG_NAME = "Path";

    public static final String[] TYPES = new String[] {
            "rect", "quad"
    };

    private static final int TYPE_RECT = 0;

    private static final int TYPE_QUAD = 1;

    protected Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    protected Paint mPaintShadow;

    protected Paint mPaintShadow2;

    protected Path mPath = new Path();

    protected Path mPathShadow;

    protected Path mPathShadow2;

    private Expression[] mPoints;

    private float[] mPointsValue;

    private float mHeight;

    private ColorParser mColorParser;

    private float mWidth;

    private int mType = -1;

    private boolean mShadow;

    public PathScreenElement(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, root);
        if (node == null)
            Log.e(LOG_TAG, "node is null");
        String type = node.getAttribute("type");
        for (int i = 0, N = TYPES.length; i < N; i++) {
            if (TYPES[i].equals(type)) {
                mType = i;
                switch (i) {
                    case TYPE_RECT:
                        mPoints = new Expression[] {
                                Expression.build(node.getAttribute("left")),
                                Expression.build(node.getAttribute("top")),
                                Expression.build(node.getAttribute("right")),
                                Expression.build(node.getAttribute("bottom")),
                        };
                        break;
                    case TYPE_QUAD:
                        mPoints = new Expression[] {
                                Expression.build(node.getAttribute("fromX")),
                                Expression.build(node.getAttribute("fromY")),
                                Expression.build(node.getAttribute("moveX")),
                                Expression.build(node.getAttribute("moveY")),
                                Expression.build(node.getAttribute("toX")),
                                Expression.build(node.getAttribute("toY"))
                        };
                        break;
                }
            }
        }
        if (mType < 0) {
            throw new ScreenElementLoadException("unsupported type " + type);
        }
        mPointsValue = new float[mPoints.length];
        mColorParser = ColorParser.fromElement(node);
        int color = getColor();
        mPaint.setColor(color);

        float width = scale(evaluate(Expression.build(node.getAttribute("width"))));
        mWidth = width == 0 ? 1 : width;
        mPaint.setStrokeWidth(mWidth);

        String style = node.getAttribute("style");
        if (!TextUtils.isEmpty(style)) {
            if ("stroke".equals(style))
                mPaint.setStyle(Style.STROKE);
            else if ("fill".equals(style))
                mPaint.setStyle(Style.FILL);
        }
        if (mShadow = Boolean.parseBoolean(node.getAttribute("shadow"))) {
            mPaintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaintShadow2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPathShadow = new Path();
            mPathShadow2 = new Path();
            mPaintShadow.setStyle(Style.STROKE);
            mPaintShadow2.setStyle(Style.STROKE);
            mPaintShadow.setStrokeWidth(mWidth);
            mPaintShadow2.setStrokeWidth(mWidth);
        }
    }

    protected int getColor() {
        return mColorParser.getColor(getVariables());
    }

    private float getPoint(Expression exp) {
        return scale(evaluate(exp));
    }

    protected void updatePath() {
        if (mType < 0)
            return;
        int alpha = getAlpha();
        mPaint.setAlpha(alpha);
        switch (mType) {
            case TYPE_QUAD:
                float cx = getPoint(mPoints[2]);
                float cy = getPoint(mPoints[3]);
                if (cx != mPointsValue[2] || cy != mPointsValue[3]) {
                    mPointsValue[2] = cx;
                    mPointsValue[3] = cy;
                    cx += getOffsetX();
                    cy += getOffsetY();
                    float height = getHeight();
                    if (height != mHeight) {
                        mHeight = height;
                        for (int i = mPoints.length - 1; i >= 0; i--) {
                            mPointsValue[i] = getPoint(mPoints[i]);
                        }
                    }
                    mPath.rewind();
                    if (mShadow) {
                        mPathShadow.rewind();
                        mPathShadow2.rewind();
                    }
                    float sx = mPointsValue[0];
                    float sy = mPointsValue[1];
                    float ex = mPointsValue[4];
                    float ey = mPointsValue[5];
                    mPath.moveTo(sx, sy);
                    mPath.quadTo(cx, cy, ex, ey);
                    if (mShadow) {
                        float shadowOffset = (float) (mWidth);
                        mPathShadow.moveTo(sx, sy - shadowOffset);
                        mPathShadow.quadTo(cx, cy - shadowOffset, ex, ey - shadowOffset);
                        mPathShadow.moveTo(sx, sy + shadowOffset);
                        mPathShadow.quadTo(cx, cy + shadowOffset, ex, ey + shadowOffset);
                        shadowOffset *= 2;
                        mPathShadow2.moveTo(sx, sy - shadowOffset);
                        mPathShadow2.quadTo(cx, cy - shadowOffset, ex, ey - shadowOffset);
                        mPathShadow2.moveTo(sx, sy + shadowOffset);
                        mPathShadow2.quadTo(cx, cy + shadowOffset, ex, ey + shadowOffset);
                    }
                }
                if (mShadow) {
                    int color = mPaint.getColor();
                    mPaintShadow.setColor(color & 0xffffff | (alpha / 6) << 24);
                    mPaintShadow2.setColor(color & 0xffffff | (alpha / 8) << 24);
                }
                break;
            case TYPE_RECT:
                float height = getHeight();
                if (height != mHeight) {
                    mHeight = height;
                    for (int i = mPoints.length - 1; i >= 0; i--) {
                        mPointsValue[i] = getPoint(mPoints[i]);
                    }
                }
                float x = getOffsetX();
                float y = getOffsetY();
                mPath.rewind();
                mPath.addRect(mPointsValue[0] + x, mPointsValue[1] + y, mPointsValue[2] + x,
                        mPointsValue[3] + y, Direction.CW);
                break;
        }
    }

    @Override
    public void doRender(Canvas c) {
        if (isVisible()) {
            updatePath();
            c.save();
            c.drawPath(mPath, mPaint);
            if (mShadow) {
                c.drawPath(mPathShadow, mPaintShadow);
                c.drawPath(mPathShadow2, mPaintShadow2);
            }
            c.restore();
        }
    }
}
