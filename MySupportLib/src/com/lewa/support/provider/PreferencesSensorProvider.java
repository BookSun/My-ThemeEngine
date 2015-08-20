package com.lewa.support.provider;

import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import lewa.provider.ExtraSettings;

public class PreferencesSensorProvider {


    public PreferencesSensorProvider(Context context) {
    }

    //Shake and Shake degree
    public static int getShakeDegree(Context context) {
        return Settings.System.getInt(context.getContentResolver(),ExtraSettings.System.SENSOR_SHAKE_VALUE,
            SensorProviderListener.DEFAULT_SHAKE_SPEED);
    }

    public static void setShakeDegree(Context context, int degree) {
        Settings.System.putInt(context.getContentResolver(),ExtraSettings.System.SENSOR_SHAKE_VALUE,degree);
    }

    //Turn phone down degree,from 90 to 180
    public static float getTurnDownDegree(Context context) {
        return Settings.System.getInt(context.getContentResolver(),ExtraSettings.System.SENSOR_TURN_DOWN_VALUE,
            SensorProviderListener.DEFAULT_TURN_DOWN_DEGREE);
    }

    public static void setTurnDownDegree(Context context, double degree) {
        if (degree >= 90 && degree <= 180) {
            int degreeTrigger = (int)getTriggerDegree(degree);
            Settings.System.putInt(context.getContentResolver(),ExtraSettings.System.SENSOR_TURN_DOWN_VALUE,-degreeTrigger);
        }
    }

    //Turn phone up degree , from 90 to 180
    public static float getTurnUpDegree(Context context) {
        return Settings.System.getInt(context.getContentResolver(),ExtraSettings.System.SENSOR_TURN_UP_VALUE,
            SensorProviderListener.DEFAULT_TURN_UP_DEGREE);
    }

    public static void setTurnUpDegree(Context context, double degree) {
        if (degree >= 90 && degree <= 180) {
            int degreeTrigger = (int)getTriggerDegree(degree);
            Settings.System.putInt(context.getContentResolver(),ExtraSettings.System.SENSOR_TURN_UP_VALUE,degreeTrigger);
        }
    }

    private static double getTriggerDegree(double degree) {
        degree = Math.PI-degree*Math.PI/SensorProviderListener.DEGREE_PI;
        degree = Math.cos(degree);
        degree = degree*SensorProviderListener.GRAVITY;
        return degree;
    }

    //Adjust smart Call phone delay time.
    public static int getSmartCallDelayTime(Context context) {
        return Settings.System.getInt(context.getContentResolver(),ExtraSettings.System.SENSOR_CALL_DELAY_VALUE,
            SensorProviderListener.DEFAULT_SMART_CALL_TIME);
    }

    public static void setSmartCallDelayTime(Context context, int time) {
        Settings.System.putInt(context.getContentResolver(),ExtraSettings.System.SENSOR_CALL_DELAY_VALUE,time);
    }


}
