
package com.lewa.support.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lewa.support.laml.RenderThread;
import com.lewa.support.laml.RendererController;
import com.lewa.support.laml.RendererCore;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.RendererController.IRenderable;

public class FancyDrawable extends Drawable implements RendererController.IRenderable {
    private static final boolean DBG = false;

    private static final boolean SELF_WAKEUP = false;

    private static final String LOG_TAG = "FancyDrawable";

    private static final int RENDER_TIMEOUT = 100;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private RendererCore mRendererCore;

    private int mWidth;

    private int mHeight;

    private float mScale;

    private int mIntrinsicHeight;

    private int mIntrinsicWidth;

    private Object mPauseLock = new Object();

    private boolean mPaused = true;

    private Runnable mRenderTimeout = new Runnable() {
        public void run() {
            doPause();
        }
    };

    private Runnable mInvalidateSelf = new Runnable() {
        public void run() {
            invalidateSelf();
        }
    };

    public FancyDrawable(RendererCore rc) {
        init(rc);
    }

    public FancyDrawable(ScreenElementRoot root, RenderThread t) {
        init(root, t);
    }

    private void doPause() {
        if (mPaused)
            return;
        synchronized (mPauseLock) {
            if (!mPaused) {
                if (DBG)
                    logd("doPause: ");
                mPaused = true;
                mRendererCore.pauseRenderable(this);
            }
        }
    }

    private void doResume() {
        if (!mPaused)
            return;
        synchronized (mPauseLock) {
            if (mPaused) {
                if (DBG)
                    logd("doResume: ");
                mPaused = false;
                mRendererCore.resumeRenderable(this);
            }
        }
    }

    public void doRender() {
        if (SELF_WAKEUP) {
            mHandler.removeCallbacks(mRenderTimeout);
            mHandler.postDelayed(mRenderTimeout, RENDER_TIMEOUT);
        }
        mHandler.post(mInvalidateSelf);
    }

    public static FancyDrawable fromZipFile(Context context, String path) {
        return fromZipFile(context, path, RenderThread.globalThread(true));
    }

    public static FancyDrawable fromZipFile(Context context, String path, RenderThread t) {
        RendererCore rc = RendererCore.createFromZipFile(context, path, t);
        return rc == null ? null : new FancyDrawable(rc);
    }

    private void init(RendererCore rc) {
        if (rc == null)
            throw new NullPointerException();

        mRendererCore = rc;
        mRendererCore.addRenderable(this);
        setIntrinsicSize((int) mRendererCore.getRoot().getWidth(), (int) mRendererCore.getRoot()
                .getHeight());
        mScale = mRendererCore.getRoot().getScale();
    }

    private void init(ScreenElementRoot root, RenderThread t) {
        if (DBG)
            logd("init  root:" + root.toString());
        init(new RendererCore(root, t));
    }

    private void logd(CharSequence info) {
        Log.d(LOG_TAG, info + "  [" + toString() + "]");
    }

    public void cleanUp() {
        if (DBG)
            logd("cleanUp: ");
        mRendererCore.removeRenderable(this);
    }

    public void draw(Canvas canvas) {
        mHandler.removeCallbacks(mRenderTimeout);
        doResume();
        try {
            int sa = canvas.save();
            Rect rect = getBounds();
            if (rect.left != 0 && rect.top != 0) {
                float div = 2 * mScale;
                canvas.translate(rect.left, rect.top);
                canvas.scale((float) mWidth / mIntrinsicWidth, (float) mHeight / mIntrinsicHeight,
                        (float) (mWidth - mIntrinsicWidth) / div,
                        (float) (mHeight - mIntrinsicHeight) / div);
            } else {
                canvas.scale((float) mWidth / mIntrinsicWidth, (float) mHeight / mIntrinsicHeight);
            }
            mRendererCore.render(canvas);
            canvas.restoreToCount(sa);
            if (DBG)
                logd("draw:");
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public synchronized Drawable getCurrent() {
        mHandler.removeCallbacks(mRenderTimeout);
        doResume();
        if (mWidth < 1 || mHeight < 1) {
            mWidth = IconCustomizer.sCustomizedIconWidth;
            mHeight = IconCustomizer.sCustomizedIconHeight;
        }
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mRendererCore.render(canvas);
        return new BitmapDrawable(bitmap);
    }

    protected void finalize() throws Throwable {
        cleanUp();
        super.finalize();
    }

    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setAlpha(int alpha) {
    }

    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        mWidth = (right - left);
        mHeight = (bottom - top);
    }

    public void setColorFilter(ColorFilter cf) {
    }

    public void setIntrinsicSize(int width, int height) {
        mIntrinsicWidth = width;
        mIntrinsicHeight = height;
    }
}
