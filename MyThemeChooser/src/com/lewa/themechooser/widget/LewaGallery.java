package com.lewa.themechooser.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class LewaGallery extends HorizontalScrollView {
    private ListAdapter mAdapter;

    private int mCenterViewPosition = -1;
    private int mLastX;

    private boolean mForceNoSmoothScroll = false;
    private int mScrollCount = 0;

    private OnItemSelectedListener mOnItemSelectedListner;

    public LewaGallery(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setHorizontalFadingEdgeEnabled(true);
        this.setHorizontalScrollBarEnabled(false);
        this.setFadingEdgeLength(5);
        this.setSmoothScrollingEnabled(true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getChildCount() == 0) return;

        initCenterView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getChildCount() == 0) return;

        ViewGroup parent = (ViewGroup) getChildAt(0);

        if (parent.getChildCount() == 0) return;

        View FirstChild = parent.getChildAt(0);

        int LeftPadding = (getWidth() / 2) - (FirstChild.getMeasuredWidth() / 2);

        View LastChild = parent.getChildAt(getChildCount() - 1);

        int RightPadding = (getWidth() / 2) - (LastChild.getMeasuredWidth() / 2);

        if (parent.getPaddingLeft() != LeftPadding && parent.getPaddingRight() != RightPadding) {
            parent.setPadding(LeftPadding
                    , parent.getPaddingTop(), RightPadding, parent.getPaddingBottom());
            requestLayout();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (getChildCount() == 0) {
            return false;
        }
        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mLastX = (int) ev.getRawX();
                break;
            }
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (ev.getRawX() - mLastX > 30f) {
                    if (mCenterViewPosition > 0) mCenterViewPosition--;
                } else if (ev.getRawX() - mLastX < -30f) {
                    if (mCenterViewPosition < ((ViewGroup) getChildAt(0)).getChildCount() - 1)
                        mCenterViewPosition++;
                }
                scrollToSelectedIndex();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
        }
        return true;
    }

    private int getInternalCenterView() {
        if (getChildCount() == 0) return -1;

        int CenterView = 0;
        int CenterX = getScrollX() + (getWidth() / 2);

        ViewGroup parent = (ViewGroup) getChildAt(0);

        if (parent.getChildCount() == 0) return -1;

        View child = parent.getChildAt(0);

        while (child != null && child.getRight() <= CenterX && CenterView < parent.getChildCount()) {
            CenterView++;
            child = parent.getChildAt(CenterView);
        }

        if (CenterView >= parent.getChildCount()) CenterView = parent.getChildCount() - 1;

        return CenterView;
    }

    private int getCenterPositionFromView() {
        int CenterView = getInternalCenterView();

        if (mCenterViewPosition != CenterView) {
            if (null != mOnItemSelectedListner) {
                mOnItemSelectedListner.onItemSelected(this, CenterView);
            }
        }

        mCenterViewPosition = CenterView;

        return mCenterViewPosition;
    }

    public int getCenterViewPosition() {
        return mCenterViewPosition;
    }

    public ListAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(ListAdapter mAdapter) {
        this.mAdapter = mAdapter;
        fillViewWithAdapter();
    }

    private void fillViewWithAdapter() {
        if (getChildCount() == 0 || mAdapter == null)
            return;

        ViewGroup parent = (ViewGroup) getChildAt(0);

        parent.removeAllViews();

        for (int i = 0; i < mAdapter.getCount(); i++) {
            parent.addView(mAdapter.getView(i, null, parent));
        }
        scrollTo(0, 0);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        getCenterPositionFromView();

        initCenterView();
    }

    private void initCenterView() {
        if (getChildCount() == 0) return;

        ViewGroup parent = (ViewGroup) getChildAt(0);

        if (parent.getChildCount() == 0) return;

        int CenterView = getCenterViewPosition();

        if (CenterView == -1) {
            mCenterViewPosition = 0;
            CenterView = 0;
        }

        if (CenterView != -1 && CenterView
                != getInternalCenterView() && parent.getChildAt(0).getLeft() >= 0) {
            scrollToSelectedIndex();
        }

        if (CenterView < 0 || CenterView > parent.getChildCount()) return;

        for (int i = 0; i <= parent.getChildCount(); i++) {
            if (!(parent.getChildAt(i) instanceof TextView)) continue;
            if (i == CenterView) {
                // Start Animation
            } else {
                // Remove Animation for other Views
            }
        }
    }

    public int getChildWidth() {
        return getChildAt(0).getMeasuredWidth();
    }

    public int getSelection() {
        return getCenterViewPosition();
    }

    public void setSelection(int index) {
        if (/* mCenterViewPosition == index || */ getChildCount() == 0) return;

        ViewGroup parent = (ViewGroup) getChildAt(0);

        if (index < 0 || index > parent.getChildCount()) {
            throw new ArrayIndexOutOfBoundsException(index);
            // return;
        }

        mCenterViewPosition = index;

        if (null != mOnItemSelectedListner) {
            mOnItemSelectedListner.onItemSelected(this, mCenterViewPosition);
        }

        mForceNoSmoothScroll = true;
        mScrollCount = 0;

        // requestLayout();
        scrollToSelectedIndex();
    }

    protected void scrollToSelectedIndex() {
        ViewGroup parent = (ViewGroup) getChildAt(0);

        View child = parent.getChildAt(mCenterViewPosition);
        int childCenterX = child.getLeft() + (child.getMeasuredWidth() / 2);

        int screenCenterX = getWidth() / 2;

        int childScrollToX = childCenterX - screenCenterX;

        if (isSmoothScrollingEnabled()
                && (1 != mCenterViewPosition || android.os.Build.VERSION.SDK_INT < 17
                || !mForceNoSmoothScroll)) {
            smoothScrollTo(childScrollToX, 0);
        } else {
            scrollTo(childScrollToX, 0);
            if (++mScrollCount > 1) {
                mForceNoSmoothScroll = false;
            }
        }
        //#64580 deleted by bin dong  
        /*if (null != mOnItemSelectedListner) {
            mOnItemSelectedListner.onItemSelected(this, mCenterViewPosition);
        }*/
    }

    public OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListner;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listner) {
        if (listner != mOnItemSelectedListner) {
            mOnItemSelectedListner = listner;
        }
    }

    public interface OnItemSelectedListener {
        public void onItemSelected(View view, int newPosition);
    }
}
