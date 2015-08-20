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

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBar.OnNavigationListener;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.view.CollapsibleActionView;
import lewa.support.v7.internal.view.ActionBarPolicy;
import lewa.support.v7.internal.view.menu.ActionMenuItem;
import lewa.support.v7.internal.view.menu.ActionMenuPresenter;
import lewa.support.v7.internal.view.menu.ActionMenuView;
import lewa.support.v7.internal.view.menu.ListMenuPresenter;
import lewa.support.v7.internal.view.menu.MenuBuilder;
import lewa.support.v7.internal.view.menu.MenuItemImpl;
import lewa.support.v7.internal.view.menu.MenuPresenter;
import lewa.support.v7.internal.view.menu.MenuView;
import lewa.support.v7.internal.view.menu.SubMenuBuilder;
import lewa.support.v7.lewa.v5.LewaActionBarContainer;
import lewa.support.v7.lewa.v5.LewaActionMenuPresenter;
import android.support.v4.internal.view.SupportMenu;
import android.support.v4.internal.view.SupportMenuItem;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
///LEWA BEGIN
import android.annotation.LewaHook;
//import lewa.util.LewaUiUtil;
//import lewa.util.ColorUtils;
//import android.provider.Settings;
//import android.provider.Settings.SettingNotFoundException;
//import android.provider.Settings.System;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
//import android.annotation.LewaHook;

///LEWA END
/**
 * @hide
 */
public class ActionBarView extends AbsActionBarView {

    private static final String TAG = "ActionBarView";

    /**
     * Display options applied by default
     */
    public static final int DISPLAY_DEFAULT = 0;

    /**
     * Display options that require re-layout as opposed to a simple invalidate
     */
    private static final int DISPLAY_RELAYOUT_MASK =
            ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_USE_LOGO |
                    ActionBar.DISPLAY_HOME_AS_UP |
                    ActionBar.DISPLAY_SHOW_CUSTOM |
            ActionBar.DISPLAY_SHOW_TITLE ;

    private static final int DEFAULT_CUSTOM_GRAVITY = Gravity.LEFT | Gravity.CENTER_VERTICAL;

    private int mNavigationMode;
    private int mDisplayOptions = -1;
    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private Drawable mIcon;
    private Drawable mLogo;

    private Context mContext;
    private HomeView mHomeLayout;
    private HomeView mExpandedHomeLayout;
    private LinearLayout mTitleLayout;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private View mTitleUpView;

    private SpinnerICS mSpinner;
    private LinearLayout mListNavLayout;
    private ScrollingTabContainerView mTabScrollView;
    private View mCustomNavView;
    private ProgressBarICS mProgressView;
    private ProgressBarICS mIndeterminateProgressView;

    private int mProgressBarPadding;
    private int mItemPadding;

    private int mTitleStyleRes;
    private int mSubtitleStyleRes;
    private int mProgressStyle;
    private int mIndeterminateProgressStyle;

    private boolean mUserTitle;
    private boolean mIncludeTabs;
    private boolean mIsCollapsable;
    private boolean mIsCollapsed;

    private MenuBuilder mOptionsMenu;

    private ActionBarContextView mContextView;

    private ActionMenuItem mLogoNavItem;

    private SpinnerAdapter mSpinnerAdapter;
    private OnNavigationListener mCallback;

    private Runnable mTabSelector;

    private ExpandedActionViewMenuPresenter mExpandedMenuPresenter;
    View mExpandedActionView;

    Window.Callback mWindowCallback;
///LEWA BEGIN
    private final Rect mTempRect = new Rect();
    private int mMaxHomeSlop;
    private static final int MAX_HOME_SLOP = 32; // dp

	//@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
	private int mColorfulMode;

	//@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
	private static final int STATUSBAR_HEIGHT_DP = 25;

	//@LewaHook(LewaHook.LewaHookType.NEW_FIELD)
	protected int mStatusbarHeight;
///LEWA END
    private final AdapterViewICS.OnItemSelectedListener mNavItemSelectedListener =
            new AdapterViewICS.OnItemSelectedListener() {
                public void onItemSelected(AdapterViewICS<?> parent, View view, int position,
                        long id) {
                    if (mCallback != null) {
                        mCallback.onNavigationItemSelected(position, id);
                    }
                }

                public void onNothingSelected(AdapterViewICS<?> parent) {
                    // Do nothing
                }
            };

    private final OnClickListener mExpandedActionViewUpListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final MenuItemImpl item = mExpandedMenuPresenter.mCurrentExpandedItem;
            if (item != null) {
                item.collapseActionView();
            }
        }
    };

    private final OnClickListener mUpClickListener = new OnClickListener() {
        public void onClick(View v) {
            mWindowCallback.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, mLogoNavItem);
        }
    };

    public ActionBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

            mContext = context;

            // Background is always provided by the container.
            setBackgroundResource(0);

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionBar,
                    R.attr.actionBarStyle, 0);

            ApplicationInfo appInfo = context.getApplicationInfo();
            PackageManager pm = context.getPackageManager();
            mNavigationMode = a.getInt(R.styleable.ActionBar_navigationMode,
                    ActionBar.NAVIGATION_MODE_STANDARD);
            mTitle = a.getText(R.styleable.ActionBar_lewa_title);
            mSubtitle = a.getText(R.styleable.ActionBar_subtitle);
            mLogo = a.getDrawable(R.styleable.ActionBar_logo);
            if (mLogo == null) {
                if (Build.VERSION.SDK_INT >= 9) {
                    if (context instanceof Activity) {
                        try {
                            mLogo = pm.getActivityLogo(((Activity) context).getComponentName());
                        } catch (NameNotFoundException e) {
                            Log.e(TAG, "Activity component name not found!", e);
                        }
                    }
                    if (mLogo == null) {
                        mLogo = appInfo.loadLogo(pm);
                    }
                }
            }

            // TODO(trevorjohns): Should these use the android namespace
            mIcon = a.getDrawable(R.styleable.ActionBar_lewa_icon);
            if (mIcon == null) {
                if (context instanceof Activity) {
                    try {
                        mIcon = pm.getActivityIcon(((Activity) context).getComponentName());
                    } catch (NameNotFoundException e) {
                        Log.e(TAG, "Activity component name not found!", e);
                    }
                }
                if (mIcon == null) {
                    mIcon = appInfo.loadIcon(pm);
                }
            }

            final LayoutInflater inflater = LayoutInflater.from(context);

            final int homeResId = a.getResourceId(
                    R.styleable.ActionBar_homeLayout,
                    R.layout.abc_action_bar_home);

            mHomeLayout = (HomeView) inflater.inflate(homeResId, this, false);

            mExpandedHomeLayout = (HomeView) inflater.inflate(homeResId, this, false);
            mExpandedHomeLayout.setUp(true);
            mExpandedHomeLayout.setOnClickListener(mExpandedActionViewUpListener);
            mExpandedHomeLayout.setContentDescription(getResources().getText(
                    R.string.abc_action_bar_up_description));
        mExpandedHomeLayout.setEnabled(true);
        mExpandedHomeLayout.setFocusable(true);

            mTitleStyleRes = a.getResourceId(R.styleable.ActionBar_titleTextStyle, 0);
            mSubtitleStyleRes = a.getResourceId(R.styleable.ActionBar_subtitleTextStyle, 0);
            mProgressStyle = a.getResourceId(R.styleable.ActionBar_progressBarStyle, 0);
            mIndeterminateProgressStyle = a.getResourceId(
                    R.styleable.ActionBar_indeterminateProgressStyle, 0);

            mProgressBarPadding = a
                    .getDimensionPixelOffset(R.styleable.ActionBar_progressBarPadding, 0);
            mItemPadding = a.getDimensionPixelOffset(R.styleable.ActionBar_itemPadding, 0);
///LEWA BEGIN
		mStatusbarHeight = (int)context.getResources().getDimension(R.dimen.android_status_bar_height);
///LEWA END

            setDisplayOptions(a.getInt(R.styleable.ActionBar_displayOptions, DISPLAY_DEFAULT));

            final int customNavId = a.getResourceId(R.styleable.ActionBar_customNavigationLayout, 0);
            if (customNavId != 0) {
                mCustomNavView = (View) inflater.inflate(customNavId, this, false);
                mNavigationMode = ActionBar.NAVIGATION_MODE_STANDARD;
                setDisplayOptions(mDisplayOptions | ActionBar.DISPLAY_SHOW_CUSTOM);
            }

            mContentHeight = a.getLayoutDimension(R.styleable.ActionBar_height, 0);
