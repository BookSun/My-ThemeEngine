package com.lewa.themechooser;

import com.lewa.themechooser.adapters.online.OnlineThemePkgAdapter;
import com.lewa.themechooser.adapters.online.ThumbnailOnlineAdapter;
import com.lewa.themechooser.server.intf.NetBaseParam;
import com.lewa.themechooser.server.intf.UrlParam;

public class OnLineThemeFragment extends OnLineBaseFragment {
    protected NetBaseParam initUrl() {
        return UrlParam.newUrlParam(UrlParam.THEMEPACKAGE);
    }

    @Override
    protected ThumbnailOnlineAdapter onlineAdapterInstance() {
		if (getActivity() == null) {
			return null;
		}
        return new OnlineThemePkgAdapter(getActivity(), themeBases);
    }
}
