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
import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBarImplBase.ActionModeImpl;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.view.ActionMode;
import lewa.support.v7.internal.view.menu.ActionMenuPresenter;
import lewa.support.v7.internal.view.menu.ActionMenuView;
import lewa.support.v7.internal.view.menu.MenuBuilder;
///LEWA BEGIN
import lewa.support.v7.internal.view.ActionBarPolicy;
import lewa.support.v7.internal.view.StandaloneActionMode;
import lewa.support.v7.internal.view.menu.ActionMenuItem;
import lewa.support.v7.internal.view.menu.ListMenuPresenter;
import lewa.support.v7.internal.view.menu.MenuItemImpl;
import lewa.support.v7.internal.view.menu.MenuPresenter;
import lewa.support.v7.internal.view.menu.MenuView;
import lewa.support.v7.lewa.v5.LewaActionBarContainer;
import lewa.support.v7.lewa.v5.LewaActionMenuPresenter;

///LEWA END
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


///LEWA BEGIN
import android.content.res.Configuration;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.view.Menu;
//import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.util.Log;
//import lewa.util.LewaUiUtil;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.app.WallpaperManager;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;



//import android.content.res.LewaColorfulResources.LewaColorfulStyle;
//import android.content.res.LewaResources;
import com.lewa.themes.ThemeManager;

import android.widget.FrameLayout;
import android.view.Gravity;
//import lewa.internal.v5.widget.LewaActionBarContainer;
//import lewa.internal.v5.view.menu.LewaActionMenuPresenter;

//import lewa.graphics.BitmapFilter;
//import lewa.graphics.drawable.BitmapBlurDrawable;
//import lewa.graphics.drawable.BlurOptions;
//import lewa.graphics.BitmapFilterFactory;

//import com.android.internal.view.ActionBarPolicy;

import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Message;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;

import android.provider.Settings;
///LEWA END
/**
 * @hide
 */
public class ActionBarContextView extends AbsActionBarView implements AnimatorListener {
    private static final String TAG = "ActionBarContextView";

    private CharSequence mTitle;
///LEWA BEGIN
    // private CharSequence mSubtitle;
///LEWA END

    private View mClose;
    private View mCustomView;
    private LinearLayout mTitleLayout;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private int mTitleStyleRes;
    private int mSubtitleStyleRes;
    private Drawable mSplitBackground;
    private boolean mTitleOptional;

    private Animator mCurrentAnimation;
    private boolean mAnimateInOnLayout;
    private int mAnimationMode;

    private static final int ANIMATE_IDLE = 0;
    private static final int ANIMATE_IN = 1;
    private static final int ANIMATE_OUT = 2;
    // LEWA ADD BEGIN
    //@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
    private int mColorfulMode;

    //@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
    private boolean mIsLewaUi;

    //@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
    protected int mStatusbarHeight;

    //@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
    private InjectorTouchListener mInjectorTouchListener;

    //@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
    private PanelFeatureState mOptionalMenuState;
    // LEWA ADD END
    public ActionBarContextView(Context context) {
        this(context, null);
    }

    public ActionBarContextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.actionModeStyle);
    }

    public ActionBarContextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionMode, defStyle, 0);
///LEWA BEGIN
        Injector.initilize(context, this, a);
        //setBackgroundDrawable(a.getDrawable(
        //        R.styleable.ActionMode_background));
        Injector.setActionModeBackground(this, context);
    	mBackground = ThemeManager.getInstance(context).getBlurredWallpaper();
//    	mBackground = WallpaperManager.getInstance(context).getDrawable();
//    	if (mBackground != null) {
//    		 mBackground = mBackground.getConstantState().newDrawable();
//    	    	this.setBackground(mBackground);
//    	}
       
///LEWA END
        mTitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_titleTextStyle, 0);
        mSubtitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_subtitleTextStyle, 0);

        mContentHeight = a.getLayoutDimension(
                R.styleable.ActionMode_height, 0);

        // mContentHeight = 50;
        mSplitBackground = a.getDrawable(
                R.styleable.ActionMode_backgroundSplit);

///LEWA BEGIN
        Injector.resetContextViewHeight(this);
///LEWA END
        a.recycle();
    }
    private Drawable mBackground;
//    @Override
//    public void setVisibility(int visibility) {
//        super.setVisibility(visibility);
//        final boolean isVisible = visibility == VISIBLE;
//        if (mBackground != null) mBackground.setVisible(isVisible, false);
//        
//    }
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.hideOverflowMenu();
            mActionMenuPresenter.hideSubMenus();
        }
    }

//    @Override
//    protected void drawableStateChanged() {
//        super.drawableStateChanged();
//        if (mBackground != null && mBackground.isStateful()) {
//            mBackground.setState(getDrawableState());
//        }
// 
//    }
///LEWA BEGIN

    //@LewaHook(LewaHook.LewaHookType.NEW_METHOD)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        if (mBackground != null) {
