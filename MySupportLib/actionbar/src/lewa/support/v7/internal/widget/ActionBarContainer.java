/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lewa.support.v7.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.internal.VersionUtils;
import lewa.support.v7.view.ActionMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

///LEWA BEGIN
import android.annotation.LewaHook;
import android.graphics.Canvas;
import android.graphics.Color;

import com.lewa.themes.ThemeManager;
import lewa.support.v7.app.ActionBar;
import lewa.support.v7.widget.Toolbar;
///LEWA END
/**
 * This class acts as a container for the action bar view and action mode context views.
 * It applies special styles as needed to help handle animated transitions between them.
 *
 * @hide
 */
public class ActionBarContainer extends FrameLayout {
    private boolean mIsTransitioning;
    private View mTabContainer;
    private View mActionBarView;
    private View mContextView;

    Drawable mBackground;
    Drawable mStackedBackground;
    Drawable mSplitBackground;
    boolean mIsSplit;
    boolean mIsStacked;
    ///LEWA BEGIN
    private int mColorfulMode;

    private boolean mIsActionBarOverlay;
    ///LEWA END
    private int mHeight;

    public ActionBarContainer(Context context) {
        this(context, null);
    }

    public ActionBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Set a transparent background so that we project appropriately.
        final Drawable bg = VersionUtils.isAtLeastL()
                ? new ActionBarBackgroundDrawableV21(this)
                : new ActionBarBackgroundDrawable(this);
        setBackgroundDrawable(bg);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ActionBar);
        mBackground = a.getDrawable(R.styleable.ActionBar_background);
        mStackedBackground = a.getDrawable(
                R.styleable.ActionBar_backgroundStacked);
        mHeight = a.getDimensionPixelSize(R.styleable.ActionBar_height, -1);

        if (getId() == R.id.split_action_bar) {
            mIsSplit = true;
            mSplitBackground = a.getDrawable(R.styleable.ActionBar_backgroundSplit);
        }
        a.recycle();

        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
        // Add by Fan.Yang for color action bar
        initBackground(context);
    }

    private void initBackground(Context context) {
        mStackedBackground = ThemeManager.getInstance(context).getBlurredWallpaper();
        mBackground = mStackedBackground;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mActionBarView = findViewById(R.id.action_bar);
        mContextView = findViewById(R.id.action_context_bar);
        // LEWA BEGIN
        Injector.setActionBarViewBg(this);
        // LEWA END
    }

    public void setPrimaryBackground(Drawable bg) {
        if (mBackground != null) {
            mBackground.setCallback(null);
            unscheduleDrawable(mBackground);
        }
        mBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if (mActionBarView != null) {
                mBackground.setBounds(mActionBarView.getLeft(), mActionBarView.getTop(),
                        mActionBarView.getRight(), mActionBarView.getBottom());
            }
        }
        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
        invalidate();
    }

    public void setStackedBackground(Drawable bg) {
        if (mStackedBackground != null) {
            mStackedBackground.setCallback(null);
            unscheduleDrawable(mStackedBackground);
        }
        mStackedBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if ((mIsStacked && mStackedBackground != null)) {
                mStackedBackground.setBounds(mTabContainer.getLeft(), mTabContainer.getTop(),
                        mTabContainer.getRight(), mTabContainer.getBottom());
            }
        }
        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
        invalidate();
    }

    public void setSplitBackground(Drawable bg) {
        // LEWA ADD BEGIN
        if (true) {
            return;
        }
        // LEWA ADD END

        if (mSplitBackground != null) {
            mSplitBackground.setCallback(null);
            unscheduleDrawable(mSplitBackground);
        }
        mSplitBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if (mIsSplit && mSplitBackground != null) {
                mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }
        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
        invalidate();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        final boolean isVisible = visibility == VISIBLE;
        if (mBackground != null)
            mBackground.setVisible(isVisible, false);
        if (mStackedBackground != null)
            mStackedBackground.setVisible(isVisible, false);
        if (mSplitBackground != null)
            mSplitBackground.setVisible(isVisible, false);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return (who == mBackground && !mIsSplit) || (who == mStackedBackground && mIsStacked) ||
                (who == mSplitBackground && mIsSplit) || super.verifyDrawable(who);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mBackground != null && mBackground.isStateful()) {
            mBackground.setState(getDrawableState());
        }
        if (mStackedBackground != null && mStackedBackground.isStateful()) {
            mStackedBackground.setState(getDrawableState());
        }
        if (mSplitBackground != null && mSplitBackground.isStateful()) {
            mSplitBackground.setState(getDrawableState());
        }
    }

    public void jumpDrawablesToCurrentState() {
        if (Build.VERSION.SDK_INT >= 11) {
            super.jumpDrawablesToCurrentState();
            if (mBackground != null) {
                mBackground.jumpToCurrentState();
            }
            if (mStackedBackground != null) {
                mStackedBackground.jumpToCurrentState();
            }
            if (mSplitBackground != null) {
                mSplitBackground.jumpToCurrentState();
            }
        }
    }

    /**
     * Set the action bar into a "transitioning" state. While transitioning the bar will block focus
     * and touch from all of its descendants. This prevents the user from interacting with the bar
     * while it is animating in or out.
     *
     * @param isTransitioning true if the bar is currently transitioning, false otherwise.
     */
    public void setTransitioning(boolean isTransitioning) {
        mIsTransitioning = isTransitioning;
        setDescendantFocusability(isTransitioning ? FOCUS_BLOCK_DESCENDANTS
                : FOCUS_AFTER_DESCENDANTS);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsTransitioning || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        // An action bar always eats touch events.
        return true;
    }

    public void setTabContainer(ScrollingTabContainerView tabView) {
        if (mTabContainer != null) {
            removeView(mTabContainer);
        }
        mTabContainer = tabView;
        if (tabView != null) {
            addView(tabView);
            final ViewGroup.LayoutParams lp = tabView.getLayoutParams();
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.WRAP_CONTENT;
            tabView.setAllowCollapse(false);
        }
    }

    public View getTabContainer() {
        return mTabContainer;
    }

    ///LEWA BEGIN
    @Override
    public void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        if (mIsSplit) {
            if (mSplitBackground != null)
                mSplitBackground.draw(canvas);
        } else {
            if (mBackground != null) {
                mBackground.draw(canvas);
            }
            if (mStackedBackground != null && mIsStacked) {
                mStackedBackground.draw(canvas);
            }
        }

        Injector.drawMask(this, canvas);
    }

    ///LEWA END
    //@Override
    public ActionMode startActionModeForChild(View child, ActionMode.Callback callback) {
        // No starting an action mode for an action bar child! (Where would it go?)
        return null;
    }

    @Override
    public android.view.ActionMode startActionModeForChild(View originalView,
            android.view.ActionMode.Callback callback) {
        return null;
    }

    private boolean isCollapsed(View view) {
        return view == null || view.getVisibility() == GONE || view.getMeasuredHeight() == 0;
    }

    private int getMeasuredHeightWithMargins(View view) {
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        return view.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
    }

