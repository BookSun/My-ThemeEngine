package com.lewa.themechooser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;

import java.util.ArrayList;

import util.ThemeUtil;

import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBarActivity;

public class ThemeInfoActivity extends ActionBarActivity {
    private static final int DIALOG_APPLY = 0;
    protected final ChangeThemeHelper mChangeHelper = new ChangeThemeHelper(this, DIALOG_APPLY);
    private static final int APPLY_ID = 100;
    private static final int THEME_DETAIL = 7;
    public ActionBar mActionBar;
    private TextView tv_info;
    private GridView gv_info;
    private ThemeInfoAdapter mAdapter;
    private ThemeItem mThemeItem;
    private ArrayList<String> mNameList = new ArrayList();
    private ArrayList mUriList = new ArrayList();
    private ArrayList mThumbnailList = new ArrayList();
    private ArrayList mCropPreviews = new ArrayList();//0 截取上半部分  1:截取下半部分
    private boolean isSystemApp = false;
    private ThemeStatus mThemeStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeStatus = ThemeApplication.sThemeStatus;
        setContentView(R.layout.theme_info);
        mThemeItem = ThemeItem.getInstance(this, getIntent().getData());
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(true);
            mActionBar.setTitle(mThemeItem.getName());
        }

        initList();
        initViews();
        mChangeHelper.dispatchOnCreate();
    }

    private void initList() {
        mNameList.clear();
        mUriList.clear();
        isSystemApp = false;
        if (!ThemeManager.STANDALONE && mThemeItem.getLockWallpaperUri(this) != null) {
            mNameList.add(getString(R.string.lockscreen_wallpaper));
            mUriList.add(mThemeItem.getLockWallpaperUri(this));
            if (NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.LOCKSCREEN).size() != 0) {
                mThumbnailList.add(NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.LOCKSCREEN).get(0));
            } else {
                mThumbnailList.add(null);
            }
            mCropPreviews.add(1);
        }
        if (!ThemeManager.STANDALONE && mThemeItem.getLockscreenUri() != null || mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
            mNameList.add(getString(R.string.lockscreen_style));
            if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
                mUriList.add(Uri.parse("com.lewa.theme.LewaDefaultTheme"));
            } else {
                mUriList.add(mThemeItem.getLockscreenUri());
            }
            if (NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.LOCKSCREEN).size() != 0) {
                mThumbnailList.add(NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.LOCKSCREEN).get(0));
            } else {
                mThumbnailList.add(null);
            }
            mCropPreviews.add(1);
        }
        if (mThemeItem.getIconsUri() != null || mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
            mNameList.add(getString(R.string.custom_icon));
            if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
                mUriList.add(Uri.parse("com.lewa.theme.LewaDefaultTheme"));
            } else {
                mUriList.add(mThemeItem.getIconsUri());
            }
            if (NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.LAUNCHER_ICONS) != null && NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.LAUNCHER_ICONS).size() != 0) {
                mThumbnailList.add(NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.LAUNCHER_ICONS).get(0));
            } else {
                mThumbnailList.add(null);
            }
            mCropPreviews.add(1);
        }
        if (mThemeItem.getWallpaperUri(this) != null) {
            mNameList.add(getString(R.string.desktop_wallpaper));
            mUriList.add(mThemeItem.getWallpaperUri(this));
            mThumbnailList.add(mThemeItem.getWallpaperUri(this));
            mCropPreviews.add(2);
        }
        if (mThemeItem.hasThemePackageScope()) {
            mNameList.add(getString(R.string.system_app));
            isSystemApp = true;
            mUriList.add(Uri.parse("true"));
            if (NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.FRAMEWORK_APPS).size() != 0) {
                mThumbnailList.add(NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.FRAMEWORK_APPS).get(0));
            } else {
                mThumbnailList.add(null);
            }
            mCropPreviews.add(0);
        }
        if (mThemeItem.getFontUril() != null || mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
            mNameList.add(getString(R.string.custom_font));
            if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
                mUriList.add(Uri.parse("com.lewa.theme.LewaDefaultTheme"));
            } else {
                mUriList.add(mThemeItem.getFontUril());
            }
            if (NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.FONTS).size() != 0) {
                mThumbnailList.add(NewMechanismHelp.getPreviews(this, mThemeItem, PreviewsType.FONTS).get(0));
            } else {
                mThumbnailList.add(null);
            }
            mCropPreviews.add(0);
        }
        ThemeUtil.updateCurrentThemeInfo(mThemeItem);
    }

    private void initViews() {
        tv_info = (TextView) findViewById(R.id.tv_info);
        gv_info = (GridView) findViewById(R.id.gv_info);
        tv_info.setText(getString(R.string.theme_count, mNameList.size()));
        mAdapter = new ThemeInfoAdapter(mNameList, mUriList, this, mThemeItem, mThumbnailList, mCropPreviews);
        gv_info.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mChangeHelper.dispatchOnResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    // Woody Guo @ 2012/09/13: Call ThemeItem.close() to close the cursor
    @Override
    public void onDestroy() {
        if (null != mThemeItem) {
            mThemeItem.close();
            mThemeItem = null;
        }
        super.onDestroy();
    }

    protected void onPrepareDialog(int id, Dialog dialog) {
        mChangeHelper.dispatchOnPrepareDialog(id, dialog);
    }

    protected Dialog onCreateDialog(int id, Bundle bundle) {
        return mChangeHelper.dispatchOnCreateDialog(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, APPLY_ID, 0, R.string.apply)
                .setIcon(R.drawable.ic_menu_done)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    protected void onPause() {
        mChangeHelper.dispatchOnPause();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == APPLY_ID) {
            if (mAdapter.mHashMap.size() == 0) {
                Toast.makeText(this, getString(R.string.no_modules_to_apply), Toast.LENGTH_SHORT).show();
                return true;
            }
            final ThemeItem appliedTheme = Themes.getAppliedTheme(this);
            if (null == appliedTheme) {
                return true;
            }
            Uri font_uri = mAdapter.mHashMap.get(getString(R.string.custom_font));
            boolean reboot = getResources().getBoolean(R.bool.config_font_reboot);
            ThemeUtil.isChangeFont = (font_uri != null);
            if (reboot && ThemeUtil.isChangeFont) {
                new AlertDialog.Builder(this)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.font_change_reboot)
                        .setPositiveButton(R.string.apply_reboot, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                applyThemeInternal(appliedTheme);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null).show();
            } else {
                applyThemeInternal(appliedTheme);
            }
        }
        return true;
    }

    private void applyThemeInternal(ThemeItem appliedTheme) {
        mChangeHelper.beginChange(mThemeItem.getName());
        Uri uri = null;
        Uri icon_uri = mAdapter.mHashMap.get(getString(R.string.custom_icon));
        Uri system_app_uri = mAdapter.mHashMap.get(getString(R.string.system_app));
        Uri deskTopWallpaper_uri = mAdapter.mHashMap.get(getString(R.string.desktop_wallpaper));
        Uri font_uri = mAdapter.mHashMap.get(getString(R.string.custom_font));
        Uri lockscreen_uri = mAdapter.mHashMap.get(getString(R.string.lockscreen_style));
        Uri LockScreenWallpaper_uri = mAdapter.mHashMap.get(getString(R.string.lockscreen_wallpaper));
        uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
        appliedTheme.close();
        Intent i;

        if (system_app_uri != null) {
            ThemeUtil.isKillProcess = true;
            if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                    && mThemeItem.getThemeId().equals("LewaDefaultTheme")) {
                if (ThemeApplication.sThemeStatus.isApplied("com.lewa.theme.LewaDefaultTheme"
                        , com.lewa.themechooser.ThemeStatus.THEME_TYPE_STYLE)) {
                    ThemeUtil.isKillProcess = false;
                } else {
                    ThemeUtil.isKillProcess = true;
                }
            }
            i = new Intent(ThemeManager.ACTION_CHANGE_THEME, mThemeItem.getUri(this));
            i.putExtra(ThemeManager.EXTRA_SYSTEM_APP, true);
            mThemeStatus.setAppliedThumbnail(ThemeUtil.parseSafeUri(NewMechanismHelp.getThumbnails(
                    this, appliedTheme, PreviewsType.FRAMEWORK_APPS)), ThemeStatus.THEME_TYPE_STYLE);
        } else {
            i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        }
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);

        if (icon_uri != null) {
            if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme") && mThemeItem.getThemeId().equals("LewaDefaultTheme")) {
                i.putExtra(ThemeManager.DEFAULT_ICON, true);
            }
            ThemeUtil.isChangeIcon = true;
            i.putExtra(ThemeManager.EXTRA_ICONS_URI, icon_uri);
            mThemeStatus.setAppliedThumbnail(ThemeUtil.parseSafeUri(NewMechanismHelp.getThumbnails(
                    this, appliedTheme, PreviewsType.LAUNCHER_ICONS)), ThemeStatus.THEME_TYPE_ICONS);
        }
        if (deskTopWallpaper_uri != null) {
            i.putExtra(ThemeManager.EXTRA_WALLPAPER_URI, deskTopWallpaper_uri);
            mThemeStatus.setAppliedThumbnail(ThemeUtil.parseSafeUri(mThemeItem.getWallpaperUri(
                    this)), ThemeStatus.THEME_TYPE_WALLPAPER);
            ThemeApplication.sThemeStatus.setAppliedPkgName(
                    null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
        }
        if (font_uri != null) {
            if (font_uri.toString().equals(Uri.parse("com.lewa.theme.LewaDefaultTheme").toString())) {
                i.putExtra(ThemeManager.DEFAULT_FONT, true);
                ThemeUtil.isKillProcess = true;
                if (ThemeApplication.sThemeStatus.isApplied("com.lewa.theme.LewaDefaultTheme",
                        com.lewa.themechooser.ThemeStatus.THEME_TYPE_STYLE)) {
                    ThemeUtil.isKillProcess = false;
                } else {
                    ThemeUtil.isKillProcess = true;
                }
            } else {
                i.putExtra(ThemeManager.EXTRA_FONT_URI, font_uri);
            }
            mThemeStatus.setAppliedThumbnail(ThemeUtil.parseSafeUri(NewMechanismHelp.getThumbnails(
                    this, appliedTheme, PreviewsType.FONTS)), ThemeStatus.THEME_TYPE_FONT);
        }
        if (lockscreen_uri != null) {
            if (lockscreen_uri.toString().equals(Uri.parse("com.lewa.theme.LewaDefaultTheme").toString())) {
                i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, true);
            } else {
                i.putExtra(ThemeManager.EXTRA_LOCKSCREEN_URI, lockscreen_uri);
            }

            mThemeStatus.setAppliedThumbnail(ThemeUtil.parseSafeUri(NewMechanismHelp.getThumbnails(
                    this, appliedTheme, PreviewsType.LOCKSCREEN)), ThemeStatus.THEME_TYPE_LOCK_SCREEN);
            mThemeStatus.setAppliedThumbnail("", ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        }
        if (LockScreenWallpaper_uri != null) {
            if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme") && mThemeItem.getThemeId().equals("LewaDefaultTheme")) {
                i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_WALLPAPER, true);
            }
            i.putExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI
                    , mAdapter.mHashMap.get(getString(R.string.lockscreen_wallpaper)));
            mThemeStatus.setAppliedThumbnail(ThemeUtil.parseSafeUri(mThemeItem
                    .getLockscreenUri()), ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        }
        i.putExtra(CustomType.EXTRA_NAME, CustomType.THEME_DETAIL);

        //mark this theme as applied
        if (mAdapter.mHashMap.size() == mUriList.size()) {
            mThemeStatus.setAppliedPkgName(
                    mThemeItem.getPackageName(), ThemeStatus.THEME_TYPE_PACKAGE);
        }
        ApplyThemeHelp.applyInternal(this, i, mThemeItem);
    }
}
