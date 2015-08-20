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

package lewa.support.v7.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBarImplBase;
import lewa.support.v7.appcompat.R;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;


/**
 * Special layout for the containing of an overlay action bar (and its content) to correctly handle
 * fitting system windows when the content has request that its layout ignore them.
 *
 * @hide
 */
public class ActionBarOverlayLayout extends FrameLayout {

    private int mActionBarHeight;
    private ActionBarImplBase mActionBar;
    private View mContent;
    private View mActionBarTop;
    private ActionBarContainer mContainerView;
    private ActionBarView mActionView;
    private View mActionBarBottom;
    private final Rect mZeroRect = new Rect(0, 0, 0, 0);

    static final int[] mActionBarSizeAttr = new int[]{
            R.attr.actionBarSize
    };

    public ActionBarOverlayLayout(Context context) {
        super(context);
        init(context);
    }

    public ActionBarOverlayLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(mActionBarSizeAttr);
        mActionBarHeight = ta.getDimensionPixelSize(0, 0);
        ta.recycle();
    }

    public void setActionBar(ActionBarImplBase impl) {
        mActionBar = impl;
    }

    private boolean applyInsets(View view, Rect insets, boolean left, boolean top,
            boolean bottom, boolean right) {
        boolean changed = false;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (left && lp.leftMargin != insets.left) {
            changed = true;
            lp.leftMargin = insets.left;
        }
        if (top && lp.topMargin != insets.top) {
            changed = true;
            lp.topMargin = insets.top;
        }
        if (right && lp.rightMargin != insets.right) {
            changed = true;
            lp.rightMargin = insets.right;
        }
        if (bottom && lp.bottomMargin != insets.bottom) {
            changed = true;
            lp.bottomMargin = insets.bottom;
        }
        return changed;
    }

       @Override
        protected final boolean fitSystemWindows( Rect insets) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Intentionally do not modify the bottom inset. For some reason, 
                // if the bottom inset is modified, window resizing stops working.
                // TODO: Figure out why.
    
                insets.left = 0;
                insets.top = 0;
                insets.right = 0;
            }
            boolean result = super.fitSystemWindows(insets);
            // this.setPadding(0,0, 0, insets.bottom);
            return result;
        }
//    @Override
//    protected boolean fitSystemWindows(Rect insets) {
//        pullChildren();
//
//        final int vis = getWindowSystemUiVisibility();
//        final boolean stable = (vis & SYSTEM_UI_FLAG_LAYOUT_STABLE) != 0;
//
//        // The top and bottom action bars are always within the content area.
//        boolean changed = applyInsets(mActionBarTop, insets, true, true, false, true);
//        if (mActionBarBottom != null) {
//            changed |= applyInsets(mActionBarBottom, insets, true, false, true, true);
//        }
//
//        // If the window has not requested system UI layout flags, we need to
//        // make sure its content is not being covered by system UI...  though it
//        // will still be covered by the action bar since they have requested it to
//        // overlay.
//        if ((vis & SYSTEM_UI_LAYOUT_FLAGS) == 0) {
//            changed |= applyInsets(mContent, insets, true, true, true, true);
//            // The insets are now consumed.
//            insets.set(0, 0, 0, 0);
//        } else {
//            changed |= applyInsets(mContent, mZeroRect, true, true, true, true);
//
/////LEWA BEGIN
//            //add for fix the bug:
//            //the target view at bottom won't show when ime window displayed and the window has immersive statusbar
//            changed |= Injector.reApplyInsets(getContext(), this, mContent, insets);
/////LEWA END
//        }
//
//
//        if (stable || mActionBarTop.getVisibility() == VISIBLE) {
//            // The action bar creates additional insets for its content to use.
//            insets.top += mActionBarHeight;
//        }
//
//        if (mActionBar != null && mActionBar.hasNonEmbeddedTabs()) {
//            View tabs = mContainerView.getTabContainer();
//            if (stable || (tabs != null && tabs.getVisibility() == VISIBLE)) {
//                // If tabs are not embedded, adjust insets to account for them.
//                insets.top += mActionBarHeight;
//            }
//        }
//
//        if (mActionView.isSplitActionBar()) {
//            if (stable || (mActionBarBottom != null
//                    && mActionBarBottom.getVisibility() == VISIBLE)) {
//                // If action bar is split, adjust buttom insets for it.
//                insets.bottom += mActionBarHeight;
//            }
//        }
//
//        if (changed) {
//            requestLayout();
//        }
//
//        return super.fitSystemWindows(insets);
//    }
//    void pullChildren() {
//        if (mContent == null) {
//            mContent = findViewById(R.id.action_bar_activity_content);
//            if (mContent == null) {
//                mContent = findViewById(android.R.id.content);
//            }
//            mActionBarTop = findViewById(R.id.top_action_bar);
//            mContainerView = (ActionBarContainer) findViewById(R.id.action_bar_container);
//            mActionView = (ActionBarView) findViewById(R.id.action_bar);
//            mActionBarBottom = findViewById(R.id.split_action_bar);
//        }
//    }
	//	@LewaHook(LewaHook.LewaHookType.NEW_CLASS)
    static class Injector {
		static boolean reApplyInsets(Context context, ActionBarOverlayLayout abol, View view, Rect insets) {
			boolean ret = false;
			if (true) {
				ret = abol.applyInsets(view, insets, false, false, true, false);
 	            // The insets are now consumed.
    	        insets.set(0, 0, 0, 0);
			} 

			return ret;
		}
    }
}
