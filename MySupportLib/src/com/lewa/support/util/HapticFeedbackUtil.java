
package com.lewa.support.util;

import com.lewa.support.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
//import android.os.SystemProperties;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;

public class HapticFeedbackUtil {

    private static final String TAG = "HapticFeedbackUtil";
    private static final boolean USE_SYSTEM_PATTERN = false;

    public static final int LONG_PRESS_PATTERN = 0;
    public static final int VIRTUAL_DOWN_PATTERN = 1;
    public static final int VIRTUAL_UP_PATTERN = 2;
    public static final int KEYBOARD_TAP_PATTERN = 3;

    private static final String[] KEYBOARD_TAP_PATTERN_PROPERTY = {
            "sys.haptic.tap.weak", "sys.haptic.tap.normal", "sys.haptic.tap.strong"
    };

    private static final String[] LONG_PRESS_PATTERN_PROPERTY = {
            "sys.haptic.long.weak", "sys.haptic.long.normal", "sys.haptic.long.strong"
    };

    private static final String[] VIRTUAL_DOWN_PATTERN_PROPERTY = {
            "sys.haptic.down.weak", "sys.haptic.down.normal", "sys.haptic.down.strong"
    };

    private static final String[] VIRTUAL_UP_PATTERN_PROPERTY = {
            "sys.haptic.up.weak", "sys.haptic.up.normal", "sys.haptic.up.strong"
    };

    private final Context mContext;

    private long[] mKeyboardTapVibePattern;

    private long[] mLongPressVibePattern;

    private Vibrator mVibrator;

    private long[] mVirtualKeyUpVibePattern;

    private long[] mVirtualKeyVibePattern;

    public HapticFeedbackUtil(Context c, boolean onceOnly) {
        mVibrator = (Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
        mContext = c;
        if (onceOnly) {
            updateSettings();
        } else {
            (new SettingsObserver(new Handler())).observe();
        }
    }

    private static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        long[] out;
        if (ar == null) {
            out = null;
        } else {
            out = new long[ar.length];
            for (int i = 0; i < ar.length; i++)
                out[i] = ar[i];
        }

        return out;
    }

//    private long[] loadHaptic(String key, int defaultRes) {
//        String hapString = SystemProperties.get(key);
//
//        if (TextUtils.isEmpty(hapString))
//            return getLongIntArray(mContext.getResources(), defaultRes);
//        return stringToLongArray(hapString);
//    }

    private long[] stringToLongArray(String inpString) {
        long[] returnByte;
        if (inpString == null) {
            returnByte = new long[1];
            returnByte[0] = 0;
        } else {
            String[] splitStr = inpString.split(",");
            int los = splitStr.length;
            returnByte = new long[los];

            for (int i = 0; i < los; i++)
                returnByte[i] = Long.parseLong(splitStr[i].trim());
        }

        return returnByte;
    }

    public boolean isSupportedEffect(int effectId) {
        return effectId <= 3;
    }

    public boolean performHapticFeedback(int effectId, boolean always) {
        boolean hapticsDisabled = System.getInt(mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 0;

        if (always || !hapticsDisabled) {
            long[] pattern;
            switch (effectId) {
                case LONG_PRESS_PATTERN:
                    pattern = mLongPressVibePattern;
                    break;
                case VIRTUAL_DOWN_PATTERN:
                    pattern = mVirtualKeyVibePattern;
                    break;
                case VIRTUAL_UP_PATTERN:
                    pattern = mVirtualKeyUpVibePattern;
                    break;
                case KEYBOARD_TAP_PATTERN:
                    pattern = mKeyboardTapVibePattern;
                    break;
                default:
                    return false;
            }

            if (pattern != null && pattern.length != 0) {
                try {
                    if (pattern.length == 1) {
                        mVibrator.vibrate(pattern[0]);
                    } else {
                        mVibrator.vibrate(pattern, -1);
                    }
                    return true;
                } catch (Exception e) {
                }
            } else {
                Log.w(TAG, "vibrate: null or empty pattern");
            }
        }
        return false;
    }

    public void updateSettings() {
//        if (USE_SYSTEM_PATTERN) {
//            int level = Settings.System.getInt(mContext.getContentResolver(),
//                    "haptic_feedback_level", 1);
//            int i = Math.min(2, Math.max(0, level));
//            mLongPressVibePattern = loadHaptic(LONG_PRESS_PATTERN_PROPERTY[i],
//                    R.array.config_longPressVibePattern);
//            mVirtualKeyVibePattern = loadHaptic(VIRTUAL_DOWN_PATTERN_PROPERTY[i],
//                    R.array.config_virtualKeyVibePattern);
//            mKeyboardTapVibePattern = loadHaptic(KEYBOARD_TAP_PATTERN_PROPERTY[i],
//                    R.array.config_keyboardTapVibePattern);
//            mVirtualKeyUpVibePattern = loadHaptic(VIRTUAL_UP_PATTERN_PROPERTY[i],
//                    R.array.config_virtualKeyVibePattern);
//        } else {
            mLongPressVibePattern = new long[]{50};
            mVirtualKeyVibePattern = new long[]{20};
            mVirtualKeyUpVibePattern = new long[]{20};
            mKeyboardTapVibePattern = new long[]{10};
//        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("haptic_feedback_level"),
                    false, this);
            updateSettings();
        }

        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
}
