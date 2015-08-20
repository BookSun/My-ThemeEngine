package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.util.ColorParser;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.view.MotionEvent;

public class PaintScreenElement extends AnimatedScreenElement {

    private Bitmap             mCachedBitmap;
    private Canvas             mCachedCanvas;
    private Paint              mCachedPaint;
    private int                mColor;
    private ColorParser        mColorParser;
    private Paint              mPaint;
    private Path               mPath;
    private boolean            mPendingMouseUp;
    private boolean            mPressed;
    private float              mWeight;
    private Expression         mWeightExp;
    private Xfermode           mXfermode;
    private static float       DEFAULT_WEIGHT = 1;
    public static final String TAG_NAME       = "Paint";

    public PaintScreenElement(Element node, ScreenElementRoot root) throws ScreenElementLoadException{
        super(node, root);
        load(node, root);
        mPath = new Path();
        DEFAULT_WEIGHT = scale(DEFAULT_WEIGHT);
        mWeight = DEFAULT_WEIGHT;
        mPaint = new Paint();
        mPaint.setXfermode(mXfermode);
        mPaint.setAntiAlias(true);
        mCachedPaint = new Paint();
        mCachedPaint.setStyle(Paint.Style.STROKE);
        mCachedPaint.setStrokeWidth(DEFAULT_WEIGHT);
        mCachedPaint.setStrokeCap(Paint.Cap.ROUND);
        mCachedPaint.setStrokeJoin(Paint.Join.ROUND);
        mCachedPaint.setAntiAlias(true);
    }

    private void load(Element node, ScreenElementRoot root) {
        if (node == null) {
            return;
        }
        mWeightExp = Expression.build(node.getAttribute("weight"));
        mColorParser = ColorParser.fromElement(node);
        mXfermode = new PorterDuffXfermode(Utils.getPorterDuffMode(node.getAttribute("xfermode")));
    }

    private void performAction(String str) {
        mRoot.onUIInteractive(this, str);
    }

    public void doRender(Canvas canvas) {
        float abLeft = getAbsoluteLeft();
        float abTop = getAbsoluteTop();

        float left = getLeft(0, getWidth());
        float top = getTop(0, getHeight());
        
        if (mPendingMouseUp) {
            mCachedCanvas.save();
            mCachedCanvas.translate(-abLeft, -abTop);
            mCachedCanvas.drawPath(mPath, mCachedPaint);
            mCachedCanvas.restore();
            mPath.reset();
            mPendingMouseUp = false;
        }
        canvas.drawBitmap(mCachedBitmap, left, top, mPaint);
        if (mPressed && mWeight > 0 && getAlpha() > 0) {
            mCachedPaint.setStrokeWidth(mWeight);
            mCachedPaint.setColor(mColor);
            mCachedPaint.setAlpha(Utils.mixAlpha(mCachedPaint.getAlpha(), getAlpha()));
            canvas.save();
            canvas.translate(left , top );
//            canvas.translate(left - abLeft, top - abTop);
            Xfermode localXfermode = mCachedPaint.getXfermode();
            mCachedPaint.setXfermode(mXfermode);
            canvas.drawPath(mPath, mCachedPaint);
            mCachedPaint.setXfermode(localXfermode);
            canvas.restore();
        }
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        doTick(currentTime);
    }

    protected void doTick(long currentTime) {
        if (!isVisible()) {
            return;
        }
        Variables variables = getVariables();
        if (mWeightExp != null) {
            mWeight = scale(mWeightExp.evaluate(variables));
        }
        mColor = mColorParser.getColor(variables);
    }

    public void finish() {
        super.finish();
        mCachedBitmap.recycle();
        mCachedBitmap = null;
        mCachedCanvas = null;
    }

    public void init() {
        super.init();
        float width = getWidth();
        if (width < 0) {
            width = scale(Utils.getVariableNumber("screen_width", getVariables()));
        }
        float height = getHeight();
        if (height < 0) {
            height = scale(Utils.getVariableNumber("screen_height", getVariables()));
        }
        mCachedBitmap = Bitmap.createBitmap((int)Math.ceil(width), (int)Math.ceil(height), Bitmap.Config.ARGB_8888);
        mCachedBitmap.setDensity(mRoot.getTargetDensity());
        mCachedCanvas = new Canvas(mCachedBitmap);
    }

    public boolean onTouch(MotionEvent event) {
        if (!isVisible()) {
            return false;
        }
        float touchX = event.getX();
        float touchY = event.getY();
        if (!touched(touchX, touchY)) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPressed = true;
                mPath.reset();
                mPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(touchX, touchY);
                performAction("move");
                break;
            case MotionEvent.ACTION_CANCEL:
                mPressed = false;
                mPendingMouseUp = true;
                performAction("cancel");
                break;
        }
        return true;
    }

    public void reset(long currentTime) {
        super.reset(currentTime);
        mCachedCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        mPressed = false;
    }
}