//            mContentHeight = Math.max(mContentHeight,
//                    r.getDimensionPixelSize(R.dimen.abc_action_bar_stacked_max_height));
            mContentHeight = ActionBarPolicy.get(mContext).getTabContainerHeight();
            a.recycle();
            mLogoNavItem = new ActionMenuItem(context, 0, android.R.id.home, 0, 0, mTitle);
            mHomeLayout.setOnClickListener(mUpClickListener);
            mHomeLayout.setClickable(true);
            mHomeLayout.setFocusable(true);
///LEWA BEGIN
        mMaxHomeSlop =
                (int) (MAX_HOME_SLOP * context.getResources().getDisplayMetrics().density + 0.5f);
///LEWA END

///LEWA BEGIN
		Injector.initilize(context, this);
///LEWA END
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mTitleView = null;
        mSubtitleView = null;
        mTitleUpView = null;
        if (mTitleLayout != null && mTitleLayout.getParent() == this) {
            removeView(mTitleLayout);
        }
        mTitleLayout = null;
        if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
            initTitle();
        }

        if (mTabScrollView != null && mIncludeTabs) {
            ViewGroup.LayoutParams lp = mTabScrollView.getLayoutParams();
            if (lp != null) {
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.height = LayoutParams.FILL_PARENT;
            }
            mTabScrollView.setAllowCollapse(true);
        }

        if (mProgressView != null) {
            removeView(mProgressView);
            initProgress();
        }
        if (mIndeterminateProgressView != null) {
            removeView(mIndeterminateProgressView);
            initIndeterminateProgress();
        }
    }

    /**
     * Set the view callback used to invoke menu items; used for dispatching home button presses.
     *
     * @param cb View callback to dispatch to
     */
    public void setWindowCallback(Window.Callback cb) {
        mWindowCallback = cb;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mTabSelector);
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.hideOverflowMenu();
            mActionMenuPresenter.hideSubMenus();
        }
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void initProgress() {
        mProgressView = new ProgressBarICS(mContext, null, 0, mProgressStyle);
        mProgressView.setId(R.id.progress_horizontal);
        mProgressView.setMax(10000);
        mProgressView.setVisibility(GONE);
        addView(mProgressView);
    }

    public void initIndeterminateProgress() {
        mIndeterminateProgressView = new ProgressBarICS(mContext, null, 0,
                mIndeterminateProgressStyle);
        mIndeterminateProgressView.setId(R.id.progress_circular);
        mIndeterminateProgressView.setVisibility(GONE);
        addView(mIndeterminateProgressView);
    }

    @Override
    public void setSplitActionBar(boolean splitActionBar) {
        if (mSplitActionBar != splitActionBar) {
            if (mMenuView != null) {
                final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
                if (oldParent != null) {
                    oldParent.removeView(mMenuView);
                }
                if (splitActionBar) {
                    if (mSplitView != null) {
                        mSplitView.addView(mMenuView);
                    }
                    mMenuView.getLayoutParams().width = LayoutParams.FILL_PARENT;
                } else {
                    addView(mMenuView);
                    mMenuView.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
                }
                mMenuView.requestLayout();
            }
            if (mSplitView != null) {
                mSplitView.setVisibility(splitActionBar ? VISIBLE : GONE);
            }

            if (mActionMenuPresenter != null) {
                if (!splitActionBar) {
                    /// M: width limit may change when orientation changed
                    final ActionBarPolicy abp = ActionBarPolicy.get(mContext);
                    if (abp != null) {
                        mActionMenuPresenter.setWidthLimit(abp.getEmbeddedMenuWidthLimit(), false);
                    }
                    mActionMenuPresenter.setExpandedActionViewsExclusive(
                            getResources().getBoolean(
                                    R.bool.abc_action_bar_expanded_action_views_exclusive));
                } else {
                    mActionMenuPresenter.setExpandedActionViewsExclusive(false);
                    // Allow full screen width in split mode.
                    mActionMenuPresenter.setWidthLimit(
                            getContext().getResources().getDisplayMetrics().widthPixels, true);
                    // No limit to the item count; use whatever will fit.
                    mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
                }
            }
            super.setSplitActionBar(splitActionBar);
        }
    }

    public boolean isSplitActionBar() {
        return mSplitActionBar;
    }

    public boolean hasEmbeddedTabs() {
        return mIncludeTabs;
    }

    public void setEmbeddedTabView(ScrollingTabContainerView tabs) {
        if (mTabScrollView != null) {
            removeView(mTabScrollView);
        }
        mTabScrollView = tabs;
        mIncludeTabs = tabs != null;
        if (mIncludeTabs && mNavigationMode == ActionBar.NAVIGATION_MODE_TABS) {
            addView(mTabScrollView);
            ViewGroup.LayoutParams lp = mTabScrollView.getLayoutParams();
            lp.width = LayoutParams.WRAP_CONTENT;
            lp.height = LayoutParams.FILL_PARENT;
            tabs.setAllowCollapse(true);
        }
    }

    public void setCallback(OnNavigationListener callback) {
        mCallback = callback;
    }

