package com.lewa.themechooser;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.widget.FrameLayout;


public class WallpaperSelector extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout parent = new FrameLayout(this);
        parent.setId(android.R.id.content);
        setContentView(parent);
        initActionbar();
        setupFragment();
    }

    private void initActionbar() {
        ActionBar bar = getActionBar();
        bar.setDisplayShowHomeEnabled(false);
        bar.setDisplayShowTitleEnabled(true);
    }

    private void setupFragment() {
        Fragment fragment = new CustomerFragment();
        Bundle args = new Bundle();
        args.putBoolean("wallpaper", true);
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }
}
