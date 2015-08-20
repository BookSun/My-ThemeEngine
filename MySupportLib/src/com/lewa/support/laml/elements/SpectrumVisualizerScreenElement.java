
package com.lewa.support.laml.elements;

import android.graphics.*;
import android.graphics.PorterDuff.Mode;
import android.text.TextUtils;
import android.util.Log;

import com.lewa.support.laml.*;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

public class SpectrumVisualizerScreenElement extends ImageScreenElement {

    public static final String TAG_NAME = "SpectrumVisualizer";

    private int mAlphaWidthNum;

    private Canvas mCanvas;

    private String mDotbar;

    private Bitmap mPanel;

    private String mPanelSrc;

    private int mResDensity;

    private String mShadow;

    private SpectrumVisualizer mSpectrumVisualizer;

    public SpectrumVisualizerScreenElement(Element ele, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(ele, root);
        mPanelSrc = ele.getAttribute("panelSrc");
        mDotbar = ele.getAttribute("dotbarSrc");
        mShadow = ele.getAttribute("shadowSrc");
        mSpectrumVisualizer = new SpectrumVisualizer(getContext().mContext);
        mSpectrumVisualizer.setSoftDrawEnabled(false);
        mSpectrumVisualizer.enableUpdate(false);
        mAlphaWidthNum = Utils.getAttrAsInt(ele, "alphaWidthNum", -1);
    }

    public void doRender(Canvas c) {
        if (mPanel != null) {
            mPaint.setAlpha(getAlpha());
            c.drawBitmap(mPanel, getLeft(), getTop(), mPaint);
        }
        super.doRender(c);
    }

    public void enableUpdate(boolean b) {
        mSpectrumVisualizer.enableUpdate(b);
    }

    protected Bitmap getBitmap() {
        if (mCanvas == null) {
            return null;
        } else {
            mCanvas.drawColor(0, Mode.CLEAR);
            mCanvas.setDensity(0);
            mSpectrumVisualizer.draw(mCanvas);
            mCanvas.setDensity(mResDensity);
            return mBitmap;
        }
    }

    public void init() {
        super.init();
        mPanel = TextUtils.isEmpty(mPanelSrc) ? null : getContext().mResourceManager.getBitmap(mPanelSrc);
        Bitmap dotbar = TextUtils.isEmpty(mDotbar) ? null : getContext().mResourceManager.getBitmap(mDotbar);
        Bitmap shadow = TextUtils.isEmpty(mShadow) ? null : getContext().mResourceManager.getBitmap(mShadow);
        int width = (int) getWidth();
        int height = (int) getHeight();
        if (width <= 0 || height <= 0) {
            if (mPanel == null) {
                Log.e("SpectrumVisualizerScreenElement", "no panel or size");
                return;
            }
            width = mPanel.getWidth();
            height = mPanel.getHeight();
        }
        if(dotbar != null && shadow != null)
            mSpectrumVisualizer.setBitmaps(width, height, dotbar, shadow);
        if (mAlphaWidthNum >= 0)
            mSpectrumVisualizer.setAlphaNum(mAlphaWidthNum);
        mResDensity = dotbar == null ? ScreenElementRoot.DEFAULT_RES_DENSITY : dotbar.getDensity();
        mSpectrumVisualizer.layout(0, 0, width, height);
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBitmap.setDensity(mResDensity);
        mCanvas = new Canvas(mBitmap);
    }
}
