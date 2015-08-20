
package com.lewa.support.laml;


import com.lewa.support.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * SpectrumVisualizer needs permissions below android.permission.RECORD_AUDIO
 * android.permission.MODIFY_AUDIO_SETTINGS
 * 
 * @author Kavana Sum <krshen@lewatek.com>
 */
public class SpectrumVisualizer extends ImageView {

    private static final String TAG = "SpectrumVisualizer";

    private static final int CONSIDER_SAMPLE_LENGTH = 160;

    private static final int VISUALIZATION_SAMPLE_LENGTH = 256;

    public static boolean IS_LPA_DECODE = false;

    private static final int RES_DEFAULT_SLIDING_PANEL_ID = R.drawable.sliding_panel_visualization_bg;

    private static final int RES_DEFAULT_SLIDING_DOT_BAR_ID = R.drawable.sliding_panel_visualization_dot_bar;

    private static final int RES_DEFAULT_SLIDING_SHADOW_DOT_BAR_ID = R.drawable.sliding_panel_visualization_shadow_dot_bar;

    private static final int MAX_VALID_SAMPLE = 20;

    private float INDEX_SCALE_FACTOR;

    private float SAMPLE_SCALE_FACTOR;

    private float VISUALIZE_DESC_HEIGHT;

    int mAlphaWidthNum;

    private Bitmap mCachedBitmap;

    private Canvas mCachedCanvas;

    int mCellSize;

    int mDotbarHeight;

    private DotBarDrawer mDrawer;

    private boolean mEnableDrawing;

    private boolean mIsEnableUpdate;

    private boolean mIsNeedCareStreamActive;

    int[] mPixels;

    float[] mPointData;

    int mShadowDotbarHeight;

    int[] mShadowPixels;

    private boolean mSoftDrawEnabled = true;

    private int mVisualizationHeight;

    int mVisualizationHeightNum;

    private int mVisualizationWidth;

    int mVisualizationWidthNum;

    private Visualizer mVisualizer;

    private short[] mSampleBuf = new short[CONSIDER_SAMPLE_LENGTH];

    Paint mPaint = new Paint();
    
    private AudioManager mAudioManager;

    public SpectrumVisualizer(Context context) {
        this(context, null);
    }

    public SpectrumVisualizer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpectrumVisualizer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void drawInternal(Canvas canvas) {
        mPaint.setAlpha(255);
        int end = mVisualizationWidthNum - mAlphaWidthNum;

        for (int i = mAlphaWidthNum; i < end; i++)
            mDrawer.drawDotBar(canvas, i);

        for (int i = mAlphaWidthNum; i > 0; i--) {
            mPaint.setAlpha(i * 255 / mAlphaWidthNum);
            mDrawer.drawDotBar(canvas, i - 1);
            mDrawer.drawDotBar(canvas, mVisualizationWidthNum - i);
        }
    }

    private Bitmap drawToBitmap() {
        Bitmap bm = mCachedBitmap;
        Canvas canvas = mCachedCanvas;
        if (bm != null && (bm.getWidth() != getWidth() || bm.getHeight() != getHeight())) {
            bm.recycle();
            bm = null;
        }
        if (bm == null) {
            bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            mCachedBitmap = bm;
            canvas = new Canvas(bm);
            mCachedCanvas = canvas;
        }
        canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
        drawInternal(canvas);
        return bm;
    }

