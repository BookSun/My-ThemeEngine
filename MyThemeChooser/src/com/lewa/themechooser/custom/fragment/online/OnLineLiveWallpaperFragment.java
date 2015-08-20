package com.lewa.themechooser.custom.fragment.online;

import com.lewa.themechooser.OnLineBaseFragment;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.adapters.online.OnlineLiveWallpaperAdapter;
import com.lewa.themechooser.adapters.online.ThumbnailOnlineAdapter;
import com.lewa.themechooser.server.intf.NetBaseParam;
import com.lewa.themechooser.server.intf.UrlParam;

public class OnLineLiveWallpaperFragment extends OnLineBaseFragment {

    @Override
    protected NetBaseParam initUrl() {
        return UrlParam.newUrlParam(UrlParam.LIVE_WALLPAPER, ThemeConstants.LIVE_WALLPAPER);
    }

    @Override
    protected ThumbnailOnlineAdapter onlineAdapterInstance() {
		if (getActivity() == null) {
			return null;
		}
        return new OnlineLiveWallpaperAdapter(getActivity(), themeBases);
    }

}
