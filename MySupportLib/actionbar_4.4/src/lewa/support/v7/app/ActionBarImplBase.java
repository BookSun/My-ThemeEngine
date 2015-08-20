/*
 * Copyright (C) 2012 The Android Open Source Project
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

package lewa.support.v7.app;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.internal.view.ActionBarPolicy;
import lewa.support.v7.internal.view.SupportMenuInflater;
import lewa.support.v7.internal.view.menu.MenuBuilder;
import lewa.support.v7.internal.view.menu.SubMenuBuilder;
import lewa.support.v7.internal.widget.ActionBarContainer;
import lewa.support.v7.internal.widget.ActionBarContextView;
import lewa.support.v7.internal.widget.ActionBarOverlayLayout;
import lewa.support.v7.internal.widget.ActionBarView;
import lewa.support.v7.internal.widget.ScrollingTabContainerView;
import lewa.support.v7.view.ActionMode;
import android.support.v4.internal.view.SupportMenuItem;
import android.util.TypedValue;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SpinnerAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ActionBarImplBase extends ActionBar {
    private Context mContext;
    private Context mThemedContext;
    private ActionBarActivity mActivity;
    private Dialog mDialog;

    private ActionBarOverlayLayout mOverlayLayout;
    private ActionBarContainer mContainerView;
    private ViewGroup mTopVisibilityView;
    private ActionBarView mActionView;
    private ActionBarContextView mContextView;
    private ActionBarContainer mSplitView;
    private View mContentView;
    private ScrollingTabContainerView mTabScrollView;

    private ArrayList<TabImpl> mTabs = new ArrayList<TabImpl>();

    private TabImpl mSelectedTab;
    private int mSavedTabPosition = INVALID_POSITION;

    private boolean mDisplayHomeAsUpSet;

    ActionModeImpl mActionMode;
    ActionMode mDeferredDestroyActionMode;
    ActionMode.Callback mDeferredModeDestroyCallback;

    private boolean mLastMenuVisibility;
    private ArrayList<OnMenuVisibilityListener> mMenuVisibilityListeners =
            new ArrayList<OnMenuVisibilityListener>();

    private static final int CONTEXT_DISPLAY_NORMAL = 0;
    private static final int CONTEXT_DISPLAY_SPLIT = 1;

    private static final int INVALID_POSITION = -1;

    private int mContextDisplayMode;
    private boolean mHasEmbeddedTabs;

    final Handler mHandler = new Handler();
    Runnable mTabSelector;

    private int mCurWindowVisibility = View.VISIBLE;

    private boolean mHiddenByApp;
    private boolean mHiddenBySystem;
    private boolean mShowingForMode;

    private boolean mNowShowing = true;

    private Animator mCurrentShowAnim;
    private boolean mShowHideAnimationEnabled;
///LEWA BEGIN
    boolean mWasHiddenBeforeMode;
///LEWA END

    private Callback mCallback;
///LEWA BEGIN
    final AnimatorListener mSplitHideListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setTranslationY(0);
                mSplitView.setVisibility(View.GONE);
                mSplitView.setTransitioning(false);
            }
            mCurrentShowAnim = null;
            //completeDeferredDestroyActionMode();
        }
    };
///LEWA END

    final AnimatorListener mShowListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mCurrentShowAnim = null;
            mTopVisibilityView.requestLayout();
        }
    };
    public ActionBarImplBase(ActionBarActivity activity, Callback callback) {
        mActivity = activity;
        mContext = activity;
        mCallback = callback;
        init(mActivity);
        if (!mActivity.getWindow().hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY)) {
            mContentView = mActivity.findViewById(android.R.id.content);
        }
    }

    private void init(ActionBarActivity activity) {
        mOverlayLayout = (ActionBarOverlayLayout) activity.findViewById(
                R.id.action_bar_overlay_layout);
        if (mOverlayLayout != null) {
            mOverlayLayout.setActionBar(this);
        }
        mActionView = (ActionBarView) activity.findViewById(R.id.action_bar);
        mContextView = (ActionBarContextView) activity.findViewById(R.id.action_context_bar);
        mContainerView = (ActionBarContainer) activity.findViewById(R.id.action_bar_container);
///LEWA BEGIN
        mContainerView.setVisibility(View.VISIBLE);
///LEWA END
        mTopVisibilityView = (ViewGroup) activity.findViewById(R.id.top_action_bar);
        if (mTopVisibilityView == null) {
            mTopVisibilityView = mContainerView;
        }
        mSplitView = (ActionBarContainer) activity.findViewById(R.id.split_action_bar);

        if (mActionView == null || mContextView == null || mContainerView == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with a compatible window decor layout");
        }

        mActionView.setContextView(mContextView);
        mContextDisplayMode = mActionView.isSplitActionBar() ?
                CONTEXT_DISPLAY_SPLIT : CONTEXT_DISPLAY_NORMAL;

        // This was initially read from the action bar style
        final int current = mActionView.getDisplayOptions();
        final boolean homeAsUp = (current & DISPLAY_HOME_AS_UP) != 0;
        if (homeAsUp) {
            mDisplayHomeAsUpSet = true;
        }

        ActionBarPolicy abp = ActionBarPolicy.get(mContext);
        setHomeButtonEnabled(abp.enableHomeButtonByDefault() || homeAsUp);
        setHasEmbeddedTabs(abp.hasEmbeddedTabs());
        ///LEWA ADD BEGIN
        //mContainerView.setBackground(WallpaperManager.getInstance(mContext).getDrawable());
        ///LEWA ADD END
        mActionView.setWindowTitle(mActivity.getTitle());
    }

    public void onConfigurationChanged(Configuration newConfig) {
        setHasEmbeddedTabs(ActionBarPolicy.get(mContext).hasEmbeddedTabs());
    }

    private void setHasEmbeddedTabs(boolean hasEmbeddedTabs) {
        mHasEmbeddedTabs = hasEmbeddedTabs;
        // Switch tab layout configuration if needed
        if (!mHasEmbeddedTabs) {
            mActionView.setEmbeddedTabView(null);
            mContainerView.setTabContainer(mTabScrollView);
        } else {
            mContainerView.setTabContainer(null);
            mActionView.setEmbeddedTabView(mTabScrollView);
        }
        final boolean isInTabMode = getNavigationMode() == NAVIGATION_MODE_TABS;
        if (mTabScrollView != null) {
            if (isInTabMode) {
                mTabScrollView.setVisibility(View.VISIBLE);
                if (mOverlayLayout != null) {
                    mOverlayLayout.requestFitSystemWindows();
                }
            } else {
                mTabScrollView.setVisibility(View.GONE);
            }
        }
        mActionView.setCollapsable(!mHasEmbeddedTabs && isInTabMode);
    }

    public boolean hasNonEmbeddedTabs() {
        return !mHasEmbeddedTabs && getNavigationMode() == NAVIGATION_MODE_TABS;
    }
	
    private void ensureTabsExist() {
        if (mTabScrollView != null) {
            return;
        }

        ScrollingTabContainerView tabScroller = new ScrollingTabContainerView(mContext);

        if (mHasEmbeddedTabs) {
            tabScroller.setVisibility(View.VISIBLE);
            mActionView.setEmbeddedTabView(tabScroller);
        } else {
            if (getNavigationMode() == NAVIGATION_MODE_TABS) {
                tabScroller.setVisibility(View.VISIBLE);
            } else {
                tabScroller.setVisibility(View.GONE);
            }
            mContainerView.setTabContainer(tabScroller);
        }
        mTabScrollView = tabScroller;
    }
	
    @Override
    public void setCustomView(View view) {
        mActionView.setCustomNavigationView(view);
    }

    @Override
    public void setCustomView(View view, LayoutParams layoutParams) {
        view.setLayoutParams(layoutParams);
        mActionView.setCustomNavigationView(view);

    }

    @Override
    public void setCustomView(int resId) {
        setCustomView(LayoutInflater.from(getThemedContext())
                .inflate(resId, mActionView, false));
    }

    @Override
    public void setIcon(int resId) {
        mActionView.setIcon(resId);
    }

    @Override
    public void setIcon(Drawable icon) {
        mActionView.setIcon(icon);
    }

    @Override
    public void setLogo(int resId) {
        mActionView.setLogo(resId);
    }

    @Override
    public void setLogo(Drawable logo) {
        mActionView.setLogo(logo);
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback) {
        mActionView.setDropdownAdapter(adapter);
        mActionView.setCallback(callback);
    }

    @Override
    public void setSelectedNavigationItem(int position) {
        switch (mActionView.getNavigationMode()) {
            case NAVIGATION_MODE_TABS:
                selectTab(mTabs.get(position));
                break;
            case NAVIGATION_MODE_LIST:
                mActionView.setDropdownSelectedPosition(position);
                break;
            default:
                throw new IllegalStateException(
                        "setSelectedNavigationIndex not valid for current navigation mode");
        }
    }

    @Override
    public int getSelectedNavigationIndex() {
        switch (mActionView.getNavigationMode()) {
            case NAVIGATION_MODE_TABS:
                return mSelectedTab != null ? mSelectedTab.getPosition() : -1;
            case NAVIGATION_MODE_LIST:
                return mActionView.getDropdownSelectedPosition();
            default:
                return -1;
        }
    }

    @Override
    public int getNavigationItemCount() {
        switch (mActionView.getNavigationMode()) {
            case NAVIGATION_MODE_TABS:
                return mTabs.size();
            case NAVIGATION_MODE_LIST:
                SpinnerAdapter adapter = mActionView.getDropdownAdapter();
                return adapter != null ? adapter.getCount() : 0;
            default:
                return 0;
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mActionView.setTitle(title);
    }

    @Override
    public void setTitle(int resId) {
        setTitle(mContext.getString(resId));
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mActionView.setSubtitle(subtitle);
    }

    @Override
    public void setSubtitle(int resId) {
        setSubtitle(mContext.getString(resId));
    }

    @Override
    public void setDisplayOptions(int options) {
        if ((options & DISPLAY_HOME_AS_UP) != 0) {
            mDisplayHomeAsUpSet = true;
        }
        mActionView.setDisplayOptions(options);
    }

    @Override
    public void setDisplayOptions(int options, int mask) {
        final int current = mActionView.getDisplayOptions();
        if ((mask & DISPLAY_HOME_AS_UP) != 0) {
            mDisplayHomeAsUpSet = true;
        }
        mActionView.setDisplayOptions((options & mask) | (current & ~mask));
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean useLogo) {
        setDisplayOptions(useLogo ? DISPLAY_USE_LOGO : 0, DISPLAY_USE_LOGO);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean showHome) {
        setDisplayOptions(showHome ? DISPLAY_SHOW_HOME : 0, DISPLAY_SHOW_HOME);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        setDisplayOptions(showHomeAsUp ? DISPLAY_HOME_AS_UP : 0, DISPLAY_HOME_AS_UP);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean showTitle) {
        setDisplayOptions(showTitle ? DISPLAY_SHOW_TITLE : 0, DISPLAY_SHOW_TITLE);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean showCustom) {
        setDisplayOptions(showCustom ? DISPLAY_SHOW_CUSTOM : 0, DISPLAY_SHOW_CUSTOM);
    }

    @Override
    public void setHomeButtonEnabled(boolean enable) {
        mActionView.setHomeButtonEnabled(enable);
    }

    @Override
    ///LEWA ADD BEGIN
    //@LewaHook(LewaHook.LewaHookType.NEW_METHOD)
    public void smoothScrollTabIndicator(int position, float positionOffset, int positionOffsetPixels) {
        switch (mActionView.getNavigationMode()) {
        case NAVIGATION_MODE_TABS:
            mTabScrollView.smoothScrollTabIndicator(position, positionOffset, positionOffsetPixels);
            break;
        default:
            break;
        }
    }

    //@LewaHook(LewaHook.LewaHookType.NEW_METHOD)
    public void setScrollState(int state) {
        switch (mActionView.getNavigationMode()) {
        case NAVIGATION_MODE_TABS:
            mTabScrollView.setScrollState(state);
            break;
        default:
            break;
        }
    }
    ///LEWA ADD END

    public void setBackgroundDrawable(Drawable d) {
        mContainerView.setPrimaryBackground(d);
    }

    @Override
    public View getCustomView() {
        return mActionView.getCustomNavigationView();
    }

    @Override
    public CharSequence getTitle() {
        return mActionView.getTitle();
    }

    @Override
    public CharSequence getSubtitle() {
        return mActionView.getSubtitle();
    }

    @Override
    public int getNavigationMode() {
        return mActionView.getNavigationMode();
    }
    @Override
    public int getDisplayOptions() {
        return mActionView.getDisplayOptions();
    }
	
   public ActionMode startActionMode(ActionMode.Callback callback) {
        if (mActionMode != null) {
            mActionMode.finish();
        }

        mContextView.killMode();
        ActionModeImpl mode = new ActionModeImpl(callback);
        if (mode.dispatchOnCreate()) {
            mode.invalidate();
            mContextView.initForMode(mode);
            animateToMode(true);
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                if (mSplitView.getVisibility() != View.VISIBLE) {
                    mSplitView.setVisibility(View.VISIBLE);
                }
            }
            mContextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            mActionMode = mode;
			///LEWA BEGIN
            mode.setSelectionMode(ActionMode.SELECT_ALL);
///LEWA END
            return mode;
        }
        return null;
    }
    @Override
    public void setNavigationMode(int mode) {
        final int oldMode = mActionView.getNavigationMode();
        switch (oldMode) {
            case NAVIGATION_MODE_TABS:
                mSavedTabPosition = getSelectedNavigationIndex();
                selectTab(null);
                mTabScrollView.setVisibility(View.GONE);
                break;
        }
        mActionView.setNavigationMode(mode);
        switch (mode) {
            case NAVIGATION_MODE_TABS:
                ensureTabsExist();
                mTabScrollView.setVisibility(View.VISIBLE);
                if (mSavedTabPosition != INVALID_POSITION) {
                    setSelectedNavigationItem(mSavedTabPosition);
                    mSavedTabPosition = INVALID_POSITION;
                }
                break;
        }
        mActionView.setCollapsable(mode == NAVIGATION_MODE_TABS && !mHasEmbeddedTabs);
    }



    @Override
    public Tab newTab() {
        return new TabImpl();
    }

    @Override
    public void addTab(Tab tab) {
        addTab(tab, mTabs.isEmpty());
    }

    @Override
    public void addTab(Tab tab, boolean setSelected) {
        ensureTabsExist();
        mTabScrollView.addTab(tab, setSelected);
        configureTab(tab, mTabs.size());
        if (setSelected) {
            selectTab(tab);
        }
    }

    @Override
    public void addTab(Tab tab, int position) {
        addTab(tab, position, mTabs.isEmpty());
    }

    @Override
    public void addTab(Tab tab, int position, boolean setSelected) {
        ensureTabsExist();
        mTabScrollView.addTab(tab, position, setSelected);
        configureTab(tab, position);
        if (setSelected) {
            selectTab(tab);
        }
    }

///LEWA BEGIN
	/**
	* to tell action bar a tab has secondary tab or not
	* @hide
	*/
	//@LewaHook(LewaHook.LewaHookType.NEW_METHOD)
	@Override
	public void setHasSecondaryTab(int index) {
		mTabScrollView.setHasSecondaryTab(index);
	}
