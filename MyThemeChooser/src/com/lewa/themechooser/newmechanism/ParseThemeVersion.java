package com.lewa.themechooser.newmechanism;

import android.util.Xml;

import com.lewa.themechooser.newmechanism.ThemeDescription.ThemeName;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * ParseThemeVersion.java:
 *
 * @author yljiang@lewatek.com 2013-11-26
 */
public class ParseThemeVersion {

    private static final String[] NAME_LIST = {"nameList", "language", "name", "author"};
    private static final String[] ElEMENT_TAG = {"packageName", "versionCode", "versionName"};
    private static final String ENC = "UTF-8";

    public static void getParseData(InputStream input, ThemeDescription themeDescription) throws Exception {
        if (input == null || themeDescription == null)
            return;
        XmlPullParser xml = Xml.newPullParser();
        xml.setInput(input, ENC);
        ArrayList<ThemeName> themeArrayList = new ArrayList<ThemeDescription.ThemeName>();
        ThemeName themeName = null;
        int event_type = xml.getEventType();
        while (event_type != XmlPullParser.END_DOCUMENT) {

            switch (event_type) {
                case XmlPullParser.END_TAG: {
                    final String key = xml.getName();
                    if (NAME_LIST[0].equals(key) && themeName != null) {
                        themeArrayList.add(themeName);
                    }
                }
                break;
                case XmlPullParser.START_TAG: {
                    final String key = xml.getName();
                    if (NAME_LIST[0].equals(key)) {
                        themeName = themeDescription.new ThemeName();
                    } else if (NAME_LIST[1].equals(key)) {
                        if (themeName != null) {
                            final String value = xml.nextText();
                            themeName.setLanguage(value);
                        }
                    } else if (NAME_LIST[2].equals(key)) {
                        if (themeName != null) {
                            final String value = xml.nextText();
                            themeName.setName(value);
                        }
                    } else if (NAME_LIST[3].equals(key)) {
                        if (themeName != null) {
                            final String value = xml.nextText();
                            themeName.setAuthor(value);
                        }
                    } else if (ElEMENT_TAG[0].equals(key)) {
                        final String value = xml.nextText();
                        themeDescription.setPackageName(value);
                    } else if (ElEMENT_TAG[1].equals(key)) {
                        final String value = xml.nextText();
                        try {
                            themeDescription.setVersionCode(Integer.parseInt(value));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (ElEMENT_TAG[2].equals(key)) {
                        final String value = xml.nextText();
                        themeDescription.setVersionName(value);
                    }

                }
                break;
            }
            event_type = xml.next();
        }
        themeDescription.setThemeNames(themeArrayList);
    }

}
