package com.lewa.support.laml.elements;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

import com.lewa.support.laml.ActionCommand;
import com.lewa.support.laml.CommandTrigger;
import com.lewa.support.laml.Constants;
import com.lewa.support.laml.ScreenContext;
import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.data.VariableNames;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.elements.AdvancedSlider.TaskWrapper.LoadConfigTask;
import com.lewa.support.laml.elements.ButtonScreenElement.ButtonAction;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.Task;
import com.lewa.support.laml.util.Utils;
import com.lewa.support.util.HapticFeedbackUtil;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AdvancedSlider extends ScreenElement {

    private static final boolean      DEBUG                      = false;

    private static final int          DEFAULT_DRAG_TOLERANCE     = 150;

    private static final float        FREE_ENDPOINT_DIST         = Float.MAX_VALUE / 2;

    private static final String       LOG_TAG                    = "LockScreen_AdvancedSlider";

    public static final String        MOVE_DIST                  = "move_dist";

    public static final String        MOVE_X                     = "move_x";

    public static final String        MOVE_Y                     = "move_y";

    private static final float        NONE_ENDPOINT_DIST         = Float.MAX_VALUE;

    public static final int           SLIDER_STATE_NORMAL        = 0;

    public static final int           SLIDER_STATE_PRESSED       = 1;

    public static final int           SLIDER_STATE_REACHED       = 2;

    public static final String        STATE                      = "state";

    public static final String        TAG_NAME                   = "Slider";

    private BounceAnimationController mBounceAnimationController = new BounceAnimationController();

    private EndPoint                  mCurrentEndPoint;

    private ArrayList<EndPoint>       mEndPoints                 = new ArrayList<EndPoint>();

    private IndexedNumberVariable     mMoveDistVar;

    private IndexedNumberVariable     mMoveXVar;

    private IndexedNumberVariable     mMoveYVar;

    private boolean                   mMoving;

    private OnLaunchListener          mOnLaunchListener;

    private boolean                   mPressed;

    private StartPoint                mStartPoint;

    private IndexedNumberVariable     mStateVar;

    private float                     mTouchOffsetX;

    private float                     mTouchOffsetY;

    public AdvancedSlider(Element node, ScreenElementRoot root) throws ScreenElementLoadException{
        super(node, root);
        if (mHasName) {
            mStateVar = new IndexedNumberVariable(mName, STATE, getVariables());
            mMoveXVar = new IndexedNumberVariable(mName, MOVE_X, getVariables());
            mMoveYVar = new IndexedNumberVariable(mName, MOVE_Y, getVariables());
            mMoveDistVar = new IndexedNumberVariable(mName, MOVE_DIST, getVariables());
        }
        root.addPreTicker(mBounceAnimationController);
        load(node);
    }

    private void cancelMoving() {
        resetInner();
        requestUpdate();
        onCancel();
    }

    private boolean checkEndPoint(Utils.Point point, EndPoint endPoint) {
        if (endPoint.touched((float)point.x, (float)point.y)) {
            if (endPoint.getState() != State.Reached) {
                endPoint.setState(State.Reached);
                for (EndPoint ep : mEndPoints) {
                    if (ep != endPoint) ep.setState(State.Pressed);
                }
                onReach(endPoint.mName);
            }
            return true;
        }
        endPoint.setState(State.Pressed);
        return false;
    }

    private CheckTouchResult checkTouch(float x, float y) {
        float minDist = NONE_ENDPOINT_DIST;
        Utils.Point point = null;
        CheckTouchResult result = new CheckTouchResult();

        for (EndPoint ep : mEndPoints) {
            Utils.Point pt = ep.getNearestPoint(x, y);
            float di = ep.getTransformedDist(pt, x, y);
            if (di < minDist) {
                minDist = di;
                point = pt;
                result.endPoint = ep;
            }
        }

        boolean reached = false;

        if (minDist < NONE_ENDPOINT_DIST) {
            moveStartPoint((float)point.x, (float)point.y);
            if (minDist < FREE_ENDPOINT_DIST) reached = checkEndPoint(point, result.endPoint);
            else {
                for (EndPoint ep : mEndPoints) {
                    if (ep.mPath == null) {
                        reached = checkEndPoint(point, ep);
                        if (reached) {
                            result.endPoint = ep;
                            break;
                        }
                    }
                }
            }

            mStartPoint.setState(reached ? State.Reached : State.Pressed);
            if (mHasName) {
                mStateVar.set(reached ? SLIDER_STATE_REACHED : SLIDER_STATE_PRESSED);
            }
            result.reached = reached;
            return result;
        }
        Log.i(LOG_TAG, "unlock touch canceled due to exceeding tollerance");
        return null;
    }

    private boolean doLaunch(EndPoint endPoint) {
        Intent intent = null;
        if (endPoint.mAction != null) intent = endPoint.mAction.perform();
        return onLaunch(endPoint.mName, intent);
    }

    private void loadEndPoint(Element node) throws ScreenElementLoadException {
        mEndPoints.clear();
        NodeList nodeList = node.getElementsByTagName("EndPoint");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element item = (Element)nodeList.item(i);
            mEndPoints.add(new EndPoint(item));
        }
        Utils.asserts(!mEndPoints.isEmpty(), "no end point for unlocker!");
    }

    private void loadStartPoint(Element node) throws ScreenElementLoadException {
        Element ele = Utils.getChild(node, "StartPoint");
        Utils.asserts(ele != null, "no StartPoint node");
        mStartPoint = new StartPoint(ele);
    }

    private void moveStartPoint(float x, float y) {
        mStartPoint.moveTo(x, y);
        if (mHasName) {
            double move_x = descale(mStartPoint.mCurrentX) - mStartPoint.mX.evaluate(getVariables());
            double move_y = descale(mStartPoint.mCurrentY) - mStartPoint.mY.evaluate(getVariables());
            double move_dist = Math.sqrt(move_x * move_x + move_y * move_y);

            mMoveXVar.set(move_x);
            mMoveYVar.set(move_y);
            mMoveDistVar.set(move_dist);
        }
    }

    public void doRender(Canvas c) {
        for (EndPoint ep : mEndPoints) {
            ep.render(c);
        }
        mStartPoint.render(c);
    }

    public ScreenElement findElement(String name) {
        ScreenElement ele = super.findElement(name);
        if (ele != null) return ele;

        ele = mStartPoint.findElement(name);
        if (ele != null) return ele;

        for (EndPoint ep : mEndPoints) {
            ele = ep.findElement(name);
            if (ele != null) return ele;

        }

        return null;
    }

    public void finish() {
        super.finish();
        mStartPoint.finish();
        for (EndPoint ep : mEndPoints) {
            ep.finish();
        }

        resetInner();
    }

    public void init() {
        super.init();
        mBounceAnimationController.init();
        mStartPoint.init();
        for (EndPoint ep : mEndPoints) {
            ep.init();
        }
        resetInner();
    }

    public void load(Element node) throws ScreenElementLoadException {
        assert (node.getNodeName().equalsIgnoreCase(TAG_NAME));
        mBounceAnimationController.load(node);
        loadStartPoint(node);
        loadEndPoint(node);
    }

    protected void onCancel() {
    }

    protected boolean onLaunch(String name, Intent intent) {
        if (mOnLaunchListener != null) mOnLaunchListener.onLaunch(name);

        return false;
    }

    protected void onReach(String name) {
        mRoot.haptic(HapticFeedbackUtil.LONG_PRESS_PATTERN);
    }

    protected void onRelease() {
        mRoot.haptic(HapticFeedbackUtil.VIRTUAL_DOWN_PATTERN);
    }

    protected void onStart() {
        mRoot.haptic(HapticFeedbackUtil.VIRTUAL_DOWN_PATTERN);
    }

    public boolean onTouch(MotionEvent event) {
        float x;
        float y;
        if (isVisible()) {
            float scale = mRoot.getMatrixScale();
            x = event.getX() / scale;
            y = event.getY() / scale;
        } else {
            x = 0;
            y = 0;
        }
        CheckTouchResult result;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_OUTSIDE:
                if (mMoving) {
                    mBounceAnimationController.startCancelMoving(null);
                    mCurrentEndPoint = null;
                    mMoving = false;
                    onRelease();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mMoving) {
                    if (DEBUG) Log.i(LOG_TAG, "unlock touch up");
                    mMoving = false;
                    result = checkTouch(x, y);
                    boolean launched = false;
                    if (result != null) {
                        if (result.reached) launched = doLaunch(result.endPoint);
                        mCurrentEndPoint = result.endPoint;
                    }
                    if (!launched) {
                        mBounceAnimationController.startCancelMoving(mCurrentEndPoint);
                        onRelease();
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMoving) {
                    result = checkTouch(x, y);
                    if (result != null) {
                        mCurrentEndPoint = result.endPoint;
                    } else {
                        mBounceAnimationController.startCancelMoving(mCurrentEndPoint);
                        mMoving = false;
                        onRelease();
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                if (mStartPoint.touched(x, y)) {
                    mMoving = true;
                    mTouchOffsetX = (x - mStartPoint.getX());
                    mTouchOffsetY = (y - mStartPoint.getY());
                    mStartPoint.setState(State.Pressed);
                    for (EndPoint ep : mEndPoints) {
                        ep.setState(State.Pressed);
                    }
                    mPressed = true;
                    if (mHasName) mStateVar.set(SLIDER_STATE_PRESSED);
                    mBounceAnimationController.init();
                    onStart();
                    return true;
                }
                break;
        }
        return false;
    }

    public void pause() {
        super.pause();
        mStartPoint.pause();
        for (EndPoint ep : mEndPoints) {
            ep.pause();
        }
        resetInner();
    }

    public void reset(long time) {
        super.reset(time);
        mStartPoint.reset(time);
        for (EndPoint ep : mEndPoints) {
            ep.reset(time);
        }
    }

    protected void resetInner() {
        mPressed = false;
        mStartPoint.moveTo(mStartPoint.getX(), mStartPoint.getY());
        mStartPoint.setState(State.Normal);
        for (EndPoint ep : mEndPoints) {
            ep.setState(State.Normal);
        }

        if (mHasName) {
            mMoveXVar.set(0);
            mMoveYVar.set(0);
            mMoveDistVar.set(0);
            mStateVar.set(0);
        }

        mMoving = false;
    }

    public void resume() {
        super.resume();
        mStartPoint.resume();
        for (EndPoint ep : mEndPoints) {
            ep.resume();
        }
    }

    public void setOnLaunchListener(OnLaunchListener l) {
        mOnLaunchListener = l;
    }

    public void showCategory(String category, boolean show) {
        mStartPoint.showCategory(category, show);
        for (EndPoint ep : mEndPoints) {
            ep.showCategory(category, show);
        }
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        if (isVisible()) {
            mStartPoint.tick(currentTime);
            for (EndPoint ep : mEndPoints) {
                ep.tick(currentTime);
            }
        }
    }

    private class BounceAnimationController implements ITicker {

        private static final int      BOUNCE_THRESHOLD = 3;

        private int                   mBounceAccelation;

        private Expression            mBounceAccelationExp;

        private int                   mBounceInitSpeed;

        private Expression            mBounceInitSpeedExp;

        private IndexedNumberVariable mBounceProgress;

        private int                   mBounceStartPointIndex;

        private long                  mBounceStartTime = -1;

        private EndPoint              mEndPoint;

        private long                  mPreDistance;

        private float                 mStartX;

        private float                 mStartY;

        private double                mTraveledDistance;

        private BounceAnimationController(){
        }

        private Utils.Point getPoint(float x1, float y1, float x2, float y2, long distance) {
            Utils.Point pt1 = new Utils.Point(x1, y1);
            Utils.Point pt2 = new Utils.Point(x2, y2);
            double total = Utils.Dist(pt1, pt2, true);
            if (distance >= total) return null;

            double ratio = (total - distance) / total;
            double dx = ratio * (pt2.x - pt1.x);
            double dy = ratio * (pt2.y - pt1.y);
            return new Utils.Point(dx + pt1.x, dy + pt1.y);
        }

        private void startBounceAnimation(EndPoint ep) {
            if (mBounceInitSpeedExp != null) mBounceInitSpeed = (int)evaluate(mBounceInitSpeedExp);

            if (mBounceAccelationExp != null) mBounceAccelation = (int)evaluate(mBounceAccelationExp);

            mBounceStartTime = 0;
            mEndPoint = ep;
            mStartX = mStartPoint.getCurrentX();
            mStartY = mStartPoint.getCurrentY();
            mBounceStartPointIndex = -1;
            mTraveledDistance = 0;
            Utils.Point p0 = new Utils.Point(mStartX, mStartY);
            if (ep != null && ep.mPath != null) {
                ArrayList<Position> path = ep.mPath;
                for (int i = 1, N = path.size(); i < N; i++) {
                    Position pt1 = path.get(i - 1);
                    Position pt2 = path.get(i);
                    Utils.Point p1 = new Utils.Point(pt1.getX(), pt1.getY());
                    Utils.Point p2 = new Utils.Point(pt2.getX(), pt2.getY());
                    Utils.Point pt = Utils.pointProjectionOnSegment(p1, p2, p0, false);
                    if (pt != null) {
                        mBounceStartPointIndex = i - 1;
                        mTraveledDistance += Utils.Dist(p1, pt, true);
                    } else {
                        mTraveledDistance += Utils.Dist(p1, p2, true);
                    }
                }
            } else {
                mTraveledDistance = Utils.Dist(new Utils.Point(mStartPoint.getX(), mStartPoint.getY()), p0, true);
            }
            if (mTraveledDistance < BOUNCE_THRESHOLD) {
                cancelMoving();
                mBounceStartTime = -1;
            } else {
                if (mBounceProgress != null) mBounceProgress.set(0);
                requestUpdate();
            }
        }

        public void init() {
            mBounceStartTime = -1;
            if (mBounceProgress != null) mBounceProgress.set(1);
        }

        public void load(Element node) {
            mBounceInitSpeedExp = Expression.build(node.getAttribute("bounceInitSpeed"));
            mBounceAccelationExp = Expression.build(node.getAttribute("bounceAcceleration"));
            if (mHasName) mBounceProgress = new IndexedNumberVariable(mName, VariableNames.BOUNCE_PROGRESS,
                                                                      getVariables());
        }

        public void startCancelMoving(EndPoint ep) {
            if (mBounceInitSpeedExp == null) {
                cancelMoving();
            } else {
                startBounceAnimation(ep);
            }
        }

        public void tick(long currentTime) {
            if (mBounceStartTime >= 0) {
                if (mBounceStartTime == 0) {
                    mBounceStartTime = currentTime;
                    mPreDistance = 0;
                } else {
                    long time = currentTime - mBounceStartTime;
                    long dist = time * mBounceInitSpeed / 1000 + time * (time * mBounceAccelation) / 2000000;
                    long speed = mBounceInitSpeed + time * mBounceAccelation / 1000;
                    if (speed > 0 && mTraveledDistance >= 3) {
                        if (mEndPoint != null && mEndPoint.mPath != null) {
                            Utils.Point pt = null;
                            float x2 = mStartPoint.getCurrentX();
                            float y2 = mStartPoint.getCurrentY();
                            long d = dist - mPreDistance;
                            for (int i = mBounceStartPointIndex; i >= 0; i--) {
                                Position pt1 = mEndPoint.mPath.get(i);
                                pt = getPoint(pt1.getX(), pt1.getY(), x2, y2, d);
                                if (pt == null) {
                                    mBounceStartPointIndex = i;
                                    break;
                                }
                                x2 = pt1.getX();
                                y2 = pt1.getY();
                                d -= Utils.Dist(new Utils.Point(pt1.getX(), pt1.getY()), new Utils.Point(x2, y2), true);
                            }
                            if (pt != null) {
                                moveStartPoint((int)pt.x, (int)pt.y);
                            }
                        } else {
                            Utils.Point pt = getPoint(mStartPoint.getX(), mStartPoint.getY(), mStartX, mStartY, dist);
                            if (pt == null) {
                                cancelMoving();
                                mBounceStartTime = -1;
                            } else {
                                moveStartPoint((int)pt.x, (int)pt.y);
                            }
                        }
                        mPreDistance = dist;
                    } else {
                        cancelMoving();
                        mBounceStartTime = -1;
                        if (mBounceProgress != null) {
                            mBounceProgress.set(1);
                        }
                        return;
                    }
                }

                requestUpdate();
                if (mTraveledDistance > 0) {
                    double progress = mPreDistance / mTraveledDistance;
                    if (mBounceProgress != null) {
                        if (progress > 1) {
                            progress = 1;
                            cancelMoving();
                        }
                        mBounceProgress.set(progress);
                    }
                }
            }
        }
    }

    private class CheckTouchResult {

        public EndPoint endPoint;

        public boolean  reached;

        private CheckTouchResult(){
        }
    }

    private class EndPoint extends SliderPoint {

        public static final String  TAG_NAME      = "EndPoint";

        public LaunchAction         mAction;

        private ArrayList<Position> mPath;

        private Expression          mPathX;

        private Expression          mPathY;

        private int                 mRawTolerance = DEFAULT_DRAG_TOLERANCE;

        private float               mTolerance;

        public EndPoint(Element node) throws ScreenElementLoadException{
            super(node, TAG_NAME);
            load(node);
        }

        private Utils.Point getNearestPoint(float x, float y) {
            Utils.Point pos = null;
            if (mPath == null) {
                pos = new Utils.Point(x - mTouchOffsetX, y - mTouchOffsetY);
            } else {
                double dist = Double.MAX_VALUE;
                for (int i = 1; i < mPath.size(); i++) {
                    Position pt1 = mPath.get(i - 1);
                    Position pt2 = mPath.get(i);
                    Utils.Point p1 = new Utils.Point(pt1.getX(), pt1.getY());
                    Utils.Point p2 = new Utils.Point(pt2.getX(), pt2.getY());
                    Utils.Point p0 = new Utils.Point(x - mTouchOffsetX, y - mTouchOffsetY);
                    Utils.Point pt = Utils.pointProjectionOnSegment(p1, p2, p0, true);

                    double d = Utils.Dist(pt, p0, false);
                    if (d < dist) {
                        dist = d;
                        pos = pt;
                    }
                }
            }
            return pos;
        }

        private void load(Element node) throws ScreenElementLoadException {
            loadTask(node);
            loadPath(node);
        }

        private void loadPath(Element node) throws ScreenElementLoadException {
            Element ele = Utils.getChild(node, "Path");
            if (ele == null) {
                mPath = null;
            } else {
                mRawTolerance = Utils.getAttrAsInt(ele, "tolerance", DEFAULT_DRAG_TOLERANCE);
                mPath = new ArrayList<Position>();
                mPathX = Expression.build(ele.getAttribute("x"));
                mPathY = Expression.build(ele.getAttribute("y"));
                NodeList nodeList = ele.getElementsByTagName(Position.TAG_NAME);

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element item = (Element)nodeList.item(i);

                    mPath.add(new Position(item, mPathX, mPathY));
                }
            }
        }

        private void loadTask(Element node) {
            ArrayList<TaskWrapper> tasks = TaskWrapper.createTaskWrapperList(node, getVariables());
            Element commandEle = Utils.getChild(node, "Command");
            Element triggerEle = Utils.getChild(node, "Trigger");
            if (tasks == null && (commandEle == null) && (triggerEle == null)) return;
            mAction = new LaunchAction();
            mAction.mTaskList = tasks;
            if (commandEle != null) {
                mAction.mCommand = ActionCommand.create(commandEle, mRoot);
                if (mAction.mCommand == null) if (DEBUG) Log.w(LOG_TAG,
                                                               "invalid Command element: " + commandEle.toString());
            }
            if (triggerEle != null) {
                try {
                    mAction.mTrigger = new CommandTrigger(triggerEle, mRoot);
                } catch (ScreenElementLoadException e) {
                }
                if (mAction.mTrigger == null) if (DEBUG) Log.w(LOG_TAG,
                                                               "invalid Trigger element: " + triggerEle.toString());
            }
        }

        public void finish() {
            super.finish();
            if (mAction != null) mAction.finish();
        }

        public float getTransformedDist(Utils.Point pt, float x, float y) {
            float dist = 0;
            if (mPath == null) {
                dist = FREE_ENDPOINT_DIST;
            } else {
                if (pt == null) return NONE_ENDPOINT_DIST;
                dist = (float)Utils.Dist(pt, new Utils.Point(x - mTouchOffsetX, y - mTouchOffsetY), true);
                if (dist >= mTolerance) return NONE_ENDPOINT_DIST;
            }
            return dist;
        }

        public void init() {
            super.init();
            if (mAction != null) mAction.init();

            mTolerance = scale(mRawTolerance);
        }

        protected void onStateChange(State pre, State s) {
            switch (s) {
                case Reached:
                    mRoot.playSound(mReachedSound);
                    break;
                case Invalid:
                case Normal:
                case Pressed:
                    break;
            }
            super.onStateChange(pre, s);
        }

        public void pause() {
            super.pause();
            if (mAction != null) mAction.pause();
        }

        public void resume() {
            super.resume();
            if (mAction != null) mAction.resume();
        }
    }

    public static abstract interface OnLaunchListener {

        public abstract void onLaunch(String name);
    }

    private class Position {

        public static final String TAG_NAME = "Position";

        private Expression         mBaseX;

        private Expression         mBaseY;

        private int                x;

        private int                y;

        public Position(Element node, Expression baseX, Expression baseY) throws ScreenElementLoadException{
            mBaseX = baseX;
            mBaseY = baseY;
            load(node);
        }

        public float getX() {
            return scale(mBaseX == null ? x : x + mBaseX.evaluate(getVariables()));
        }

        public float getY() {
            return scale(mBaseY == null ? y : y + mBaseY.evaluate(getVariables()));
        }

        public void load(Element node) throws ScreenElementLoadException {
            if (node == null) {
                Log.e(LOG_TAG, "node is null");
                throw new ScreenElementLoadException("node is null");
            }

            Utils.asserts(node.getNodeName().equalsIgnoreCase(TAG_NAME), "wrong node tag");
            x = Utils.getAttrAsInt(node, "x", 0);
            y = Utils.getAttrAsInt(node, "y", 0);
        }
    }

    private class SliderPoint {

        private ScreenElement         mCurrentStateElements;

        protected float               mCurrentX;

        protected float               mCurrentY;

        private Expression            mHeight;

        protected String              mName;

        protected ElementGroup        mNormalStateElements;

        private IndexedNumberVariable mPointStateVar;

        private ElementGroup          mReachedStateElements;

        private ElementGroup          mPressedStateElements;

        private CommandTrigger        mNormalStateTrigger;

        private CommandTrigger        mPressedStateTrigger;

        private CommandTrigger        mReachedStateTrigger;

        protected String              mReachedSound;

        protected String              mPressedSound;

        protected String              mNormalSound;

        private State                 mState = State.Invalid;

        private Expression            mWidth;

        protected Expression          mX;

        protected Expression          mY;

        public SliderPoint(Element node, String tag) throws ScreenElementLoadException{
            load(node, tag);
        }

        private ElementGroup loadGroup(Element node, String tag) throws ScreenElementLoadException {
            Element ele = Utils.getChild(node, tag);
            return ele != null ? new ElementGroup(ele, mRoot) : null;
        }

        private CommandTrigger loadTrigger(Element node, String tag) throws ScreenElementLoadException {
            Element ele = Utils.getChild(node, tag);
            return ele != null ? CommandTrigger.fromParentElement(ele, mRoot) : null;
        }

        public ScreenElement findElement(String name) {
            ScreenElement ele = null;
            if (mPressedStateElements != null) {
                ele = mPressedStateElements.findElement(name);
            }

            if (mNormalStateElements != null) {
                ele = mNormalStateElements.findElement(name);
            }

            if (mReachedStateElements != null) {
                ele = mReachedStateElements.findElement(name);
            }

            return ele;
        }

        public void finish() {
            if (mNormalStateElements != null) mNormalStateElements.finish();

            if (mPressedStateElements != null) mPressedStateElements.finish();

            if (mReachedStateElements != null) mReachedStateElements.finish();
        }

        public float getCurrentX() {
            return mCurrentX;
        }

        public float getCurrentY() {
            return mCurrentY;
        }

        public State getState() {
            return mState;
        }

        public float getX() {
            return scale(mX.evaluate(getVariables()));
        }

        public float getY() {
            return scale(mY.evaluate(getVariables()));
        }

        public void init() {
            mCurrentX = scale(mX.evaluate(getVariables()));
            mCurrentY = scale(mY.evaluate(getVariables()));
            if (mNormalStateElements != null) {
                mNormalStateElements.init();
                mNormalStateElements.show(true);
            }

            if (mPressedStateElements != null) {
                mPressedStateElements.init();
                mPressedStateElements.show(false);
            }

            if (mReachedStateElements != null) {
                mReachedStateElements.init();
                mReachedStateElements.show(false);
            }

            setState(State.Normal);
        }

        public void load(Element node, String tag) throws ScreenElementLoadException {
            Utils.asserts(node.getNodeName().equalsIgnoreCase(tag), "wrong node name");
            mName = node.getAttribute("name");
            mNormalSound = node.getAttribute("normalSound");
            mPressedSound = node.getAttribute("pressedSound");
            mReachedSound = node.getAttribute("reachedSound");
            mX = Expression.build(node.getAttribute("x"));
            mY = Expression.build(node.getAttribute("y"));
            mWidth = Expression.build(node.getAttribute("w"));
            mHeight = Expression.build(node.getAttribute("h"));
            mNormalStateElements = loadGroup(node, "NormalState");
            mPressedStateElements = loadGroup(node, "PressedState");
            mReachedStateElements = loadGroup(node, "ReachedState");
            mNormalStateTrigger = loadTrigger(node, "NormalState");
            mPressedStateTrigger = loadTrigger(node, "PressedState");
            mReachedStateTrigger = loadTrigger(node, "ReachedState");
            if (!TextUtils.isEmpty(mName)) mPointStateVar = new IndexedNumberVariable(mName, STATE, getVariables());
        }

        public void moveTo(float x, float y) {
            mCurrentX = x;
            mCurrentY = y;
        }

        protected void onStateChange(State pre, State s) {
            switch (s) {
                case Reached:
                    if (mReachedStateTrigger != null
                        && (mReachedStateTrigger.getAction() == ButtonAction.Other || mReachedStateTrigger.getAction() != null
                                                                                      && mReachedStateTrigger.getAction() == ButtonAction.Up
                                                                                      && !mMoving)) mReachedStateTrigger.perform();
                    break;
                case Pressed:
                    if (mPressedStateTrigger != null) mPressedStateTrigger.perform();
                    break;
                case Normal:
                    if (mNormalStateTrigger != null) mNormalStateTrigger.perform();
                    break;
                default:
                    break;
            }
        }

        public void pause() {
            if (mNormalStateElements != null) mNormalStateElements.pause();

            if (mPressedStateElements != null) mPressedStateElements.pause();

            if (mReachedStateElements != null) mReachedStateElements.pause();
        }

        public void render(Canvas c) {
            float x = scale(mX.evaluate(getVariables()));
            float y = scale(mY.evaluate(getVariables()));
            int rs = c.save();

            c.translate(mCurrentX - x, mCurrentY - y);
            if (mCurrentStateElements != null) mCurrentStateElements.render(c);

            c.restoreToCount(rs);
        }

        public void reset(long time) {
            if (mNormalStateElements != null) mNormalStateElements.reset(time);

            if (mPressedStateElements != null) mPressedStateElements.reset(time);

            if (mReachedStateElements != null) mReachedStateElements.reset(time);
        }

        public void resume() {
            if (mNormalStateElements != null) mNormalStateElements.resume();

            if (mPressedStateElements != null) mPressedStateElements.resume();

            if (mReachedStateElements != null) mReachedStateElements.resume();
        }

        public void setState(State s) {
            if (mState == s && mMoving) return;

            State preState = mState;
            mState = s;
            boolean reset = false;
            ElementGroup eg = null;
            int state = SLIDER_STATE_NORMAL;
            switch (s) {
                case Reached:
                    if (mReachedStateElements != null) eg = mReachedStateElements;
                    else if (mPressedStateElements != null) eg = mPressedStateElements;
                    else eg = mNormalStateElements;
                    reset = mReachedStateElements != null;
                    state = SLIDER_STATE_REACHED;
                    break;
                case Pressed:
                    if (mPressedStateElements != null) eg = mPressedStateElements;
                    else eg = mNormalStateElements;
                    reset = mPressedStateElements != null && !mPressed;
                    state = SLIDER_STATE_PRESSED;
                    break;
                case Normal:
                    eg = mNormalStateElements;
                    reset = mPressedStateElements != null;
                    state = SLIDER_STATE_NORMAL;
                    break;
                case Invalid:
                    break;
            }

            if (mCurrentStateElements != eg) {
                if (mCurrentStateElements != null) mCurrentStateElements.show(false);

                if (eg != null) eg.show(true);

                mCurrentStateElements = eg;
            }

            if (eg != null && reset) eg.reset();

            if (mPointStateVar != null) mPointStateVar.set(state);

            try {
                onStateChange(preState, mState);
            } catch (Exception e) {
            }
        }

        public void showCategory(String category, boolean show) {
            if (mPressedStateElements != null) mPressedStateElements.showCategory(category, show);

            if (mNormalStateElements != null) mNormalStateElements.showCategory(category, show);

            if (mReachedStateElements != null) mReachedStateElements.showCategory(category, show);
        }

        public void tick(long currentTime) {
            if (mCurrentStateElements != null) mCurrentStateElements.tick(currentTime);
        }

        public boolean touched(float x, float y) {
            float cx = scale(mX.evaluate(getVariables()));
            float cw = scale(mWidth.evaluate(getVariables()));
            float cy = scale(mY.evaluate(getVariables()));
            float ch = scale(mHeight.evaluate(getVariables()));
            return (x >= cx) && (x <= cx + cw) && (y >= cy) && (y <= cy + ch);
        }
    }

    private class StartPoint extends SliderPoint {

        public static final String TAG_NAME = "StartPoint";

        public StartPoint(Element node) throws ScreenElementLoadException{
            super(node, TAG_NAME);
        }

        protected void onStateChange(State pre, State s) {
            switch (s) {
                case Reached:
                    if (!mPressed) mRoot.playSound(mReachedSound);
                    break;
                case Pressed:
                    mRoot.playSound(mPressedSound);
                    break;
                case Normal:
                    mRoot.playSound(mNormalSound);
                    break;
                case Invalid:
                    break;
            }
            super.onStateChange(pre, s);
        }
    }

    private static enum State {
        Normal, Pressed, Reached, Invalid
    }

    private class LaunchAction implements LoadConfigTask {

        public ActionCommand          mCommand;

        public boolean                mConfigTaskLoaded;

        // Begin, added by yljiang@lewatek.com 2013-04-14
        public ArrayList<TaskWrapper> mTaskList;
        // End

        public CommandTrigger         mTrigger;

        private LaunchAction(){
        }

        @Override
        public Task loadTask(Task mTask) {
            if (mTask != null && !mConfigTaskLoaded) {
                Task configTask = mRoot.findTask(mTask.id);
                mConfigTaskLoaded = true;
                if (configTask != null && !TextUtils.isEmpty(configTask.action)) {
                    return configTask;
                }
            }
            return mTask;
        }

        public void finish() {
            if (mCommand != null) {
                mCommand.finish();
            }

            if (mTrigger != null) {
                mTrigger.finish();
            }
        }

        public void init() {
            if (mCommand != null) {
                mCommand.init();
            }

            if (mTrigger != null) {
                mTrigger.init();
            }
        }

        public void pause() {
            if (mCommand != null) {
                mCommand.pause();
            }

            if (mTrigger != null) {
                mTrigger.pause();
            }
        }

        public Intent perform() {
            if (mTaskList != null && !mTaskList.isEmpty()) {
                return TaskWrapper.performTask(mRoot.getContext().mContext, mTaskList, this);
            }
            if (mCommand != null) {
                mCommand.perform();
            } else if (mTrigger != null) {
                mTrigger.perform();
            }

            return null;
        }

        public void resume() {
            if (mCommand != null) {
                mCommand.resume();
            }
            if (mTrigger != null) {
                mTrigger.resume();
            }
        }

    }

    public static final class TaskWrapper {

        public Task                 mTask;
        private Expression          mCondition;
        private Variables           mVariables;
        private static final String INTENT = "Intent";

        public TaskWrapper(Task task, Expression condition, Variables variables){
            mTask = task;
            mCondition = condition;
            mVariables = variables;
        }

        public interface LoadConfigTask {

            Task loadTask(Task task);
        }

        public boolean isConditionTrue() {
            return mCondition == null || mCondition.evaluate(mVariables) > 0;
        }

        public static ArrayList<TaskWrapper> createTaskWrapperList(Element ele, Variables variables) {
            if (ele == null) {
                return null;
            }
            ArrayList<TaskWrapper> tasks = new ArrayList<TaskWrapper>();
            NodeList nodeList = ele.getChildNodes();
            for (int i = 0, N = nodeList.getLength(); i < N; i++) {
                Node item = nodeList.item(i);
                if (item.getNodeType() == Node.ELEMENT_NODE && item.getNodeName().equalsIgnoreCase(INTENT)) {
                    Task task = Task.load((Element)item);
                    if (task != null) {
                        if (tasks == null) {
                            tasks = new ArrayList<TaskWrapper>();
                        }
                        TaskWrapper extra = new TaskWrapper(
                                                            task,
                                                            Expression.build(((Element)item).getAttribute("condition")),
                                                            variables);
                        tasks.add(extra);
                    }
                }
            }
            return tasks;
        }

        static Intent performTask(Context context, ArrayList<TaskWrapper> mTaskList, LoadConfigTask taskLoaded) {
            Task mTask = mTaskList.get(0).mTask;
            if (taskLoaded != null) {
                mTask = taskLoaded.loadTask(mTask);
            }
            Intent intentFirst = createIntentFromTask(mTask);
            if (mTaskList.size() == 1 || context == null) {
                return intentFirst;
            }
            PackageManager packageManager = context.getPackageManager();
            for (TaskWrapper task : mTaskList) {
                if (task.isConditionTrue()) {
                    Intent intent = createIntentFromTask(task.mTask);
                    if (isActivityIntent(intent, packageManager)) {
                        return intent;
                    }
                }
            }
            return intentFirst;
        }

        private static boolean isActivityIntent(Intent intent, PackageManager packageManager) {
            return intent != null && packageManager.resolveActivity(intent, 0) != null;
        }

        private static Intent createIntentFromTask(Task mTask) {
            if (mTask != null) {
                if (!TextUtils.isEmpty(mTask.action)) {
                    Intent intent = new Intent(mTask.action);

                    if (!TextUtils.isEmpty(mTask.type)) {
                        intent.setType(mTask.type);
                    }

                    if (!TextUtils.isEmpty(mTask.category)) {
                        intent.addCategory(mTask.category);
                    }

                    if (!TextUtils.isEmpty(mTask.packageName) && !TextUtils.isEmpty(mTask.className)) {
                        intent.setComponent(new ComponentName(mTask.packageName, mTask.className));
                    }
                    int flag = Constants.INTENT_FLAG;
                    if (!mTask.anim) {
                        flag |= Intent.FLAG_ACTIVITY_NO_ANIMATION;
                    }
                    intent.setFlags(flag);
                    return intent;
                }

            }
            return null;
        }

    }

}