///LEWA END

    @Override
    public void removeTab(Tab tab) {
        removeTabAt(tab.getPosition());
    }

    @Override
    public void removeTabAt(int position) {
        if (mTabScrollView == null) {
            // No tabs around to remove
            return;
        }

        int selectedTabPosition = mSelectedTab != null
                ? mSelectedTab.getPosition() : mSavedTabPosition;
        mTabScrollView.removeTabAt(position);
        TabImpl removedTab = mTabs.remove(position);
        if (removedTab != null) {
            removedTab.setPosition(-1);
        }

        final int newTabCount = mTabs.size();
        for (int i = position; i < newTabCount; i++) {
            mTabs.get(i).setPosition(i);
        }

        if (selectedTabPosition == position) {
            selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));
        }
    }

    @Override
    public void removeAllTabs() {
        cleanupTabs();
    }

    @Override
    public void selectTab(Tab tab) {
        if (getNavigationMode() != NAVIGATION_MODE_TABS) {
            mSavedTabPosition = tab != null ? tab.getPosition() : INVALID_POSITION;
            return;
        }

        final FragmentTransaction trans = mActivity.getSupportFragmentManager().beginTransaction()
                .disallowAddToBackStack();

        if (mSelectedTab == tab) {
            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabReselected(mSelectedTab, trans);
                mTabScrollView.animateToTab(tab.getPosition());
            }
        } else {
            mTabScrollView.setTabSelected(tab != null ? tab.getPosition() : Tab.INVALID_POSITION);
            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabUnselected(mSelectedTab, trans);
            }
            mSelectedTab = (TabImpl) tab;
            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabSelected(mSelectedTab, trans);
            }
        }

        if (!trans.isEmpty()) {
            trans.commit();
        }
    }

    @Override
    public Tab getSelectedTab() {
        return mSelectedTab;
    }

    @Override
    public Tab getTabAt(int index) {
        return mTabs.get(index);
    }

    @Override
