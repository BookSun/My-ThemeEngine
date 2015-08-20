package com.lewa.thememanager.receiver;

import com.lewa.thememanager.Constants;
import com.lewa.thememanager.utils.NewMechanismHelp;
import com.lewa.thememanager.utils.ThemeUtilities;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes.ThemeColumns;
import com.lewa.themes.CustomType;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ChangeThemeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Constants.DEBUG) {
            Log.d(Constants.TAG, "ChangeThemeReceiver: intent=" + intent);
        }

        ReceiverExecutor.execute(getClass().getSimpleName(), new Runnable() {
            public void run() {
                handleChangeTheme(context, intent);
            }
        });
    }

    private void handleChangeTheme(Context context, Intent intent) {
        ThemeItem item = ThemeItem.getInstance(context, intent.getData());
        if (item == null) {
            Log.e(Constants.TAG, "Could not retrieve theme item for uri=" + intent.getData());
            return;
        }
        try {
            if (intent.getBooleanExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, false) ||
                    intent.getType() == null ||
                    ThemeColumns.CONTENT_ITEM_TYPE.equals(intent.getType())) {
                int type=intent.getIntExtra(CustomType.EXTRA_NAME , CustomType.THEME_TYPE);
                
                switch(type){
                case CustomType.LOCKSCREEN_WALLPAPER_TYPE:
                    ThemeUtilities.applyLockScreenWallpaper(context, item, intent);
					//add by huzeyin for Bug66082 and 65224 ,This Set a special Value as a  symbol  for lockscreen wallpaper changed.
					android.os.SystemProperties.set("sys.lewa.themeChanged", "-100");
                    break;
                case CustomType.LOCKSCREEN_STYLE_TYPE:
                    ThemeUtilities.applyLockScreenStyle(context, item, intent);
                    break;
                case CustomType.DESKTOP_ICON_TYPE:
                    ThemeUtilities.applyIcon(context, item, intent);
                    break;
                case CustomType.DESKTOP_STYLE_TYPE:
                    ThemeUtilities.applyDeskTopStyle(context, item, intent);
                    break;
                case CustomType.DESKTOP_WALLPAPER_TYPE:
                    ThemeUtilities.applyDeskTopWallpaper(context, item, intent);
                    break;
                case CustomType.BOOT_ANIMATION_TYPE:
                    ThemeUtilities.applyBootAnimation(context, item, intent);
                    break;
                case CustomType.FONT_TYPE:
                    ThemeUtilities.applyFonts(context, item, intent);
                    break;
                case CustomType.THEME_DETAIL:
                    ThemeUtilities.applyForThemeDetail(context, item, intent);
                    break;
                case CustomType.SYSTEM_APP:
                    ThemeUtilities.applySystemApp(context, item,intent);
                    break;
                case CustomType.LIVE_WALLPAPER:
                    ThemeUtilities.applyLiveWallpaper(context, intent);
                    break;
                case CustomType.INCALL_STYLE:
                    NewMechanismHelp.applyInCallStyle(context,item,intent,true);
                    break;
                case CustomType.THEME_TYPE:
                    ThemeUtilities.applyTheme(context, item, intent, true);
                    break;
                }
                try {
                    Class<?> im = Class.forName("lewa.util.IconManager");
                    im.getMethod("reset").invoke(im.newInstance());
                } catch (Throwable e) {
                }
            } else if (ThemeColumns.STYLE_CONTENT_ITEM_TYPE.equals(intent.getType())) {
                ThemeUtilities.applyStyle(context, item);
            } else {
                Log.w(Constants.TAG,
                        "Ignoring unknown change theme request (but we aborted it, sorry)...");
            }
        } finally {
            item.close();
        }
    }
}
