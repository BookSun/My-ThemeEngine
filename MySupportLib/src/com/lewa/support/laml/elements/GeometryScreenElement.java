package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.shader.ShadersElement;
import com.lewa.support.laml.util.ColorParser;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.text.TextUtils;
import android.util.Log;

/**
 * GeometryScreenElement.java:
 * 
 * @author yljiang@lewatek.com 2014-7-8
 */
public abstract class GeometryScreenElement extends AnimatedScreenElement {

    private static final String LOG_TAG = "GeometryScreenElement";
    private int                 mFillColor;
    protected ColorParser       mFillColorParser;
    protected ShadersElement    mFillShadersElement;
    protected Paint             mPaint  = new Paint();
    private final DrawMode      mStrokeAlign;
    private int                 mStrokeColor;
    protected ColorParser       mStrokeColorParser;

    protected ShadersElement    mStrokeShadersElement;
    protected float             mWeight = scale(1);
    protected Expression        mWeightExp;
    protected Expression        mXfermodeNumExp;

    public GeometryScreenElement(Element ele, ScreenElementRoot root) throws ScreenElementLoadException{
        super(ele, root);
        String strokeColor = ele.getAttribute("strokeColor");
        if (!TextUtils.isEmpty(strokeColor)) {
            mStrokeColorParser = new ColorParser(strokeColor);
        }
        String fillColor = ele.getAttribute("fillColor");
        if (!TextUtils.isEmpty(fillColor)) {
            mFillColorParser = new ColorParser(fillColor);
        }
        mWeightExp = Expression.build(ele.getAttribute("weight"));
        Paint.Cap cap = getCap(ele.getAttribute("cap"));
        mPaint.setStrokeCap(cap);
        float[] arrayOfFloat = resolveDashIntervals(ele);
        if (arrayOfFloat != null) {
            mPaint.setPathEffect(new DashPathEffect(arrayOfFloat, 0));
        }
        mStrokeAlign = DrawMode.getStrokeAlign(ele.getAttribute("strokeAlign"));
        mXfermodeNumExp = Expression.build(ele.getAttribute("xfermodeNum"));
        if (mXfermodeNumExp == null) {
            PorterDuff.Mode localMode = Utils.getPorterDuffMode(ele.getAttribute("xfermode"));
            mPaint.setXfermode(new PorterDuffXfermode(localMode));
        }
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        loadShadersElement(ele, root);
    }

    private final Paint.Cap getCap(String str) {
        Paint.Cap cap = Paint.Cap.BUTT;
        if (TextUtils.isEmpty(str)) return cap;
        if (str.equalsIgnoreCase("round")) return Paint.Cap.ROUND;
        if (str.equalsIgnoreCase("square")) return Paint.Cap.SQUARE;
        return cap;
    }

    private void loadShadersElement(Element ele, ScreenElementRoot root) {
        Element strokeShadersElement = Utils.getChild(ele, "StrokeShaders");
        if (strokeShadersElement != null) {
            mStrokeShadersElement = new ShadersElement(strokeShadersElement, root);
        }
        Element fillShadersElement = Utils.getChild(ele, "FillShaders");
        if (fillShadersElement != null) {
            mFillShadersElement = new ShadersElement(fillShadersElement, root);
        }
    }

    private float[] resolveDashIntervals(Element ele) {
        String str = ele.getAttribute("dash");
        if (TextUtils.isEmpty(str)) return null;
        String[] arrayOfString = str.split(",");
        if ((arrayOfString.length < 2) || (arrayOfString.length % 2 != 0)) return null;
        float[] arrayOfFloat = new float[arrayOfString.length];
        for (int i = 0; i < arrayOfString.length; i++) {
            arrayOfFloat[i] = Float.parseFloat(arrayOfString[i]);
        }
        return arrayOfFloat;
    }

    @SuppressLint("WrongCall")
    public void doRender(Canvas canvas) {
        mPaint.setShader(null);
        if (mFillShadersElement != null || mFillColorParser != null) {
            mPaint.setStyle(Paint.Style.FILL);
            if (mFillColorParser != null){
                mPaint.setColor(mFillColor);
            } 
            if (mFillShadersElement != null) {
                mPaint.setShader(mFillShadersElement.getShader());
            }
            mPaint.setAlpha(getAlpha());
            onDraw(canvas, DrawMode.FILL);
            return;
        } else if (mWeight > 0 && (mStrokeShadersElement != null || mStrokeColorParser != null)) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mWeight);
            if (mStrokeColorParser != null) mPaint.setColor(mStrokeColor);
            if (mStrokeShadersElement != null) {
                mPaint.setShader(mStrokeShadersElement.getShader());
            }
        }
        mPaint.setAlpha(getAlpha());
        onDraw(canvas, mStrokeAlign);
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        doTick(currentTime);
    }

    protected void doTick(long currentTime) {
        if (!isVisible()) return;
        Variables variables = getVariables();
        if (mStrokeColorParser != null) mStrokeColor = mStrokeColorParser.getColor(variables);
        if (mFillColorParser != null) mFillColor = mFillColorParser.getColor(variables);
        if (mStrokeShadersElement != null) mStrokeShadersElement.updateShader();
        if (mFillShadersElement != null) mFillShadersElement.updateShader();
        if (mWeightExp != null) {
            mWeight = scale(mWeightExp.evaluate(variables));
        }
        if (mXfermodeNumExp != null) {
            PorterDuff.Mode localMode = Utils.getPorterDuffMode((int)mXfermodeNumExp.evaluate(variables));
            mPaint.setXfermode(new PorterDuffXfermode(localMode));
        }
    }

    protected abstract void onDraw(Canvas canvas, DrawMode mode);

    protected static enum DrawMode {
        STROKE_CENTER, STROKE_OUTER, STROKE_INNER, FILL;

        public static DrawMode getStrokeAlign(String tagName) {
            if ("inner".equalsIgnoreCase(tagName)) return DrawMode.STROKE_INNER;
            if ("center".equalsIgnoreCase(tagName)) return DrawMode.STROKE_CENTER;
            return DrawMode.STROKE_OUTER;
        }
    }

}