///LEWA BEGIN
    //@LewaHook(LewaHook.LewaHookType.NEW_CLASS)
    static class Injector {
        static ActionMenuPresenter newMenuPresenter(Context context, boolean isSplitActionBar) {
            if (isSplitActionBar) {
                ActionMenuPresenter presenter = new LewaActionMenuPresenter(context);
                presenter.setReserveOverflow(true);
                return presenter;
            } else {
                return new ActionMenuPresenter(context);
            }
        }

        static int getTitleResouceId(Context context) {
            if (true) {
                return R.layout.abc_action_bar_title_item;
            } else {
                return R.layout.abc_action_bar_title_item;
            }
        }

        static void setTitleShadow(Context context, TextView textview) {
            if (true) {
                //textview.setShadowLayer(1F, 1F, 1F, 0x77000000);
            }
        }
        
        static int measureTitleView(View child, int availableWidth, int childSpecHeight,
            int spacing) {
            child.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY),
                    childSpecHeight);

            availableWidth -= child.getMeasuredWidth();
            availableWidth -= spacing;

            return Math.max(0, availableWidth);
        }

        static void resetTitleLayoutPadding (ActionBarView view)    {
            if (view != null && view.mTitleLayout != null) {
                if (!view.mSplitActionBar && view.mMenuView != null && view.mMenuView.getChildCount() != 0
                    || view.mCustomNavView != null && (view.mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0
                        && view.mCustomNavView.getVisibility() != View.GONE && view.mCustomNavView.getMeasuredWidth() != 0) {
                    view.mTitleLayout.setPadding(0, 0, 0, 0);
                } else {
                    view.mTitleLayout.setPadding(0, 0, dip2px(view.getContext(), 48), 0);
                }
            }
			
			setViewPadding(view.getContext(), view.mTitleLayout, view.mStatusbarHeight);
        }
        public static int dip2px(Context context, float dpValue) { 
            final float scale = context.getResources().getDisplayMetrics().density; 
            return (int) (dpValue * scale + 0.5f); 
         }
		static void setViewPadding(Context context, View v, int statusbarHeight) {
			if (true) {
				v.setPadding(v.getPaddingLeft(), statusbarHeight, v.getPaddingRight(), v.getPaddingBottom());
			}
		}

		static int resetCustomViewTop(Context context, View v, int statusbarHeight, int oriHeight) {
			if (v.getPaddingTop() < statusbarHeight) {
				oriHeight += STATUSBAR_HEIGHT_DP;
			}
			return oriHeight;
		}

		static void initilize(Context context, ActionBarView abv) {
			//if (LewaUiUtil.isV5Ui(context)) {
			//	try {
			//		abv.mColorfulMode = System.getInt(context.getContentResolver(), Settings.LEWA_COLORVIEW_MODE);
			//	} catch (SettingNotFoundException e) {
			//		e.printStackTrace();
			//	}
			//}

			//if (LewaUiUtil.isImmersiveStatusbar(context) && !LewaUiUtil.isActionBarOverlay(context)) {
				abv.mContentHeight += abv.mStatusbarHeight;
			//}
		}

		static void setTitleColor(Context context, ActionBarView abv) {
			/*if (LewaUiUtil.isV5Ui(context)) {
				if (abv.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_NONE) {
					return;
				}
				
				android.content.res.LewaColorfulResources.LewaColorfulStyle.ColorfulNode colorfulStyle =
	                ((android.content.res.LewaResources) context.getResources()).getColorfulResources().getColorfulStyle(context);

	            if (colorfulStyle != null) {
					abv.mTitleView.setTextColor(colorfulStyle.getFirstTextColor());
				}
			}*/
		}

		static void setSubTitleColor(Context context, ActionBarView abv) {
			/*if (LewaUiUtil.isV5Ui(context)) {
				if (abv.mColorfulMode == Settings.LEWA_COLORVIEW_MODE_NONE) {
					return;
				}
				
				android.content.res.LewaColorfulResources.LewaColorfulStyle.ColorfulNode colorfulStyle =
	                ((android.content.res.LewaResources) context.getResources()).getColorfulResources().getColorfulStyle(context);

	            if (colorfulStyle != null) {
					abv.mSubtitleView.setTextColor(colorfulStyle.getSecondTextColor());
					//textView.setAlpha(colorfulStyle.getTransparency());
				}
			}*/
		}

		static void resetUpImage(ActionBarView abv) {
			//if (abv.mColorfulMode != Settings.LEWA_COLORVIEW_MODE_NONE) {
				((ImageView)abv.mTitleUpView).setImageResource(R.drawable.actionbar_edit_icon_back);
			    ((ImageView)abv.mTitleUpView).setBackgroundColor(0);
			//}
		}

		static void relayoutTitleView(Context context, ActionBarView abv) {
			if (true) {
				if (abv.mTitleView != null && abv.mTitleView.getVisibility() != GONE) {
					int titleLeft = abv.mTitleLayout.getLeft();
					int screenWidth = abv.getWidth();
					int parentLeft = 0;
					int titleWidth = abv.mTitleView.getMeasuredWidth();
					if (abv.mTitleUpView != null && abv.mTitleUpView.getVisibility() != GONE) {
						parentLeft = abv.mTitleUpView.getMeasuredWidth();
					}

					int left = (screenWidth - abv.mTitleView.getMeasuredWidth()) / 2 - parentLeft - titleLeft;
	
					abv.mTitleView.layout(left, abv.mTitleView.getTop(), left + titleWidth, abv.mTitleView.getBottom());
				}

				if (abv.mSubtitleView != null && abv.mSubtitleView.getVisibility() != GONE) {
					int titleLeft = abv.mTitleLayout.getLeft();
					int screenWidth = abv.getWidth();
					int parentLeft = 0;
					int titleWidth = abv.mSubtitleView.getMeasuredWidth();
					if (abv.mTitleUpView != null && abv.mTitleUpView.getVisibility() != GONE) {
						parentLeft = abv.mTitleUpView.getMeasuredWidth();
					}

					int left = (screenWidth - abv.mSubtitleView.getMeasuredWidth()) / 2 - parentLeft - titleLeft;

					abv.mSubtitleView.layout(left, abv.mSubtitleView.getTop(), left + titleWidth, abv.mSubtitleView.getBottom());
				}
			}
		}
    }
