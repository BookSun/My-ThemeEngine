package com.lewa.themechooser.custom.main;


import android.app.Fragment;

import com.lewa.themechooser.custom.CustomBase;
import com.lewa.themechooser.custom.fragment.local.FontsFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineFontsFragment;

public class Fonts extends CustomBase {

    @Override
    public Fragment getLocalFragment() {
        return new FontsFragment();
    }

    @Override
    public Fragment getOnLineFragment() {
        return new OnLineFontsFragment();
    }

}
