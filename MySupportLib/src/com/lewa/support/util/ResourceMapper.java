package com.lewa.support.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class ResourceMapper {
    public static int resolveReference(Context context, int id) {
        if (context == null) {
            android.util.Log.e("ResourceMapper", "error to getResources, context is null");
            return id;
        }
        TypedValue outValue = new TypedValue();
        context.getResources().getValue(id, outValue, true);
        if (outValue.resourceId == 0) {
            return id;
        }
        return outValue.resourceId;
    }
}