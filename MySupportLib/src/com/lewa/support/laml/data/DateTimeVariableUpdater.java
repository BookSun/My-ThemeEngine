
package com.lewa.support.laml.data;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.util.LunarDate;

public class DateTimeVariableUpdater extends NotifierVariableUpdater {

    public static enum Accuracy {
        Day, Hour, Minute, Second;
    }

    private static final String LOG_TAG = "DateTimeVariableUpdater";

    private static final int TIME_SECOND = 1000;

    private static final int TIME_MINUTE = TIME_SECOND * 60;

    private static final int TIME_HOUR = TIME_MINUTE * 60;

    private static final int TIME_DAY = TIME_HOUR * 24;

    public static final String USE_TAG = "DateTime";

    private IndexedNumberVariable mAmPm;

    protected Calendar mCalendar;

    private int mCurDay;

    private long mCurrentTime;

    private IndexedNumberVariable mDate;

    private IndexedNumberVariable mDateLunar;

    private IndexedNumberVariable mDayOfWeek;

    private IndexedNumberVariable mHour12;

    private IndexedNumberVariable mHour24;

    private long mLastUpdatedTime;

    private IndexedNumberVariable mMinute;

    private IndexedNumberVariable mMonth;

    private IndexedNumberVariable mMonthLunar;

    private IndexedNumberVariable mMonthLunarLeap;

    private IndexedStringVariable mNextAlarm;

    private long mNextUpdateTime;

    private IndexedNumberVariable mSecond;

    private IndexedNumberVariable mTime;

    private long mTimeAccuracy;

    private IndexedNumberVariable mTimeFormat;

    private IndexedNumberVariable mTimeSys;

    private final Runnable mTimeUpdater;

    private IndexedNumberVariable mYear;

    private IndexedNumberVariable mYearLunar;

    private IndexedNumberVariable mYearLunar1864;
    
    private Locale mLocale;

    public DateTimeVariableUpdater(VariableUpdaterManager m) {
        this(m, Accuracy.Minute);
    }

    class TimeUpdater implements Runnable {
        public void run() {
            checkUpdateTime();
        }
    }

    public DateTimeVariableUpdater(VariableUpdaterManager m, String accuracy) {
        super(m, Intent.ACTION_TIME_CHANGED);
        mCalendar = Calendar.getInstance();
        mTimeUpdater = new TimeUpdater();
        Accuracy ac = null;
        if (!TextUtils.isEmpty(accuracy)) {
            for (Accuracy a : Accuracy.values()) {
                if (a.name().equals(accuracy)) {
                    ac = a;
                    break;
                }
            }
        }
        if (ac == null) {
            ac = Accuracy.Minute;
            Log.w(LOG_TAG, "invalid accuracy tag:" + accuracy);
        }
        initInner(ac);
    }

    public DateTimeVariableUpdater(VariableUpdaterManager m, Accuracy accuracy) {
        super(m, Intent.ACTION_TIME_CHANGED);
        mCalendar = Calendar.getInstance();
        mTimeUpdater = new TimeUpdater();
        initInner(accuracy);
    }

    private void checkUpdateTime() {
        getContext().getHandler().removeCallbacks(mTimeUpdater);
        long currentTimeMillis = System.currentTimeMillis();
        long currentTime = (currentTimeMillis / mTimeAccuracy) * mTimeAccuracy;
        if (mCurrentTime != currentTime) {
            mCurrentTime = currentTime;
            mNextUpdateTime = mCurrentTime + mTimeAccuracy;
            updateTime();
            mTimeSys.set(currentTimeMillis);
            getContext().requestUpdate();
        }
        getContext().getHandler().postDelayed(mTimeUpdater, mNextUpdateTime - currentTimeMillis);
    }

