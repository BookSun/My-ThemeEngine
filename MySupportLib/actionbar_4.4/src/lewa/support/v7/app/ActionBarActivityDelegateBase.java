/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.WindowCompat;
import android.util.TypedValue;
import android.view.*;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.internal.view.menu.ActionMenuPresenter;
import lewa.support.v7.internal.view.menu.ListMenuPresenter;
import lewa.support.v7.internal.view.menu.MenuBuilder;
import lewa.support.v7.internal.view.menu.MenuItemImpl;
import lewa.support.v7.internal.view.menu.MenuPresenter;
import lewa.support.v7.internal.view.menu.MenuView;
import lewa.support.v7.internal.view.menu.MenuWrapperFactory;
import lewa.support.v7.internal.widget.ActionBarContainer;
import lewa.support.v7.internal.widget.ActionBarContextView;
import lewa.support.v7.internal.widget.ActionBarView;
import lewa.support.v7.internal.widget.ProgressBarICS;
import lewa.support.v7.lewa.v5.LewaActionBarContainer;
import lewa.support.v7.lewa.v5.LewaActionMenuPresenter;
import lewa.support.v7.view.ActionMode;
import android.util.Log;
///LEWA  BEGIN
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.provider.Settings;
///LEWA  END
import android.widget.FrameLayout;
import android.widget.LinearLayout;

class ActionBarActivityDelegateBase extends ActionBarActivityDelegate implements
        MenuPresenter.Callback, MenuBuilder.Callback {
    private static final String TAG = "ActionBarActivityDelegateBase";

//    private static final int[] ACTION_BAR_DRAWABLE_TOGGLE_ATTRS = new int[] {
//            R.attr.homeAsUpIndicator
//    };

    private ActionBarView mActionBarView;
    private ListMenuPresenter mListMenuPresenter;
    private MenuBuilder mMenu;

    private ActionMode mActionMode;
    ActionBarContextView mActionModeView;
    private LewaActionBarContainer splitActionBarView;

    // true if we have installed a window sub-decor layout.
    private boolean mSubDecorInstalled;

    private CharSequence mTitleToSet;

    // Used to keep track of Progress Bar Window features
    private boolean mFeatureProgress, mFeatureIndeterminateProgress;

    private boolean mInvalidateMenuPosted;
    private final boolean IS_IGNORE_HOME = true;
    private final Runnable mInvalidateMenuRunnable = new Runnable() {
        @Override
        public void run() {
            final MenuBuilder menu = createMenu();
            if (mActivity.superOnCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu) &&
                    mActivity.superOnPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, menu)) {
                setMenu(menu);
            } else {
                setMenu(null);
            }
            mInvalidateMenuPosted = false;
        }
    };

    ActionBarActivityDelegateBase(ActionBarActivity activity) {
        super(activity);
    }

    private List<String> getHomes() {
        List<String> names = new ArrayList<String>();
        try {
        	PackageManager packageManager = mActivity.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : resolveInfo) {
                names.add(ri.activityInfo.packageName);
                System.out.println(ri.activityInfo.packageName);
            }
        	
        }catch(Exception e) {
        	
        }
        
        return names;
    }
    
	public boolean isHome() {
		if (!IS_IGNORE_HOME) {
			try {

				ActivityManager mActivityManager = (ActivityManager) mActivity
						.getSystemService(Context.ACTIVITY_SERVICE);
				if (mActivityManager != null) {
					List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
					// boolean isLauncherClass =
					// rti.get(0).topActivity.getClassName()
					// .equals("com.lewa.launcher.Launcher");
					// Log.i(TAG,"isLauncherClass :"+rti.get(0).topActivity.getClassName());
					if (rti != null) {
						if (rti.size() != 0) {
							ComponentName cn = rti.get(0).topActivity;
							if (cn != null) {
								return getHomes().contains(cn.getPackageName());
							}

						}
					}

				}

			} catch (Exception e) {

			}
		}
		

		//
		return false;
	}
    @Override
    void onCreate(Bundle savedInstanceState) {
///LEWA ADD BEGIN FOR IMMERSIVE
        //mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 0x10000000);
        //mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
//        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    	if (!isHome()) {
    		mActivity.getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE); 
        	mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);  
    	} 
    
        
