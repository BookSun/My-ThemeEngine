
package com.lewa.support.laml;

import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.*;
import android.os.Process;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.VariableBinder;
import com.lewa.support.laml.data.VariableNames;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.elements.ListScreenElement;
import com.lewa.support.laml.elements.ScreenElement;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.laml.util.Task;
import com.lewa.support.laml.util.Utils;
import com.lewa.support.laml.util.Variable;

import org.w3c.dom.Element;

public abstract class ActionCommand {
    private static final boolean DBG = true;

    private static final String LOG_TAG = "ActionCommand";

    private static final int STATE_DISABLED = 0;

    private static final int STATE_ENABLED = 1;

    private static final int STATE_INTERMEDIATE = 5;

    private static final int STATE_TURNING_OFF = 3;

    private static final int STATE_TURNING_ON = 2;

    private static final int STATE_UNKNOWN = 4;

    public static final String TAG_NAME = "Command";

    private static final Handler mHandler = new Handler();

    protected ScreenElementRoot mRoot;

    public ActionCommand(ScreenElementRoot root) {
        mRoot = root;
    }

    protected static ActionCommand create(ScreenElementRoot root, String target, String value) {
        if (!TextUtils.isEmpty(target) && !TextUtils.isEmpty(value)) {
            Variable targetObj = new Variable(target);
            if (targetObj.getObjName() != null)
                return PropertyCommand.create(root, target, value);
            String property = targetObj.getPropertyName();
            if (RingModeCommand.PROPERTY_NAME.equals(property))
                return new RingModeCommand(root, value);
            if (WifiSwitchCommand.PROPERTY_NAME.equals(property))
                return new WifiSwitchCommand(root, value);
            if (DataSwitchCommand.PROPERTY_NAME.equals(property))
                return new DataSwitchCommand(root, value);
            if (BluetoothSwitchCommand.PROPERTY_NAME.equals(property))
                return new BluetoothSwitchCommand(root, value);
            if (UsbStorageSwitchCommand.PROPERTY_NAME.equals(property))
                return new UsbStorageSwitchCommand(root, value);
        }
        return null;
    }

    public static ActionCommand create(Element ele, ScreenElementRoot root) {
        if (ele == null)
            return null;
        Expression condition = Expression.build(ele.getAttribute("condition"));
        long delay = Utils.getAttrAsLong(ele, "delay", 0);
        String tag = ele.getNodeName();
        ActionCommand ret = null;
        if (ActionCommand.TAG_NAME.equals(tag)) {
            String target = ele.getAttribute("target");
            String value = ele.getAttribute("value");
            ret = create(root, target, value);
        } else if (VariableAssignmentCommand.TAG_NAME.equals(tag)) {
            ret = new VariableAssignmentCommand(root, ele);
        } else if (VariableBinderCommand.TAG_NAME.equals(tag)) {
            ret = new VariableBinderCommand(root, ele);
        } else if (IntentCommand.TAG_NAME.equals(tag)) {
            ret = new IntentCommand(root, ele);
        } else if (SoundCommand.TAG_NAME.equals(tag)) {
            ret = new SoundCommand(root, ele);
        } else if (ExternCommand.TAG_NAME.equals(tag)) {
            ret = new ExternCommand(root, ele);
        } else if (ListCommand.TAG_NAME.equals(tag)) {
            ret = new ListCommand(root, ele);
        }
        if (ret != null) {
            if (delay > 0)
                ret = new DelayCommand(ret, delay);
            if (condition != null)
                ret = new ConditionCommand(ret, condition);
        }
        return ret;
    }

    protected abstract void doPerform();

    public void finish() {
    }

    protected ScreenElementRoot getRoot() {
        return mRoot;
    }

    public void init() {
    }

    public void pause() {
    }

    public void perform() {
        doPerform();
        mRoot.requestUpdate();
    }

    public void resume() {
    }

    private static class AnimationProperty extends PropertyCommand {

        public static final String PROPERTY_NAME = "animation";

        private boolean mIsPlay;

        public void doPerform() {
            if (mIsPlay)
                mTargetElement.reset();
        }

        protected AnimationProperty(ScreenElementRoot root, Variable targetObj, String value) {
            super(root, targetObj, value);
            if (value.equalsIgnoreCase("play"))
                mIsPlay = true;
        }
    }

    private static class BluetoothSwitchCommand extends NotificationReceiver {

        public static final String PROPERTY_NAME = "Bluetooth";

