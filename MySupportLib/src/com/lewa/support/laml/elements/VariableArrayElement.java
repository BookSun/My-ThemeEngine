package com.lewa.support.laml.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.laml.util.Utils;
import com.lewa.support.laml.util.Utils.XmlTraverseListener;

import org.w3c.dom.Element;

import android.graphics.Canvas;

public class VariableArrayElement extends ScreenElement {

    public static final String    TAG_NAME     = "VarArray";

    private ArrayList<Item>       mArray       = new ArrayList<Item>();

    private ArrayList<Var>        mVars        = new ArrayList<Var>();

    private Type                  mType        = Type.DOUBLE;
    private int                   mItemCount;
    private IndexedNumberVariable mItemCountVar;
    private HashSet<VarObserver>  mVarObserver = new HashSet<VarObserver>();
    private Object[]              mData;

    public VariableArrayElement(Element ele, ScreenElementRoot root){
        super(ele, root);
        if (ele != null) {
            String type = ele.getAttribute("type");
            if ("string".equalsIgnoreCase(type)) {
                mType = Type.STRING;
            } else if ("bitmap".equalsIgnoreCase(type)) {
                mType = Type.BITMAP;
            } else {
                mType = Type.DOUBLE;
            }

            Utils.traverseXmlElementChildren(Utils.getChild(ele, "Vars"), "Var", new XmlTraverseListener() {

                public void onChild(Element child) {
                    mVars.add(new Var(child));
                }
            });
            Utils.traverseXmlElementChildren(Utils.getChild(ele, "Items"), "Item", new XmlTraverseListener() {

                public void onChild(Element child) {
                    mArray.add(new Item(child));
                }
            });
            if (mHasName) {
                mItemCountVar = new IndexedNumberVariable(mName, "count", root.getVariables());
            }
        }
    }

    public void doRender(Canvas canvas) {
    }

    public void init() {
        for (int i = 0, N = mVars.size(); i < N; i++) {
            mVars.get(i).init();
        }
        mItemCount = mArray.size();
        if (mData == null) {
            mData = new Object[mItemCount];
            for (int i = 0; i < mItemCount; i++) {
                mData[i] = mArray.get(i).mValue;
            }
        }

    }

    public void tick(long currentTime) {
        for (int i = 0, N = mVars.size(); i < N; i++)
            mVars.get(i).tick();

    }

    public static abstract interface VarObserver {

        public abstract void onDataChange(Object[] objects);
    }

    public void setItems(Object[] objects) {
        if (objects == null) {
            return;
        }
        mArray.clear();
        if (mItemCount != 0 && mItemCount == objects.length) {
            if (mItemCountVar != null) {
                mItemCountVar.set(mItemCount);
            }
            if (mData == null || mData.length != mItemCount) {
                mData = new Object[mItemCount];
            }
            mData = Arrays.copyOf(objects, mItemCount);
            Iterator<Var> iteratorVar = mVars.iterator();
            while (iteratorVar.hasNext()) {
                iteratorVar.next().init();
            }
            for (int i = 0, N = objects.length; i < N; i++) {
                mArray.add(new Item(objects[i]));
            }
            Iterator<VarObserver> iteratorObserver = mVarObserver.iterator();
            while (iteratorObserver.hasNext()) {
                VarObserver observer = iteratorObserver.next();
                if (observer != null) {
                    observer.onDataChange(mData);
                }
            }
        }
    }

    public void registerVarObserver(VarObserver observer, boolean add) {
        if (observer == null) {
            return;
        }
        if (add) {
            mVarObserver.add(observer);
            observer.onDataChange(mData);
        } else {
            mVarObserver.remove(observer);
        }
    }

    public static enum Type {
        DOUBLE, STRING, BITMAP
    }

    private class Item {

        public Expression mExpression;

        public Object     mValue;

        public Double evaluate(Variables vars) {
            if (mExpression != null) {
                if (mExpression.isNull(vars)){
                    return null;   
                }else{
                    return Double.valueOf(mExpression.evaluate(vars));   
                }
            } else {
                if (mValue != null) {
                    return Double.valueOf(((Number)mValue).doubleValue());
                }
            }
            return null;
        }

        public String evaluateStr(Variables vars) {
            if (mExpression != null) {
                return mExpression.evaluateStr(vars);
            } else {
                if (mValue instanceof String) {
                    return (String)mValue;
                }
            }
            return null;
        }

        public boolean isExpression() {
            return mExpression != null;
        }

        public Item(Object object){
            mValue = object;
            mExpression = null;
        }

        public Item(Element ele){
            super();
            if (ele == null) return;
            mExpression = Expression.build(ele.getAttribute("expression"));
            String mStrValue = ele.getAttribute("value");
            mValue = mStrValue;
            if (mType == Type.DOUBLE) return;
            else {
                try {
                    mValue = Double.parseDouble(mStrValue);
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    private class Var {

        private boolean               mConst;

        private boolean               mCurrentItemIsExpression;

        private int                   mIndex = -1;

        private Expression            mIndexExpression;

        private String                mName;

        private IndexedNumberVariable mNumberVar;

        private IndexedStringVariable mStringVar;

        private void update() {
            if (mIndexExpression != null) {
                Variables var = getVariables();
                int index = (int)mIndexExpression.evaluate(var);
                if (index < 0 || index >= mArray.size()) return;
                if (mIndex != index || mCurrentItemIsExpression) {
                    Item item = (Item)mArray.get(index);
                    if (mIndex != index) {
                        mIndex = index;
                        mCurrentItemIsExpression = item.isExpression();
                    }
                    if (mType == Type.STRING) {
                        mStringVar.set(item.evaluateStr(var));
                    } else {
                        mNumberVar.set(item.evaluate(var));
                    }
                }
            } else {
                if (mType == Type.STRING) {
                    mStringVar.set(null);
                } else {
                    mNumberVar.set(null);
                }
            }
        }

        public void init() {
            mIndex = -1;
            update();
        }

        public void tick() {
            if (!mConst) {
                update();
            }
        }

        public Var(Element ele){
            super();
            if (ele != null) {
                mName = ele.getAttribute("name");
                mIndexExpression = Expression.build(ele.getAttribute("index"));
                mConst = Boolean.parseBoolean(ele.getAttribute("const"));
                if (mType == Type.STRING) {
                    mStringVar = new IndexedStringVariable(mName, getVariables());
                } else if (mType == Type.DOUBLE) {
                    mNumberVar = new IndexedNumberVariable(mName, getVariables());
                }
            }

        }
    }
}