///LEWA ADD BEGIN FOR IMMERSIVE
        super.onCreate(savedInstanceState);
        Log.d(TAG, "splitActionBarView" + splitActionBarView);
       
    }
    
     void toggleActionMenuStyle(boolean actionMode) {
        ArrayList<MenuItemImpl> actionMenus = null;
        if (!actionMode) {
//            PanelFeatureState st = window.getPanelState(FEATURE_OPTIONS_PANEL, false);
//            if (window.isDestroyed() || (st == null || st.menu == null)) {
//                return;
//            }
//            actionMenus = st.menu.lewaGetVisibleItems();
        	if (mMenu != null) {
        		 actionMenus = mMenu.lewaGetVisibleItems();
        	}
        } else {
        	if (mActionMode != null) {
        		 MenuBuilder menu = (MenuBuilder) mActionMode.getMenu();
                 if (menu != null) {
                     actionMenus = menu.lewaGetVisibleItems();
                 }
        	}
           
        }

        if (actionMenus == null) {
            return;
        }

        MenuItemImpl item = null;
        int actionFlag = 0;
        int menuStyle = -1;
        for (int i = 0; i < actionMenus.size(); i++) {
            item = actionMenus.get(i);
            actionFlag = item.getShowAsAction();
            if (actionFlag == MenuItem.SHOW_AS_ACTION_NEVER) {
                continue;
            }

            if (item.showsTextAsAction()) {
                menuStyle = ActionBar.LEWA_ACTION_MENU_STYLE_ICON;
                actionFlag ^= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
                item.setShowAsAction(actionFlag);
            } else {
                menuStyle = ActionBar.LEWA_ACTION_MENU_STYLE_ICON_WITH_TEXT;
                actionFlag |= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
                item.setShowAsAction(actionFlag);
            }

            saveActionMenuStyle(menuStyle);
        }
    }
     
      void saveActionMenuStyle(int menuStyle) {
         if (menuStyle == -1) {
             return;
         }

         Settings.System.putInt(mActivity.getContentResolver(), ActionBar.LEWA_ACTION_MENU_STYLE, menuStyle);
     }
    @Override
    public ActionBar createSupportActionBar() {
        ensureSubDecor();
        return new ActionBarImplBase(mActivity, mActivity);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // If this is called before sub-decor is installed, ActionBar will not
        // be properly initialized.
        if (mHasActionBar && mSubDecorInstalled) {
            // Note: The action bar will need to access
            // view changes from superclass.
            ActionBarImplBase actionBar = (ActionBarImplBase) getSupportActionBar();
            actionBar.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onStop() {
        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(false);
        }
    }

    @Override
    public void onPostResume() {
        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(true);
        }
      if (mActionBarView != null) {//update
    	     mActionBarView.updateActionMenuStyle();
             mActionBarView.requestLayout();
         }
      
    }

    @Override
    public void setContentView(View v) {
        ensureSubDecor();
        if (mHasActionBar) {
            ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
            contentParent.removeAllViews();
            contentParent.addView(v);
        } else {
            mActivity.superSetContentView(v);
        }
        mActivity.onSupportContentChanged();
    }

    @Override
    public void setContentView(int resId) {
        ensureSubDecor();
        if (mHasActionBar) {
            ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
            contentParent.removeAllViews();
            mActivity.getLayoutInflater().inflate(resId, contentParent);
        } else {
            mActivity.superSetContentView(resId);
        }
        mActivity.onSupportContentChanged();
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        if (mHasActionBar) {
            ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
            contentParent.removeAllViews();
            contentParent.addView(v, lp);
        } else {
            mActivity.superSetContentView(v, lp);
        }
        mActivity.onSupportContentChanged();
    }

    @Override
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        if (mHasActionBar) {
            ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
            contentParent.addView(v, lp);
        } else {
            mActivity.superSetContentView(v, lp);
        }
        mActivity.onSupportContentChanged();
    }

    @Override
    public void onContentChanged() {
        // Ignore all calls to this method as we call onSupportContentChanged manually above
    }

    final void ensureSubDecor() {
        Log.d("simply","mHasActionBar:"+mHasActionBar+",mSubDecor:"+mSubDecorInstalled+
                ",mOverlayActionBar:"+mOverlayActionBar);
        Window window = mActivity.getWindow();

        // Initializing the window decor can change window feature flags.
        // Make sure that we have the correct set before performing the test below.
        window.getDecorView();

        if (mHasActionBar && !mSubDecorInstalled) {
            //Add by Fan.Yang
/*            *//**
             * This needs some explanation. As we can not use the android:theme attribute
             * pre-L, we emulate it by manually creating a LayoutInflater using a
             * ContextThemeWrapper pointing to actionBarTheme.
             *//*
            TypedValue outValue = new TypedValue();
            mActivity.getTheme().resolveAttribute(R.attr.actionBarTheme, outValue, true);

            Context themedContext;
            if (outValue.resourceId != 0) {
                themedContext = new ContextThemeWrapper(mActivity, outValue.resourceId);
            } else {
                themedContext = mActivity;
            }*/
            if (mOverlayActionBar) {
                mActivity.superSetContentView(R.layout.abc_action_bar_decor_overlay);
            } else {
                mActivity.superSetContentView(R.layout.abc_action_bar_decor);
            }
            if (!isHome()) {
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS  
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);  
            mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN  
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);  
          
           
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);  
            mActivity.getWindow().setStatusBarColor(Color.TRANSPARENT);  
            mActivity.getWindow().setNavigationBarColor(Color.TRANSPARENT); 
            }
            mActionBarView = (ActionBarView) mActivity.findViewById(R.id.action_bar);
            mActionBarView.setWindowCallback(mActivity);
            mActionModeView = (ActionBarContextView) mActivity.findViewById(R.id.action_context_bar);
            splitActionBarView = (LewaActionBarContainer)mActivity.findViewById(R.id.split_action_bar);
            if (splitActionBarView != null) {
            	
            	splitActionBarView.setOnActionMenuDoubleClickListener(new LewaActionBarContainer.OnActionMenuDoubleClickListener() {
    				
    				@Override
    				public void onDoubleClick() {
    					// TODO Auto-generated method stub
    					Log.d(TAG,  "===ActionMenuDouble===");
    					toggleActionMenuStyle(false);
    					
    				}
    			});
            	splitActionBarView.setOnActionModeMenuDoubleClickListener(new LewaActionBarContainer.OnActionMenuDoubleClickListener() {
    				
    				@Override
    				public void onDoubleClick() {
    					// TODO Auto-generated method stub
    					Log.d(TAG,  "===ModeMenuDoubleClick===");
    					toggleActionMenuStyle(true);
    					
    				}
    			});
            	
            	splitActionBarView.setOnActionOptionMenuSlideListener(
                         new LewaActionBarContainer.OnActionSlideListener() {
                 public void onSlide(boolean isUp) {
                     if (isUp) {
                        
                    	 splitActionBarView.setActionOptionMenuVisibility(true);
                         
                     } else {
                    	 splitActionBarView.setActionOptionMenuVisibility(false);
                
                     }
                 }
             });
            	
            }

            /**
             * Progress Bars
             */
            if (mFeatureProgress) {
                mActionBarView.initProgress();
            }
            if (mFeatureIndeterminateProgress) {
                mActionBarView.initIndeterminateProgress();
            }

            /**
             * Split Action Bar
             */
            boolean splitWhenNarrow = UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW
                    .equals(getUiOptionsFromMetadata());
            boolean splitActionBar;

            if (splitWhenNarrow) {
                splitActionBar = mActivity.getResources()
                        .getBoolean(R.bool.abc_split_action_bar_is_narrow);
            } else {
                TypedArray a = mActivity.obtainStyledAttributes(R.styleable.Theme);
//                splitActionBar = a
//                        .getBoolean(R.styleable.Theme_windowActionBar, false);
                splitActionBar = true;
                a.recycle();
            }

            final ActionBarContainer splitView = (ActionBarContainer) mActivity.findViewById(
                    R.id.split_action_bar);
            if (splitView != null) {
                mActionBarView.setSplitView(splitView);
                mActionBarView.setSplitActionBar(splitActionBar);
                mActionBarView.setSplitWhenNarrow(splitWhenNarrow);

                final ActionBarContextView cab = (ActionBarContextView) mActivity.findViewById(
                        R.id.action_context_bar);
                cab.setSplitView(splitView);
                cab.setSplitActionBar(splitActionBar);
                cab.setSplitWhenNarrow(splitWhenNarrow);
            }

            // Change our content FrameLayout to use the android.R.id.content id.
            // Useful for fragments.
            View content = mActivity.findViewById(android.R.id.content);
            content.setId(View.NO_ID);
            View abcContent = mActivity.findViewById(R.id.action_bar_activity_content);
            abcContent.setId(android.R.id.content);

            // A title was set before we've install the decor so set it now.
            if (mTitleToSet != null) {
                mActionBarView.setWindowTitle(mTitleToSet);
                mTitleToSet  = null;
            }

            mSubDecorInstalled = true;
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public boolean supportRequestWindowFeature(int featureId) {
        switch (featureId) {
            case WindowCompat.FEATURE_ACTION_BAR:
                mHasActionBar = true;
                return true;
            case WindowCompat.FEATURE_ACTION_BAR_OVERLAY:
                mOverlayActionBar = true;
                return true;
            case Window.FEATURE_PROGRESS:
                mFeatureProgress = true;
                return true;
            case Window.FEATURE_INDETERMINATE_PROGRESS:
                mFeatureIndeterminateProgress = true;
                return true;
            default:
                return mActivity.requestWindowFeature(featureId);
        }
    }

    @Override
    public void onTitleChanged(CharSequence title) {
        if (mActionBarView != null) {
            mActionBarView.setWindowTitle(title);
        } else {
            mTitleToSet = title;
        }
    }

    @Override
    public View onCreatePanelView(int featureId) {
        View createdPanelView = null;
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            boolean show = true;
            MenuBuilder menu = mMenu;
            if (mActionMode == null) {
                // We only want to dispatch Activity/Fragment menu calls if there isn't
                // currently an action mode

                if (menu == null) {
                    // We don't have a menu created, so create one
                    menu = createMenu();
                    setMenu(menu);

                    // Make sure we're not dispatching item changes to presenters
                    menu.stopDispatchingItemsChanged();
                    // Dispatch onCreateOptionsMenu
                    show = mActivity.superOnCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
                }
                if (show) {
                    // Make sure we're not dispatching item changes to presenters
                    menu.stopDispatchingItemsChanged();
                    // Dispatch onPrepareOptionsMenu
                    show = mActivity.superOnPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, menu);
                }
            }

            if (show) {

                createdPanelView = (View) getListMenuView(mActivity, this);
                //createdPanelView = mActionBarView.mOptionalMenuState.shownPanelView;
                // Allow menu to start dispatching changes to presenters
                menu.startDispatchingItemsChanged();
            } else {
                // If the menu isn't being shown, we no longer need it
                setMenu(null);
            }
        }

        return createdPanelView;
    }
    
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) {
            return mActivity.superOnCreatePanelMenu(featureId, menu);
        }
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) {
            return mActivity.superOnPreparePanel(featureId, view, menu);
        }
        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            item = MenuWrapperFactory.createMenuItemWrapper(item);
        }
        if(mActionBarView != null && mActionBarView.getSplitView() != null ){
            mSplitView = (LewaActionBarContainer)(mActionBarView.getSplitView());
            if (mSplitView.isActionOptionMenuVisible()) {
//                mSplitView.setActionOptionMenuVisibility(false);
            	mSplitView.setActionOptionMenuVisibility(false, true, 80);
            }
        }
        return mActivity.superOnMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
    }

    @Override
    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(menu, true);
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        mActivity.closeOptionsMenu();
    }

    @Override
    public boolean onOpenSubMenu(MenuBuilder subMenu) {
        return false;
    }

    @Override
    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ActionMode callback can not be null.");
        }

        if (mActionMode != null) {
            mActionMode.finish();
        }

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);

        ActionBarImplBase ab = (ActionBarImplBase) getSupportActionBar();
        if (ab != null) {
            mActionMode = ab.startActionMode(wrappedCallback);
        }

        if (mActionMode != null) {
            mActivity.onSupportActionModeStarted(mActionMode);
        }
        return mActionMode;
    }

    @Override
    public void supportInvalidateOptionsMenu() {
	
        if (!mInvalidateMenuPosted) {
            mInvalidateMenuPosted = true;
            mActivity.getWindow().getDecorView().post(mInvalidateMenuRunnable);
        }
    }

    private MenuBuilder createMenu() {
        MenuBuilder menu = new MenuBuilder(getActionBarThemedContext());
        menu.setCallback(this);
        return menu;
    }

    private void reopenMenu(MenuBuilder menu, boolean toggleMenuMode) {
        if (mActionBarView != null && mActionBarView.isOverflowReserved()) {
            if (!mActionBarView.isOverflowMenuShowing() || !toggleMenuMode) {
                if (mActionBarView.getVisibility() == View.VISIBLE) {
                    mActionBarView.showOverflowMenu();
                }
            } else {
                mActionBarView.hideOverflowMenu();
            }
            return;
        }

        menu.close();
    }

    private MenuView getListMenuView(Context context, MenuPresenter.Callback cb) {
        if (mMenu == null) {
            return null;
        }

        if (mListMenuPresenter == null) {
            TypedArray a = context.obtainStyledAttributes(R.styleable.Theme);
//            final int listPresenterTheme = a.getResourceId(
//                    R.styleable.Theme_panelMenuListTheme,
//                    R.style.Theme_AppCompat_CompactMenu);
            final int 
            listPresenterTheme = R.style.Theme_AppCompat_CompactMenu;
            a.recycle();

            mListMenuPresenter = new ListMenuPresenter(context, 
                    R.layout.abc_list_menu_item_layout);
            mListMenuPresenter.setCallback(cb);
            mMenu.addMenuPresenter(mListMenuPresenter);
        } else {
            // Make sure we update the ListView
            mListMenuPresenter.updateMenuView(false);
        }

        return mListMenuPresenter.getMenuView(new FrameLayout(context));
    }

    private void setMenu(MenuBuilder menu) {
        if (menu == mMenu) {
            return;
        }

        if (mMenu != null) {
            mMenu.removeMenuPresenter(mListMenuPresenter);
        }
        mMenu = menu;

        if (menu != null && mListMenuPresenter != null) {
            // Only update list menu if there isn't an action mode menu
            menu.addMenuPresenter(mListMenuPresenter);
        }
        if (mActionBarView != null) {
            mActionBarView.setMenu(menu, this);
        }
    }
    protected LewaActionBarContainer mSplitView;
    @Override
    public boolean onBackPressed() {
        // Back cancels action modes first.
        if (mActionMode != null) {
        	if(mActionModeView != null && mActionModeView.mSplitView != null ){

        		mSplitView = (LewaActionBarContainer)(mActionModeView.mSplitView);
                if (mSplitView.isActionModeOptionMenuVisible()) {
                	mSplitView.setActionModeOptionMenuVisibility(false);
                } else {
                	mActionMode.finish();
                }
        		return true;
        	}
            mActionMode.finish();
            return true;
        }

    	if(mActionBarView != null && mActionBarView.getSplitView() != null ){
    		mSplitView = (LewaActionBarContainer)(mActionBarView.getSplitView());
            if (mSplitView.isActionOptionMenuVisible()) {
            	mSplitView.setActionOptionMenuVisibility(false);
            	return true;
            }	
    	}
        // Next collapse any expanded action views.
//        if (mActionBarView != null && mActionBarView.hasExpandedActionView()) {
//            mActionBarView.collapseActionView();
//            return true;
//        }

        return false;
    }

    @Override
    void setSupportProgressBarVisibility(boolean visible) {
        updateProgressBars(visible ? Window.PROGRESS_VISIBILITY_ON :
                Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        updateProgressBars(visible ? Window.PROGRESS_VISIBILITY_ON :
                Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    void setSupportProgressBarIndeterminate(boolean indeterminate) {
        updateProgressBars(indeterminate ? Window.PROGRESS_INDETERMINATE_ON
                : Window.PROGRESS_INDETERMINATE_OFF);
    }

    @Override
    void setSupportProgress(int progress) {
        updateProgressBars(Window.PROGRESS_START + progress);
    }

    @Override
    ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return new ActionBarDrawableToggleImpl();
    }

    @Override
    boolean onKeyUp(int keyCode, KeyEvent event) {
    	 switch (keyCode) { 
		case KeyEvent.KEYCODE_MENU: {
			// LEWA MODIFY BEGIN
			if (true && mActionBar != null
					&& mActionMode != null) {// action mode 
				mActionModeView.toggleActionModeOptionMenu();
			} else if (true && mActionBar != null) {
				//refresh menu
				if (splitActionBarView != null && mActivity != null) {
					LinearLayout splitView = splitActionBarView.getActionOptionMenuBar();
					if (splitView != null && splitView.getVisibility() != View.VISIBLE) {
					
//						mActivity.invalidateOptionsMenu();
						
						if (!mInvalidateMenuPosted) {
							mInvalidateMenuPosted = true;
							final MenuBuilder menu = createMenu();
				            if (mActivity.superOnCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu) &&
				                    mActivity.superOnPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, menu)) {
				                setMenu(menu);
				            } else {
				                setMenu(null);
				            }
//				            updateActionMenuCount(splitActionBarView, mMenu);
				            mInvalidateMenuPosted = false;
						} else {
							return true;
						}
					
//						 final MenuBuilder menu = createMenu();
//						if (mMenu != null) {
//							 if (mActivity.superOnPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, mMenu)) {
//									
//					                setMenu(mMenu);
//					            } else {
//		
//					                setMenu(null);
//					            }
//					            updateActionMenuCount(splitActionBarView, mMenu);
//						} 
						
						
					}
				}
				
		        	
		        	mActionBarView.toggleActionOptionMenu();

			}
			// LEWA MODIFY END

			return true;
		}
    	 }
    	return false;
    }
    
    void updateActionMenuCount(ActionBarContainer actionMenuView, Menu menu) {
        if (menu == null) {
            return;
        }

        if (!(menu instanceof MenuBuilder)) {
            return;
        }

        MenuBuilder menuBuilder = (MenuBuilder) menu;
       
        if (actionMenuView != null) {
            if (actionMenuView instanceof LewaActionBarContainer) {
                LewaActionBarContainer lewaActionBarContainer = (LewaActionBarContainer) actionMenuView;

                int itemSize = menuBuilder.getNonActionItems().size();
                lewaActionBarContainer.setNonActionItemsSize(itemSize);

                itemSize = menuBuilder.lewaGetActionItems().size();
                lewaActionBarContainer.setActionItemsSize(itemSize);
            }
        }
    }
  
    @Override
    boolean onKeyDown(int keyCode, KeyEvent event) {
        // On API v7-10 we need to manually call onKeyShortcut() as this is not called
        // from the Activity
        return super.onKeyDown(keyCode, event);
    }
    /**
     * Progress Bar function. Mostly extracted from PhoneWindow.java
     */
    private void updateProgressBars(int value) {
        ProgressBarICS circularProgressBar = getCircularProgressBar();
        ProgressBarICS horizontalProgressBar = getHorizontalProgressBar();

        if (value == Window.PROGRESS_VISIBILITY_ON) {
            if (mFeatureProgress) {
                int level = horizontalProgressBar.getProgress();
                int visibility = (horizontalProgressBar.isIndeterminate() || level < 10000) ?
                        View.VISIBLE : View.INVISIBLE;
                horizontalProgressBar.setVisibility(visibility);
            }
            if (mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(View.VISIBLE);
            }
        } else if (value == Window.PROGRESS_VISIBILITY_OFF) {
            if (mFeatureProgress) {
                horizontalProgressBar.setVisibility(View.GONE);
            }
            if (mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(View.GONE);
            }
        } else if (value == Window.PROGRESS_INDETERMINATE_ON) {
            horizontalProgressBar.setIndeterminate(true);
        } else if (value == Window.PROGRESS_INDETERMINATE_OFF) {
            horizontalProgressBar.setIndeterminate(false);
        } else if (Window.PROGRESS_START <= value && value <= Window.PROGRESS_END) {
            // We want to set the progress value before testing for visibility
            // so that when the progress bar becomes visible again, it has the
            // correct level.
            horizontalProgressBar.setProgress(value - Window.PROGRESS_START);

            if (value < Window.PROGRESS_END) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
            }
        }
    }

    private void showProgressBars(ProgressBarICS horizontalProgressBar,
            ProgressBarICS spinnyProgressBar) {
        if (mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == View.INVISIBLE) {
            spinnyProgressBar.setVisibility(View.VISIBLE);
        }
        // Only show the progress bars if the primary progress is not complete
        if (mFeatureProgress && horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBars(ProgressBarICS horizontalProgressBar,
            ProgressBarICS spinnyProgressBar) {
        if (mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == View.VISIBLE) {
            spinnyProgressBar.setVisibility(View.INVISIBLE);
        }
        if (mFeatureProgress && horizontalProgressBar.getVisibility() == View.VISIBLE) {
            horizontalProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private ProgressBarICS getCircularProgressBar() {
        ProgressBarICS pb = (ProgressBarICS) mActionBarView.findViewById(R.id.progress_circular);
        if (pb != null) {
            pb.setVisibility(View.INVISIBLE);
        }
        return pb;
    }

    private ProgressBarICS getHorizontalProgressBar() {
        ProgressBarICS pb = (ProgressBarICS) mActionBarView.findViewById(R.id.progress_horizontal);
        if (pb != null) {
            pb.setVisibility(View.INVISIBLE);
        }
        return pb;
    }

    /**
     * Clears out internal reference when the action mode is destroyed.
     */
    private class ActionModeCallbackWrapper implements ActionMode.Callback {
        private ActionMode.Callback mWrapped;

        public ActionModeCallbackWrapper(ActionMode.Callback wrapped) {
            mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mActivity.onSupportActionModeFinished(mode);
            mActionMode = null;
        }
    }

    public void closeActionMenu() {
    	if(mActionBarView != null && mActionBarView.getSplitView() != null ){
    		mSplitView = (LewaActionBarContainer)(mActionBarView.getSplitView());
            if (mSplitView.isActionOptionMenuVisible()) {
            	mSplitView.setActionOptionMenuVisibility(false);
            	
            }	
    	}
    	
    }
    private class ActionBarDrawableToggleImpl
            implements ActionBarDrawerToggle.Delegate {

        @Override
        public Drawable getThemeUpIndicator() {
//            final TypedArray a = mActivity.obtainStyledAttributes(ACTION_BAR_DRAWABLE_TOGGLE_ATTRS);
//            final Drawable result = a.getDrawable(0);
//            a.recycle();
//        	@drawable/abc_ic_ab_back_mtrl_am_alpha
        	if (mActivity != null) {
        		Drawable result = mActivity.getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        		return result;
        	}
        	
            return null;
        }

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
            if (mActionBarView != null) {
                mActionBarView.setHomeAsUpIndicator(upDrawable);
            }
        }

        @Override
        public void setActionBarDescription(int contentDescRes) {
            // No support for setting Action Bar content description
        }
    }

}
