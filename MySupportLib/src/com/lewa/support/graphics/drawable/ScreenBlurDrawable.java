package com.lewa.support.graphics.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import com.lewa.support.graphics.Snapshot;
import com.lewa.support.util.ImageUtils;

public class ScreenBlurDrawable extends Drawable implements Runnable, Animatable {
    private static final String TAG = "ScreenBlurDrawable";
    private static final int SNAPSHOT_INTERVAL = -1;

    private final Paint mPaint;

    private Snapshot mSnapshot;

    private Rect mDstRect = new Rect();

    private int mMinLayer;
    private int mMaxLayer;

    private boolean mIsRunning = false;
    private int mBlurInterval = SNAPSHOT_INTERVAL;
    private long mBlurTransitionTime;
    private Bitmap[] mBlurredBitmaps = new Bitmap[2];
    private int mUpdatingIndex = 0;

    private int mWidth = -1;
    private int mHeight = -1;

    private long mStartTimeMillis;

    private float mGlobalAlphaScale = 1.f;
    private int mColorFilterColor;
    private PorterDuff.Mode mColorFilterMode;
    private boolean mHasColorFilter;

    public ScreenBlurDrawable(Context context) {
        this(context, 0, 0);
    }

    public ScreenBlurDrawable(Context context, int minLayer, int maxLayer) {
        mSnapshot = new Snapshot();
        mMinLayer = minLayer;
        mMaxLayer = maxLayer;
        mWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        mHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        mSnapshot.setSampleSize(16);
        mPaint = new Paint();
        mPaint.setFilterBitmap(true);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            start();
        } else {
            stop();
        }
        return changed;
    }

    @Override
    public void draw(Canvas canvas) {
        Gravity.apply(Gravity.FILL, mWidth, mHeight, getBounds(), mDstRect);

        boolean done = true;
        int preUpdatedIndex = 1- mUpdatingIndex;

        if (mBlurredBitmaps[preUpdatedIndex] == null) {
            // Nothing to draw
            return;
        }

        if (mBlurredBitmaps[mUpdatingIndex] == null) {
            // Transition to blurred bitmap
            float normalized = (SystemClock.uptimeMillis() - mStartTimeMillis) / 100.f;
            normalized = Math.min(normalized, 1.0f);
            done = normalized >= 1.0f;
            int alpha = (int) (255 * normalized);

            mPaint.setAlpha((int) (alpha * mGlobalAlphaScale));
            mPaint.setXfermode(null);
            canvas.drawBitmap(mBlurredBitmaps[preUpdatedIndex], null, mDstRect, mPaint);
        } else {
            // Transition between
            float normalized = (SystemClock.uptimeMillis() - mStartTimeMillis) / (float) mBlurTransitionTime;
            normalized = Math.min(normalized, 1.0f);
            done = normalized >= 1.0f;
            int alpha = (int) (255 * normalized);

            mPaint.setAlpha((int) ((255 - alpha) * mGlobalAlphaScale));
            mPaint.setXfermode(null);
            canvas.drawBitmap(mBlurredBitmaps[mUpdatingIndex], null, mDstRect, mPaint);

            mPaint.setAlpha((int) (alpha * mGlobalAlphaScale));
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            canvas.drawBitmap(mBlurredBitmaps[preUpdatedIndex], null, mDstRect, mPaint);
        }

        if (mHasColorFilter) {
            canvas.drawColor(mColorFilterColor, mColorFilterMode);
        }

        if (!done)
            invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        mGlobalAlphaScale = alpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public void setColorFilter(int color, PorterDuff.Mode mode) {
        mColorFilterColor = color;
        mColorFilterMode = mode;
        mHasColorFilter = true;
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
            mBlurredBitmaps[0] = null;
            mBlurredBitmaps[1] = null;
            run();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            mIsRunning = false;
            unscheduleSelf(this);
            mBlurredBitmaps[0] = null;
            mBlurredBitmaps[1] = null;
        }
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void run() {
        new SnapshotTask().execute();
    }

    @Override
    public Drawable getCurrent() {
        return null;
    }

    public void setBlurInterval(int millis) {
        mBlurInterval = millis;
    }

    public int getBlurInterval() {
        return mBlurInterval;
    }

    private class SnapshotTask extends AsyncTask<Void, Integer, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... voids) {
            long t = SystemClock.uptimeMillis();

            Bitmap snapshot = mSnapshot.snapshotScreen(mMinLayer, mMaxLayer);
            if (snapshot == null || snapshot.getWidth() < 3 || snapshot.getHeight() < 3) {
                snapshot = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                snapshot.eraseColor(Color.WHITE);
                return snapshot;
            }

            // The last pixel in each row is incorrect, we remove it;
            Bitmap snapshot2 = Bitmap.createBitmap(snapshot, 1, 1, snapshot.getWidth() - 1, snapshot.getHeight() - 1);
            Bitmap blurred = Bitmap.createBitmap(snapshot2.getWidth(), snapshot2.getHeight(), Bitmap.Config.ARGB_8888);

            ImageUtils.fastBlur(snapshot2, blurred, 8);

            Log.d(TAG, "snapshot screen & blur image with size(" + mWidth + ", " + mHeight + ")" + " cost " +
                    (SystemClock.uptimeMillis() - t) + " ms");

            return blurred;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                return;
            }

            long updateTimeMillis = SystemClock.uptimeMillis();
            mBlurredBitmaps[mUpdatingIndex] = bitmap;
            mUpdatingIndex = 1 - mUpdatingIndex;
            mBlurTransitionTime = updateTimeMillis - mStartTimeMillis;
            mStartTimeMillis = updateTimeMillis;

            if (isRunning() && mBlurInterval != -1) {
                scheduleSelf(ScreenBlurDrawable.this, mStartTimeMillis  + mBlurInterval);
            }

            // Call from view's thread;
            Callback cb = getCallback();
            if (cb instanceof View) {
                View cbView = (View) cb;
                cbView.post(new Runnable() {
                    @Override
                    public void run() {
                        invalidateSelf();
                    }
                });
            }
        }
    }
}