///LEWA  END
    public void setMenu(SupportMenu menu, MenuPresenter.Callback cb) {
        if (menu == mOptionsMenu) {
            return;
        }

        if (mOptionsMenu != null) {
            mOptionsMenu.removeMenuPresenter(mActionMenuPresenter);
            mOptionsMenu.removeMenuPresenter(mExpandedMenuPresenter);
        }

        MenuBuilder builder = (MenuBuilder) menu;
        mOptionsMenu = builder;
        if (mMenuView != null) {
            final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
            if (oldParent != null) {
                oldParent.removeView(mMenuView);
            }
        }
        if (mActionMenuPresenter == null) {
///LEWA BEGIN
            mActionMenuPresenter = Injector.newMenuPresenter(mContext, mSplitActionBar);
///LEWA END
            mActionMenuPresenter.setCallback(cb);
            mActionMenuPresenter.setId(R.id.action_menu_presenter);
            mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
        }

        ActionMenuView menuView;
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.FILL_PARENT);
        if (!mSplitActionBar) {
            mActionMenuPresenter.setExpandedActionViewsExclusive(
                    getResources().getBoolean(
                            R.bool.abc_action_bar_expanded_action_views_exclusive));
            configPresenters(builder);
            menuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            menuView.initialize(builder);
            final ViewGroup oldParent = (ViewGroup) menuView.getParent();
            if (oldParent != null && oldParent != this) {
                oldParent.removeView(menuView);
            }
            addView(menuView, layoutParams);
        } else {
            mActionMenuPresenter.setExpandedActionViewsExclusive(false);
            // Allow full screen width in split mode.
            mActionMenuPresenter.setWidthLimit(
                    getContext().getResources().getDisplayMetrics().widthPixels, true);
            // No limit to the item count; use whatever will fit.
            mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);

            // Span the whole width
            layoutParams.width = LayoutParams.FILL_PARENT;
            configPresenters(builder);
            menuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            if (mSplitView != null) {
                final ViewGroup oldParent = (ViewGroup) menuView.getParent();
                if (oldParent != null && oldParent != mSplitView) {
                    oldParent.removeView(menuView);
                }
                menuView.setVisibility(getAnimatedVisibility());
                // LEWA ADD START
                if (true) {
                    // Add menu bar into ActionBar
/*                    if (mSplitView instanceof LewaActionBarContainer) {
                        ((LewaActionBarContainer) mSplitView)
                            .getActionMenuBar().addView(menuView, layoutParams);
                    } else {
                        mSplitView.addView(menuView, layoutParams);
                    }*/
                	
                } else {
                     mSplitView.addView(menuView, layoutParams);
                }
                // LEWA ADD END
            } else {
                // We'll add this later if we missed it this time.
                menuView.setLayoutParams(layoutParams);
            }
        }
        mMenuView = menuView;
        InjectorMenu.addActionOptionMenu(this, (MenuBuilder)menu, layoutParams);
    }

    private void configPresenters(MenuBuilder builder) {
        if (builder != null) {
            builder.addMenuPresenter(mActionMenuPresenter);
            builder.addMenuPresenter(mExpandedMenuPresenter);
        } else {
            mActionMenuPresenter.initForMenu(mContext, null);
            mExpandedMenuPresenter.initForMenu(mContext, null);
        }

        // Make sure the Presenter's View is updated
        mActionMenuPresenter.updateMenuView(true);
        mExpandedMenuPresenter.updateMenuView(true);
    }

    public boolean hasExpandedActionView() {
        return mExpandedMenuPresenter != null &&
                mExpandedMenuPresenter.mCurrentExpandedItem != null;
    }

    public void collapseActionView() {
        final MenuItemImpl item = mExpandedMenuPresenter == null ? null :
                mExpandedMenuPresenter.mCurrentExpandedItem;
        if (item != null) {
            item.collapseActionView();
        }
    }

    public void setCustomNavigationView(View view) {
        final boolean showCustom = (mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0;
        if (mCustomNavView != null && showCustom) {
            removeView(mCustomNavView);
        }
        mCustomNavView = view;
        if (mCustomNavView != null && showCustom) {
///LEWA BEGIN			
			//Injector.setViewPadding(getContext(), mCustomNavView, mStatusbarHeight);
///LEWA END			
            addView(mCustomNavView);
        }
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Set the action bar title. This will always replace or override window titles.
     *
     * @param title Title to set
     * @see #setWindowTitle(CharSequence)
     */
    public void setTitle(CharSequence title) {
        mUserTitle = true;
        setTitleImpl(title);
    }

    /**
     * Set the window title. A window title will always be replaced or overridden by a user title.
     *
     * @param title Title to set
     * @see #setTitle(CharSequence)
     */
    public void setWindowTitle(CharSequence title) {
        if (!mUserTitle) {
            setTitleImpl(title);
        }
    }

    private void setTitleImpl(CharSequence title) {

        mTitle = title;
        if (mTitleView != null) {
            mTitleView.setText(title);
            mTitleView.setTextColor(Color.WHITE);
            final boolean visible = mExpandedActionView == null &&
                    (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0 &&
                    (!TextUtils.isEmpty(mTitle) || !TextUtils.isEmpty(mSubtitle));
            mTitleLayout.setVisibility(visible ? VISIBLE : GONE);
        }
        if (mLogoNavItem != null) {
            mLogoNavItem.setTitle(title);
        }
    }

    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    public void setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        if (mSubtitleView != null) {
            mSubtitleView.setText(subtitle);
            mSubtitleView.setVisibility(subtitle != null ? VISIBLE : GONE);
            final boolean visible = mExpandedActionView == null &&
                    (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0 &&
                    (!TextUtils.isEmpty(mTitle) || !TextUtils.isEmpty(mSubtitle));
            mTitleLayout.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    public void setHomeButtonEnabled(boolean enable) {
        mHomeLayout.setEnabled(enable);
        mHomeLayout.setFocusable(enable);
        // Make sure the home button has an accurate content description for accessibility.
        if (!enable) {
            mHomeLayout.setContentDescription(null);
        } else if ((mDisplayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.abc_action_bar_up_description));
        } else {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.abc_action_bar_home_description));
        }
    }

    public void setDisplayOptions(int options) {
        final int flagsChanged = mDisplayOptions == -1 ? -1 : options ^ mDisplayOptions;
        mDisplayOptions = options;

        if ((flagsChanged & DISPLAY_RELAYOUT_MASK) != 0) {
            final boolean showHome = (options & ActionBar.DISPLAY_SHOW_HOME) != 0;
            final int vis = showHome && mExpandedActionView == null ? VISIBLE : GONE;
            mHomeLayout.setVisibility(vis);

            if ((flagsChanged & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
                final boolean setUp = (options & ActionBar.DISPLAY_HOME_AS_UP) != 0;
                mHomeLayout.setUp(setUp);

                // Showing home as up implicitly enables interaction with it.
                // In honeycomb it was always enabled, so make this transition
                // a bit easier for developers in the common case.
                // (It would be silly to show it as up without responding to it.)
                if (setUp) {
                    setHomeButtonEnabled(true);
                }
            }

            if ((flagsChanged & ActionBar.DISPLAY_USE_LOGO) != 0) {
                final boolean logoVis = mLogo != null
                        && (options & ActionBar.DISPLAY_USE_LOGO) != 0;
                mHomeLayout.setIcon(logoVis ? mLogo : mIcon);
            }

            if ((flagsChanged & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
                if ((options & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
                    initTitle();
                } else {
                    removeView(mTitleLayout);
                }
            }

            if (mTitleLayout != null && (flagsChanged &
                    (ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME)) != 0) {
                final boolean homeAsUp = (mDisplayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0;
                mTitleUpView.setVisibility(!showHome ? (homeAsUp ? VISIBLE : INVISIBLE) : GONE);
///LEWA BEGIN
                if (true) {
                    mTitleLayout.setEnabled(false);
                    mTitleLayout.setClickable(false);
                } else {
                    mTitleLayout.setEnabled(!showHome && homeAsUp);
                    mTitleLayout.setClickable(!showHome && homeAsUp);
                }
///LEWA END
            }

            if ((flagsChanged & ActionBar.DISPLAY_SHOW_CUSTOM) != 0 && mCustomNavView != null) {
                if ((options & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
                    addView(mCustomNavView);
                } else {
                    removeView(mCustomNavView);
                }
            }

            requestLayout();
        } else {
            invalidate();
        }

        // Make sure the home button has an accurate content description for accessibility.
        if (!mHomeLayout.isEnabled()) {
            mHomeLayout.setContentDescription(null);
        } else if ((options & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.abc_action_bar_up_description));
        } else {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.abc_action_bar_home_description));
        }
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
        if (icon != null &&
                ((mDisplayOptions & ActionBar.DISPLAY_USE_LOGO) == 0 || mLogo == null)) {
            mHomeLayout.setIcon(icon);
        }
        if (mExpandedActionView != null) {
            mExpandedHomeLayout.setIcon(mIcon.getConstantState().newDrawable(getResources()));
        }
    }

    public void setIcon(int resId) {
        setIcon(mContext.getResources().getDrawable(resId));
    }

    public void setLogo(Drawable logo) {
        mLogo = logo;
        if (logo != null && (mDisplayOptions & ActionBar.DISPLAY_USE_LOGO) != 0) {
            mHomeLayout.setIcon(logo);
        }
    }

    public void setLogo(int resId) {
        setLogo(mContext.getResources().getDrawable(resId));
    }

    public void setNavigationMode(int mode) {
        final int oldMode = mNavigationMode;
        if (mode != oldMode) {
            switch (oldMode) {
                case ActionBar.NAVIGATION_MODE_LIST:
                    if (mListNavLayout != null) {
                        removeView(mListNavLayout);
                    }
                    break;
                case ActionBar.NAVIGATION_MODE_TABS:
                    if (mTabScrollView != null && mIncludeTabs) {
                        removeView(mTabScrollView);
                    }
            }

            switch (mode) {
                case ActionBar.NAVIGATION_MODE_LIST:
                    if (mSpinner == null) {
                        mSpinner = new SpinnerICS(mContext, null,
                                R.attr.actionDropDownStyle);
                        mListNavLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(
                                R.layout.abc_action_bar_view_list_nav_layout, null);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
                        params.gravity = Gravity.CENTER;
                        mListNavLayout.addView(mSpinner, params);
                    }
                    if (mSpinner.getAdapter() != mSpinnerAdapter) {
                        mSpinner.setAdapter(mSpinnerAdapter);
                    }
                    mSpinner.setOnItemSelectedListener(mNavItemSelectedListener);
                    addView(mListNavLayout);
                    break;
                case ActionBar.NAVIGATION_MODE_TABS:
                    if (mTabScrollView != null && mIncludeTabs) {
                        addView(mTabScrollView);
                    }
                    break;
            }
            mNavigationMode = mode;
            requestLayout();
        }
    }

    public void setDropdownAdapter(SpinnerAdapter adapter) {
        mSpinnerAdapter = adapter;
        if (mSpinner != null) {
            mSpinner.setAdapter(adapter);
        }
    }

    public SpinnerAdapter getDropdownAdapter() {
        return mSpinnerAdapter;
    }

    public void setDropdownSelectedPosition(int position) {
        mSpinner.setSelection(position);
    }

    public int getDropdownSelectedPosition() {
        return mSpinner.getSelectedItemPosition();
    }

    public View getCustomNavigationView() {
        return mCustomNavView;
    }

    public int getNavigationMode() {
        return mNavigationMode;
    }

    public int getDisplayOptions() {
        return mDisplayOptions;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        // Used by custom nav views if they don't supply layout params. Everything else
        // added to an ActionBarView should have them already.
        return new ActionBar.LayoutParams(DEFAULT_CUSTOM_GRAVITY);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        addView(mHomeLayout);

        if (mCustomNavView != null && (mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
            final ViewParent parent = mCustomNavView.getParent();
            if (parent != this) {
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(mCustomNavView);
                }
                addView(mCustomNavView);
            }
        }
    }

    private void initTitle() {
        if (mTitleLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
///LEWA  BEGIN
            int resId = Injector.getTitleResouceId(getContext());
            mTitleLayout = (LinearLayout) inflater.inflate(resId, this, false);
///LEWA  END
            mTitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_title);
            mSubtitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_subtitle);
            mTitleUpView = (View) mTitleLayout.findViewById(R.id.up);

///LEWA  BEGIN
            if (true) {
                mTitleView.setClickable(false);
                mSubtitleView.setClickable(false);
                mTitleUpView.setOnClickListener(mUpClickListener);
				Injector.resetUpImage(this);
				mTitleUpView.setOnTouchListener(new InjectorTouchListener());
            } else {
                mTitleLayout.setOnClickListener(mUpClickListener);
            }
///LEWA  END

            if (mTitleStyleRes != 0) {
                mTitleView.setTextAppearance(mContext, mTitleStyleRes);
            }
///LEWA  BEGIN
            Injector.setTitleShadow(getContext(), mTitleView);

			Injector.setTitleColor(getContext(), this);
			Injector.setSubTitleColor(getContext(), this);
///LEWA  END
            if (mTitle != null) {
                mTitleView.setText(mTitle);
            }

            if (mSubtitleStyleRes != 0) {
                mSubtitleView.setTextAppearance(mContext, mSubtitleStyleRes);
            }
            if (mSubtitle != null) {
                mSubtitleView.setText(mSubtitle);
                mSubtitleView.setVisibility(VISIBLE);
            }

            final boolean homeAsUp = (mDisplayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0;
            final boolean showHome = (mDisplayOptions & ActionBar.DISPLAY_SHOW_HOME) != 0;
            final boolean showTitleUp = !showHome;
            mTitleUpView.setVisibility(!showHome ? (homeAsUp ? VISIBLE : INVISIBLE) : GONE);
            ///LEWA BEGIN
            if (true) {
                mTitleLayout.setEnabled(false);
                mTitleLayout.setClickable(false);
            } else {
                mTitleLayout.setEnabled(homeAsUp && showTitleUp);
                mTitleLayout.setClickable(homeAsUp && showTitleUp);
            }
///LEWA END
        }

        addView(mTitleLayout);
        if (mExpandedActionView != null ||
                (TextUtils.isEmpty(mTitle) && TextUtils.isEmpty(mSubtitle))) {
            // Don't show while in expanded mode or with empty text
            mTitleLayout.setVisibility(GONE);
        } else {
            mTitleLayout.setVisibility(VISIBLE);
        }
    }

    public void setContextView(ActionBarContextView view) {
        mContextView = view;
    }

    public void setCollapsable(boolean collapsable) {
        mIsCollapsable = collapsable;
    }

    public boolean isCollapsed() {
        return mIsCollapsed;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int childCount = getChildCount();
        if (mIsCollapsable) {
            int visibleChildren = 0;
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE &&
                        !(child == mMenuView && mMenuView.getChildCount() == 0)) {
                    visibleChildren++;
                }
            }

            if (visibleChildren == 0) {
                // No size for an empty action bar when collapsable.
                setMeasuredDimension(0, 0);
                mIsCollapsed = true;
                return;
            }
        }
        mIsCollapsed = false;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_width=\"MATCH_PARENT\" (or fill_parent)");
        }

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }

        int contentWidth = MeasureSpec.getSize(widthMeasureSpec);

        int maxHeight = mContentHeight > 0 ?
                mContentHeight : MeasureSpec.getSize(heightMeasureSpec);

        final int verticalPadding = getPaddingTop() + getPaddingBottom();
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int height = maxHeight - verticalPadding;
        final int childSpecHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

        int availableWidth = contentWidth - paddingLeft - paddingRight;
        int leftOfCenter = availableWidth / 2;
        int rightOfCenter = leftOfCenter;

        HomeView homeLayout = mExpandedActionView != null ? mExpandedHomeLayout : mHomeLayout;

        if (homeLayout.getVisibility() != GONE) {
            final ViewGroup.LayoutParams lp = homeLayout.getLayoutParams();
            int homeWidthSpec;
            if (lp.width < 0) {
                homeWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
            } else {
                homeWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }
            homeLayout.measure(homeWidthSpec,
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            final int homeWidth = homeLayout.getMeasuredWidth() + homeLayout.getLeftOffset();
            availableWidth = Math.max(0, availableWidth - homeWidth);
            leftOfCenter = Math.max(0, availableWidth - homeWidth);
        }

        if (mMenuView != null && mMenuView.getParent() == this) {
            availableWidth = measureChildView(mMenuView, availableWidth,
                    childSpecHeight, 0);
            rightOfCenter = Math.max(0, rightOfCenter - mMenuView.getMeasuredWidth());
        }

        if (mIndeterminateProgressView != null &&
                mIndeterminateProgressView.getVisibility() != GONE) {
            availableWidth = measureChildView(mIndeterminateProgressView, availableWidth,
                    childSpecHeight, 0);
            rightOfCenter = Math.max(0,
                    rightOfCenter - mIndeterminateProgressView.getMeasuredWidth());
        }

        final boolean showTitle = mTitleLayout != null && mTitleLayout.getVisibility() != GONE &&
                (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0;

        if (mExpandedActionView == null) {
            switch (mNavigationMode) {
                case ActionBar.NAVIGATION_MODE_LIST:
                    if (mListNavLayout != null) {
                        final int itemPaddingSize = showTitle ? mItemPadding * 2 : mItemPadding;
                        availableWidth = Math.max(0, availableWidth - itemPaddingSize);
                        leftOfCenter = Math.max(0, leftOfCenter - itemPaddingSize);
                        mListNavLayout.measure(
                                MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                        final int listNavWidth = mListNavLayout.getMeasuredWidth();
                        availableWidth = Math.max(0, availableWidth - listNavWidth);
                        leftOfCenter = Math.max(0, leftOfCenter - listNavWidth);
                    }
                    break;
                case ActionBar.NAVIGATION_MODE_TABS:
                    if (mTabScrollView != null) {
                        final int itemPaddingSize = showTitle ? mItemPadding * 2 : mItemPadding;
                        availableWidth = Math.max(0, availableWidth - itemPaddingSize);
                        leftOfCenter = Math.max(0, leftOfCenter - itemPaddingSize);
                        mTabScrollView.measure(
                                MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                        final int tabWidth = mTabScrollView.getMeasuredWidth();
                        availableWidth = Math.max(0, availableWidth - tabWidth);
                        leftOfCenter = Math.max(0, leftOfCenter - tabWidth);
                    }
                    break;
            }
        }

        View customView = null;
        if (mExpandedActionView != null) {
            customView = mExpandedActionView;
        } else if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0 &&
                mCustomNavView != null) {
            customView = mCustomNavView;
        }

        if (customView != null) {
            final ViewGroup.LayoutParams lp = generateLayoutParams(customView.getLayoutParams());
            final ActionBar.LayoutParams ablp = lp instanceof ActionBar.LayoutParams ?
                    (ActionBar.LayoutParams) lp : null;

            int horizontalMargin = 0;
            int verticalMargin = 0;
            if (ablp != null) {
                horizontalMargin = ablp.leftMargin + ablp.rightMargin;
                verticalMargin = ablp.topMargin + ablp.bottomMargin;
            }

            // If the action bar is wrapping to its content height, don't allow a custom
            // view to FILL_PARENT.
            int customNavHeightMode;
            if (mContentHeight <= 0) {
                customNavHeightMode = MeasureSpec.AT_MOST;
            } else {
                customNavHeightMode = lp.height != LayoutParams.WRAP_CONTENT ?
                        MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            }
            final int customNavHeight = Math.max(0,
                    (lp.height >= 0 ? Math.min(lp.height, height) : height) - verticalMargin);

            final int customNavWidthMode = lp.width != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            int customNavWidth = Math.max(0,
                    (lp.width >= 0 ? Math.min(lp.width, availableWidth) : availableWidth)
                            - horizontalMargin);
            final int hgrav = (ablp != null ? ablp.gravity : DEFAULT_CUSTOM_GRAVITY) &
                    Gravity.HORIZONTAL_GRAVITY_MASK;

            // Centering a custom view is treated specially; we try to center within the whole
            // action bar rather than in the available space.
            if (hgrav == Gravity.CENTER_HORIZONTAL && lp.width == LayoutParams.FILL_PARENT) {
                customNavWidth = Math.min(leftOfCenter, rightOfCenter) * 2;
            }

            customView.measure(
                    MeasureSpec.makeMeasureSpec(customNavWidth, customNavWidthMode),
                    MeasureSpec.makeMeasureSpec(customNavHeight, customNavHeightMode));
            availableWidth -= horizontalMargin + customView.getMeasuredWidth();
        }

        if (mExpandedActionView == null && showTitle) {
///LEWA  BEGIN
            if (true) {
                Injector.resetTitleLayoutPadding(this);
                availableWidth = Injector.measureTitleView(mTitleLayout, availableWidth,
                    MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY), 0);
            } else {
                availableWidth = measureChildView(mTitleLayout, availableWidth,
                    MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY), 0);
            }
///LEWA  END

            leftOfCenter = Math.max(0, leftOfCenter - mTitleLayout.getMeasuredWidth());
        }

        if (mContentHeight <= 0) {
            int measuredHeight = 0;
            for (int i = 0; i < childCount; i++) {
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

        if (mContextView != null) {
            mContextView.setContentHeight(getMeasuredHeight());
        }

        if (mProgressView != null && mProgressView.getVisibility() != GONE) {
            mProgressView.measure(MeasureSpec.makeMeasureSpec(
                    contentWidth - mProgressBarPadding * 2, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();

        if (contentHeight <= 0) {
            // Nothing to do if we can't see anything.
            return;
        }

        HomeView homeLayout = mExpandedActionView != null ? mExpandedHomeLayout : mHomeLayout;
        if (homeLayout.getVisibility() != GONE) {
            final int leftOffset = homeLayout.getLeftOffset();
            x += positionChild(homeLayout, x + leftOffset, y, contentHeight) + leftOffset;
        }

        if (mExpandedActionView == null) {
            final boolean showTitle = mTitleLayout != null && mTitleLayout.getVisibility() != GONE
                    &&
                    (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0;
            if (showTitle) {
                x += positionChild(mTitleLayout, x, y, contentHeight);
            }

            switch (mNavigationMode) {
                case ActionBar.NAVIGATION_MODE_STANDARD:
                    break;
                case ActionBar.NAVIGATION_MODE_LIST:
                    if (mListNavLayout != null) {
                        if (showTitle) {
                            x += mItemPadding;
                        }
                        x += positionChild(mListNavLayout, x, y, contentHeight) + mItemPadding;
                    }
                    break;
                case ActionBar.NAVIGATION_MODE_TABS:
                    if (mTabScrollView != null) {
                        if (showTitle) {
                            x += mItemPadding;
                        }
                        x += positionChild(mTabScrollView, x, y, contentHeight) + mItemPadding;
                    }
                    break;
            }
        }

        int menuLeft = r - l - getPaddingRight();
        if (mMenuView != null && mMenuView.getParent() == this) {
            positionChildInverse(mMenuView, menuLeft, y, contentHeight);
            menuLeft -= mMenuView.getMeasuredWidth();
        }

        if (mIndeterminateProgressView != null &&
                mIndeterminateProgressView.getVisibility() != GONE) {
            positionChildInverse(mIndeterminateProgressView, menuLeft, y, contentHeight);
            menuLeft -= mIndeterminateProgressView.getMeasuredWidth();
        }

        View customView = null;
        if (mExpandedActionView != null) {
            customView = mExpandedActionView;
        } else if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0 &&
                mCustomNavView != null) {
            customView = mCustomNavView;
        }
        if (customView != null) {
        	
            ViewGroup.LayoutParams lp = customView.getLayoutParams();
            final ActionBar.LayoutParams ablp = lp instanceof ActionBar.LayoutParams ?
                    (ActionBar.LayoutParams) lp : null;
                    
            final int gravity = ablp != null ? ablp.gravity : DEFAULT_CUSTOM_GRAVITY;
            final int navWidth = customView.getMeasuredWidth();

            int topMargin = 0;
            int bottomMargin = 0;
            if (ablp != null) {
                x += ablp.leftMargin;
                menuLeft -= ablp.rightMargin;
                topMargin = ablp.topMargin;
                bottomMargin = ablp.bottomMargin;
            }

            int hgravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            // See if we actually have room to truly center; if not push against left or right.
            if (hgravity == Gravity.CENTER_HORIZONTAL) {
                final int centeredLeft = (getWidth() - navWidth) / 2;
                if (centeredLeft < x) {
                    hgravity = Gravity.LEFT;
                } else if (centeredLeft + navWidth > menuLeft) {
                    hgravity = Gravity.RIGHT;
                }
            } else if (gravity == -1) {
                hgravity = Gravity.LEFT;
            }

            int xpos = 0;
            switch (hgravity) {
                case Gravity.CENTER_HORIZONTAL:
                    xpos = (getWidth() - navWidth) / 2;
                    break;
                case Gravity.LEFT:
                    xpos = x;
                    break;
                case Gravity.RIGHT:
                    xpos = menuLeft - navWidth;
                    break;
            }

            int vgravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

            if (gravity == -1) {
                vgravity = Gravity.CENTER_VERTICAL;
            }

            int ypos = 0;
            switch (vgravity) {
                case Gravity.CENTER_VERTICAL:
                    final int paddedTop = getPaddingTop();
                    final int paddedBottom = getHeight() - getPaddingBottom();
                    ypos = ((paddedBottom - paddedTop + mStatusbarHeight) - customView.getMeasuredHeight()) / 2;
                    break;
                case Gravity.TOP:
                    ypos = getPaddingTop() + topMargin;
                    break;
                case Gravity.BOTTOM:
                    ypos = getHeight() - getPaddingBottom() - customView.getMeasuredHeight()
                            - bottomMargin;
                    break;
            }
            final int customWidth = customView.getMeasuredWidth();
			
///LEWA BEGIN
//            ypos = Injector.resetCustomViewTop(getContext(), customView, mStatusbarHeight, ypos);
///LEWA END
			
            customView.layout(xpos, ypos, xpos + customWidth,
                    ypos + customView.getMeasuredHeight());
            x += customWidth;
        }

        if (mProgressView != null) {
            mProgressView.bringToFront();
            final int halfProgressHeight = mProgressView.getMeasuredHeight() / 2;
            mProgressView.layout(mProgressBarPadding, -halfProgressHeight,
                    mProgressBarPadding + mProgressView.getMeasuredWidth(), halfProgressHeight);
        }

///LEWA BEGIN
		Injector.relayoutTitleView(getContext(), this);
///LEWA END
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ActionBar.LayoutParams(getContext(), attrs);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp == null) {
            lp = generateDefaultLayoutParams();
        }
        return lp;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);

        if (mExpandedMenuPresenter != null && mExpandedMenuPresenter.mCurrentExpandedItem != null) {
            state.expandedMenuItemId = mExpandedMenuPresenter.mCurrentExpandedItem.getItemId();
        }

        state.isOverflowOpen = isOverflowMenuShowing();

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable p) {
        SavedState state = (SavedState) p;

        super.onRestoreInstanceState(state.getSuperState());

        if (state.expandedMenuItemId != 0 &&
                mExpandedMenuPresenter != null && mOptionsMenu != null) {
            final SupportMenuItem item =
                    (SupportMenuItem) mOptionsMenu.findItem(state.expandedMenuItemId);
            if (item != null) {
                item.expandActionView();
            }
        }

        if (state.isOverflowOpen) {
            postShowOverflowMenu();
        }
    }

    public void setHomeAsUpIndicator(Drawable indicator) {
        mHomeLayout.setUpIndicator(indicator);
    }

    public void setHomeAsUpIndicator(int resId) {
        mHomeLayout.setUpIndicator(resId);
    }

    static class SavedState extends BaseSavedState {

        int expandedMenuItemId;
        boolean isOverflowOpen;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            expandedMenuItemId = in.readInt();
            isOverflowOpen = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(expandedMenuItemId);
            out.writeInt(isOverflowOpen ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public static class HomeView extends FrameLayout {
        private ImageView mUpView;
        private ImageView mIconView;
        private int mUpWidth;
        private int mUpIndicatorRes;
        private Drawable mDefaultUpIndicator;

        public HomeView(Context context) {
            this(context, null);
        }

        public HomeView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setUp(boolean isUp) {
            mUpView.setVisibility(isUp ? VISIBLE : GONE);
        }

        public void setIcon(Drawable icon) {
        	mIconView.setVisibility(View.GONE);
            mIconView.setImageDrawable(icon);
        }

        public void setUpIndicator(Drawable d) {
            mUpView.setImageDrawable(d != null ? d : mDefaultUpIndicator);
            mUpIndicatorRes = 0;
        }

        public void setUpIndicator(int resId) {
            mUpIndicatorRes = resId;
            mUpView.setImageDrawable(resId != 0 ? getResources().getDrawable(resId) : null);
        }

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            if (mUpIndicatorRes != 0) {
                // Reload for config change
                setUpIndicator(mUpIndicatorRes);
            }
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            final CharSequence cdesc = getContentDescription();
            if (!TextUtils.isEmpty(cdesc)) {
                event.getText().add(cdesc);
            }
            return true;
        }

        @Override
        protected void onFinishInflate() {
            mUpView = (ImageView) findViewById(R.id.up);
            mIconView = (ImageView) findViewById(R.id.home);
///LEWA BEGIN
			((ImageView)mUpView).setImageResource(R.drawable.actionbar_edit_icon_back);
            int statusbarHeight = (int)getContext().getResources().getDimension(R.dimen.android_status_bar_height);
            Injector.setViewPadding(getContext(), mUpView, statusbarHeight);
			if (true) {
				this.setOnTouchListener(new InjectorTouchListenerHomeView());
				mUpView.setBackgroundColor(0);
			}
///LEWA END
            mDefaultUpIndicator = mUpView.getDrawable();
        }
///LEWA BEGIN
        public int getLeftOffset() {
            return mUpView.getVisibility() == GONE ? mUpWidth/3 : 0;
        }
///LEWA END
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            measureChildWithMargins(mUpView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            final LayoutParams upLp = (LayoutParams) mUpView.getLayoutParams();
            mUpWidth = upLp.leftMargin + mUpView.getMeasuredWidth() + upLp.rightMargin;
            int width = mUpView.getVisibility() == GONE ? 0 : mUpWidth;
            int height = upLp.topMargin + mUpView.getMeasuredHeight() + upLp.bottomMargin;
            measureChildWithMargins(mIconView, widthMeasureSpec, width, heightMeasureSpec, 0);
            final LayoutParams iconLp = (LayoutParams) mIconView.getLayoutParams();
//            width += iconLp.leftMargin + mIconView.getMeasuredWidth() + iconLp.rightMargin;
//            height = Math.max(height,
//                    iconLp.topMargin + mIconView.getMeasuredHeight() + iconLp.bottomMargin);
            
            width += iconLp.leftMargin  + iconLp.rightMargin;
            height = Math.max(height,
                    iconLp.topMargin  + iconLp.bottomMargin);

            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            switch (widthMode) {
                case MeasureSpec.AT_MOST:
                    width = Math.min(width, widthSize);
                    break;
                case MeasureSpec.EXACTLY:
                    width = widthSize;
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    break;
            }
            switch (heightMode) {
                case MeasureSpec.AT_MOST:
                    height = Math.min(height, heightSize);
                    break;
                case MeasureSpec.EXACTLY:
                    height = heightSize;
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    break;
            }
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            final int vCenter = (b - t) / 2;
            int width = r - l;
            int upOffset = 0;
            if (mUpView.getVisibility() != GONE) {
                final LayoutParams upLp = (LayoutParams) mUpView.getLayoutParams();
                final int upHeight = mUpView.getMeasuredHeight();
                final int upWidth = mUpView.getMeasuredWidth();
                final int upTop = vCenter - upHeight / 2;
                mUpView.layout(0, upTop, upWidth, upTop + upHeight);
                upOffset = upLp.leftMargin + upWidth + upLp.rightMargin;
                width -= upOffset;
                l += upOffset;
            }
            final LayoutParams iconLp = (LayoutParams) mIconView.getLayoutParams();
            final int iconHeight = mIconView.getMeasuredHeight();
            final int iconWidth = mIconView.getMeasuredWidth();
            final int hCenter = (r - l) / 2;
            final int iconLeft = upOffset + Math.max(iconLp.leftMargin, hCenter - iconWidth / 2);
            final int iconTop = Math.max(iconLp.topMargin, vCenter - iconHeight / 2);
            mIconView.layout(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);
        }
    }

    private class ExpandedActionViewMenuPresenter implements MenuPresenter {

        MenuBuilder mMenu;
        MenuItemImpl mCurrentExpandedItem;

        @Override
        public void initForMenu(Context context, MenuBuilder menu) {
            // Clear the expanded action view when menus change.
            if (mMenu != null && mCurrentExpandedItem != null) {
                mMenu.collapseItemActionView(mCurrentExpandedItem);
            }
            mMenu = menu;
        }

        @Override
        public MenuView getMenuView(ViewGroup root) {
            return null;
        }

        @Override
        public void updateMenuView(boolean cleared) {
            // Make sure the expanded item we have is still there.
            if (mCurrentExpandedItem != null) {
                boolean found = false;

                if (mMenu != null) {
                    final int count = mMenu.size();
                    for (int i = 0; i < count; i++) {
                        final SupportMenuItem item = (SupportMenuItem) mMenu.getItem(i);
                        if (item == mCurrentExpandedItem) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    // The item we had expanded disappeared. Collapse.
                    collapseItemActionView(mMenu, mCurrentExpandedItem);
                }
            }
        }

        @Override
        public void setCallback(Callback cb) {
        }

        @Override
        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        @Override
        public boolean flagActionItems() {
            return false;
        }

        @Override
        public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
            mExpandedActionView = item.getActionView();
            mExpandedHomeLayout.setIcon(mIcon.getConstantState().newDrawable(getResources()));
            mCurrentExpandedItem = item;
            if (mExpandedActionView.getParent() != ActionBarView.this) {
                addView(mExpandedActionView);
            }
            if (mExpandedHomeLayout.getParent() != ActionBarView.this) {
                addView(mExpandedHomeLayout);
            }
            mHomeLayout.setVisibility(GONE);
            if (mTitleLayout != null) {
                mTitleLayout.setVisibility(GONE);
            }
            if (mTabScrollView != null) {
                mTabScrollView.setVisibility(GONE);
            }
            if (mSpinner != null) {
                mSpinner.setVisibility(GONE);
            }
            if (mCustomNavView != null) {
                mCustomNavView.setVisibility(GONE);
            }
            requestLayout();
            item.setActionViewExpanded(true);

            if (mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) mExpandedActionView).onActionViewExpanded();
            }

            return true;
        }

        @Override
        public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
            // Do this before detaching the actionview from the hierarchy, in case
            // it needs to dismiss the soft keyboard, etc.
            if (mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) mExpandedActionView).onActionViewCollapsed();
            }

            removeView(mExpandedActionView);
            removeView(mExpandedHomeLayout);
            mExpandedActionView = null;
            if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_HOME) != 0) {
                mHomeLayout.setVisibility(VISIBLE);
            }
            if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
                if (mTitleLayout == null) {
                    initTitle();
                } else {
                    mTitleLayout.setVisibility(VISIBLE);
                }
            }
            if (mTabScrollView != null && mNavigationMode == ActionBar.NAVIGATION_MODE_TABS) {
                mTabScrollView.setVisibility(VISIBLE);
            }
            if (mSpinner != null && mNavigationMode == ActionBar.NAVIGATION_MODE_LIST) {
                mSpinner.setVisibility(VISIBLE);
            }
            if (mCustomNavView != null && (mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
                mCustomNavView.setVisibility(VISIBLE);
            }
            mExpandedHomeLayout.setIcon(null);
            mCurrentExpandedItem = null;
            requestLayout();
            item.setActionViewExpanded(false);

            return true;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            return null;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
        }
    }

    // LEWA ADD START
    /** @hide */
    public ActionBarContainer getSplitView() {
        return mSplitView;
    }
    // LEWA ADD END

