package com.lewa.support.provider;

import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.provider.BaseColumns;

public final class YellowpagesContract {
    public static final String AUTHORITY = "com.lewa.providers.yellowpages";
    /** A content:// style uri to the authority for the contacts provider */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    
    public static class Shop implements BaseColumns{
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "shop");
        
        public static final String SHOP_ID = "shop_id";
        public static final String NAME = "name";
        public static final String PHONES = "phones";
        public static final String PHONE_TYPES = "phone_types";
        public static final String CAT_IDS = "cat_ids";
        public static final String LOGO = "logo";
        public static final String LOGO_URL = "logo_url";
        public static final String LARGE_IMAGE = "large_image";
        public static final String LARGE_IMAGE_URL = "large_image_url"; 
        public static final String ADDRESS = "address";
        public static final String WEB = "web";
        public static final String WEIBO = "weibo";
        public static final String DELETED = "deleted";
    }
    
    public static class Photo implements BaseColumns{
        public static final String CONTENT_DIRECTORY = "photo";
    }
    
    public static InputStream openShopPhotoInputStream(ContentResolver cr, Uri uri) {
        final Uri displayPhotoUri = Uri.withAppendedPath(uri, Photo.CONTENT_DIRECTORY);
        InputStream inputStream;
        try {
            AssetFileDescriptor fd = cr.openAssetFileDescriptor(displayPhotoUri, "r");
            return fd.createInputStream();
        } catch (IOException e) {
            return null;
        }
   }
}
