/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lewa.support.util;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.AsyncTask;
import android.net.Uri;
import android.content.Context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import com.lewa.support.R;

import android.util.Log;


public class LocationUtil {
    public static final String PHONE_TYPE_FIXED_LINE = Resources.getSystem().getString(R.string.fixed_line);
    private static final String AUTHORITY = "com.lewa.providers.location";
    private static final Uri LOCATION_URI = Uri.parse("content://" + AUTHORITY + "/location");
    private static final Uri CARDTYPE_URI = Uri.parse("content://" + AUTHORITY + "/cardType");
    private static final Uri AREACODE_URI = Uri.parse("content://" + AUTHORITY + "/areacode");
    private static final Uri SPECIAL_PHONE_URI = Uri.parse("content://" + AUTHORITY + "/special_phone");
    private static final Uri SPECIAL_PHONE_LOCAL_URI = Uri.parse("content://" + AUTHORITY + "/special_phone_local");
    private static final String PHONE_NO = "number";
    private static final String CARDTYPE = "cardType";
    private static final String LOCATION = "location";
    private static final String AREACODE = "areacode";
    private static final ConcurrentHashMap<String, LocationHolder> mLocationCache =
                            new ConcurrentHashMap<String, LocationHolder>();
    private static final ConcurrentHashMap<String, String> mSpecialNumCache =
                            new ConcurrentHashMap<String, String>();

    private static final int ONLY_LOCATION            = 0;
    private static final int ONLY_LOCAL_SPECIALNUM    = 1;
    private static final int ONLY_SPECIALNUM          = 2;
    private static final int SPECIALNUM_FIRST         = 3;
    private static final int SPECIALNUM_FIRST_LOCAL   = 4;
    
    private static class LocationHolder {  
        String cardType;
        String location;
    }

    private static String filterPhoneNo(String strPhoneNo) {
        if (strPhoneNo == null) {
            return null;
        }

        strPhoneNo = strPhoneNo.replaceAll("\\-","");
        strPhoneNo = strPhoneNo.replaceAll("\\ ","");
        strPhoneNo = strPhoneNo.replaceAll("\\*","");
        //delete phone No prefix
        if (strPhoneNo.startsWith("+86")) {
            strPhoneNo = strPhoneNo.substring(3);
        }
        else if (strPhoneNo.startsWith("17951")) {
            strPhoneNo = strPhoneNo.substring(5);
        }
        else if (strPhoneNo.startsWith("12593")) {
            strPhoneNo = strPhoneNo.substring(5);
        }
        else if (strPhoneNo.startsWith("17911")) {
            strPhoneNo = strPhoneNo.substring(5);
        }
        else if (strPhoneNo.startsWith("10193")) {
            strPhoneNo = strPhoneNo.substring(5);
        }
        else if (strPhoneNo.startsWith("17909")) {
            strPhoneNo = strPhoneNo.substring(5);
        }
        else if (strPhoneNo.startsWith("17908")) {
            strPhoneNo = strPhoneNo.substring(5);
        }
        else if(strPhoneNo.startsWith("11808")){
            strPhoneNo = strPhoneNo.substring(5);
    }

        if(!isNumeric(strPhoneNo)) {
            return null;
        }
        return strPhoneNo;
    }

    private static boolean isNumeric(String str){
        return str.matches("\\d*");
    }

    public  static String getPhoneNoKey(String phoneNoStr) {
        String strPhoneNoFilter = filterPhoneNo(phoneNoStr);
        if (strPhoneNoFilter == null) {
            return null;
        }
         //Lewa fjli#add begin
         //Start with 170 input 8 bit identification number attribution  
        if (strPhoneNoFilter.startsWith("1") && strPhoneNoFilter.length() >= 7 && !strPhoneNoFilter.startsWith("170")) {
            return strPhoneNoFilter.subSequence(0, 7).toString();
        }else if(strPhoneNoFilter.startsWith("170") && strPhoneNoFilter.length() >= 8){
            return strPhoneNoFilter.subSequence(0, 8).toString(); //end
        }else if (strPhoneNoFilter.startsWith("0") && strPhoneNoFilter.length() >= 3 && strPhoneNoFilter.charAt(1) <= '2') {
           return strPhoneNoFilter.subSequence(0, 3).toString();
        }else if (strPhoneNoFilter.startsWith("0") && strPhoneNoFilter.length() >= 4 ) {
           return strPhoneNoFilter.subSequence(0, 4).toString();
        }else  {
           return null;
        }
    }

