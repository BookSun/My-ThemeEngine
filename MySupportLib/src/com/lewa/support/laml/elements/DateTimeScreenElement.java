
package com.lewa.support.laml.elements;

import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;

import com.lewa.support.laml.*;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Expression.NumberExpression;
import com.lewa.support.util.LunarDate;

import org.w3c.dom.Element;

public class DateTimeScreenElement extends TextScreenElement {

    public static final String TAG_NAME = "DateTime";

    protected Calendar mCalendar = Calendar.getInstance();

    private int mCurDay = -1;

    private String mLunarDate;

    private long mPreValue;

    private String mText;

    private Expression mValue;

    public DateTimeScreenElement(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, root);
        mValue = Expression.build(node.getAttribute("value"));
    }

    private Expression.NumberExpression numberExpression;

    public void setText(String text) {
        if (numberExpression == null) {
            numberExpression = (NumberExpression)Expression.build(text);
            mValue = numberExpression;
        }
        numberExpression.setValue(Double.valueOf(text));
        super.setText(text);
    }

    protected String getText() {
        long ms = mValue != null ? (long) evaluate(mValue) : System.currentTimeMillis();
        if (Math.abs(ms - mPreValue) < 200)
            return mText;
        mCalendar.setTimeInMillis(ms);
        String format = getFormat();
        if (format == null)
            return null;
        if (format.contains("NNNN")) {
            if (mCalendar.get(5) != mCurDay) {
                Resources res = getContext().mContext.getResources();
                mLunarDate = LunarDate.getString(res, mCalendar);
                String term = LunarDate.getSolarTerm(res, mCalendar);
                if (term != null)
                    mLunarDate = mLunarDate + ' ' + term;
                mCurDay = mCalendar.get(5);
                Log.i("DateTimeScreenElement", "get lunar date:" + mLunarDate);
            }
            format = format.replace("NNNN", mLunarDate);
        }
        mText = DateFormat.format(format, mCalendar).toString();
        mPreValue = ms;
        return mText;
    }

    public void resume() {
        super.resume();
        mCalendar = Calendar.getInstance();
    }
}
