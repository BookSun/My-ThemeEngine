package com.lewa.thememanager.receiver;

import com.lewa.thememanager.Constants;
import com.lewa.thememanager.utils.ThemeUtilities;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.CustomTheme;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class ThemePackageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        String pkg = data == null ? null : data.getSchemeSpecificPart();
        if (Constants.DEBUG) {
            Log.d(Constants.TAG, "ThemePackageReceiver.onReceive: action=" + action + "; package=" + pkg);
        }

        try {
            Bundle extra = intent.getExtras();
            boolean isReplacing = extra == null ? false : extra.getBoolean(Intent.EXTRA_REPLACING);
            if (Constants.DEBUG) {
                Log.d(Constants.TAG, "ThemePackageReceiver.onReceive: replacing=" + isReplacing);
            }

            if (isReplacing) {
                if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                    PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                    //Delete for standalone by Fan.Yang
/*                    if (isThemeFromPackageApplied(context, pkg) &&
                            pi.themeInfos != null && pi.themeInfos.length > 0) {
                        ThemeUtilities.updateConfiguration(context, pi, pi.themeInfos[0]);
                    }*/
                }
            } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                if (isThemeFromPackageApplied(context, pkg)) {
                    /* Switch us back to the system default. */
                    CustomTheme defaultTheme = CustomTheme.getSystemTheme();
                    if (pkg.equals(defaultTheme.getThemePackageName())) {
                        Log.e(Constants.TAG, "Removed the system default theme?  This should not happen.");
                    } else {
                        // ThemeUtilities.updateConfiguration(context, defaultTheme);
                        // Woody Guo @ 2012/07/05
                        if (Constants.DEBUG) {
                            Log.d(Constants.TAG, "Currently applied theme was removed; falling back to default theme: "
                                    + defaultTheme.getThemeId() + "/" + defaultTheme.getThemePackageName());
                        }
                        // Woody Guo @ 2012/09/13: Call ThemeItem.close() to close the cursor
                        ThemeItem theme = ThemeItem.getInstance(context, Themes.getThemeUri(context
                                , defaultTheme.getThemePackageName(), defaultTheme.getThemeId()));
                        if (null != theme) {
                            try {
                                ThemeUtilities.applyTheme(context, theme);
                            } finally {
                                theme.close();
                            }
                        }
                    }
                }
            }
        } catch (NameNotFoundException e) {
            if (Constants.DEBUG) {
                Log.e(Constants.TAG, "Unable to process intent=" + intent, e);
            }
        }
    }

    private static boolean isThemeFromPackageApplied(Context context, String packageName) {
        CustomTheme theme = ThemeUtilities.getAppliedTheme(context);
        return packageName.equals(theme.getThemePackageName());
    }
}
