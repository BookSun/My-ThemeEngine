package com.lewa.support.laml.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.lewa.support.laml.CommandTrigger;
import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.ContextVariables;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.elements.AdvancedSlider.TaskWrapper;
import com.lewa.support.laml.elements.AdvancedSlider.TaskWrapper.LoadConfigTask;
import com.lewa.support.laml.tween.Animation;
import com.lewa.support.laml.tween.Animation.AnimationListener;
import com.lewa.support.laml.tween.easing.EaseOutSine;
import com.lewa.support.laml.util.ColorParser;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.laml.util.Task;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

/**
 * ListScreenElement.java:
 * 
 * @author yljiang@lewatek.com 2014-7-8
 */
public class ListScreenElement extends ElementGroup {

    private int                     mBottomIndex;
    private int                     mCachedItemCount;
    private int                     mCurrentIndex = -1;
    private int                     mItemCount;
    private float                   mOffsetX;
    private float                   mOffsetY;
    private boolean                 mPressed;
    private int                     mSelectedId;
    private float                   mSpeedY;
    private float                   mSpeedX;
    private int                     mTopIndex;
    private float                   mTouchStartX;
    private float                   mTouchStartY;
    private int                     mVisibleItemCount;

    private ArrayList<ColumnInfo>   mColumnInfoList;
    private ArrayList<DataIndexMap> mDataList     = new ArrayList<DataIndexMap>();
    private Object[]                mIndexedVariables;

    protected AttrDataBinders       mAttrDataBinders;

    private IndexedNumberVariable   mSelectedIdVar;
    private Expression              mMaxHeight;

    private Expression              mEnableExpression;
    private ElementGroup            mInnerGroup;
    private AnimatedScreenElement   mScrollBar;
    private ListItemElement         mItem;

    private ListData                mListData;
    private boolean                 mIsMixedList;
    private MixedListHelp           mMixedListHelp;
    private LaunchAction            mLaunchAction;

    private boolean                 mNeedDrawMask = false;
    private ListMaskHelp            mListMaskHelp;

    public static final String      TAG_NAME      = "List";
    private static final boolean    DEBUG         = false;
    private static final String     LOG_TAG       = "ListScreenElement";
    public ListScreenElement(Element node, ScreenElementRoot root){
        super(node, root);
        setClip(true);

        String tmp = node.getAttribute("isMixedList");
        mIsMixedList = Boolean.parseBoolean(tmp);
        if (mIsMixedList) {
            mMixedListHelp = new MixedListHelp(node, root);
        }
        mListMaskHelp = new ListMaskHelp(node, root);
        addElement(onCreateChild(node));
        if (mItem == null) {
            if (mIsMixedList) {
                mItem = mMixedListHelp.getListItemElement();
            } else {
                Log.e(LOG_TAG, "no item");
                throw new IllegalArgumentException("List: no item");
            }
        }
        mMaxHeight = Expression.build(node.getAttribute("maxHeight"));
        tmp = node.getAttribute("data");
        if (TextUtils.isEmpty(tmp) && !mIsMixedList) {
            Log.e(LOG_TAG, "no data");
            throw new IllegalArgumentException("List: no data");
        }
        mColumnInfoList = ColumnInfo.createColumnInfoList(tmp);
        if (mColumnInfoList == null && !mIsMixedList) {
            Log.e(LOG_TAG, "invalid item data");
            throw new IllegalArgumentException("List: invalid item data");
        }
        Element tmpElement = Utils.getChild(node, "Data");
        if (tmpElement != null) {
            mListData = new ListData(tmpElement, mRoot, this);
        }
        // if(mColumnInfoList != null){
        // mIndexedVariables = new Object[mColumnInfoList.size()];
        // }
        tmpElement = Utils.getChild(node, "AttrDataBinders");
        if (tmpElement != null) {
            mAttrDataBinders = new AttrDataBinders(tmpElement, mRoot.getContext().mContextVariables);
        } else  if (!mIsMixedList){
            Log.e(LOG_TAG, "no attr data binder");
            throw new IllegalArgumentException("List: no attr data binder");
        }
        ScreenElement scrollbar = findElement("scrollbar");
        if (scrollbar instanceof AnimatedScreenElement) {
            mScrollBar = (AnimatedScreenElement)scrollbar;
            mScrollBar.mAlignV = ScreenElement.AlignV.TOP;
            removeElement(scrollbar);
            addElement(mScrollBar);
        }
        mSelectedIdVar = new IndexedNumberVariable(mName, "selectedId", mRoot.getContext().mVariables);
        mLaunchAction = new LaunchAction(node, root);
        mOpenHorizontalScroll = Boolean.parseBoolean(node.getAttribute("onItemHorizontalScroll"));
        mOpenOnItemClick = Boolean.parseBoolean(node.getAttribute("onItemClick"));
        mOpenOnItemClick = Boolean.parseBoolean(node.getAttribute("onItemLongClick"));
        tmp = node.getAttribute("enabled");
        if (!TextUtils.isEmpty(tmp)) {
            if (tmp.equalsIgnoreCase("false")) {
                mEnable = false;
            } else if (tmp.equalsIgnoreCase("true")) {
                mEnable = true;
            } else {
                mEnableExpression = Expression.build(tmp);
            }
        }
        initConfig(root.getContext().mContext);
        isFinished=false;
    }
    protected boolean isEnable() {
        return mEnable && (mEnableExpression == null || mEnableExpression.evaluate(mRoot.getContext().mVariables) > 0);
    }
    private HashMap<String, Integer> mMixedItemNameForIndexMap;

    private void createMixedListCache() {
        mMixedItemNameForIndexMap = new HashMap<String, Integer>();
        ArrayList<ListItemElement> arrayList = mMixedListHelp.getItemElements();
        int index = 0;
        for (ListItemElement root : arrayList) {
            ArrayList<ListItemElement> cache = new ArrayList<ListScreenElement.ListItemElement>();
            for (int start = 0; start < getCacheCount(); start++) {
                ListItemElement newElement = new ListItemElement(root.mNode, root.mRoot);
                newElement.init();
                mInnerGroup.addElement(newElement);
                cache.add(newElement);
            }
            mMixedItemNameForIndexMap.put(root.getName(), index);
            index++;
        }
    }
    private static class MixedListHelp {

        private HashMap<String, MixedItem> mMixedMap = new HashMap<String, MixedItem>();

        public int getSize() {
            return mMixedMap.size();
        }
        public MixedListHelp(Element node, final ScreenElementRoot root){
            Utils.traverseXmlElementChildren(node, MixedItem.LOG_TAG, new Utils.XmlTraverseListener() {

                @Override
                public void onChild(Element element) {
                    MixedItem item = new MixedItem(element, root);
                    mMixedMap.put(item.mName, item);
                }
            });

        }

        public void init() {
            Iterator<Entry<String, MixedItem>> iter = mMixedMap.entrySet().iterator();
            while (iter.hasNext()) {
                iter.next().getValue().mItem.init();
            }

        }
        public ArrayList<ListItemElement> getItemElements() {
            ArrayList<ListItemElement> arrayList = new ArrayList<ListItemElement>(getSize());
            Iterator<Entry<String, MixedItem>> iter = mMixedMap.entrySet().iterator();
            while (iter.hasNext()) {
                MixedItem item = iter.next().getValue();
                if (item.mItem != null) {
                    arrayList.add(item.mItem);
                }
            }
            return arrayList;
        }

        private ArrayList<ColumnInfo> getColumnInfos(String name) {
            MixedItem blendedItem = mMixedMap.get(name);
            if (blendedItem != null) {
                return blendedItem.mColumnInfos;
            }
            return null;
        }

        public AttrDataBinders getAttrDataBinders(String name) {
            MixedItem blendedItem = mMixedMap.get(name);
            if (blendedItem != null) {
                return blendedItem.mAttrDataBinders;
            }
            return null;
        }

        public ListItemElement getListItemElement(String name) {
            MixedItem item = mMixedMap.get(name);
            if (item != null) {
                return item.mItem;
            }
            return null;
        }

        public ListItemElement getListItemElement() {
            Iterator<Entry<String, MixedItem>> iter = mMixedMap.entrySet().iterator();
            while (iter.hasNext()) {
                return iter.next().getValue().mItem;
            }
            return null;
        }

        public LaunchAction getLaunchAction(String name) {
            MixedItem blendedItem = mMixedMap.get(name);
            if (blendedItem != null) {
                return blendedItem.mLaunchAction;
            }
            return null;
        }

        public static class MixedItem {

            private String                mName;
            private ListItemElement       mItem;
            private ArrayList<ColumnInfo> mColumnInfos;
            private LaunchAction          mLaunchAction;
            protected AttrDataBinders     mAttrDataBinders;
            private static final String   LOG_TAG = "MaxedItem";

