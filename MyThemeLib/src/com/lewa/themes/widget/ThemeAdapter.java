/*
 * Copyright (C) 2010, T-Mobile USA, Inc.
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

package com.lewa.themes.widget;

import android.content.ContentResolver;
import android.database.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CursorAdapter;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;

import android.app.Activity;
import android.content.Context;
import android.content.res.CustomTheme;
import android.util.Log;

import static com.lewa.themes.ThemeManager.DEBUG;
import static com.lewa.themes.ThemeManager.STANDALONE;

import java.io.File;

/**
 * Re-usable adapter which fills itself with all currently installed visual
 * themes. The Adapter will manager the cursor.
 *
 * @author T-Mobile USA
 */
public abstract class ThemeAdapter extends AbstractDAOItemAdapter<ThemeItem> {
    private static final int LOCKSCREEN_WALLPAPER_TYPE = 0;
    private static final int LOCKSCREEN_STYLE_TYPE = 1;
    private static final int DESKTOP_ICON_TYPE = 2;
    private static final int DESKTOP_STYLE_TYPE = 3;
    private static final int DESKTOP_WALLPAPER_TYPE = 4;
    private static final int BOOT_ANIMATION_TYPE = 5;
    private static final int FONT_TYPE = 6;
    private static final int SYSTEM_APP = 7;
    private static final int LIVE_WALLPAPER_TYPE = 8;

    public ThemeAdapter(Activity context) {
        super(context, loadThemes(context), true);
        ((ThemeCursorWrapper) getCursor()).setCursorAdapter(this);
    }

    public ThemeAdapter(Activity context, int type) {
        super(context, loadThemes(context, type), true);
        ((ThemeCursorWrapper) getCursor()).setCursorAdapter(this);
    }

    private static Cursor loadThemes(Activity context) {
        // Woody Guo @ 2012/08/21: Return themes which have resource redirections
/*        cursors[0]=context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI, null
                , ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme'", null);
        cursors[1]=context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI
              , null,"((case when " + ThemeColumns.WALLPAPER_URI + " is null then 0 else 1 end)"
                      + "+(case when (" + ThemeColumns.LOCK_WALLPAPER_URI + " is null and "+ThemeColumns.LOCKSCREEN_URI+" is null) "+"then 0 else 1 end)"
                      + "+(case when " + ThemeColumns.FONT_URI + " is null then 0 else 1 end)"
                      + "+(case when " + ThemeColumns.BOOT_ANIMATION_URI + " is null then 0 else 1 end)"
                      + "+(case when " + ThemeColumns.ICONS_URI + " is null then 0 else 1 end)) >= 2"+" AND "
                      +ThemeColumns.THEME_PACKAGE + "<>'com.lewa.theme.LewaDefaultTheme'",
                      ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");*/

        // Fan.Yang @ 2015/04/05：Wrapper cursor which may closed when theme provider killed
        return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI
                , null, ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or " +
                        "((case when " + ThemeColumns.WALLPAPER_URI + " is null then 0 else 1 end)"
                        + "+(case when (" + ThemeColumns.LOCK_WALLPAPER_URI + " is null and " +
                        ThemeColumns.LOCKSCREEN_URI + " is null) " + "then 0 else 1 end)"
                        + "+(case when " + ThemeColumns.FONT_URI + " is null then 0 else 1 end)"
                        + "+(case when " + ThemeColumns.BOOT_ANIMATION_URI +
                        " is null then 0 else 1 end)" + "+(case when " + ThemeColumns.ICONS_URI +
                        " is null then 0 else 1 end)) >= 2" + " AND "
                        + ThemeColumns.THEME_PACKAGE + "<>'com.lewa.theme.LewaDefaultTheme'", null,
                ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
    }

    // swap managed cursor, add by Fan.Yang
    private static Cursor wrapperManagedQuery(Activity context, Uri uri,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = context.getContentResolver().query(uri, projection, selection,
                selectionArgs, sortOrder);
        Cursor cursorWrapper = new ThemeCursorWrapper(context, cursor, uri, projection,
                selection, selectionArgs, sortOrder);
        context.startManagingCursor(cursorWrapper);
        return cursorWrapper;
    }

