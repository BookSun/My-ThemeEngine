package com.lewa.themechooser.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextViewCheck extends TextView {
    private boolean mChecked;

    public TextViewCheck(Context context) {
        super(context);
    }

    public TextViewCheck(Context context, AttributeSet attrs) {
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
