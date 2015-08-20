package com.lewa.support.laml.data;

import java.util.Locale;

import com.lewa.support.laml.CommandTrigger;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

public class BroadcastBinder extends VariableBinder {

    private static final boolean DEBUGE     = true;

    public static final String   TAG_NAME   = "BroadcastBinder";

    private static final String  LOG_TAG    = TAG_NAME;

    private String               mAction;

    private IntentFilter         mIntentFilter;

    protected volatile String    mName;

    private boolean              mRegistered;

    private CommandTrigger       mTrigger;


    public BroadcastBinder(Element node, ScreenElementRoot root){
        super(root);
        load(node);
    }

    private void load(Element node) {
        if (node == null) {
            Log.e(LOG_TAG, "ContentProviderBinder node is null");
            throw new NullPointerException("node is null");
        }
        mName = node.getAttribute("name");
        mAction = node.getAttribute("action");
        if (TextUtils.isEmpty(mAction)) {
            Log.e(LOG_TAG, "no action in broadcast binder");
            throw new IllegalArgumentException("no action in broadcast binder element");
        } else {
            mIntentFilter = new IntentFilter(mAction);
            mTrigger = CommandTrigger.fromParentElement(node, super.mRoot);
            loadVariables(node);
        }
    }

    protected Variable onLoadVariable(Element node) {
        return new Variable(node, getContext().mVariables);
    }

    private void updateVariables(Intent intent) {
        if (intent != null) {
            if (DEBUGE) Log.d(LOG_TAG, "updateVariables: " + intent.toString());
            for (com.lewa.support.laml.data.VariableBinder.Variable variable : mVariables) {
                Variable v= (Variable)variable ;
                double value = 0;
                String valueStr = null;
                if (v.isNumber()) {
                    switch (v.mType) {
                        case Variable.STRING:
                            valueStr = intent.getStringExtra(v.mExtraName);
                            v.mStringVar.set(valueStr == null ? v.mDefStringValue : valueStr);
                            break;
                        case Variable.INT:
                            value = intent.getIntExtra(v.mExtraName, (int)v.mDefNumberValue);
                            break;
                        case Variable.LONG:
                            value = intent.getLongExtra(v.mExtraName, (long)v.mDefNumberValue);
                            break;
                        case Variable.FLOAT:
                            value = intent.getFloatExtra(v.mExtraName, (float)v.mDefNumberValue);
                            break;
                        case Variable.DOUBLE:
                            value = intent.getDoubleExtra(v.mExtraName, v.mDefNumberValue);
                            break;
                        default:
                            Log.w(LOG_TAG, "invalide type" + v.mTypeStr);
                            valueStr = String.format(Locale.US, "%f", value);
                            break;
                    }
                    v.mNumberVar.set(value);
                }
                if (DEBUGE) Log.d(LOG_TAG,
                                  "updateVariables: "
                                          + String.format("name:%s type:%s value:%s", v.mName, v.mType, valueStr));
            }

        }
    }

    public void finish() {
        if (mTrigger != null) mTrigger.finish();
        unregister();
        super.finish();
    }

    public String getName() {
        return mName;
    }

    public void init() {
        super.init();
        if (mTrigger != null) mTrigger.init();
        register();
    }

    protected void onNotify(Context context, Intent intent, Object object) {
        updateVariables(intent);
        if (mTrigger != null) mTrigger.perform();
        mRoot.requestUpdate();
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "onNotify: " + toString());
            onNotify(context, intent, null);
        }
    };

    protected void onRegister() {
        Intent intent = getContext().mContext.registerReceiver(mIntentReceiver, mIntentFilter);
        updateVariables(intent);
    }

    protected void onUnregister() {
        getContext().mContext.unregisterReceiver(mIntentReceiver);
    }

    public void pause() {
        super.pause();
        if (mTrigger != null) mTrigger.pause();
    }

    protected void register() {
        if (!mRegistered) {
            onRegister();
            mRegistered = true;
        }
    }

    public void resume() {
        super.resume();
        if (mTrigger != null) mTrigger.resume();
    }

    protected void unregister() {
        if (mRegistered) {
            try {
                onUnregister();
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.toString());
            }
            mRegistered = false;
        }
    }

    private static class Variable extends VariableBinder.Variable {

        public String mExtraName;

        protected void onLoad(Element node) {
            mExtraName = node.getAttribute("extra");
        }

        public Variable(String name, String type, Variables var){
            super(name, type, var);
        }

        public Variable(Element node, Variables var){
            super(node, var);
        }
    }
}
