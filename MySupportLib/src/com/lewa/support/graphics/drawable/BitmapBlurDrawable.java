package com.lewa.support.graphics.drawable;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;

import com.lewa.support.util.ImageUtils;

public class BitmapBlurDrawable extends Drawable {
    private static final String TAG = "BitmapBlurDrawable";

    private final int mScreenWidth;
    private final int mScreenHeight;

    private Bitmap mBlurredBitmap;
    private int mBitmapWidth;
    private int mBitmapHeight;

    private BlurOptions mOptions;

    private Paint mPaint = new Paint();
    private Rect mDstRect = new Rect();

    public BitmapBlurDrawable(Context context, Bitmap bitmap) {
        this(context, bitmap, new BlurOptions());
    }

    public BitmapBlurDrawable(Context context, Bitmap bitmap, BlurOptions options) {
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;

        mBitmapWidth = bitmap.getWidth();
        mBitmapHeight = bitmap.getHeight();

        mOptions = (options == null ? new BlurOptions() : options);

        blurBitmap(bitmap);
    }

    private void blurBitmap(Bitmap bitmap) {
        float samplerSize;
        int radius;

        switch (mOptions.strength) {
            case BlurOptions.STRENGTH_LOW:
                samplerSize = 8 * mScreenWidth / 1080.0f;
                radius = 8;
                break;
            case BlurOptions.STRENGTH_MODERATE:
                samplerSize = 12 * mScreenWidth / 1080.0f;
                radius = 8;
                break;
            case BlurOptions.STRENGTH_HIGH:
            default:
                samplerSize = 16 * mScreenWidth / 1080.0f;
                radius = 8;
                break;
        }

        int width = (int) (mBitmapWidth / samplerSize);
        int height = (int) (mBitmapHeight / samplerSize);

        width = width < 1 ? 1 : width;
        height = height < 1 ? 1 : height;

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        mBlurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        long t = SystemClock.uptimeMillis();
        ImageUtils.fastBlur(scaledBitmap, mBlurredBitmap, radius);

        Log.d(TAG, "blur image with size(" + mBlurredBitmap.getWidth() + ", " + mBlurredBitmap.getHeight() + ")" + " cost " +
                (SystemClock.uptimeMillis() - t) + " ms");

        mPaint.setFilterBitmap(true);
    }

    @Override
    public void draw(Canvas canvas) {
        Gravity.apply(Gravity.FILL, mBitmapWidth, mBitmapHeight, getBounds(), mDstRect);
        canvas.drawBitmap(mBlurredBitmap, null, mDstRect, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    @Override
    public Drawable getCurrent() {
        return new BitmapDrawable(mBlurredBitmap);
    }

    @Deprecated
    public BitmapDrawable getCurrentDrawable() {
        return new BitmapDrawable(mBlurredBitmap);
    }
}
