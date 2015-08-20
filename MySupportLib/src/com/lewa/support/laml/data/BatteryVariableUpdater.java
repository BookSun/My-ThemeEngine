
package com.lewa.support.laml.data;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.lewa.support.laml.ScreenContext;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.IndexedNumberVariable;

public class BatteryVariableUpdater extends NotifierVariableUpdater {

    public static final String USE_TAG = "Battery";

    public static final String TAG_NAME_BATTERYFULL = "BatteryFull";

    public static final String TAG_NAME_CHARGING = "Charging";

    public static final String TAG_NAME_LOWBATTERY = "BatteryLow";

    public static final String TAG_NAME_NORMAL = "Normal";

    private IndexedNumberVariable mBatteryLevel;

    private IndexedNumberVariable mBatteryStatus;

    private String mCategory;

    private ScreenContext mContext;

    public BatteryVariableUpdater(VariableUpdaterManager m, ScreenContext context) {
        super(m, Intent.ACTION_BATTERY_CHANGED);
        mContext = context;
        mBatteryLevel = new IndexedNumberVariable(VariableNames.BATTERY_LEVEL,
                getContext().mVariables);
        mBatteryStatus = new IndexedNumberVariable(VariableNames.BATTERY_STATE,
                getContext().mVariables);
    }

    public void onNotify(Context context, Intent intent, Object o) {
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
            if (level != -1) {
                mBatteryLevel.set(level >= 100 ? 100 : level);
            }
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            if (status != -1) {
                String cate;
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        mBatteryStatus.set(VariableNames.BATTERY_STATE_CHARGING);
                        cate = TAG_NAME_CHARGING;
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        mBatteryStatus.set(VariableNames.BATTERY_STATE_FULL);
                        cate = TAG_NAME_BATTERYFULL;
                        break;
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    default:
                        if (level > 0 && level < 10) {
                            mBatteryStatus.set(VariableNames.BATTERY_STATE_LOW);
                            cate = TAG_NAME_LOWBATTERY;
                        } else {
                            mBatteryStatus.set(VariableNames.BATTERY_STATE_UNPLUGGED);
                            cate = TAG_NAME_NORMAL;
                        }
                        break;
                }
                if (mCategory == null || !mCategory.equals(cate)) {
                    mCategory = cate;
                    ScreenElementRoot root = mContext.mRoot;
                    if (root != null) {
                        try {
                            if (!mContext.isGlobalThread()) {
                                root.getClass()
                                        .getMethod("onRefreshBatteryInfo", boolean.class,
                                                boolean.class, int.class)
                                        .invoke(root, plugged || level <= 10, plugged, level);
                            }
                        } catch (Exception e) {
                        }
                    }
                    getContext().requestUpdate();
                }
            }
        }
    }
}