    private void init(Context context, AttributeSet attrs) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Drawable panelDrawable = null;
        Drawable dotBarDrawble = null;
        Drawable shadowDotbarDrawable = null;
        boolean symmetry = false;
        mEnableDrawing = true;
        mIsNeedCareStreamActive = true;
        mAlphaWidthNum = 0;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SpectrumVisualizer);
            panelDrawable = a.getDrawable(R.attr.sliding_panel);
            dotBarDrawble = a.getDrawable(R.attr.sliding_dot_bar);
            shadowDotbarDrawable = a.getDrawable(R.attr.sliding_shadow_dot_bar);
            symmetry = a.getBoolean(R.attr.symmetry, false);
            mAlphaWidthNum = a.getInt(R.attr.alpha_width, mAlphaWidthNum);
            mIsEnableUpdate = a.getBoolean(R.attr.update_enable, false);
            mIsNeedCareStreamActive = a.getBoolean(R.attr.care_streamactive, false);
            a.recycle();
        }

        if (panelDrawable == null)
            panelDrawable = context.getResources().getDrawable(RES_DEFAULT_SLIDING_PANEL_ID);
        Bitmap panelBm = ((BitmapDrawable) panelDrawable).getBitmap();

        if (dotBarDrawble == null)
            dotBarDrawble = context.getResources().getDrawable(RES_DEFAULT_SLIDING_DOT_BAR_ID);
        Bitmap dotBar = ((BitmapDrawable) dotBarDrawble).getBitmap();

        Bitmap shadowDotBar = null;
        if (symmetry) {
            if (shadowDotbarDrawable == null)
                shadowDotbarDrawable = context.getResources().getDrawable(
                        RES_DEFAULT_SLIDING_SHADOW_DOT_BAR_ID);
            shadowDotBar = ((BitmapDrawable) shadowDotbarDrawable).getBitmap();
        }

        setBitmaps(panelBm, dotBar, shadowDotBar);
    }

    public void enableDrawing(boolean enable) {
        mEnableDrawing = enable;
    }

    public void enableUpdate(boolean enable) {
        try {
            if (mIsEnableUpdate != enable) {
                mIsEnableUpdate = enable;
                if (enable && mVisualizer == null) {
                    if (IS_LPA_DECODE)
                        Log.v(TAG, "lpa decode is on, can't enable");
                    mVisualizer = new Visualizer(0);
                    if (!mVisualizer.getEnabled()) {
                        mVisualizer.setCaptureSize(VISUALIZATION_SAMPLE_LENGTH * 2);
                        mVisualizer.setDataCaptureListener(mOnDataCaptureListener,
                                Visualizer.getMaxCaptureRate(), false, true);
                        mVisualizer.setEnabled(true);
                    }
                } else if (!enable && mVisualizer != null) {
                    mVisualizer.setEnabled(false);
                    Thread.sleep(50);
                    mVisualizer.release();
                    mVisualizer = null;
                }
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, e.toString(), e);
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString(), e);
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    public int getVisualHeight() {
        return mVisualizationHeight;
    }

    public int getVisualWidth() {
        return mVisualizationWidth;
    }

    public boolean isUpdateEnabled() {
        return mIsEnableUpdate;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mEnableDrawing) {
            if (mSoftDrawEnabled) {
                canvas.drawBitmap(drawToBitmap(), 0, 0, null);
            } else {
                drawInternal(canvas);
            }
        }
    }

    public void setAlphaNum(int num) {
        if (num <= 0) {
            mAlphaWidthNum = 0;
        } else {
            if (num > mVisualizationWidthNum / 2)
                num = mVisualizationWidthNum / 2;
            mAlphaWidthNum = num;
        }
    }

    public void setBitmaps(int width, int height, Bitmap dotbar, Bitmap shadow) {
        mVisualizationWidth = width;
        mVisualizationHeight = height;
        mCellSize = dotbar.getWidth();
        mDotbarHeight = dotbar.getHeight();
        if (mDotbarHeight > mVisualizationHeight)
            mDotbarHeight = mVisualizationHeight;

        mPixels = new int[mCellSize * mDotbarHeight];
        dotbar.getPixels(mPixels, 0, mCellSize, 0, 0, mCellSize, mDotbarHeight);
        mVisualizationWidthNum = mVisualizationWidth / mCellSize;
        mVisualizationHeightNum = mDotbarHeight / mCellSize;
        SAMPLE_SCALE_FACTOR = (float) MAX_VALID_SAMPLE / mVisualizationHeightNum;
        INDEX_SCALE_FACTOR = (float) Math.log(mVisualizationWidthNum / 3);
        VISUALIZE_DESC_HEIGHT = (float) 1 / mVisualizationHeightNum;
        mPointData = new float[mVisualizationWidthNum];
        if (mAlphaWidthNum == 0)
            mAlphaWidthNum = (mVisualizationWidthNum / 2);

        mShadowPixels = null;
        if (shadow != null) {
            mShadowDotbarHeight = shadow.getHeight();
            if (mShadowDotbarHeight + mDotbarHeight > mVisualizationHeight)
                mShadowDotbarHeight = (mVisualizationHeight - mDotbarHeight);

            if (mShadowDotbarHeight < mCellSize) {
                mDrawer = new AsymmetryDotBar();
            } else {
                mShadowPixels = new int[mCellSize * mShadowDotbarHeight];
                shadow.getPixels(mShadowPixels, 0, mCellSize, 0, 0, mCellSize, mShadowDotbarHeight);
                mDrawer = new SymmetryDotBar();
            }
        } else {
            mDrawer = new AsymmetryDotBar();
        }
    }

    public void setBitmaps(Bitmap panel, Bitmap dotbar, Bitmap shadow) {
        setImageBitmap(panel);
        setBitmaps(panel.getWidth(), panel.getHeight(), dotbar, shadow);
    }

    public void setSoftDrawEnabled(boolean endabled) {
        mSoftDrawEnabled = endabled;
        if ((!endabled) && (mCachedBitmap != null)) {
            mCachedBitmap.recycle();
            mCachedBitmap = null;
            mCachedCanvas = null;
        }
    }

    void update(byte[] fFtBuffer) {
        if (!mIsNeedCareStreamActive || mAudioManager.isMusicActive()) {
            enableDrawing(true);
            if (fFtBuffer != null) {
                short[] sampleBuf = mSampleBuf;
                int sampleLen = mSampleBuf.length;
                for (int i = 0; i < sampleLen; i++) {
                    int a = fFtBuffer[(i * 2)];
                    int b = fFtBuffer[(1 + i * 2)];
                    int c = (int) Math.sqrt(a * a + b * b);
                    if (c >= Short.MAX_VALUE)
                        c = Short.MAX_VALUE;
                    sampleBuf[i] = ((short) c);
                }
                
                int srcIdx = 0;
                int count = 0;
                int i = 0;
                while (i < mVisualizationWidthNum) {
                    int max = 0;
                    while (count < sampleLen) {
                        max = Math.max(max, sampleBuf[srcIdx]);
                        srcIdx++;
                        count += mVisualizationWidthNum;
                    }

                    count -= sampleLen;
                    float rawData;
                    if (max > 1) {
                        float f = (float) (Math.log(i + 2) / INDEX_SCALE_FACTOR);
                        rawData = f * (f * (max - 1));
                    } else {
                        rawData = 0;
                    }
                    mPointData[i] = Math.max((rawData > MAX_VALID_SAMPLE ? mVisualizationHeightNum
                            : rawData / SAMPLE_SCALE_FACTOR) / mVisualizationHeightNum,
                            mPointData[i] - VISUALIZE_DESC_HEIGHT);
                    i++;
                }
                invalidate();
            }
        } else {
            enableDrawing(false);
        }
    }

    class AsymmetryDotBar implements SpectrumVisualizer.DotBarDrawer {

        public void drawDotBar(Canvas canvas, int index) {
            int top = (int) (0.5 + mDotbarHeight * (1.0 - mPointData[index]) / mCellSize)
                    * mCellSize;
            if (top < mDotbarHeight) {
                canvas.drawBitmap(mPixels, top * mCellSize, mCellSize, index * mCellSize, top,
                        mCellSize, mDotbarHeight - top, true, mPaint);
            }
        }
    }

    private static abstract interface DotBarDrawer {
        public abstract void drawDotBar(Canvas paramCanvas, int paramInt);
    }

    class SymmetryDotBar implements SpectrumVisualizer.DotBarDrawer {
        SymmetryDotBar() {
        }

        public void drawDotBar(Canvas canvas, int index) {
            int top = (int) (0.5 + mDotbarHeight * (1.0 - mPointData[index]) / mCellSize)
                    * mCellSize;
            if (top < mDotbarHeight) {
                canvas.drawBitmap(mPixels, top * mCellSize, mCellSize, index * mCellSize, top,
                        mCellSize, mDotbarHeight - top, true, mPaint);
            }
            int bottom = (int) (0.5 + mShadowDotbarHeight * mPointData[index] / mCellSize)
                    * mCellSize;
            if (bottom > mShadowDotbarHeight)
                bottom = mShadowDotbarHeight;

            if (bottom > 0) {
                canvas.drawBitmap(mShadowPixels, 0, mCellSize, index * mCellSize, mDotbarHeight,
                        mCellSize, bottom, true, mPaint);
            }
        }
    }

    private Visualizer.OnDataCaptureListener mOnDataCaptureListener = new Visualizer.OnDataCaptureListener() {
        public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            update(bytes);
        }

        public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
        }
    };
}