        private BluetoothAdapter mBluetoothAdapter;

        private boolean mBluetoothEnable;

        private boolean mBluetoothEnabling;

        private OnOffCommandHelper mOnOffHelper;

        private boolean ensureBluetoothAdapter() {
            if (mBluetoothAdapter == null)
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return mBluetoothAdapter != null;
        }

        protected void doPerform() {
            if (ensureBluetoothAdapter()) {
                if (mOnOffHelper.mIsToggle) {
                    if (mBluetoothEnable) {
                        mBluetoothAdapter.disable();
                        mBluetoothEnabling = false;
                    } else {
                        mBluetoothAdapter.enable();
                        mBluetoothEnabling = true;
                    }
                } else if (!mBluetoothEnabling && mBluetoothEnable != mOnOffHelper.mIsOn) {
                    if (mOnOffHelper.mIsOn) {
                        mBluetoothAdapter.enable();
                        mBluetoothEnabling = true;
                    } else {
                        mBluetoothAdapter.disable();
                        mBluetoothEnabling = false;
                    }
                }
                update();
            }
        }

        protected void update() {
            if (ensureBluetoothAdapter()) {
                mBluetoothEnable = mBluetoothAdapter.isEnabled();
                if (mBluetoothEnable) {
                    mBluetoothEnabling = false;
                    updateState(STATE_ENABLED);
                } else {
                    updateState(mBluetoothEnabling ? STATE_TURNING_ON : STATE_DISABLED);
                }
            }
        }

        public BluetoothSwitchCommand(ScreenElementRoot root, String value) {
            super(root, VariableNames.BLUETOOTH_STATE, BluetoothAdapter.ACTION_STATE_CHANGED);
            mOnOffHelper = new OnOffCommandHelper(value);
        }
    }

    private static class ConditionCommand extends ActionCommand {

        private ActionCommand mCommand;

        private Expression mCondition;

        protected void doPerform() {
            if (mCondition.evaluate(mRoot.getContext().mVariables) > 0)
                mCommand.perform();
        }

        public void init() {
            mCommand.init();
        }

        public ConditionCommand(ActionCommand command, Expression condition) {
            super(command.getRoot());
            mCommand = command;
            mCondition = condition;
        }
    }

    private static class DataSwitchCommand extends NotificationReceiver {

        public static final String PROPERTY_NAME = "Data";

        private boolean mApnEnable;

        private ConnectivityManager mCm;

        private OnOffCommandHelper mOnOffHelper;

        private boolean ensureConnectivityManager() {
            if (mCm == null)
                mCm = (ConnectivityManager) mRoot.getContext().mContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
            return mCm != null;
        }

        protected void doPerform() {
            if (ensureConnectivityManager()) {
                boolean flag = mOnOffHelper.mIsToggle ? !mApnEnable : mOnOffHelper.mIsOn;
                if (mApnEnable != flag) {
                    // NingYI For Launcher
                    // mCm.setMobileDataEnabled(flag);
                }
            }
        }

        protected void update() {
            if (ensureConnectivityManager()) {
                mApnEnable = mCm.getMobileDataEnabled();
                updateState(mApnEnable ? STATE_ENABLED : STATE_DISABLED);
            }
        }

        public DataSwitchCommand(ScreenElementRoot root, String value) {
            super(root, VariableNames.DATA_STATE, NotifierManager.TYPE_MOBILE_DATA);
            mOnOffHelper = new OnOffCommandHelper(value);
        }
    }

    private static class DelayCommand extends ActionCommand {

        private ActionCommand mCommand;

        private long mDelay;

        protected void doPerform() {
            mRoot.postDelayed(new Runnable() {
                public void run() {
                    mCommand.perform();
                }
            }, mDelay);
        }

        public void init() {
            mCommand.init();
        }

        public DelayCommand(ActionCommand command, long delay) {
            super(command.getRoot());
            mCommand = command;
            mDelay = delay;
        }
    }

    private static class IntentCommand extends ActionCommand {

        public static final String TAG_NAME = "IntentCommand";

        private Intent mIntent;

        private Task mTask;

        private ArrayList<Extra> mExtraList = new ArrayList<Extra> ();
        private Uri mUri;
        private Expression mUriExp;
        private boolean mIsBroadcast;

