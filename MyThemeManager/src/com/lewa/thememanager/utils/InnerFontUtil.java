package com.lewa.thememanager.utils;

import com.lewa.thememanager.Constants;
import com.lewa.thememanager.provider.ThemesProvider;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.Themes.ThemeColumns;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentValues;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import lewa.os.FileUtilities;

public class InnerFontUtil {
    public static final String PKG_PREFIX = "com.lewa.font.inner.";
    private static final String FONT_PATH = "/system/fonts";
    private static final String FONT_CONFIG_FILE = "/data/data/" + ThemeManager.THEME_ELEMENTS_PACKAGE + "/fallback_fonts.xml";
    private static final String CONFIG_FILE = FONT_PATH + "/Lewa-Fonts.xml";
    
    public static void InsertInnerFonts(SQLiteDatabase db){
        InputStream in = null;
        try {
            File config = new File(CONFIG_FILE);
            if(config.exists()){
                in = new FileInputStream(config);
                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(in, "utf-8");
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.getName().equals("Font")) {
                        String name = parser.getAttributeValue(null, "name");
                        String label = parser.getAttributeValue(null, "label");
                        String id = PKG_PREFIX + name;
                        ContentValues values = new ContentValues();
                        values.put(ThemeColumns.THEME_PACKAGE,id);
                        values.put(ThemeColumns.THEME_ID, id);
                        values.put(ThemeColumns.NAME, label);
                        values.put(ThemeColumns.SIZE, 0);
                        values.put(ThemeColumns.AUTHOR, "");
                        values.put(ThemeColumns.STYLE_NAME, "");
                        values.put(ThemeColumns.FONT_URI, name);
                        values.put(ThemeColumns.IS_SYSTEM, 1);
                        db.insert(ThemesProvider.TABLE_NAME, ThemeColumns._ID, values);
                    }
                    eventType = parser.next();
                }
                in.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "insert inner font", e);
        } finally {
            if(in != null){
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        }
    }
    
    public static void resetFontConfig(){
        try {
            FileUtilities.deleteIfExists(FONT_CONFIG_FILE);
        } catch (Exception e) {
        }
    }
    
    public static boolean setFont(String name){
        String config = "<familyset><family order=\"0\"><fileset><file>" + name + ".ttf</file></fileset></family></familyset>";
        try {
            FileUtilities.stringToFile(FONT_CONFIG_FILE, config);
            setPermission(new File(FONT_CONFIG_FILE));
            reloadFont();
            System.exit(0);
            return true;
        } catch (IOException e) {
        }
        return false;
    }
    
    private static void setPermission(File file){
        try {
            FileUtilities.setPermissions(file);
            File parent = file.getParentFile();
            if(parent!= null){
                setPermission(parent);
            }
        } catch (Exception e) {
        }
    }

    public static void reloadFont() {
        try {
            android.app.IActivityManager am = android.app.ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();
            Configuration tempConfig = new Configuration(config);
            tempConfig.locale = new Locale("zz", "ZZ");
            tempConfig.userSetLocale = true;
            am.updateConfiguration(tempConfig);
            config.userSetLocale = true;
            am.updateConfiguration(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
