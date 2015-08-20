package com.lewa.support.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;

import com.lewa.support.graphics.Snapshot;

// TODO: Add more api, like snapshot once.

/**
 * A Drawable use screenshot or viewshot as its content.
 * <p>Use {@link #SnapshotDrawable()} or {@link #SnapshotDrawable(int, int)} to create a
 * screenshot Drawable. Use {@link #SnapshotDrawable(android.view.View)} to create a viewshot
 * Drawable.</p>
 * <p>Notice: Screenshot needs system permission. </p>
 */
public class SnapshotDrawable extends Drawable implements Runnable, Animatable {
    private static final int SNAPSHOT_INTERVAL = 50;

    private Snapshot mSnapshot;

    private View mParentView;
    private View mSourceView;
    private int mMinLayer;
    private int mMaxLayer;

    private Bitmap mSnapshotBitmap;
    private Rect mDstRect = new Rect();
    private boolean mIsRunning;
    private boolean mCurrentUsage;
    private int mWidth = -1;
    private int mHeight = -1;
    private int mSnapshotInterval = SNAPSHOT_INTERVAL;

    /**
     * Create a SnapshotDrawable which the content is all the screen layers.
     */
    public SnapshotDrawable() {
        this(0, 0);
    }

    /**
     * Create a SnapshotDrawable which the context is some layers of the screen.
     *
     * @see lewa.graphics.Snapshot#snapshotScreen(int, int)
     */
    public SnapshotDrawable(int minLayer, int maxLayer) {
        mSnapshot = new Snapshot();
        mMinLayer = minLayer;
        mMaxLayer = maxLayer;
        mWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        mHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    /**
     * Create a SnapshotDrawable which uses a view as it's content.
     *
     * @see lewa.graphics.Snapshot#snapshotView(android.view.View)
     */
    public SnapshotDrawable(View view) {
        mSnapshot = new Snapshot();
        mSourceView = view;
        mSourceView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int width = right - left;
                int height = bottom - top;

                if (width != mWidth || height != mHeight) {
                    mWidth = width;
                    mHeight = height;
                    if (mParentView != null) {
                        mParentView.setSelected(mParentView.isSelected()); // Trigger parent view re-get drawable size;
                    }
                }
            }
        });
    }

    /**
     * @see lewa.graphics.Snapshot#setSampleSize(int)
     */
    public void setSamplerSize(int size) {
        mSnapshot.setSampleSize(size);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            if (changed || restart) {
                start();
            }

            if (getCallback() instanceof View && mParentView == null) {
                mParentView = (View) getCallback();
            }
        } else {
            stop();
        }
        return changed;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mSnapshotBitmap == null) {
            return;
        }

        Gravity.apply(Gravity.FILL, mWidth, mHeight, getBounds(), mDstRect);
        canvas.drawBitmap(mSnapshotBitmap, null, mDstRect, null);
    }

    @Override
    public void setAlpha(int i) {
        // TODO
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // TODO
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public void start() {
        if (!isRunning()) {
            mIsRunning = true;
            run();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            mIsRunning = false;
            unscheduleSelf(this);
        }
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void run() {
        new SnapshotTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public Drawable getCurrent() {
        if (mSnapshotBitmap != null) {
            mCurrentUsage = true;
            return new BitmapDrawable(Resources.getSystem(), mSnapshotBitmap);
        } else {
            return null;
        }
    }

    public void setSnapshotInterval(int millis) {
        mSnapshotInterval = millis;
    }

    public int getSnapshotInterval() {
        return mSnapshotInterval;
    }

    private void updateSnapshot(Bitmap bitmap) {
        if (mSnapshotBitmap != null && !mCurrentUsage) {
            mSnapshotBitmap.recycle();
        }

        mSnapshotBitmap = bitmap;
        mCurrentUsage = false;

        if (mIsRunning) {
            scheduleSelf(this, SystemClock.uptimeMillis() + mSnapshotInterval);
        }
        invalidateSelf();
    }

    private class SnapshotTask extends AsyncTask<Void, Integer, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... voids) {
            if (mSourceView != null) {
                return mSnapshot.snapshotView(mSourceView);
            } else {
                return mSnapshot.snapshotScreen(mMinLayer, mMaxLayer);
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            updateSnapshot(bitmap);
        }
    }
}
