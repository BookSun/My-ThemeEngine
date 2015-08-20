package com.lewa.support.laml.elements;

import android.graphics.Canvas;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.MotionEvent;

import com.lewa.support.laml.NotifierManager;
import com.lewa.support.laml.ScreenContext;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.FramerateTokenList.FramerateToken;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.Task;

import org.w3c.dom.Element;

public abstract class ScreenElement {

    public static final String    ACTUAL_H         = "actual_h";

    public static final String    ACTUAL_W         = "actual_w";

    public static final String    ACTUAL_X         = "actual_x";

    public static final String    ACTUAL_Y         = "actual_y";

    public static final String    VISIBILITY       = "visibility";

    public static final int       VISIBILITY_FALSE = 0;

    public static final int       VISIBILITY_TRUE  = 1;

    private IndexedNumberVariable mActualHeightVar;

    private IndexedNumberVariable mActualWidthVar;

    protected Align               mAlign;

    protected AlignV              mAlignV;

    protected String              mCategory;

    protected FramerateToken      mFramerateToken;

    protected boolean             mHasName;

    private boolean               mInitShow        = true;

    private boolean               mIsVisible       = true;

    protected String              mName;

    protected ElementGroup        mParent;

    protected ScreenElementRoot   mRoot;

    private boolean               mShow            = true;

    private Expression            mVisibilityExpression;

    private IndexedNumberVariable mVisibilityVar;
    protected Element             mNode;

    public ScreenElement(Element ele, ScreenElementRoot root){
        mRoot = root;
        mNode = ele;
        if (ele != null) {
            mCategory = ele.getAttribute(Task.TAG_CATEGORY);
            mName = ele.getAttribute(Task.TAG_NAME);
            mHasName = !TextUtils.isEmpty(mName);
            String vis = ele.getAttribute(VISIBILITY);

            if (!TextUtils.isEmpty(vis)) {
                if (vis.equalsIgnoreCase("false")) {
                    mInitShow = false;
                } else if (vis.equalsIgnoreCase("true")) {
                    mInitShow = true;
                } else {
                    mVisibilityExpression = Expression.build(vis);
                }
            }

            String align = ele.getAttribute("align");
            if (align.equalsIgnoreCase("right")) {
                mAlign = Align.RIGHT;
            } else if (align.equalsIgnoreCase("left")) {
                mAlign = Align.LEFT;
            } else if (align.equalsIgnoreCase("center")) {
                mAlign = Align.CENTER;
            } else {
                mAlign = Align.LEFT;
            }

            align = ele.getAttribute("alignV");
            if (align.equalsIgnoreCase("bottom")) {
                mAlignV = AlignV.BOTTOM;
            } else if (align.equalsIgnoreCase("top")) {
                mAlignV = AlignV.TOP;
            } else if (align.equalsIgnoreCase("center")) {
                mAlignV = AlignV.CENTER;
            } else {
                mAlignV = AlignV.TOP;
            }
        }
    }

    private void setVisibilityVar(boolean visible) {
        if (mHasName) {
            if (mVisibilityVar == null) mVisibilityVar = new IndexedNumberVariable(mName, VISIBILITY,
                                                                                   mRoot.getContext().mVariables);
            mVisibilityVar.set(visible ? VISIBILITY_TRUE : VISIBILITY_FALSE);
        }
    }

    protected float descale(float v) {
        return v / mRoot.getScale();
    }

    public abstract void doRender(Canvas canvas);

    protected double evaluate(Expression exp) {
        return exp == null ? 0 : exp.evaluate(getVariables());
    }

    protected String evaluateStr(Expression exp) {
        return exp == null ? null : exp.evaluateStr(getVariables());
    }

    public ScreenElement findElement(String name) {
        return mName != null && mName.equals(name) ? this : null;
    }

    public void finish() {
    }

    public ScreenContext getContext() {
        return mRoot.getContext();
    }

    protected float getFramerate() {
        return mFramerateToken == null ? 0 : mFramerateToken.getFramerate();
    }

    protected float getLeft(float pos, float width) {
        if (width > 0) {
            float x = pos;
            switch (mAlign) {
                case LEFT:
                    break;
                case CENTER:
                    x -= width / 2;
                    break;
                case RIGHT:
                    x -= width;
                    break;
            }
            return x;
        }
        return pos;
    }

    public String getName() {
        return mName;
    }

    protected NotifierManager getNotifierManager() {
        return NotifierManager.getInstance(getContext().mContext);
    }

    protected float getTop(float pos, float height) {
        if (height > 0) {
            float y = pos;
            switch (mAlignV) {
                case TOP:
                    break;
                case CENTER:
                    y -= height / 2;
                    break;
                case BOTTOM:
                    y -= height;
                    break;
            }
            return y;
        }
        return pos;
    }

    public Variables getVariables() {
        return getContext().mVariables;
    }

    public void init() {
        mShow = mInitShow;
        updateVisibility();
        setVisibilityVar(isVisible());
        mFramerateToken = null;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    protected boolean isVisibleInner() {
        return mShow
               && (mVisibilityExpression == null || mVisibilityExpression.evaluate(mRoot.getContext().mVariables) > 0);
    }

    public boolean onTouch(MotionEvent event) {
        return false;
    }

    protected void onVisibilityChange(boolean visible) {
        setVisibilityVar(visible);
    }

    public void pause() {
    }

    public void render(Canvas c) {
        updateVisibility();
        if (isVisible()) doRender(c);
    }

    protected void requestFramerate(float f) {
        if (f >= 0) {
            if (mFramerateToken == null && f != 0) {
                mFramerateToken = getContext().createToken(toString());
            }
            if (mFramerateToken != null) {
                mFramerateToken.requestFramerate(f);
            }
        }
    }

    public void requestUpdate() {
        getContext().requestUpdate();
    }

    public void reset() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        reset(elapsedRealtime);
        tick(elapsedRealtime);
    }

    public void reset(long time) {
    }

    public void resume() {
        updateVisibility();
    }

    protected float scale(double v) {
        return (float)(v * mRoot.getScale());
    }

    protected void setActualHeight(double value) {
        if (!mHasName) return;

        if (mActualHeightVar == null) mActualHeightVar = new IndexedNumberVariable(mName, ACTUAL_H, getVariables());

        mActualHeightVar.set(value);
    }

    protected void setActualWidth(double value) {
        if (!mHasName) return;

        if (mActualWidthVar == null) mActualWidthVar = new IndexedNumberVariable(mName, ACTUAL_W, getVariables());

        mActualWidthVar.set(value);
    }

    public void setParent(ElementGroup parent) {
        mParent = parent;
    }

    public void show(boolean show) {
        mShow = show;
        updateVisibility();
    }

    public void showCategory(String category, boolean show) {
        if (mCategory != null && mCategory.equals(category)) show(show);
    }

    public void tick(long currentTime) {
    }

    protected void updateVisibility() {
        boolean v = isVisibleInner();

        if (mIsVisible != v) {
            mIsVisible = v;
            onVisibilityChange(mIsVisible);
        }
    }

    protected static enum Align {
        LEFT, CENTER, RIGHT
    }

    protected static enum AlignV {
        TOP, CENTER, BOTTOM
    }

    public void setName(String name) {
        mName = name;
    }

    public void onUIInteractive(ScreenElement element, String action) {
    }
    //TODO This should be add by Manifest.xml ,But for the moment we do this, for the later,will be optimized
    protected boolean isAsyncLoad=false;
    protected boolean isTickFinished=true;
    protected int position;
}
