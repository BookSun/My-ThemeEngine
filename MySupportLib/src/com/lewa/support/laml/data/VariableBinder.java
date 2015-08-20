package com.lewa.support.laml.data;

import java.util.ArrayList;

import com.lewa.support.laml.ScreenContext;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.elements.ScreenElement;
import com.lewa.support.laml.elements.VariableArrayElement;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.util.Log;

public abstract class VariableBinder {

    protected boolean                  mFinished;

    protected boolean                  mPaused;

    protected ScreenElementRoot        mRoot;
    protected ArrayList<VariableArray> mVariableArrays = new ArrayList<VariableArray>();
    protected ArrayList<Variable>      mVariables      = new ArrayList<Variable>();

    public VariableBinder(ScreenElementRoot root){
        mRoot = root;
    }

    public void finish() {
        mFinished = true;
    }

    protected ScreenContext getContext() {
        return mRoot.getContext();
    }

    public CharSequence getName() {
        return null;
    }

    public void init() {
        mFinished = mPaused = false;
    }

    public void pause() {
        mPaused = true;
    }

    public void refresh() {
    }

    public void resume() {
        mPaused = false;
    }

    public void tick() {
    }

    protected Variable onLoadVariable(Element node) {
        return null;
    }

    protected VariableArray onLoadVariableArray(Element node) {
        return null;
    }

    protected void loadVariables(Element node) {
        Utils.traverseXmlElementChildren(node, "Variable", new Utils.XmlTraverseListener() {

            public void onChild(Element element) {
                Variable variable = onLoadVariable(element);
                if (variable != null) {
                    mVariables.add(variable);
                }
            }
        });
    }

    protected void loadVariableArrays(Element node) {
        Utils.traverseXmlElementChildren(node, "VarArray", new Utils.XmlTraverseListener() {

            public void onChild(Element element) {
                VariableArray variableArray = onLoadVariableArray(element);
                if (variableArray != null) {
                    mVariableArrays.add(variableArray);
                }
            }
        });
    }

    public static class TypedValue {

        public static final int STRING    = 2;

        public static final int INT       = 3;

        public static final int LONG      = 4;

        public static final int FLOAT     = 5;

        public static final int DOUBLE    = 6;

        public static final int TYPE_BASE = 1000;

        public String           mName;

        public int              mType;

        public String           mTypeStr;

        public TypedValue(String name, String type){
            initInner(name, type);
        }

        public TypedValue(Element node){
            if (node == null) {
                throw new NullPointerException("node is null");
            }
            initInner(node.getAttribute("name"), node.getAttribute("type"));
        }

        private void initInner(String name, String type) {
            mName = name;
            mTypeStr = type;
            mType = parseType(mTypeStr);
        }

        public boolean isNumber() {
            return mType >= INT && mType <= DOUBLE;
        }

        protected int parseType(String string) {
            int type = DOUBLE;
            if ("string".equalsIgnoreCase(mTypeStr)) {
                type = STRING;
            } else if ("double".equalsIgnoreCase(mTypeStr)) {
                type = DOUBLE;
            } else if ("float".equalsIgnoreCase(mTypeStr)) {
                type = FLOAT;
            } else if ("int".equalsIgnoreCase(mTypeStr) || "integer".equalsIgnoreCase(mTypeStr)) {
                type = INT;
            } else if ("long".equalsIgnoreCase(mTypeStr)) {
                type = LONG;
            } else {
                type = DOUBLE;
            }
            return type;
        }

    }

    public static class Variable extends TypedValue {

        public static final String   TAG_NAME = "Variable";

        public IndexedNumberVariable mNumberVar;

        public IndexedStringVariable mStringVar;

        protected double             mDefNumberValue;
        protected String             mDefStringValue;

        public Variable(String name, String type, Variables var){
            super(name, type);
            createVar(var);
        }

        public Variable(Element node, Variables var){
            super(node);
            createVar(var);
            onLoad(node);
            mDefStringValue = node.getAttribute("default");
            if (isNumber()) {
                try {
                    mDefNumberValue = Double.parseDouble(this.mDefStringValue);
                } catch (NumberFormatException localNumberFormatException) {
                    mDefStringValue = null;
                    mDefNumberValue = 0;
                }
            }
        }

        private void createVar(Variables v) {
            if (mType == STRING) {
                mStringVar = new IndexedStringVariable(mName, v);
            } else if (isNumber()) {
                mNumberVar = new IndexedNumberVariable(mName, v);
            }
        }

        protected void onLoad(Element element) {
        }

        public void setValue(double value) {
            if (mNumberVar != null) mNumberVar.set(value);
        }

        public void setValue(String value) {
            if (mStringVar != null) mStringVar.set(value);
        }
    }

    public static class VariableArray extends VariableBinder.TypedValue {

        public static final String  TAG_NAME = "VariableArray";
        public ScreenElementRoot    mRoot;
        public VariableArrayElement mVariableArrayElement;

        public VariableArray(String name, String type, ScreenElementRoot root){
            super(name, type);
            mRoot = root;
        }

        public VariableArray(Element node, ScreenElementRoot root){
            super(node);
            mRoot = root;
        }

        public void fillData(Object[] objects) {
            if (mVariableArrayElement == null) {
                init();
            } else {
                mVariableArrayElement.setItems(objects);
                return;
            }
            Log.e("VariableArray", "fail to find VarArray: " + mName);
        }

        public void init() {
            if (mVariableArrayElement == null) {
                ScreenElement element = mRoot.findElement(mName);
                if (element instanceof VariableArrayElement) {
                    mVariableArrayElement = (VariableArrayElement)element;
                }
            }
        }
    }
}
