/*
 * Copyright (C) 2014 The LeWa Project
 *
 * Base class for Sensor interfaces.When defining a new interface,
 * you must device the interfaces.
 * For details
 * See {@link android.hardware.SensorManager SensorManager}
 * See also {@link android.hardware.SensorEvent SensorEvent}
 * See also {@link android.hardware.SensorEventListener SensorEventListener}
 * 
 * Usage:Shake and Shake Sample Code
 * import lewa.provider.SensorProviderListener;
 * import lewa.provider.SensorProviderListener.OnShakeAndShakeListener;
 * private SensorProviderService mSensorProviderService;
 * @onCreate{mSensorProviderService = new SensorProviderService(this);}
 * @need{mSensorProviderService.registerSensorEventerListener(int type);}
 * public static final int SHAKE_SENSOR = 1;
 * public static final int TURNDOWN_SENSOR =2;
 * public static final int TURNUP_SENSOR = 3;
 * public static final int PROXIMITY_SENSOR = 4;
 * public static final int SCREENUP_SENSOR = 5;
 * public static final int SCREENDOWN_SENSOR = 6;
 * public static final int NOMATTERTURN_SENSOR =7;
 * mSensorProviderService.setOnShakeListener(new OnShakeAndShakeListener() {
 *          @Override
 *          public void onShake() {
 *              // TODO Auto-generated method stub
 *              //your codes
 *          }
 *      });
 * @onPause or onDestory{mSensorProviderService.unregisterSensorEventListener(int type);}
 * Add by wsliu@lewatek.com 2014-01-15
 */
package com.lewa.support.provider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemProperties;
import android.util.Log;