        public IntentCommand(ScreenElementRoot root, Element ele) {
            super(root);
            mTask = Task.load(ele);
            mIsBroadcast = Boolean.parseBoolean(ele.getAttribute("broadcast"));
            String str = ele.getAttribute("uri");
            if (!TextUtils.isEmpty(str))
              this.mUri = Uri.parse(str);
            this.mUriExp = Expression.build(ele.getAttribute("uriExp"));
            loadExtras(ele);
        }

        protected void doPerform() {
            if (mIntent != null) {
                try {
                    if(mUri == null && mUriExp != null){
                        String str = this.mUriExp.evaluateStr(this.mRoot.getContext().mVariables);
                        mIntent.setData(Uri.parse(str));
                    }
                    putExtras(mIntent);
                    if(mIsBroadcast){
                        mRoot.getContext().mContext.sendBroadcast(this.mIntent);
                    } else {
                        mRoot.getContext().mContext.startActivity(mIntent);
                    }
                } catch (ActivityNotFoundException e) {
                    Log.e(LOG_TAG, e.toString());
                    if(mTask != null){
                        Intent intent = new Intent(mTask.action);
                        mRoot.getContext().mContext.sendBroadcast(intent);
                    }else{
                        mRoot.getContext().mContext.sendBroadcast(mIntent);
                    }
                }
            }
        }

        public void init() {
            Task configTask = mRoot.findTask(mTask.id);
            if (configTask != null && !TextUtils.isEmpty(configTask.action))
                mTask = configTask;
            if (!TextUtils.isEmpty(mTask.action)) {
                mIntent = new Intent(mTask.action);
                if (!TextUtils.isEmpty(mTask.type))
                    mIntent.setType(mTask.type);
                if (!TextUtils.isEmpty(mTask.category))
                    mIntent.addCategory(mTask.category);
                if (!TextUtils.isEmpty(mTask.packageName) && !TextUtils.isEmpty(mTask.className))
                    mIntent.setComponent(new ComponentName(mTask.packageName, mTask.className));
                int flag = Constants.INTENT_FLAG;
                if (!mTask.anim)
                    flag |= Intent.FLAG_ACTIVITY_NO_ANIMATION;
                if (mUri != null)
                    mIntent.setData(mUri);
                if (!mIsBroadcast)
                    mIntent.setFlags(flag);
            }
        }

        // Begin, added by yljiang@lewatek.com 2014-06-11
        private void loadExtras(final Element element){
            Utils.traverseXmlElementChildren(element, "Extra", new Utils.XmlTraverseListener(){
                @Override
                public void onChild(Element element) {
                    addExtra(new Extra(element));
                }
            });
        }
        protected void addExtra(Extra extra){
            mExtraList.add(extra);
        }

        private void putExtras(Intent mIntent){
            if(this.mExtraList != null && !mExtraList .isEmpty()){
                Iterator<Extra> localIterator = this.mExtraList.iterator();
                while(true){
                    if (!localIterator.hasNext())
                        return;
                    Extra localExtra = (Extra)localIterator.next();
                    if(!localExtra.isConditionTrue()){
                        mIntent.removeExtra(localExtra.getName());
                    } else {
                        switch (localExtra.mType){
                            case STRING:
                                this.mIntent.putExtra(localExtra.getName(), localExtra.getString());
                                break;
                            case INT:
                                this.mIntent.putExtra(localExtra.getName(), (int)localExtra.getDouble());
                                break;
                            case LONG:
                                this.mIntent.putExtra(localExtra.getName(), (long)localExtra.getDouble());
                                break;
                            case FLOAT:
                                this.mIntent.putExtra(localExtra.getName(), (float)localExtra.getDouble());
                                break;
                            case DOUBLE:
                                this.mIntent.putExtra(localExtra.getName(), localExtra.getDouble());
                                break;
                        }
                    }
                }
            }
        }

        public static enum Type {
        INT,LONG ,FLOAT, DOUBLE ,STRING
    }

    private class Extra {

        public static final String                 TAG_NAME = "Extra";
        private Expression                         mCondition;
        private Expression                         mExpression;
        private String                             mName;
        protected ActionCommand.IntentCommand.Type mType    = ActionCommand.IntentCommand.Type.DOUBLE;

        public Extra(Element paramElement){
            load(paramElement);
        }

