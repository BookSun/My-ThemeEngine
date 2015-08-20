package com.lewa.support.laml;

import android.text.TextUtils;

public class SystemCommandListener implements ScreenElementRoot.OnExternCommandListener {

    private static final String CLEAR_RESOURCE = "__clearResource";
    private static final String REQUEST_UPDATE = "__requestUpdate";
    private ScreenElementRoot   mRoot;

    public SystemCommandListener(ScreenElementRoot ele){
        mRoot = ele;
    }

    public void onCommand(String command, Double para1, String para2) {
        if (CLEAR_RESOURCE.equals(command)) {
            if (TextUtils.isEmpty(para2)) {
                mRoot.getContext().mResourceManager.clear();
            } else {
                mRoot.getContext().mResourceManager.clear(para2);
            }
        } else if (REQUEST_UPDATE.equals(command)) {
            mRoot.requestUpdate();
        }
    }
}
