package lewa.support.v7.lewa.v5;

import lewa.support.v7.internal.view.menu.ActionMenuItemView;
import lewa.support.v7.internal.widget.ViewUtils;
import lewa.support.v7.widget.ActionMenuView;
import lewa.support.v7.appcompat.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.util.Log;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.provider.Settings.System;

public class LewaActionMenuView extends ActionMenuView {
    public static int MAX_CELL_COUNT = 4;

    static final int MAX_ITEM_WIDTH = 64; // dips
    static final int ITEM_MARGIN = 16; // dips
    static final int MORE_ITEM_WIDTH = 30; // dips
    static final int GENERATED_ITEM_PADDING = 2; // dips
    private static final int ACTION_ITEM_TOP_PADDING = 6; // dips
    private static final int ACTION_ITEM_BOTTOM_PADDING = 6; // dips
    public static final int ACTION_MENU_VIEW_HEIGHT = 54; // dips

    private Drawable mActionMenuBackground;

    private int mGeneratedItemPadding;
    private boolean mFormatItems;
    private int mMaxItemHeight;
    private static int mMaxItemWidth;
    private static int mItemMargin;
    private static int mMoreItemWidth;

    private Context mContext;
    private int mActionItemTopPadding;
    private int mActionItemBottomPadding;

    public LewaActionMenuView(Context context) {
        this(context, null);
    }

    public LewaActionMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float density = context.getResources().getDisplayMetrics().density;

        mMaxItemWidth = (int) (MAX_ITEM_WIDTH * density);
        mItemMargin = (int) (ITEM_MARGIN * density);
        mMoreItemWidth = (int) (MORE_ITEM_WIDTH * density);

        mGeneratedItemPadding = (int) (GENERATED_ITEM_PADDING * density);
        mActionItemTopPadding = (int) (ACTION_ITEM_TOP_PADDING * density);
        mActionItemBottomPadding = (int) (ACTION_ITEM_BOTTOM_PADDING * density);

        mMaxItemHeight = (int) (ACTION_MENU_VIEW_HEIGHT * density);

        mActionMenuBackground = context.getResources()
                .getDrawable(R.drawable.split_action_bar_action_menu_background);

