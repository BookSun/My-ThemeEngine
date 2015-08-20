
package com.lewa.support.laml.elements;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Typeface;
import android.graphics.Shader.TileMode;
import android.text.*;
import android.text.Layout.Alignment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.util.*;
import com.lewa.support.util.FileUtilities;


import org.w3c.dom.Element;

import static com.lewa.support.util.Process.IS_SYSTEM;

public class TextScreenElement extends AnimatedScreenElement {
    private static final int SHADOW_COLOR = 0x20000000;

    private static final int MIN_SHADOW_ALPHA = 0xBB;

    private static final int DEFAULT_SIZE = 18;

    private static final String LOG_TAG = "TextScreenElement";

    private static final int MARQUEE_FRAMERATE = 30;

    private static final int PADDING = 50;

    public static final String TAG_NAME = "Text";

    public static final String TEXT_HEIGHT = "text_height";

    public static final String TEXT_WIDTH = "text_width";

    private ColorParser mColorParser;

    private TextFormatter mFormatter;

    private int mMarqueeGap;

    private float mMarqueePos = Float.MAX_VALUE;

    private int mMarqueeSpeed;

    private boolean mMultiLine;

    private TextPaint mPaint = new TextPaint();

    private String mPreText;

    private long mPreviousTime;

    protected String mSetText;

    private boolean mShouldMarquee;

    private Expression mSizeExpression;

    private float mSpacingAdd;

    private float mSpacingMult;

    private String mText;

    private IndexedNumberVariable mTextHeightVar;

    private StaticLayout mTextLayout;

    private float mTextWidth;

    private float mTextSize;

    private float mShadowRadius, mShadowDx, mShadowDy;

    private int mShadowColor;

    private IndexedNumberVariable mTextWidthVar;

