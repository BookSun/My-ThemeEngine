package lewa.support.v7.lewa.v5;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import lewa.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;
import lewa.support.v7.internal.widget.*;
import lewa.support.v7.internal.view.menu.*;
import lewa.support.v7.internal.view.menu.MenuBuilder;
import lewa.support.v7.internal.view.StandaloneActionMode;
import lewa.support.v7.appcompat.R;
import lewa.support.v7.internal.app.WindowDecorActionBar.ActionModeImpl;
import android.view.MotionEvent;

public class LewaActionBarContextView extends ActionBarContextView {

    private ViewGroup mDone;
    private ActionMenuItem mRightButtonMenuItem;
    private Drawable mRightButtonImage;
    private int mDoneVisibility = View.GONE;

    public LewaActionBarContextView(Context context) {
        this(context, null);
    }

    public LewaActionBarContextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRightButtonMenuItem = new ActionMenuItem(context, 0, R.id.action_mode_right_button, 0, 0,
                null);
    }

    public LewaActionBarContextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mRightButtonMenuItem = new ActionMenuItem(context, 0, R.id.action_mode_right_button, 0, 0,
                null);
    }

    public boolean lewaInitTitle() {
        LinearLayout titleLayout = getTitleLayout();
        if (titleLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.action_bar_title_item_multichoice, this);
            titleLayout = (LinearLayout) getChildAt(getChildCount() - 1);

            int statusbarHeight = (int) getContext().getResources().getDimension(
                    R.dimen.android_status_bar_height);
            titleLayout.setPadding(titleLayout.getPaddingLeft(), statusbarHeight,
                    titleLayout.getPaddingRight(), titleLayout.getPaddingBottom());

            setTitleLayout(titleLayout);
            TextView titleView = (TextView) titleLayout.findViewById(R.id.android_action_bar_title);
            TextView subtitleView = (TextView) titleLayout
                    .findViewById(R.id.android_action_bar_subtitle);
            if (getTitleStyleRes() != 0) {
                titleView.setTextAppearance(mContext, getTitleStyleRes());
            }
            if (getSubtitleStyleRes() != 0) {
                subtitleView.setTextAppearance(mContext, getSubtitleStyleRes());
            }
            titleView.setText(getTitle());
            subtitleView.setText(getSubtitle());

            setTitleView(titleView);
            setSubTitleView(subtitleView);

            final boolean hasTitle = !TextUtils.isEmpty(getTitle());
            final boolean hasSubtitle = !TextUtils.isEmpty(getSubtitle());
            subtitleView.setVisibility(hasSubtitle ? VISIBLE : GONE);
            titleLayout.setVisibility(hasTitle || hasSubtitle ? VISIBLE : GONE);
            if (titleLayout.getParent() == null) {
                addView(titleLayout);
            }
        }
        return true;
    }

    public void initForMode(final ActionMode mode) {
        super.initForMode(mode);

        if (mDone == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mDone = (ViewGroup) inflater.inflate(R.layout.action_mode_right_item, this, false);
            addView(mDone);
        } else if (mDone.getParent() == null) {
            addView(mDone);
        }
        mDone.setVisibility(View.VISIBLE);

        if (mRightButtonImage != null) {
            setRightActionButtonDrawable(mRightButtonImage);
        }

        View doneButton = mDone.findViewById(R.id.action_mode_right_button);
        int statusbarHeight = (int) getContext().getResources()
                .getDimension(R.dimen.android_status_bar_height);
        Log.d("simply",
                "--left:" + doneButton.getPaddingLeft() + ",right:" + doneButton.getPaddingRight());
        doneButton.setPadding(doneButton.getPaddingLeft(), statusbarHeight,
                doneButton.getPaddingRight(), doneButton.getPaddingBottom());

        doneButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ActionMode actionMode = getActionMode();
                if (actionMode != null) {
                    if (actionMode instanceof ActionModeImpl) {
                        ((ActionModeImpl) actionMode)
                                .onMenuItemSelected((MenuBuilder) (actionMode.getMenu()),
                                        mRightButtonMenuItem);
                    }
                    if (actionMode instanceof StandaloneActionMode) {
                        ((StandaloneActionMode) actionMode)
                                .onMenuItemSelected((MenuBuilder) (actionMode.getMenu()),
                                        mRightButtonMenuItem);
                    }
                }
            }
        });
        doneButton.setOnTouchListener(new InjectorTouchListener());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int contentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = mContentHeight > 0 ?
                mContentHeight : MeasureSpec.getSize(heightMeasureSpec);
        final int verticalPadding = getPaddingTop() + getPaddingBottom();
        int availableWidth = contentWidth - getPaddingLeft() - getPaddingRight();
        final int height = maxHeight - verticalPadding;
        final int childSpecHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

        if (mDone != null) {
            measureChildView(mDone, availableWidth, childSpecHeight, 0);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = r - l - getPaddingRight();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();
        if (mDone != null && mDone.getVisibility() != GONE) {
            MarginLayoutParams lp = (MarginLayoutParams) mDone.getLayoutParams();
            positionChild(mDone, x, y, contentHeight, true);
        }

        super.onLayout(changed, l, t, r, b);
    }

    public void setRightActionButtonDrawable(Drawable drawable) {
        mRightButtonImage = drawable;
        if (mDone != null) {
            ImageView doneButton = (ImageView) (mDone
                    .findViewById(R.id.action_mode_right_imageview));
            doneButton.setImageDrawable(mRightButtonImage);
        }
    }

    protected ViewPropertyAnimatorCompat makeRightButtonInAnimation() {
        mDone.setTranslationX(mDone.getRight() + mDone.getWidth() +
                ((MarginLayoutParams) mDone.getLayoutParams()).rightMargin);
        ViewPropertyAnimatorCompat buttonAnimator = ViewCompat.animate(mDone).translationX(0);
        buttonAnimator.setDuration(200);
        buttonAnimator.setInterpolator(new DecelerateInterpolator());
        return buttonAnimator;
    }

    protected ViewPropertyAnimatorCompat makeRightButtonOutAnimation() {
        ViewPropertyAnimatorCompat buttonAnimator = ViewCompat.animate(mDone).translationX(
                mDone.getRight() + mDone.getWidth() +
                        ((MarginLayoutParams) mDone.getLayoutParams()).rightMargin);
        buttonAnimator.setDuration(200);
        buttonAnimator.setInterpolator(new DecelerateInterpolator());
        return buttonAnimator;
    }

    public void setRightActionButtonVisibility(int visibility) {
        mDoneVisibility = visibility;
        if (mDone != null) {
            mDone.setVisibility(visibility);
        }
    }

    private class InjectorTouchListener implements OnTouchListener {
        private MotionEvent mCurrentDownEvent;

        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            final float x = event.getX();
            final float y = event.getY();
            ImageView closeView = (ImageView) v.findViewById(R.id.action_mode_right_imageview);

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

}