        mContext = context;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mActionMenuBackground != null) {
            mActionMenuBackground.draw(canvas);
        }
        super.dispatchDraw(canvas);
    }

    public void setMaxItemHeight(int maxItemHeight) {
        mMaxItemHeight = maxItemHeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If we've been given an exact size to match, apply special formatting during layout.
        final boolean wasFormatted = mFormatItems;
        mFormatItems = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY;

        // Special formatting can change whether items can fit as action buttons.
        // Kick the menu and update presenters when this changes.
        final int widthSize = MeasureSpec.getMode(widthMeasureSpec);
        if (mFormatItems && getMenu() != null) {
            onMenuItemsChangedWrapper(true);
        }

        if (mFormatItems) {
            onMeasureExactFormat(widthMeasureSpec, heightMeasureSpec);
        } else {
            // Previous measurement at exact format may have set margins - reset them.
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.leftMargin = lp.rightMargin = 0;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void onMeasureExactFormat(int widthMeasureSpec, int heightMeasureSpec) {
        // We already know the width mode is EXACTLY if we're here.
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = mMaxItemHeight;

        final int widthPadding = getPaddingLeft() + getPaddingRight();
        final int heightPadding = getPaddingTop() + getPaddingBottom();

        final int itemHeightSpec = heightMode == MeasureSpec.EXACTLY
                ? MeasureSpec.makeMeasureSpec(heightSize - heightPadding, MeasureSpec.EXACTLY)
                : MeasureSpec.makeMeasureSpec(
                Math.min(mMaxItemHeight, heightSize - heightPadding), MeasureSpec.AT_MOST);

        widthSize -= widthPadding;

        int maxChildHeight = 0;

        final int childCount = getChildCount();
        //if(childCount > 1)
        {

            int menuStyle = 1;//System.getInt(mContext.getContentResolver(),
            //Settings.LEWA_ACTION_MENU_STYLE,
            //Settings.LEWA_ACTION_MENU_STYLE_ICON);

            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE)
                    continue;

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if ((childCount <= 1) && (lp.isOverflowButton))
                    break;
                lp.expanded = false;
                lp.extraPixels = 0;
                lp.cellsUsed = 0;
                lp.expandable = false;
                lp.leftMargin = 0;
                lp.rightMargin = 0;

                measureChildForCells(child, itemHeightSpec, heightPadding);

                maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());

                if (!lp.isOverflowButton) {
                    final ActionMenuItemView itemView = child instanceof ActionMenuItemView ?
                            (ActionMenuItemView) child : null;
                    Drawable icon = null;
                    int topPadding = 0;
                    int bottomPadding = 0;
                    if (itemView != null) {
                        icon = itemView.getIcon();

                        if (menuStyle == 1) {
                            topPadding = mActionItemTopPadding;
                            bottomPadding = mActionItemBottomPadding;
                        } else {
                            if (!itemView.hasText() && icon != null) {
                                topPadding = (mMaxItemHeight - icon.getBounds().bottom) / 2;
                            }
                            bottomPadding = 0;
                        }
                    }

                    child.setPadding(mGeneratedItemPadding, topPadding,
                            mGeneratedItemPadding, bottomPadding);
                }
            }
        }

        if (heightMode != MeasureSpec.EXACTLY) {
            heightSize = maxChildHeight;
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    /**
     * Measure a child view to fit within cell-based formatting. The child's width
     * will be measured to a whole multiple of cellSize.
     * <p/>
     * <p>Sets the expandable and cellsUsed fields of LayoutParams.
     *
     * @param child                   Child to measure
     * @param cellSize                Size of one cell
     * @param cellsRemaining          Number of cells remaining that this view can expand to fill
     * @param parentHeightMeasureSpec MeasureSpec used by the parent view
     * @param parentHeightPadding     Padding present in the parent view
     * @return Number of cells this child was measured to occupy
     */
    static void measureChildForCells(View child, int parentHeightMeasureSpec,
            int parentHeightPadding) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        final int childHeightSize = MeasureSpec.getSize(parentHeightMeasureSpec) -
                parentHeightPadding;
        final int childHeightMode = MeasureSpec.getMode(parentHeightMeasureSpec);
        final int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeightSize, childHeightMode);

        final ActionMenuItemView itemView = child instanceof ActionMenuItemView ?
                (ActionMenuItemView) child : null;
        final boolean hasText = itemView != null && itemView.hasText();

        final int childWidthSpec = MeasureSpec.makeMeasureSpec(
                mMaxItemWidth, MeasureSpec.AT_MOST);
        child.measure(childWidthSpec, childHeightSpec);

        final boolean expandable = !lp.isOverflowButton && hasText;
        lp.expandable = expandable;

        final int targetWidth = mMaxItemWidth;
        child.measure(MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY),
                childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!mFormatItems) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }

        final int childCount = getChildCount();
        final int midVertical = (top + bottom) / 2;
        final int dividerWidth = getDividerWidth();
        int overflowWidth = 0;
        int nonOverflowWidth = 0;
        int nonOverflowCount = 0;
        int widthRemaining = right - left - getPaddingRight() - getPaddingLeft();
        boolean hasOverflow = false;
        final boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        /// M: Add this variable to count action items who will take place, if only one, we will center it.
        int toTakePlaceChildCount = 0;
        /// M: Record the latest child's index who will take place
        int toTakePlaceChildIndex = -1;
        boolean hasOverflowButton = false;

        // Set background of menu bar
        if (mActionMenuBackground != null && childCount > 0) {
            mActionMenuBackground.setBounds(left, top, right, bottom);
        }

        for (int i = 0; i < childCount; i++) {
            final View v = getChildAt(i);
            if (v.getVisibility() == GONE) {
                continue;
            }

            /// M: If the view is not gone, it will take place, so count it and record its index
            toTakePlaceChildCount++;
            toTakePlaceChildIndex = i;

            LayoutParams p = (LayoutParams) v.getLayoutParams();
            if (p.isOverflowButton) {
                hasOverflowButton = true;
                overflowWidth = mMoreItemWidth;
                if (hasDividerBeforeChildAt(i)) {
                    overflowWidth += dividerWidth;
                }

                int height = v.getMeasuredHeight();
                int r;
                int l;
                if (isLayoutRtl) {
                    l = getPaddingLeft() + p.leftMargin;
                    r = l + overflowWidth;
                } else {
                    r = getWidth() - getPaddingRight() - p.rightMargin;
                    l = r - overflowWidth;
                }
                int t = midVertical - (height / 2);
                int b = t + height;
                v.layout(l, t, r, b);

                widthRemaining -= overflowWidth;
                hasOverflow = true;
            } else {
                final int size = v.getMeasuredWidth() + p.leftMargin + p.rightMargin;
                nonOverflowWidth += size;
                widthRemaining -= size;
                if (hasDividerBeforeChildAt(i)) {
                    nonOverflowWidth += dividerWidth;
                }
                nonOverflowCount++;
            }
        }

        /// Check if there is only one non-gone state action item, if yes, we center it.
        if (toTakePlaceChildCount == 1 && !hasOverflow) {
            // Center a single child
            final View v = getChildAt(toTakePlaceChildIndex);
            final int width = v.getMeasuredWidth();
            final int height = v.getMeasuredHeight();
            final int midHorizontal = (right - left) / 2;
            final int l = midHorizontal - width / 2;
            final int t = midVertical - height / 2;
            v.layout(l, t, l + width, t + height);
            return;
        }

        final int spacerCount = nonOverflowCount - (hasOverflow ? 0 : 1);
        final int spacerSize = Math.max(0, spacerCount > 0 ? widthRemaining / spacerCount : 0);

        if (isLayoutRtl) {
            int startRight = getWidth() - getPaddingRight();
            for (int i = 0; i < childCount; i++) {
                final View v = getChildAt(i);
                final LayoutParams lp = (LayoutParams) v.getLayoutParams();
                if (v.getVisibility() == GONE || lp.isOverflowButton) {
                    continue;
                }

                startRight -= lp.rightMargin;
                int width = v.getMeasuredWidth();
                int height = v.getMeasuredHeight();
                int t = midVertical - height / 2;
                v.layout(startRight - width, t, startRight, t + height);
                startRight -= width + lp.leftMargin + spacerSize;
            }
        } else {
            int startLeft = 0;
            for (int i = 0; i < childCount; i++) {
                final View v = getChildAt(i);
                final LayoutParams lp = (LayoutParams) v.getLayoutParams();
                if (v.getVisibility() == GONE || lp.isOverflowButton) {
                    continue;
                }

                int width = mMaxItemWidth;
                int height = v.getMeasuredHeight();
                int t = midVertical - height / 2;

                if (startLeft <= 0) {
                    if (hasOverflowButton) {
                        startLeft = (int) ((right - ((childCount - 1) * width)) -
                                (mItemMargin * (childCount - 1 - 1))) / 2;
                    } else {
                        startLeft = (int) ((right - (childCount * width)) -
                                (mItemMargin * (childCount - 1))) / 2;
                    }
                }

                v.layout(startLeft, t, startLeft + width, t + height);
                startLeft += width + mItemMargin;
            }
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
    }

    @Override
    public void setBackground(Drawable background) {
    }
}