///LEWA BEGIN
    //add by Lewa Shenqi
    public int getSplitHeight() {
        if(mSplitView != null) {
            return mSplitView.getSplitHeight();
        }
        else {
            return 0;
        }
    }
    //end by Lewa Shenqi
///LEWA END
    public int getTabCount() {
        return mTabs.size();
    }

    @Override
    public Context getThemedContext() {
        if (mThemedContext == null) {
            TypedValue outValue = new TypedValue();
            Resources.Theme currentTheme = mContext.getTheme();
            currentTheme.resolveAttribute(R.attr.actionBarWidgetTheme, outValue, true);
            final int targetThemeRes = outValue.resourceId;

            if (targetThemeRes != 0) {
                mThemedContext = new ContextThemeWrapper(mContext, targetThemeRes);
            } else {
                mThemedContext = mContext;
            }
        }
        return mThemedContext;
    }

    @Override
    public int getHeight() {
        return mContainerView.getHeight();
    }

    @Override
    public void show() {
        if (mHiddenByApp) {
            mHiddenByApp = false;
            updateVisibility(false);
        }
    }

    void showForActionMode() {
        if (!mShowingForMode) {
            mShowingForMode = true;

            updateVisibility(false);
        }
    }

    @Override
    public void hide() {
        if (!mHiddenByApp) {
            mHiddenByApp = true;
            updateVisibility(false);
        }
    }

    void hideForActionMode() {
        if (mShowingForMode) {
            mShowingForMode = false;

            updateVisibility(false);
        }
    }

    @Override
    public boolean isShowing() {
        return mNowShowing;
    }

    @Override
    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        mMenuVisibilityListeners.add(listener);
    }

    ///LEWA BEGIN
    //add by Lewa Shenqi
    @Override
    public void showSplit() {
        showSplit(true);
    }

    void showSplit(boolean markHiddenBeforeMode) {
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        if (mContainerView.getVisibility() == View.VISIBLE) {
            if (markHiddenBeforeMode)
                mWasHiddenBeforeMode = false;
        }

        if (mShowHideAnimationEnabled) {
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                AnimatorSet anim = new AnimatorSet();
                AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mSplitView, "alpha", 1));
                mSplitView.setAlpha(0);
                mSplitView.setVisibility(View.VISIBLE);
                //mSplitView.setTranslationY(mSplitView.getHeight());
                b.with(ObjectAnimator.ofFloat(mSplitView, "translationY",
                        mSplitView.getHeight(), 0));
                anim.addListener(mShowListener);
                mCurrentShowAnim = anim;
                anim.start();
            }
        } else {
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setAlpha(1);
                mSplitView.setTranslationY(0);
                mSplitView.setVisibility(View.VISIBLE);
            }
            mShowListener.onAnimationEnd(null);
        }
    }
    //end add by Lewa shenqi
