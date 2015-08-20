package com.lewa.themechooser.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewCheck extends ImageView {
    private boolean mChecked;

    public ImageViewCheck(Context context) {
        super(context);
    }

    public ImageViewCheck(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
        }
    }
}
