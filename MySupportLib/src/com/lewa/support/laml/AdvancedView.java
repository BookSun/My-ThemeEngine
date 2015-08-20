
package com.lewa.support.laml;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.lewa.support.laml.util.Utils;

public class AdvancedView extends View implements RendererController.IRenderable {
    private static final String LOG_TAG = "AdvancedView";

    private static final String VARIABLE_VIEW_HEIGHT = "view_height";

    private static final String VARIABLE_VIEW_WIDTH = "view_width";

    private SingleRootListener mListener;

    private boolean mLoggedHardwareRender;

    private boolean mNeedDisallowInterceptTouchEvent;

    protected RendererController mRendererController;

    protected ScreenElementRoot mRoot;

    private RenderThread mThread;

    private boolean mUseExternalRenderThread;

    private boolean mPaused = true;

    public AdvancedView(Context context, ScreenElementRoot root) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        mRoot = root;
        mListener = new SingleRootListener(mRoot, this);
        mRendererController = new RendererController(mListener);
        mRoot.setRenderController(mRendererController);
    }

    public AdvancedView(Context context, ScreenElementRoot root, RenderThread t) {
        this(context, root);
        if (t != null) {
            mRendererController.init();
            mUseExternalRenderThread = true;
            mThread = t;
            mThread.addRendererController(mRendererController);
        }
    }

    public void cleanUp() {
        cleanUp(false);
    }

    public void cleanUp(boolean keepResource) {
        mRoot.setKeepResource(keepResource);
        setOnTouchListener(null);
        mRoot.setRenderController(null);
        if (mThread != null) {
            if (!mUseExternalRenderThread)
                mThread.setStop();
            mThread.removeRendererController(mRendererController);
            mRendererController.finish();
        }
    }

    public void doRender() {
        postInvalidate();
    }

    public final ScreenElementRoot getRoot() {
        return mRoot;
    }

    protected int getSuggestedMinimumHeight() {
        return (int) mRoot.getHeight();
    }

    protected int getSuggestedMinimumWidth() {
        return (int) mRoot.getWidth();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mUseExternalRenderThread && mThread == null) {
            mThread = new RenderThread(mRendererController);
            mThread.setPaused(mPaused);
            mThread.start();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mThread == null || !mThread.isStarted()) {
            return;
        }
        if (!mLoggedHardwareRender) {
            Log.d(LOG_TAG, "canvas hardware render: " + canvas.isHardwareAccelerated());
            mLoggedHardwareRender = true;
        }

        try {
            mRoot.render(canvas);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Utils.putVariableNumber(VARIABLE_VIEW_WIDTH, mRoot.getContext().mVariables,
                (double) (right - left) / mRoot.getScale());
        Utils.putVariableNumber(VARIABLE_VIEW_HEIGHT, mRoot.getContext().mVariables,
                (double) (bottom - top) / mRoot.getScale());
    }

    public void onPause() {
        mPaused = true;
        if (mThread != null) {
            if (!mUseExternalRenderThread)
                mThread.setPaused(true);
            mRendererController.selfPause();
        }
    }

    public void onResume() {
        mPaused = false;
        if (mThread != null) {
            if (!mUseExternalRenderThread)
                mThread.setPaused(false);
            mRendererController.selfResume();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (mRoot != null) {
            if (event.getActionIndex() > 0) {
                Log.d(LOG_TAG, "touch point index > 1, ignore");
                return false;
            }

            boolean b = mRoot.needDisallowInterceptTouchEvent();
            if (mNeedDisallowInterceptTouchEvent != b) {
                getParent().requestDisallowInterceptTouchEvent(b);
                mNeedDisallowInterceptTouchEvent = b;
            }

            mRendererController.postMessage(MotionEvent.obtain(event));
            return true;
        }
        return false;
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            onResume();
        } else if (visibility == View.INVISIBLE || visibility == View.GONE) {
            onPause();
        }
    }
}
