
package com.lewa.support.laml.elements;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;

import com.lewa.support.laml.*;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

public class TimepanelScreenElement extends ImageScreenElement implements
com.lewa.support.laml.NotifierManager.OnNotifyListener {

    private static final String LOG_TAG = "TimepanelScreenElement";

    private static final String M12 = "hh:mm";

    private static final String M24 = "kk:mm";

    public static final String TAG_NAME = "Time";

    private int mBmpHeight;

    private int mBmpWidth;

    protected Calendar mCalendar;

    private String mFormat;

    private Expression mFormatExp;

    private long mLastUpdateTime;

    private boolean mLoadResourceFailed;

    private CharSequence mPreTime;

    private int mSpace;

    public TimepanelScreenElement(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(node, root);
        mFormat = M24;
        mCalendar = Calendar.getInstance();
        mFormat = node.getAttribute("format");
        mFormatExp = Expression.build(node.getAttribute("formatExp"));
        mSpace = Utils.getAttrAsInt(node, "space", 0);
    }

    private void createBitmap() {
        String digits = "0123456789:";
        int maxWidth = 0;
        int density = 0;
        for (int i = 0; i < digits.length(); i++) {
            Bitmap digitBmp = getDigitBmp(digits.charAt(i));
            if (digitBmp == null) {
                mLoadResourceFailed = true;
                Log.e(LOG_TAG, "Failed to load digit bitmap: " + digits.charAt(i));
                return;
            }
            if (maxWidth < digitBmp.getWidth())
                maxWidth = digitBmp.getWidth();
            if (mBmpHeight < digitBmp.getHeight())
                mBmpHeight = digitBmp.getHeight();
            if (density == 0)
                density = digitBmp.getDensity();
        }

        int space = (int) scale(mSpace);
        mBitmap = Bitmap.createBitmap(maxWidth * 5 + space * 4, mBmpHeight, Bitmap.Config.ARGB_8888);
        mBitmap.setDensity(density);
        setActualHeight(mBmpHeight);
    }

    private Bitmap getDigitBmp(char c) {
        String src = TextUtils.isEmpty(mAni.getSrc()) ? "time.png" : mAni.getSrc();
        String suffix = c == ':' ? "dot" : String.valueOf(c);
        return getContext().mResourceManager.getBitmap(Utils.addFileNameSuffix(src, suffix));
    }

    private String getFormat() {
        if (mFormatExp != null)
            return mFormatExp.evaluateStr(getVariables());
        else
            return mFormat;
    }

    private void setDateFormat() {
        if (mFormatExp == null) {
            mFormat = DateFormat.is24HourFormat(getContext().mContext) ? M24 : M12;
        }
    }

    private void updateTime() {
        if (mLoadResourceFailed)
            return;
        if (mBitmap == null) {
            createBitmap();
        }
        if (mBitmap != null){
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis / 60 * 1000 != mLastUpdateTime) {
                mCalendar.setTimeInMillis(currentTimeMillis);
                CharSequence newTime = DateFormat.format(getFormat(), mCalendar);
                if (!newTime.equals(mPreTime)) {
                    mPreTime = newTime;
                    Canvas tmpCanvas = new Canvas(mBitmap);
                    tmpCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    int x = 0;
                    int space = (int) scale(mSpace);
                    for (int i = 0; i < newTime.length(); i++) {
                        char digit = newTime.charAt(i);
                        Bitmap bmp = getDigitBmp(digit);
                        if (bmp != null) {
                            tmpCanvas.drawBitmap(bmp, x, 0, null);
                            x = space + (x + bmp.getWidth());
                        }
                    }

                    mBmpWidth = x - space;
                    setActualWidth(descale(mBmpWidth));
                    mLastUpdateTime = currentTimeMillis / 60000L;
                    requestUpdate();
                }
            }
        }
    }

    public void finish() {
        mPreTime = null;
        mLoadResourceFailed = false;
        mLastUpdateTime = 0;
        getNotifierManager().releaseNotifier(Intent.ACTION_TIME_CHANGED, this);
        getNotifierManager().releaseNotifier(Intent.ACTION_TIME_TICK, this);
    }

    protected int getBitmapWidth() {
        return mBmpWidth;
    }

    public void init() {
        super.init();
        setDateFormat();
        mPreTime = null;
        mLastUpdateTime = 0;
        getNotifierManager().acquireNotifier(Intent.ACTION_TIME_CHANGED, this);
        getNotifierManager().acquireNotifier(Intent.ACTION_TIME_TICK, this);
        updateTime();
    }

    public void onNotify(Context context, Intent intent, Object o) {
        updateTime();
    }

    public void pause() {
        getNotifierManager().pause(Intent.ACTION_TIME_TICK, this);
        getNotifierManager().pause(Intent.ACTION_TIME_TICK, this);
    }

    public void resume() {
        mCalendar = Calendar.getInstance();
        getNotifierManager().resume(Intent.ACTION_TIME_TICK, this);
        getNotifierManager().resume(Intent.ACTION_TIME_TICK, this);
        updateTime();
    }
}