public class SensorProviderListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mOrientationSensor;
    private Sensor mProximitySensor;
    private Sensor mAdjustVolumeSensor;
    private Context mContext;

    private OnPhoneStaticScreenDownListener onPhoneStaticScreenDownListener;
    private OnAdjustVolumeListener onAdjustVolumeListener;
    private OnPhoneStaticScreenUpListener onPhoneStaticScreenUpListener;
    private OnShakeAndShakeListener onShakeAndShakeListener;
    private OnTurnPhoneDownListener onTurnPhoneDownListener;
    private OnTurnPhoneUpListener onTurnPhoneUpListener;
    private OnProximityListener onProximityListener;
    private OnNoMatterHowToTurnListener onNoMatterHowToTurnListener;
    private OnSmartCallListener onSmartCallListener;
    private OnOnlyProximityListener onOnlyProximityListener;

    public static final float GRAVITY = 10;
    public static final float DEGREE_PI = 180;
    public static final float PROXIMITY_THRESHOLD = 5.0F;
    public static final int UPDATE_INTERVAL_TIME = 1000;
    public static final int UPTATE_INTERVAL_TIME = 150;
    public static final int DEFAULT_SMART_CALL_TIME = 1000;
    public static final int ADJUST_VOLUME_TIME = 1000;
    public static final int DEFAULT_SHAKE_SPEED = 17;
    public static final float DEFAULT_TURN_DOWN = 5.0f;
    public static final int DEFAULT_TURN_DOWN_DEGREE = -5;
    public static final float DEFAULT_TURN_UP= -5.0f;
    public static final int DEFAULT_TURN_UP_DEGREE = 5;
    public static final float DEFAULT_SMART_CALL_DEGREE = 7.0f;

    public static final int SHAKE_SENSOR = 1;
    public static final int TURNDOWN_SENSOR =2;
    public static final int TURNUP_SENSOR = 3;
    public static final int PROXIMITY_SENSOR = 4;
    public static final int SCREENUP_SENSOR = 5;
    public static final int SCREENDOWN_SENSOR = 6;
    public static final int NOMATTERTURN_SENSOR =7;
    public static final int ADJUSTVOLUME_SENSOR = 8;
    public static final int SMARTCALL_SENSOR = 9;
    public static final int ONLYPROXIMITY_SENSOR = 10;

    private boolean mRegisterTurnUp;
    private boolean mRegisterTurnDown;
    private boolean mRegisterProxim;
    private boolean mRegisterShake;
    private boolean mRegisterScreenUp;
    private boolean mRegisterScreenDown;
    private boolean mRegisterNoMatterTurn;
    private boolean mRegisterAdjustVolume;
    private boolean mPhoneScreenUp;
    private boolean mPhoneScreenDown;
    private boolean mProximityActive;
    private boolean mPhoneStatusUp;
    private boolean mPhoneStatusDown;
    private boolean mTurnPhoneUp;
    private boolean mTurnPhoneDown;
    private boolean mScreenUp;
    private boolean mScreenDown;
    private boolean mTurnDown;
    private boolean mTurnUp;
    private boolean mRegisterOnlyPro;
    private long mLastUpdateTime = -1;
    private long mLastShakeTime;
    private long mLastTime = -2;
    private long mCallTime = -3;
    private float mShakeSpeed;
    private float mTurnDownValue;
    private float mTurnUpValue;
    private int mCallDelayTime;

    public SensorProviderListener(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mAdjustVolumeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if ("N1T".equals(SystemProperties.get("ro.product.device"))) {
            mShakeSpeed = PreferencesSensorProvider.getShakeDegree(context)*62;
        } else  if ("mione_plus".equals(SystemProperties.get("ro.product.device"))) {
            mShakeSpeed = PreferencesSensorProvider.getShakeDegree(context)*53;
        } else {
            mShakeSpeed = PreferencesSensorProvider.getShakeDegree(context)*108;
        }
        mTurnDownValue = PreferencesSensorProvider.getTurnDownDegree(context);
        mTurnUpValue = PreferencesSensorProvider.getTurnUpDegree(context);
        mCallDelayTime = PreferencesSensorProvider.getSmartCallDelayTime(context);
        Log.d("SensorProviderListener","mShakeSpeed:"+mShakeSpeed);
    }

    private final SensorEventListener mShakeEventListener = new SensorEventListener() {
        private float mLastX = 0.0f;
        private float mLastY = 0.0f;
        private float mLastZ = 0.0f;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentUpdateTime = System.currentTimeMillis();
                long timeInterval = currentUpdateTime - mLastShakeTime;
                if (timeInterval < UPTATE_INTERVAL_TIME) 
                    return;
                mLastShakeTime = currentUpdateTime;
                float gx = event.values[SensorManager.DATA_X];
                float gy = event.values[SensorManager.DATA_Y];
                float gz = event.values[SensorManager.DATA_Z];
                float deltaX = gx - mLastX;
                float deltaY = gy - mLastY;
                float deltaZ = gz - mLastZ;
                float speed = (float) Math.sqrt(deltaX*deltaX+deltaY*deltaY+deltaZ*deltaZ)/timeInterval*10000;
                Log.d("SensorProviderListener","speed:"+speed);
                if (speed > mShakeSpeed) {
                    if (onShakeAndShakeListener != null)
                        onShakeAndShakeListener.onShake();
                }
                mLastX = gx;
                mLastY = gy;
                mLastZ = gz;
            }
        }
    };
    private final SensorEventListener mTurnPhoneDownEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (event.values[2] > DEFAULT_TURN_DOWN) {
                    mPhoneScreenUp = true;
                } else {
                    if (mPhoneScreenUp && event.values[2] < mTurnDownValue) {
                        mPhoneScreenUp = false;
                        if (onTurnPhoneDownListener != null)
                            onTurnPhoneDownListener.onTurnPhoneDown();
                    }
                }
            }
        }
    };
    private final SensorEventListener mTurnPhoneUpEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (event.values[2] < DEFAULT_TURN_UP) {
                    mPhoneScreenDown = true;
                } else {
                    if (mPhoneScreenDown && event.values[2] > mTurnUpValue) {
                        mPhoneScreenDown = false;
                        if (onTurnPhoneUpListener != null)
                            onTurnPhoneUpListener.onTurnPhoneUp();
                    }
                }
            }
        }
    };
    private final SensorEventListener mTurnPhoneEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (event.values[2] > DEFAULT_TURN_DOWN) {
                    mScreenUp = true;
                } else {
                    if (mScreenUp && event.values[2] < mTurnDownValue) {
                        mScreenUp = false;
                        mTurnDown = true;
                    }
                }
                if (event.values[2] < DEFAULT_TURN_UP) {
                    mScreenDown = true;
                } else {
                    if (mScreenDown && event.values[2] > mTurnUpValue) {
                        mScreenDown = false;
                        mTurnUp = true;
                    }
                }
                if (mTurnUp || mTurnDown) {
                    if (onNoMatterHowToTurnListener != null)
                        onNoMatterHowToTurnListener.onTurnPhone();
                }

            }
        }
    };

    private final SensorEventListener mProximityEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                if (distance >= 0.0f && distance < PROXIMITY_THRESHOLD && distance < mProximitySensor.getMaximumRange()) {
                    if (onProximityListener != null) {
                        onProximityListener.onProximity();
                        Log.d("SensorProviderListener","onProximity......");
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    };

    private final SensorEventListener mOnlyProximityEventListener = new SensorEventListener() {
        boolean sensorFlag = false;
        boolean proximityActive = false;
        boolean proximityFlag = false;
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                sensorFlag = (distance >= 0.0f && distance < mProximitySensor.getMaximumRange());
                if (sensorFlag) {
                    proximityActive = true;
                } else {
                    proximityFlag = true;
                }
                if (proximityActive && !proximityFlag) {
                    if (onOnlyProximityListener != null)
                        onOnlyProximityListener.onOnlyProximity();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    };

    private final SensorEventListener mPhoneScreenUpEventListener = new SensorEventListener() {
        private float mLast_ox = 0.0f;
        private float mLast_oy = 0.0f;
        private float mLast_oz = 0.0f;
        private float mLast_gx = 0.0f;
        private float mLast_gy = 0.0f;
        private float mLast_gz = 0.0f;
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float gx = event.values[SensorManager.DATA_X];
                float gy = event.values[SensorManager.DATA_Y];
                float gz = event.values[SensorManager.DATA_Z];
                mLast_gx = gx;
                mLast_gy = gy;
                mLast_gz = gz;
            }
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                long currentUpdateTime = System.currentTimeMillis();
                long timeInterval = currentUpdateTime - mLastUpdateTime;
                if (timeInterval < UPDATE_INTERVAL_TIME)
                    return;
                mLastUpdateTime = currentUpdateTime;
                float ox = event.values[SensorManager.DATA_X];
                float oy = event.values[SensorManager.DATA_Y];
                float oz = event.values[SensorManager.DATA_Z];
                float deltaX = ox - mLast_ox;
                float deltalY = oy - mLast_oy;
                float deltalZ = oz - mLast_oz;
                mLast_ox = ox;
                mLast_oy = oy;
                mLast_oz = oz;
                if (mLast_gz > 9.0f && mLast_gz < 11.0f
                        && Math.abs(mLast_oy) < 15.0f
                        && Math.abs(mLast_oz) < 15.0f
                        && Math.abs(deltalY) < 2.0f && Math.abs(deltalZ) < 2.0f
                        && Math.abs(deltaX) < 10.0f) {
                    mPhoneStatusUp = true;
                    if (onPhoneStaticScreenUpListener != null)
                        onPhoneStaticScreenUpListener.onPhoneScreenUp();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
    };
    private final SensorEventListener mPhoneScreenDownEventListener = new SensorEventListener() {
        private float mLast_ox = 0.0f;
        private float mLast_oy = 0.0f;
        private float mLast_oz = 0.0f;
        private float mLast_gx = 0.0f;
        private float mLast_gy = 0.0f;
        private float mLast_gz = 0.0f;
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float gx = event.values[SensorManager.DATA_X];
                float gy = event.values[SensorManager.DATA_Y];
                float gz = event.values[SensorManager.DATA_Z];
                mLast_gx = gx;
                mLast_gy = gy;
                mLast_gz = gz;
            }
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                long currentUpdateTime = System.currentTimeMillis();
                long timeInterval = currentUpdateTime - mLastUpdateTime;
                if (timeInterval < UPDATE_INTERVAL_TIME)
                    return;
                mLastUpdateTime = currentUpdateTime;
                float ox = event.values[SensorManager.DATA_X];
                float oy = event.values[SensorManager.DATA_Y];
                float oz = event.values[SensorManager.DATA_Z];
                float deltaX = ox - mLast_ox;
                float deltalY = oy - mLast_oy;
                float deltalZ = oz - mLast_oz;
                mLast_ox = ox;
                mLast_oy = oy;
                mLast_oz = oz;
                if (mLast_gz > -11.0f && mLast_gz < -9.0f
                        && Math.abs(mLast_oy) > 165.0f
                        && Math.abs(mLast_oz) < 15.0f
                        && Math.abs(deltalY) < 2.0f && Math.abs(deltalZ) < 2.0f
                        && Math.abs(deltaX) < 10.0f) {
                    mPhoneStatusDown = true;
                    if (onPhoneStaticScreenDownListener != null)
                        onPhoneStaticScreenDownListener.onPhoneScreenDown();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
    };

    private final SensorEventListener mAdjustVolumeEventListener = new SensorEventListener() {
        private boolean adjustVolume = false;
        private boolean proximityActive = false;
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                if (distance > 0.0f && distance <= mAdjustVolumeSensor.getMaximumRange()) {
                    adjustVolume = true;
                } else {
                    proximityActive = true;
                }
                if (adjustVolume && !proximityActive){
                    if (onAdjustVolumeListener != null) {
                        onAdjustVolumeListener.onVolume();
                        Log.d("SensorProviderListener","proximityActive:");
                    }
                }
            }
        }
    };
    private final SensorEventListener mSmartCallEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
        boolean changeFlag = false;
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                changeFlag = (distance >= 0.0f && distance < PROXIMITY_THRESHOLD && distance < mProximitySensor.getMaximumRange());
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentUpdateTime = System.currentTimeMillis();
                long timeInterval = currentUpdateTime - mCallTime;
                if (timeInterval < mCallDelayTime)
                    return;
                mCallTime = currentUpdateTime;
                float gy = event.values[SensorManager.DATA_Y];
                if (changeFlag && gy > DEFAULT_SMART_CALL_DEGREE) {
                    if (onSmartCallListener != null) {
                        onSmartCallListener.onSmartCall();
                    }
                }
            }
        }
    };

    /*
     * Before using the Sensor interfaces,you must register first.
     *     public static final int SHAKE_SENSOR = 1;
     *     public static final int TURNDOWN_SENSOR =2;
     *     public static final int TURNUP_SENSOR = 3;
     *     public static final int PROXIMITY_SENSOR = 4;
     *     public static final int SCREENUP_SENSOR = 5;
     *     public static final int SCREENDOWN_SENSOR = 6;
     *     public static final int NOMATTERTURN_SENSOR = 7;
     *     public static final int ADJUSTVOLUME_SENSOR = 8;
     *     public static final int SMARTCALL_SENSOR = 9;
     */
    public void registerSensorEventerListener(int type) {
        switch (type) {
        case SHAKE_SENSOR://1
            if (mAccelerometerSensor != null) {
                mRegisterShake = mSensorManager.registerListener(
                        mShakeEventListener, mAccelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case TURNDOWN_SENSOR://2
            if (mAccelerometerSensor != null) {
                mRegisterTurnDown = mSensorManager.registerListener(
                        mTurnPhoneDownEventListener, mAccelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case TURNUP_SENSOR://3
            if (mAccelerometerSensor != null) {
                mRegisterTurnUp = mSensorManager.registerListener(
                        mTurnPhoneUpEventListener, mAccelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case PROXIMITY_SENSOR://4
            if (mProximitySensor != null) {
                mRegisterProxim = mSensorManager.registerListener(
                        mProximityEventListener, mProximitySensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case SCREENUP_SENSOR://5
            if (mAccelerometerSensor != null && mOrientationSensor != null) {
                mRegisterScreenUp = mSensorManager.registerListener(mPhoneScreenUpEventListener,
                        mOrientationSensor,SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mPhoneScreenUpEventListener,
                        mAccelerometerSensor,SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case SCREENDOWN_SENSOR://6
            if (mAccelerometerSensor != null && mOrientationSensor != null) {
                mRegisterScreenDown = mSensorManager.registerListener(mPhoneScreenDownEventListener,
                        mOrientationSensor,SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mPhoneScreenDownEventListener,
                        mAccelerometerSensor,SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case NOMATTERTURN_SENSOR://7
            if (mAccelerometerSensor != null) {
                mRegisterNoMatterTurn = mSensorManager.registerListener(
                        mTurnPhoneEventListener, mAccelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case ADJUSTVOLUME_SENSOR://8
            if (mAdjustVolumeSensor != null) {
                mRegisterAdjustVolume = mSensorManager.registerListener(mAdjustVolumeEventListener,
                    mAdjustVolumeSensor,SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case SMARTCALL_SENSOR://9
            if (mProximitySensor != null && mAccelerometerSensor != null) {
                mSensorManager.registerListener(mSmartCallEventListener,mProximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mSmartCallEventListener,mAccelerometerSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            }
            break;
        case ONLYPROXIMITY_SENSOR://10
            if (mProximitySensor != null) {
                mRegisterOnlyPro = mSensorManager.registerListener(mOnlyProximityEventListener, mProximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            }
        default:
            break;
        }
    }

    /*
     * When you finish the class,you must unregister the seneor
     */
    public void unregisterSensorEventerListener(int type) {
        switch (type) {
        case SHAKE_SENSOR://1
            if (mRegisterShake)
                mSensorManager.unregisterListener(mShakeEventListener);
            break;
        case TURNDOWN_SENSOR://2
            if (mRegisterTurnDown)
                mSensorManager.unregisterListener(mTurnPhoneDownEventListener);
            break;
        case TURNUP_SENSOR://3
            if (mRegisterTurnUp)
                mSensorManager.unregisterListener(mTurnPhoneUpEventListener);
            break;
        case PROXIMITY_SENSOR://4
            if (mRegisterProxim)
                mSensorManager.unregisterListener(mProximityEventListener);
            break;
        case SCREENUP_SENSOR://5
            if (mRegisterScreenUp)
                mSensorManager.unregisterListener(mPhoneScreenUpEventListener);
            break;
        case SCREENDOWN_SENSOR://6
            if (mRegisterScreenDown)
                mSensorManager.unregisterListener(mPhoneScreenDownEventListener);
            break;
        case NOMATTERTURN_SENSOR://7
            if (mRegisterNoMatterTurn)
                mSensorManager.unregisterListener(mTurnPhoneEventListener);
            break;
        case ADJUSTVOLUME_SENSOR://8
            if (mRegisterAdjustVolume)
                mSensorManager.unregisterListener(mAdjustVolumeEventListener);
            break;
        case SMARTCALL_SENSOR://9
                mSensorManager.unregisterListener(mSmartCallEventListener);
                break;
        case ONLYPROXIMITY_SENSOR://10
            if (mRegisterOnlyPro)
                mSensorManager.unregisterListener(mOnlyProximityEventListener);
            break;
        default:
            break;
        }
    }
    /*
     * Shake interface
     * Function:Realize the phone shake and shake
     */
    public void setOnShakeListener(OnShakeAndShakeListener listener) {
        onShakeAndShakeListener = listener;
    }

    public interface OnShakeAndShakeListener {
        public void onShake();
    }

    /*
     * Phone Static and Screen Up interface
     * Function:Realize the phone status is static and screen up
     */
    public void setOnPhoneStatusListener(OnPhoneStaticScreenUpListener listener) {
        onPhoneStaticScreenUpListener = listener;
    }

    public interface OnPhoneStaticScreenUpListener {
        public void onPhoneScreenUp();
    }

    /*
     * Phone Static and Screen Down interface
     * Function:Realize the phone status is static and screen down
     */
    public void setOnPhoneScreenListener(
            OnPhoneStaticScreenDownListener listener) {
        onPhoneStaticScreenDownListener = listener;
    }

    public interface OnPhoneStaticScreenDownListener {
        public void onPhoneScreenDown();
    }

    /*
     * Phone proximity interface
     * Function:Realize the phone proximity is true
     */
    public void setOnProximityListener(OnProximityListener listener) {
        onProximityListener = listener;
    }

    public interface OnProximityListener {
        public void onProximity();
    }

    /*
     * Turn Phone Down interface
     * Function:Realize the phone turn from up to down
     */
    public void setOnTurnPhoneDownListener(OnTurnPhoneDownListener listener) {
        onTurnPhoneDownListener = listener;
    }

    public interface OnTurnPhoneDownListener {
        public void onTurnPhoneDown();
    }

    /*
     * Turn Phone Up interface
     * Function:Realize the phone turn from down to up
     */
    public void setOnTurnPhoneUpListener(OnTurnPhoneUpListener listener) {
        onTurnPhoneUpListener = listener;
    }

    public interface OnTurnPhoneUpListener {
        public void onTurnPhoneUp();
    }

    /*
     * Turn Phone interface
     * Function:Realize the phone turn no matter how to turn
     */
    public void setOnNoMatterHowToTurnListener(OnNoMatterHowToTurnListener listener) {
        onNoMatterHowToTurnListener = listener;
    }

    public interface OnNoMatterHowToTurnListener {
        public void onTurnPhone();
    }

    /*
     *Add for adjustvolume
     */
    public void setOnAdjustVolumeListener(OnAdjustVolumeListener listener) {
        onAdjustVolumeListener = listener;
    }
    public interface OnAdjustVolumeListener {
        public void onVolume();
    }

    /*
     *Add for smart call
     */
    public void setOnSmartCallListener(OnSmartCallListener listener) {
        onSmartCallListener = listener;
    }
    public interface OnSmartCallListener {
        public void onSmartCall();
    }
    /*
     *Proximity Interface
     *Function:Only the proximity sensor value=0 is effection.
     */
    public void setOnOnlyProximityListener (OnOnlyProximityListener listener) {
        onOnlyProximityListener = listener;
    }
    public interface OnOnlyProximityListener {
        public void onOnlyProximity();
    }
}