///LEWA END

///LEWA BEGIN
    //add by shenqi for hidden only splitView
    public void hideSplit() {
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        if (mSplitView == null || mSplitView.getVisibility() == View.GONE) {
            return;
        }
        if (mShowHideAnimationEnabled) {
            mSplitView.setAlpha(1);
            mSplitView.setTransitioning(true);
            AnimatorSet anim = new AnimatorSet();
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mSplitView, "alpha", 0));
            b.with(ObjectAnimator.ofFloat(mSplitView, "translationY",
                        0,mSplitView.getHeight()));
            anim.addListener(mSplitHideListener);
            mCurrentShowAnim = anim;
            anim.start();
        } else {
            mSplitHideListener.onAnimationEnd(null);
        }
    }

    public boolean isSplitVisible() {
        if (mSplitView == null) {
            return false;
        }

        if (mSplitView.getVisibility() == View.VISIBLE) {
            return true;
        }

        return false;
    }

    public boolean isActionOptionMenuVisible() {
        if (mSplitView == null) {
            return false;
        }

        View vActionOptionMenu = mSplitView.findViewById(R.id.ll_action_option_menubar);
        if (vActionOptionMenu == null) {
            return false;
        }

        if (vActionOptionMenu.getVisibility() == View.VISIBLE) {
            return true;
        }

        return false;
    }

    public void hideSplitNoAnimation() {
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        if (mSplitView == null || mSplitView.getVisibility() == View.GONE) {
            return;
        }
        mSplitHideListener.onAnimationEnd(null);
    }
    // end add by Lewa shenqi