        private void load(Element paramElement) {
            if (paramElement == null) {
                Log.e("ActionCommand", "node is null");
                return;
            }
            this.mName = paramElement.getAttribute("name");
            String str = paramElement.getAttribute("type");
            this.mType = ActionCommand.IntentCommand.Type.DOUBLE;

            if ("string".equalsIgnoreCase(str)) {
                mType = ActionCommand.IntentCommand.Type.STRING;
            }
            if (("int".equalsIgnoreCase(str)) || ("integer".equalsIgnoreCase(str))) {
                this.mType = ActionCommand.IntentCommand.Type.INT;
            }
            if ("long".equalsIgnoreCase(str)) {
                this.mType = ActionCommand.IntentCommand.Type.LONG;
            }
            if ("float".equalsIgnoreCase(str)) {
                this.mType = ActionCommand.IntentCommand.Type.FLOAT;
            }
            this.mExpression = Expression.build(paramElement.getAttribute("expression"));
            if (this.mExpression == null)
                Log.e("ActionCommand", "invalid expression in IntentCommand");
            this.mCondition = Expression.build(paramElement.getAttribute("condition"));
        }

        public double getDouble() {
            if (this.mExpression == null)
                return 0;
            return mExpression.evaluate(mRoot.getContext().mVariables);
        }

        public String getName() {
            return this.mName;
        }

        public String getString() {
            if (this.mExpression == null)
                return null;
            return mExpression.evaluateStr(mRoot.getContext().mVariables);
        }

        public boolean isConditionTrue() {
           return  mCondition == null || mCondition.evaluate(mRoot.getContext().mVariables) > 0;
        }
    }
    // End
}

    private static class ModeToggleHelper {

        private int mCurModeIndex;

        private int mCurToggleIndex;

        private boolean mToggle;

        private boolean mToggleAll;

        private ArrayList<Integer> mModeIds = new ArrayList<Integer>();

        private ArrayList<String> mModeNames = new ArrayList<String>();

        private ArrayList<Integer> mToggleModes = new ArrayList<Integer>();

        private int findMode(String name) {
            for (int i = 0, N = mModeNames.size(); i < N; i++)
                if (mModeNames.get(i).equals(name))
                    return i;
            return -1;
        }

        public void addMode(String mode, int id) {
            mModeNames.add(mode);
            mModeIds.add(Integer.valueOf(id));
        }

        public boolean build(String value) {
            int index = findMode(value);
            if (index >= 0) {
                mCurModeIndex = index;
                return true;
            }
            if ("toggle".equals(value)) {
                mToggleAll = true;
                return true;
            }
            String modes[] = value.split(",");
            for (int i = 0, N = modes.length; i < N; i++) {
                int ind = findMode(modes[i]);
                if (ind < 0)
                    return false;
                mToggleModes.add(Integer.valueOf(ind));
            }

            mToggle = true;
            return true;
        }

        public void click() {
            if (mToggle) {
                int j = 1 + mCurToggleIndex;
                mCurToggleIndex = j;
                mCurToggleIndex = j % mToggleModes.size();
                mCurModeIndex = ((Integer) mToggleModes.get(mCurToggleIndex)).intValue();
            } else if (mToggleAll) {
                int i = 1 + mCurModeIndex;
                mCurModeIndex = i;
                mCurModeIndex = i % mModeNames.size();
            }
        }

        public int getModeId() {
            return ((Integer) mModeIds.get(mCurModeIndex)).intValue();
        }

        public String getModeName() {
            return mModeNames.get(mCurModeIndex);
        }
    }

    private static abstract class NotificationReceiver extends StatefulActionCommand implements
            NotifierManager.OnNotifyListener {

        private NotifierManager mNotifierManager;

        private String mType;

        protected void asyncUpdate() {
            ActionCommand.mHandler.post(new Runnable() {
                public void run() {
                    update();
                }
            });
        }

        public void finish() {
            mNotifierManager.releaseNotifier(mType, this);
        }

        public void init() {
            update();
            mNotifierManager.acquireNotifier(mType, this);
        }

        public void onNotify(Context context, Intent intent, Object o) {
            asyncUpdate();
        }

        public void pause() {
            mNotifierManager.pause(mType, this);
        }

        public void resume() {
            update();
            mNotifierManager.resume(mType, this);
        }

        protected abstract void update();

        public NotificationReceiver(ScreenElementRoot root, String stateName, String type) {
            super(root, stateName);
            mType = type;
            mNotifierManager = NotifierManager.getInstance(mRoot.getContext().mContext);
        }
    }

    private static class OnOffCommandHelper {

        protected boolean mIsOn;

        protected boolean mIsToggle;

