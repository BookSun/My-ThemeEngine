package com.lewa.themechooser.custom.main;


import android.app.Fragment;

import com.lewa.themechooser.custom.CustomBase;
import com.lewa.themechooser.custom.fragment.local.SystemAppFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineSystemAppFragment;

public class SystemApp extends CustomBase {

    @Override
    public Fragment getLocalFragment() {
        return new SystemAppFragment();
    }

    @Override
    public Fragment getOnLineFragment() {
        return new OnLineSystemAppFragment();
    }

}
