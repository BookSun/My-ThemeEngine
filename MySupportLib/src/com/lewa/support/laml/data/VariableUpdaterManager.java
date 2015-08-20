
package com.lewa.support.laml.data;

import android.text.TextUtils;

import java.util.ArrayList;

import com.lewa.support.laml.ScreenContext;

public class VariableUpdaterManager {

    public static final String USE_TAG_NONE = "none";

    private ScreenContext mContext;

    private ArrayList<VariableUpdater> mUpdaters = new ArrayList<VariableUpdater>();

    public VariableUpdaterManager(ScreenContext c) {
        mContext = c;
    }

    public void add(VariableUpdater updater) {
        mUpdaters.add(updater);
    }

    public void addFromTag(String tag) {
        if (!TextUtils.isEmpty(tag) && !USE_TAG_NONE.equalsIgnoreCase(tag)) {
            String updaters[] = tag.split(",");
            for (int i = 0, N = updaters.length; i < N; i++) {
                String str = updaters[i].trim();
                String name = str;
                String ext = null;
                int dotPos = str.indexOf('.');
                if (dotPos != -1) {
                    name = str.substring(0, dotPos);
                    ext = str.substring(dotPos + 1);
                }
                if (name.equals(DateTimeVariableUpdater.USE_TAG)) {
                    add(new DateTimeVariableUpdater(this, ext));
                } else if (name.equals(BatteryVariableUpdater.USE_TAG)) {
                    add(new BatteryVariableUpdater(this, mContext));
                }
            }

        }
    }

    public void finish() {
        for (VariableUpdater updater : mUpdaters) {
            updater.finish();
        }
    }

    public ScreenContext getContext() {
        return mContext;
    }

    public void init() {
        for (VariableUpdater updater : mUpdaters) {
            updater.init();
        }
    }

    public void pause() {
        for (VariableUpdater updater : mUpdaters) {
            updater.pause();
        }
    }

    public void remove(VariableUpdater updater) {
        mUpdaters.remove(updater);
    }

    public void resume() {
        for (VariableUpdater updater : mUpdaters) {
            updater.resume();
        }
    }

    public void tick(long currentTime) {
        for (VariableUpdater updater : mUpdaters) {
            updater.tick(currentTime);
        }
    }
}