    private static Cursor loadThemes(Activity context, int type) {
        switch (type) {
            case LOCKSCREEN_WALLPAPER_TYPE:
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or "
                                + ThemeColumns.LOCK_WALLPAPER_URI + " is not null", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            case LOCKSCREEN_STYLE_TYPE:
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null
                        , ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or "
                                + ThemeColumns.LOCKSCREEN_URI + " is not null", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            case DESKTOP_ICON_TYPE:
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null
                        , ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or "
                                + ThemeColumns.ICONS_URI + " is not null", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            case DESKTOP_WALLPAPER_TYPE:
                /*#64909 delete by bin.dong
                Cursor[] cursors = new Cursor[2];
                cursors[0] = context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme'",
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
                cursors[1] = context.managedQuery(ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.WALLPAPER_URI + " is not null AND "
                                + ThemeColumns.IS_IMAGE_FILE + "=-1" + " AND "
                                +ThemeColumns.THEME_PACKAGE
                                + "<>'com.lewa.theme.LewaDefaultTheme'",
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
                return new MergeCursor(cursors);*/
                // TCL968830 modify by Fan.Yang, themechooser 里的默认壁纸和系统默认壁纸重复，需要过滤掉一个
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null
                        , ThemeColumns.THEME_PACKAGE + "<>'com.lewa.pkg.rw1' AND ("
                                + ThemeColumns.WALLPAPER_URI + " is not null )", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            case BOOT_ANIMATION_TYPE:
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or "
                                + ThemeColumns.BOOT_ANIMATION_URI + " is not null", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");

            case FONT_TYPE:
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or "
                                + ThemeColumns.FONT_URI + " is not null", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            case SYSTEM_APP:
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or "
                                + ThemeColumns.HAS_THEME_PACKAGE_SCOPE + "=1", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            case LIVE_WALLPAPER_TYPE:
                return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.LIVE_WALLPAPER_URI + " IS NOT NULL", null,
                        ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
            default:
                break;
        }
        return wrapperManagedQuery(context, ThemeColumns.CONTENT_PLURAL_URI, null, null, null,
                ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ThemeItem getCurrentlyAppliedItem(Context context) {
        return Themes.getAppliedTheme(context);
    }

    @Override
    protected void onAllocInternal(Cursor c) {
        mDAOItem = new ThemeItem(c);
    }

    /**
     * @deprecated use {@link #getDAOItem(int)}.
     */
    public ThemeItem getTheme(int position) {
        return getDAOItem(position);
    }

    public int findItem(CustomTheme theme) {
        if (theme == null)
            return -1;
        int n = getCount();
        while (n-- > 0) {
            ThemeItem item = getDAOItem(n);
            if (item.equals(theme) == true) {
                return n;
            }
        }
        return -1;
    }

    // add by Fan.Yang, kill theme provider process will cause cursor closed
    /*
    * @author Fan.Yang ,杀掉theme provider进程会导致cursor关闭，theme chooser FC，
    * Actvity performRestart()时requery cursor 抛出异常
    * */
    static class ThemeCursorWrapper extends CursorWrapper {

        //Cursor cursor;
        Activity context;
        Uri uri;
        String[] projection;
        String selection;
        String[] selectionArgs;
        String sortOrder;
        // hold reference to notify cursor changed
        CursorAdapter cursorAdapter;

        ThemeCursorWrapper(Activity context, Cursor cursor, Uri uri,
                String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            super(cursor);
            //this.cursor = cursor;
            this.context = context;
            this.uri = uri;
            this.projection = projection;
            this.selection = selection;
            this.selectionArgs = selectionArgs;
            this.sortOrder = sortOrder;
        }

        public void setCursorAdapter(CursorAdapter cursorAdapter) {
            this.cursorAdapter = cursorAdapter;
        }

        @Override
        public boolean requery() {
            if (!getWrappedCursor().requery() || getWrappedCursor().isClosed()) {
                Cursor swappedCursor = swapCursor();
                if (swappedCursor != null) {
                    return swappedCursor.requery();
                }
                if(DEBUG){
                    Log.e("simply","ERROR requery, get swapped cursor is null, return old cursor!!!");
                }
                return getWrappedCursor().requery();
            }
            return getWrappedCursor().requery();
        }

        private Cursor swapCursor() {
            if (cursorAdapter != null) {
                Cursor cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, sortOrder);
                ThemeCursorWrapper cursorWrapper = new ThemeCursorWrapper(context, cursor, uri,
                        projection, selection, selectionArgs, sortOrder);
                cursorWrapper.cursorAdapter = cursorAdapter;
                Cursor old = cursorAdapter.swapCursor(cursorWrapper);
                if(DEBUG){
                    Log.d("simply","///////////old:"+old);
                }
                if (old != null && old instanceof ThemeCursorWrapper) {
                    //((ThemeCursorWrapper) old).cursorAdapter = null;
                    //((ThemeCursorWrapper) old).context = null;
                    return cursorWrapper;
                }
            } else {
                if(DEBUG){
                    Log.d("simply", "///////////cursorAdapter:" + cursorAdapter);
                }
            }
            return null;
        }
    }
}
