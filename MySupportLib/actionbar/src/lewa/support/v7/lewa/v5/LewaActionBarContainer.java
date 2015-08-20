package lewa.support.v7.lewa.v5;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.WindowManager;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.view.Display;
import android.view.Surface;
import android.widget.ImageView.ScaleType;
import android.view.Window;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.Animation;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.util.Log;
import android.util.DisplayMetrics;

import lewa.support.v7.internal.widget.ActionBarContainer;
import lewa.support.v7.appcompat.R;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator.AnimatorUpdateListener;

public class LewaActionBarContainer extends ActionBarContainer {
    private static final boolean DBG = true;
    private static final String TAG = "LewaActionBarContainer";

    private static final int ACTION_MENU_ITEM_HEIGHT = 54; // dips
    private static final int ACTION_OPTION_MENU_ITEM_HEIGHT = 48; //dips

    private static final int ACTION_MENU_SLIDE_ANIMATION_DURATION = 300; // ms
    private static final int MASK_SHOW_ANIMATION_DURATION = 500; // ms
    private static final int DOUBLE_TAP_INTERVAL = 500; // ms

    private static final int ACTION_SLIDE_NONE = 0;
    private static final int ACTION_SLIDE_UP = 1;
    private static final int ACTION_SLIDE_DOWN = 2;

    private static final int ACTION_OPTION_MENU_BLUR_MASK = 0x7FFFFFFF;
    private static final int ACTION_MODE_OPTION_MENU_BLUR_MASK = 0x7f000000;

    private static final int ACTION_OPTION_MENU_MASK = 0xFFFFFFFF;
    private static final int ACTION_MODE_OPTION_MENU_MASK = 0xFF626262;

    private static final int ACTION_MODE_ROOT_MASK_BACKGROUND_COLOR = 0x80ffffff;
    private static final int ACTION_ROOT_MASK_BACKGROUND_COLOR = 0x80000000;

    private static final boolean ENABLE_BLUR_EFFECT = false;

    private LinearLayout mLlActionMenuBar;
    private LinearLayout mLlActionOptionMenuBar;
    private LinearLayout mLlActionModeMenuBar;
    private LinearLayout mLlActionModeOptionMenuBar;

    private ImageView mActionMenuBlurView;
    private ImageView mActionModeMenuBlurView;

    private Context mContext;

    private int mTouchCount;
    private long mFirstTouchMills;

    private float mActionDownY;
    private float mActionUpY;

    // Store is not need to reenable blur effect in setVisibility
    private boolean mRestoreBlurEffect;

    private boolean mIsSlided;