///LEWA ADD BEGIN
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mActionBarView == null) return;

        final LayoutParams lp = (LayoutParams) mActionBarView.getLayoutParams();
        final int actionBarViewHeight = isCollapsed(mActionBarView) ? 0 :
                mActionBarView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

        if (mTabContainer != null && mTabContainer.getVisibility() != GONE) {
        
            final int mode = MeasureSpec.getMode(heightMeasureSpec);
            if (mode == MeasureSpec.AT_MOST) {
                final int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(getMeasuredWidth(),
                        Math.min(actionBarViewHeight + mTabContainer.getMeasuredHeight(),
                                maxHeight));
            }
        } 
    }
 /**   
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mActionBarView == null &&
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST && mHeight >= 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    Math.min(mHeight, MeasureSpec.getSize(heightMeasureSpec)), MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mActionBarView == null) return;

        final int mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mTabContainer != null && mTabContainer.getVisibility() != GONE
                && mode != MeasureSpec.EXACTLY) {
            final int topMarginForTabs;
            if (!isCollapsed(mActionBarView)) {
                topMarginForTabs = getMeasuredHeightWithMargins(mActionBarView);
            } else if (!isCollapsed(mContextView)) {
                topMarginForTabs = getMeasuredHeightWithMargins(mContextView);
            } else {
                topMarginForTabs = 0;
            }
            final int maxHeight = mode == MeasureSpec.AT_MOST ?
                    MeasureSpec.getSize(heightMeasureSpec) : Integer.MAX_VALUE;
            setMeasuredDimension(getMeasuredWidth(),
                    Math.min(topMarginForTabs + getMeasuredHeightWithMargins(mTabContainer),
                            maxHeight));
        }
    }
//    
///LEWA ADD END
 * 
 */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        final View tabContainer = mTabContainer;
        final boolean hasTabs = tabContainer != null && tabContainer.getVisibility() != GONE;

        boolean needsInvalidate = false;
        if (tabContainer != null && tabContainer.getVisibility() != GONE) {
            final int containerHeight = getMeasuredHeight();
            final LayoutParams lp = (LayoutParams) tabContainer.getLayoutParams();
            final int tabHeight = tabContainer.getMeasuredHeight();
            ///LEWA ADD BEGIN
            if ((((Toolbar)mActionBarView).getWrapper().getDisplayOptions() & ActionBar.DISPLAY_SHOW_HOME) == 0) {
                // Not showing home, put tabs on top.
           
                final int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);

                    if (child == tabContainer) continue;

                    if (!isCollapsed(mActionBarView)) {
                        child.offsetTopAndBottom(tabHeight);
                    }
                }
                tabContainer.layout(l, 0, r, tabHeight);
            } else {
            	tabContainer.layout(l, containerHeight - tabHeight, r, containerHeight);
            }
            //tabContainer.layout(l, containerHeight - tabHeight - lp.bottomMargin, r,
            //        containerHeight - lp.bottomMargin);
            ///LEWA ADD END
        } 
       
        if (mIsSplit) {
        
            if (mSplitBackground != null) {
                mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                needsInvalidate = true;
            }
        } else {
            if (mBackground != null) {
                if (mActionBarView != null && mActionBarView.getVisibility() == View.VISIBLE) {
                    mBackground.setBounds(mActionBarView.getLeft(), mActionBarView.getTop(),
                            mActionBarView.getRight(), mActionBarView.getBottom());
                } else if (mContextView != null &&
                        mContextView.getVisibility() == View.VISIBLE) {
                    mBackground.setBounds(mContextView.getLeft(), mContextView.getTop(),
                            mContextView.getRight(), mContextView.getBottom());
                } else {
                    mBackground.setBounds(0, 0, 0, 0);
                }
                needsInvalidate = true;
            }
            mIsStacked = hasTabs;
            if (hasTabs && mStackedBackground != null) {
                mStackedBackground.setBounds(tabContainer.getLeft(), tabContainer.getTop(),
                        tabContainer.getRight(), tabContainer.getBottom());
                needsInvalidate = true;
            }
        }

        if (needsInvalidate) {
            invalidate();
        }
    }

