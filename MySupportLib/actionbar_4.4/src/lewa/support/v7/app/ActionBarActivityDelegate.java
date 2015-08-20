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

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NavUtils;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.internal.view.SupportMenuInflater;
import lewa.support.v7.view.ActionMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

abstract class ActionBarActivityDelegate {

    static final String METADATA_UI_OPTIONS = "android.support.UI_OPTIONS";
    static final String UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW = "splitActionBarWhenNarrow";

    private static final String TAG = "ActionBarActivityDelegate";

    static ActionBarActivityDelegate createDelegate(ActionBarActivity activity) {
        final int version = Build.VERSION.SDK_INT;
/*        if (version >= Build.VERSION_CODES.JELLY_BEAN) {
            return new ActionBarActivityDelegateJB(activity);
        } else if (version >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new ActionBarActivityDelegateICS(activity);
        } else */
        //if (version >= Build.VERSION_CODES.HONEYCOMB) {
        //    return new ActionBarActivityDelegateHC(activity);
        //} else {
            return new ActionBarActivityDelegateBase(activity);
        //}
    }

    final ActionBarActivity mActivity;

    protected ActionBar mActionBar;
    private MenuInflater mMenuInflater;

    // true if this activity has an action bar.
    boolean mHasActionBar;
    // true if this activity's action bar overlays other activity content.
    boolean mOverlayActionBar;
    // true if this any action modes should overlay the activity content
    boolean mOverlayActionMode;
    // true if this activity is floating (e.g. Dialog)
    boolean mIsFloating;

    private boolean mEnableDefaultActionBarUp;

    ActionBarActivityDelegate(ActionBarActivity activity) {
        mActivity = activity;
    }

    abstract ActionBar createSupportActionBar();

    final ActionBar getSupportActionBar() {
        // The Action Bar should be lazily created as mHasActionBar or mOverlayActionBar
        // could change after onCreate
        if (mHasActionBar || mOverlayActionBar) {
            if (mActionBar == null) {
                mActionBar = createSupportActionBar();

                if (mEnableDefaultActionBarUp) {
                    mActionBar.setDisplayHomeAsUpEnabled(true);
                }
            }
        } else {
            // If we're not set to have a Action Bar, null it just in case it's been set
            mActionBar = null;
        }
        return mActionBar;
    }

    MenuInflater getMenuInflater() {
        if (mMenuInflater == null) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                mMenuInflater = new SupportMenuInflater(ab.getThemedContext());
            } else {
                mMenuInflater = new SupportMenuInflater(mActivity);
            }
        }
        return mMenuInflater;
    }

    void onCreate(Bundle savedInstanceState) {
        TypedArray a = mActivity.obtainStyledAttributes(R.styleable.Theme);

//        if (!a.hasValue(R.styleable.Theme_windowActionBar)) {
//            a.recycle();
//          throw new IllegalStateException(
//                    "You need to use a Theme.AppCompat theme (or descendant) with this activity.");
//        }

//        mHasActionBar = a.getBoolean(R.styleable.Theme_windowActionBar, false);
        mHasActionBar = true;
//        mOverlayActionBar = a.getBoolean(R.styleable.Theme_windowActionBarOverlay, false);
//        mOverlayActionBar = true;
//        mOverlayActionMode = a.getBoolean(R.styleable.Theme_windowActionModeOverlay, false);
        mOverlayActionMode = false;
        mIsFloating = a.getBoolean(R.styleable.Theme_android_windowIsFloating, false);
        mIsFloating = false;
        a.recycle();
       
        TypedArray abc = mActivity.obtainStyledAttributes(com.android.internal.R.styleable.Window);
        mOverlayActionBar = abc.getBoolean(com.android.internal.R.styleable.Window_windowActionBarOverlay, false);
//        mOverlayActionBar = false;
        abc.recycle();

        if (NavUtils.getParentActivityName(mActivity) != null) {
            if (mActionBar == null) {
                mEnableDefaultActionBarUp = true;
            } else {
                mActionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    abstract void onConfigurationChanged(Configuration newConfig);

    abstract void onStop();

    abstract void onPostResume();

    abstract void setContentView(View v);

    abstract void setContentView(int resId);

    abstract void setContentView(View v, ViewGroup.LayoutParams lp);

    abstract void addContentView(View v, ViewGroup.LayoutParams lp);

    abstract void onTitleChanged(CharSequence title);

    abstract void supportInvalidateOptionsMenu();

    abstract boolean supportRequestWindowFeature(int featureId);

    // Methods used to create and respond to options menu
    abstract View onCreatePanelView(int featureId);

    abstract boolean onPreparePanel(int featureId, View view, Menu menu);

    boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // Call straight through to onPrepareOptionsMenu, bypassing super.onPreparePanel().
            // This is because Activity.onPreparePanel() on <v4.1 calls menu.hasVisibleItems(),
            // which interferes with the initially invisible items.
            return mActivity.onPrepareOptionsMenu(menu);
        }
        return mActivity.superOnPrepareOptionsPanel(view, menu);
    }

    abstract boolean onCreatePanelMenu(int featureId, Menu menu);

    abstract boolean onMenuItemSelected(int featureId, MenuItem item);

    abstract boolean onBackPressed();

    abstract ActionMode startSupportActionMode(ActionMode.Callback callback);

    abstract void setSupportProgressBarVisibility(boolean visible);

    abstract void setSupportProgressBarIndeterminateVisibility(boolean visible);

    abstract void setSupportProgressBarIndeterminate(boolean indeterminate);

    abstract void setSupportProgress(int progress);
    boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }
    boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }
    abstract ActionBarDrawerToggle.Delegate getDrawerToggleDelegate();

    abstract void onContentChanged();

    protected final String getUiOptionsFromMetadata() {
        try {
            PackageManager pm = mActivity.getPackageManager();
            ActivityInfo info = pm.getActivityInfo(mActivity.getComponentName(),
                    PackageManager.GET_META_DATA);

            String uiOptions = null;
            if (info.metaData != null) {
                uiOptions = info.metaData.getString(METADATA_UI_OPTIONS);
            }
            return uiOptions;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getUiOptionsFromMetadata: Activity '" + mActivity.getClass()
                    .getSimpleName() + "' not in manifest");
            return null;
        }
    }
    
   public void closeActionMenu() {
    	
    }

    protected final Context getActionBarThemedContext() {
        Context context = mActivity;

        // If we have an action bar, initialize the menu with a context themed from it.
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            context = ab.getThemedContext();
        }
        return context;
    }
}
