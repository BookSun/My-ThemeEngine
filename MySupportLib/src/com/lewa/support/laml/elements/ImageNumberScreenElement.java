
package com.lewa.support.laml.elements;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import com.lewa.support.laml.*;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

public class ImageNumberScreenElement extends ImageScreenElement {

    public static final String TAG_NAME = "ImageNumber";

    private String LOG_TAG = "ImageNumberScreenElement";

    private int mBmpHeight;

    private int mBmpWidth;

    private Bitmap mCachedBmp;

    private Canvas mCachedCanvas;

    private Expression mNumExpression;

    private int mPreNumber = 0x80000000;

    public ImageNumberScreenElement(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, root);
        load(node);
    }

    private Bitmap getNumberBitmap(char c) {
        String name = Utils.addFileNameSuffix(mAni.getSrc(), String.valueOf(c));
        return getContext().mResourceManager.getBitmap(name);
    }

    private Bitmap recreateBitmapIfNeeded(int width, int height, int density) {
        Bitmap bitmap = null;
        if (mCachedBmp != null && width <= mCachedBmp.getWidth()) {
            int i = mCachedBmp.getHeight();
            if (height <= i)
                return bitmap;
        }
        if (mCachedBmp != null) {
            if (width <= mCachedBmp.getWidth())
                width = mCachedBmp.getWidth();
            if (height <= mCachedBmp.getHeight())
                height = mCachedBmp.getHeight();
        }
        mBmpHeight = height;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCachedBmp = bitmap;
        mCachedBmp.setDensity(density);
        mCachedCanvas = new Canvas(bitmap);
        return bitmap;
    }

    protected Bitmap getBitmap() {
        int number = (int) evaluate(mNumExpression);
        if (number != mPreNumber) {
            mPreNumber = number;
            mBmpWidth = 0;
            String numStr = String.valueOf(number);
            Bitmap bmp0 = getNumberBitmap(numStr.charAt(0));
            int width = bmp0.getWidth() * numStr.length();
            int height = bmp0.getHeight();
            recreateBitmapIfNeeded(width, height, bmp0.getDensity());
            mCachedBmp.eraseColor(0);
            for (int i = 0; i < numStr.length(); i++) {
                Bitmap bmp = getNumberBitmap(numStr.charAt(i));
                if (bmp == null) {
                    Log.e(LOG_TAG, "Fail to get bitmap for number " + numStr.charAt(i));
                    continue;
                }
                int j = mBmpWidth + bmp.getWidth();
                int k = bmp.getHeight();
                Bitmap oldBmp = mCachedBmp;
                if (recreateBitmapIfNeeded(j, k, bmp.getDensity()) != null)
                    mCachedCanvas.drawBitmap(oldBmp, 0.0F, 0.0F, null);
                mCachedCanvas.drawBitmap(bmp, mBmpWidth, 0.0F, null);
                mBmpWidth = mBmpWidth + bmp.getWidth();
            }

        }
        return mCachedBmp;
    }

    protected int getBitmapHeight() {
        return mBmpHeight;
    }

    protected int getBitmapWidth() {
        return mBmpWidth;
    }

    public void load(Element node) throws ScreenElementLoadException {
        super.load(node);
        mNumExpression = Expression.build(node.getAttribute("number"));
    }
}
