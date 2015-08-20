
package com.lewa.support.laml.util;

import android.graphics.Color;
import android.util.Log;

import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;

import org.w3c.dom.Element;

public class ColorParser {
    private static final int DEFAULT_COLOR = -1;

    private static final String LOG_TAG = "ColorParser";

    private int mColor;

    private String mColorExpression;

    private IndexedStringVariable mIndexedColorVar;

    private Expression[] mRGBExpression;

    private ExpressionType mType;

    public ColorParser(String expression) {
        mColorExpression = expression.trim();
        if (mColorExpression.startsWith("#")) {
            mType = ExpressionType.CONST;
            try {
                mColor = Color.parseColor(mColorExpression);
            } catch (IllegalArgumentException e) {
                mColor = DEFAULT_COLOR;
            }
        } else if (mColorExpression.startsWith("@")) {
            mType = ExpressionType.VARIABLE;
        } else if (mColorExpression.startsWith("argb(") && mColorExpression.endsWith(")")) {
            mRGBExpression = Expression.buildMultiple(mColorExpression.substring(5, -1
                    + mColorExpression.length()));
            if (mRGBExpression.length == 4) {
                mType = ExpressionType.ARGB;
            } else {
                Log.e(LOG_TAG, "bad expression format");
                throw new IllegalArgumentException("bad expression format.");
            }
        } else {
            mType = ExpressionType.INVALID;
        }
    }

    public static ColorParser fromElement(Element e) {
        return fromElement(e, "color");
    }

    public static ColorParser fromElement(Element e, String tagName) {
        return new ColorParser(e.getAttribute(tagName));
    }

    public int getColor(Variables v) {
        switch (mType) {
            case ARGB:
                int a = (int) mRGBExpression[0].evaluate(v);
                int r = (int) mRGBExpression[1].evaluate(v);
                int g = (int) mRGBExpression[2].evaluate(v);
                int b = (int) mRGBExpression[3].evaluate(v);
                mColor = Color.argb(a, r, g, b);
                break;
            case VARIABLE:
                if (mIndexedColorVar == null)
                    mIndexedColorVar = new IndexedStringVariable(mColorExpression.substring(1), v);
                mColor = mIndexedColorVar.get() != null ? Color.parseColor(mIndexedColorVar.get())
                        : DEFAULT_COLOR;
                break;
            case CONST:
            default:
                break;
        }

        return mColor;
    }

    static enum ExpressionType {
        CONST, VARIABLE, ARGB, INVALID
    }
}
