package com.lewa.support.graphics.drawable;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.Gravity;

import com.lewa.support.graphics.BitmapFilter;

/**
 * A Drawable that filter an image. You can create a FilterDrawable from a Bitmap/Drawable.
 * <p/>
 * <b>Notice:</b> The view that uses this Drawable should has hardware layer type, or the blending is wrong and the View
 * shows flickering graphics.
 */
public class FilterDrawable extends Drawable
{
    private static final String TAG = "FilterDrawable";

    private static final int TRANSITION_STARTING = 0;
    private static final int TRANSITION_RUNNING = 1;
    private static final int TRANSITION_NONE = 2;

    private BitmapFilter mBitmapFilter;

    private Drawable mSourceDrawable;
    private Bitmap mSourceBitmap;
    private Bitmap mFilteredBitmap;
    private final boolean mIsUsingDrawable;

    private int mBitmapWidth;
    private int mBitmapHeight;
    private Rect mDstRect = new Rect();

    private int mFrom;
    private int mTo;
    private int mAlpha = 255;
    private int mDuration;
    private int mTransitionState = TRANSITION_NONE;
    private long mStartTimeMillis;
    private Paint mPaint = new Paint();
    private boolean mHasAlpha;
    private float mGlobalAlphaScale = 1.f;
    private int mColorFilterColor;
    private PorterDuff.Mode mColorFilterMode;
    private boolean mHasColorFilter;

    /**
     * Create a new FilterDrawable with the specified origin Bitmap and the BitmapFilter.
     */
    public FilterDrawable(Bitmap bitmap, BitmapFilter bitmapFilter) {
        this(new BitmapDrawable(Resources.getSystem(), bitmap), bitmapFilter);
    }

    /**
     * Create a new FilterDrawable from a resource.
     */
    public FilterDrawable(Resources res, int id, BitmapFilter bitmapFilter) {
        this(res.getDrawable(id), bitmapFilter);
    }

    /**
     * Create a new FilterDrawable with the specified origin Drawable and the BitmapFilter.
     *
     * <b>Notice:</b> It's only cast a still image form the Drawable. Use
     * {@link lewa.graphics.drawable.AnimationFilterDrawable} to do animation filtering.
     */
    public FilterDrawable(Drawable drawable, BitmapFilter bitmapFilter) {
        if (drawable == null || bitmapFilter == null) {
            throw new NullPointerException();
        }

        mBitmapFilter = bitmapFilter;

        if (drawable.getCurrent() instanceof BitmapDrawable) {
            mIsUsingDrawable = false;
            mSourceBitmap = ((BitmapDrawable) drawable.getCurrent()).getBitmap();
            fromBitmap();
        } else {
            mIsUsingDrawable = true;
            mSourceDrawable = drawable;
            fromDrawable();
        }
    }

    /**
     * Begin transition form source image to filtered image.
     *
     * @param durationMillis The length of the transition in milliseconds.
     */
    public void startTransitionToFiltered(int durationMillis) {
        mFrom = 0;
        mTo = 255;
        mAlpha = 0;
        mDuration = durationMillis;
        mTransitionState = TRANSITION_STARTING;
        invalidateSelf();
    }

    /**
     * Begin transition from filtered image to origin image.
     *
     * @param durationMillis The length of the transition in milliseconds.
     */
    public void startTransitionToOrigin(int durationMillis) {
        mFrom = 255;
        mTo = 0;
        mAlpha = 0;
        mDuration = durationMillis;
        mTransitionState = TRANSITION_STARTING;
        invalidateSelf();
    }

    /**
     * Reset to draw the filtered result.
     */
    public void resetToFiltered() {
        mAlpha = 255;
        mTransitionState = TRANSITION_NONE;
        invalidateSelf();
    }

    /**
     * Reset to draw the origin image.
     */
    public void resetToOrigin() {
        mAlpha = 0;
        mTransitionState = TRANSITION_NONE;
        invalidateSelf();
    }

