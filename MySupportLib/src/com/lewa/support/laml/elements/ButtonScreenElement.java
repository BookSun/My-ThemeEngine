
package com.lewa.support.laml.elements;

import android.graphics.Canvas;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import java.util.ArrayList;

import com.lewa.support.laml.*;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.*;

public class ButtonScreenElement extends AnimatedScreenElement {

    private static final String LOG_TAG = "ButtonScreenElement";

    public static final String TAG_NAME = "Button";

    private ButtonActionListener mListener;

    private String mListenerName;

    private ElementGroup mNormalElements;

    private boolean mPressed;

    private ElementGroup mPressedElements;

    private float mPreviousTapPositionX;

    private float mPreviousTapPositionY;

    private long mPreviousTapUpTime;

    private boolean mTouching;

    private ArrayList<CommandTrigger> mTriggers = new ArrayList<CommandTrigger>();

    public static enum ButtonAction {
        Down, Up, Double, Long, Cancel, Other
    }

    public static interface ButtonActionListener {

        public abstract boolean onButtonDoubleClick(String s);

        public abstract boolean onButtonDown(String s);

        public abstract boolean onButtonLongClick(String s);

        public abstract boolean onButtonUp(String s);
    }

    public ButtonScreenElement(Element ele, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(ele, root);
        load(ele, root);
        if (ele != null)
            mListenerName = ele.getAttribute("listener");
    }

    private ElementGroup getCur() {
        if (mPressed && mTouching && mPressedElements != null)
            return mPressedElements;
        else
            return mNormalElements;
    }

    private void onCancel() {
        performAction(ButtonAction.Cancel);
    }

    private void performAction(ButtonAction action) {
        for (CommandTrigger tri : mTriggers) {
            if (tri.getAction() == action)
                tri.perform();
        }
        mRoot.onButtonInteractive(this, action);
    }

    public void doRender(Canvas c) {
        ElementGroup cur = getCur();
        if (cur != null)
            cur.render(c);
    }

    public void finish() {
        if (mNormalElements != null)
            mNormalElements.finish();
        if (mPressedElements != null)
            mPressedElements.finish();
        for (CommandTrigger tri : mTriggers) {
            tri.finish();
        }
    }

    @Override
    public void init() {
        super.init();
        if (mNormalElements != null) {
            mNormalElements.init();
        }
        if (mPressedElements != null) {
            mPressedElements.init();
        }
        for (CommandTrigger tri : mTriggers) {
            tri.init();
        }

        try {
            if (mListener == null && !TextUtils.isEmpty(mListenerName)) {
                mListener = (ButtonActionListener) mRoot.findElement(mListenerName);
            }
        } catch (ClassCastException e) {
            Log.e(LOG_TAG, "button listener designated by the name is not actually a listener: "
                    + mListenerName);
        }
    }

    public void load(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        if (node == null) {
            Log.e(LOG_TAG, "node is null");
            throw new ScreenElementLoadException("node is null");
        }
        Element normal = Utils.getChild(node, "Normal");
        if (normal != null)
            mNormalElements = new ElementGroup(normal, root);
        Element pressed = Utils.getChild(node, "Pressed");
        if (pressed != null)
            mPressedElements = new ElementGroup(pressed, root);
        Element triggers = Utils.getChild(node, "Triggers");
        if (triggers != null) {
            NodeList children = triggers.getChildNodes();
            for (int i = 0, N = children.getLength(); i < N; i++) {
                if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element item = (Element) children.item(i);
                    if (item.getNodeName().equals("Trigger"))
                        mTriggers.add(new CommandTrigger(item, root));
                }
            }

        }
    }

    public boolean onTouch(MotionEvent event) {
        if (isVisible()) {
            float scale = mRoot.getMatrixScale();
            float x = event.getX() / scale;
            float y = event.getY() / scale;
            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    if (mNormalElements != null)
                        mNormalElements.reset();
                    onCancel();
                    mTouching = false;
                    mPressed = false;
                    return false;

                case MotionEvent.ACTION_UP:
                    if (mPressed) {
                        if (touched(x, y)) {
                            if (mListener != null)
                                mListener.onButtonUp(mName);
                            performAction(ButtonAction.Up);
                            mPreviousTapUpTime = SystemClock.uptimeMillis();
                        } else {
                            onCancel();
                        }
                        if (mNormalElements != null)
                            mNormalElements.reset();
                        mPressed = false;
                        mTouching = false;
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    mTouching = touched(x, y);
                    return mTouching;

                case MotionEvent.ACTION_DOWN:
                    if (!touched(x, y))
                        break;
                    mPressed = true;
                    mTouching = true;
                    if (mListener != null)
                        mListener.onButtonDown(mName);
                    performAction(ButtonAction.Down);
                    long l = SystemClock.uptimeMillis() - mPreviousTapUpTime;
                    if (l <= (long) ViewConfiguration.getDoubleTapTimeout()) {
                        float deltaX = x - mPreviousTapPositionX;
                        float deltaY = y - mPreviousTapPositionY;
                        float distanceSquared = deltaX * deltaX + deltaY * deltaY;
                        int doubleTapSlop = ViewConfiguration.get(getContext().mContext)
                                .getScaledDoubleTapSlop();
                        int slopSquared = doubleTapSlop * doubleTapSlop;
                        if (distanceSquared < (float) slopSquared) {
                            if (mListener != null)
                                mListener.onButtonDoubleClick(mName);
                            performAction(ButtonAction.Double);
                        }
                    }
                    mPreviousTapPositionX = x;
                    mPreviousTapPositionY = y;
                    if (mPressedElements != null)
                        mPressedElements.reset();
                    return true;

                default:
                    break;
            }
        }
        return false;
    }

    public void pause() {
        if (mNormalElements != null)
            mNormalElements.pause();
        if (mPressedElements != null)
            mPressedElements.pause();
        for (CommandTrigger tri : mTriggers) {
            tri.pause();
        }
        mPressed = false;
    }

    public void reset(long time) {
        super.reset(time);
        if (mNormalElements != null)
            mNormalElements.reset(time);
        if (mPressedElements != null)
            mPressedElements.reset(time);
    }

    public void resume() {
        if (mNormalElements != null)
            mNormalElements.resume();
        if (mPressedElements != null)
            mPressedElements.resume();
        for (CommandTrigger tri : mTriggers) {
            tri.resume();
        }
    }

    public void setListener(ButtonActionListener listener) {
        mListener = listener;
    }

    public void showCategory(String category, boolean show) {
        if (mNormalElements != null)
            mNormalElements.showCategory(category, show);
        if (mPressedElements != null)
            mPressedElements.showCategory(category, show);
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        if (isVisible()) {
            ElementGroup cur = getCur();
            if (cur != null) {
                cur.tick(currentTime);
            }
        }
    }

    public boolean touched(float x, float y) {
        float parentX = mParent != null ? mParent.getLeft() : 0;
        float f = mParent != null ? mParent.getTop() : 0;
        float f1 = getX();
        float f2 = getY();
        return x >= parentX + f1 && x <= parentX + f1 + getWidth() && y >= f + f2
                && y <= f + f2 + getHeight();
    }
}
