package com.lewa.support.graphics.drawable;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

public class BlurTargetView extends FrameLayout {
    private static final String TAG = "BlurTargetView";

    private final float mDensity;

    private int mSamplerSize = 8;
    private float mCanvasScale = 1.f / mSamplerSize;
    private int mMinSnapshotInterval = 50;

    private boolean mIsEnableBlur = false;

    private Canvas mSnapshotCanvas;
    private Bitmap mSnapshot;
    private int mSnapshotWidth;
    private int mSnapshotHeight;
    private Drawable mBackgroundDrawable;
    private Bitmap mWallpaper;
    private Rect mBlurRect;

    private WallpaperManager mWallpaperManager;

    private long mLastSnapshotMillis;

    private WeakReference<BlurListener> mListenerRef;
    private boolean isInit;
    private int width;
    private int height;

//    private class WallpaperChangeListener extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (Intent.ACTION_WALLPAPER_CHANGED.equals(intent.getAction())) {
//                Log.d(TAG, "" + intent);
//                if (mBackgroundDrawable == null ||
//                        (mBackgroundDrawable instanceof ColorDrawable &&
//                                ((ColorDrawable) mBackgroundDrawable).getColor() == Color.TRANSPARENT)) {
//                    mWallpaperManager = WallpaperManager.getInstance(context);
//                    Drawable drawable = mWallpaperManager.getDrawable();
//                    if (drawable != null) {
//                        mWallpaper = ((BitmapDrawable) drawable).getBitmap();
//                    }
//                    mWallpaperManager.forgetLoadedWallpaper();
//                }
//            }
//        }
//    }
//
//    private WallpaperChangeListener mWallpaperChangeListener = new WallpaperChangeListener();

    public interface BlurListener {
        public void onTargetUpdate(WeakReference<Bitmap> bitmap, boolean bk);
        public void onLayoutChange(int left, int top, int right, int bottom);
    }

    public BlurTargetView(Context context) {
        this(context, null);
    }

    public BlurTargetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlurTargetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mDensity = context.getResources().getDisplayMetrics().density;
        // Log.d(TAG, "density " + mDensity);
    }

//    @Override
//    protected void onAttachedToWindow() {
//        super.onAttachedToWindow();
//        getContext().registerReceiver(mWallpaperChangeListener, new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED));
//    }
//
//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//        getContext().unregisterReceiver(mWallpaperChangeListener);
//    }
//
//    private void initBackgroundDrawable() {
//        Context context = getContext();
//        if (mBackgroundDrawable == null && context instanceof Activity) {
//            Activity activity = (Activity) context;
//            mBackgroundDrawable = activity.getWindow().getDecorView().getBackground();
//        }
//
//        if (mBackgroundDrawable == null ||
//                (mBackgroundDrawable instanceof ColorDrawable &&
//                        ((ColorDrawable) mBackgroundDrawable).getColor() == Color.TRANSPARENT)) {
//            if (mWallpaper == null) {
//                mWallpaperManager = WallpaperManager.getInstance(context);
//                Drawable drawable = mWallpaperManager.getDrawable();
//                Log.d(TAG, "drawable " + drawable);
//                if (drawable != null) {
//                    mWallpaper = ((BitmapDrawable) drawable).getBitmap();
//                }
//                mWallpaperManager.forgetLoadedWallpaper();
//            }
//        }
//
//        if (mWallpaper != null) {
//            mSamplerSize = 16;
//            mCanvasScale = 1.0f / mSamplerSize;
//            mMinSnapshotInterval = 2000;
//        } else {
//            mSamplerSize = 8;
//            mCanvasScale = 1.0f / mSamplerSize;
//            mMinSnapshotInterval = 50;
//        }
//
//        mSnapshotWidth = (int) (width * mCanvasScale);
//        mSnapshotHeight = (int) (height * mCanvasScale);
//
//        mSnapshotWidth = mSnapshotWidth < 3 ? 3 : mSnapshotWidth;
//        mSnapshotHeight = mSnapshotHeight < 3 ? 3 : mSnapshotHeight;
//    }

    public void addBlurListener(BlurListener listener) {
        mListenerRef = new WeakReference<BlurListener>(listener);
    }

    public void setEnableBlur(boolean enableBlur) {
//        synchronized (this) {
//            if (mIsEnableBlur == enableBlur) {
//                return;
//            }
//
//            if (enableBlur) {
//                mIsEnableBlur = true;
//                snapshot();
//            } else {
//                mIsEnableBlur = false;
//                destroySnapshotCanvas();
//            }
//        }
    }

    public boolean isBlurEnabled() {
        return mIsEnableBlur;
    }

    public void setBlurRect(Rect rect) {
//        mBlurRect = rect;
//        width = mBlurRect.width();
//        height = mBlurRect.height();
//        snapshot();
    }

    public Rect getBlurRect() {
        return mBlurRect;
    }

