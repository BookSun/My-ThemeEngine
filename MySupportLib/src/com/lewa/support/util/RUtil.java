package com.lewa.support.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import android.content.Context;
import android.text.TextUtils;
/**
 * 
 * RUtil.java:
 * @author yljiang@lewatek.com 2014-8-7
 */
public class RUtil {

    private Context                mContext;
    private HashMap<String, Class> mHashMap;
    private static final String    TAG      = "ClassUtil";
    private static final String    R_ANIM   = ".R$anim";
    private static final String    R_STRING = ".R$string";

    public RUtil(Context context){
        mContext = context;
        mHashMap = new HashMap<String, Class>();
    }

    public int getAnimAttrId(String attr) {
        return getAttrID(getClassForName(R_ANIM), attr, 0);
    }

    public int getStringAttrId(String attr) {
        return getAttrID(getClassForName(R_STRING), attr, 0);
    }

    public Class getClassForName(String naem) {
        if (mContext == null) {
            return null;
        }
        Class clazz = mHashMap.get(naem);
        if (clazz == null) {
            clazz = getRClass(mContext, naem);
            mHashMap.put(naem, clazz);
        }
        return clazz;
    }

    private Class getRClass(Context context, String name) {
        Class clazz = null;
        try {
            clazz = Class.forName(context.getPackageName() + name);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clazz;
    }

    private int getAttrID(Class clazz, String attr, int defaultId) {
        if (clazz == null || TextUtils.isEmpty(attr)) {
            return defaultId;
        }
        Field field;
        Object componentObj = null;
        try {
            field = clazz.getField(attr);
            componentObj = field.get(clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (componentObj instanceof Integer) {
            return (Integer)componentObj;
        }
        return defaultId;

    }
}
