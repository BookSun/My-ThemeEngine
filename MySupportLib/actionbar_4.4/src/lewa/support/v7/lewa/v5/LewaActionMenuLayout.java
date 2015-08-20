package lewa.support.v7.lewa.v5;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import lewa.support.v7.appcompat.R;
public class LewaActionMenuLayout extends RelativeLayout {
    public static final int ACTION_BLUR_VIEW_ID = R.id.v_action_menubar;
    public static final int ACTION_MODE_BLUR_VIEW_ID = R.id.v_actionmode_menubar;
    public LewaActionMenuLayout(Context context) {
        this(context, null, 0);
        // TODO Auto-generated constructor stub
    }

    public LewaActionMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        // TODO Auto-generated constructor stub
    }

    public LewaActionMenuLayout(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        super.onLayout(changed, l, t, r, b);

        int childs = this.getChildCount();
        View blurView = this.getChildAt(0);
        if (blurView.getId() == ACTION_BLUR_VIEW_ID
            || blurView.getId() == ACTION_MODE_BLUR_VIEW_ID) {
            blurView.layout(l, t, r, b);
        }
    }
}