///LEWA BEGIN
	//@LewaHook(LewaHook.LewaHookType.NEW_CLASS)
	static class InjectorTouchListener implements OnTouchListener {
		private MotionEvent mCurrentDownEvent;
		public boolean onTouch(View v, MotionEvent event) {
			final int action = event.getAction();
			final float x = event.getX();
			final float y = event.getY();
	
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				if (mCurrentDownEvent != null) {
					mCurrentDownEvent.recycle();
				}
				mCurrentDownEvent = MotionEvent.obtain(event);
									
				((ImageView)v).setAlpha(0x80);
				break;
			case MotionEvent.ACTION_MOVE:
				final int deltaX = (int) (x - mCurrentDownEvent.getX());
				final int deltaY = (int) (y - mCurrentDownEvent.getY());
				int distance = (deltaX * deltaX) + (deltaY * deltaY);

				if (distance == 0) {
					break;
				}

				((ImageView)v).setAlpha(0xff);
				break;
			case MotionEvent.ACTION_UP:
				
				((ImageView)v).setAlpha(0xff);
				break;
			}
	
			return false;
		}
	}

	//@LewaHook(LewaHook.LewaHookType.NEW_CLASS)
	static class InjectorTouchListenerHomeView implements OnTouchListener {
		private MotionEvent mCurrentDownEvent;
		public boolean onTouch(View v, MotionEvent event) {
			final int action = event.getAction();
			final float x = event.getX();
			final float y = event.getY();
			ImageView backView = (ImageView)v.findViewById(R.id.up);
	
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				if (mCurrentDownEvent != null) {
					mCurrentDownEvent.recycle();
				}
				mCurrentDownEvent = MotionEvent.obtain(event);
									
				backView.setAlpha(0x80);
				break;
			case MotionEvent.ACTION_MOVE:
				final int deltaX = (int) (x - mCurrentDownEvent.getX());
				final int deltaY = (int) (y - mCurrentDownEvent.getY());
				int distance = (deltaX * deltaX) + (deltaY * deltaY);

				if (distance == 0) {
					break;
				}

				backView.setAlpha(0xff);
				break;
			case MotionEvent.ACTION_UP:
				backView.setAlpha(0xff);
				break;
			}
	
			return false;
		}
	}
