package com.lewa.support.provider;

import android.net.Uri;
import android.content.Context;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

public class AutostartsConstants {
    public static final boolean DEBUG = true;

    public static final String TAG ="AutostartsConstants";

    public static final String AUTOHORITY = "com.lewa.providers.autostarts";

    public static final String TABLENAME = "autostartsData";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTOHORITY
                + "/" + TABLENAME);

    public static final String DBNAME = "autostartsDb";

    public static final int ITEM = 1;

    public static final int ITEM_ID = 2;

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.lewa.autostarts.providers";

    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.lewa.autostarts.providers";

    public static String COLUMN_BLOCK_NAME_ID  = "id";

    public static String COLUMN_PACKAGE_NAME = "pkgName";

    public static String COLUMN_CLASS_NAME = "className";

    public static final String[] PROJECTION = new String[] {
        COLUMN_PACKAGE_NAME,
        COLUMN_CLASS_NAME,
    };


    public static boolean getEnableComponentPermission(Context mContext, ComponentName component, int newState) {
        if((newState != COMPONENT_ENABLED_STATE_DEFAULT)
          && (newState != COMPONENT_ENABLED_STATE_ENABLED))
          return true;

        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        try{
            cursor = cr.query(AutostartsConstants.CONTENT_URI,
                     AutostartsConstants.PROJECTION,
                     String.format("%s = ? AND %s = ?", AutostartsConstants.COLUMN_PACKAGE_NAME, AutostartsConstants.COLUMN_CLASS_NAME),
                     new String[]{component.getPackageName(), component.getClassName()},
                     null);

            if(cursor != null && cursor.getCount() > 0) {
                Log.i(TAG,"PMS forbid to enable component:" + component.getClassName());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(cursor != null ){
                cursor.close();
            }
        }

        return true;
    }
}
