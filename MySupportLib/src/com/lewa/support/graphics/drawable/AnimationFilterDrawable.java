package com.lewa.support.graphics.drawable;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.Gravity;

import com.lewa.support.graphics.BitmapFilter;

/**
 * Live drawable that perform filtering continuing on Drawable.
 * <p/>
 * Use it with SnapshotDrawable and AnimationDrawable, the other types of Drawable only show still image.
 * <p/>
 * <b>Notice:</b> The view that uses this Drawable should has hardware layer type, or the blending is wrong and the View
 * shows flickering graphics.
 */
public class AnimationFilterDrawable extends Drawable implements Animatable, Drawable.Callback
{
    private BitmapFilter mBitmapFilter;

    private Drawable mSourceDrawable;

    private Bitmap[] mFilterBitmaps = new Bitmap[2];
    private int mUpdatingIndex = 0;

    private Rect mDstRect = new Rect();

    private long mStartTimeMillis;
    private long mDuration = 1;

    private Paint mPaint = new Paint();
    private float mGlobalAlphaScale = 1.f;
    private boolean mHasAlpha;
    private boolean mHasColorFilter;
    private int mColorFilterColor;
    private PorterDuff.Mode mColorFilterMode;

    /**
     * Crate an AnimationFilterDrawable from Drawable resource.
     *
     * @see #AnimationFilterDrawable(android.graphics.drawable.Drawable, lewa.graphics.BitmapFilter)
     */
    public AnimationFilterDrawable(Resources resources, int id, BitmapFilter bitmapFilter) {
        this(resources.getDrawable(id), bitmapFilter);
    }

