
package com.lewa.support.laml.util;

import com.lewa.support.laml.data.Variables;

public class IndexedNumberVariable {

    private int mIndex = -1;

    private Variables mVars;

    public IndexedNumberVariable(String object, String property, Variables vars) {
        mIndex = vars.registerNumberVariable(object, property);
        mVars = vars;
    }

    public IndexedNumberVariable(String name, Variables vars) {
        this(null, name, vars);
    }

    public Double get() {
        return mVars.getNum(mIndex);
    }

    public int getIndex() {
        return mIndex;
    }

    public int getVersion() {
        return mVars.getNumVer(mIndex);
    }

    public void set(double value) {
        set(Double.valueOf(value));
    }

    public void set(Double value) {
        mVars.putNum(mIndex, value);
    }
}
