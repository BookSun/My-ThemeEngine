package com.lewa.support.util;
import java.lang.reflect.Method;

import android.graphics.Bitmap;
import android.os.Build;
import android.view.Surface;
public class SurfaceWrapper {

    private static Method mScreenshot = null;
    private static Method mScreenshotLayer = null;

    private static final String SURFACECONTROL = "android.view.SurfaceControl" ;
    
    public static Bitmap screenshot(int width, int height) {
        try {
            if (Build.VERSION.SDK_INT >=18) {
                return screenShotForControl(width,height) ;
            }
            else {
                // return Surface.screenshot(width,height) ;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap screenshot(int width, int height, int minLayer, int maxLayer) {
        try {
            if (Build.VERSION.SDK_INT >=18) {
                return screenShotForControl(width,height,minLayer ,maxLayer) ;
            }
            else {
                // return Surface.screenshot(width,height,minLayer,maxLayer) ;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Bitmap screenShotForControl(int width, int height) throws Exception{
        if (mScreenshot == null) {
            Class<?> class1 =  getClass(SURFACECONTROL) ;
            if(class1 != null)
                mScreenshot =class1.getDeclaredMethod("screenshot", Integer.TYPE, Integer.TYPE);
        }
         Object object = mScreenshot.invoke(null,width,height);
         if(object != null)
             return (Bitmap)object ;
         return null;
    }

    private static Bitmap screenShotForControl(int width, int height, int minLayer, int maxLayer) throws Exception{
        if (mScreenshotLayer == null) {
            Class<?> class1 =  getClass(SURFACECONTROL) ;
            if(class1 != null)
                mScreenshotLayer =class1.getDeclaredMethod("screenshot", Integer.TYPE, Integer.TYPE,Integer.TYPE,Integer.TYPE);
        }
        Object object =  mScreenshotLayer.invoke(null,width,height,minLayer ,maxLayer); 
        if(object != null) {
            return (Bitmap)object ;
        }
        return null;
    }

    private static Class<?>  getClass(String className) {
        Class<?>  class1 = null ;
        try{
            class1=Class.forName(className);
        }catch(Exception e){
            e.printStackTrace();
        }
        return class1 ;
    } 
}