//    @Override
//    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        super.onLayout(changed, left, top, right, bottom);
//        if (changed) {
//            if (mBlurRect == null) {
//                width = (right - left);
//                height = (bottom - top);
//                snapshot();
//            }
//
//            if (mListenerRef != null) {
//                BlurListener listener = mListenerRef.get();
//                if (listener != null) {
//                    listener.onLayoutChange(left, top, right, bottom);
//                }
//            }
//
//            snapshot();
//        }
//    }
//
//    @Override
//    protected void dispatchGetDisplayList() {
//        super.dispatchGetDisplayList();
//        snapshot();
//    }
//
//    @Override
//    protected void dispatchDraw(Canvas canvas) {
//        super.dispatchDraw(canvas);
//        snapshot();
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        snapshot();
//    }
//
//    private void createSnapshotCanvas() {
//        synchronized (this) {
//            if (mIsEnableBlur && mSnapshotWidth > 0 && mSnapshotHeight > 0) {
//                if (mSnapshot == null || mSnapshot.getWidth() != mSnapshotWidth || mSnapshot.getHeight() != mSnapshotHeight) {
//                    mSnapshot = Bitmap.createBitmap(mSnapshotWidth, mSnapshotHeight, Bitmap.Config.ARGB_8888);
//                    mSnapshotCanvas = new Canvas(mSnapshot);
//                }
//            }
//        }
//    }
//
//    private void destroySnapshotCanvas() {
//        synchronized (this) {
//            if (!mIsEnableBlur) {
//                mWallpaper = null;
//                mSnapshot = null;
//                mSnapshotCanvas = null;
//                mBackgroundDrawable = null;
//            }
//        }
//    }
//
//    private void snapshot() {
//        synchronized (this) {
//            if (!mIsEnableBlur) {
//                return;
//            }
//
//            long currentSnapshotMillis = SystemClock.uptimeMillis();
//            if ((currentSnapshotMillis - mLastSnapshotMillis) < mMinSnapshotInterval) {
//                return;
//            }
//            mLastSnapshotMillis = currentSnapshotMillis;
//
//            initBackgroundDrawable();
//            createSnapshotCanvas();
//
//            if (mSnapshotCanvas == null) {
//                return;
//            }
//
//            long t = SystemClock.uptimeMillis();
//
//            float scaleX = getScaleX() * mCanvasScale;
//            float scaleY = getScaleY() * mCanvasScale;
//            float translateX = -getScrollX();
//            float translateY = -getScrollY();
//
//            if (mBlurRect != null) {
//                translateX -= mBlurRect.left;
//                translateY -= mBlurRect.top;
//            }
//
//            mSnapshotCanvas.save();
//            mSnapshotCanvas.scale(scaleX, scaleY);
//            mSnapshotCanvas.translate(translateX, translateY);
//
//            if (mWallpaper != null) {
//                WallpaperInfo info = mWallpaperManager.getWallpaperInfo();
//                Log.d(TAG, "" + info);
//                if (info == null) {
//                    int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
//                    int offsetX = (int) ((mWallpaper.getWidth() - screenWidth) * mWallpaperManager.wallpaperOffsetX);
//                    mSnapshotCanvas.drawBitmap(mWallpaper, -offsetX, 0, null);
//                } else {
//                    mSnapshot.eraseColor(Color.WHITE);
//                }
//            } else if (mBackgroundDrawable != null) {
//                mBackgroundDrawable.draw(mSnapshotCanvas);
//                Log.d(TAG, "" + mBackgroundDrawable);
//                if (mBackgroundDrawable instanceof ColorDrawable) {
//                    Log.d(TAG, "" + ((ColorDrawable) mBackgroundDrawable).getColor());
//                    int color = ((ColorDrawable) mBackgroundDrawable).getColor();
//
//                    int a = (color >> 24) & 0xFF;
//                    int r = (color >> 16) & 0xFF;
//                    int g = (color >> 8) & 0xFF;
//                    int b = color & 0xFF;
//
//                    Log.d(TAG, a + "; " + r + "; " + g + "; " + b);
//                }
//                draw(mSnapshotCanvas);
//            } else {
//                mSnapshot.eraseColor(Color.WHITE);
//                draw(mSnapshotCanvas);
//            }
//
//            mSnapshotCanvas.restore();
//
//
//            Log.d(TAG, "snapshot view cost " + "(" +
//                    mSnapshotCanvas.getWidth() + "; " + mSnapshotCanvas.getHeight() + ")" +
//                    (SystemClock.uptimeMillis() - t) + " ms");
//
//            if (mListenerRef != null) {
//                BlurListener listener = mListenerRef.get();
//                if (listener != null) {
//                    // The last pixel in each row is incorrect, we remove it;
//                    Bitmap snapshot2 = mSnapshot;
//                    if (mSnapshot.getWidth() > 3 && mSnapshot.getHeight() > 3) {
//                        snapshot2 = Bitmap.createBitmap(mSnapshot, 1, 1, mSnapshot.getWidth() - 1, mSnapshot.getHeight() - 1);
//                    }
//
//                    // For image has alpha channel
//                    int p = snapshot2.getPixel(0, 0);
//                    int a = (p >> 24) & 0xFF;
//                    if (a != 255) {
//                        snapshot2.eraseColor(Color.WHITE);
//                    }
//
//                    listener.onTargetUpdate(new WeakReference<Bitmap>(snapshot2), mWallpaper != null);
//                }
//            }
//        }
//    }
}