///LEWA END

    @Override
    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        mMenuVisibilityListeners.remove(listener);
    }

 

    void animateToMode(boolean toActionMode) {
        if (toActionMode) {
            showForActionMode();
        } else {
            hideForActionMode();
        }

        mActionView.animateToVisibility(toActionMode ? View.GONE : View.VISIBLE);
        if (!toActionMode) {//update
        	mActionView.updateActionMenuStyle();
        }
        mContextView.animateToVisibility(toActionMode ? View.VISIBLE : View.GONE);
        if (mTabScrollView != null && !mActionView.hasEmbeddedTabs() && mActionView.isCollapsed()) {
            mTabScrollView.setVisibility(toActionMode ? View.GONE : View.VISIBLE);
        }
    }



public  class ActionModeImpl extends ActionMode implements MenuBuilder.Callback {

        private ActionMode.Callback mCallback;
        private MenuBuilder mMenu;
        private WeakReference<View> mCustomView;

        public ActionModeImpl(ActionMode.Callback callback) {
            mCallback = callback;
            mMenu = new MenuBuilder(getThemedContext())
                    .setDefaultShowAsAction(SupportMenuItem.SHOW_AS_ACTION_IF_ROOM);
            mMenu.setCallback(this);
        }

        @Override
        public MenuInflater getMenuInflater() {
            return new SupportMenuInflater(getThemedContext());
        }

