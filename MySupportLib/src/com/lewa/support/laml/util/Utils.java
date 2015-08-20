
package com.lewa.support.laml.util;

import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.Log;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.data.Variables;

import org.w3c.dom.*;

import java.util.Locale;

public class Utils {
    public static class GetChildWrapper {

        private Element mEle;

        public GetChildWrapper getChild(String name) {
            return new GetChildWrapper(Utils.getChild(mEle, name));
        }

        public Element getElement() {
            return mEle;
        }

        public GetChildWrapper(Element ele) {
            mEle = ele;
        }
    }

    public static class Point {

        public double x;

        public double y;

        public void Offset(Point a) {
            x = x + a.x;
            y = y + a.y;
        }

        Point minus(Point a) {
            return new Point(x - a.x, y - a.y);
        }

        public Point(double x0, double y0) {
            x = x0;
            y = y0;
        }
    }

    public static interface XmlTraverseListener {

        public abstract void onChild(Element element);
    }

    public Utils() {
    }

    public static double Dist(Point a, Point b, boolean sqr) {
        double x = a.x - b.x;
        double y = a.y - b.y;
        if (sqr)
            return Math.sqrt(x * x + y * y);
        else
            return x * x + y * y;
    }

    public static String addFileNameSuffix(String src, String suffix) {
        return addFileNameSuffix(src, "_", suffix);
    }

    public static String addFileNameSuffix(String src, String separator, String suffix) {
        int dot = src.indexOf('.');
        StringBuilder sb = new StringBuilder(src.substring(0, dot));
        return sb.append(separator).append(suffix).append(src.substring(dot)).toString();
    }

    public static void asserts(boolean t) throws ScreenElementLoadException {
        asserts(t, "assert error");
    }

    public static void asserts(boolean t, String s) throws ScreenElementLoadException {
        if (!t)
            throw new ScreenElementLoadException(s);
        else
            return;
    }

    public static String doubleToString(double value) {
        String str = String.valueOf(value);
        if (str.endsWith(".0"))
            str = str.substring(0, -2 + str.length());
        return str;
    }

