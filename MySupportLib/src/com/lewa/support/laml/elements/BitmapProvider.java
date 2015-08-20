
package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.util.SurfaceWrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public abstract class BitmapProvider {
    private static class AppIconProvider extends BitmapProvider {

        public static final String TAG_NAME = "ApplicationIcon";

        public void init(String src) {
            super.init(src);
            if (src != null) {
                String as[] = src.split(",");
                if (as.length == 2) {
                    Context c = mRoot.getContext().mContext;
                    try {
                        Drawable d = c.getPackageManager().getActivityIcon(
                                new ComponentName(as[0], as[1]));
                        if (d instanceof BitmapDrawable) {
                            mBitmap = ((BitmapDrawable) d).getBitmap();
                        } else {
                            int size = c.getResources().getDimensionPixelSize(
                                    android.R.dimen.app_icon_size);
                            mBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(mBitmap);
                            d.draw(canvas);
                            canvas.setBitmap(null);
                        }
                    } catch (NameNotFoundException e) {
                        Log.e(LOG_TAG, "fail to get icon for src of ApplicationIcon type: " + src);
                    }
                } else {
                    Log.e(LOG_TAG, "invalid src of ApplicationIcon type: " + src);
                }
            } else {
                Log.e(LOG_TAG, "invalid src of ApplicationIcon type: " + src);
            }
        }

        public AppIconProvider(ScreenElementRoot root) {
            super(root);
        }
    }

    private static class ResourceImageProvider extends BitmapProvider {

        public static final String TAG_NAME = "ResourceImage";

        private String mCachedBitmapName;

        public void finish() {
            super.finish();
            mCachedBitmapName = null;
            mBitmap = null;
        }

        public Bitmap getBitmap(String src) {
            if (mBitmap != null && mBitmap.isRecycled()
                    || !TextUtils.equals(mCachedBitmapName, src)) {
                mCachedBitmapName = src;
                mBitmap = mRoot.getContext().mResourceManager.getBitmap(src);
            }
            return mBitmap;
        }

        public ResourceImageProvider(ScreenElementRoot root) {
            super(root);
        }
    }

    public static Bitmap sBitmap = null;

    public static int sWidth = -1;

    public static int sHeight = -1;

    @SuppressWarnings("deprecation")
    public static Bitmap takeScreenshot(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int maxLayer = ScreenshotProvider.TYPE_LAYER_MULTIPLIER * ScreenshotProvider.KEYGUARD_LAYER;
        return SurfaceWrapper.screenshot(sWidth > 0 ? sWidth : display.getWidth(), sHeight > 0 ? sHeight
                : display.getHeight(), 0, maxLayer);
    }

    public static void screenshot(Context context) {
        sBitmap = takeScreenshot(context);
        Log.d(LOG_TAG, "take screenshot");
    }

    private static class ScreenshotProvider extends BitmapProvider {

        public static final String TAG_NAME = "Screenshot";

        private static final int KEYGUARD_LAYER = getKeyguardLayer();

        private static final int TYPE_LAYER_MULTIPLIER = 10000;

        public ScreenshotProvider(ScreenElementRoot root) {
            super(root);
        }

        @Override
        public void init(String src) {
            super.init(src);
            sWidth = mRoot.getScreenWidth();
            sHeight = mRoot.getScreenHeight();
        }

        public void reset() {
            super.reset();
            int maxLayer = TYPE_LAYER_MULTIPLIER * KEYGUARD_LAYER;
            mBitmap = sBitmap != null ? sBitmap : SurfaceWrapper.screenshot(mRoot.getScreenWidth(),
                    mRoot.getScreenHeight(), 0, maxLayer);
        }

        public void finish() {
            super.finish();
            sBitmap = null;
        }

        private static int getKeyguardLayer() {
            switch (Build.VERSION.SDK_INT) {
                default:
                case 16:/* Build.VERSION_CODES.JELLY_BEAN */
                    return 11;
                case 17:/* Build.VERSION_CODES.JELLY_BEAN_MR1 */
                case 18:/* Build.VERSION_CODES.JELLY_BEAN_MR2 */
                    return 12;
            }
        }
    }

    private static class VirtualScreenProvider extends BitmapProvider {

        public static final String TAG_NAME = "VirtualScreen";

        private VirtualScreen mVirtualScreen;

        public Bitmap getBitmap(String src) {
            return mVirtualScreen != null ? mVirtualScreen.getBitmap() : null;
        }

        public void init(String src) {
            super.init(src);
            ScreenElement se = mRoot.findElement(src);
            if (se instanceof VirtualScreen) {
                mVirtualScreen = (VirtualScreen) se;
            }
        }

        public VirtualScreenProvider(ScreenElementRoot root) {
            super(root);
        }
    }

    private static final String LOG_TAG = "BitmapProvider";

    protected Bitmap mBitmap;

    protected ScreenElementRoot mRoot;

    public BitmapProvider(ScreenElementRoot root) {
        mRoot = root;
    }

    public static BitmapProvider create(ScreenElementRoot root, String type) {
        if(!TextUtils.isEmpty(type)){
            if (type.equalsIgnoreCase(ResourceImageProvider.TAG_NAME))
                return new ResourceImageProvider(root);
            if (type.equalsIgnoreCase(VirtualScreenProvider.TAG_NAME))
                return new VirtualScreenProvider(root);
            if (type.equalsIgnoreCase(AppIconProvider.TAG_NAME))
                return new AppIconProvider(root);
            if (type.equalsIgnoreCase(ScreenshotProvider.TAG_NAME))
                return new ScreenshotProvider(root);
        }
        return new ResourceImageProvider(root);
    }

    public void finish() {
        mBitmap = null;
    }

    public Bitmap getBitmap(String src) {
        return mBitmap;
    }

    public void init(String src) {
        reset();
    }

    public void reset() {
    }
}
