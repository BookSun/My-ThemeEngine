package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.VariableNames;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;

public class VirtualScreen extends ElementGroup {

    public static final String    TAG_NAME                      = "VirtualScreen";

    private Bitmap                mScreenBitmap;
    private Canvas                mScreenCanvas;
    private IndexedNumberVariable mFreezeMode;
    private boolean               mFreeze;
    private boolean               mDrawn;
    private int                   mCanvasMode                   = DRAW_HARDWARE;
    private Expression            mFreezeModeExpression;
    private final Object          mObject                       = new Object();
    private static final int      DRAW_HARDWARE                 = 0;
    private static final int      DRAW_BITMAP_ONCE              = 1;
    private static final int      DRAW_BITMAP                   = 2;
    private static final int      DRAW_HARDWARE_AND_BITMAP      = 3;
    private static final int      DRAW_HARDWARE_AND_BITMAP_ONCE = 4;

    public VirtualScreen(Element node, ScreenElementRoot root) throws ScreenElementLoadException{
        super(node, root);
        mFreeze = Boolean.parseBoolean(node.getAttribute(VariableNames.FREEZE));
        if (mFreeze) {
            mFreezeModeExpression = Expression.build(node.getAttribute(VariableNames.FREEZE_MODE));
            mFreezeMode = new IndexedNumberVariable(getName(), VariableNames.FREEZE_MODE, getContext().mVariables);
        }
    }

    public void doRender(Canvas c) {
        switch (mCanvasMode) {
            case DRAW_HARDWARE:
                super.doRender(c);
                break;

            case DRAW_BITMAP_ONCE:
                if (!mDrawn) {
                    super.doRender(c);
                    mScreenCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    super.doRender(mScreenCanvas);
                    mDrawn = true;
                }
                break;
            case DRAW_BITMAP:
                mScreenCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                super.doRender(mScreenCanvas);
                break;
            case DRAW_HARDWARE_AND_BITMAP:
                super.doRender(c);
                mScreenCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                super.doRender(mScreenCanvas);
                break;
            case DRAW_HARDWARE_AND_BITMAP_ONCE:
                if (!mDrawn) {
                    synchronized (mObject) {
                        mScreenCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        super.doRender(mScreenCanvas);
                        mDrawn = true;
                    }
                }
                super.doRender(c);
                break;
            default:
                break;
        }
    }

    public void tick(long currentTime) {
        if (mFreeze) {
            Double freeze = mFreezeMode.get();
            mCanvasMode = freeze == null ? 0 : freeze.intValue();
            if (mFreezeModeExpression != null) {
                mCanvasMode = (int)mFreezeModeExpression.evaluate(getVariables());
            }
            if (mCanvasMode != DRAW_BITMAP_ONCE) {
                super.tick(currentTime);
            }
        } else {
            mCanvasMode = DRAW_HARDWARE;
            super.tick(currentTime);
        }
    }

    public void reset(long time) {
        super.reset(time);
        mCanvasMode = DRAW_HARDWARE;
        mDrawn = false;
    }

    public void pause() {
        super.pause();
        if (mFreezeMode != null) {
            mFreezeMode.set(DRAW_HARDWARE);
        }
    }

    public void finish() {
        mScreenBitmap.recycle();
    }

    public Bitmap getBitmap() {
        switch (mCanvasMode) {
            case DRAW_HARDWARE:
                if (mDrawn) {
                    mDrawn = false;
                    return mScreenBitmap;
                }
                return null;
            case DRAW_HARDWARE_AND_BITMAP_ONCE:
                synchronized (mObject) {
                    return mScreenBitmap;
                }
            case DRAW_BITMAP_ONCE:
            case DRAW_BITMAP:
            case DRAW_HARDWARE_AND_BITMAP:

            default:
                return mScreenBitmap;
        }
    }

    protected void onVisibilityChange(boolean visible) {
        super.onVisibilityChange(visible);
        if (mCanvasMode == DRAW_HARDWARE_AND_BITMAP_ONCE && visible) {
            mDrawn = false;
        }
    }

    public void init() {
        super.init();
        float width = getWidth();
        if (width < 0) {
            width = scale(Utils.getVariableNumber(VariableNames.SCREEN_WIDTH, getVariables()));
        }
        float height = getHeight();
        if (height < 0) {
            height = scale(Utils.getVariableNumber(VariableNames.SCREEN_HEIGHT, getVariables()));
        }
        mScreenBitmap = Bitmap.createBitmap(Math.round(width), Math.round(height), Bitmap.Config.ARGB_8888);
        mScreenBitmap.setDensity(mRoot.getTargetDensity());
        mScreenCanvas = new Canvas(mScreenBitmap);
    }
}