            public MixedItem(Element node, final ScreenElementRoot root){
                mName = node.getAttribute("name");
                String data = node.getAttribute("data");
                if (TextUtils.isEmpty(data)) {
                    Log.e(LOG_TAG, "no data");
                    throw new IllegalArgumentException("List: no data");
                }
                mColumnInfos = ColumnInfo.createColumnInfoList(data);
                mLaunchAction = new LaunchAction(node, root);

                Element element = Utils.getChild(node, "AttrDataBinders");
                if (element != null) {
                    mAttrDataBinders = new AttrDataBinders(element, root.getContext().mContextVariables);
                }
                Utils.traverseXmlElementChildren(node, ListItemElement.TAG_NAME, new Utils.XmlTraverseListener() {

                    @Override
                    public void onChild(Element element) {
                        if (mItem == null) {
                            mItem = new ListItemElement(element, root);
                            mItem.setName(mName);
                        }
                    }
                });

            }

        }

    }

    private void initConfig(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    private void bindData(ListItemElement item, int index, int crrentIndex) {
        if (crrentIndex < 0 || crrentIndex >= mItemCount) {
            Log.e(LOG_TAG, "invalid item data");
            return;
        }
        String name = null;
        if (mMixedListHelp != null && TextUtils.isEmpty(name)) {
            if (crrentIndex >= 0 && crrentIndex < mDatas.size()) {
                name = mDatas.get(crrentIndex).mMixedItemName;
            }
        }
        AttrDataBinders mAttrDataBinders = this.mAttrDataBinders;
        ArrayList<ColumnInfo> mColumnInfoList = this.mColumnInfoList;
        if (mMixedListHelp != null && !TextUtils.isEmpty(name)) {
            mColumnInfoList = mMixedListHelp.getColumnInfos(name);
            mAttrDataBinders = mMixedListHelp.getAttrDataBinders(name);
        }
        if (mAttrDataBinders != null) {
            mDataList.get(crrentIndex).mElementIndex = index;
            Object[] objects = mDataList.get(crrentIndex).mData;
            mDataList.get(crrentIndex).mNeedRebind = false;
            item.setDataIndex(crrentIndex);
            item.setY(descale((float)(crrentIndex * mItem.getHeight())));
            ContextVariables variables = getContext().mContextVariables;
            for (int i = 0, size = mColumnInfoList.size(); i < size; i++) {
                variables.setVar((mColumnInfoList.get(i)).mVarName, objects[i]);
            }
            mAttrDataBinders.bind(item);
        }
    }

    private void checkVisibility() {
        for (ScreenElement element : mInnerGroup.getElements()) {
            if (element instanceof ListItemElement) {
                int index = ((ListItemElement)element).getDataIndex();
                if (index >= 0 && index >= mTopIndex-1 && index <= mBottomIndex+1) {
                    element.show(true);
                } else {
                    element.show(false);
                }
            }
        }
    }

    private ListItemElement getItem(int position) {
        if (position < 0 || position >= mItemCount) {
            return null;
        }
        ListItemElement item = null;
        int elementIndex = ((DataIndexMap)mDataList.get(position)).mElementIndex;
        if (elementIndex >= 0) {
            item = (ListItemElement)mInnerGroup.getElements().get(elementIndex);
        }
        if (item == null || elementIndex < 0 || item.getDataIndex() != position) {
            elementIndex = getUseableElementIndex(position);
            item = (ListItemElement)mInnerGroup.getElements().get(elementIndex);
        }
        if (item.getDataIndex() != position || item.getDataIndex() < 0 || mDataList.get(position).mNeedRebind) {
            item.reset();
            bindData(item, elementIndex, position);
        }
        return item;
    }

    public static void logStackTrace() {
        java.util.Map<Thread, StackTraceElement[]> ts = Thread.getAllStackTraces();
        StackTraceElement[] ste = ts.get(Thread.currentThread());
        for (StackTraceElement s : ste) {
            Log.e(LOG_TAG, s.toString());
        }
    }

    private int getUseableElementIndex(int postion) {
        if (mMixedListHelp != null && mDatas.size() > 0) {
            DataWrapper data = mDatas.get(postion);
            if (data != null) {
                int index = mMixedItemNameForIndexMap.get(data.mMixedItemName);
                return postion % mCachedItemCount + index * mCachedItemCount;
            }
        }
        return postion % mCachedItemCount;
    }

    private void moveTo(float dy) {
        mOffsetY = getValidY(dy);
        resetBoundPosition();
        for (int i = Math.max(mTopIndex-1, 0); i <= Math.min(mBottomIndex+1, mItemCount - 1); i++) {
            getItem(i);
        }
        mInnerGroup.setY(descale((float)mOffsetY));
        checkVisibility();
        updateScrollBar();
    }

    private void resetBoundPosition() {
        int showCount = (int)Math.ceil(getHeight() / mItem.getHeight());
        mTopIndex = (int)Math.floor(-mOffsetY / mItem.getHeight());
        // mBottomIndex = Math.min((int)Math.floor((-mOffsetY+getHeight())/ mItem.getHeight()), mItemCount - 1);
        mBottomIndex = Math.min(showCount + mTopIndex, mItemCount - 1);
    }

    private void performAction(String data) {
        mRoot.onUIInteractive(this, data);
    }

    private void resetInner() {
        if (mScrollBar != null) {
            mScrollBar.show(false);
        }
        mCanTouch = false;
        mSpeedY = 0;
        mTouchMode = TOUCH_MODE_REST;
        mItemTouchHelp.clear();
        mOffsetX = 0;
    }

    private boolean mNeedSetVariables = false ;

    private void setVariables() {
        if(!mNeedSetVariables){
            return ;
        }
        int index = 0;
        for (ColumnInfo data : mColumnInfoList) {
            switch (data.mType) {
                case STRING: {
                    IndexedStringVariable variable = null;
                    if (mIndexedVariables[index] == null) {
                        variable = new IndexedStringVariable(mName, data.mVarName, mRoot.getContext().mVariables);
                        mIndexedVariables[index] = variable;
                    } else {
                        variable = (IndexedStringVariable)mIndexedVariables[index];
                    }
                    if (mSelectedId >= 0) {
                        String str = (String)((DataIndexMap)mDataList.get(mSelectedId)).mData[index];
                        variable.set(str);
                    }
                    break;
                }
                case INTEGER:
                case DOUBLE:
                case LONG:
                case FLOAT: {
                    IndexedNumberVariable numberVariable = null;
                    if (mIndexedVariables[index] == null) {
                        numberVariable = new IndexedNumberVariable(mName, data.mVarName, mRoot.getContext().mVariables);
                        mIndexedVariables[index] = numberVariable;
                    } else {
                        numberVariable = (IndexedNumberVariable)mIndexedVariables[index];
                    }
                    if (mSelectedId >= 0) {
                        Double double1 = Double.valueOf(((Number)((DataIndexMap)mDataList.get(mSelectedId)).mData[index]).doubleValue());
                        numberVariable.set(double1);
                    }
                    break;
                }
            }
            index++;
        }
    }

    private void updateScrollBar() {
        if (mScrollBar != null && mTouchMode == TOUCH_MODE_SCROLL) {
            float allItemHeight = mItemCount * mItem.getHeight();
            float showHeight = getHeight();
            float scale = showHeight / allItemHeight;
            boolean bool = true;
            if (scale >= 1) {
                scale = 0;
                bool = false;
            }
            double d4 = Math.min(1, mInnerGroup.getY() / (showHeight - allItemHeight));
            mScrollBar.setY(descale((float)(d4 * (showHeight * (1 - scale)))));
            mScrollBar.setH(descale((float)(showHeight * scale)));
            if (mScrollBar.isVisible() != bool) {
                mScrollBar.show(bool);
            }
        }
    }

    protected void addColumn(String mName, Object[] objects, Object[] oldobjects) {

        if (mName == null || objects == null) {
            return;
        }
        int index = -1;
        for (ColumnInfo info : mColumnInfoList) {
            if (mName.equals(info.mVarName)) {
                index = mColumnInfoList.indexOf(info);
                break;
            }
        }
        if (index != -1) {
            int i = 0;
            for (DataIndexMap dataIndexMap : mDataList) {
                Object object = null;
                if (i < objects.length) {
                    object = objects[i];
                }
                dataIndexMap.setData(index, object,oldobjects);
                if (dataIndexMap.mElementIndex >= 0) {
                    getItem(i);
                }
                i++;
            }
        } else {
            try {
                addItem(objects);
            } catch (ScreenElementLoadException e) {
                e.printStackTrace();
            }
        }
        requestUpdate();

    }

    public void addItem(Object[] objects) throws ScreenElementLoadException {
        addItem(objects,objects,null);
    }

    private int getCacheCount() {
        if (mCachedItemCount == 0) {
            float showHight = Math.max(super.getHeight(), scale(evaluate(mMaxHeight)));
            mVisibleItemCount = (int)Math.ceil(showHight / mItem.getHeight());
            mCachedItemCount = 2 * mVisibleItemCount;
        }
        return mCachedItemCount;
    }

    private boolean mCheckValidateType = false;

    public void addItem(Object[] objects, Object[] oldObjects,String name) throws ScreenElementLoadException {
        if (mMixedListHelp != null && mMixedItemNameForIndexMap == null) {
            createMixedListCache();
        }
        ArrayList<ColumnInfo> mColumnInfoList = this.mColumnInfoList;
        if (mMixedListHelp != null) {
            mColumnInfoList = mMixedListHelp.getColumnInfos(name);
        }
        if (objects == null || objects.length == 0 || objects.length != mColumnInfoList.size()) {
            return;
        }
        if (mCheckValidateType) {
            for (int index = 0, size = objects.length; index < size; index++) {
                if (!mColumnInfoList.get(index).validate(objects[index])) {
                    Log.e(LOG_TAG, "invalid item data type: " + objects[index]);
                    return;
                }
            }
        }
        mDataList.add(new DataIndexMap((Object[])objects.clone(),(Object[])oldObjects.clone()));
        mCurrentIndex++;
        mItemCount++;
        setActualHeight(descale(getHeight()));
        float showHight = Math.max(super.getHeight(), scale(evaluate(mMaxHeight)));
        mVisibleItemCount = (int)Math.ceil(showHight / mItem.getHeight());
        mCachedItemCount = 2 * mVisibleItemCount;

        int nowSize = mInnerGroup.getElements().size();
        if (nowSize < mCachedItemCount) {
            ListItemElement rootItem = mItem;
            if (mMixedListHelp != null) {
                rootItem = mMixedListHelp.getListItemElement(name);
            }
            ListItemElement itemElement = new ListItemElement(rootItem.mNode, rootItem.mRoot);
            itemElement.init();
            mInnerGroup.addElement(itemElement);
            bindData(itemElement, nowSize, mCurrentIndex);
            requestUpdate();
        }
    }

    public void tick(long currentTime) {
        super.tick(currentTime);
        if(isTickFinished){
            doTick(currentTime);
        }
    }

    public void doTick(long currentTime) {
        switch (mTouchMode) {
            case TOUCH_MODE_UP:
                mListFlingHelp.doTick(currentTime);
                break;
            case TOUCH_MODE_ITEM_SCROLL:
                itemScrollHelp.onItemScroll(mOffsetX);
                break;
            case TOUCH_MODE_ITEM_UP:
                itemScrollHelp.doTick(currentTime);
                break;
            default:
                moveTo(mOffsetY);
                break;
        }
        mItemTouchHelp.doTick(currentTime);

    }

    private float getValidY(float y) {
        return Math.min(0, Math.max(getHeight() - mItemCount * mItem.getHeight(), y));
    }

    private boolean isHeadOrBottom(float y) {
        return y >= 0 || y <= (getHeight() - mItemCount * mItem.getHeight());
    }

    public ScreenElement findElement(String name) {
        if (mSelectedId >= 0 && mSelectedId < mItemCount) {
            int i = ((DataIndexMap)mDataList.get(mSelectedId)).mElementIndex;
            if (i >= 0) {
                ScreenElement localScreenElement = ((ListItemElement)mInnerGroup.getElements().get(i)).findElement(name);
                if (localScreenElement != null) return localScreenElement;
            }
        }
        return super.findElement(name);
    }
    private boolean isFinished=false;
    public void finish() {
        super.finish();
        isFinished=true;
        removeAllItems();
        if (mListData != null) {
            mListData.finish();
        }
    }
    public float getHeight() {
        if (mMaxHeight == null) return super.getHeight();
        return Math.min(mItemCount * mItem.getHeight(), scale(evaluate(mMaxHeight)));
    }

    public ArrayList<ColumnInfo> getColumnInfoList() {
        return mColumnInfoList;
    }

    public ArrayList<ColumnInfo> getColumnInfoList(String name) {
        if (mMixedListHelp != null && !TextUtils.isEmpty(name)) {
            return mMixedListHelp.getColumnInfos(name);
        }
        return mColumnInfoList;
    }
    public void init() {
        super.init();
        isFinished=false;
        mItem.init();
        resetInner();
        mSelectedId = -1;
        mSelectedIdVar.set(mSelectedId);
        setVariables();
        if (mListData != null) {
            mListData.init();
        }
        if (mMixedListHelp != null) {
            mMixedListHelp.init();
        }
    }

    protected ScreenElement onCreateChild(final Element node) {
        Utils.traverseXmlElementChildren(node, ListItemElement.TAG_NAME, new Utils.XmlTraverseListener() {

            @Override
            public void onChild(Element element) {
                if (mInnerGroup == null) {
                    mInnerGroup = new ElementGroup(null, mRoot);
                    mItem = new ListItemElement(element, mRoot);
                }
            }
        });
        if (mInnerGroup == null && mIsMixedList) {
            mInnerGroup = new ElementGroup(null, mRoot);
        }
        return mInnerGroup;
    }

    private static final int      TOUCH_MODE_REST        = -1;
    private static final int      TOUCH_MODE_DOWN        = 0;
    private static final int      TOUCH_MODE_SCROLL      = 1;
    private static final int      TOUCH_MODE_UP          = 2;

    private static final int      TOUCH_MODE_ITEM_SCROLL = 10;
    private static final int      TOUCH_MODE_ITEM_UP     = 11;

    private static final int      INVALID_POSITION       = -1;

    private int                   mTouchMode             = TOUCH_MODE_REST;

    private VelocityTracker       mVelocityTracker;
    private int                   mTouchSlop;
    private int                   mMaximumFlingVelocity;
    private float                 mScale                 = -1;
    private float                 mLastX;
    private float                 mLastY;
    private float                 mItemWidth             = 0;

    private boolean               mCanTouch              = false;
    private boolean               mOpenHorizontalScroll  = false;
    private boolean               mOpenOnItemClick       = false;
    private boolean               mOpenOnItemLongClick   = false;
    private boolean               mEnable                = true;

    private final OnItemTouchHelp mItemTouchHelp         = new OnItemTouchHelp();
    private final ListFlingHelp   mListFlingHelp         = new ListFlingHelp();

    public boolean onTouch(MotionEvent event) {
        if (!isVisible() || !isEnable()) {
            return false;
        }
        initTouch();
        mVelocityTracker.addMovement(event);
        float touchX = event.getX() / mScale;
        float touchY = event.getY() / mScale;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (mTouchMode) {
                    case TOUCH_MODE_ITEM_UP:
                        if (touched(touchX, touchY)) {
                            if (mSelectedId != INVALID_POSITION) {
                                if (getPosition(touchY) != mSelectedId) {
                                    itemScrollHelp.startClosedAnimation();
                                } else if (!itemScrollHelp.inTouchItemDeleteRect(touchX)) {
                                    itemScrollHelp.startClosedAnimation();
                                }
                            }
                        }
                        break;
                    default:
                        resetInner();
                        mCanTouch = touched(touchX, touchY);
                        if (!mCanTouch) {
                            break;
                        }
                        mTouchMode = TOUCH_MODE_DOWN;
                        mPressed = true;
                        performAction("down");
                        mSelectedId = getPosition(touchY);
                        mSelectedIdVar.set(mSelectedId);
                        setVariables();
                        mTouchStartX = touchX;
                        mTouchStartY = touchY;
                        mLastX = touchX;
                        mLastY = touchY;
                        updateScrollBar();
                        if (isShowFullForPositionItem(mSelectedId)) {
                            mItemTouchHelp.checkItemtPress(touchY);
                        }
                        if (mOpenOnItemLongClick) {
                            mItemTouchHelp.checkItemLongClick(touchY);
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mCanTouch) {
                    break;
                }
                switch (mTouchMode) {
                    case TOUCH_MODE_DOWN:
                        onTouchMoveStart(touchX, touchY);
                        break;
                    case TOUCH_MODE_SCROLL:
                        mOffsetY += (touchY - mLastY);
                        break;
                    case TOUCH_MODE_ITEM_SCROLL:
                        mOffsetX += (touchX - mLastX);
                        if (mItemWidth == 0) {
                            mItemWidth = getItem(mSelectedId).getWidth();
                        }
                        if (mItemWidth > 0) {
                            mOffsetX = Math.min(mItemWidth, Math.max(-mItemWidth, mOffsetX));
                        }
                        break;
                    case TOUCH_MODE_ITEM_UP:
                        break;
                }
                mLastX = touchX;
                mLastY = touchY;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mCanTouch) {
                    break;
                }
                mPressed = false;
                performAction("up");
                mItemTouchHelp.clearItemtPress();
                switch (mTouchMode) {
                    case TOUCH_MODE_DOWN:
                        if (mOpenOnItemClick) {
                            mItemTouchHelp.checkItemtClick(touchY);
                        }
                        // resetInner();
                        break;
                    case TOUCH_MODE_SCROLL:
                        mTouchMode = TOUCH_MODE_UP;
                        mListFlingHelp.maybeStartAnimation();
                        break;
                    case TOUCH_MODE_ITEM_SCROLL:
                        mTouchMode = TOUCH_MODE_ITEM_UP;
                        itemScrollHelp.startOpenOrClosedAnimation();
                        break;
                    case TOUCH_MODE_ITEM_UP:
                        if (mSelectedId != INVALID_POSITION) {
                            itemScrollHelp.mayeDeleteClick(touchX, mSelectedId);
                        }
                        break;
                    default:
                        resetInner();
                        break;
                }
                break;
//            case MotionEvent.ACTION_CANCEL:
//                if (!mCanTouch) {
//                    break;
//                }
//                mPressed = false;
//                performAction("cancel");
//                resetInner();
//                break;
        }

        return true;
    }