    public TextScreenElement(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, root);
        load(node);
        if (mHasName) {
            mTextWidthVar = new IndexedNumberVariable(mName, TEXT_WIDTH, getVariables());
            mTextHeightVar = new IndexedNumberVariable(mName, TEXT_HEIGHT, getVariables());
        }
    }

    private Alignment getAlignment() {
        switch (mAlign) {
            case LEFT:
                return Alignment.ALIGN_LEFT;
            case CENTER:
                return Alignment.ALIGN_CENTER;
            case RIGHT:
                return Alignment.ALIGN_RIGHT;
            default:
                return Alignment.ALIGN_NORMAL;
        }
    }

    private void updateTextSize() {
        if (mSizeExpression != null) {
            mTextSize = scale(evaluate(mSizeExpression));
            mPaint.setTextSize(mTextSize);
        }
    }

    private void updateTextWidth() {
        if (!TextUtils.isEmpty(mText)) {
            updateTextSize();
            mTextWidth = mPaint.measureText(mText);
            if (mHasName) {
                mTextWidthVar.set(descale(mTextWidth));
            }
        }
    }

    public void doRender(Canvas c) {
        if (!isVisible()||TextUtils.isEmpty(mText))
            return;
        if (mPaint.getShader() == null)
            mPaint.setColor(getColor());
        int alpha = getAlpha();
        mPaint.setAlpha(alpha);
        mPaint.setShadowLayer(alpha > MIN_SHADOW_ALPHA ? (c.isHardwareAccelerated() ? mShadowRadius
                : mShadowRadius / 2) : 0, mShadowDx, mShadowDy, mShadowColor);
        float width = getWidth();
        if (width < 0 || width > mTextWidth)
            width = mTextWidth;
        float height = getHeight();
        float lineHeight = mPaint.getTextSize();
        if (height < 0 && mTextLayout == null)
            height = lineHeight;
        float x = getLeft(getX(), width);
        float y = height > 0 ? getTop(getY(), height) : getY();
        c.save();
        if (width >= 0 && height >= 0) {
            c.clipRect(x, y - 10, x + width, 20 + y + height);
        }
        if (mTextLayout != null) {
            for (int k = 0, j = mTextLayout.getLineCount(); k < j; k++) {
                c.drawText(mText, mTextLayout.getLineStart(k), mTextLayout.getLineEnd(k), x
                        + mTextLayout.getLineLeft(k),
                        y + lineHeight + (float) mTextLayout.getLineTop(k), mPaint);
            }
        } else {
            c.drawText(mText, (mMarqueePos == Float.MAX_VALUE ? 0 : mMarqueePos) + x, y
                    + lineHeight, mPaint);
            float nextPos = mMarqueePos + mTextWidth + mTextSize * (float) mMarqueeGap;
            if (nextPos < width) {
                c.drawText(mText, x + nextPos, y + lineHeight, mPaint);
            }
        }
        c.restore();
    }

    public void finish() {
        super.finish();
        mSetText = null;
        mMarqueePos = Float.MAX_VALUE;
    }

    protected int getColor() {
        return mColorParser.getColor(getVariables());
    }

    protected String getFormat() {
        return mFormatter.getFormat(getVariables());
    }

    protected String getText() {
        if (mSetText != null)
            return mSetText;
        else
            return mFormatter.getText(getVariables());
    }

    public void init() {
        super.init();
        mText = getText();
        mMarqueePos = Float.MAX_VALUE;
        updateTextWidth();
    }

    public void load(Element node) throws ScreenElementLoadException {
        if (node == null) {
            Log.e(LOG_TAG, "node is null");
            throw new ScreenElementLoadException("node is null");
        } else {
            mFormatter = TextFormatter.fromElement(node);
            String gradient = node.getAttribute("gradient");
            if (!TextUtils.isEmpty(gradient)) {
                String[] attrs = gradient.split("\\|");
                try {
                    float x0 = scale(Float.valueOf(attrs[0]));
                    float y0 = scale(Float.valueOf(attrs[1]));
                    float x1 = scale(Float.valueOf(attrs[2]));
                    float y1 = scale(Float.valueOf(attrs[3]));
                    int color0 = Color.parseColor(attrs[4]);
                    int color1 = Color.parseColor(attrs[5]);
                    TileMode tileMode = TileMode.MIRROR;
                    if (attrs.length == 7) {
                        String mode = attrs[6];
                        if ("clamp".equals(mode)) {
                            tileMode = TileMode.CLAMP;
                        } else if ("repeat".equals(mode)) {
                            tileMode = TileMode.REPEAT;
                        }
                    }
                    mPaint.setShader(new LinearGradient(x0, y0, x1, y1, color0, color1, tileMode));
                } catch (Exception e) {
                }
            }
            String shadow = node.getAttribute("shadow");
            if (!TextUtils.isEmpty(shadow)) {
                String[] attrs = shadow.split("\\|");
                try {
                    mShadowRadius = scale(Float.valueOf(attrs[0]));
                    mShadowDx = scale(Float.valueOf(attrs[1]));
                    mShadowDy = scale(Float.valueOf(attrs[2]));
                    mShadowColor = attrs.length == 4 ? Color.parseColor(attrs[3]) : SHADOW_COLOR;
                } catch (Exception e) {
                    mShadowRadius = 2;
                    mShadowDx = 1;
                    mShadowDy = 1;
                    mShadowColor = SHADOW_COLOR;
                }
            }
            String face = node.getAttribute("typeface");
            if (!TextUtils.isEmpty(face)) {
                try {
                    File font = extractTypeface(face);
                    if (font != null) {
                        Typeface typeface = Typeface.createFromFile(font);
                        mPaint.setTypeface(typeface);
                    }
                } catch (Exception e) {
                }
            }
            mSizeExpression = Expression.build(node.getAttribute("size"));
            mMarqueeSpeed = Utils.getAttrAsInt(node, "marqueeSpeed", 0);
            boolean bold = Boolean.parseBoolean(node.getAttribute("bold"));
            mSpacingMult = Utils.getAttrAsFloat(node, "spacingMult", 1);
            mSpacingAdd = Utils.getAttrAsFloat(node, "spacingAdd", 0);
            mMarqueeGap = Utils.getAttrAsInt(node, "marqueeGap", 4);
            mMultiLine = Boolean.parseBoolean(node.getAttribute("multiLine"));
            mPaint.setTextSize(DEFAULT_SIZE);
            mPaint.setAntiAlias(true);
            mPaint.setFakeBoldText(bold);
            mColorParser = ColorParser.fromElement(node);
            if (mPaint.getShader() == null)
                mPaint.setColor(getColor());
        }
    }

    private File extractTypeface(String face) {
        InputStream is = null;
        OutputStream os = null;
        try {
            File dir = new File((IS_SYSTEM ? "/cache" : getContext().mContext.getFilesDir()
                    .getAbsolutePath()) + "/typefaces");
            File font = new File(dir, face);
            long[] size = new long[1];
            is = mRoot.getContext().mResourceManager.getInputStream(face, size);
            if (font.exists() && font.length() == size[0])
                return font;
            dir.mkdirs();
            if (is == null)
                return null;
            os = new FileOutputStream(font);
            int n;
            byte[] buf = new byte[4096];
            while ((n = is.read(buf)) >= 0) {
                os.write(buf, 0, n);
            }
            os.flush();
            return font;
        } catch (Exception e) {
        } finally {
            FileUtilities.closeQuietly(is);
            FileUtilities.closeQuietly(os);
        }
        return null;
    }

    protected void onVisibilityChange(boolean visible) {
        super.onVisibilityChange(visible);
        requestFramerate(mShouldMarquee && visible ? MARQUEE_FRAMERATE : 0);
    }

    public void setText(String text) {
        mSetText = text;
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        if (!isVisible())
            return;
        mShouldMarquee = false;
        mText = getText();
        if (TextUtils.isEmpty(mText)) {
            mTextLayout = null;
        } else {
            updateTextWidth();
            float width = getWidth();
            if (width > 0 && mTextWidth > width) {
                if (mMultiLine) {
                    if (mTextLayout == null || !mPreText.equals(mText)) {
                        mPreText = mText;
                        mTextLayout = new StaticLayout(mText, mPaint, (int) width, getAlignment(),
                                mSpacingMult, mSpacingAdd, true);
                        if (mHasName)
                            mTextHeightVar.set(descale(mTextLayout.getLineTop(mTextLayout
                                    .getLineCount())));
                    }
                } else if (mMarqueeSpeed > 0) {
                    if (mMarqueePos == Float.MAX_VALUE) {
                        mMarqueePos = PADDING;
                    } else {
                        mMarqueePos = mMarqueePos
                                - (float) ((long) mMarqueeSpeed * (currentTime - mPreviousTime))
                                / 1000;
                        if (mMarqueePos < -mTextWidth)
                            mMarqueePos = mMarqueePos
                                    + (mTextWidth + mTextSize * (float) mMarqueeGap);
                    }
                    mPreviousTime = currentTime;
                    mShouldMarquee = true;
                }
            } else {
                mTextLayout = null;
                mMarqueePos = Float.MAX_VALUE;
            }
            requestFramerate(mShouldMarquee ? MARQUEE_FRAMERATE : 0);
        }
    }
}
