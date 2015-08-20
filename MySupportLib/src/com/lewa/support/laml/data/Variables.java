
package com.lewa.support.laml.data;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public class Variables {

    private static boolean DBG = false;

    private static final String GLOBAL = "__global";

    private static final String LOG_TAG = "Variables";

    private int mNextDoubleIndex = 0;

    private int mNextStringIndex = 0;

    private Object mNumLock = new Object();

    private Object mStrLock = new Object();

    private HashMap<String, HashMap<String, Integer>> mNumObjects = new HashMap<String, HashMap<String, Integer>>();

    private HashMap<String, HashMap<String, Integer>> mStrObjects = new HashMap<String, HashMap<String, Integer>>();

    private ArrayList<StringInfo> mStringArray = new ArrayList<StringInfo>();

    private ArrayList<DoubleInfo> mDoubleArray = new ArrayList<DoubleInfo>();

    private int getIndex(HashMap<String, HashMap<String, Integer>> map, String object,
            String property, int nextIndex) {
        if (object == null) {
            object = GLOBAL;
        }

        HashMap<String, Integer> obj = map.get(object);
        if (obj == null) {
            obj = new HashMap<String, Integer>();
            map.put(object, obj);
        }

        Integer index = obj.get(property);
        if (index == null) {
            index = Integer.valueOf(nextIndex);
            obj.put(property, index);
            return nextIndex;
        }
        return index.intValue();
    }

    public Double getNum(int index) {
        synchronized (mNumLock) {
            if (index >= 0 && index < mDoubleArray.size()) {
                DoubleInfo doubleInfo = mDoubleArray.get(index);
                if (doubleInfo != null)
                    return doubleInfo.mValue;
            }
        }
        return null;
    }

    public int getNumVer(int index) {
        synchronized (mNumLock) {
            if (index >= 0 && index < mDoubleArray.size()) {
                DoubleInfo doubleInfo = mDoubleArray.get(index);
                if (doubleInfo != null)
                    return doubleInfo.mVersion;
            }
        }
        return -1;
    }

    public String getStr(int index) {
        synchronized (mStrLock) {
            if (index >= 0 && index < mStringArray.size()) {
                StringInfo stringInfo = mStringArray.get(index);
                if (stringInfo != null)
                    return stringInfo.mValue;
            }
        }
        return null;
    }

    public int getStrVer(int index) {
        synchronized (mStrLock) {
            if (index >= 0 && index < mStringArray.size()) {
                StringInfo stringInfo = mStringArray.get(index);
                if (stringInfo != null)
                    return stringInfo.mVersion;
            }
        }
        return -1;
    }

    public void putNum(int index, double value) {
        putNum(index, Double.valueOf(value));
    }

    public void putNum(int index, Double value) {
        synchronized (mNumLock) {
            if (index >= 0) {
                while (index > (mDoubleArray.size() - 1)) {
                    mDoubleArray.add(null);
                }
                DoubleInfo doubleInfo = mDoubleArray.get(index);
                if (doubleInfo != null) {
                    doubleInfo.setValue(value);
                } else {
                    doubleInfo = new DoubleInfo(value, 0);
                    mDoubleArray.set(index, doubleInfo);
                }
            }
        }
    }

    public void putStr(int index, String value) {
        synchronized (mStrLock) {
            if (index >= 0) {
                while (index > (mStringArray.size() - 1)) {
                    mStringArray.add(null);
                }
                StringInfo stringInfo = mStringArray.get(index);
                if (stringInfo != null) {
                    stringInfo.setValue(value);
                } else {
                    stringInfo = new StringInfo(value, 0);
                    mStringArray.set(index, stringInfo);
                }
            }
        }
    }

    public int registerNumberVariable(String mapKey, String valueKey) {
        synchronized (mNumLock) {
            int index = getIndex(mNumObjects, mapKey, valueKey, mNextDoubleIndex);
            if (index == mNextDoubleIndex) {
                mNextDoubleIndex++;
            }
            if (DBG)
                Log.d(LOG_TAG, "registerNumberVariable: " + mapKey + "." + valueKey + "  index:"
                        + index);
            return index;
        }
    }

    public int registerStringVariable(String mapKey, String valueKey) {
        synchronized (mStrLock) {
            int index = getIndex(mStrObjects, mapKey, valueKey, mNextStringIndex);
            if (index == mNextStringIndex) {
                mNextStringIndex++;
            }
            if (DBG)
                Log.d(LOG_TAG, "registerStringVariable: " + mapKey + "." + valueKey + "  index:"
                        + index);
            return index;
        }
    }

    public void reset() {
        int size = mDoubleArray.size();
        for (int i = 0; i < size; i++) {
            mDoubleArray.set(i, null);
        }

        size = mStringArray.size();
        for (int i = 0; i < size; i++) {
            mStringArray.set(i, null);
        }
    }

    private static class DoubleInfo {

        Double mValue;

        int mVersion;

        public void setValue(Double value) {
            mValue = value;
            mVersion++;
        }

        public DoubleInfo(Double value, int version) {
            mValue = value;
            mVersion = version;
        }
    }

    private static class StringInfo {

        String mValue;

        int mVersion;

        public void setValue(String value) {
            mValue = value;
            mVersion++;
        }

        public StringInfo(String value, int version) {
            mValue = value;
            mVersion = version;
        }
    }

}