    public  static int getSpecialPhoneAreaNum(String phoneNoStr) {
        String strPhoneNoFilter = filterPhoneNo(phoneNoStr);
        if (strPhoneNoFilter == null || strPhoneNoFilter.length() > 10) { //special number
            return 0;
        }

        if (strPhoneNoFilter.startsWith("0") && strPhoneNoFilter.length() >= 3 && strPhoneNoFilter.charAt(1) <= '2') {
           return 3;
        }else if (strPhoneNoFilter.startsWith("0") && strPhoneNoFilter.length() >= 4 ) {
           return 4;
        }else  {
           return 0;
        }
    }

    private static String[] getPhoneLocationCached(Context context,String phoneNo) {
        LocationHolder mLocationHolder;
        String[] phoneLocation = new String[2];

        if(phoneNo == null) {
            return null;
        }

        String phoneNoKey = getPhoneNoKey(phoneNo);
        if (null == phoneNoKey) {
           return null;
        }
        mLocationHolder = mLocationCache.get(phoneNoKey);
        if(mLocationHolder != null) {
           phoneLocation[0] = mLocationHolder.location;
           phoneLocation[1] = mLocationHolder.cardType;
        }
        else {
            return null;
        }
        return phoneLocation;
    }

    private static String[] getPhoneLocationInternal(Context context,String phoneNo) {
        return getPhoneLocationInternal(context,phoneNo,false);
    }

    private static String[] getPhoneLocationInternal(Context context,String phoneNo,boolean onlyLocation) {
        String[] phoneLocation = new String[2];
        Cursor cursor = null;
        String selStrings = null;
        ContentResolver cr = context.getContentResolver();
        LocationHolder mLocationHolder;

        if(phoneNo == null) {
            return null;
        }

        //step 1 : get special code
        if(!onlyLocation) {
        //get special code from db
            int phoneNoPreNum =  getSpecialPhoneAreaNum(phoneNo);
            selStrings = PHONE_NO + "='" + phoneNo.substring(phoneNoPreNum) + "'";

            try {
                 cursor = cr.query(SPECIAL_PHONE_URI,
                         new String[] {LOCATION},
                         selStrings,
                         null,
                         null);
                 if ((cursor != null) && cursor.moveToFirst()) {
                        phoneLocation[0] = cursor.getString(cursor.getColumnIndex(LOCATION));
                 }
             }
             catch (Exception e) {
                 e.printStackTrace();
             }
             finally {
                if (null != cursor) {
                   cursor.close();
                }
             }
        }

              //step 2 :if is not special code ,get location code
         if (null == phoneLocation[0]){
                String phoneNoKey = getPhoneNoKey(phoneNo);
                if (null == phoneNoKey) {
                    return null;
                }
                cursor = null;
                try {
                     Uri uri = Uri.parse("content://" + AUTHORITY + "/location/" + phoneNoKey);
                             cursor = cr.query(uri,
                                         null,
                                         null,
                                         null,
                                         null);

                      if ((cursor != null) && cursor.moveToFirst()) {
                          phoneLocation[0] = cursor.getString(cursor.getColumnIndex(LOCATION));
                          phoneLocation[1] = cursor.getString(cursor.getColumnIndex(CARDTYPE));
                      }
                 }
                 catch (Exception e) {
                     e.printStackTrace();
                 }
                 finally {
                     if (null != cursor) {
                         cursor.close();
                     }
                 }
                 if(phoneLocation[0] != null) {// query location code from Database save in cache
                            LocationHolder mholder = new LocationHolder();
                            mholder.location = phoneLocation[0];
                            mholder.cardType = phoneLocation[1];
                            mLocationCache.put(phoneNoKey,mholder);
                 }
         }
         else { // query special code from Database save in cache
            mSpecialNumCache.put(phoneNo,phoneLocation[0]);
         }
         return phoneLocation;
    }


    public static String getPhoneLocation(Context context,String phoneNo) {
        return getPhoneLocation(context,phoneNo,false);
    }

    public static String getPhoneLocation(Context context,String phoneNo ,boolean onlyLocation) {
        String[] phoneLocation = getPhoneLocationCached(context,phoneNo);
        if(phoneLocation != null){
            return phoneLocation[0];
        }
        else {
            phoneLocation = getPhoneLocationInternal(context,phoneNo,onlyLocation);
            if(phoneLocation != null){
                return phoneLocation[0];
            }
            else {
                return null;
            }
        }
    }

    //include special phone or location
    public static void getPhoneLocationAsync(Context context,String phoneNo,LocationCallBack mCallback) { 
        getPhoneLocationAsync(context,phoneNo,false,false,mCallback);
    }


