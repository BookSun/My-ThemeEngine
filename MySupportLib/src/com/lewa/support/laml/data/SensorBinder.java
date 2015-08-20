
package com.lewa.support.laml.data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseArray;

public class SensorBinder extends VariableBinder {

    private static final int DEFAULT_DELAY = 0x3040;

    public static final String TAG_NAME = "SensorBinder";

    private static final String LOG_TAG = TAG_NAME;

    private static final HashMap<String, Integer> SENSOR_TYPES;

    private static SensorManager mSensorManager;

    private int mRate;

    private boolean mRegistered;

    private Sensor mSensor;

    private SensorEventListener mSensorEventListener;

    private String mType;


    static {
        SENSOR_TYPES = new HashMap<String, Integer>();
        for(Field field : Sensor.class.getFields()) {
            String name = field.getName();
            if(name.startsWith("TYPE_")) {
                try {
                    name = name.substring(5).toLowerCase(Locale.US);
                    SENSOR_TYPES.put(name, field.getInt(null));
                } catch (Exception e) {
                }
            }
        }
    }

    public SensorBinder(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        super(root);
        mType = node.getAttribute("type");
        mRate = Utils.getAttrAsInt(node, "rate", DEFAULT_DELAY);
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getContext().mContext
                    .getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(getSensorType(mType));
        if (mSensor == null) {
            Log.e(LOG_TAG, "Fail to get sensor! TYPE: " + mType);
        } else {
            mSensorEventListener = new SensorEventListener() {

                public void onSensorChanged(SensorEvent event) {
                    for (int i = 0; i < event.values.length; i++) {
                        float data = event.values[i];
                        Variable var = getVariable(i);
                        if (var != null)
                            var.setValue(data);
                    }

                    getContext().requestUpdate();
                }

                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };
            loadVariables(node);
        }
    }

    private int getSensorType(String name) {
        Integer type = SENSOR_TYPES.get(name);
        return type == null ? 0 : type;
    }

    private Variable getVariable(int index) {
        return (Variable)mVariables.get(index);
    }

    protected Variable onLoadVariable(Element node) {
        return  new Variable(node, getContext().mVariables);
    }

    private void registerListener() {
        if (!mRegistered && mSensor != null) {
            mSensorManager.registerListener(mSensorEventListener, mSensor, mRate);
            mRegistered = true;
        }
    }

    private void unregisterListener() {
        if (mRegistered && mSensor != null) {
            mSensorManager.unregisterListener(mSensorEventListener, mSensor);
            mRegistered = false;
        }
    }

    public void finish() {
        unregisterListener();
        super.finish();
    }

    public void init() {
        super.init();
        registerListener();
    }

    public void pause() {
        super.pause();
        unregisterListener();
    }

    public void resume() {
        super.resume();
        registerListener();
    }

    private static class Variable extends VariableBinder.Variable {

        public int mIndex;

        protected void onLoad(Element node) {
            mIndex = Utils.getAttrAsInt(node, "index", 0);
        }

        public Variable(String name, String type, Variables var) {
            super(name, type, var);
        }

        public Variable(Element node, Variables var){
            super(node, var);
        }
    }
}
