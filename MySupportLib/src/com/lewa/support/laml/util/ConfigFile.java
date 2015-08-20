
package com.lewa.support.laml.util;

import android.text.TextUtils;
import android.util.Log;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;

import com.lewa.support.util.FileUtils;

public class ConfigFile {
    private static final String LOG_TAG = "ConfigFile";

    private static final String TAG_GADGET = "Gadget";

    private static final String TAG_GADGETS = "Gadgets";

    private static final String TAG_ROOT = "Config";

    private static final String TAG_TASK = "Intent";

    private static final String TAG_TASKS = "Tasks";

    private static final String TAG_VARIABLE = "Variable";

    private static final String TAG_VARIABLES = "Variables";

    private String mFilePath;

    private ArrayList<Gadget> mGadgets = new ArrayList<Gadget>();

    private HashMap<String, Task> mTasks = new HashMap<String, Task>();

    private HashMap<String, Variable> mVariables = new HashMap<String, Variable>();

    public static class Gadget {

        public String path;

        public int x;

        public int y;

        public Gadget(String pa, int gx, int gy) {
            path = pa;
            x = gx;
            y = gy;
        }
    }

    private static interface OnLoadElementListener {

        public abstract void OnLoadElement(Element element);
    }

    public static class Variable {

        public String name;

        public String type;

        public String value;

        public Variable() {
        }
    }

    private void loadGadgets(Element root) {
        loadList(root, TAG_GADGETS, TAG_GADGET, new OnLoadElementListener() {
            public void OnLoadElement(Element ele) {
                if (ele != null)
                    putGadget(new ConfigFile.Gadget(ele.getAttribute("path"), Utils.getAttrAsInt(
                            ele, "x", 0), Utils.getAttrAsInt(ele, "x", 0)));
            }
        });
    }