// LEWA ADD BEGIN

    /**
     * @hide
     */
    public int getSplitHeight() {
        return getHeight();
    }
// LEWA ADD END

    ///LEWA BEGIN
    static class Injector {
        static void initBgResources(Context context, AttributeSet attrs, ActionBarContainer abc) {
            //try {
            //	abc.mColorfulMode = System.getInt(context.getContentResolver(), Settings.LEWA_COLORVIEW_MODE);
            //	android.content.res.TypedArray a = context.obtainStyledAttributes(attrs,
            //       com.lewa.internal.R.styleable.EnableColorfulView);
            //	abc.mIsEnableColorful = a.getBoolean(
            //		com.lewa.internal.R.styleable.EnableColorfulView_enable_colorful_actionbar_background, true);
            //} catch (Exception e) {
            //	e.printStackTrace();
            //}

            abc.mIsActionBarOverlay = true;//LewaUiUtil.isActionBarOverlay(context);

            //if (!abc.mIsLewaUi || abc.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_INVALID || !abc.mIsEnableColorful || abc.mIsActionBarOverlay) {
            //	return;
            //}

            //if (abc.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_WALLPAPER) {
            //	getWallPaperBg(context, abc);
            //} else if (abc.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_GAUSSIAN) {
            //	getGaussianBg(context, abc);
            //} else if (abc.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_NONE) {
            getDefaultBg(context, abc);
            //}
            //abc.mIsActionBarOverlay = true;
        }

        static void getDefaultBg(Context context, ActionBarContainer abc) {
            abc.mBackground = context.getResources().getDrawable(R.drawable.actionbar_tittle_bg);
            abc.mStackedBackground = context.getResources().getDrawable(
                    R.drawable.actionbar_tittle_bg);
        }

        static float dpToPxCoord(Context context, float oriSize) {
            float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
            if (scaledDensity == 0) {
                return oriSize;
            }

            return oriSize / scaledDensity;
        }

        static void drawMask(ActionBarContainer abc, Canvas canvas) {
            android.graphics.Paint p = new android.graphics.Paint();
            p.setColor(0x33000000);
            canvas.drawRect(0, 0, abc.getWidth(), abc.getHeight(), p);
        }

        static void setActionBarViewBg(ActionBarContainer abc) {
            if (abc.mIsActionBarOverlay && abc.mActionBarView != null) {
                abc.mActionBarView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    ///LEWA END
}
