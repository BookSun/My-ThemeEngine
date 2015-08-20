
package com.lewa.support.laml.util;

import org.w3c.dom.Element;
import android.text.TextUtils;

public class Task {

    public static String TAG_ACTION = "action";

    public static String TAG_CATEGORY = "category";

    public static String TAG_CLASS = "class";

    public static String TAG_ID = "id";

    public static String TAG_NAME = "name";

    public static String TAG_PACKAGE = "package";

    public static String TAG_TYPE = "type";

    public static String TAG_ANIMATION = "anim";

    public String action;

    public String category;

    public String className;

    public String id;

    public String name;

    public String packageName;

    public String type;

    public boolean anim;

    public static Task load(Element ele) {
        if (ele == null) {
            return null;
        } else {
            Task task = new Task();
            task.id = ele.getAttribute(TAG_ID);
            task.action = ele.getAttribute(TAG_ACTION);
            task.type = ele.getAttribute(TAG_TYPE);
            task.category = ele.getAttribute(TAG_CATEGORY);
            task.packageName = ele.getAttribute(TAG_PACKAGE);
            task.className = ele.getAttribute(TAG_CLASS);
            task.name = ele.getAttribute(TAG_NAME);
            String animStr = ele.getAttribute(TAG_ANIMATION);
            task.anim = TextUtils.isEmpty(animStr) ? true : Boolean.getBoolean(animStr);
            return task;
        }
    }
}