        @Override
        public Menu getMenu() {
            return mMenu;
        }

        @Override
        public void finish() {
            if (mActionMode != this) {
                // Not the active action mode - no-op
                return;
            }

            // If this change in state is going to cause the action bar
            // to be hidden, defer the onDestroy callback until the animation
            // is finished and associated relayout is about to happen. This lets
            // apps better anticipate visibility and layout behavior.
            if (!checkShowingFlags(mHiddenByApp, mHiddenBySystem, false)) {
                // With the current state but the action bar hidden, our
                // overall showing state is going to be false.
                mDeferredDestroyActionMode = this;
                mDeferredModeDestroyCallback = mCallback;
            } else {
                mCallback.onDestroyActionMode(this);
            }
            // LEWA MODIFY START
            // BUGBUG: Object will be assign to null when enter as second time.
            // mCallback = null;
            // LEWA MODIFY END
            animateToMode(false);

            // Clear out the context mode views after the animation finishes
            mContextView.closeMode();
            mActionView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

            mActionMode = null;
        }

        @Override
        public void invalidate() {
            mMenu.stopDispatchingItemsChanged();
            try {
                mCallback.onPrepareActionMode(this, mMenu);
            } finally {
                mMenu.startDispatchingItemsChanged();
            }
        }

        public boolean dispatchOnCreate() {
            mMenu.stopDispatchingItemsChanged();
            try {
                return mCallback.onCreateActionMode(this, mMenu);
            } finally {
                mMenu.startDispatchingItemsChanged();
            }
        }

        @Override
        public void setCustomView(View view) {
            mContextView.setCustomView(view);
            mCustomView = new WeakReference<View>(view);
        }

        @Override
        public void setSubtitle(CharSequence subtitle) {
            mContextView.setSubtitle(subtitle);
        }