    public static boolean equals(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static float getAttrAsFloat(Element ele, String name, float def) {
        try {
            return Float.parseFloat(ele.getAttribute(name));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static float getAttrAsFloatThrows(Element ele, String name)
            throws ScreenElementLoadException {
        try {
            return Float.parseFloat(ele.getAttribute(name));
        } catch (NumberFormatException e) {
            throw new ScreenElementLoadException(String.format(
                    "fail to get attribute name: %s of Element %s", name, ele.toString()));
        }
    }

    public static int getAttrAsInt(Element ele, String name, int def) {
        try {
            return Integer.parseInt(ele.getAttribute(name));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int getAttrAsIntThrows(Element ele, String name)
            throws ScreenElementLoadException {
        try {
            return Integer.parseInt(ele.getAttribute(name));
        } catch (NumberFormatException e) {
            throw new ScreenElementLoadException(String.format(
                    "fail to get attribute name: %s of Element %s", name, ele.toString()));
        }
    }

    public static long getAttrAsLong(Element ele, String name, long def) {
        try {
            return Long.parseLong(ele.getAttribute(name));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static long getAttrAsLongThrows(Element ele, String name)
            throws ScreenElementLoadException {
        try {
            return Long.parseLong(ele.getAttribute(name));
        } catch (NumberFormatException e) {
            throw new ScreenElementLoadException(String.format(
                    "fail to get attribute name: %s of Element %s", name, ele.toString()));
        }
    }

    public static Element getChild(Element ele, String name) {
        if (ele == null){
            return null;
        }
        NodeList nl = ele.getChildNodes();
        for (int i = 0, N = nl.getLength(); i < N; i++) {
            Node item = nl.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE && item.getNodeName().equalsIgnoreCase(name)){
                return (Element) item;
            }
        }

        return null;
    }

    public static double getVariableNumber(String object, String property, Variables vars) {
        IndexedNumberVariable tmp = new IndexedNumberVariable(object, property, vars);
        return tmp.get().doubleValue();
    }

    public static double getVariableNumber(String property, Variables vars) {
        return getVariableNumber(null, property, vars);
    }

    public static String getVariableString(String object, String property, Variables vars) {
        IndexedStringVariable tmp = new IndexedStringVariable(object, property, vars);
        return tmp.get();
    }

    public static String getVariableString(String property, Variables vars) {
        return getVariableString(null, property, vars);
    }

    public static int mixAlpha(int a1, int a2) {
        if (a1 < 255) {
            if (a2 >= 255)
                return a1;
            else
                return Math.round((float) (a1 * a2) / 255F);
        } else {
            return a2;
        }
    }

    public static Point pointProjectionOnSegment(Point a, Point b, Point c, boolean nearestEnd) {
        Point AB = b.minus(a);
        Point AC = c.minus(a);
        double r = AB.x * AC.x + AB.y * AC.y;
        double d = r / Dist(a, b, false);
        if (d >= 0 && d <= 1) {
            Point D = AB;
            D.x = d * D.x;
            D.y = d * D.y;
            D.Offset(a);
            return D;
        }
        if (!nearestEnd)
            a = null;
        else if (d >= 0)
            a = b;
        return a;
    }

    public static void putVariableNumber(String object, String property, Variables vars,
            Double value) {
        IndexedNumberVariable tmp = new IndexedNumberVariable(object, property, vars);
        tmp.set(value);
    }

    public static void putVariableNumber(String property, Variables vars, Double value) {
        putVariableNumber(null, property, vars, value);
    }

    public static void putVariableString(String object, String property, Variables vars, String str) {
        IndexedStringVariable tmp = new IndexedStringVariable(object, property, vars);
        tmp.set(str);
    }

    public static void putVariableString(String property, Variables vars, String str) {
        putVariableString(null, property, vars, str);
    }

    public static double stringToDouble(String value, double def) {
        if (value == null)
            return def;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void traverseXmlElementChildren(Element parent, String tag, XmlTraverseListener l) {
        NodeList children = parent.getChildNodes();
        for (int i = 0, N = children.getLength(); i < N; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && (tag == null || TextUtils.equals(node.getNodeName(), tag) || node.getNodeName().equalsIgnoreCase(tag))) {
                l.onChild((Element) node);
            }
        }

    }

    public static PorterDuff.Mode getPorterDuffMode(int index) {
        PorterDuff.Mode[] arrayOfMode = PorterDuff.Mode.values();
        if (index >= 0 && index <= arrayOfMode.length - 1) {
            return arrayOfMode[index];
        }
        return PorterDuff.Mode.SRC_OVER;
    }

    public static PorterDuff.Mode getPorterDuffMode(String name) {
        if (TextUtils.isEmpty(name)) {
            return PorterDuff.Mode.SRC_OVER;
        }
        PorterDuff.Mode mode = PorterDuff.Mode.SRC_OVER;
        try {
            PorterDuff.Mode mode2 = PorterDuff.Mode.valueOf(name.toUpperCase());
            if (mode2 != null) return mode2;
        } catch (IllegalArgumentException localIllegalArgumentException) {
            Log.w("Utils", "illegal xfermode: " + name);
        }
        return mode;
    }
    public static int getCurrentLanguage() {
        int languageFlag = 3;
        if (isLanguageZhCn()) {
            languageFlag = 1;
        } else if (isLanguageZhTw()) {
            languageFlag = 2;
        } else {
            languageFlag = 3;
        }
        return languageFlag;
    }
    public static boolean isLanguageZhCn() {
        String defaultLanguage = getLanguageHeader();
        return defaultLanguage.equalsIgnoreCase("zh-cn");
    }

    public static boolean isLanguageZhTw() {
        String defaultLanguage = getLanguageHeader();
        return defaultLanguage.equalsIgnoreCase("zh-tw");
    }

    public static boolean isLanguageEnUs() {
        String defaultLanguage = getLanguageHeader();
        return defaultLanguage.equalsIgnoreCase("en-us");
    }

    private static String getLanguageHeader() {
        StringBuilder builder = new StringBuilder();
        builder.append(Locale.getDefault().getLanguage());
        builder.append("-");
        builder.append(Locale.getDefault().getCountry());
        return builder.toString().toLowerCase();
    }
}