//            mBackground.draw(canvas);
//        }
        Injector.onDraw(this, canvas);
    }
///LEWA END

    @Override
    public void setSplitActionBar(boolean split) {
        if (mSplitActionBar != split) {
            if (mActionMenuPresenter != null) {
                // Mode is already active; move everything over and adjust the menu itself.
                final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.FILL_PARENT);
                if (!split) {
                    mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
                    mMenuView.setBackgroundDrawable(null);
                    final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
                    if (oldParent != null) {
                        oldParent.removeView(mMenuView);
                    }
                    addView(mMenuView, layoutParams);
                } else {
                    // Allow full screen width in split mode.
                    mActionMenuPresenter.setWidthLimit(
                            getContext().getResources().getDisplayMetrics().widthPixels, true);
                    // No limit to the item count; use whatever will fit.
                    mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
                    // Span the whole width
                    layoutParams.width = ViewGroup.LayoutParams.FILL_PARENT;
                    layoutParams.height = mContentHeight;
///LEWA BEGIN
                    Injector.resetSplitActionBarHeight(getContext(), layoutParams);
///LEWA END

                    mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
                    mMenuView.setBackgroundDrawable(mSplitBackground);
                    final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
                    if (oldParent != null) {
                        oldParent.removeView(mMenuView);
                    }
                    mSplitView.addView(mMenuView, layoutParams);
                }
            }
            super.setSplitActionBar(split);
        }
    }

    public void setContentHeight(int height) {
        mContentHeight = height;
    }

    public void setCustomView(View view) {
        if (mCustomView != null) {
            removeView(mCustomView);
        }
        mCustomView = view;
        if (mTitleLayout != null) {
            removeView(mTitleLayout);
            mTitleLayout = null;
        }
        if (view != null) {
///LEWA BEGIN
			Injector.setViewPadding(this, view);
///LEWA END
            addView(view);
        }
        requestLayout();
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        initTitle();
    }

    public void setSubtitle(CharSequence subtitle) {
///LEWA BEGIN
        // mSubtitle = subtitle;
///LEWA END
        initTitle();
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public CharSequence getSubtitle() {
///LEWA BEGIN
        // return mSubtitle;
        return null;
///LEWA END
    }
	
///LEWA BEGIN
    public static int dip2px(Context context, float dpValue) { 
        final float scale = context.getResources().getDisplayMetrics().density; 
        return (int) (dpValue * scale + 0.5f); 
     }
    private Button mSpinner;
    private ActionMode mActionMode;
    private void initTitle() {
 if(!lewaInitTitle()) {
        if (mTitleLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
			inflater.inflate(R.layout.action_bar_title_item_with_spinner, this);
            //inflater.inflate(R.layout.abc_action_bar_title_item, this);
            mTitleLayout = (LinearLayout) getChildAt(getChildCount() - 1);
                // mTitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_title);
                // mSubtitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_subtitle);
                // if (mTitleStyleRes != 0) {
                    // mTitleView.setTextAppearance(mContext, mTitleStyleRes);
                // }
                // if (mSubtitleStyleRes != 0) {
                    // mSubtitleView.setTextAppearance(mContext, mSubtitleStyleRes);
                // }

                mSpinner = (Button) mTitleLayout.findViewById(R.id.action_bar_spinner);
                mSpinner.setText("One item selected");

                final PopupMenu popMenu = new PopupMenu(mContext, mSpinner);
                final Menu menu = popMenu.getMenu();
                menu.add(Menu.NONE, android.R.id.selectAll, Menu.NONE, android.R.string.selectAll);
                popMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        if (null != mActionMode) {
                            if (mActionMode instanceof ActionModeImpl) {
                                return ((ActionModeImpl) mActionMode)
                                        .onMenuItemSelected((MenuBuilder)menu, item);
                            }
                            if (mActionMode instanceof StandaloneActionMode) {
                                return ((StandaloneActionMode) mActionMode)
                                        .onMenuItemSelected((MenuBuilder)menu, item);
                            }
                            // Woody Guo @ 2012/12/18: State of the selection menu item
                            // is totally controlled by the calling client
                            /*
                             * mActionMode.setSelectionMode(
                             *         ActionMode.SELECT_ALL == mActionMode.getSelectionMode()
                             *         ? ActionMode.SELECT_NONE : ActionMode.SELECT_ALL);
                             * item.setTitle(ActionMode.SELECT_ALL == mActionMode.getSelectionMode()
                             *         ? android.R.string.selectAll : R.string.selectNone);
                             * return true;
                             */
                        }
                        return false;
                    }
                });

                mSpinner.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        popMenu.getMenu().getItem(0).setTitle(
                                ActionMode.SELECT_ALL == mActionMode.getSelectionMode()
                                ? android.R.string.selectAll : R.string.selectNone);
                        popMenu.show();
                    }
                });
            }

            mSpinner.setText(mTitle);
        }
        if (true) {
			if (mTitleView != null) {
            	mTitleView.setText(mTitle);
			}
            //mSubtitleView.setText(mSubtitle);
        }
        final boolean hasTitle = !TextUtils.isEmpty(mTitle);
        //final boolean hasSubtitle = !TextUtils.isEmpty(mSubtitle);
        //mSubtitleView.setVisibility(hasSubtitle ? VISIBLE : GONE);
        //mTitleLayout.setVisibility(hasTitle || hasSubtitle ? VISIBLE : GONE);
        mTitleLayout.setVisibility(hasTitle ? VISIBLE : GONE);
        if (mTitleLayout.getParent() == null) {
            addView(mTitleLayout);
        }
    }