        @Override
        public void setTitle(CharSequence title) {
            mContextView.setTitle(title);
        }

        @Override
        public void setTitle(int resId) {
            setTitle(mContext.getResources().getString(resId));
        }

        @Override
        public void setSubtitle(int resId) {
            setSubtitle(mContext.getResources().getString(resId));
        }

        @Override
        public CharSequence getTitle() {
            return mContextView.getTitle();
        }

        @Override
        public CharSequence getSubtitle() {
            return mContextView.getSubtitle();
        }

        @Override
        public void setTitleOptionalHint(boolean titleOptional) {
            super.setTitleOptionalHint(titleOptional);
            mContextView.setTitleOptional(titleOptional);
        }

        @Override
        public boolean isTitleOptional() {
            return mContextView.isTitleOptional();
        }

        @Override
        public View getCustomView() {
            return mCustomView != null ? mCustomView.get() : null;
        }
		
		///LEWA BEGIN
        @Override
        /** @hide */
        public void setRightActionButtonDrawable(Drawable drawable) {
            if (drawable != null) {
                mContextView.setRightActionButtonDrawable(drawable);
            }
        }

        @Override
        /** @hide */
        public void setRightActionButtonResource(int resId) {
            if (resId != 0) {
                mContextView.setRightActionButtonDrawable(mContext.getResources().getDrawable(resId));
            }
        }

