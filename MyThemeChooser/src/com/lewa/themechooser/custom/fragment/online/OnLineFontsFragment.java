package com.lewa.themechooser.custom.fragment.online;

import com.lewa.themechooser.OnLineBaseFragment;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.adapters.online.OnlineFontsAdapter;
import com.lewa.themechooser.adapters.online.ThumbnailOnlineAdapter;
import com.lewa.themechooser.server.intf.NetBaseParam;
import com.lewa.themechooser.server.intf.UrlParam;

public class OnLineFontsFragment extends OnLineBaseFragment {
    @Override
    protected NetBaseParam initUrl() {
        return UrlParam.newUrlParam(UrlParam.FONT, ThemeConstants.FONT);
    }

    @Override
    protected ThumbnailOnlineAdapter onlineAdapterInstance() {
		if (getActivity() == null) {
			return null;
		}
        return new OnlineFontsAdapter(getActivity(), themeBases);
    }

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayoutRes = com.lewa.themechooser.R.layout.online_fonts;
    }
}