        public OnOffCommandHelper(String value) {
            if (value.equalsIgnoreCase("toggle")) {
                mIsToggle = true;
            } else {
                if (value.equalsIgnoreCase("on")) {
                    mIsOn = true;
                }
                if (value.equalsIgnoreCase("off")) {
                    mIsOn = false;
                }
            }
        }
    }

    public static abstract class PropertyCommand extends ActionCommand {

        protected ScreenElement mTargetElement;

        private Variable mTargetObj;

        public static PropertyCommand create(ScreenElementRoot root, String target, String value) {
            Variable t = new Variable(target);
            if (VisibilityProperty.PROPERTY_NAME.equals(t.getPropertyName()))
                return new VisibilityProperty(root, t, value);
            if (AnimationProperty.PROPERTY_NAME.equals(t.getPropertyName()))
                return new AnimationProperty(root, t, value);
            else
                return null;
        }

        public void perform() {
            if (mTargetObj == null)
                return;
            if (mTargetElement == null) {
                mTargetElement = mRoot.findElement(mTargetObj.getObjName());
                if (mTargetElement == null) {
                    Log.w(LOG_TAG,
                            "could not find PropertyCommand target, name: "
                                    + mTargetObj.getObjName());
                    mTargetObj = null;
                }
            }
            doPerform();
        }

        protected PropertyCommand(ScreenElementRoot root, Variable targetObj, String value) {
            super(root);
            mTargetObj = targetObj;
        }
    }

    private static class RingModeCommand extends NotificationReceiver {

        public static final String PROPERTY_NAME = "RingMode";

        private AudioManager mAudioManager;

        private ModeToggleHelper mToggleHelper;

        protected void doPerform() {
            if (mAudioManager != null) {
                mToggleHelper.click();
                int mode = mToggleHelper.getModeId();
                mAudioManager.setRingerMode(mode);
                updateState(mode);
                if (DBG)
                    Log.d(LOG_TAG, "ModeName: " + mToggleHelper.getModeName());
            }
        }

        protected void update() {
            if (mAudioManager == null)
                mAudioManager = (AudioManager) mRoot.getContext().mContext
                        .getSystemService(Context.AUDIO_SERVICE);
            if (mAudioManager != null) {
                updateState(mAudioManager.getRingerMode());
            }
        }

        public RingModeCommand(ScreenElementRoot root, String value) {
            super(root, VariableNames.RING_MODE, AudioManager.RINGER_MODE_CHANGED_ACTION);
            mToggleHelper = new ModeToggleHelper();
            mToggleHelper.addMode("normal", AudioManager.RINGER_MODE_NORMAL);
            mToggleHelper.addMode("vibrate", AudioManager.RINGER_MODE_VIBRATE);
            mToggleHelper.addMode("silent", AudioManager.RINGER_MODE_SILENT);
            if (!mToggleHelper.build(value))
                Log.e(LOG_TAG, "invalid ring mode command value: " + value);
        }
    }

    private static class SoundCommand extends ActionCommand {

        public static final String TAG_NAME = "SoundCommand";

        private boolean mKeepCur;

        private boolean mLoop;

        private String mSound;

        private Expression mVolumeExp;

        private Expression mStopExp;

        protected void doPerform() {
            Variables v = mRoot.getContext().mVariables;
            mRoot.playSound(mSound, new SoundManager.SoundOptions(mSound, mKeepCur, mLoop,
                    mVolumeExp == null ? 0 : (float) mVolumeExp.evaluate(v),
                    mStopExp == null ? false : mStopExp.evaluate(v) == 1));
        }

        public SoundCommand(ScreenElementRoot root, Element ele) {
            super(root);
            mSound = ele.getAttribute("sound");
            mKeepCur = Boolean.parseBoolean(ele.getAttribute("keepCur"));
            mLoop = Boolean.parseBoolean(ele.getAttribute("loop"));
            mVolumeExp = Expression.build(ele.getAttribute("volume"));
            mStopExp = Expression.build(ele.getAttribute("stop"));
            if (mVolumeExp == null)
                Log.e(LOG_TAG, "invalid expression in SoundCommand");
        }
    }

    public static abstract class StateTracker {

        private Boolean mActualState;

        private boolean mDeferredStateChangeRequestNeeded;

        private boolean mInTransition;

        private Boolean mIntendedState;

        public abstract int getActualState(Context context);

        public final int getTriState(Context context) {
            if (mInTransition)
                return STATE_INTERMEDIATE;
            switch (getActualState(context)) {
                case STATE_ENABLED:
                    return STATE_ENABLED;
                case STATE_DISABLED:
                    return STATE_DISABLED;
            }
            return STATE_INTERMEDIATE;
        }