    /**
     * Create an AnimationFilterDrawable with the source Drawable.
     * <p/>
     * If the drawable is instance of AnimationDrawable or SnapshotDrawable, the drawable is live; or, it draws still
     * image.
     */
    public AnimationFilterDrawable(Drawable drawable, BitmapFilter bitmapFilter) {
        if (drawable == null || bitmapFilter == null) {
            throw new NullPointerException();
        }

        mBitmapFilter = bitmapFilter;

        mSourceDrawable = drawable;
        mSourceDrawable.setCallback(this); // Get source drawable update event.

        if (!(mSourceDrawable instanceof Animatable)) {
            scheduleDrawable(mSourceDrawable, null, SystemClock.uptimeMillis());
        }

        if (mSourceDrawable instanceof SnapshotDrawable) {
            ((SnapshotDrawable) mSourceDrawable).setSamplerSize(mBitmapFilter.getDownsizeSampleSize());
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        super.setVisible(visible, restart);
        return mSourceDrawable.setVisible(visible, restart); // AnimationDrawable will auto run when visible.
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        mSourceDrawable.setBounds(left, top, right, bottom);
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        mSourceDrawable.setBounds(bounds);
    }

    /**
     * Start the source and filter Drawable.
     * <p/>
     * Ignore when the source Drawable can't be started.
     */
    @Override
    public void start() {
        if (mSourceDrawable instanceof Animatable) {
            ((Animatable) mSourceDrawable).start();
        }
    }

    /**
     * Stop the source and filter Drawable.
     * <p/>
     * Ignore when the source Drawable can't be stopped.
     */
    @Override
    public void stop() {
        if (mSourceDrawable instanceof Animatable) {
            ((Animatable) mSourceDrawable).stop();
        }
    }

    /**
     * Indicates whether the source and filter Drawable are currently running or not.
     *
     * @return Return False when the source Drawable is not be Animatable.
     */
    @Override
    public boolean isRunning() {
        if (mSourceDrawable instanceof Animatable) {
            return ((Animatable) mSourceDrawable).isRunning();
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isVisible()) {
            return;
        }

        Gravity.apply(Gravity.FILL, mSourceDrawable.getIntrinsicWidth(), mSourceDrawable.getIntrinsicHeight(),
                getBounds(), mDstRect);

        float normalized = (SystemClock.uptimeMillis() - mStartTimeMillis) / (float) mDuration;
        normalized = Math.min(normalized, 1.0f);
        int alpha = (int) (255 * normalized);

        if (mFilterBitmaps[mUpdatingIndex] != null) {
            mPaint.setAlpha((int) ((255 - alpha) * mGlobalAlphaScale));
            mPaint.setXfermode(null);
            canvas.drawBitmap(mFilterBitmaps[mUpdatingIndex], null, mDstRect, mPaint);
        } else {
            // Has only one Bitmap
            alpha = 255;
        }

        if (mFilterBitmaps[1 - mUpdatingIndex] != null) {
            mPaint.setAlpha((int) (alpha * mGlobalAlphaScale));
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            canvas.drawBitmap(mFilterBitmaps[1 - mUpdatingIndex], null, mDstRect, mPaint);
        }

        if (mHasColorFilter) {
            canvas.drawColor(mColorFilterColor, mColorFilterMode);
        }

        if (isRunning())
            invalidateSelf();
    }

    @Override
    public void setAlpha(int i) {
        mGlobalAlphaScale = i / 255.f;
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
        return mSourceDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mSourceDrawable.getIntrinsicHeight();
    }

    /**
     * This method exists for implementation purpose only and should not be called directly.
     * Transform source Drawable's invalidate request to this Drawable's Callback.
     */
    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    /**
     * This method exists for implementation purpose only and should not be called directly.
     * Transform source Drawable's schedule request to this Drawable's Callback. And, get source Bitmap form the source
     * Drawable.
     */
    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (who instanceof SnapshotDrawable) {
            mDuration = ((SnapshotDrawable) who).getSnapshotInterval();
            Drawable currentDrawable = who.getCurrent();
            Bitmap sourceBitmap = ((BitmapDrawable) currentDrawable).getBitmap();
            new FilterBitmapTask(true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sourceBitmap);
        } else if (who instanceof AnimationDrawable) {
            mDuration = ((AnimationDrawable) who).getDuration(0);
            Drawable currentDrawable = who.getCurrent();
            Bitmap sourceBitmap = ((BitmapDrawable) currentDrawable).getBitmap();
            new FilterBitmapTask(false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sourceBitmap);
        } else {
            Drawable currentDrawable = who.getCurrent();
            if (currentDrawable instanceof BitmapDrawable) {
                Bitmap sourceBitmap = ((BitmapDrawable) currentDrawable).getBitmap();
                new FilterBitmapTask(false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sourceBitmap);
            } else {
                Bitmap sourceBitmap = Bitmap.createBitmap(currentDrawable.getIntrinsicWidth(),
                        currentDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(sourceBitmap);
                currentDrawable.draw(canvas);
                new FilterBitmapTask(true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sourceBitmap);
            }
        }

        if (getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
    }

    /**
     * This method exists for implementation purpose only and should not be called directly.
     * Transform source Drawable's unschedule request to this Drawable's Callback.
     */
    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
    }

    private final class FilterBitmapTask extends AsyncTask<Bitmap, Integer, Bitmap>
    {
        private boolean mRecycleSourceBitmap = false;

        public FilterBitmapTask(boolean recycleSourceBitmap) {
            mRecycleSourceBitmap = recycleSourceBitmap;
        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            Bitmap sourceBitmap = bitmaps[0];
            Bitmap filteredBitmap = mBitmapFilter.filterBitmap(sourceBitmap, mBitmapFilter.getDownsizeSampleSize());

            if (mRecycleSourceBitmap) {
                sourceBitmap.recycle();
            }

            return filteredBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mHasAlpha = bitmap.hasAlpha();

            // Recycle old Bitmap.
            if (mFilterBitmaps[mUpdatingIndex] != null) {
                mFilterBitmaps[mUpdatingIndex].recycle();
            }

            // Swap Bitmap.
            mFilterBitmaps[mUpdatingIndex] = bitmap;
            mUpdatingIndex = 1 - mUpdatingIndex;

            mStartTimeMillis = SystemClock.uptimeMillis();

            invalidateSelf();
        }
    }
}
