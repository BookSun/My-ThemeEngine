
package com.lewa.support.laml.elements;

import android.graphics.Canvas;

import com.lewa.support.laml.*;
import com.lewa.support.laml.animation.VariableAnimation;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.util.*;

import org.w3c.dom.Element;

public class VariableElement extends ScreenElement {

    private static final String OLD_VALUE = "old_value";

    public static final String TAG_NAME = "Var";

    private VariableAnimation mAnimation;

    private boolean mConst;

    private Expression mExpression;

    private boolean mIsStringType;

    private IndexedNumberVariable mNumberVar;

    private IndexedNumberVariable mOldNumberVar;

    private IndexedStringVariable mOldStringVar;

    private Double mOldValue;

    private IndexedStringVariable mStringVar;

    private double mThreshold;

    private CommandTrigger mTrigger;

    public VariableElement(Element ele, ScreenElementRoot root) {
        super(ele, root);
        mOldValue = null;
        if (ele != null) {
            mExpression = Expression.build(ele.getAttribute("expression"));
            mThreshold = Math.abs(Utils.getAttrAsFloat(ele, "threshold", 1));
            mIsStringType = "string".equalsIgnoreCase(ele.getAttribute("type"));
            mConst = Boolean.parseBoolean(ele.getAttribute("const"));
            Element element;
            if (mIsStringType) {
                mStringVar = new IndexedStringVariable(mName, root.getContext().mVariables);
                mOldStringVar = new IndexedStringVariable(mName, OLD_VALUE,
                        root.getContext().mVariables);
            } else {
                mNumberVar = new IndexedNumberVariable(mName, root.getContext().mVariables);
                mOldNumberVar = new IndexedNumberVariable(mName, OLD_VALUE,
                        root.getContext().mVariables);
            }
            element = Utils.getChild(ele, "VariableAnimation");
            if (element != null)
                try {
                    mAnimation = new VariableAnimation(element, root);
                } catch (ScreenElementLoadException e) {
                    e.printStackTrace();
                }
            mTrigger = CommandTrigger.fromParentElement(ele, root);
        }
    }

    private void update() {
        Variables var = mRoot.getContext().mVariables;
        if (mIsStringType) {
            if (mExpression != null) {
                String str = mExpression.evaluateStr(var);
                String oldStr = mStringVar.get();
                if (!Utils.equals(str, oldStr)) {
                    mOldStringVar.set(oldStr);
                    mStringVar.set(str);
                    if (mTrigger != null) {
                        mTrigger.perform();
                        return;
                    }
                }
            }
        } else {
            Double value = null;
            if (mAnimation != null) {
                value = Double.valueOf(mAnimation.getValue());
            } else if (mExpression != null) {
                value = mExpression.isNull(var) ? null : mExpression.evaluate(var);
            }
            mNumberVar.set(value);
            if (value != null && !value.equals(mOldValue)) {
                if (mOldValue == null){
                    mOldValue = value;
                }
                mOldNumberVar.set(mOldValue);
                if (mTrigger != null && Math.abs(value - mOldValue) >= mThreshold){
                    mTrigger.perform();
                    mOldValue = value;
                }
            }
        }
    }

    public void doRender(Canvas canvas) {
    }

    public void finish() {
        super.finish();
        if (mTrigger != null)
            mTrigger.finish();
        mOldValue = null;
    }

    public void init() {
        if (mAnimation != null)
            mAnimation.init();
        if (mTrigger != null)
            mTrigger.init();
        update();
    }

    public void pause() {
        super.pause();
        if (mTrigger != null)
            mTrigger.pause();
    }

    public void reset(long time) {
        if (mAnimation != null)
            mAnimation.reset(time);
        update();
    }

    public void resume() {
        super.resume();
        if (mTrigger != null)
            mTrigger.resume();
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        if (isVisible()) {
            if (mAnimation != null)
                mAnimation.tick(currentTime);
            if (!mConst) {
                update();
            }
        }
    }
}