     //include special phone or location 
    public static void getPhoneLocationAsync(Context context,String phoneNo,boolean onlyLocation, LocationCallBack mCallback) {     

        getPhoneLocationAsync(context,phoneNo,onlyLocation,false,mCallback);       
    }

     //include special phone or location
    public static void getPhoneLocationAsync(Context context,String phoneNo,boolean onlyLocation, boolean onlyLocal,LocationCallBack mCallback) {     
        new LocationUtil.AsyncQueryLocation().executeOnExecutor(Executors.newSingleThreadExecutor(), context, phoneNo,new Integer(onlyLocation ? ONLY_LOCATION :(onlyLocal ? SPECIALNUM_FIRST_LOCAL:SPECIALNUM_FIRST)),mCallback);
    }

    public static String getPhoneCardType(Context context, String phoneNo) {
        String[] phoneLocation = getPhoneLocationInternal(context,phoneNo);
        if(phoneLocation != null){
            return phoneLocation[1];
        }
        else {
            return null;
        }
    }

    public static String[] getPhoneLocationAndCardType(Context context,String phoneNo) {
        return getPhoneLocationInternal(context,phoneNo,true);
    }

    public static void getSpecialPhoneAsync(Context context,String phoneNo,boolean local,LocationCallBack mCallback) {     
        new LocationUtil.AsyncQueryLocation().executeOnExecutor(Executors.newSingleThreadExecutor(), context, phoneNo, new Integer(local ?ONLY_LOCAL_SPECIALNUM :ONLY_SPECIALNUM),mCallback);
    }
    public static String getSpecialPhone(Context context,String phoneNo) {
        return getSpecialPhone(context,phoneNo,false);
    }
    public static String getSpecialPhone(Context context,String phoneNo,boolean local) {
        String phoneLocation = null;
        Cursor cursor = null;

        if(mSpecialNumCache.get(phoneNo) != null){
            return mSpecialNumCache.get(phoneNo);
        }
        ContentResolver cr = context.getContentResolver();
        if (phoneNo != null) {
            int phoneNoPreNum =  getSpecialPhoneAreaNum(phoneNo);
            try {
                if(local) {
                    cursor = cr.query(SPECIAL_PHONE_LOCAL_URI,
                            new String[] {LOCATION},
                            PHONE_NO + "='" + phoneNo.substring(phoneNoPreNum) + "'",
                            null,
                            null);

                }
                else {
                    cursor = cr.query(SPECIAL_PHONE_URI,
                            new String[] {LOCATION},
                            PHONE_NO + "='" + phoneNo.substring(phoneNoPreNum) + "'",
                            null,
                            null);
                }
                if ((cursor != null) && cursor.moveToFirst()) {
                    phoneLocation = cursor.getString(cursor.getColumnIndex(LOCATION));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (null != cursor) {
                    cursor.close();
                }
            }
        }
        if(phoneLocation != null && phoneLocation.length() != 0) {
            mSpecialNumCache.put(phoneNo,phoneLocation);
        }
        return phoneLocation;
    }

    public interface LocationCallBack {
        public void call(String[] location);
    }

    static class AsyncQueryLocation extends AsyncTask<Object, Integer, String[]> {
        LocationCallBack mCallback;

        @Override
        protected String[] doInBackground(Object... params) {

            Context context = (Context)params[0];
            String number = (String)params[1];
            int queryType = ((Integer)params[2]).intValue();
            mCallback = (LocationCallBack)params[3];

            String[] result = new String[2];

            if(queryType != ONLY_LOCATION) {
                String specialPhone  = getSpecialPhone(context,number,(queryType == ONLY_LOCAL_SPECIALNUM || 
                                                                       queryType == SPECIALNUM_FIRST_LOCAL));
                if(specialPhone != null) {
                    result[0] = number;
                    result[1] = specialPhone;
                    return result;
                }
                else if(queryType == ONLY_SPECIALNUM || queryType == ONLY_LOCAL_SPECIALNUM) {
                    result[0] = number;
                    result[1] = null;
                    return result;
                }
            }

            String[] phoneLocation = getPhoneLocationCached(context,number);
            if(phoneLocation != null) {
                result[0] = number;
                result[1] = phoneLocation[0];
                return result;
            }
            else {
                phoneLocation = getPhoneLocationInternal(context,number,true);
                if(phoneLocation != null) {
                    result[0] = number;
                    result[1] = phoneLocation[0];
                    return result;
                }
                else {
                    result[0] = number;
                    result[1] = null;
                    return result;
                }
            }
        }
        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            mCallback.call(result);
        }
    }

}