    /**
     * Set the image filter.
     */
    public void setImageFilter(BitmapFilter bitmapFilter) {
        if (mBitmapFilter != bitmapFilter) {
            mBitmapFilter = bitmapFilter;

            recycleResources();

            if (mIsUsingDrawable) {
                new FilterDrawableTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSourceDrawable);
            } else {
                new FilterBitmapTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSourceBitmap);
            }
        }
    }

    /**
     * Get the current image filter.
     */
    public BitmapFilter getImageFilter() {
        return mBitmapFilter;
    }

    /**
     * Recycle the resources allocated by this Drawable.
     */
    public void recycleResources() {
        if (mIsUsingDrawable) {
            // The source bitmap is created form Drawable.
            mSourceBitmap.recycle();
            mSourceBitmap = null;
        }
        mFilteredBitmap.recycle();
        mFilteredBitmap = null;
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isVisible()) {
            return;
        }

        if (mSourceDrawable == null && mSourceBitmap == null) {
            return;
        }

        Gravity.apply(Gravity.FILL, mBitmapWidth, mBitmapHeight, getBounds(), mDstRect);

        if (mFilteredBitmap == null) {
            if (mIsUsingDrawable) {
                mSourceDrawable.setAlpha((int) (255 * mGlobalAlphaScale));
                mSourceDrawable.draw(canvas);
                mSourceDrawable.setAlpha(255);
            } else {
                mPaint.setAlpha((int) (255 * mGlobalAlphaScale));
                canvas.drawBitmap(mSourceBitmap, null, mDstRect, mPaint);
            }

            if (mHasColorFilter) {
                canvas.drawColor(mColorFilterColor, mColorFilterMode);
            }

            return;
        }

        boolean done = true;

        switch (mTransitionState) {
            case TRANSITION_STARTING:
                mStartTimeMillis = SystemClock.uptimeMillis();
                mAlpha = mFrom;
                done = false;
                mTransitionState = TRANSITION_RUNNING;
                break;
            case TRANSITION_RUNNING:
                if (mStartTimeMillis >= 0) {
                    float normalized = (SystemClock.uptimeMillis() - mStartTimeMillis) / (float) mDuration;
                    done = normalized >= 1.0f;
                    normalized = Math.min(normalized, 1.0f);
                    mAlpha = (int) (mFrom + (mTo - mFrom) * normalized);
                }
                break;
        }

        final int alpha = mAlpha;
        if (done) {
            mTransitionState = TRANSITION_NONE;
            mPaint.setAlpha((int) (255 * mGlobalAlphaScale));
            if (alpha == 0) {
                canvas.drawBitmap(mSourceBitmap, null, mDstRect, mPaint);
            } else if (alpha == 255) {
                canvas.drawBitmap(mFilteredBitmap, null, mDstRect, mPaint);
            }

            if (mHasColorFilter) {
                canvas.drawColor(mColorFilterColor, mColorFilterMode);
            }

            return;
        }

        if (mGlobalAlphaScale != 1.f) {
            mPaint.setAlpha((int) ((255 - alpha) * mGlobalAlphaScale));
            canvas.drawBitmap(mSourceBitmap, null, mDstRect, mPaint);
            mPaint.setAlpha((int) (alpha * mGlobalAlphaScale));
            canvas.drawBitmap(mFilteredBitmap, null, mDstRect, mPaint);
        } else {
            if (mTo > mFrom) {
                canvas.drawBitmap(mSourceBitmap, null, mDstRect, null);
                mPaint.setAlpha(alpha);
                canvas.drawBitmap(mFilteredBitmap, null, mDstRect, mPaint);
            } else {
                canvas.drawBitmap(mFilteredBitmap, null, mDstRect, null);
                mPaint.setAlpha(255 - alpha);
                canvas.drawBitmap(mSourceBitmap, null, mDstRect, mPaint);
            }
        }

        if (mHasColorFilter) {
            canvas.drawColor(mColorFilterColor, mColorFilterMode);
        }

        if (!done) {
            invalidateSelf();
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mGlobalAlphaScale = alpha / 255.f;
    }

    /**
     * Has no effect.
     * Use {@link #setColorFilter(int, android.graphics.PorterDuff.Mode)}
     */
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Nothing to do
    }

    /**
     * Set the color filter to modify the filtered image.
     */
    public void setColorFilter(int src, PorterDuff.Mode mode) {
        mHasColorFilter = true;
        mColorFilterColor = src;
        mColorFilterMode = mode;
    }

    @Override
    public int getOpacity() {
        if (!mHasAlpha) {
            return PixelFormat.OPAQUE;
        } else {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    private void fromBitmap() {
        if (mSourceBitmap != null) {
            mBitmapWidth = mSourceBitmap.getWidth();
            mBitmapHeight = mSourceBitmap.getHeight();
        }

        if (mBitmapFilter != null && mSourceBitmap != null) {
            new FilterBitmapTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSourceBitmap);
        }
    }

    private void fromDrawable() {
        if (mSourceDrawable != null) {
            mBitmapWidth = mSourceDrawable.getIntrinsicWidth();
            mBitmapHeight = mSourceDrawable.getIntrinsicHeight();
        }

        if (mBitmapFilter != null && mSourceDrawable != null) {
            new FilterDrawableTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSourceDrawable);
        }
    }

    private final class FilterBitmapTask extends AsyncTask<Bitmap, Integer, Bitmap>
    {
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            mFilteredBitmap = mBitmapFilter.filterBitmap(bitmaps[0]);
            return mFilteredBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            invalidateSelf();
        }
    }

    private final class FilterDrawableTask extends AsyncTask<Drawable, Integer, Bitmap>
    {
        @Override
        protected Bitmap doInBackground(Drawable... drawables) {
            mSourceBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mSourceBitmap);
            drawables[0].draw(canvas);
            mFilteredBitmap = mBitmapFilter.filterBitmap(mSourceBitmap);
            mHasAlpha = mFilteredBitmap.hasAlpha();
            return mFilteredBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            invalidateSelf();
        }
    }
}