///LEWA END

    public void initForMode(final ActionMode mode) {
///LEWA BEGIN
        mActionMode = mode;
///LEWA END

        if (mClose == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
///LEWA  BEGIN
            int resId = Injector.getLeftButtonLayoutId(mContext);
			mClose = inflater.inflate(resId, this, false);
///LEWA  END
            addView(mClose);
        } else if (mClose.getParent() == null) {
            addView(mClose);
        }

        View closeButton = mClose.findViewById(R.id.action_mode_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mode.finish();
            }
        });

///LEWA BEGIN
        Injector.setViewClickStyle(getContext(), closeButton);
        Injector.setViewPadding(this, closeButton);
///LEWA END

        final MenuBuilder menu = (MenuBuilder) mode.getMenu();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.dismissPopupMenus();
        }
        
///LEWA BEGIN
        //mActionMenuPresenter = new ActionMenuPresenter(getContext());
        mActionMenuPresenter = Injector.newMenuPresenter(getContext(), mSplitActionBar);
        // Set action mode for change style of action mode.
        Injector.setActionMode(this, true);
///LEWA END
        mActionMenuPresenter.setReserveOverflow(true);

        final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT);
        if (!mSplitActionBar) {
            menu.addMenuPresenter(mActionMenuPresenter);
            mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            mMenuView.setBackgroundDrawable(null);
            addView(mMenuView, layoutParams);
        } else {
            // Allow full screen width in split mode.
            mActionMenuPresenter.setWidthLimit(
                    getContext().getResources().getDisplayMetrics().widthPixels, true);
            // No limit to the item count; use whatever will fit.
            mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
            // Span the whole width
            layoutParams.width = ViewGroup.LayoutParams.FILL_PARENT;
            layoutParams.height = mContentHeight;
///LEWA BEGIN
            Injector.resetSplitActionBarHeight(getContext(), layoutParams);
///LEWA END
            menu.addMenuPresenter(mActionMenuPresenter);
            mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            mMenuView.setBackgroundDrawable(mSplitBackground);
            // LEWA ADD START
            if (true) {
                // Add menu bar into action mode.
                Injector.addActionModeOptionMenu(this, menu, layoutParams);
            } else {
                mSplitView.addView(mMenuView, layoutParams);
            }
            // LEWA ADD END
        }

        mAnimateInOnLayout = true;
    }

    public void closeMode() {
        if (mAnimationMode == ANIMATE_OUT) {
            // Called again during close; just finish what we were doing.
            return;
        }

        // LEWA ADD START
        if (true) {
            Injector.clearActionModeOptionMenu(this);

            // Remove menu bar when exit action mode.
            if (mSplitView instanceof LewaActionBarContainer) {
                ((LewaActionBarContainer) mSplitView).getActionModeMenuBar().removeAllViews();
            }
        }
        // LEWA ADD END
        if (mClose == null) {
            killMode();
            return;
        }

        finishAnimation();
        mAnimationMode = ANIMATE_OUT;
        mCurrentAnimation = makeOutAnimation();
        mCurrentAnimation.start();

        // LEWA ADD BEGIN
        // Reset action mode for change style of normal mode.
        Injector.setActionMode(this, false);
        // LEWA ADD END
    }

    private void finishAnimation() {
        final Animator a = mCurrentAnimation;
        if (a != null) {
            mCurrentAnimation = null;
            a.end();
        }
    }

    public void killMode() {
        finishAnimation();
        removeAllViews();
        if (mSplitView != null) {
            mSplitView.removeView(mMenuView);
        }
        mCustomView = null;
        mMenuView = null;
        mAnimateInOnLayout = false;
///LEWA BEGIN
        mActionMode = null;
///LEWA END
    }

    @Override
    public boolean showOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean hideOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean isOverflowMenuShowing() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        // Used by custom views if they don't supply layout params. Everything else
        // added to an ActionBarContextView should have them already.
        return new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != View.MeasureSpec.EXACTLY) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_width=\"FILL_PARENT\" (or fill_parent)");
        }

        final int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == View.MeasureSpec.UNSPECIFIED) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }

        final int contentWidth = View.MeasureSpec.getSize(widthMeasureSpec);

        int maxHeight = mContentHeight > 0 ?
                mContentHeight : View.MeasureSpec.getSize(heightMeasureSpec);

        final int verticalPadding = getPaddingTop() + getPaddingBottom();
        int availableWidth = contentWidth - getPaddingLeft() - getPaddingRight();
        final int height = maxHeight - verticalPadding;
        final int childSpecHeight = View.MeasureSpec
                .makeMeasureSpec(height, View.MeasureSpec.AT_MOST);

        if (mClose != null) {
          
            availableWidth = measureChildView(mClose, availableWidth, childSpecHeight, 0);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mClose
                    .getLayoutParams();
            availableWidth -= lp.leftMargin + lp.rightMargin;
        }

        if (mMenuView != null && mMenuView.getParent() == this) {
            availableWidth = measureChildView(mMenuView, availableWidth,
                    childSpecHeight, 0);
        }

        if (mTitleLayout != null && mCustomView == null) {
            if (mTitleOptional) {
                final int titleWidthSpec = View.MeasureSpec
                        .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mTitleLayout.measure(titleWidthSpec, childSpecHeight);
                final int titleWidth = mTitleLayout.getMeasuredWidth();
                final boolean titleFits = titleWidth <= availableWidth;
                if (titleFits) {
                    availableWidth -= titleWidth;
                }
///LEWA BEGIN
                final boolean hasTitle = !TextUtils.isEmpty(mTitle);
                mTitleLayout.setVisibility((hasTitle && titleFits) ? VISIBLE : GONE);
///LEWA END
            } else {
///LEWA BEGIN
                if (true) {
                    availableWidth = Injector.measureTitleView(mTitleLayout, availableWidth, childSpecHeight, 0);
                } else {
                    availableWidth = measureChildView(mTitleLayout, availableWidth, childSpecHeight, 0);
                }
            }
///LEWA END
        }

        if (mCustomView != null) {
            ViewGroup.LayoutParams lp = mCustomView.getLayoutParams();
            final int customWidthMode = lp.width != ViewGroup.LayoutParams.WRAP_CONTENT ?
                    View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST;
            final int customWidth = lp.width >= 0 ?
                    Math.min(lp.width, availableWidth) : availableWidth;
            final int customHeightMode = lp.height != ViewGroup.LayoutParams.WRAP_CONTENT ?
                    View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST;
            final int customHeight = lp.height >= 0 ?
                    Math.min(lp.height, height) : height;
            mCustomView.measure(View.MeasureSpec.makeMeasureSpec(customWidth, customWidthMode),
                    View.MeasureSpec.makeMeasureSpec(customHeight, customHeightMode));
        }

        if (mContentHeight <= 0) {
            int measuredHeight = 0;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View v = getChildAt(i);
                int paddedViewHeight = v.getMeasuredHeight() + verticalPadding;
                if (paddedViewHeight > measuredHeight) {
                    measuredHeight = paddedViewHeight;
                }
            }
            setMeasuredDimension(contentWidth, measuredHeight);
        } else {
            setMeasuredDimension(contentWidth, maxHeight);
        }
    }

    private Animator makeInAnimation() {
        mClose.setTranslationX(-mClose.getWidth() -
                ((MarginLayoutParams) mClose.getLayoutParams()).leftMargin);
        ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(mClose, "translationX", 0);
        buttonAnimator.setDuration(200);
        buttonAnimator.addListener(this);
        buttonAnimator.setInterpolator(new DecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        AnimatorSet.Builder b = set.play(buttonAnimator);

        if (mMenuView != null) {
            final int count = mMenuView.getChildCount();
            if (count > 0) {
                for (int i = count - 1, j = 0; i >= 0; i--, j++) {
                    View child = mMenuView.getChildAt(i);
                    child.setScaleY(0);
                    ObjectAnimator a = ObjectAnimator.ofFloat(child, "scaleY", 0, 1);
                    a.setDuration(300);
                    b.with(a);
                }
            }
        }

///LEWA  BEGIN
        ObjectAnimator rightBbuttonAnimator = makeRightButtonInAnimation();
        if (rightBbuttonAnimator != null) {
            b.with(rightBbuttonAnimator);
        }
///LEWA  END
        return set;
    }

    private Animator makeOutAnimation() {
        ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(mClose, "translationX",
                -mClose.getWidth() - ((MarginLayoutParams) mClose.getLayoutParams()).leftMargin);
        buttonAnimator.setDuration(200);
        buttonAnimator.addListener(this);
        buttonAnimator.setInterpolator(new DecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        AnimatorSet.Builder b = set.play(buttonAnimator);

        if (mMenuView != null) {
            final int count = mMenuView.getChildCount();
            if (count > 0) {
                for (int i = 0; i < 0; i++) {
                    View child = mMenuView.getChildAt(i);
                    child.setScaleY(0);
                    ObjectAnimator a = ObjectAnimator.ofFloat(child, "scaleY", 0);
                    a.setDuration(300);
                    b.with(a);
                }
            }
        }
///LEWA BEGIN
        ObjectAnimator rightBbuttonAnimator = makeRightButtonOutAnimation();
        if (rightBbuttonAnimator != null) {
            b.with(rightBbuttonAnimator);
        }
///LEWA END

        return set;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();

        if (mClose != null && mClose.getVisibility() != GONE) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mClose
                    .getLayoutParams();
            x += lp.leftMargin;
           
            x += positionChild(mClose, x, y, contentHeight);
            x += lp.rightMargin;

            if (mAnimateInOnLayout) {
                mAnimationMode = ANIMATE_IN;
                mCurrentAnimation = makeInAnimation();
                mCurrentAnimation.start();
                mAnimateInOnLayout = false;
            }
        }

        if (mTitleLayout != null && mCustomView == null && mTitleLayout.getVisibility() != GONE) {
            x += positionChild(mTitleLayout, x, y, contentHeight);
        }

        if (mCustomView != null) {
            x += positionChild(mCustomView, x, y, contentHeight);
        }

        x = r - l - getPaddingRight();
//        if (mBackground != null) {
//            mBackground.setBounds(this.getLeft(), this.getTop(),
//                    this.getRight(), this.getBottom());
////            needsInvalidate = true;
////            this.setBackground(mBackground);
//            invalidate();
//        }

//        if (mMenuView != null) {
/////LEWA  BEGIN
//            x += positionChild(mMenuView, x, y, Injector.getContextMenuViewHeight(getContext(), mSplitActionBar, contentHeight));
/////LEWA  END
//        }
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (mAnimationMode == ANIMATE_OUT) {
            killMode();
        }
        mAnimationMode = ANIMATE_IDLE;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Action mode started
            event.setSource(this);
            event.setClassName(getClass().getName());
            event.setPackageName(getContext().getPackageName());
            event.setContentDescription(mTitle);
        } else {
            super.onInitializeAccessibilityEvent(event);
        }
    }

    public void setTitleOptional(boolean titleOptional) {
        if (titleOptional != mTitleOptional) {
            requestLayout();
        }
        mTitleOptional = titleOptional;
    }

    public boolean isTitleOptional() {
        return mTitleOptional;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        // M: consume touch event to prevent from dispatching it to parent or siblings
        return true;
    }