        public final boolean isTurningOn() {
            return mIntendedState != null && mIntendedState.booleanValue();
        }

        public abstract void onActualStateChange(Context context, Intent intent);

        protected abstract void requestStateChange(Context context, boolean flag);

        protected final void setCurrentState(Context context, int newState) {
            boolean wasInTransition = mInTransition;
            switch (newState) {
                case STATE_TURNING_OFF:
                    mInTransition = true;
                    mActualState = true;
                    break;

                case STATE_TURNING_ON:
                    mInTransition = true;
                    mActualState = false;
                    break;

                case STATE_ENABLED:
                    mInTransition = false;
                    mActualState = true;
                    break;

                case STATE_DISABLED:
                    mInTransition = false;
                    mActualState = false;
                    break;
            }
            if (wasInTransition && !mInTransition && mDeferredStateChangeRequestNeeded) {
                Log.v(LOG_TAG, "processing deferred state change");
                if (mActualState != null && mIntendedState != null
                        && mIntendedState.equals(mActualState))
                    Log.v(LOG_TAG, "... but intended state matches, so no changes.");
                else if (mIntendedState != null) {
                    mInTransition = true;
                    requestStateChange(context, mIntendedState.booleanValue());
                }
                mDeferredStateChangeRequestNeeded = false;
            }
        }

        public final void toggleState(Context context) {
            int currentState = getTriState(context);
            switch (currentState) {
                case STATE_ENABLED:
                case STATE_TURNING_ON:
                case STATE_TURNING_OFF:
                case STATE_UNKNOWN:
                default:
                    mIntendedState = false;
                    break;

                case STATE_INTERMEDIATE:
                    mIntendedState = mIntendedState == null ? false : !mIntendedState;
                    break;

                case STATE_DISABLED:
                    mIntendedState = true;
                    break;
            }
            if (mInTransition) {
                mDeferredStateChangeRequestNeeded = true;
            } else {
                mInTransition = true;
                requestStateChange(context, mIntendedState);
            }
        }

        public StateTracker() {
            mInTransition = false;
            mActualState = null;
            mIntendedState = null;
            mDeferredStateChangeRequestNeeded = false;
        }
    }

    private static abstract class StatefulActionCommand extends ActionCommand {

        private IndexedNumberVariable mVar;

        protected final void updateState(int state) {
            if (mVar != null) {
                mVar.set(state);
                mRoot.getContext().requestUpdate();
            }
        }

        public StatefulActionCommand(ScreenElementRoot root, String stateName) {
            super(root);
            mVar = new IndexedNumberVariable(stateName, mRoot.getContext().mVariables);
        }
    }

    private static class UsbStorageSwitchCommand extends NotificationReceiver {

        public static final String PROPERTY_NAME = "UsbStorage";

        private boolean mConnected;

        private OnOffCommandHelper mOnOffHelper;

        private StorageManager mStorageManager;

        protected void doPerform() {
            if (mStorageManager != null) {
                boolean enabled = mStorageManager.isUsbMassStorageEnabled();
                final boolean on = mOnOffHelper.mIsToggle ? !enabled : mOnOffHelper.mIsOn;
                updateState(STATE_TURNING_OFF);
                new Thread() {
                    public void run() {
                        if (on)
                            mStorageManager.enableUsbMassStorage();
                        else
                            mStorageManager.disableUsbMassStorage();
                        updateState(on ? STATE_TURNING_ON : STATE_ENABLED);
                    }
                }.start();
            }
        }

        public void onNotify(Context context, Intent intent, Object o) {
            mConnected = intent.getExtras().getBoolean("connected");
            super.onNotify(context, intent, o);
        }

        protected void update() {
            if (mStorageManager == null) {
                mStorageManager = (StorageManager) mRoot.getContext().mContext
                        .getSystemService(Context.STORAGE_SERVICE);
                if (mStorageManager == null) {
                    Log.w(LOG_TAG, "Failed to get StorageManager");
                    return;
                }
            }
            boolean enabled = mStorageManager.isUsbMassStorageEnabled();
            updateState(mConnected ? (enabled ? STATE_TURNING_ON : STATE_ENABLED) : STATE_DISABLED);
        }

        public UsbStorageSwitchCommand(ScreenElementRoot root, String value) {
            super(root, VariableNames.USB_MODE, UsbManager.ACTION_USB_STATE);
            mOnOffHelper = new OnOffCommandHelper(value);
        }
    }

