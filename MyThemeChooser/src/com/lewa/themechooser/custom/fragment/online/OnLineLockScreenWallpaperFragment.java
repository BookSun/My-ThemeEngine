package com.lewa.themechooser.custom.fragment.online;

import com.lewa.themechooser.OnLineBaseFragment;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.adapters.online.OnlineLockScreenWallPaperAdapter;
import com.lewa.themechooser.adapters.online.ThumbnailOnlineAdapter;
import com.lewa.themechooser.server.intf.NetBaseParam;
import com.lewa.themechooser.server.intf.UrlParam;

/**
 * @author xufeng
 */
public class OnLineLockScreenWallpaperFragment extends OnLineBaseFragment {

    @Override
    protected NetBaseParam initUrl() {
        return UrlParam.newUrlParam(UrlParam.WALLPAPER, ThemeConstants.LOCKSCREEN_WALLPAPER);
    }

    @Override
    protected ThumbnailOnlineAdapter onlineAdapterInstance() {
		if (getActivity() == null) {
			return null;
		}
        return new OnlineLockScreenWallPaperAdapter(getActivity(), themeBases);
    }

}
