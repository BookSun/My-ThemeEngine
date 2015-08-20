package com.lewa.support.laml.data;

import android.graphics.Bitmap;
import java.util.HashMap;

public class ContextVariables {

    private HashMap<String, Object> mMap = new HashMap<String, Object>();

    public void clear() {
        mMap.clear();
    }

    public Bitmap getBmp(String paramString) {
        Object localObject = mMap.get(paramString);
        if (localObject == null || !(localObject instanceof Bitmap))
            return null;
        return (Bitmap)localObject;
    }

    public Double getDouble(String paramString) {
        Object localObject = mMap.get(paramString);
        if (localObject == null || !(localObject instanceof Double))
            return null;
        return (Double)localObject;
    }

    public Integer getInt(String paramString) {
        Object localObject = mMap.get(paramString);
        if (localObject == null || !(localObject instanceof Integer))
            return null;
        return (Integer)localObject;
    }

    public Long getLong(String paramString) {
        Object localObject = mMap.get(paramString);
        if (localObject == null || !(localObject instanceof Long))
            return null;
        return (Long)localObject;
    }

    public String getString(String paramString) {
        Object localObject = mMap.get(paramString);
        if (localObject == null || !(localObject instanceof String))
            return null;
        return (String)localObject;
    }

    public void setVar(String paramString, Object paramObject) {
         mMap.put(paramString, paramObject);
    }
}