    private static class VariableAssignmentCommand extends ActionCommand {

        public static final String TAG_NAME = "VariableCommand";

        private Expression mExpression;

        private IndexedNumberVariable mNumVariable;

        private IndexedStringVariable mStrVariable;

        protected void doPerform() {
            if (mExpression != null) {
                if (mNumVariable != null) {
                    mNumVariable.set(mExpression.evaluate(mRoot.getContext().mVariables));
                    return;
                }
                if (mStrVariable != null) {
                    mStrVariable.set(mExpression.evaluateStr(mRoot.getContext().mVariables));
                    return;
                }
            }
        }

        public VariableAssignmentCommand(ScreenElementRoot root, Element ele) {
            super(root);
            String name = ele.getAttribute("name");
            String expression = ele.getAttribute("expression");
            String type = ele.getAttribute("type");
            Variable v = new Variable(name);
            if (type.equals("string"))
                mStrVariable = new IndexedStringVariable(v.getObjName(), v.getPropertyName(),
                        root.getContext().mVariables);
            else
                mNumVariable = new IndexedNumberVariable(v.getObjName(), v.getPropertyName(),
                        root.getContext().mVariables);
            mExpression = Expression.build(expression);
            if (mExpression == null)
                Log.e(LOG_TAG, "invalid expression in VariableAssignmentCommand");
        }
    }

    private static class VariableBinderCommand extends ActionCommand {

        public static final String TAG_NAME = "BinderCommand";

        private VariableBinder mBinder;

        private Command mCommand;

        private String mName;

        protected void doPerform() {
            if (mBinder != null)
                switch (mCommand) {
                    case Refresh:
                        mBinder.refresh();
                    default:
                        break;
                }
        }

        public void init() {
            mBinder = mRoot.findBinder(mName);
        }

        public VariableBinderCommand(ScreenElementRoot root, Element ele) {
            super(root);
            mCommand = Command.Invalid;
            mRoot = root;
            mName = ele.getAttribute("name");
            String command = ele.getAttribute("command");
            if (command.equals("refresh"))
                mCommand = Command.Refresh;
        }
    }

    private static class ExternCommand extends ActionCommand {

        public static final String TAG_NAME = "ExternCommand";

        private String mCommand;

        private Expression mNumParaExp;

        private Expression mStrParaExp;

        protected void doPerform() {
            Variables v = mRoot.getContext().mVariables;
            mRoot.issueExternCommand(mCommand,
                    mNumParaExp == null ? null : mNumParaExp.evaluate(v),
                    mStrParaExp == null ? null : mStrParaExp.evaluateStr(v));
        }

        public ExternCommand(ScreenElementRoot root, Element ele) {
            super(root);
            mCommand = ele.getAttribute("command");
            mNumParaExp = Expression.build(ele.getAttribute("numPara"));
            mStrParaExp = Expression.build(ele.getAttribute("strPara"));
        }
    }

    private static enum Command {
        Refresh, Invalid, Remove;
    }

    private static class VisibilityProperty extends PropertyCommand {

        public static final String PROPERTY_NAME = "visibility";

        private boolean mIsShow;

        private boolean mIsToggle;

        public void doPerform() {
            if (mIsToggle) {
                mTargetElement.show(!mTargetElement.isVisible());
            } else {
                mTargetElement.show(mIsShow);
            }
        }

        protected VisibilityProperty(ScreenElementRoot root, Variable targetObj, String value) {
            super(root, targetObj, value);
            if (value.equalsIgnoreCase("toggle")) {
                mIsToggle = true;
            } else if (value.equalsIgnoreCase("true")) {
                mIsShow = true;
            } else if (value.equalsIgnoreCase("false")) {
                mIsShow = false;
            }
        }
    }

    private static final class WifiStateTracker extends StateTracker {

        private static final int MAX_SCAN_ATTEMPT = 3;

        public boolean zConnected = false;

