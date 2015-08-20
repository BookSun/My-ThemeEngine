package com.lewa.support.laml.elements;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Canvas;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;

public class ElementGroup extends AnimatedScreenElement {

    private static final String        LOG_TAG   = "LockScreen_ElementGroup";

    public static final String         TAG_NAME  = "ElementGroup";

    public static final String         TAG_NAME1 = "Group";

    private boolean                    mClip;

    protected ArrayList<ScreenElement> mElements = new ArrayList<ScreenElement>();

    protected ExecutorService mExecutorService,mListMsgsExecutorService;
    protected ScreenElementRoot mRoot;
    public ElementGroup(Element node, ScreenElementRoot root){
        super(node, root);
        mRoot=root;
        mExecutorService=root.mExecutorService;
        mListMsgsExecutorService=root.mListMsgsExecutorService;
        try {
            load(node, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addElement(ScreenElement newElement) {
        if (newElement != null) {
            newElement.setParent(this);
            newElement.isAsyncLoad=false;
            newElement.isTickFinished=true;
            mElements.add(newElement);
        }
    }

    public void addElement(ScreenElement newElement, int index) {
        if (newElement != null) {
            newElement.setParent(this);
            newElement.isAsyncLoad=false;
            newElement.isTickFinished=true;
            mElements.add(index, newElement);
        }
    }

    public void doRender(Canvas c) {
        if (!isVisible()) return;

        float x = getX();
        float y = getY();
        int rs = c.save();
        c.translate(x, y);

        float w = getWidth();
        float h = getHeight();
        if (mClip && w >= 0 && h >= 0) {
            c.clipRect(0, 0, w, h);
        }

        for (ScreenElement e : mElements)
            if(e.isTickFinished) e.render(c);
        c.restoreToCount(rs);
    }

    public ArrayList<ScreenElement> getElements() {
        return mElements;
    }

    public ScreenElement findElement(String name) {
        ScreenElement ele = super.findElement(name);
        if (ele != null) {
            return ele;
        }
        if (mElements != null && mElements.size() > 0) {
            for (ScreenElement element : mElements) {
                if (element != null) {
                    ele = element.findElement(name);
                    if (ele != null) {
                        return ele;
                    }
                }
            }
        }
        return null;
    }
    //TODO :This should be optimized for the later
    private static final String GROUP_WEATHER="weather_group";
    private static final String GROUP_LIST_SMSGS="list_msgs";
    public void load(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        if (node == null) {
            Log.e(LOG_TAG, "node is null");
            return;
        }
        String tmp = node.getAttribute("clip");
        if (!TextUtils.isEmpty(tmp)){
            mClip = Boolean.parseBoolean(tmp);
        }
        ScreenElementFactory factory = getContext().mFactory;
        NodeList children = node.getChildNodes();
        for (int i = 0, N = children.getLength(); i < N; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                ScreenElement newElement = factory.createInstance((Element)child, root);
                if (newElement != null) {
                    newElement.setParent(this);
                    newElement.position=i;
                    final String newElementName=newElement.getName();
                    if(newElementName.equalsIgnoreCase(GROUP_WEATHER)||newElementName.equalsIgnoreCase(GROUP_LIST_SMSGS)){
                        newElement.isAsyncLoad=true;
                        newElement.isTickFinished=false;
                    }
                    mElements.add(newElement);
                }
            }
        }
    }
    private boolean mOpenCancle = false ;
    public boolean onTouch(MotionEvent event) {
        boolean ret = false;
        if (isVisible()) {
            ret = super.onTouch(event);
            int action = event.getAction()  ;
            if(mOpenCancle){
                if (mClip && !touched(event.getX(), event.getY())) {
                    event.setAction(MotionEvent.ACTION_CANCEL);
                }
            }
            for (ScreenElement ele : mElements) {
                if (ele.isTickFinished&&ele.onTouch(event)){
                    ret = true;   
                }
            }
            if(mOpenCancle){
                event.setAction(action);
            }
        }
        return ret;
    }

    protected void onVisibilityChange(boolean visible) {
        super.onVisibilityChange(visible);
        for (ScreenElement ele : mElements)
            if(ele.isTickFinished)
            ele.onVisibilityChange(visible);
    }

    public void pause() {
        super.pause();
        for (ScreenElement ele : mElements)
            if(ele.isTickFinished)
            ele.pause();
    }

    public void reset(long time) {
        if(isVisibleInner()){
            super.reset(time);
            for (ScreenElement ele : mElements)
                if(ele.isTickFinished)
                ele.reset();
        }
    }

    public void resume() {
        super.resume();
        for (ScreenElement ele : mElements)
            if(ele.isTickFinished)
            ele.resume();
    }
    private class FinishRunnable implements Runnable{
        @Override
        public void run() {
            //mdf by huzeyin for Bug62809 2014.11.4
            try {
                final int elementSize=mElements.size();
                if(elementSize<=0){
                    Log.d(LOG_TAG, "current element size is <=0");
                    return ;
                }
                for(int i=0;i<elementSize;i++){
                    final ScreenElement ele=mElements.get(i);
                    try {
                        ele.finish();
                        if(ele.getName().equals(GROUP_WEATHER)||ele.getName().equals(GROUP_LIST_SMSGS)){
                            ele.isAsyncLoad=true;
                            ele.isTickFinished=false;
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, "finish element group get error:"+e.getMessage());
            }
        }
    }
    public void finish() {
        super.finish();
        mExecutorService.execute(new FinishRunnable());
    }
    public void init() {
        super.init();
        if(null==mExecutorService) mExecutorService=mRoot.mExecutorService;
        if(null==mListMsgsExecutorService) mListMsgsExecutorService=mRoot.mListMsgsExecutorService;
        for(int i=0,N=mElements.size();i<N;i++){
            final ScreenElement element=mElements.get(i);
            if(element.isAsyncLoad){
                if(element.getName().equals(GROUP_LIST_SMSGS)){
                    mListMsgsExecutorService.execute(new AsyncInitListRunnable(element, i));
                }else {
                    mExecutorService.execute(new AsyncInitRunnable(element, i));
                }
            }else {
                element.init();
            }
        }
    }
    private class AsyncInitListRunnable implements Runnable{
        private ScreenElement element;
        private int mIndex ;
        public AsyncInitListRunnable(ScreenElement ele,int index){
            element=ele;
            mIndex=index;
        }
        public void run() {
            element.init();
            mListMsgsExecutorService.execute(new AsyncResetListRunnable(element, mIndex));
        };
    }
    private class AsyncResetListRunnable implements Runnable{
        private ScreenElement element;
        private int mIndex ;
        public AsyncResetListRunnable(ScreenElement ele,int index){
            element=ele;
            mIndex=index;
        }
        @Override
        public void run() {
            element.reset();
            mListMsgsExecutorService.execute(new AsyncTickListRunnable(element, mIndex));
        }
    }
    private class AsyncTickListRunnable implements Runnable{
        private ScreenElement element;
        private int mIndex ;
        public AsyncTickListRunnable(ScreenElement ele,int index){
            element=ele;
            mIndex=index;
        }
        @Override
        public void run() {
            element.tick(SystemClock.elapsedRealtime());
            element.isTickFinished=true;
            element.resume();
        }
    }
    private class AsyncInitRunnable implements Runnable{
        private ScreenElement element;
        private int mIndex ;
        public AsyncInitRunnable(ScreenElement ele,int index){
            element=ele;
            mIndex=index;
        }
        @Override
        public void run() {
            element.init();
            mExecutorService.execute(new AsyncResetRunnable(element, mIndex));
        }
    }
    private class AsyncResetRunnable implements Runnable{
        private ScreenElement element;
        private int mIndex ;
        public AsyncResetRunnable(ScreenElement ele,int index){
            element=ele;
            mIndex=index;
        }
        @Override
        public void run() {
            element.reset();
            mExecutorService.execute(new AsyncTickRunnable(element, mIndex));
        }
    }
    private class AsyncTickRunnable implements Runnable{
        private ScreenElement element;
        private int mIndex ;
        public AsyncTickRunnable(ScreenElement ele,int index){
            element=ele;
            mIndex=index;
        }
        @Override
        public void run() {
            element.tick(SystemClock.elapsedRealtime());
            element.isTickFinished=true;
            element.resume();
        }
    }
    public void showCategory(String category, boolean show) {
        super.showCategory(category, show);
        for (ScreenElement ele : mElements)
            if(ele.isTickFinished)
            ele.showCategory(category, show);
    }
    public void tick(long currentTime) {
        super.tick(currentTime);
        if (isVisible()) {
            for (ScreenElement ele : mElements)
                if(ele.isTickFinished)
                ele.tick(currentTime);
        }
    }

    public void setClip(boolean clip) {
        mClip = clip;
    }

    public boolean touched(float touchX, float touchY) {
        float left = getLeft();
        float top = getTop();
        if (mParent != null) {
            left += mParent.getAbsoluteLeft();
            top += mParent.getAbsoluteTop();
        }
        float right = getWidth() + left;
        float bottom = getHeight() + top;
        if (touchX < left || touchX > right || touchY < top || touchY > bottom) {
            return false;
        }
        return true;
    }
    public void removeElement(ScreenElement ele) {
        if(ele.isTickFinished){
            mElements.remove(ele);
            requestUpdate();
        }
    }

    public void removeAllElements() {
        mElements.clear();
        requestUpdate();
    }
}
