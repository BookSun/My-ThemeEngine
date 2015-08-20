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

package lewa.support.v7.internal.view.menu;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.internal.text.AllCapsTransformationMethod;
import lewa.support.v7.internal.widget.CompatTextView;
import lewa.support.v7.widget.ActionMenuView;
import lewa.support.v7.widget.ListPopupWindow;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Locale;
///LEWA ADD BEGIN
import java.util.Hashtable;
import android.graphics.PorterDuff;
//import lewa.util.LewaUiUtil;
//import lewa.util.ColorUtils;
//import android.annotation.LewaHook;
//import android.content.res.LewaResources;
//import android.content.res.LewaColorfulResources.LewaColorfulStyle;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
///LEWA ADD END
/**
 * @hide
 */
public class ActionMenuItemView extends CompatTextView
        implements MenuView.ItemView, View.OnClickListener, View.OnLongClickListener,
        ActionMenuView.ActionMenuChildView {

    private static final String TAG = "ActionMenuItemView";

    private MenuItemImpl mItemData;
    private CharSequence mTitle;
    private Drawable mIcon;
    private MenuBuilder.ItemInvoker mItemInvoker;
    private ListPopupWindow.ForwardingListener mForwardingListener;
    private PopupCallback mPopupCallback;

    private boolean mAllowTextWithIcon;
    private boolean mExpandedFormat;
    private int mMinWidth;
    private int mSavedPaddingLeft;

    private static final int MAX_ICON_SIZE = 32; // dp
    // LEWA ADD BEGIN
    private static final int LEWA_MAX_ICON_SIZE = 24; // dp
    // LEAW ADD END
    private int mMaxIconSize;

    // LEWA ADD BEGIN
    private static final int MAX_ITEM_WIDTH = 64; // dips

    public static final int ACTION_MODE_ITEM_COLOR = 0xfffffffe;

    public static final int ACTION_MENU_ITEM_DISABLED_COLOR = 0xffc9c9c9;

    private boolean mIsV5UI;
    private int mColorfulColor;
    private Context mContext;
    private int mMaxItemWidth;
    //private LewaColorfulStyle.ColorfulNode mColorfulStyle;
    private Drawable mColorfulIcon;
    private int mActionModeColor;
    private static Hashtable<CharSequence, Drawable> mColorfulIcons = new Hashtable();
    // LEWA ADD END

    public ActionMenuItemView(Context context) {
        this(context, null);
    }

    public ActionMenuItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionMenuItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // LEWA ADD BEGIN
        mContext = context;
        // LEWA ADD END

        final Resources res = context.getResources();
        mAllowTextWithIcon = res.getBoolean(R.bool.abc_config_allowActionMenuItemTextWithIcon);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ActionMenuItemView, defStyle, 0);
        mMinWidth = a.getDimensionPixelSize(
                R.styleable.ActionMenuItemView_android_minWidth, 0);
        a.recycle();

        final float density = res.getDisplayMetrics().density;
        // LEWA MODIFY BEGIN
        mMaxIconSize = Injector.getMaxIconSize(this);
        // LEWA MODIFY END

        // LEWA ADD BEGIN
        mMaxItemWidth = (int)(MAX_ITEM_WIDTH * density);
        // LEWA ADD END
        mMaxIconSize = (int) (MAX_ICON_SIZE * density + 0.5f);

        setOnClickListener(this);
        setOnLongClickListener(this);

        setTransformationMethod(new AllCapsTransformationMethod(context));

        mSavedPaddingLeft = -1;
        // LEWA ADD BEGIN
        Injector.initColorfulStyle(this);
        // LEWA ADD END
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= 8) {
            super.onConfigurationChanged(newConfig);
        }

        mAllowTextWithIcon = getContext().getResources().getBoolean(
                R.bool.abc_config_allowActionMenuItemTextWithIcon);
        updateTextButtonVisibility();
    }

    @Override
    public void setPadding(int l, int t, int r, int b) {
        mSavedPaddingLeft = l;
        super.setPadding(l, t, r, b);
    }

    public MenuItemImpl getItemData() {
        return mItemData;
    }

    public void initialize(MenuItemImpl itemData, int menuType) {
        mItemData = itemData;

        setIcon(itemData.getIcon());
        setTitle(itemData.getTitleForItemView(this)); // Title only takes effect if there is no icon
        setId(itemData.getItemId());

        setVisibility(itemData.isVisible() ? View.VISIBLE : View.GONE);
        setEnabled(itemData.isEnabled());
        if (itemData.hasSubMenu()) {
            if (mForwardingListener == null) {
                mForwardingListener = new ActionMenuItemForwardingListener();
            }
        }
    }
    // LEWA ADD BEGIN
    /** @hide */
    //@LewaHook(LewaHook.LewaHookType.NEW_METHOD)
    public void initialize(MenuItemImpl itemData, int menuType, int color) {
        mItemData = itemData;

        mActionModeColor = color;

        setId(itemData.getItemId());
        setTitle(itemData.getTitleForItemView(this)); // Title only takes effect if there is no icon
        setIcon(itemData.getIcon(), color);

        this.setTextColor(color);

        setVisibility(itemData.isVisible() ? View.VISIBLE : View.GONE);
        setEnabled(itemData.isEnabled());
    }
    // LEWA ADD END
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mItemData.hasSubMenu() && mForwardingListener != null
                && mForwardingListener.onTouch(this, e)) {
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public void onClick(View v) {
        if (mItemInvoker != null) {
            mItemInvoker.invokeItem(mItemData);
        }
    }

    public void setItemInvoker(MenuBuilder.ItemInvoker invoker) {
        mItemInvoker = invoker;
    }

    public void setPopupCallback(PopupCallback popupCallback) {
        mPopupCallback = popupCallback;
    }

    public boolean prefersCondensedTitle() {
        return true;
    }

    public void setCheckable(boolean checkable) {
        // TODO Support checkable action items
    }

    public void setChecked(boolean checked) {
        // TODO Support checkable action items
    }

    public void setExpandedFormat(boolean expandedFormat) {
        if (mExpandedFormat != expandedFormat) {
            mExpandedFormat = expandedFormat;
            if (mItemData != null) {
                mItemData.actionFormatChanged();
            }
        }
    }

    private void updateTextButtonVisibility() {
        boolean visible = !TextUtils.isEmpty(mTitle);
        visible &= mIcon == null ||
                (mItemData.showsTextAsAction() && (mAllowTextWithIcon || mExpandedFormat));
        visible = true;
        setText(visible ? mTitle : null);
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
        if (icon != null) {
            // LEWA ADD BEGIN
            mColorfulIcon = null;
            mColorfulIcon = Injector.resetDrawableColor(this, icon);
            // LEWA ADD END
            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();
            if (width > mMaxIconSize) {
                final float scale = (float) mMaxIconSize / width;
                width = mMaxIconSize;
                height *= scale;
            }
            if (height > mMaxIconSize) {
                final float scale = (float) mMaxIconSize / height;
                height = mMaxIconSize;
                width *= scale;
            }
            // LEWA MODIFY BEGIN
            if (true) {
                mColorfulIcon.setBounds(0, 0, width, height);
            } else {
                icon.setBounds(0, 0, width, height);
            }
            // LEWA MODIFY END
        }
        // LEWA MODIFY BEGIN
        if (true) {
            // Set icon at top in Lewa OS
            setCompoundDrawables(null, mColorfulIcon, null, null);
        } else {
            setCompoundDrawables(icon, null, null, null);
        }
        // LEWA MODIFY END

        updateTextButtonVisibility();
    }

    // LEWA ADD BEGIN
    /** @hide */
    //@LewaHook(LewaHook.LewaHookType.NEW_METHOD)
    public void setIcon(Drawable icon, int color) {
        if (mIcon == icon) {
            return;
        }

        mIcon = null;
        mIcon = icon;
        if (icon != null) {
            // LEWA ADD BEGIN
            mColorfulIcon = null;
            mColorfulIcon = Injector.resetActionModeDrawableColor(this, icon, color);
            // LEWA ADD END

            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();
            if (width > mMaxIconSize) {
                final float scale = (float) mMaxIconSize / width;
                width = mMaxIconSize;
                height *= scale;
            }
            if (height > mMaxIconSize) {
                final float scale = (float) mMaxIconSize / height;
                height = mMaxIconSize;
                width *= scale;
            }
            // LEWA MODIFY BEGIN
            if (true) {
                mColorfulIcon.setBounds(0, 0, width, height);
            } else {
                icon.setBounds(0, 0, width, height);
            }
            // LEWA MODIFY END
        }
        // LEWA MODIFY BEGIN
        if (true) {
            // Set icon at top in Lewa OS
            setCompoundDrawables(null, mColorfulIcon, null, null);
        } else {
            setCompoundDrawables(icon, null, null, null);
        }
        // LEWA MODIFY END

        updateTextButtonVisibility();
    }
    // LEWA ADD END

    // LEWA ADD BEGIN
    /** @hide */
    //@LewaHook(LewaHook.LewaHookType.NEW_METHOD)
    public Drawable getIcon() {
        if (true) {
            return mColorfulIcon;
        }

        return mIcon;
    }
    // LEWA ADD END

    public boolean hasText() {
        return !TextUtils.isEmpty(getText());
    }

    public void setShortcut(boolean showShortcut, char shortcutKey) {
        // Action buttons don't show text for shortcut keys.
    }

    public void setTitle(CharSequence title) {
        mTitle = title;

        setContentDescription(mTitle);
        updateTextButtonVisibility();
    }

    public boolean showsIcon() {
        return true;
    }

    public boolean needsDividerBefore() {
        return hasText() && mItemData.getIcon() == null;
    }

    public boolean needsDividerAfter() {
        return hasText();
    }

    @Override
    public boolean onLongClick(View v) {
        if (hasText()) {
            // Don't show the cheat sheet for items that already show text.
            return false;
        }

        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        getLocationOnScreen(screenPos);
        getWindowVisibleDisplayFrame(displayFrame);

        final Context context = getContext();
        final int width = getWidth();
        ///LEWA MODIFY BEGIN
        final int height = Injector.getHeight(this);
        final int midy = Injector.measureMidy(this, screenPos[1], height);
        //final int height = getHeight();
        //final int midy = screenPos[1] + height / 2;
		///LEWA MODIFY END
        int referenceX = screenPos[0] + width / 2;
        if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            referenceX = screenWidth - referenceX; // mirror
        }
        Toast cheatSheet = Toast.makeText(context, mItemData.getTitle(), Toast.LENGTH_SHORT);
        if (midy < displayFrame.height()) {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP | GravityCompat.END, referenceX, height);
        } else {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
        }
        cheatSheet.show();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final boolean textVisible = hasText();
        if (textVisible && mSavedPaddingLeft >= 0) {
            super.setPadding(mSavedPaddingLeft, getPaddingTop(),
                    getPaddingRight(), getPaddingBottom());
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        // LEWA ADD BEGIN
        int oldMeasuredWidth = 0;
        if (true) {
            oldMeasuredWidth = mMaxItemWidth;
        } else {
            oldMeasuredWidth = getMeasuredWidth();
        }
        // LEWA ADD END

        final int targetWidth = widthMode == MeasureSpec.AT_MOST ? Math.min(widthSize, mMinWidth)
                : mMinWidth;

        if (widthMode != MeasureSpec.EXACTLY && mMinWidth > 0 && oldMeasuredWidth < targetWidth) {
            // Remeasure at exactly the minimum width.
            super.onMeasure(MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY),
                    heightMeasureSpec);
        }
        ///LEWA MODIFY BEGIN
        if (true) {
            if (mSavedPaddingLeft >= 0 && mColorfulIcon != null) {
                // TextView won't center compound drawables in both dimensions without
                // a little coercion. Pad in to center the icon after we've measured.
                final int w = mMaxItemWidth;
                final int dw = mColorfulIcon.getBounds().width();
                super.setPadding(mSavedPaddingLeft, getPaddingTop(), getPaddingRight(), getPaddingBottom());
            }
        } else {
            if (!textVisible && mIcon != null) {
                // TextView won't center compound drawables in both dimensions without
                // a little coercion. Pad in to center the icon after we've measured.
                final int w = getMeasuredWidth();
                final int dw = mIcon.getBounds().width();
                super.setPadding((w - dw) / 2, getPaddingTop(), getPaddingRight(), getPaddingBottom());
            }
        }
        ///LEWA MODIFY END
        ///LEWA ADD BEGIN
        //Injector.setIconColorFilter(this);
        ///LEWA ADD END
    }

    private class ActionMenuItemForwardingListener extends ListPopupWindow.ForwardingListener {
        public ActionMenuItemForwardingListener() {
            super(ActionMenuItemView.this);
        }

        @Override
        public ListPopupWindow getPopup() {
            if (mPopupCallback != null) {
                return mPopupCallback.getPopup();
            }
            return null;
        }

        @Override
        protected boolean onForwardingStarted() {
            // Call the invoker, then check if the expected popup is showing.
            if (mItemInvoker != null && mItemInvoker.invokeItem(mItemData)) {
                final ListPopupWindow popup = getPopup();
                return popup != null && popup.isShowing();
            }
            return false;
        }

        @Override
        protected boolean onForwardingStopped() {
            final ListPopupWindow popup = getPopup();
            if (popup != null) {
                popup.dismiss();
                return true;
            }
            return false;
        }
    }

    public static abstract class PopupCallback {
        public abstract ListPopupWindow getPopup();
    }
    ///LEWA ADD BEGIN
    //@LewaHook(LewaHook.LewaHookType.NEW_CLASS)
    static class Injector {
        static int measureMidy(ActionMenuItemView itemView, int y, int height) {
            if (true) {
                return y + height;
            } else {
                return y + height / 2;
            }
        }

        static int getHeight(ActionMenuItemView itemView) {
            if (true) {
                return ((View)itemView.getParent()).getHeight();
            } else {
                return itemView.getHeight();
            }
        }

        static void setIconColorFilter(ActionMenuItemView itemView) {
            if (true) {
                if (itemView.mColorfulIcon != null) {
                    if (itemView.mItemData.isEnabled()) {
                        itemView.mColorfulIcon.clearColorFilter();

                        if (itemView.mActionModeColor != 0) {
                            itemView.setTextColor(itemView.mActionModeColor);
                        } else {
                            //if (itemView.mColorfulStyle != null) {
                            //    itemView.mColorfulColor = itemView.mColorfulStyle.getSecondColor();
                            //    if ((itemView.mColorfulColor & ColorUtils.INVALID_COLOR) != ColorUtils.INVALID_COLOR) {
                            //        itemView.setTextColor(itemView.mColorfulColor);
                            //    }
                            //} else {
                                // Set default color
                                itemView.setTextColor(itemView.getResources().getColor(R.color.action_menubar_textColor));
                            //}
                        }
                    } else {
                        itemView.mColorfulIcon.setColorFilter(ACTION_MENU_ITEM_DISABLED_COLOR, PorterDuff.Mode.SRC_IN);
                        itemView.setTextColor(ACTION_MENU_ITEM_DISABLED_COLOR);
                    }
                }
            }
        }

        static void initColorfulStyle(ActionMenuItemView itemView) {
            if (true) {
                itemView.mIsV5UI = true;

                //itemView.mColorfulStyle = null;
                //itemView.mColorfulStyle =
                //        ((LewaResources) itemView.getResources()).getColorfulResources().getColorfulStyle(itemView.mContext);

                //if (itemView.mColorfulStyle != null) {
                //    itemView.mColorfulColor = itemView.mColorfulStyle.getSecondColor();
                //    if ((itemView.mColorfulColor & ColorUtils.INVALID_COLOR) != ColorUtils.INVALID_COLOR) {
                //        itemView.setTextColor(itemView.mColorfulColor);
                //    }
                //} else {
                    // Set default color
                    itemView.setTextColor(itemView.getResources().getColor(R.color.action_menubar_textColor));
                //}
                Drawable background = new android.graphics.drawable.ColorDrawable(0x1f666666);
                StateListDrawable newBackground = new StateListDrawable();
                newBackground.addState(new int[] {android.R.attr.state_pressed},background);
                itemView.setBackgroundDrawable(newBackground);
            }
        }

        static Drawable resetDrawableColor(ActionMenuItemView itemView, Drawable icon) {
            if (icon == null) {
                return icon;
            }

            //if (itemView.mColorfulStyle != null) {
            //    return resetDrawableColor(itemView, icon, itemView.mColorfulColor);
            //}

            return icon;
        }

        static Drawable resetDrawableColor(ActionMenuItemView itemView, Drawable icon, int color) {
            if (icon == null) {
                return icon;
            }

            if (true) {
                Drawable colorfulIcon = null;
                // Get colorful icon from cache first
                if (mColorfulIcons.size() > 0) {
                    colorfulIcon = mColorfulIcons.get(itemView.mTitle);
                    if (colorfulIcon != null) {
                        return colorfulIcon;
                    }
                }

                colorfulIcon = icon;//ColorUtils.resetDrawableColor(itemView.mContext, icon, color);
                if (colorfulIcon != null) {
                    if (itemView.mTitle != null) {
                        // Cache origin icon as key and colorful icon as value
                        mColorfulIcons.put(itemView.mTitle, colorfulIcon);
                    }

                    return colorfulIcon;
                }
            }

            return icon;
        }

        static Drawable resetActionModeDrawableColor(ActionMenuItemView itemView, Drawable icon, int color) {
            if (icon == null) {
                return icon;
            }

            if (true) {
                Drawable colorfulIcon = null;
                colorfulIcon = icon;//ColorUtils.resetDrawableColor(itemView.mContext, icon, color);
                if (colorfulIcon != null) {
                    return colorfulIcon;
                }
            }

            return icon;
        }

        static int getMaxIconSize(ActionMenuItemView itemView) {
            Resources res = itemView.mContext.getResources();
            final float density = res.getDisplayMetrics().density;
            if (true) {
                return (int) (LEWA_MAX_ICON_SIZE * density + 0.5f);
            } else {
                return (int) (MAX_ICON_SIZE * density + 0.5f);
            }
        }
    }
    ///LEWA ADD END
}
