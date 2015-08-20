
package com.lewa.support.graphics;

import android.graphics.Bitmap;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;

import com.lewa.support.theme.IconCustomizer;

public interface IBitmapFilter {

    public static final String TAG = "IBitmapFilter";

    public static class Factory {

        public static IBitmapFilter create(String filterName, List<Entry<String, String>> params) {
            IBitmapFilter filter;
            if ("Hsl".equalsIgnoreCase(filterName)) {
                filter = new HslFilter();
            } else if ("Edges".equalsIgnoreCase(filterName)) {
                filter = new EdgesFilter();
            } else if ("Levels".equalsIgnoreCase(filterName)) {
                filter = new LevelsFilter();
            } else if ("GrayScale".equalsIgnoreCase(filterName)) {
                filter = new GrayScaleFilter();
            } else if ("BlendImage".equalsIgnoreCase(filterName)) {
                filter = new BlendFilter();
            } else {
                Log.w(TAG, "unknown filter:" + filterName);
                return null;
            }

            if (params != null && params.size() > 0) {
                for (Entry<String, String> entry : params) {
                    setProperty(filter, entry.getKey(), entry.getValue());
                }
            }
            return filter;
        }

        @SuppressWarnings({
                "unchecked", "rawtypes"
        })
        private static void setProperty(Object object, String property, String value) {
            String strPropertyMethod = "set" + property;
            Method method = null;
            for (Method m : object.getClass().getMethods()) {
                if (strPropertyMethod.equalsIgnoreCase(m.getName())
                        && m.getParameterTypes().length == 1) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                Log.w(TAG, "unknown property:" + ",obj:" + method);
                return;
            }
            Class paramClass = method.getParameterTypes()[0];
            Object obj = null;
            try {
                if (String.class.equals(paramClass)) {
                    obj = value;
                } else if (Integer.TYPE.equals(paramClass)) {
                    obj = Integer.parseInt(value);
                } else if (Float.TYPE.equals(paramClass)) {
                    obj = Float.parseFloat(value);
                } else if (Double.TYPE.equals(paramClass)) {
                    obj = Double.parseDouble(value);
                } else if (Boolean.TYPE.equals(paramClass)) {
                    obj = Boolean.parseBoolean(value);
                } else if (Bitmap.class.equals(paramClass)) {
                    obj = IconCustomizer.getRawIcon(value);
                } else if (paramClass.isEnum()) {
                    obj = Enum.valueOf(paramClass, value);
                } else {
                    Log.w("IBitmapFilter", "unknown param type:" + paramClass.getName() + ",obj:"
                            + method + ",property:" + property);
                    return;
                }
                method.invoke(object, obj);
            } catch (OutOfMemoryError e) {
            } catch (Exception e) {
                Log.e("IBitmapFilter", "set property fail. obj:" + method + ",property:" + property
                        + ",value:" + value, e);
            }
        }
    }

    public abstract void process(BitmapInfo imgData);
}