///LEWA  BEGIN
    public void setTitleLayout(LinearLayout titleLayout)
    {
        mTitleLayout = titleLayout;
    }

    public LinearLayout getTitleLayout()
    {
        return mTitleLayout;
    }

    public TextView getTitleView() {
        return mTitleView;
    }

    public void setTitleView(TextView titleView) {
        mTitleView = titleView;
    }

    public TextView getSubTitleView() {
        return mSubtitleView;
    }

    public void setSubTitleView(TextView subtitleView) {
        mSubtitleView = subtitleView;
    }

    public int getTitleStyleRes()
    {
        return mTitleStyleRes;
    }

    public int getSubtitleStyleRes()
    {
        return mSubtitleStyleRes;
    }

    /** @hide */
    protected boolean lewaInitTitle()
    {
        return false;
    }

    /** @hide */
    public void setRightActionButtonDrawable(Drawable drawable) {

    }

    /** @hide */
    public void setRightActionButtonVisibility(int visibility) {
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    /** @hide */
    protected ObjectAnimator makeRightButtonInAnimation() {
        return null;
    }

    /** @hide */
    protected ObjectAnimator makeRightButtonOutAnimation() {
        return null;
    }
///LEWA ADD END

    ///LEWA ADD START
    private final class PanelFeatureState {
        int background;

        /** The background when the panel spans the entire available width. */
        int fullBackground;

        int windowAnimations;

        ViewGroup parentView;

        /** The panel that we are actually showing. */
        View shownPanelView;

        /** Use {@link #setMenu} to set this. */
        MenuBuilder menu;

        ListMenuPresenter listMenuPresenter;

        /** Theme resource ID for list elements of the panel menu */
        int listPresenterTheme;

        FrameLayout decorView;

        boolean isOpen;

        public boolean qwertyMode;

        void setStyle(Context context) {
            TypedArray a = context.obtainStyledAttributes(R.styleable.Theme);
            background = a.getResourceId(
                    R.styleable.Theme_panelBackground, 0);
            fullBackground = a.getResourceId(
                    R.styleable.Theme_panelFullBackground, 0);
            //windowAnimations = a.getResourceId(
            //        R.styleable.Theme_android_windowAnimationStyle, 0);
            listPresenterTheme = a.getResourceId(
                    R.styleable.Theme_panelMenuListTheme,
                    R.style.Theme_AppCompat_CompactMenu);
            a.recycle();
        }

        void setMenu(MenuBuilder menu) {
            if (menu == this.menu) return;

            if (this.menu != null) {
                this.menu.removeMenuPresenter(listMenuPresenter);
            }
            this.menu = menu;
            if (menu != null) {
                if (listMenuPresenter != null) menu.addMenuPresenter(listMenuPresenter);
            }
        }

        MenuView getListMenuView(Context context, MenuPresenter.Callback cb) {
            if (menu == null) return null;

            if (listMenuPresenter == null) {
                listMenuPresenter = new ListMenuPresenter(context,
                        R.layout.list_menu_item_layout);
                listMenuPresenter.setCallback(cb);
                listMenuPresenter.setId(R.id.list_menu_presenter);
                // LEWA ADD BEGIN
                // Set action flag for reset color of list menu.
                listMenuPresenter.setActionMode(Injector.isActionMode(ActionBarContextView.this));
                // LEWA ADD END
                menu.addMenuPresenter(listMenuPresenter);
            }

            MenuView result = listMenuPresenter.getMenuView(decorView);

            return result;
        }

        void clearMenuPresenters() {
            if (menu != null) {
                menu.removeMenuPresenter(listMenuPresenter);
            }

            listMenuPresenter = null;
        }
    }

    /** @hide */
    public void setActionModeOptionMenuVisibility(boolean show) {
        if (mSplitView == null) {
            return;
        }

        LewaActionBarContainer splitView = (LewaActionBarContainer) mSplitView;
        splitView.setActionModeOptionMenuVisibility(show);
    }

    /** @hide */
    public void toggleActionModeOptionMenu() {
        if (mSplitView == null) {
            return;
        }

        LewaActionBarContainer splitView = (LewaActionBarContainer) mSplitView;

        if (splitView.isActionModeOptionMenuVisible()) {
            splitView.setActionModeOptionMenuVisibility(false);
        } else {
            splitView.setActionModeOptionMenuVisibility(true);
        }
    }

///LEWA ADD BEGIN
    //@LewaHook(LewaHook.LewaHookType.NEW_CLASS)
    static class Injector {
        static int measureTitleView(View child, int availableWidth, int childSpecHeight,
            int spacing) {
            child.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY),
                    childSpecHeight);

            availableWidth -= child.getMeasuredWidth();
            availableWidth -= spacing;

            return Math.max(0, availableWidth);
        }

        static int getLeftButtonLayoutId(Context context) {
            if (true) {
                return R.layout.action_mode_close_item;
            } else {
                return R.layout.abc_action_mode_close_item;
            }
        }

        static ActionMenuPresenter newMenuPresenter(Context context, boolean isSplitActionBar) {
            if (isSplitActionBar) {
                return new LewaActionMenuPresenter(context);
            } else {
                return new ActionMenuPresenter(context);
            }
        }

        static int getContextMenuViewHeight(Context context, boolean isSplitActionBar, int height) {
            if (isSplitActionBar) {
                return dip2px(context, 70.0f);
            } else {
                return height;
            }
        }
        
        static void initilize(Context context, final ActionBarContextView abcv, TypedArray a) {
            abcv.mIsLewaUi = true;//LewaUiUtil.isLewaUi(context);


            abcv.mStatusbarHeight = (int)context.getResources().getDimension(R.dimen.android_status_bar_height);
        }

        static void setActionModeBackground(final ActionBarContextView abcv, Context context) {
//            if (abcv.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_WALLPAPER) {
//                LewaColorfulStyle.ColorfulNode colorfulStyle =
//                       ((LewaResources) context.getResources()).getColorfulResources().getColorfulStyle(context);
//
//                if (colorfulStyle != null) {
//                       int color = colorfulStyle.getMainColor();
//                    abcv.setBackgroundColor(color);
//                }
//            } else if (abcv.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_GAUSSIAN) {
//                Drawable wpDrawable = WallpaperManager.getInstance(context).getDrawable();
//                Bitmap wallpaperBmp = ((BitmapDrawable)wpDrawable).getBitmap();
//                int statusbar_height = (int)dpToPxCoord(context,
//                    context.getResources().getDimension(com.lewa.internal.R.dimen.android_status_bar_height));
//                int actionbar_height = (int)dpToPxCoord(context,
//                    context.getResources().getDimension(com.lewa.internal.R.dimen.android_action_bar_height));
//                int screen_width = context.getResources().getDisplayMetrics().widthPixels;
//
//				BitmapFilter filter = BitmapFilterFactory.createFilter(BitmapFilterFactory.GAUSSIAN_BLUR);
//				Bitmap b;
//				if (LewaUiUtil.isImmersiveStatusbar(context)) {
//					b = WallpaperManager.getInstance(context).InjectorGetScreenShotBitmap(0, 0, screen_width, 
//						statusbar_height + actionbar_height);
//				} else {
//					b = WallpaperManager.getInstance(context).InjectorGetScreenShotBitmap(0, statusbar_height, 
//						screen_width, actionbar_height);
//				}                
//				//Bitmap b = Bitmap.createBitmap(wallpaperBmp, 0, statusbar_height, screen_width, actionbar_height);
//                //abcv.setBackgroundDrawable((Drawable) new FilterDrawable(b, filter));
//                BlurOptions blurOptions = new BlurOptions();
//				blurOptions.strength = BlurOptions.STRENGTH_LOW;
//				abcv.setBackgroundDrawable((Drawable)new BitmapBlurDrawable(context, b, blurOptions));
//				WallpaperManager.getInstance(context).forgetLoadedWallpaper();
////            } else if (abcv.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_NONE) {
            	//abcv.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.abc_ab_share_pack_holo_dark));
//        	abcv.setBackgroundDrawable(ThemeManager.getInstance(context).getBlurredWallpaper());
//        	mBackground = ThemeManager.getInstance(context).getBlurredWallpaper();
//        	abcv.setBackgroundResource(R.drawable.actionbar_tittle_bg);
//        	abcv.setBackgroundColor(0xff0000ff);
            //}
        }

        static float dpToPxCoord(Context context, float oriSize) {
            float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
            if (scaledDensity == 0) {
                return oriSize;
            }

            return oriSize / scaledDensity;
        }

        static void onDraw(final ActionBarContextView abcv, Canvas canvas) {
            //if (abcv.mIsLewaUi && abcv.mColorfulMode != Settings.LEWA_COLORVIEW_MODE_NONE) {
            if (abcv.mIsLewaUi) {
                Paint p = new Paint();
                p.setColor(0x80000000);
                canvas.drawRect(0, 0, abcv.getWidth(), abcv.getHeight(), p);
            }
        }

        /*
         * Add option menu in action mode
         */
        static void addActionModeOptionMenu(final ActionBarContextView abcv,
                final MenuBuilder menu, final LayoutParams layoutParams) {
            if (abcv.mSplitView instanceof LewaActionBarContainer) {
                ((LewaActionBarContainer) abcv.mSplitView).getActionModeMenuBar().removeAllViews();
                ((LewaActionBarContainer) abcv.mSplitView).getActionModeMenuBar().addView(abcv.mMenuView, layoutParams);

                if (abcv.mActionMenuPresenter instanceof LewaActionMenuPresenter) {
                    final LewaActionBarContainer splitView = (LewaActionBarContainer) abcv.mSplitView;
                    Injector.initializeOptionMenu(abcv, splitView.getActionModeOptionMenuBar(), menu);

                    // Hide visible action menu bar while no visible items
                    int itemSize = menu.lewaGetActionItems().size();
                    if (itemSize <= 0) {
                        splitView.setActionModeMenuVisibility(false);
                    } else {
                        splitView.setActionModeMenuVisibility(true);
                    }

                    LewaActionMenuPresenter presenter = (LewaActionMenuPresenter) abcv.mActionMenuPresenter;
                    presenter.setOnActionMenuUpdateListener(null);
                    presenter.setOnActionMenuUpdateListener(new LewaActionMenuPresenter.OnActionMenuUpdateListener() {
                        public void onUpdated(MenuBuilder menu) {
                            int itemSize = menu.getNonActionItems().size();
                            splitView.setNonActionModeItemsSize(itemSize);

                            // Hide visible action menu bar while no visible items
                            itemSize = menu.lewaGetActionItems().size();
                            splitView.setActionItemsSize(itemSize);
                            if (itemSize <= 0) {
                                splitView.setActionModeMenuVisibility(false);
                            } else {
                                splitView.setActionModeMenuVisibility(true);
                            }
                        }
                    });
                    presenter.setOnPerformClickListener(null);
                    presenter.setOnPerformClickListener(new ActionMenuPresenter.OnPerformClickListener() {
                        @Override
                        public void onPerformClick() {
                            abcv.toggleActionModeOptionMenu();
                        }
                    });
                }
            }
        }

        static boolean initializePanelContent(final ActionBarContextView abcv, PanelFeatureState st) {
            if (st.menu == null) {
                return false;
            }

            MenuView menuView = st.getListMenuView(abcv.getContext(), null);
            st.shownPanelView = (View) menuView;
            if (st.shownPanelView != null) {
                // Use the menu View's default animations if it has any
                final int defaultAnimations = menuView.getWindowAnimations();
                if (defaultAnimations != 0) {
                    st.windowAnimations = defaultAnimations;
                }
                return true;
            } else {
                return false;
            }
        }

        static void initializeOptionMenu(final ActionBarContextView abcv, ViewGroup parentView, MenuBuilder menu) {
            if (abcv.mSplitView == null) {
                return;
            }

            Injector.clearActionModeOptionMenu(abcv);
            abcv.mOptionalMenuState = abcv.new PanelFeatureState();

            abcv.mOptionalMenuState.decorView = new FrameLayout(abcv.getContext());
            abcv.mOptionalMenuState.setStyle(abcv.getContext());

            // Init menu
            abcv.mOptionalMenuState.setMenu(menu);
            Injector.initializePanelContent(abcv, abcv.mOptionalMenuState);

            // Add optional menu into menu bar.
            ViewGroup.LayoutParams lp = abcv.mOptionalMenuState.shownPanelView.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }
            parentView.addView(abcv.mOptionalMenuState.shownPanelView, lp);

            Injector.updateActionMenuStyle(abcv, menu);
        }

        static void clearActionModeOptionMenu(final ActionBarContextView abcv) {
            if (abcv.mOptionalMenuState != null) {
                LewaActionBarContainer splitView = (LewaActionBarContainer) abcv.mSplitView;
                splitView.getActionModeOptionMenuBar().removeAllViews();
                splitView.setActionModeOptionMenuVisibility(false);

                abcv.mOptionalMenuState.clearMenuPresenters();
                abcv.mOptionalMenuState.setMenu(null);

                abcv.mOptionalMenuState.parentView = null;
                abcv.mOptionalMenuState.shownPanelView = null;
                abcv.mOptionalMenuState.decorView = null;
            }

            abcv.mOptionalMenuState = null;
        }

        static void setViewPadding(final ActionBarContextView abcv, View v) {
            if (true) {
                v.setPadding(v.getPaddingLeft(), abcv.mStatusbarHeight, v.getPaddingRight(),
                    v.getPaddingBottom());
            }
        }

        static void resetContextViewHeight(final ActionBarContextView abcv) {
            if (true) {
                abcv.mContentHeight += abcv.mStatusbarHeight;
            }
        }

        static void resetSplitActionBarHeight(Context context, LayoutParams lp) {
            int actionbarHeight = ActionBarPolicy.get(context).getTabContainerHeight();
            lp.height = actionbarHeight;
        }

        static void setViewClickStyle(Context context, View v) {
            if (true) {
                v.setBackgroundColor(0);
                v.setOnTouchListener(new InjectorTouchListener());
            }
        }

        static void setActionMode(final ActionBarContextView oThis, boolean actionMode) {
            if (oThis.mActionMenuPresenter == null) {
                return;
            }

            if (true) {
                if (oThis.mActionMenuPresenter instanceof LewaActionMenuPresenter) {
                    LewaActionMenuPresenter menuPresenter = (LewaActionMenuPresenter) oThis.mActionMenuPresenter;
                    menuPresenter.setActionMode(actionMode);
                }
            }
        }

        static boolean isActionMode(final ActionBarContextView oThis) {
            if (oThis.mActionMenuPresenter == null) {
                return false;
            }

            if (true) {
                if (oThis.mActionMenuPresenter instanceof LewaActionMenuPresenter) {
                    LewaActionMenuPresenter menuPresenter = (LewaActionMenuPresenter) oThis.mActionMenuPresenter;
                    return menuPresenter.isActionMode();
                }
            }

            return false;
        }

        /**
         * Update action menu style between Icon or Icon & Text mode.
         */
        static void updateActionMenuStyle(final ActionBarContextView oThis, MenuBuilder menu) {
            if (menu == null) {
                return;
            }

            ArrayList<MenuItemImpl> actionMenus = menu.lewaGetVisibleItems();
            if (actionMenus == null) {
                return;
            }

            MenuItemImpl item = null;
            int showAsAction = 0;
            int savedMenuStyle = Settings.System.getInt(oThis.mContext.getContentResolver(), ActionBar.LEWA_ACTION_MENU_STYLE, 
            		ActionBar.LEWA_ACTION_MENU_STYLE_ICON);
            int newStyle = 0;
            for (int i = 0; i < actionMenus.size(); i++) {
                item = actionMenus.get(i);
                showAsAction = item.getShowAsAction();
                newStyle = showAsAction;

                if (savedMenuStyle == 0) {
                    if ((showAsAction & MenuItem.SHOW_AS_ACTION_WITH_TEXT) != 0) {
                        newStyle ^= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
                    }
                } else if (savedMenuStyle == 1) {
                    if ((showAsAction & MenuItem.SHOW_AS_ACTION_WITH_TEXT) == 0) {
                        newStyle |= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
                    }
                }

                item.setShowAsAction(newStyle);
            }
        }
    }

///LEWA BEGIN
    //@LewaHook(LewaHook.LewaHookType.NEW_CLASS)
    static class InjectorTouchListener implements OnTouchListener {
           private MotionEvent mCurrentDownEvent;
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            final float x = event.getX();
            final float y = event.getY();
            ImageView closeView = (ImageView)v.findViewById(R.id.action_mode_close_button_image);

            switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
                closeView.setAlpha(0x80);
                break;
            case MotionEvent.ACTION_MOVE:
                final int deltaX = (int) (x - mCurrentDownEvent.getX());
                final int deltaY = (int) (y - mCurrentDownEvent.getY());
                int distance = (deltaX * deltaX) + (deltaY * deltaY);

                if (distance == 0) {
                    break;
                }
                closeView.setAlpha(0xff);
                break;
            case MotionEvent.ACTION_UP:
                closeView.setAlpha(0xff);
                break;
            }

            return false;
        }
    }
///LEWA END
}
