package lewa.support.v7.lewa.v5;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.Gravity;


import java.lang.ref.WeakReference;

public class ViewBlurDrawable extends Drawable implements BlurTargetView.BlurListener {
    private static final String TAG = "ViewBlurDrawable";

    private static RenderScript mRs;
    private static ScriptIntrinsicBlur script16;
    private static ScriptIntrinsicBlur script8;

    private Rect mBlurRect;
    private Bitmap mBlurredBitmap;

    private int mWidth;
    private int mHeight;

    private Rect mDstRect = new Rect();

    private Paint mPaint = new Paint();

    private Allocation mInput;
    private Allocation mOutput;

    public ViewBlurDrawable(BlurTargetView srcView) {
//        if (mRs == null) {
//            mRs = RenderScript.create(srcView.getContext());
//        }
//
//        if (script16 == null) {
//            script16 = ScriptIntrinsicBlur.create(mRs, Element.U8_4(mRs));
//            script16.setRadius(16);
//        }
//
//        if (script8 == null) {
//            script8 = ScriptIntrinsicBlur.create(mRs, Element.U8_4(mRs));
//            script8.setRadius(8);
//        }
//
//        mBlurRect = srcView.getBlurRect();
//        mPaint.setFilterBitmap(true);
//
//        srcView.addBlurListener(this);
//
//        mWidth = srcView.getWidth();
//        mHeight = srcView.getHeight();

        Log.d(TAG, "create view blur drawable");
    }

//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//
//        if (mInput != null) {
//            mInput.destroy();
//            mInput = null;
//        }
//
//        if (mOutput != null) {
//            mOutput.destroy();
//            mOutput = null;
//        }
//
//        Log.d(TAG, "finalize view blur drawable");
//    }

    @Override
    public void draw(Canvas canvas) {
//        if (mBlurredBitmap == null) {
//            return;
//        }
//
//        Rect rect = mBlurRect != null ? mBlurRect : new Rect(0, 0, mWidth, mHeight);
//        Gravity.apply(Gravity.FILL, mWidth, mHeight, rect, mDstRect);
//        canvas.drawBitmap(mBlurredBitmap, null, mDstRect, mPaint);
        canvas.drawColor(Color.WHITE);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicHeight() {
        return -1;
    }

    @Override
    public int getIntrinsicWidth() {
        return -1;
    }

    @Override
    public void onLayoutChange(int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        if (width != mWidth || height != mHeight) {
            mWidth = width;
            mHeight = height;
        }
    }

    @Override
    public void onTargetUpdate(WeakReference<Bitmap> bitmapRef, boolean bk) {
//        Bitmap bitmap = bitmapRef.get();
//        if (bitmap == null) {
//            return;
//        }
//
//        long t = SystemClock.uptimeMillis();
//
//        if (mBlurredBitmap == null ||
//                mBlurredBitmap.getWidth() != bitmap.getWidth() || mBlurredBitmap.getHeight() != bitmap.getHeight()) {
//            // mBlurredBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//
//            if (mInput != null) {
//                mInput.destroy();
//                mInput = null;
//            }
//            mInput = Allocation.createFromBitmap(mRs, bitmap);
//
//            if (mOutput != null) {
//                mOutput.destroy();
//                mOutput = null;
//            }
//            mOutput = Allocation.createTyped(mRs, mInput.getType());
//        }
//
//        mInput.copyFrom(bitmap);
//        if (bk) {
//            script16.setInput(mInput);
//            script16.forEach(mOutput);
//        } else {
//            script8.setInput(mInput);
//            script8.forEach(mOutput);
//        }
//        mOutput.copyTo(bitmap);
//
//        mBlurredBitmap = bitmap;
//        mBlurredBitmap.setHasAlpha(false);
//
//        Log.d(TAG, "blur image with size(" + bitmap.getWidth() + ", " + bitmap.getHeight() + ")" + " cost " +
//                (SystemClock.uptimeMillis() - t) + " ms");
//
//        invalidateSelf();
    }
}
