package com.lewa.support.laml.elements;

import android.graphics.*;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.text.TextUtils;
import android.util.Log;

import java.util.*;

import com.lewa.support.laml.*;
import com.lewa.support.laml.animation.AnimatedElement;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.util.ImageUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ImageScreenElement extends AnimatedScreenElement {

    private static final String        LOG_TAG                = "ImageScreenElement";

    public static final String         MASK_TAG_NAME          = "Mask";

    public static final String         TAG_NAME               = "Image";

    private static final String        VAR_BMP_HEIGHT         = "bmp_height";

    private static final String        VAR_BMP_WIDTH          = "bmp_width";

    private float                      mAniHeight;

    private float                      mAniWidth;

    private boolean                    mAntiAlias             = true;

    protected Bitmap                   mBitmap;

    protected Bitmap                   mBluredBitmap;

    private BitmapProvider             mBitmapProvider;

    private float                      mBmpHeight;

    private IndexedNumberVariable      mBmpSizeHeightVar;

    private IndexedNumberVariable      mBmpSizeWidthVar;

    private float                      mBmpWidth;

    private Canvas                     mBufferCanvas;

    protected Bitmap                   mCurrentBitmap;

    private Rect                       mDesRect               = new Rect();

    private float                      mHeight;

    private String                     mKey;

    private Bitmap                     mMaskBuffer;

    protected int                      mBlurRadius;

    private Paint                      mMaskPaint             = new Paint();

    private ArrayList<AnimatedElement> mMasks;

    protected Paint                    mPaint                 = new Paint();

    private pair<Double, Double>       mRotateXYpair;

    private Expression                 mSrcH;

    private Rect                       mSrcRect;

    private Expression                 mSrcW;

    private Expression                 mSrcX;

    private Expression                 mSrcY;

    private float                      mWidth;

    private float                      mX;

    private float                      mY;

    protected int[]                    mBlurMaskColors;

    protected int[]                    mCoverColors;

    private int                        mScaleType;

    private static final int           SCALE_TYPE_CROP_CENTER = 1;

    public ImageScreenElement(Element node, ScreenElementRoot root) throws ScreenElementLoadException{
        super(node, root);
        load(node);
        mPaint.setFilterBitmap(mAntiAlias);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mMaskPaint.setFilterBitmap(mAntiAlias);
        mSrcX = Expression.build(node.getAttribute("srcX"));
        mSrcY = Expression.build(node.getAttribute("srcY"));
        mSrcW = Expression.build(node.getAttribute("srcW"));
        mSrcH = Expression.build(node.getAttribute("srcH"));
        if (mSrcX != null && mSrcY != null && mSrcW != null && mSrcH != null) mSrcRect = new Rect();
        boolean useVirtualScreen = Boolean.parseBoolean(node.getAttribute("useVirtualScreen"));
        String srcType = node.getAttribute("srcType");
        if (useVirtualScreen) srcType = "VirtualScreen";
        String scaleType = node.getAttribute("scaleType");
        if (scaleType != null && scaleType.equals("centerCrop")) {
            mScaleType = SCALE_TYPE_CROP_CENTER;
        }
        mBitmapProvider = BitmapProvider.create(root, srcType);
        if (mHasName) {
            mBmpSizeWidthVar = new IndexedNumberVariable(mName, VAR_BMP_WIDTH, getVariables());
            mBmpSizeHeightVar = new IndexedNumberVariable(mName, VAR_BMP_HEIGHT, getVariables());
        }
    }

    private String getKey() {
        if (mKey == null) mKey = UUID.randomUUID().toString();
        return mKey;
    }

    private void loadMask(Element node) throws ScreenElementLoadException {
        if (mMasks == null) mMasks = new ArrayList<AnimatedElement>();
        mMasks.clear();
        NodeList images = node.getElementsByTagName(MASK_TAG_NAME);
        for (int i = 0, N = images.getLength(); i < N; i++)
            mMasks.add(new AnimatedElement((Element)images.item(i), mRoot));

    }

    private void renderWithMask(Canvas bufferCanvas, AnimatedElement mask, int x, int y) {
        bufferCanvas.save();
        Bitmap rawMask = getContext().mResourceManager.getBitmap(mask.getSrc());
        if (rawMask == null) return;
        double maskX = scale(mask.getX());
        double maskY = scale(mask.getY());
        float maskAngle = mask.getRotationAngle();
        if (mask.isAlignAbsolute()) {
            float angle = getRotation();
            if (angle == 0) {
                maskX -= x;
                maskY -= y;
            } else {
                maskAngle -= angle;
                double angleA = Math.PI * (double)angle / 180;
                double cx = getPivotX();
                double cy = getPivotY();
                if (mRotateXYpair == null) mRotateXYpair = new pair<Double, Double>();
                rotateXY(cx, cy, angleA, mRotateXYpair);
                double rx = x + mRotateXYpair.p1;
                double ry = y + mRotateXYpair.p2;
                rotateXY(mask.getPivotX(), mask.getPivotY(), Math.PI * (double)mask.getRotationAngle() / 180,
                         mRotateXYpair);
                double dx = maskX + scale(mRotateXYpair.p1) - rx;
                double dy = maskY + scale(mRotateXYpair.p2) - ry;
                double dm = Math.sqrt(dx * dx + dy * dy);
                double angleB = Math.asin(dx / dm);
                double angleC = dy > 0 ? angleA + angleB : (Math.PI + angleA) - angleB;
                maskX = dm * Math.sin(angleC);
                maskY = dm * Math.cos(angleC);
            }
        }
        bufferCanvas.rotate(maskAngle, (float)(maskX + scale(mask.getPivotX())),
                            (float)(maskY + scale(mask.getPivotY())));

        int i = (int)maskX;
        int j = (int)maskY;
        int k = Math.round(scale(mask.getWidth()));
        if (k < 0) k = rawMask.getWidth();
        int l = Math.round(scale(mask.getHeight()));
        if (l < 0) l = rawMask.getHeight();
        mDesRect.set(i, j, i + k, j + l);

        mMaskPaint.setAlpha(mask.getAlpha());
        bufferCanvas.drawBitmap(rawMask, null, mDesRect, mMaskPaint);
        bufferCanvas.restore();
    }

    private void rotateXY(double centerX, double centerY, double angle, pair<Double, Double> pr) {
        double cm = Math.sqrt(centerX * centerX + centerY * centerY);
        if (cm > 0) {
            double angle1 = Math.acos(centerX / cm);
            double angle2 = Math.PI - angle1 - angle;
            pr.p1 = centerX + cm * Math.cos(angle2);
            pr.p2 = centerY - cm * Math.sin(angle2);
        } else {
            pr.p1 = pr.p2 = 0D;
        }
    }

    private void updateBitmap() {
        mCurrentBitmap = getBitmap();
        if (mCurrentBitmap != null && mBluredBitmap == null && mNeedBlur && mChangeForVisibility) {
            mChangeForVisibility = false;
            mBluredBitmap = getBlurredBitmap(mCurrentBitmap);
            mCurrentBitmap = getBitmap();
        }
        if (mHasName) {
            mBmpSizeWidthVar.set(descale(getBitmapWidth()));
            mBmpSizeHeightVar.set(descale(getBitmapHeight()));
        }
        mAniWidth = super.getWidth();
        mBmpWidth = getBitmapWidth();
        mWidth = mAniWidth >= 0 ? mAniWidth : mBmpWidth;
        mAniHeight = super.getHeight();
        mBmpHeight = getBitmapHeight();
        mHeight = mAniHeight >= 0 ? mAniHeight : mBmpHeight;
        mX = super.getX();
        mY = super.getY();
    }

    public void doRender(Canvas c) {
        if(isVisible()){
        Bitmap bmp = mCurrentBitmap;
        if (bmp != null) {
            int alpha = getAlpha();
            mPaint.setAlpha(alpha);
            int oldDensity = c.getDensity();
            c.setDensity(0);
            if (mWidth != 0 && mHeight != 0) {
                int x = (int)getLeft(mX, mWidth);
                int y = (int)getTop(mY, mHeight);
                c.save();
                if (mMasks.size() == 0) {
                    if (bmp.getNinePatchChunk() != null) {
                        NinePatch np = getContext().mResourceManager.getNinePatch(mAni.getSrc());
                        if (np != null) {
                            mDesRect.set(x, y, (int)((float)x + mWidth), (int)((float)y + mHeight));
                            np.draw(c, mDesRect, mPaint);
                        } else {
                            Log.e(LOG_TAG, "the image contains ninepatch chunk but couldn't get NinePatch object: "
                                           + mAni.getSrc());
                        }
                    } else if (mAniWidth <= 0 && mAniHeight <= 0 && mSrcRect == null) {
                        c.drawBitmap(bmp, x, y, mPaint);
                    } else {
                        if (mScaleType == SCALE_TYPE_CROP_CENTER) {
                            if (mBmpWidth < mBmpHeight) {
                                float scale = mBmpWidth / mBmpHeight;
                                mWidth = scale * mHeight;
                                x = (int)getLeft(mX, mWidth);
                            } else {
                                float scale = mBmpHeight / mBmpWidth;
                                mHeight = scale * mWidth;
                                y = (int)getTop(mY, mHeight);
                            }
                            mDesRect.set(x, y, (int)(x + mWidth), (int)(y + mHeight));
                            if (mSrcRect != null) {
                                int sX = (int)scale(evaluate(mSrcX));
                                int sY = (int)scale(evaluate(mSrcY));
                                int sW = (int)scale(evaluate(mSrcW));
                                int sH = (int)scale(evaluate(mSrcH));
                                mSrcRect.set(sX, sY, sX + sW, sY + sH);
                            }
                            c.drawBitmap(bmp, mSrcRect, mDesRect, mPaint);
                        } else {
                            mDesRect.set(x, y, (int)(x + mWidth), (int)(y + mHeight));
                            if (mSrcRect != null) {
                                int sX = (int)scale(evaluate(mSrcX));
                                int sY = (int)scale(evaluate(mSrcY));
                                int sW = (int)scale(evaluate(mSrcW));
                                int sH = (int)scale(evaluate(mSrcH));
                                mSrcRect.set(sX, sY, sX + sW, sY + sH);
                            }
                            c.drawBitmap(bmp, mSrcRect, mDesRect, mPaint);
                        }

                    }
                } else {
                    float maxWidth = getMaxWidth();
                    float maxHeight = getMaxHeight();
                    int bufferWidth = (int)Math.ceil(Math.max(maxWidth, mWidth));
                    int bufferHeight = (int)Math.ceil(Math.max(maxHeight, mHeight));
                    if (mMaskBuffer == null || bufferWidth > mMaskBuffer.getWidth()
                        || bufferHeight > mMaskBuffer.getHeight()) {
                        Bitmap bitmap = getContext().mResourceManager.getMaskBufferBitmap(bufferWidth, bufferHeight,
                                                                                          getKey());
                        mMaskBuffer = bitmap;
                        mMaskBuffer.setDensity(bmp.getDensity());
                        Canvas canvas = new Canvas(mMaskBuffer);
                        mBufferCanvas = canvas;
                    }
                    mBufferCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    float scale = mRoot.getScale();
                    if (mAniWidth <= 0 && mAniHeight <= 0 && mSrcRect == null) {
                        mBufferCanvas.drawBitmap(bmp, 0, 0, null);
                    } else {
                        mDesRect.set(0, 0, (int)mWidth, (int)mHeight);
                        if (mSrcRect != null) {
                            int sX = (int)(scale * evaluate(mSrcX));
                            int sY = (int)(scale * evaluate(mSrcY));
                            int sW = (int)(scale * evaluate(mSrcW));
                            int sH = (int)(scale * evaluate(mSrcH));
                            mSrcRect.set(sX, sY, sX + sW, sY + sH);
                        }
                        mBufferCanvas.drawBitmap(bmp, mSrcRect, mDesRect, mPaint);
                    }
                    for (AnimatedElement ae : mMasks) {
                        renderWithMask(mBufferCanvas, ae, x, y);
                    }
                    c.drawBitmap(mMaskBuffer, x, y, mPaint);
                }
                c.restore();
                c.setDensity(oldDensity);
            }
         }
        }
    }

    public void finish() {
        super.finish();
        if (mBitmapProvider != null) mBitmapProvider.finish();
        mBitmap = null;
        mCurrentBitmap = null;
        mMaskBuffer = null;
        mBluredBitmap = null;
    }

    protected Bitmap getBitmap() {
        if (mBluredBitmap != null) return mBluredBitmap;
        if (mBitmap != null) return mBitmap;
        if (mBitmapProvider != null) return mBitmapProvider.getBitmap(mAni.getSrc());
        else return null;
    }

    protected int getBitmapHeight() {
        if (mCurrentBitmap != null) return mCurrentBitmap.getHeight();
        else return 0;
    }

    protected int getBitmapWidth() {
        if (mCurrentBitmap != null) return mCurrentBitmap.getWidth();
        else return 0;
    }

    public float getHeight() {
        return mHeight;
    }

    public float getWidth() {
        return mWidth;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public void init() {
        super.init();
        mBitmap = null;
        mBluredBitmap = null;
        mMaskBuffer = null;
        if (mMasks != null) {
            for (AnimatedElement mask : mMasks)
                mask.init();
        }
        if (mBitmapProvider != null) mBitmapProvider.init(mAni.getSrc());
    }

    public void load(Element node) throws ScreenElementLoadException {
        if (node == null) {
            Log.e(LOG_TAG, "node is null");
            throw new ScreenElementLoadException("node is null");
        } else {
            String tmp = node.getAttribute("needBlurForVisibilityChange");
            if (!TextUtils.isEmpty(tmp)) {
                mNeedBlurForVisibilityChange = Boolean.parseBoolean(tmp);
            }
            loadMask(node);
            tmp = node.getAttribute("blureMode");
            if (!TextUtils.isEmpty(tmp)) {
                mBlurMode = Integer.valueOf(tmp);
            }
            String attr = node.getAttribute("blur");
            if (!TextUtils.isEmpty(attr)) {
                mBlurRadius = Integer.parseInt(attr);
                mNeedBlur = true;
            }
            try {
                attr = node.getAttribute("blurMaskGradient");
                if (!TextUtils.isEmpty(attr)) {
                    String[] attrs = attr.split("\\|");
                    int length = attrs.length;
                    if (attrs.length >= 2) {
                        mBlurMaskColors = new int[length];
                        for (int i = 0; i < length; i++)
                            mBlurMaskColors[i] = Color.parseColor(attrs[i]);
                    }
                }
                attr = node.getAttribute("coverGradient");
                if (!TextUtils.isEmpty(attr)) {
                    String[] attrs = attr.split("\\|");
                    int length = attrs.length;
                    if (length >= 2) {
                        mCoverColors = new int[length];
                        for (int i = 0; i < length; i++)
                            mCoverColors[i] = Color.parseColor(attrs[i]);
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "create wallpaper error", e);
            }
        }
    }

    protected void onVisibilityChange(boolean visible) {
        super.onVisibilityChange(visible);
        if (mNeedBlurForVisibilityChange && visible) {
            mBluredBitmap = null;
        }
        if (visible) {
            updateBitmap();
        } else {
            mCurrentBitmap = null;
        }
        mChangeForVisibility = visible;
    }

    public void reset(long time) {
        super.reset(time);
        if (mMasks != null) {
            for (AnimatedElement mask : mMasks)
                mask.reset(time);
        }
        if (mBitmapProvider != null) mBitmapProvider.reset();
    }

    private int     mBlurMode                    = 0;
    private boolean mNeedBlur                    = false;
    private boolean mChangeForVisibility         = true;
    private boolean mNeedBlurForVisibilityChange = false;

    public void setBitmap(Bitmap bmp) {
        if (bmp != mBitmap) {
            mBitmap = bmp;
            mBluredBitmap = getBlurredBitmap(bmp);
            mNeedBlur = false;
            updateBitmap();
        }
    }

    protected Bitmap getBlurredBitmap(Bitmap bmp) {
        if (mBlurMode == 0) {
            return getBlurredBitmap(bmp, 150);
        }
        return getBlurredBitmapForBase(bmp, 150);
    }

    protected Bitmap getBlurredBitmap(Bitmap bmp, int minSize) {
        Bitmap mod = null;
        if (bmp != null && bmp.getWidth() > 1 && bmp.getNinePatchChunk() == null && mBlurRadius > 0) {
            try {
                int width = bmp.getWidth();
                int height = bmp.getHeight();
                // calculate blurred bitmap size
                int bwidth = (int)((float)width / 3);
                int bheight = (int)((float)height / 3);
                int bmax = Math.max(bwidth, bheight);
                if (bmax < minSize) {
                    if (Math.max(width, height) < minSize) {
                        bwidth = width;
                        bheight = height;
                    } else if (bwidth > bheight) {
                        bwidth = minSize;
                        bheight = (int)(minSize * (float)height / width);
                    } else if (bwidth < bheight) {
                        bheight = minSize;
                        bwidth = (int)(minSize * (float)width / height);
                    }
                }
                boolean scaled = width != bwidth && height != bheight;
                // create blurred bitmap
                Bitmap tmp = scaled ? Bitmap.createScaledBitmap(bmp, bwidth, bheight, false) : bmp;
                Bitmap blurred = tmp.copy(Bitmap.Config.ARGB_8888, true);
                blurred = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
                blurred.eraseColor(0xff000000);
                ImageUtils.fastBlur(tmp, blurred, mBlurRadius);
                if (scaled) tmp.recycle();
                // draw linear gradient blur
                Canvas c = new Canvas(blurred);
                if (mBlurMaskColors != null) {
                    Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                    maskPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
                    maskPaint.setShader(new LinearGradient(0, 0, 1, bheight, mBlurMaskColors, null, TileMode.CLAMP));
                    c.drawRect(new Rect(0, 0, bwidth, bheight), maskPaint);
                }
                // draw original bitmap
                mod = bmp.copy(Bitmap.Config.ARGB_8888, true);
                c.setBitmap(mod);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                c.drawBitmap(blurred, null, new RectF(0, 0, width, height), paint);
                // draw linear gradient black cover
                if (mCoverColors != null) {
                    paint.setShader(new LinearGradient(0, 0, 1, height, mCoverColors, null, TileMode.CLAMP));
                    c.drawRect(new Rect(0, 0, width, height), paint);
                }
            } catch (OutOfMemoryError e) {
            } catch (Exception e) {
            }
        }
        return mod;
    }

    protected Bitmap getBlurredBitmapForBase(Bitmap bmp, int minSize) {
        Bitmap mod = null;
        if (bmp != null && bmp.getWidth() > 1 && bmp.getNinePatchChunk() == null && mBlurRadius > 0) {
            try {
                int width = bmp.getWidth();
                int height = bmp.getHeight();
                // calculate blurred bitmap size
                int bwidth = (int)((float)width / 3);
                int bheight = (int)((float)height / 3);
                int bmax = Math.max(bwidth, bheight);
                if (bmax < minSize) {
                    if (Math.max(width, height) < minSize) {
                        bwidth = width;
                        bheight = height;
                    } else if (bwidth > bheight) {
                        bwidth = minSize;
                        bheight = (int)(minSize * (float)height / width);
                    } else if (bwidth < bheight) {
                        bheight = minSize;
                        bwidth = (int)(minSize * (float)width / height);
                    }
                }
                boolean scaled = width != bwidth && height != bheight;
                // create blurred bitmap
                Bitmap tmp = scaled ? Bitmap.createScaledBitmap(bmp, bwidth, bheight, false) : bmp;
                Bitmap blurred = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
//                blurred.eraseColor(Color.BLACK);
                 blurred.eraseColor(Color.TRANSPARENT);
                ImageUtils.fastBlur(tmp, blurred, mBlurRadius);
                if (scaled) tmp.recycle();
                return blurred;
            } catch (OutOfMemoryError e) {
            } catch (Exception e) {
            }
        }
        return mod;
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        if (!isVisible()) return;
        if (mMasks != null) {
            for (AnimatedElement mask : mMasks)
                mask.tick(currentTime);
        }
        updateBitmap();
    }

    private static class pair<T1, T2> {

        public T1 p1;

        public T2 p2;

    }
}
