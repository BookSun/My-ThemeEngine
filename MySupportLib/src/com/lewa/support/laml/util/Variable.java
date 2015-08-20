
package com.lewa.support.laml.util;

import android.text.TextUtils;
import android.util.Log;

public class Variable {

    private String mObjectName;

    private String mPropertyName;

    public Variable(String var) {
        int dot = var.indexOf('.');
        if (dot == -1) {
            mObjectName = null;
            mPropertyName = var;
        } else {
            mObjectName = var.substring(0, dot);
            mPropertyName = var.substring(dot + 1);
        }
        if (TextUtils.isEmpty(mPropertyName))
            Log.e("Variable", "invalid variable name:" + var);
    }

    public String getObjName() {
        return mObjectName;
    }

    public String getPropertyName() {
        return mPropertyName;
    }
}