    private void initInner(Accuracy accuracy) {
        Log.i(LOG_TAG, "init with accuracy:" + accuracy.name());
        switch (accuracy) {
            case Second:
                mTimeAccuracy = TIME_SECOND;
                break;
            case Minute:
                mTimeAccuracy = TIME_MINUTE;
                break;
            case Hour:
                mTimeAccuracy = TIME_HOUR;
                break;
            case Day:
                mTimeAccuracy = TIME_DAY;
                break;
            default:
                mTimeAccuracy = TIME_HOUR;
        }

        mYear = new IndexedNumberVariable(VariableNames.VAR_YEAR, getContext().mVariables);
        mMonth = new IndexedNumberVariable(VariableNames.VAR_MONTH, getContext().mVariables);
        mDate = new IndexedNumberVariable(VariableNames.VAR_DATE, getContext().mVariables);
        mYearLunar = new IndexedNumberVariable("year_lunar", getContext().mVariables);
        mYearLunar1864 = new IndexedNumberVariable("year_lunar1864", getContext().mVariables);
        mMonthLunar = new IndexedNumberVariable("month_lunar", getContext().mVariables);
        mMonthLunarLeap = new IndexedNumberVariable("month_lunar_leap", getContext().mVariables);
        mDateLunar = new IndexedNumberVariable("date_lunar", getContext().mVariables);
        mDayOfWeek = new IndexedNumberVariable(VariableNames.VAR_DAY_OF_WEEK,
                getContext().mVariables);
        mAmPm = new IndexedNumberVariable(VariableNames.VAR_AMPM, getContext().mVariables);
        mHour12 = new IndexedNumberVariable(VariableNames.VAR_HOUR12, getContext().mVariables);
        mHour24 = new IndexedNumberVariable(VariableNames.VAR_HOUR24, getContext().mVariables);
        mMinute = new IndexedNumberVariable(VariableNames.VAR_MINUTE, getContext().mVariables);
        mSecond = new IndexedNumberVariable(VariableNames.VAR_SECOND, getContext().mVariables);
        mTime = new IndexedNumberVariable(VariableNames.VAR_TIME, getContext().mVariables);
        mTimeSys = new IndexedNumberVariable(VariableNames.VAR_TIME_SYS, getContext().mVariables);
        mTimeSys.set(System.currentTimeMillis());
        mNextAlarm = new IndexedStringVariable(VariableNames.VAR_NEXT_ALARM_TIME,
                getContext().mVariables);
        mTimeFormat = new IndexedNumberVariable(VariableNames.VAR_TIME_FORMAT,
                getContext().mVariables);
        mTimeFormat
                .set(DateFormat.is24HourFormat(getContext().mContext) ? VariableNames.TIME_FORMAT_24
                        : VariableNames.TIME_FORMAT_12);
        updateTime();
    }

    private void refreshAlarm() {
        String nextAlarm = android.provider.Settings.System.getString(
                getContext().mContext.getContentResolver(), "next_alarm_formatted");
        mNextAlarm.set(nextAlarm);
    }

    private void updateTime() {
        long elapsedTime = SystemClock.elapsedRealtime();
        if (elapsedTime - mLastUpdatedTime >= 200) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            int year = mCalendar.get(Calendar.YEAR);
            int month = mCalendar.get(Calendar.MONTH);
            int date = mCalendar.get(Calendar.DATE);
            mAmPm.set(mCalendar.get(Calendar.AM_PM));
            mHour12.set(mCalendar.get(Calendar.HOUR));
            mHour24.set(mCalendar.get(Calendar.HOUR_OF_DAY));
            mMinute.set(mCalendar.get(Calendar.MINUTE));
            mYear.set(year);
            mMonth.set(month);
            mDate.set(date);
            mDayOfWeek.set(mCalendar.get(Calendar.DAY_OF_WEEK));
            mSecond.set(mCalendar.get(Calendar.SECOND));
            if (date != mCurDay) {
                long lunarDate[] = LunarDate.calLunar(year, month, date);
                mYearLunar.set(lunarDate[0]);
                mMonthLunar.set(lunarDate[1]);
                mDateLunar.set(lunarDate[2]);
                mYearLunar1864.set(lunarDate[3]);
                mMonthLunarLeap.set(lunarDate[6]);
            }
            mLastUpdatedTime = elapsedTime;
        }
    }

    public void finish() {
        super.finish();
        getContext().getHandler().removeCallbacks(mTimeUpdater);
    }

    public void init() {
        super.init();
        mLocale = getContext().mContext.getResources().getConfiguration().locale;
        mTimeFormat
                .set(DateFormat.is24HourFormat(getContext().mContext) ? VariableNames.TIME_FORMAT_24
                        : VariableNames.TIME_FORMAT_12);
        refreshAlarm();
        checkUpdateTime();
    }

    public void onNotify(Context context, Intent intent, Object o) {
        checkUpdateTime();
    }

    public void pause() {
        super.pause();
        if (!getContext().isGlobalThread())
            getContext().getHandler().removeCallbacks(mTimeUpdater);
    }

    public void resume() {
        super.resume();
        refreshAlarm();
        mCalendar = Calendar.getInstance();
		Locale locale = getContext().mContext.getResources().getConfiguration().locale;
		if (mLocale != null && !mLocale.equals(locale)) {
			mCurrentTime = 0;
			mLocale = locale;
		}
        checkUpdateTime();
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        mTime.set(currentTime);
        mTimeSys.set(System.currentTimeMillis());
        updateTime();
    }

}
