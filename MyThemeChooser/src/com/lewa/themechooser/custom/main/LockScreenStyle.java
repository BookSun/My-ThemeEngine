package com.lewa.themechooser.custom.main;

import android.app.Fragment;

import com.lewa.themechooser.custom.CustomBase;
import com.lewa.themechooser.custom.fragment.local.LocalLockScreenStyleFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineLockScreenStyleFragment;

/**
 * @author xufeng
 */
public class LockScreenStyle extends CustomBase {

    @Override
    public Fragment getLocalFragment() {
        return new LocalLockScreenStyleFragment();
    }

    @Override
    public Fragment getOnLineFragment() {
        return new OnLineLockScreenStyleFragment();
    }

}
