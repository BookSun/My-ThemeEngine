package com.lewa.themechooser.custom.main;

import android.app.Fragment;

import com.lewa.themechooser.custom.CustomBase;
import com.lewa.themechooser.custom.fragment.local.IconFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineIconFragment;

public class Icon extends CustomBase {

    @Override
    public Fragment getLocalFragment() {
        return new IconFragment();
    }

    @Override
    public Fragment getOnLineFragment() {
        return new OnLineIconFragment();
    }

}