        @Override
        /** @hide */
        public void setRightActionButtonVisibility(int visibility) {
            mContextView.setRightActionButtonVisibility(visibility);
        }
///LEWA END

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            if (mCallback != null) {
			                // LEWA ADD START
                if (true) {
                    mContextView.setActionModeOptionMenuVisibility(false);
                }
                // LEWA ADD END
                return mCallback.onActionItemClicked(this, item);
            } else {
                return false;
            }
        }

        @Override
        public void onMenuModeChange(MenuBuilder menu) {
            if (mCallback == null) {
                return;
            }
            invalidate();
            mContextView.showOverflowMenu();
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            if (mCallback == null) {
                return false;
            }

            if (!subMenu.hasVisibleItems()) {
                return true;
            }

            //new MenuPopupHelper(getThemedContext(), subMenu).show();
            return true;
        }

        public void onCloseSubMenu(SubMenuBuilder menu) {
        }

        public void onMenuModeChange(Menu menu) {
            if (mCallback == null) {
                return;
            }
            invalidate();
            mContextView.showOverflowMenu();
        }
    }

    /**
     * @hide
     */
    public class TabImpl extends ActionBar.Tab {

        private ActionBar.TabListener mCallback;
        private Object mTag;
        private Drawable mIcon;
        private CharSequence mText;
        private CharSequence mContentDesc;
        private int mPosition = -1;
        private View mCustomView;
		private int mCustomViewWidth = 0;
		private boolean mHideIndicator = false;

        @Override
        public Object getTag() {
            return mTag;
        }

        @Override
        public Tab setTag(Object tag) {
            mTag = tag;
            return this;
        }

        public ActionBar.TabListener getCallback() {
            return mCallback;
        }

        @Override
        public Tab setTabListener(ActionBar.TabListener callback) {
            mCallback = callback;
            return this;
        }
		
		        @Override
        public Tab setCustomView(int layoutResId) {
            return setCustomView(LayoutInflater.from(getThemedContext())
                    .inflate(layoutResId, null));
        }

        @Override
        public View getCustomView() {
            return mCustomView;
        }

        @Override
        public Tab setCustomView(View view) {
            mCustomView = view;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }


		@Override
		public void setCustomViewWidth(int width) {
		    mCustomViewWidth = width;
		}

		@Override
		public int getCustomViewWidth() {
		    return mCustomViewWidth;
		}

		@Override
		public void setHideIndicator(boolean hide) {
		    mHideIndicator = hide;
		}

		@Override
		public boolean isHideIndicator() {
			return mHideIndicator;
		}
        @Override
        public Drawable getIcon() {
            return mIcon;
        }

        @Override
        public int getPosition() {
            return mPosition;
        }

        public void setPosition(int position) {
            mPosition = position;
        }

        @Override
        public CharSequence getText() {
            return mText;
        }

        @Override
        public Tab setIcon(Drawable icon) {
            mIcon = icon;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }

        @Override
        public Tab setIcon(int resId) {
            return setIcon(mContext.getResources().getDrawable(resId));
        }

        @Override
        public Tab setText(CharSequence text) {
            mText = text;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }

        @Override
        public Tab setText(int resId) {
            return setText(mContext.getResources().getText(resId));
        }

        @Override
        public void select() {
            selectTab(this);
        }

        @Override
        public Tab setContentDescription(int resId) {
            return setContentDescription(mContext.getResources().getText(resId));
        }

        @Override
        public Tab setContentDescription(CharSequence contentDesc) {
            mContentDesc = contentDesc;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }

        @Override
        public CharSequence getContentDescription() {
            return mContentDesc;
        }
    }


    private void configureTab(Tab tab, int position) {
        final TabImpl tabi = (TabImpl) tab;
        final ActionBar.TabListener callback = tabi.getCallback();

        if (callback == null) {
            throw new IllegalStateException("Action Bar Tab must have a Callback");
        }

        tabi.setPosition(position);
        mTabs.add(position, tabi);

        final int count = mTabs.size();
        for (int i = position + 1; i < count; i++) {
            mTabs.get(i).setPosition(i);
        }
    }

    private void cleanupTabs() {
        if (mSelectedTab != null) {
            selectTab(null);
        }
        mTabs.clear();
        if (mTabScrollView != null) {
            mTabScrollView.removeAllTabs();
        }
        mSavedTabPosition = INVALID_POSITION;
    }

    private static boolean checkShowingFlags(boolean hiddenByApp, boolean hiddenBySystem,
            boolean showingForMode) {
        if (showingForMode) {
            return true;
        } else if (hiddenByApp || hiddenBySystem) {
            return false;
        } else {
            return true;
        }
    }

    private void updateVisibility(boolean fromSystem) {
        // Based on the current state, should we be hidden or shown?
        final boolean shown = checkShowingFlags(mHiddenByApp, mHiddenBySystem, mShowingForMode);

        if (shown) {
            if (!mNowShowing) {
                mNowShowing = true;
                doShow(fromSystem);
            }
        } else {
            if (mNowShowing) {
                mNowShowing = false;
                doHide(fromSystem);
            }
        }
    }

    public void setShowHideAnimationEnabled(boolean enabled) {
        mShowHideAnimationEnabled = enabled;
        if (!enabled) {
            mTopVisibilityView.clearAnimation();
            if (mSplitView != null) {
                mSplitView.clearAnimation();
            }
        }
    }

    public void doShow(boolean fromSystem) {
        mTopVisibilityView.clearAnimation();
        if (mTopVisibilityView.getVisibility() == View.VISIBLE) {
            return;
        }

        final boolean animate = isShowHideAnimationEnabled() || fromSystem;

        if (animate) {
            Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.abc_slide_in_top);
            mTopVisibilityView.startAnimation(anim);
        }
        mTopVisibilityView.setVisibility(View.VISIBLE);

        if (mSplitView != null && mSplitView.getVisibility() != View.VISIBLE) {
            if (animate) {
                Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.abc_slide_in_bottom);
                mSplitView.startAnimation(anim);
            }
            mSplitView.setVisibility(View.VISIBLE);
        }
    }

    public void doHide(boolean fromSystem) {
        mTopVisibilityView.clearAnimation();
        if (mTopVisibilityView.getVisibility() == View.GONE) {
            return;
        }

        final boolean animate = isShowHideAnimationEnabled() || fromSystem;

        if (animate) {
            Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.abc_slide_out_top);
            mTopVisibilityView.startAnimation(anim);
        }
        mTopVisibilityView.setVisibility(View.GONE);

        if (mSplitView != null && mSplitView.getVisibility() != View.GONE) {
            if (animate) {
                Animation anim = AnimationUtils
                        .loadAnimation(mContext, R.anim.abc_slide_out_bottom);
                mSplitView.startAnimation(anim);
            }
            mSplitView.setVisibility(View.GONE);
        }
    }

    boolean isShowHideAnimationEnabled() {
        return mShowHideAnimationEnabled;
    }

}