///LEWA END
    public PanelFeatureState mOptionalMenuState;
    
    ///LEWA ADD START
    public final class PanelFeatureState {
        int background;

        /** The background when the panel spans the entire available width. */
        int fullBackground;

        public int windowAnimations;

        public ViewGroup parentView;

        /** The panel that we are actually showing. */
        public View shownPanelView;

        /** Use {@link #setMenu} to set this. */
        public MenuBuilder menu;

        ListMenuPresenter listMenuPresenter;

        /** Theme resource ID for list elements of the panel menu */
        int listPresenterTheme;

        public FrameLayout decorView;

        boolean isOpen;

        public boolean qwertyMode;

       public void setStyle(Context context) {
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

        public void setMenu(MenuBuilder menu) {
            if (menu == this.menu) return;

            if (this.menu != null) {
                this.menu.removeMenuPresenter(listMenuPresenter);
            }
            this.menu = menu;
            if (menu != null) {
                if (listMenuPresenter != null) menu.addMenuPresenter(listMenuPresenter);
            }
        }

        public MenuView getListMenuView(Context context, MenuPresenter.Callback cb) {
            if (menu == null) return null;

            if (listMenuPresenter == null) {
                listMenuPresenter = new ListMenuPresenter(context,
                        R.layout.abc_list_menu_item_layout);
                listMenuPresenter.setCallback(cb);
                listMenuPresenter.setId(R.id.list_menu_presenter);
                // LEWA ADD BEGIN
                // Set action flag for reset color of list menu.
                listMenuPresenter.setActionMode(false);
                // LEWA ADD END
                menu.addMenuPresenter(listMenuPresenter);
            }

            MenuView result = listMenuPresenter.getMenuView(decorView);

            return result;
        }

        public void clearMenuPresenters() {
            if (menu != null) {
                menu.removeMenuPresenter(listMenuPresenter);
            }

            listMenuPresenter = null;
        }
    }   
    /** @hide */
    public void toggleActionOptionMenu() {
        if (mSplitView == null) {
            return;
        }

        LewaActionBarContainer splitView = (LewaActionBarContainer) mSplitView;
        if (mSplitView.getVisibility() != View.VISIBLE) {
    		mSplitView.setVisibility(View.VISIBLE);
    	}

        if (mSplitView.getAlpha() < 1) {
            mSplitView.setAlpha(1);
        }

        if (splitView.isActionOptionMenuVisible()) {
            splitView.setActionOptionMenuVisibility(false);
        } else {
            splitView.setActionOptionMenuVisibility(true);
        }
    }
    
    public void updateActionMenuStyle() {
    	
    	InjectorMenu.updateActionMenuStyle(this, mOptionsMenu);
    }
    static class InjectorMenu {
        /*
         * Add option menu in action mode
         */
        static void addActionOptionMenu(final ActionBarView abcv,
                final MenuBuilder menu, final LayoutParams layoutParams) {
    			if (abcv.getSplitView() instanceof LewaActionBarContainer) {
    				((LewaActionBarContainer) abcv.getSplitView())
    						.getActionMenuBar().removeAllViews();
    				((LewaActionBarContainer) abcv.getSplitView())
    						.getActionMenuBar().addView(abcv.mMenuView,
    								layoutParams);

    				if (abcv.mActionMenuPresenter instanceof LewaActionMenuPresenter) {
    					final LewaActionBarContainer splitView = (LewaActionBarContainer) abcv.getSplitView();
    					InjectorMenu.initializeOptionMenu(abcv,
    					 splitView.getActionOptionMenuBar(), menu);

    					// Hide visible action menu bar while no visible items
//    					int itemSize = menu.lewaGetActionItems().size();
//    					if (itemSize <= 0) {
//    						splitView.setActionMenuVisibility(false);
//    					} else {
//    						splitView.setActionMenuVisibility(true);
//    					}

    					LewaActionMenuPresenter presenter = (LewaActionMenuPresenter) abcv.mActionMenuPresenter;
    					presenter.setOnActionMenuUpdateListener(null);
    					presenter.setOnActionMenuUpdateListener(new LewaActionMenuPresenter.OnActionMenuUpdateListener() {
    								public void onUpdated(MenuBuilder menu) {
    									if (menu != null) {
    									int itemSize = menu.getNonActionItems()
    											.size();
    									splitView.setNonActionItemsSize(itemSize);

    									// Hide visible action menu bar while no
    									// visible items
    									itemSize = menu.lewaGetActionItems().size();
    									splitView.setActionItemsSize(itemSize);
    									if (itemSize <= 0) {
    										splitView
    												.setActionMenuVisibility(false);
    									} else {
    										splitView
    												.setActionMenuVisibility(true);
    									}
    									}
    								}
    							});
    					presenter.setOnPerformClickListener(null);
    					presenter.setOnPerformClickListener(new ActionMenuPresenter.OnPerformClickListener() {
    								@Override
    								public void onPerformClick() {
    									abcv.toggleActionOptionMenu();
    								}
    							});
    				}
    			}
    		}
        
    		static void initializeOptionMenu(final ActionBarView abcv,
    				ViewGroup parentView, MenuBuilder menu) {
    			if (abcv.getSplitView() == null) {
    				return;
    			}


    			InjectorMenu.clearActionOptionMenu(abcv);
    			if (menu == null) {
    				return;
    			}
    			abcv.mOptionalMenuState = abcv.new PanelFeatureState();

    			abcv.mOptionalMenuState.decorView = new FrameLayout(
    					abcv.getContext());
    			abcv.mOptionalMenuState.setStyle(abcv.getContext());

    			// Init menu
    			abcv.mOptionalMenuState.setMenu(menu);
    			InjectorMenu.initializePanelContent(abcv, abcv.mOptionalMenuState);

    			// Add optional menu into menu bar.
    			ViewGroup.LayoutParams lp = abcv.mOptionalMenuState.shownPanelView
    					.getLayoutParams();
    			if (lp == null) {
    				lp = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
    						LayoutParams.WRAP_CONTENT);
    			}
    			parentView.addView(abcv.mOptionalMenuState.shownPanelView, lp);
//
    			InjectorMenu.updateActionMenuStyle(abcv, menu);
    		}
    		
    		/**
             * Update action menu style between Icon or Icon & Text mode.
             */
            static void updateActionMenuStyle(final ActionBarView oThis, MenuBuilder menu) {
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
                		ActionBar.LEWA_ACTION_MENU_STYLE_ICON);;
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
    		  static boolean initializePanelContent(final ActionBarView abcv, PanelFeatureState st) {
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
    		static void clearActionOptionMenu(final ActionBarView abcv) {
    			if (abcv.mOptionalMenuState != null) {
    				LewaActionBarContainer splitView = (LewaActionBarContainer) abcv.getSplitView();
    				splitView.getActionOptionMenuBar().removeAllViews();
    				splitView.setActionOptionMenuVisibility(false);

    				abcv.mOptionalMenuState.clearMenuPresenters();
    				abcv.mOptionalMenuState.setMenu(null);

    				abcv.mOptionalMenuState.parentView = null;
    				abcv.mOptionalMenuState.shownPanelView = null;
    				abcv.mOptionalMenuState.decorView = null;
    			}

    			abcv.mOptionalMenuState = null;
    		}
        }
}