        private int zScanAttempt = 0;

        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_STATE_ENABLING:
                    return STATE_TURNING_ON;
                case WifiManager.WIFI_STATE_ENABLED:
                    return STATE_ENABLED;
            }
            return STATE_UNKNOWN;
        }

        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null)
                return wifiStateToFiveState(wifiManager.getWifiState());
            else
                return WifiManager.WIFI_STATE_UNKNOWN;
        }

        public void onActualStateChange(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                setCurrentState(context, wifiStateToFiveState(wifiState));
                if (WifiManager.WIFI_STATE_ENABLED == wifiState) {
                    zConnected = true;
                    zScanAttempt = 0;
                }

            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                if (zScanAttempt < MAX_SCAN_ATTEMPT) {
                    if (zScanAttempt++ == 3)
                        zConnected = false;
                }

            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                zScanAttempt = MAX_SCAN_ATTEMPT;
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.DetailedState state = networkInfo.getDetailedState();
                if ((NetworkInfo.DetailedState.SCANNING != state)
                        && (NetworkInfo.DetailedState.CONNECTING != state)
                        && (NetworkInfo.DetailedState.AUTHENTICATING != state)
                        && (NetworkInfo.DetailedState.OBTAINING_IPADDR != state)) {
                    zConnected = false;
                } else if (NetworkInfo.DetailedState.CONNECTED != state)
                    zConnected = true;
            }
        }

        protected void requestStateChange(Context context, final boolean desiredState) {
            final WifiManager wifiManager = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(LOG_TAG, "No wifiManager.");
                return;
            }
            new Thread() {
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                    int wifiApState = wifiManager.getWifiApState();
                    if (desiredState
                            && (wifiApState == WifiManager.WIFI_AP_STATE_ENABLING || wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))
                        wifiManager.setWifiApEnabled(null, false);
                    wifiManager.setWifiEnabled(desiredState);
                };
            }.start();
        }
    }

    private static class WifiSwitchCommand extends NotificationReceiver {

        public static final String PROPERTY_NAME = "Wifi";

        private OnOffCommandHelper mOnOffHelper;

        private final StateTracker mWifiState = new WifiStateTracker();

        protected void doPerform() {
            if (mOnOffHelper.mIsToggle) {
                mWifiState.toggleState(mRoot.getContext().mContext);
            } else {
                boolean flag = false;
                switch (mWifiState.getTriState(mRoot.getContext().mContext)) {
                    case STATE_ENABLED:
                        flag = !mOnOffHelper.mIsOn;
                        break;

                    case STATE_DISABLED:
                        flag = mOnOffHelper.mIsOn;
                        break;
                }
                if (flag)
                    mWifiState.requestStateChange(mRoot.getContext().mContext, mOnOffHelper.mIsOn);
            }
            update();
        }

        public void onNotify(Context context, Intent intent, Object o) {
            mWifiState.onActualStateChange(context, intent);
            super.onNotify(context, intent, o);
        }

        protected void update() {
            switch (mWifiState.getTriState(mRoot.getContext().mContext)) {
                case STATE_INTERMEDIATE:
                    updateState(mWifiState.isTurningOn() ? STATE_TURNING_OFF : STATE_DISABLED);
                    break;

                case STATE_ENABLED:
                    updateState(((WifiStateTracker) mWifiState).zConnected ? STATE_ENABLED
                            : STATE_TURNING_ON);
                    break;

                case STATE_DISABLED:
                    updateState(STATE_DISABLED);
                    break;

                case STATE_TURNING_ON:
                case STATE_TURNING_OFF:
                case STATE_UNKNOWN:
                default:
                    break;
            }
        }

        public WifiSwitchCommand(ScreenElementRoot root, String value) {
            super(root, VariableNames.WIFI_STATE, NotifierManager.TYPE_WIFI_STATE);
            update();
            mOnOffHelper = new OnOffCommandHelper(value);
        }
    }

    private static class ListCommand extends ActionCommand {

        public static final String TAG_NAME = "ListCommand";
        private Command            mCommand = Command.Invalid;
        private Expression         mIndexExp;
        private ListScreenElement  mList;
        private String             mTarget;

        public ListCommand(ScreenElementRoot root, Element ele){
            super(root);
            mTarget = ele.getAttribute("target");
            if (ele.getAttribute("command").equals("remove")) {
                mCommand = Command.Remove;
            }
            mIndexExp = Expression.build(ele.getAttribute("index"));
        }

        protected void doPerform() {
            if (mList != null) {
                switch (mCommand) {
                    case Remove:
                        if (mIndexExp != null) {
                            int position = (int)mIndexExp.evaluate(mRoot.getVariables());
                            mList.removeItem(position);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        public void init() {
            ScreenElement screenElement = mRoot.findElement(mTarget);
            if (screenElement instanceof ListScreenElement) {
                mList = ((ListScreenElement)screenElement);
            }
        }
    }
}