    private GestureDetector mActionGestureDetector;
    private ActionMenuBarGestureListener mActionMenuGestureListener;
    private OnTouchListener mActionTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!mIsAnimating) {
                    if (isDoubleTouchEvent(event)) {
                        if (mActionMenuDoubleClickListener != null) {
                            mActionMenuDoubleClickListener.onDoubleClick();
                        }
                    }
                }

                mActionDownY = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mIsSlided = false;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mActionUpY = event.getY();

                if (!mIsAnimating) {
                    handleActionMenuSlide((int) mActionDownY, (int) mActionUpY);
                }
            }

            // mActionGestureDetector.onTouchEvent(event);

            return true;
        }
    };

    private GestureDetector mActionModeGestureDetector;
    private ActionModeMenuBarGestureListener mActionModeMenuGestureListener;
    private OnTouchListener mActionModeTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!mIsAnimating) {
                    if (isDoubleTouchEvent(event)) {
                        if (mActionModeMenuDoubleClickListener != null) {
                            mActionModeMenuDoubleClickListener.onDoubleClick();
                        }
                    }
                }

                mActionDownY = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mIsSlided = false;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mActionUpY = event.getY();

                if (!mIsAnimating) {
                    handleActionModeMenuSlide((int) mActionDownY, (int) mActionUpY);
                }
            }

            //mActionModeGestureDetector.onTouchEvent(event);

            return true;
        }
    };

    private ViewGroup mActionRootView;
    private ViewGroup mActionModeRootView;

    private int mNonActionItemsSize;
    private int mActionItemsSize;
    private int mActionMenuItemHeight;
    private int mActionOptionMenuItemHeight;
    private boolean mIsAnimating;

    private View mVRootMaskView;

    private BlurTargetView mBlurTargetView;
    private ViewBlurDrawable mActionBlurDrawable;
    private ViewBlurDrawable mActionModeBlurDrawable;

    public LewaActionBarContainer(Context context) {
        this(context, null);
    }

    public LewaActionBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        initLayout(context);
    }

    private void initLayout(Context context) {
        final float density = context.getResources().getDisplayMetrics().density;
        mActionMenuItemHeight = (int)(ACTION_MENU_ITEM_HEIGHT * density);
        mActionOptionMenuItemHeight = (int)(ACTION_OPTION_MENU_ITEM_HEIGHT * density);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Init action menu in normal mode
        ViewGroup menuView = (ViewGroup) inflater.inflate(R.layout.lewa_action_menubar, this, true);
        mActionRootView = menuView;
        mActionMenuBlurView = (ImageView) menuView.findViewById(R.id.v_action_menubar);
        mActionMenuGestureListener = new ActionMenuBarGestureListener();
        mActionGestureDetector = new GestureDetector(mActionMenuGestureListener);
        mActionMenuBlurView.setOnTouchListener(mActionTouchListener);
        mLlActionMenuBar = (LinearLayout) menuView.findViewById(R.id.ll_action_menubar);
        mLlActionOptionMenuBar = (LinearLayout) menuView.findViewById(R.id.ll_action_option_menubar);

        // Init action menu in action mode
        ViewGroup optionalMenuView = (ViewGroup) inflater.inflate(R.layout.lewa_actionmode_menubar, this, true);
        mActionModeRootView = optionalMenuView;
        mActionModeMenuBlurView = (ImageView) menuView.findViewById(R.id.v_actionmode_menubar);
        mActionModeMenuGestureListener = new ActionModeMenuBarGestureListener();
        mActionModeGestureDetector = new GestureDetector(mActionModeMenuGestureListener);
        mActionModeMenuBlurView.setOnTouchListener(mActionModeTouchListener);
        mLlActionModeMenuBar = (LinearLayout) menuView.findViewById(R.id.ll_actionmode_menubar);
        mLlActionModeOptionMenuBar = (LinearLayout) menuView.findViewById(R.id.ll_actionmode_option_menubar);

        if (context instanceof android.app.Activity) {
            Window rootWindow = ((android.app.Activity) mContext).getWindow();
            mVRootMaskView = rootWindow.findViewById(R.id.v_root_mask_panel);
        }
    }

    public LinearLayout getActionMenuBar() {
        return mLlActionMenuBar;
    }

    public LinearLayout getActionOptionMenuBar() {
        return mLlActionOptionMenuBar;
    }

    public LinearLayout getActionModeMenuBar() {
        return mLlActionModeMenuBar;
    }

    public LinearLayout getActionModeOptionMenuBar() {
        return mLlActionModeOptionMenuBar;
    }

    public boolean isActionOptionMenuVisible() {
        if (mLlActionOptionMenuBar.getVisibility() == View.VISIBLE) {
            return true;
        }

        return false;
    }

    public boolean isActionModeOptionMenuVisible() {
        if (mLlActionModeOptionMenuBar.getVisibility() == View.VISIBLE) {
            return true;
        }

        return false;
    }

    public void setActionMenuVisibility(boolean show) {
        if (mLlActionMenuBar == null) {
          return;
        }

        if (show) {
            mLlActionMenuBar.setVisibility(View.VISIBLE);
        } else {
            mLlActionMenuBar.setVisibility(View.GONE);
        }
    }

    public void setActionOptionMenuVisibility(boolean show) {
        setActionOptionMenuVisibility(show, true);
    }

    public void setActionOptionMenuVisibility(final boolean show, boolean enableAnim) {
        if (mLlActionOptionMenuBar == null) {
          return;
        }

        if (!enableAnim) {
            if (show) {
                mLlActionOptionMenuBar.setVisibility(View.VISIBLE);
                setMaskVisibility(show, false);
            } else {
                mLlActionOptionMenuBar.setVisibility(View.GONE);
            }

            if (ENABLE_BLUR_EFFECT) {
                enableActionBlurEffect(show);
            }

            return;
        }

        if (!isNeedSetVisibility(mLlActionOptionMenuBar, show)) {
            return;
        }

        if (mIsAnimating) {
            return;
        }

        mActionRootView.clearAnimation();

        ValueAnimator anim = null;
        int translationY = (mActionOptionMenuItemHeight * mNonActionItemsSize) + (2 * (mNonActionItemsSize - 1));
        if (show) {
            anim = ObjectAnimator.ofFloat(mActionRootView, "translationY", translationY, 0);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setActionOptionMenuVisibility(show, false);

                    mIsAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mActionOptionMenuVisibleChangedListener != null) {
                        mActionOptionMenuVisibleChangedListener.onVisibleChanged(true);
                    }

                    //mActionRootView.setTranslationY(5);

                    mIsAnimating = false;
                }
            });
        } else {
            anim = ObjectAnimator.ofFloat(mActionRootView, "translationY", 0, translationY);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mIsAnimating = true;
                    setMaskVisibility(show, false);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mActionRootView.setTranslationY(0);
                    setActionOptionMenuVisibility(show, false);

                    if (mActionOptionMenuVisibleChangedListener != null) {
                        mActionOptionMenuVisibleChangedListener.onVisibleChanged(false);
                    }

                    mIsAnimating = false;
                }
            });
        }

        if (ENABLE_BLUR_EFFECT) {
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int animationValue = Math.round(((Float) animation.getAnimatedValue()));
                    mActionMenuBlurView.setScrollY(getScrollY(animationValue, false));
                }
            });
        }

        anim.setDuration(ACTION_MENU_SLIDE_ANIMATION_DURATION);
        anim.start();
    }

    public void setActionModeMenuVisibility(boolean show) {
        if (mLlActionModeMenuBar == null) {
          return;
        }

        if (show) {
            mLlActionModeMenuBar.setVisibility(View.VISIBLE);
        } else {
            mLlActionModeMenuBar.setVisibility(View.GONE);
        }
    }

    public void setActionModeOptionMenuVisibility(boolean show) {
        setActionModeOptionMenuVisibility(show, true);
    }

    public void setActionModeOptionMenuVisibility(final boolean show, boolean enableAnim) {
        if (mLlActionModeOptionMenuBar == null) {
          return;
        }

        if (!enableAnim) {
            if (show) {
                mLlActionModeOptionMenuBar.setVisibility(View.VISIBLE);
                setMaskVisibility(show, true);
            } else {
                mLlActionModeOptionMenuBar.setVisibility(View.GONE);
            }

            if (ENABLE_BLUR_EFFECT) {
                enableActionModeBlurEffect(show);
            }

            return;
        }

        if (!isNeedSetVisibility(mLlActionModeOptionMenuBar, show)) {
            return;
        }

        if (mIsAnimating) {
            return;
        }

        mActionModeRootView.clearAnimation();

        ValueAnimator anim = null;
        int translationY = (mActionOptionMenuItemHeight * mNonActionItemsSize) + (2 * (mNonActionItemsSize - 1));
        if (show) {
            anim = ObjectAnimator.ofFloat(mActionModeRootView, "translationY", translationY, 0);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setActionModeOptionMenuVisibility(show, false);

                    mIsAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //mActionModeRootView.setTranslationY(5);

                    mIsAnimating = false;
                }
            });
        } else {
            anim = ObjectAnimator.ofFloat(mActionModeRootView, "translationY", 0, translationY);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mIsAnimating = true;

                    setMaskVisibility(show, true);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mActionModeRootView.setTranslationY(0);
                    setActionModeOptionMenuVisibility(show, false);

                    mIsAnimating = false;
                }
            });
        }

        if (ENABLE_BLUR_EFFECT) {
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int animationValue = Math.round(((Float) animation.getAnimatedValue()));
                    mActionModeMenuBlurView.setScrollY(getScrollY(animationValue, true));
                }
            });
        }

        anim.setDuration(ACTION_MENU_SLIDE_ANIMATION_DURATION);
        anim.start();
    }

    public void applyAsActionModeBackground() {
        applyBlurEffect(mActionMenuBlurView, mBlurTargetView, true);
    }

    public boolean isActionMenuAnimating() {
        return mIsAnimating;
    }

    private void applyBlurEffect(final ImageView targetView, final BlurTargetView parentView,
            boolean isActionMode) {
        if (targetView == null || parentView == null) {
            return;
        }

        mBlurTargetView = parentView;

        if (ENABLE_BLUR_EFFECT) {
            ViewBlurDrawable blurDrawable = new ViewBlurDrawable(parentView);
            if (isActionMode) {
                if (mActionModeBlurDrawable == null) {
                    mActionModeBlurDrawable = new ViewBlurDrawable(parentView);
                    mActionModeBlurDrawable.setColorFilter(ACTION_MODE_OPTION_MENU_BLUR_MASK, PorterDuff.Mode.SRC_ATOP);
                }

                targetView.setImageDrawable(mActionModeBlurDrawable);
            } else {
                if (mActionBlurDrawable == null) {
                    mActionBlurDrawable = new ViewBlurDrawable(parentView);
                    mActionBlurDrawable.setColorFilter(ACTION_OPTION_MENU_BLUR_MASK, PorterDuff.Mode.SRC_ATOP);
                }

                targetView.setImageDrawable(mActionBlurDrawable);
            }

            // TODO: Set the maximum height for blur effect.
            targetView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                        int bottom, int oldLeft, int oldTop, int oldRight,
                        int oldBottom) {

                    targetView.setScrollY(parentView.getHeight() - v.getHeight());
                }
            });
        } else {
            if (isActionMode) {
                targetView.setBackgroundColor(ACTION_MODE_OPTION_MENU_MASK);
            } else {
                targetView.setBackgroundColor(ACTION_OPTION_MENU_MASK);
            }
        }
    }

    public void setRootMaskVisibility(final boolean show, final boolean isActionMode) {
        if (isActionMode) {
            setActionModeOptionMenuVisibility(false);
        } else {
            setActionOptionMenuVisibility(false);
        }

        setMaskVisibility(show, isActionMode);
    }

    private void setMaskVisibility(final boolean show, final boolean isActionMode) {
        if (mVRootMaskView == null) {
            if (mContext instanceof android.app.Activity) {
                Window rootWindow = ((android.app.Activity) mContext).getWindow();
                mVRootMaskView = rootWindow.findViewById(R.id.v_root_mask_panel);
            }

            if (mVRootMaskView == null) {
                return;
            }

            mVRootMaskView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (isActionMode) {
                            setActionModeOptionMenuVisibility(false);
                        } else {
                            setActionOptionMenuVisibility(false);
                        }
                    }

                    return true;
                }
            });
        }

        if (mVRootMaskView == null) {
            return;
        }

        // Reset the background of mask in different mode.
        if (isActionMode) {
            mVRootMaskView.setBackgroundColor(ACTION_MODE_ROOT_MASK_BACKGROUND_COLOR);
        } else {
            mVRootMaskView.setBackgroundColor(ACTION_ROOT_MASK_BACKGROUND_COLOR);
        }

        ObjectAnimator anim = null;
        if (show) {
            anim = ObjectAnimator.ofFloat(mVRootMaskView, "alpha", 0f, 1f);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);

                    mVRootMaskView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // TODO Auto-generated method stub
                    super.onAnimationEnd(animation);
                }
            });

            anim.setDuration(MASK_SHOW_ANIMATION_DURATION);
        } else {
            anim = ObjectAnimator.ofFloat(mVRootMaskView, "alpha", 1f, 0f);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // TODO Auto-generated method stub
                    super.onAnimationEnd(animation);

                    mVRootMaskView.setVisibility(View.GONE);
                }
            });

            anim.setDuration(ACTION_MENU_SLIDE_ANIMATION_DURATION);
        }

        anim.start();
    }

    private boolean isNeedSetVisibility(LinearLayout layout, boolean show) {
        if (layout == null) {
            return false;
        }

        if ((layout.getVisibility() == View.VISIBLE) && show) {
            return false;
        } else if ((layout.getVisibility() != View.VISIBLE) && !show) {
            return false;
        }

        if (layout.getChildCount() <= 0) {
            return false;
        }

        // Ignore to show options menu if no non-action menus in normal mode
        if (mNonActionItemsSize <= 0) {
            return false;
        }

        return true;
    }

    private boolean isSlideUp(LinearLayout layout, MotionEvent startEvent, MotionEvent endEvent) {
        float startY = startEvent.getY();
        float endY = endEvent.getY() * -1f;
        //float actionHeight = layout.getHeight() / 2;
        float actionHeight = 10;    // px

        if (endY - startY >= actionHeight) {
            return true;
        }

        return false;
    }

    private int isSlideUp(int startY, int endY) {
        //int startSlideHeight = mActionMenuItemHeight / 2;
        int startSlideHeight = 10;    // px

        // Slide up
        if (endY <= 0) {
            if (startY <= startSlideHeight) {
                return ACTION_SLIDE_NONE;
            }

            return ACTION_SLIDE_UP;
        // Slide down
        } else if (endY > 0) {
            if (endY - startY <= startSlideHeight) {
                return ACTION_SLIDE_NONE;
            }

            return ACTION_SLIDE_DOWN;
        }

        return ACTION_SLIDE_NONE;
    }

    private boolean isDoubleTouchEvent(MotionEvent event) {
        mTouchCount++;

        boolean isDoubleTouch = false;

        if (mTouchCount == 1) {
            mFirstTouchMills = System.currentTimeMillis();
        } else if (mTouchCount == 2) {
            if (System.currentTimeMillis() - mFirstTouchMills < DOUBLE_TAP_INTERVAL) {
                isDoubleTouch = true;
            }

            mTouchCount = 0;
            mFirstTouchMills = 0;
        }

        return isDoubleTouch;
    }

    class ActionMenuBarGestureListener implements OnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {

            if (!mIsAnimating) {
                if (isSlideUp(mLlActionMenuBar, e1, e2)) {
                    setActionOptionMenuVisibility(true);
                } else {
                    setActionOptionMenuVisibility(false);
                }

                if (DBG) {
                    Log.d(TAG, ", start.action=" + e1.getAction()
                        + ", start.Y=" + e1.getY()
                        + ", start.X=" + e1.getX()
                        + ", end.action=" + e2.getAction()
                        + ", end.Y=" + e2.getY()
                        + ", end.X=" + e2.getX()
                        + ", actionBar.height=" + mLlActionMenuBar.getHeight()
                        + ", velocityX=" + velocityX
                        + ", velocityY=" + velocityX);
                }
            }

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }
    }

    class ActionModeMenuBarGestureListener implements OnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {

            if (isSlideUp(mLlActionModeMenuBar, e1, e2)) {
                setActionModeOptionMenuVisibility(true);
            } else {
                setActionModeOptionMenuVisibility(false);
            }

            if (DBG) {
                Log.d(TAG, ", start.action=" + e1.getAction()
                     + ", start.Y=" + e1.getY()
                     + ", start.X=" + e1.getX()
                     + ", end.action=" + e2.getAction()
                     + ", end.Y=" + e2.getY()
                     + ", end.X=" + e2.getX()
                     + ", actionBar.height=" + mLlActionModeMenuBar.getHeight()
                     + ", velocityX=" + velocityX
                     + ", velocityY=" + velocityX);
            }

            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }
    }

    public interface OnActionMenuDoubleClickListener {
        void onDoubleClick();
    }

    public void initBlurEffect(ViewGroup view) {
        BlurTargetView parentView = null;
        if (view instanceof BlurTargetView) {
            parentView = (BlurTargetView) view;
        }

        Log.d(TAG, "initBlurEffect");

        applyBlurEffect(mActionMenuBlurView, parentView, false);
    }

    public void initActionModeBlurEffect(ViewGroup view) {
        BlurTargetView parentView = null;
        if (view instanceof BlurTargetView) {
            parentView = (BlurTargetView) view;
        }

        Log.d(TAG, "initActionModeBlurEffect");

        applyBlurEffect(mActionModeMenuBlurView, parentView, true);
    }

    public void setNonActionItemsSize(int size) {
        mNonActionItemsSize = size;
    }

    public int getSplitHeight() {
        return mActionMenuItemHeight;
    }

    public void setActionItemsSize(int size) {
        mActionItemsSize = size;
    }

    private static int getScreenHeight(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();

        return dm.heightPixels;
    }

    private int getScrollY(View v) {
        if (v == null) {
            return 0;
        }

        ViewGroup rootLayout = null;
        if (mContext instanceof android.app.Activity) {
            Window rootWindow = ((android.app.Activity) mContext).getWindow();
            rootLayout = (ViewGroup) rootWindow.getDecorView().findViewById(android.R.id.content);
        }

        if (rootLayout == null) {
            return 0;
        }

        return rootLayout.getHeight() - v.getHeight();
    }

    private int getScrollY(int animationValue, boolean isActionMode) {
        ViewGroup rootLayout = null;
        if (mContext instanceof android.app.Activity) {
            Window rootWindow = ((android.app.Activity) mContext).getWindow();
            rootLayout = (ViewGroup) rootWindow.getDecorView().findViewById(android.R.id.content);
        }

        if (rootLayout == null) {
            return 0;
        }

        int translationY = (mActionOptionMenuItemHeight * mNonActionItemsSize);
        if (isActionMode) {
            if (mLlActionModeMenuBar.getVisibility() != View.GONE && mLlActionModeMenuBar.getHeight() > 0) {
                translationY += mActionMenuItemHeight;
            }
        } else {
            if (mLlActionMenuBar.getVisibility() != View.GONE && mLlActionMenuBar.getHeight() > 0) {
                translationY += mActionMenuItemHeight;
            }
        }

        int scrollY = rootLayout.getHeight() - translationY + animationValue;

        // if (DBG) {
        //     Log.d(TAG, "[getScrollY] rootHeight=" + rootLayout.getHeight()
        //             + ", translationY=" + translationY
        //             + ", animationValue=" + animationValue
        //             + ", scrollY=" + scrollY);
        // }

        return scrollY;
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (ENABLE_BLUR_EFFECT) {
            boolean enable = (b - t > 0);
            if (mBlurTargetView != null) {
                //Log.d(TAG, "onLayout enable blur effect enable=" + enable);
                mBlurTargetView.setEnableBlur(enable);
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (ENABLE_BLUR_EFFECT) {
            // Disable and enable blur effect in setVisibility to improve performance
            if (mBlurTargetView != null) {
                if (visibility == View.GONE) {
                    if (mBlurTargetView.isBlurEnabled()) {
                        Log.d(TAG, "setVisibility disable blur effect.");
                        mBlurTargetView.setEnableBlur(false);
                        mRestoreBlurEffect = true;
                    }
                } else {
                    if (mRestoreBlurEffect) {
                        Log.d(TAG, "setVisibility disable blur effect.");
                        mBlurTargetView.setEnableBlur(true);
                        mRestoreBlurEffect = false;
                    }
                }
            }
        }
    }

    private void enableActionBlurEffect(boolean enable) {
        if (mBlurTargetView == null) {
            return;
        }

        if (mLlActionMenuBar.getVisibility() == View.GONE || mLlActionMenuBar.getHeight() <= 0) {
            Log.d(TAG, "enableActionBlurEffect enable blur effect enable=" + enable);
            mBlurTargetView.setEnableBlur(enable);
        }
    }

    private void enableActionModeBlurEffect(boolean enable) {
        if (mBlurTargetView == null) {
            return;
        }

        if (mLlActionModeMenuBar.getVisibility() == View.GONE || mLlActionModeMenuBar.getHeight() <= 0) {
            Log.d(TAG, "enableActionModeBlurEffect enable blur effect enable=" + enable);
            mBlurTargetView.setEnableBlur(enable);
        }
    }

    private int getBlurHeight() {
        int height = 0;
        if (mLlActionMenuBar.getVisibility() == View.VISIBLE && mLlActionMenuBar.getHeight() > 0) {
            height += mLlActionMenuBar.getHeight();
        }

        if (mNonActionItemsSize > 0) {
            height += (mActionOptionMenuItemHeight * mNonActionItemsSize) + (1 * (mNonActionItemsSize - 1));
        }

        Log.d(TAG, "getBlurHeight height=" + height
                + ", mActionOptionMenuItemHeight=" + mActionOptionMenuItemHeight
                + ", mNonActionItemsSize=" + mNonActionItemsSize);

        return height;
    }

    private void handleActionMenuSlide(int actionDownY, int actionUpY) {
        int slideUp = isSlideUp(actionDownY, actionUpY);
        if (DBG) {
            Log.d(TAG, "handleActionMenuSlide isSlideUp=" + slideUp
                + ", actionDownY=" + actionDownY
                + ", actionUpY=" + actionUpY);
        }

        // Ignore slide again.
        if (mIsSlided) {
            return;
        }

        if (ACTION_SLIDE_UP == slideUp) {
            mIsSlided = true;

            if (mActionSlideListener != null) {
                mActionSlideListener.onSlide(true);
            }
            // setActionOptionMenuVisibility(true);
        } else if (ACTION_SLIDE_DOWN == slideUp) {
            mIsSlided = true;

            if (mActionSlideListener != null) {
                mActionSlideListener.onSlide(false);
            }
            //setActionOptionMenuVisibility(false);
        }
    }

    private void handleActionModeMenuSlide(int actionDownY, int actionUpY) {
        int slideUp = isSlideUp(actionDownY, actionUpY);
        if (DBG) {
            Log.d(TAG, "handleActionModeMenuSlide isSlideUp=" + slideUp
                + ", actionDownY=" + actionDownY
                + ", actionUpY=" + actionUpY);
        }

        // Ignore slide again.
        if (mIsSlided) {
            return;
        }

        if (ACTION_SLIDE_UP == slideUp) {
            mIsSlided = true;

            setActionModeOptionMenuVisibility(true);
        } else if (ACTION_SLIDE_DOWN == slideUp) {
            mIsSlided = true;

            setActionModeOptionMenuVisibility(false);
        }
    }

    private OnActionMenuDoubleClickListener mActionMenuDoubleClickListener;
    private OnActionMenuDoubleClickListener mActionModeMenuDoubleClickListener;
    public void setOnActionMenuDoubleClickListener(OnActionMenuDoubleClickListener listener) {
        mActionMenuDoubleClickListener = listener;
    }
    public void setOnActionModeMenuDoubleClickListener(OnActionMenuDoubleClickListener listener) {
        mActionModeMenuDoubleClickListener = listener;
    }

    public interface OnActionOptionMenuVisibleChangedListener {
        public void onVisibleChanged(boolean show);
    }
    public void setOnActionOptionMenuVisibleChangedListener(OnActionOptionMenuVisibleChangedListener listener) {
        mActionOptionMenuVisibleChangedListener = listener;
    }
    private OnActionOptionMenuVisibleChangedListener mActionOptionMenuVisibleChangedListener;

    public interface OnActionSlideListener {
        public void onSlide(boolean isUp);
    }
    public void setOnActionOptionMenuSlideListener(OnActionSlideListener listener) {
        mActionSlideListener = listener;
    }
    private OnActionSlideListener mActionSlideListener;
}
