/* LEWA TEK TEK
 * Copyright (C) 2010, LEWA TEK, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.res;

import lewa.os.FileUtilities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.os.FileUtils;
import android.text.TextUtils;

import java.io.File;

/**
 * @hide
 */
public final class CustomTheme implements Cloneable {
    private final String mThemeId;
    private final String mThemePackageName;

    private static final CustomTheme sBootTheme = new CustomTheme();
    private static final CustomTheme sLewaTheme
            = new CustomTheme("LewaDefaultTheme", "com.lewa.theme.LewaDefaultTheme");
    // private static final CustomTheme sSystemTheme = new CustomTheme("", "");

    private CustomTheme() {
        mThemeId = SystemProperties.get("persist.sys.themeId");
        mThemePackageName = SystemProperties.get("persist.sys.themePackageName");
    }

    public CustomTheme(String themeId, String packageName) {
        mThemeId = themeId;
        mThemePackageName = packageName;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof CustomTheme) {
            CustomTheme o = (CustomTheme) object;
            if (!mThemeId.equals(o.mThemeId)) {
                return false;
            }
            String currentPackageName = (mThemePackageName == null)? "" : mThemePackageName;
            String newPackageName = (o.mThemePackageName == null)? "" : o.mThemePackageName;
            String currentThemeId = (mThemeId == null)? "" : mThemeId;
            String newThemeId = (o.mThemeId == null)? "" : o.mThemeId;

            /* uhh, why are we trimming here instead of when the object is
             * constructed? actually, why are we trimming at all? */
            return (currentPackageName.trim().equalsIgnoreCase(newPackageName.trim())) &&
                    (currentThemeId.trim().equalsIgnoreCase(newThemeId.trim()));
        }
        return false;
    }

    @Override
    public final String toString() {
        StringBuilder result = new StringBuilder();
        if (!TextUtils.isEmpty(mThemePackageName) && !TextUtils.isEmpty(mThemeId)) {
            result.append(mThemePackageName);
            result.append('(');
            result.append(mThemeId);
            result.append(')');
        } else {
            result.append("system");
        }
        return result.toString();
    }

    @Override
    public synchronized int hashCode() {
        return mThemeId.hashCode() + mThemePackageName.hashCode();
    }

    public String getThemeId() {
        return mThemeId;
    }

    public String getThemePackageName() {
        return mThemePackageName;
    }

    /**
     * Represents the theme that the device booted into. This is used to
     * simulate a "default" configuration based on the user's last known
     * preference until the theme is switched at runtime.
     */
    public static CustomTheme getBootTheme() {
        return getBootTheme(null);
    }

    public static CustomTheme getBootTheme(Context context) {
	    File face = new File("/data/system/face");
        if(!face.exists()){
		    try {
	            FileUtilities.forceMkdir(face, true);
	            FileUtilities.setPermissions(face);
                FileUtils.setPermissions(face.getAbsolutePath(), 0755, -1, -1);
		    }catch(Exception e){
		        android.util.Log.e("CustomTheme","create face fold failed!!!!!,e+"+e);
		    }
		}
        if (!TextUtils.isEmpty(sBootTheme.getThemeId())
                && !TextUtils.isEmpty(sBootTheme.getThemePackageName())) {
            return sBootTheme;
        }
        return sLewaTheme;
    }

    /**
     * Represents the system framework theme, perceived by the system as there
     * being no theme applied.
     */
    public static CustomTheme getSystemTheme() {
        return sLewaTheme;
        // return sSystemTheme;
    }
}
