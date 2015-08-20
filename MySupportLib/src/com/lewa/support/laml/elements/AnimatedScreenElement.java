package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.animation.AnimatedElement;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.TextUtils;

public abstract class AnimatedScreenElement extends ScreenElement {

    private IndexedNumberVariable mActualXVar;

    private IndexedNumberVariable mActualYVar;

    protected AnimatedElement     mAni;

    private Camera                mCamera;

    private Matrix                mMatrix = new Matrix();

    private Expression            mPivotZ;

    private Expression            mRotationX;

    private Expression            mRotationY;

    private Expression            mRotationZ;

    private Expression            mScaleExpression;

    private Expression            mScaleXExpression;

    private Expression            mScaleYExpression;

    private Paint mPaint = new Paint();

    public AnimatedScreenElement(Element node, ScreenElementRoot root){
        super(node, root);
        mAni = new AnimatedElement(node, root);
        if (node == null) {
            return;
        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(scale(1.0D));
        mPaint.setColor(-4982518);
        if (mHasName) {
            mActualXVar = new IndexedNumberVariable(mName, "actual_x", getVariables());
            mActualYVar = new IndexedNumberVariable(mName, "actual_y", getVariables());
        }

        mScaleExpression = createExp(node, "scale", null);
        mScaleXExpression = createExp(node, "scaleX", null);
        mScaleYExpression = createExp(node, "scaleY", null);
        mRotationX = createExp(node, "angleX", "rotationX");
        mRotationY = createExp(node, "angleY", "rotationY");
        mRotationZ = createExp(node, "angleZ", "rotationZ");
        mPivotZ = createExp(node, "centerZ", "pivotZ");
        if (mRotationX != null || mRotationY != null || mRotationZ != null) {
            mCamera = new Camera();
        }
    }

    private Expression createExp(Element node, String name, String compatibleName) {
        Expression exp = Expression.build(node.getAttribute(name));
        if ((exp == null) && (!TextUtils.isEmpty(compatibleName))) exp = Expression.build(node.getAttribute(compatibleName));

        return exp;
    }

    public int getAlpha() {
        int a = mAni.getAlpha();
        if (mParent != null) return Utils.mixAlpha(a, mParent.getAlpha());
        return a;
    }

    public void setAlpha(int alpha){
        mAni.setAlpha(alpha);
    }

    public float getHeight() {
        return scale(mAni.getHeight());
    }

    protected float getLeft() {
        return getLeft(getX(), getWidth());
    }

    public float getMaxHeight() {
        return scale(mAni.getMaxHeight());
    }

    public float getMaxWidth() {
        return scale(mAni.getMaxWidth());
    }

    public float getPivotX() {
        return scale(mAni.getPivotX());
    }

    public float getPivotY() {
        return scale(mAni.getPivotY());
    }

    public float getRotation() {
        return mAni.getRotationAngle();
    }

    protected float getTop() {
        return getTop(getY(), getHeight());
    }

    public float getWidth() {
        return scale(mAni.getWidth());
    }

    public float getX() {
        return scale(mAni.getX());
    }

    public float getY() {
        return scale(mAni.getY());
    }

    public float getOffsetX() {
        return scale(mAni.getOffsetX());
    }

    public float getOffsetY() {
        return scale(mAni.getOffsetY());
    }

    public void init() {
        super.init();
        mAni.init();
    }

    protected boolean isVisibleInner() {
        return super.isVisibleInner() && getAlpha() > 0;
    }

    public void render(Canvas c) {
        updateVisibility();
        if (isVisible()) {
            float pivotX = getLeft() + getPivotX();
            float pivotY = getTop() + getPivotY();
            int sc = c.save();
            Variables var = getVariables();
            mMatrix.reset();

            if (mCamera != null) {
                mCamera.save();
                float x = mRotationX != null ? (float)mRotationX.evaluate(var) : 0;
                float y = mRotationY != null ? (float)mRotationY.evaluate(var) : 0;
                float z = mRotationZ != null ? (float)mRotationZ.evaluate(var) : 0;
                if (x != 0 || y != 0 || z != 0) {
                    mCamera.rotate(x, y, z);
                    if (mPivotZ != null) mCamera.translate(0, 0, (float)mPivotZ.evaluate(var));
                    mCamera.getMatrix(mMatrix);
                    mMatrix.preTranslate(-pivotX, -pivotY);
                    mMatrix.postTranslate(pivotX, pivotY);
                    mCamera.restore();
                    c.concat(mMatrix);
                }
            }

            float rotation = getRotation();
            if (rotation != 0) {
                mMatrix.setRotate(rotation, pivotX, pivotY);
                c.concat(mMatrix);
            }

            if (mScaleExpression != null) {
                float scale = (float)mScaleExpression.evaluate(var);
                mMatrix.setScale(scale, scale, pivotX, pivotY);
                c.concat(mMatrix);
            } else if (mScaleXExpression != null || mScaleYExpression != null) {
                float scaleX = mScaleXExpression == null ? 1 : (float)mScaleXExpression.evaluate(var);
                float scaleY = mScaleYExpression == null ? 1 : (float)mScaleYExpression.evaluate(var);
                if (scaleX != 0 || scaleY != 0) {
                    mMatrix.setScale(scaleX, scaleY, pivotX, pivotY);
                    c.concat(mMatrix);
                }
            }

            doRender(c);
            c.restoreToCount(sc);
        }
    }

    public void reset(long time) {
        super.reset(time);
        mAni.reset(time);
        updateVisibility();
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        mAni.tick(currentTime);
        if (mHasName) {
            mActualXVar.set(mAni.getX());
            mActualYVar.set(mAni.getY());
        }
    }

    private Expression.NumberExpression mSIdExpression;

    public void setSrc(String string) {
        mAni.setSrc(string);
    }

    public void setSrcId(float doubleValue) {
        if (mSIdExpression == null) {
            mSIdExpression = new Expression.NumberExpression(String.valueOf(doubleValue));
        }
        mSIdExpression.setValue(doubleValue);

    }

    protected float getAbsoluteLeft() {
        float left = getLeft();
        float parentLeft = 0;
        if (mParent != null) {
            parentLeft = mParent.getAbsoluteLeft();
        }
        return parentLeft + left;
    }

    protected float getAbsoluteTop() {
        float top = getTop();
        float parentTop = 0;
        if (mParent != null) {
            parentTop = mParent.getAbsoluteTop();
        }
        return parentTop + top;
    }

    public void setY(float y) {
        mAni.setY(y);
    }

    public void setX(float x) {
        mAni.setX(x);
    }

    public void setH(float y) {
        mAni.setH(y);
    }

    public int evaluateAlpha() {
        return mAni.evaluateAlpha(mParent);
    }

    public boolean touched(float touchX, float touchY) {
        float left = getAbsoluteLeft();
        float top = getAbsoluteTop();
        float right = getWidth() + left;
        float bottom = getHeight() + top;
        if (touchX < left || touchX > right || touchY < top || touchY > bottom) {
            return false;
        }
        return true;
    }

    protected void doRenderWithTranslation(Canvas canvas) {
        int save = canvas.save();
        Variables variables = getVariables();
        mMatrix.reset();
        if (mCamera != null) {
            mCamera.save();
            float rotationX = mRotationX != null ? (float)mRotationX.evaluate(variables) : 0;
            float rotationY = mRotationY != null ? (float)mRotationY.evaluate(variables) : 0;
            float rotationZ = mRotationZ != null ? (float)mRotationZ.evaluate(variables) : 0;
            if (rotationX != 0 || rotationY != 0 || rotationZ != 0) {
                mCamera.rotate(rotationX, rotationY, rotationZ);
                if (mPivotZ != null) {
                    mCamera.translate(0, 0, (float)mPivotZ.evaluate(variables));
                }
                mCamera.getMatrix(mMatrix);
                mCamera.restore();
            }
        }
        if (getRotation() != 0) {
            mMatrix.preRotate(getRotation());
        }
        float sx = 1;
        float sy = 1;
        if (mScaleExpression != null) {
            sy = (float)mScaleExpression.evaluate(variables);
            sx = sy;
        }
        if (mScaleXExpression != null) {
            sx = (float)mScaleYExpression.evaluate(variables);
        }
        if (mScaleYExpression != null) {
            sy = (float)mScaleYExpression.evaluate(variables);
        }
        if (mScaleXExpression == null && mScaleYExpression == null) {
            if (sx != 1 || sy != 1) {
                mMatrix.preScale(sx, sy);
            }
            float x = getX();
            float y = getY();
            float f6 = getPivotX() - (x - getLeft());
            float f7 = getPivotY() - (y - getTop());
            mMatrix.preTranslate(-f6, -f7);
            mMatrix.postTranslate(f6 + x, f7 + y);
            canvas.concat(mMatrix);
            doRender(canvas);
            if (mRoot.mShowDebugLayout) {
                float width = getWidth();
                float height = getHeight();
                if (width > 0 && height > 0) {
                    float rotation0 = getLeft(0, width);
                    float rotation1 = getTop(0, height);
                    canvas.drawRect(rotation0, rotation1, rotation0 + width, rotation1 + height, mPaint);
                }
            }
            canvas.restoreToCount(save);
            return;
        }
    }

}