    private void loadList(Element root, String listTag, String itemTag,
            OnLoadElementListener listener) {
        Element element = Utils.getChild(root, listTag);
        if (element != null) {
            NodeList children = element.getChildNodes();
            for (int i = 0, N = children.getLength(); i < N; i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(itemTag)) {
                    Element item = (Element) node;
                    listener.OnLoadElement(item);
                }
            }

        }
    }

    private void loadTasks(Element root) {
        loadList(root, TAG_TASKS, TAG_TASK, new OnLoadElementListener() {
            public void OnLoadElement(Element ele) {
                putTask(Task.load(ele));
            }
        });
    }

    private void loadVariables(Element root) {
        loadList(root, TAG_VARIABLES, TAG_VARIABLE, new OnLoadElementListener() {
            public void OnLoadElement(Element ele) {
                ConfigFile.this.put(ele.getAttribute("name"), ele.getAttribute("value"),
                        ele.getAttribute("type"));
            }
        });
    }

    private void put(String name, String value, String type) {
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(type)
                && ("string".equals(type) || "number".equals(type))) {
            Variable item = (Variable) mVariables.get(name);
            if (item == null) {
                item = new Variable();
                item.name = name;
                mVariables.put(name, item);
            }
            item.type = type;
            item.value = value;
        }
    }

    private void writeGadgets(FileWriter fw) throws IOException {
        if (mGadgets.size() == 0)
            return;
        writeTag(fw, TAG_GADGETS, false);
        final String names[] = {
                "path", "x", "y"
        };
        for (Gadget item : mGadgets) {
            writeTag(fw, TAG_GADGET, names, new String[] {
                    item.path, String.valueOf(item.x), String.valueOf(item.y)
            }, true);
        }
        writeTag(fw, TAG_GADGETS, true);
    }

    private static void writeTag(FileWriter fw, String tag, boolean end) throws IOException {
        fw.write("<");
        if (end)
            fw.write("/");
        fw.write(tag);
        fw.write(">\n");
    }

    private static void writeTag(FileWriter fw, String tag, String names[], String values[])
            throws IOException {
        writeTag(fw, tag, names, values, false);
    }

    private static void writeTag(FileWriter fw, String tag, String names[], String values[],
            boolean ignoreEmptyValues) throws IOException {
        fw.write("<");
        fw.write(tag);
        for (int i = 0; i < names.length; i++)
            if (!ignoreEmptyValues || !TextUtils.isEmpty(values[i])) {
                fw.write(" ");
                fw.write(names[i]);
                fw.write("=\"");
                fw.write(values[i]);
                fw.write("\"");
            }

        fw.write("/>\n");
    }

    private void writeTasks(FileWriter fw) throws IOException {
        if (mTasks.size() == 0)
            return;
        writeTag(fw, TAG_TASKS, false);
        final String names[] = new String[] {
                Task.TAG_ID, Task.TAG_ACTION, Task.TAG_TYPE, Task.TAG_CATEGORY, Task.TAG_PACKAGE,
                Task.TAG_CLASS, Task.TAG_NAME
        };
        for (Task item : mTasks.values()) {
            writeTag(fw, TAG_TASK, names, new String[] {
                    item.id, item.action, item.type, item.category, item.packageName,
                    item.className, item.name
            }, true);
        }
        writeTag(fw, TAG_TASKS, true);
    }

    private void writeVariables(FileWriter fw) throws IOException {
        if (mVariables.size() == 0)
            return;
        writeTag(fw, TAG_VARIABLES, false);
        final String names[] = {
                "name", "type", "value"
        };
        for (Variable item : mVariables.values()) {
            writeTag(fw, TAG_VARIABLE, names, new String[] {
                    item.name, item.type, item.value
            });
        }
        writeTag(fw, TAG_VARIABLES, true);
    }

    public Collection<Gadget> getGadgets() {
        return mGadgets;
    }

    public Task getTask(String id) {
        return (Task) mTasks.get(id);
    }

    public Collection<Task> getTasks() {
        return mTasks.values();
    }

    public String getVariable(String name) {
        Variable item = (Variable) mVariables.get(name);
        if (item == null)
            return null;
        else
            return item.value;
    }

    public Collection<Variable> getVariables() {
        return mVariables.values();
    }

    public boolean load(String filePath) {
        mFilePath = filePath;
        mVariables.clear();
        mTasks.clear();
        try {
            File file = new File(filePath);
            if(!file.exists())
                return false;
            Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new FileInputStream(file)).getDocumentElement();
            if (root != null && root.getNodeName().equals(TAG_ROOT)) {
                loadVariables(root);
                loadTasks(root);
                loadGadgets(root);
                return true;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return false;
    }

    public void moveGadget(Gadget g, int position) {
        if (mGadgets.remove(g))
            mGadgets.add(position, g);
    }

    public void putGadget(Gadget g) {
        if (g != null) {
            mGadgets.add(g);
        }
    }

    public void putNumber(String name, double value) {
        putNumber(name, Utils.doubleToString(value));
    }

    public void putNumber(String name, String value) {
        put(name, value, "number");
    }

    public void putString(String name, String value) {
        put(name, value, "string");
    }

    public void putTask(Task task) {
        if (task != null && !TextUtils.isEmpty(task.id)) {
            mTasks.put(task.id, task);
        }
    }

    public void removeGadget(Gadget g) {
        mGadgets.remove(g);
    }

    public boolean save() {
        return save(mFilePath);
    }

    public boolean save(String filePath) {
        try {
            FileWriter fw = new FileWriter(filePath);
            writeTag(fw, TAG_ROOT, false);
            writeVariables(fw);
            writeTasks(fw);
            writeGadgets(fw);
            writeTag(fw, TAG_ROOT, true);
            fw.flush();
            fw.close();
            FileUtils.setPermissions(filePath, 511, -1, -1);
        } catch (IOException e) {
            Log.e(LOG_TAG, "save", e);
            return false;
        }
        return true;
    }

}