    private void initTouch() {
        if (mScale == -1) {
            mScale = mRoot.getMatrixScale();
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void onTouchMoveStart(float touchX, float touchY) {
        int diffX = (int)Math.abs(touchX - mTouchStartX);
        int diffY = (int)Math.abs(touchY - mTouchStartY);
        if (Math.abs(diffY) >= mTouchSlop && Math.abs(diffY) >= 2 * Math.abs(diffX)) {
            mTouchMode = TOUCH_MODE_SCROLL;
            mItemTouchHelp.clearItemtPress();
        } else if (mOpenHorizontalScroll && Math.abs(diffX) >= mTouchSlop && Math.abs(diffX) >= 2 * Math.abs(diffY)
                   && isShowFullForPositionItem(mSelectedId)) {
            mTouchMode = TOUCH_MODE_ITEM_SCROLL;
            mItemTouchHelp.onItemPressNow(mSelectedId);
        }
    }

    private boolean isShowFullForPositionItem(int position) {
        if (position != INVALID_POSITION) {
            return mOffsetY + mItem.getHeight() * 0.15f >= -position * mItem.getHeight()
                   && mOffsetY + (position + 0.85f) * mItem.getHeight() <= getHeight();
        }
        return false;
    }

    public int getPosition(int touchX, int touchY) {
        touchY = (int)(touchY - getY());
        int itemHeight = (int)mItem.getHeight();
        int itemWidth = (int)mItem.getWidth();
        if (mBottomIndex == 0) {
            mBottomIndex = Math.min(mItemCount - 1, mVisibleItemCount);
        }
        for (int i = mTopIndex; i <= mBottomIndex; i++) {
            int itemTop = (int)(i * itemHeight + mOffsetY);
            Rect mRect = new Rect(0, itemTop, itemWidth, itemTop + itemHeight);
            if (mRect.contains(touchX, touchY)) {
                return i;
            }
        }
        return INVALID_POSITION;
    }

    private int getPosition(float touchY) {
        touchY = (int)(touchY - getY());
        int itemHeight = (int)mItem.getHeight();
        if (mBottomIndex == 0) {
            mBottomIndex = Math.min(mItemCount - 1, mVisibleItemCount);
        }
        for (int i = mTopIndex; i <= mBottomIndex; i++) {
            int top = (int)(i * itemHeight + mOffsetY);
            int bottom = top + itemHeight;
            if (top < bottom && touchY >= top && touchY <= bottom) {
                return i;
            }
        }
        return INVALID_POSITION;
    }

    public void removeAllItems() {
        mInnerGroup.removeAllElements();
        mInnerGroup.setY(0);
        mDataList.clear();
        mCurrentIndex = -1;
        mItemCount = 0;
        setActualHeight(descale(getHeight()));
        mMixedItemNameForIndexMap = null;
    }

    private boolean mUpdateDb = true;
    public void removeItem(int position) {
        mTouchMode = TOUCH_MODE_REST;
        if (position < 0 || position >= mItemCount) {
            return;
        }
        isRemoveItem=true;
        mDataList.remove(position);
        DeleteExtra extra = mDeleteExtras != null ? mDeleteExtras.remove(position) : null;
        if (mDatas != null && mDatas.size() > 0) {
            DataWrapper mDelete = mDatas.remove(position);
            extra = mDelete != null ? mDelete.mDeleteExtra : null;
        }
        if (mUpdateDb && extra != null) {
            startInRemove();
            updateUriRead(extra.mUri, extra.mWhere, extra.mHashMap);
        }
        mItemCount--;
        setActualHeight(descale(getHeight()));
        ArrayList<ScreenElement> arrayList = mInnerGroup.getElements();
        for (ScreenElement element : arrayList) {
            ListItemElement item = (ListItemElement)element;
            item.setDataIndex(-1);
        }
        moveTo(mOffsetY);
        requestUpdate();
    }

    private ContentResolver mContentResolver;

    public void updateUriRead(String uri, String where, HashMap<String, String> hashMap) {
        try {
            if (mContentResolver == null) {
                mContentResolver = getContext().mContext.getContentResolver();
            }
            ContentValues values = new ContentValues();
            Iterator<Entry<String, String>> iter = hashMap.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, String> entry = iter.next();
                values.put(entry.getKey(), entry.getValue());
            }
            mContentResolver.update(Uri.parse(uri), values, where, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean  mInRemove;
    private Runnable mRemoveRunnable;

    private void startInRemove() {
        if (mRemoveRunnable == null) {
            mRemoveRunnable = new Runnable() {

                @Override
                public void run() {
                    mInRemove = false;
                }
            };
        }
        cancleInRemove();
        mInRemove = true;
        getContext().getHandler().postDelayed(mRemoveRunnable, 50);
    }

    private void cancleInRemove() {
        if (mRemoveRunnable != null) {
            getContext().getHandler().removeCallbacks(mRemoveRunnable);
        }
        mInRemove = false;
    }

    private void startActivity(int position) {
        LaunchAction launchAction = mLaunchAction;
        if (mMixedListHelp != null) {
            launchAction = mMixedListHelp.getLaunchAction(mDatas.get(position).mMixedItemName);
        }
        Intent intent = launchAction.perform();
        getContext().mRoot.unlocked(putExtras(intent, launchAction.mOpenExtras, position), launchAction.mEnterResName,
                                    launchAction.mExitResName);
    }

    private Intent putExtras(Intent intent, String[] mOpenExtras, int position) {
        if (intent == null || mOpenExtras == null || mOpenExtras.length == 0) {
            return intent;
        }
        ArrayList<ColumnInfo> columnInfos = mColumnInfoList;
        if (mMixedListHelp != null) {
            columnInfos = mMixedListHelp.getColumnInfos(mDatas.get(position).mMixedItemName);
        }
        DataIndexMap dataIndexMap = mDataList.get(position);
        for (String str : mOpenExtras) {
            Object value = getObjectForName(str, columnInfos, dataIndexMap);
            if (value instanceof String) {
                intent.putExtra(str, (String)value);
            }
        }
        return intent;

    }

    private Object getObjectForName(String name, ArrayList<ColumnInfo> columnInfos, DataIndexMap dataIndexMap) {
        int i = 0;
        for (ColumnInfo columnInfo : columnInfos) {
            if (columnInfo.mVarName.equals(name)) {
                return dataIndexMap.mOldObjects[i];
            }
            i++;
        }
        return null;
    }

    private final class OnItemTouchHelp {

        private int       mPressPositions = INVALID_POSITION;
        private Runnable  mPressRunnable;
        private Runnable  mLongPressRunnable;
        private Handler   mHandler;
        private final int PRESS_TIMEOUT;
        private final int LONGCLICK_TIMEOUT;

        public OnItemTouchHelp(){
            PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout() / 6;
            LONGCLICK_TIMEOUT = ViewConfiguration.getLongPressTimeout();
            mHandler = getContext().getHandler();
        }

        public void clear() {
            clearItemtPressNow();
            clearItemLongClick();
        }

        private void checkItemtPress(float touchY) {
            clearItemtPressNow();
            if (mTouchMode == TOUCH_MODE_DOWN) {
                int position = getPosition(touchY);
                if (position != INVALID_POSITION) {
                    if (mPressRunnable == null) {
                        mPressRunnable = new Runnable() {

                            @Override
                            public void run() {
                                if (mTouchMode == TOUCH_MODE_DOWN) {
                                    int position = getPosition(mLastY);
                                    if (position != INVALID_POSITION) {
                                        onItemPress(position);
                                    }
                                }
                            }
                        };
                    }
                    mHandler.postDelayed(mPressRunnable, PRESS_TIMEOUT);
                }
            }
        }

        public void doTick(long currentTime) {
            int alpha = -1;
            if (getHighlighElement() != null) {
                if (mInShowHighlight && mShowHighlight != null) {
                    alpha = (int)mShowHighlight.doTick(currentTime);
                } else if (mInDissiHighlight && mDissighlight != null) {
                    alpha = (int)mDissighlight.doTick(currentTime);
                }
            }
            if (alpha >= 0) {
                getHighlighElement().setAlpha(alpha);
            }

        }

        public Animation mShowHighlight;
        public Animation mDissighlight;
        private boolean  mInShowHighlight;
        private boolean  mInDissiHighlight;

        private ListItemElement getListItemElement() {
            return getItem(mPressPositions);
        }

        private AnimatedScreenElement getHighlighElement() {
            ListItemElement element = getItem(mPressPositions);
            if (element != null) {
                return element.getHighlight();
            }
            return null;
        }

        private void clearItemtPressNow() {
            if (mPressPositions != INVALID_POSITION) {
                mHandler.removeCallbacks(mPressRunnable);
                ListItemElement element = getItem(mPressPositions);
                if (element != null) {
                    element.showHighlight(false);
                    mInDissiHighlight = false;
                    mInShowHighlight = false;
                }
            }
            mPressPositions = INVALID_POSITION;
        }

        private void clearItemtPress() {
            if (mPressPositions != INVALID_POSITION) {
                mHandler.removeCallbacks(mPressRunnable);
                if (mDissighlight == null && getHighlighElement() != null) {
                    mDissighlight = new Animation(getListItemElement().getDefaultAlpha(), 0, 200);
                    mDissighlight.setAnimatorListener(new AnimationListener() {

                        @Override
                        public void onStateChange(int state) {
                            if (AnimationListener.END == state) {
                                mInDissiHighlight = false;
                                mPressPositions = INVALID_POSITION;
                            }
                        }
                    });
                }
                if (getHighlighElement() != null && mDissighlight != null) {
                    mDissighlight.start();
                    mInDissiHighlight = true;
                    mInShowHighlight = false;
                }
            }
        }

        private void onItemPress(int position) {
            mPressPositions = position;
            if (mShowHighlight == null && getHighlighElement() != null) {
                mShowHighlight = new Animation(0, getListItemElement().getDefaultAlpha(), 200);
                mShowHighlight.setAnimatorListener(new AnimationListener() {

                    @Override
                    public void onStateChange(int state) {
                        if (AnimationListener.END == state) {
                            mInShowHighlight = false;
                        }

                    }
                });
                getHighlighElement().show(true);
            }
            if (getHighlighElement() != null && mShowHighlight != null) {
                mShowHighlight.start();
                mInShowHighlight = true;
                mInDissiHighlight = false;
            }
        }

        private void onItemPressNow(int position) {
            mPressPositions = position;
            if (getHighlighElement() != null) {
                getHighlighElement().setAlpha(getListItemElement().getDefaultAlpha());
                mInDissiHighlight = false;
                mInShowHighlight = false;
            }
        }

        private void checkItemtClick(float touchY) {
            if (mTouchMode == TOUCH_MODE_DOWN) {
                int position = getPosition(touchY);
                if (position != INVALID_POSITION) {
                    onItemtClick(position);
                }
            }
        }

        private void onItemtClick(int position) {
            if (false) {
                clearItemtPress();
                removeItem(position);
                return;
            }
            onItemPress(position);
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    clearItemtPress();
                }
            }, 100);
            startActivity(position);
            if (DEBUG) {
                Log.e(LOG_TAG, "  onItemtClick ---position: " + position);
            }
        }

        private void checkItemLongClick(float touchY) {
            if (mTouchMode == TOUCH_MODE_DOWN) {
                int position = getPosition(touchY);
                if (position != INVALID_POSITION) {
                    if (mLongPressRunnable == null) {
                        mLongPressRunnable = new Runnable() {

                            @Override
                            public void run() {
                                if (mTouchMode == TOUCH_MODE_DOWN) {
                                    int position = getPosition(mLastY);
                                    if (position != INVALID_POSITION) {
                                        mTouchMode = TOUCH_MODE_REST;
                                        onItemtLongClick(position);
                                    }
                                }
                            }
                        };
                    }
                    mHandler.postDelayed(mLongPressRunnable, LONGCLICK_TIMEOUT);
                }
            }
        }

        private void clearItemLongClick() {
            if (mLongPressRunnable != null) {
                mHandler.removeCallbacks(mLongPressRunnable);
            }
        }

        private void onItemtLongClick(int position) {
            if (DEBUG) {
                Log.e(LOG_TAG, "  onItemtLongClick ---position: " + position);
            }
        }

    }

    private final class ListFlingHelp {

        private Animation              animation;
        private AnimationListener      listenerListFing;
        private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
        private long                   lastAnimationTime;
        private static final long      DURATION_MAX_TIME      = 2500;
        private static final long      DURATION_MIN_TIME      = 500;
        private static final float     PARAMET                = 2f;

        void maybeStartAnimation() {
            VelocityTracker vt = mVelocityTracker;
            vt.computeCurrentVelocity(1000);
            mSpeedY = vt.getYVelocity();
            if (Math.abs(mSpeedY) >= 400) {
                startAnimation();
            } else {
                resetInner();
            }
        }

        public void doTick(long currentTime) {
            if (animation != null) {
                mOffsetY += animation.doTick(currentTime) * (currentTime - lastAnimationTime) / 1000;
                lastAnimationTime = currentTime;
                moveTo(mOffsetY);
                requestUpdate();
            }
        }

        void startAnimation() {
            lastAnimationTime = SystemClock.elapsedRealtime();
            long duration = Math.min(DURATION_MAX_TIME,
                                     Math.max(DURATION_MIN_TIME,
                                              (long)(Math.abs(mSpeedY) * DURATION_MAX_TIME * PARAMET / mMaximumFlingVelocity)));
            animation = new Animation(mSpeedY, 0, duration);
            animation.setAnimatorListener(getListFlingListener());
            animation.setInterpolator(decelerateInterpolator);
            animation.start();
            requestUpdate();
        }

        AnimationListener getListFlingListener() {
            if (listenerListFing == null) {
                listenerListFing = new AnimationListener() {

                    @Override
                    public void onStateChange(int state) {
                        if (state == AnimationListener.END) {
                            if (mTouchMode == TOUCH_MODE_UP) {
                                mTouchMode = TOUCH_MODE_REST;
                            }
                            animation = null;
                        }
                    }
                };
            }
            return listenerListFing;
        }

    }

    private final ItemScrollHelp itemScrollHelp = new ItemScrollHelp();

    private final class ItemScrollHelp {

        private boolean           showDeleteItem;
        public Animation          animation;

        private AnimationListener listenerClose;
        private AnimationListener listenerOpen;

        private static final int  DUTATION_TIME = 230;

        public AnimationListener getCloseListener() {
            if (listenerClose == null) {
                listenerClose = new AnimationListener() {

                    @Override
                    public void onStateChange(int state) {
                        if (state == AnimationListener.END) {
                            clearScroll();
                            animation = null;
                            mItemTouchHelp.clearItemtPress();
                        }
                    }
                };
            }
            return listenerClose;
        }

        void startClosedAnimation() {
            float width = mItem.getDeleteWidth();
            startAnimation(mOffsetX > 0 ? width : -width, 0, ItemScrollHelp.DUTATION_TIME, getCloseListener());

        }

        private boolean mOpenForSeep = false;

        void startOpenOrClosedAnimation() {
            float width = getWidth();
            boolean open = Math.abs(mOffsetX) >= width * 0.25f;
            if (mOpenForSeep) {
                VelocityTracker vt = mVelocityTracker;
                vt.computeCurrentVelocity(1000);
                mSpeedX = vt.getXVelocity();
                open = open || Math.abs(mSpeedX) >= 1000;
            }
            if (open || Math.abs(mSpeedX) >= 1000) {
                if (mOffsetX < 0) {
                    onDelteClick(mSelectedId);
                } else {
                    onOpenClick(mSelectedId);
                }
                return;
            }
            float end = open ? (mOffsetX > 0 ? width : -width) : 0;
            int durion = Math.max(DUTATION_TIME - 100, Math.abs((int)(DUTATION_TIME * ((end - mOffsetX) / width))));
            showDeleteItem = mOffsetX < 0;
            startAnimation(mOffsetX, end, durion, open ? getOpenListener() : getCloseListener());
        }

        void doTick(long currentTime) {
            if (translationY != null) {
                float y = translationY.doTick(currentTime);
                if (translationY != null && translationY.inAnimation()) {
                    itemTranslationY(y);
                }
            } else if (animation != null) {
                float x = animation.doTick(currentTime);
                if (animation != null) {
                    onItemScroll(x);
                }
            }

        }

        AnimationListener getOpenListener() {
            if (listenerOpen == null) {
                listenerOpen = new AnimationListener() {

                    @Override
                    public void onStateChange(int state) {
                        if (state == AnimationListener.END) {
                            animation = null;
                            mItemTouchHelp.clearItemtPress();
                        }
                    }
                };
            }
            return listenerOpen;
        }

        void startAnimation(float start, float end, int duration, AnimationListener listener) {
            if (animation != null && animation.inAnimation()) {
                return;
            }
            animation = new Animation(start, end, duration);
            animation.setAnimatorListener(listener);
            animation.start();
            requestUpdate();
        }

        void clearScroll() {
            ListItemElement element = getItem(mSelectedId);
            if (element == null) {
                mTouchMode = TOUCH_MODE_REST;
                return;
            }
            mTouchMode = TOUCH_MODE_REST;
            mOffsetX = 0;
            element.setTranslationX(mOffsetX);
        }

        void onItemScroll(float mOffsetX) {
            ListItemElement element = getItem(mSelectedId);
            if (element == null) {
                mTouchMode = TOUCH_MODE_REST;
                return;
            }
            element.setTranslationX(descale(mOffsetX));
            requestUpdate();
        }

        private boolean inTouchItemDeleteRect(float touchX) {
            if (showDeleteItem) {
                return touchX >= mItem.getWidth() - mItem.getDeleteWidth() && touchX <= mItem.getWidth();
            } else {
                return touchX >= 0 && touchX <= mItem.getDeleteWidth();
            }
        }

        private void mayeDeleteClick(float touchX, int position) {
            if (inTouchItemDeleteRect(touchX)) {
                if (showDeleteItem) {
                    onDelteClick(position);
                } else {
                    onOpenClick(position);
                }
            }
        }

        private void onDelteClick(int position) {
            startRemoveAnimation(position);

        }

        private void onOpenClick(int position) {
            startOpenAnimation(position);
            if (DEBUG) Log.e(LOG_TAG, "---openClick--position:" + position);

        }

        public void startRemoveAnimation(final int position) {
            startOutAnimation(position, new AnimationListener() {

                @Override
                public void onStateChange(int state) {
                    if (state == AnimationListener.END) {
                        animation = null;
                        startTranslationY(position, false);
                    }
                }
            });
        }

        public void startOpenAnimation(final int position) {
            startOutAnimation(position, new AnimationListener() {

                @Override
                public void onStateChange(int state) {
                    if (state == AnimationListener.END) {
                        animation = null;
                        mItemTouchHelp.clearItemtPress();
                        startTranslationY(position, true);
                    }
                }
            });
        }

        private void startOutAnimation(final int position, AnimationListener listener) {
            float deletWidth = Math.abs(mOffsetX) < getWidth() ? Math.abs(mOffsetX) : getWidth();
            float itemWidth = mItem.getWidth();
            int duration = Math.min(150, (int)((itemWidth - deletWidth) / itemWidth * 300));
            startAnimation(mOffsetX > 0 ? deletWidth : -deletWidth, mOffsetX > 0 ? itemWidth : -itemWidth, duration,
                           listener);
        }

        private Animation translationY;

        private void startTranslationY(final int position, final boolean open) {
            if (open) {
                startActivity(position);
            }
            translationY = new Animation(0, mItem.getHeight(), open ? 150 : 280);
            translationY.setIEasing(new EaseOutSine());
            translationY.setAnimatorListener(new AnimationListener() {

                @Override
                public void onStateChange(int state) {
                    if (state == AnimationListener.END) {
                        translationY = null;
                        // mItemTouchHelp.clearItemtPress();
                        removeItem(position);
                    }
                }
            });
            translationY.start();
            requestUpdate();
        }

        private void itemTranslationY(float y) {
            ListItemElement selectedItem = getItem(mSelectedId);
            selectedItem.setH(descale((float)(mItem.getHeight() - y)));

            float bottomY = getHeight() - mItemCount * mItem.getHeight();
            boolean moveTop = !(mOffsetY == bottomY);
            if (mItemCount - 1 < mVisibleItemCount) {
                moveTop = true;
            }
            if (mItemCount - 1 == mVisibleItemCount) {
                moveTop = (mTopIndex == 0);// mItemCount = 6 ,mVisibleItemCount =5 , mTopIndex =0 ,remove 3
                                           // ;mTopIndex//
                                           // =1;remove 4
            }
            if (mItemCount == mVisibleItemCount && mOffsetY < 0) {
                float percent = y / mItem.getHeight();
                float needMoveY = Math.abs(mOffsetY);
                float dy = percent * needMoveY;
                for (int i = 0; i < mSelectedId; i++) {
                    getItem(i).setY(i * mItem.getHeight() + dy);
                }
                selectedItem.setY(descale(mSelectedId * mItem.getHeight() + dy));
                y = percent * (mItem.getHeight() - needMoveY);
                moveTop = true;
            }
            if (mItemCount > mVisibleItemCount && mBottomIndex == mItemCount - 1 && moveTop
                && mOffsetY <= bottomY + mItem.getHeight()) {
                float percent = y / mItem.getHeight();
                float needMoveY = Math.abs(mOffsetY - bottomY);
                float dy = percent * needMoveY;
                for (int i = mSelectedId; i < mItemCount; i++) {
                    getItem(i).setY(i * mItem.getHeight() + (moveTop ? -dy : dy));
                }
                y = percent * (mItem.getHeight() - needMoveY);
                moveTop = false;
            }
            if (!moveTop) {
                float showY = descale((float)(mSelectedId * mItem.getHeight() + y));
                selectedItem.setY(showY);
            }
            int start = Math.max(0, moveTop ? mSelectedId + 1 : mTopIndex - 1);
            int end = Math.min(mItemCount - 1, moveTop ? mBottomIndex + 1 : mSelectedId - 1);
            itemTranslationY(start, end, moveTop ? -y : y);
            requestUpdate();
        }

        private void itemTranslationY(int startPostion, int endPosition, float y) {
            for (int i = startPostion; i <= endPosition; i++) {
                float showY = descale(i * mItem.getHeight() + y);
                getItem(i).setY(showY);
            }
        }
    }

    private class ListMaskHelp {

        private Paint               mPaint;
        private ArrayList<ListMask> mListMasks;

        private static final int    LAYER_FLAGS = Canvas.MATRIX_SAVE_FLAG | Canvas.CLIP_SAVE_FLAG
                                                  | Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
                                                  | Canvas.FULL_COLOR_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG;
        private static final String TAG         = "ListMask";

        public ListMaskHelp(Element node, final ScreenElementRoot root){
            Utils.traverseXmlElementChildren(node, TAG, new Utils.XmlTraverseListener() {

                @Override
                public void onChild(Element element) {
                    init();
                    mListMasks.add(new ListMask(element, root));
                }
            });

        }

        private void init() {
            if (mListMasks == null) {
                mListMasks = new ArrayList<ListMask>();
            }
            if (mPaint == null) {
                mPaint = new Paint();
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            }
            mNeedDrawMask = true;
        }

        private void drawMask(Canvas canvas) {
            for (ListMask mask : mListMasks) {
                mask.drawMask(canvas);
            }
        }

        private class ListMask {

            private int              mType  = -1;
            private Bitmap           mBitmap;
            private Expression       mHeightExpression;
            private Expression       mWidthExpression;

            private static final int TOP    = 0;
            private static final int BOTTOM = 1;

            public ListMask(Element node, final ScreenElementRoot root){
                String tmp = node.getAttribute("type");
                if (!TextUtils.isEmpty(tmp)) {
                    if ("top".equals(tmp)) {
                        mType = TOP;
                    } else if ("bottom".equals(tmp)) {
                        mType = BOTTOM;
                    }
                }
                ColorParser mStartColor = ColorParser.fromElement(node, "startColor");
                ColorParser mEndColor = ColorParser.fromElement(node, "endColor");
                tmp = node.getAttribute("angle");
                Orientation orientation = null;
                if (!TextUtils.isEmpty(tmp)) {
                    int angle = Integer.valueOf(tmp);
                    switch (angle) {
                        case 0:
                            orientation = Orientation.LEFT_RIGHT;
                            break;
                        case 45:
                            orientation = Orientation.BL_TR;
                            break;
                        case 90:
                            orientation = Orientation.BOTTOM_TOP;
                            break;
                        case 135:
                            orientation = Orientation.BR_TL;
                            break;
                        case 180:
                            orientation = Orientation.RIGHT_LEFT;
                            break;
                        case 225:
                            orientation = Orientation.TR_BL;
                            break;
                        case 270:
                            orientation = Orientation.TOP_BOTTOM;
                            break;
                        case 315:
                            orientation = Orientation.TL_BR;
                            break;
                    }
                }
                mWidthExpression = createExp(node, "w", "width");
                mHeightExpression = createExp(node, "h", "height");
                GradientDrawable drawable = new GradientDrawable(orientation, new int[] {
                        mStartColor.getColor(root.getVariables()), mEndColor.getColor(root.getVariables()) });
                drawable.setShape(GradientDrawable.RECTANGLE);
                mBitmap = drawableToBitmap(drawable, (int)getWidth(), (int)getHeight());
            }

            public float getWidth() {
                return (float)(mWidthExpression != null ? mWidthExpression.evaluate(getContext().mVariables) : -1);
            }

            public float getHeight() {
                return (float)(mHeightExpression != null ? mHeightExpression.evaluate(getContext().mVariables) : -1);
            }

            private void drawMask(Canvas canvas) {
                if (mType == TOP) {
                    if (mOffsetY < -getHeight() / 2) {
                        canvas.drawBitmap(mBitmap, getX(), getY(), mPaint);
                    }
                } else if (mType == BOTTOM) {
                    float bottom = ListScreenElement.this.getHeight() - mItemCount * mItem.getHeight();
                    if (mItemCount >= mVisibleItemCount && mOffsetY > bottom + getHeight() / 2 && mOffsetY != bottom) {
                        canvas.drawBitmap(mBitmap, getX(), getY() + ListScreenElement.this.getHeight() - getHeight(),
                                          mPaint);
                    }
                }
            }
        }

    }

    private Expression createExp(Element node, String name, String compatibleName) {
        Expression exp = Expression.build(node.getAttribute(name));
        if (exp == null && !TextUtils.isEmpty(compatibleName)) {
            exp = Expression.build(node.getAttribute(compatibleName));
        }
        return exp;
    }

    @Override
    public void doRender(Canvas canvas) {
        if(!isTickFinished){
            return ;
        }
        if (mNeedDrawMask) {
            canvas.saveLayer(getX(), getY(), canvas.getWidth(), canvas.getHeight(), null, ListMaskHelp.LAYER_FLAGS);
        }
        super.doRender(canvas);
        if (mNeedDrawMask) {
            mListMaskHelp.drawMask(canvas);
            canvas.restore();
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }
        Bitmap bitmap = null;
        if (width > 0 && height > 0) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return bitmap;
    }

    private static final class DataIndexMap {

        public Object[] mData;
        public int      mElementIndex = -1;
        public boolean  mNeedRebind;
        public Object [] mOldObjects;//keep original data

        public DataIndexMap(Object[] objects,Object[] oldObjects){
            this(objects,oldObjects,-1);
        }

        public DataIndexMap(Object[] objects, Object[] oldObjects,int position){
            mData = objects;
            mOldObjects=oldObjects;
            mElementIndex = position;
        }

        public void setData(int index, Object object,Object oldObject) {
            if (mData != null && mData.length > index) {
                mData[index] = object;
                mOldObjects[index]=oldObject;
                mNeedRebind = true;
            }
        }
    }

    /**
     * data="address:string,body:string,date:string" in xml . address:string <-----> ColumnInfo : "address" is a Column
     * name for db ,"String" is type for "address" Column . ListScreenElement.java:
     * 
     * @author yljiang@lewatek.com 2014-7-25
     */
    public static final class ColumnInfo {

        public Type                 mType;
        public String               mVarName;

        private static final String DATA_TYPE_BITMAP   = "bitmap";
        private static final String DATA_TYPE_DOUBLE   = "double";
        private static final String DATA_TYPE_FLOAT    = "float";
        private static final String DATA_TYPE_INTEGER  = "int";
        private static final String DATA_TYPE_INTEGER1 = "integer";
        private static final String DATA_TYPE_LONG     = "long";
        private static final String DATA_TYPE_STRING   = "string";

        public ColumnInfo(String data){
            int i = data.indexOf(":");
            if (i == -1) {
                throw new IllegalArgumentException("List: invalid item data " + data);
            }
            mVarName = data.substring(0, i);
            String str = data.substring(i + 1);
            if (DATA_TYPE_STRING.equals(str)) {
                mType = Type.STRING;
                return;
            }
            if (DATA_TYPE_BITMAP.equals(str)) {
                mType = Type.BITMAP;
                return;
            }
            if (DATA_TYPE_INTEGER.equals(str) || DATA_TYPE_INTEGER1.equals(str)) {
                mType = Type.INTEGER;
                return;
            }
            if (DATA_TYPE_DOUBLE.equals(str)) {
                mType = Type.DOUBLE;
                return;
            }
            if (DATA_TYPE_LONG.equals(str)) {
                mType = Type.LONG;
                return;
            }
            if (DATA_TYPE_FLOAT.equals(str)) {
                mType = Type.FLOAT;
                return;
            }
            throw new IllegalArgumentException("List: invalid item data type:" + str);
        }

        public static ArrayList<ColumnInfo> createColumnInfoList(String data) {
            if (TextUtils.isEmpty(data)) {
                return null;
            }
            ArrayList<ColumnInfo> arrayList = new ArrayList<ColumnInfo>();
            String[] strings = data.split(",");
            for(String str:strings){
                arrayList.add(new ColumnInfo(str));
            }
            return arrayList;
        }

        public boolean validate(Object paramObject) {
            switch (mType) {
                case STRING:
                    return paramObject instanceof String;
                case BITMAP:
                    return paramObject instanceof Bitmap;
                case INTEGER:
                    return paramObject instanceof Integer;
                case DOUBLE:
                    return paramObject instanceof Double;
                case LONG:
                    return paramObject instanceof Long;
                case FLOAT:
                    return paramObject instanceof Float;
                default:
                    return false;
            }
        }

        public static enum Type {
            BITMAP, INTEGER, DOUBLE, LONG, FLOAT, STRING
        }
    }

    public static final class Column {

        public ListScreenElement                mList;
        public String                           mName;
        public VariableArrayElement.VarObserver mObserver;
        public ScreenElementRoot                mRoot;
        public String                           mTarget;
        public VariableArrayElement             mTargetElement;

        public Column(Element node, ScreenElementRoot root, ListScreenElement list){
            mRoot = root;
            mList = list;
            if (node != null) {
                load(node);
            }
        }

        private void load(Element node) {
            mName = node.getAttribute("name");
            mTarget = node.getAttribute("target");
            mObserver = new VariableArrayElement.VarObserver() {

                public void onDataChange(Object[] objects) {
                    mList.addColumn(mName, objects,objects);
                }
            };
        }

        public void finish() {
            if (mTargetElement != null) {
                mTargetElement.registerVarObserver(mObserver, false);
            }
        }

        public void init() {
            if (mTargetElement == null) {
                ScreenElement element = mRoot.findElement(mTarget);
                if (element instanceof VariableArrayElement) {
                    mTargetElement = (VariableArrayElement)element;
                }
            } else {
                mTargetElement.registerVarObserver(mObserver, true);
                return;
            }
            Log.e("ListScreenElement", "can't find VarArray:" + this.mTarget);
        }
    }

    public static final class ListData {

        public ArrayList<Column> mColumns = new ArrayList<Column>();
        public ListScreenElement mList;
        public ScreenElementRoot mRoot;

        public ListData(Element node, ScreenElementRoot root, ListScreenElement list){
            mRoot = root;
            mList = list;
            if (node != null) {
                load(node);
            }
        }

        private void load(Element node) {
            Utils.traverseXmlElementChildren(node, "Column", new Utils.XmlTraverseListener() {

                public void onChild(Element element) {
                    mColumns.add(new Column(element, mRoot, mList));
                }
            });
        }

        public void finish() {
            Iterator<Column> iterator = mColumns.iterator();
            while (iterator.hasNext()) {
                Column column = iterator.next();
                if (column != null) {
                    column.finish();
                }
            }
        }

        public void init() {
            Iterator<Column> iterator = mColumns.iterator();
            while (iterator.hasNext()) {
                Column column = iterator.next();
                if (column != null) {
                    column.init();
                }
            }
        }
    }

    private static final class ListItemElement extends ElementGroup {

        private int                   mDataIndex = -1;
        private ElementGroup          mInnerGroup;
        private AnimatedScreenElement mDivider;
        private AnimatedScreenElement mPressBg;
        private AnimatedScreenElement mDelete;

        private final float           mDefalutHeight;

        private static float          mDeleteWidth;

        public int                    mDefaultAlpha;
        public static final String    TAG_NAME   = "Item";

        public ListItemElement(Element node, ScreenElementRoot root){
            super(node, root);
            setClip(true);
            ScreenElement divider = findElement("divider");
            ScreenElement pressBg = findElement("highlight");
            if (divider instanceof AnimatedScreenElement) {
                mDivider = (AnimatedScreenElement)divider;
                removeElement(divider);
                addElement(mDivider);
            }
            if (pressBg instanceof AnimatedScreenElement) {
                mPressBg = (AnimatedScreenElement)pressBg;
                mDefaultAlpha = mPressBg.getAlpha();
                mPressBg.show(false);
            }
            ScreenElement group = findElement("group");
            if (group != null && group instanceof ElementGroup) {
                mInnerGroup = (ElementGroup)group;
            }
            ScreenElement delete = mInnerGroup == null ? findElement("delete") : mInnerGroup.findElement("delete");
            if (delete != null && delete instanceof AnimatedScreenElement) {
                mDelete = (AnimatedScreenElement)delete;
            }
            mAlignV = ScreenElement.AlignV.TOP;
            mDefalutHeight = getHeight();
        }

        public int getDefaultAlpha() {
            return mDefaultAlpha;
        }

        public int getDataIndex() {
            return mDataIndex;
        }

        public void setH(float h) {
            super.setH(h);
        }

        public void reset() {
            showHighlight(false);
            setTranslationX(0);
            setH(mDefalutHeight);
            // super.reset();
        }

        public void show(boolean show) {
            super.show(show);
            if (!show) {
                showHighlight(false);
                setTranslationX(0);
            }
        }

        public void setDataIndex(int index) {
            mDataIndex = index;
            if (mDivider != null) {
                mDivider.show(index >= 0);
            }
        }

        private void showHighlight(boolean show) {
            if (mPressBg != null) {
                mPressBg.setAlpha(0);
                // mPressBg.show(show);
            }
        }

        public AnimatedScreenElement getHighlight() {
            return mPressBg;
        }

        public void setTranslationX(float x) {
            if (mInnerGroup != null) {
                mInnerGroup.setX(x);
            } else {
                setX(x);
            }
        }

        public float getDeleteWidth() {
            if (mDeleteWidth == 0 && mDelete != null) {
                mDeleteWidth = mDelete.getWidth();
            }
            return mDeleteWidth;
        }
    }

    private final HashMap<String, ArrayList<DataWrapper>> mHashMap      = new HashMap<String, ArrayList<DataWrapper>>();
    private final Object                                  object = new Object();
    private final ArrayList<DataWrapper>                  mDatas        = new ArrayList<DataWrapper>();
    private volatile boolean isRemoveItem=false;
    public synchronized boolean getIsRemoveItem(){
        return isRemoveItem;
    }
    public synchronized void setIsRemoveItem(boolean flag){
        this.isRemoveItem=flag;
    }
    public void fillList(ArrayList<DataWrapper> dataList, String key) {
        mListMsgsExecutorService.execute(new AsyncFillList(dataList, key));
    }
    private class AsyncFillList implements Runnable{
        private ArrayList<DataWrapper> dataList;
        private String mKey;
        public AsyncFillList(ArrayList<DataWrapper> datas,String key){
            dataList=datas;
            mKey=key;
        }
        @Override
        public void run() {
            try {
                if(!isTickFinished||isFinished){
                    return;
                }
                if (mInRemove) {
                    cancleInRemove();
                    return;
                }
                synchronized (object) {
                    mDatas.clear();
                    mHashMap.put(mKey, dataList);
                    Iterator<Entry<String, ArrayList<DataWrapper>>> iter = mHashMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        ArrayList<DataWrapper> dataWrappers = iter.next().getValue();
                        if (dataWrappers != null && dataWrappers.size() > 0) {
                            mDatas.addAll(dataWrappers);
                        }
                    }
                    Collections.sort(mDatas, mSort);
                    removeAllItems();
                    for (DataWrapper data : mDatas) {
                        try {
                            addItem(data.mObjects,data.mOldObjects, data.mMixedItemName);
                        } catch (ScreenElementLoadException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
               Log.d(LOG_TAG, "AsyncFillList exception:"+e.getMessage());
            }
        }
    }
    private ArrayList<DeleteExtra> mDeleteExtras;
    public void fillList(ArrayList<Object[]> datas, ArrayList<DeleteExtra> deleteExtras) {
        mListMsgsExecutorService.execute(new AsyncLoadFillListForDeleteExtras(datas, deleteExtras));
    }
    private class AsyncLoadFillListForDeleteExtras implements Runnable{
        private ArrayList<Object[]> datalists ;
        private ArrayList<DeleteExtra> deleteExtrasLists;
        private AsyncLoadFillListForDeleteExtras(ArrayList<Object[]> datas, ArrayList<DeleteExtra> deleteExtras){
            datalists=datas;
            deleteExtrasLists=deleteExtras;
        }
        @Override
        public void run() {
            try {
                if(!isTickFinished||isFinished){
                    return ;
                }
                if (mInRemove) {
                    cancleInRemove();
                    return;
                }
                synchronized (object) {
                    if (mDatas == null) {
                        return;
                    }
                    mDeleteExtras = deleteExtrasLists;
                    removeAllItems();
                    for (Object[] data : datalists) {
                        try {
                            addItem(data);
                        } catch (ScreenElementLoadException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(LOG_TAG, "AsyncLoadFillListForDeleteExtras exception:"+e.getMessage());
            }
        }
    }
    public static final class DataWrapper {

        public Object[]    mObjects;
        public long        mDate;
        public String      mMixedItemName;
        public DeleteExtra mDeleteExtra;
        public Object[] mOldObjects;

        public void setDelete(DeleteExtra delete) {
            mDeleteExtra = delete;
        }

        public DataWrapper(Object[] objects,Object[] oldObjects, long time, String mixedItemName){
            mObjects = objects;
            mOldObjects=oldObjects;
            mDate = time;
            mMixedItemName = mixedItemName;
        }
    }

    public static final class DeleteExtra {

        public String                  mUri;
        public String                  mWhere;
        public HashMap<String, String> mHashMap;

        public DeleteExtra(String uri, String where, HashMap<String, String> hashMap){
            mUri = uri;
            mWhere = where;
            mHashMap = hashMap;
        }
    }

    private final Sort mSort = new Sort();

    private static final class Sort implements Comparator {

        public int compare(Object obj1, Object obj2) {
            if (obj1 instanceof DataWrapper && obj2 instanceof DataWrapper) {
                DataWrapper user1 = (DataWrapper)obj1;
                DataWrapper user2 = (DataWrapper)obj2;
                if (user1.mDate > user2.mDate) {
                    return -1;
                } else if (user1.mDate == user2.mDate) {
                    return 0;
                } else {
                    return 1;
                }
            }
            return 0;
        }
    }

    private static final class LaunchAction implements LoadConfigTask {

        private String                 mEnterResName;
        private String                 mExitResName;
        private CommandTrigger         mTrigger;
        private ScreenElementRoot      mRoot;
        private ArrayList<TaskWrapper> mTaskList;
        private boolean                mConfigTaskLoaded;
        private String[]               mOpenExtras;
        private static final String    TAG = "Click";

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

        public LaunchAction(Element node, ScreenElementRoot root){
            mRoot = root;
            Element ele = Utils.getChild(node, TAG);
            if (ele == null) {
                return;
            }
            String tmp = ele.getAttribute("openExtra");
            if (!TextUtils.isEmpty(tmp)) {
                mOpenExtras = tmp.split(",");
            }
            mTrigger = loadTrigger(ele);
            mTaskList = loadTaskList(ele, root);
            mEnterResName = ele.getAttribute("enterResName");
            mExitResName = ele.getAttribute("exitResName");
            if (mTrigger != null) {
                mTrigger.init();
            }
        }

        private ArrayList<TaskWrapper> loadTaskList(Element ele, ScreenElementRoot root) {
            return ele != null ? TaskWrapper.createTaskWrapperList(ele, root.getVariables()) : null;
        }

        private CommandTrigger loadTrigger(Element ele) {
            return ele != null ? CommandTrigger.fromParentElement(ele, mRoot) : null;
        }

        public Intent perform() {
            if (mTaskList != null && !mTaskList.isEmpty()) {
                return TaskWrapper.performTask(mRoot.getContext().mContext, mTaskList, this);
            }
            if (mTrigger != null) {
                mTrigger.perform();
            }
            return null;
        }
    }

}
