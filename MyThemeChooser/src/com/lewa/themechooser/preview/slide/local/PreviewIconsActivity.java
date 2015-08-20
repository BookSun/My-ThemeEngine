package com.lewa.themechooser.preview.slide.local;

//import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
//import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.themechooser.*;
import com.lewa.themechooser.custom.preview.local.DeskTopWallpaperPreview;
import com.lewa.themechooser.custom.preview.local.FontsPreview;
import com.lewa.themechooser.custom.preview.local.IconPreview;
import com.lewa.themechooser.custom.preview.local.LockScreenStylePreview;
import com.lewa.themechooser.custom.preview.local.LockScreenWallpaperPreview;
import com.lewa.themechooser.custom.preview.local.SystemAppPreview;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themechooser.widget.LewaGallery;
import com.lewa.themechooser.widget.LewaGallery.OnItemSelectedListener;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import lewa.bi.BIAgent;
import lewa.os.FileUtilities;
import util.ThemeUtil;

import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBar.LayoutParams;
import lewa.support.v7.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBarActivity;

public class PreviewIconsActivity extends ActionBarActivity implements OnItemSelectedListener, OnClickListener {
    public static final int SHOW_DELETE_DIALOG = 3;
    private static final String TAG = "PreviewIconsActivity";
    private static final int DIALOG_APPLY = 0;
    protected final ChangeThemeHelper mChangeHelper = new ChangeThemeHelper(this, DIALOG_APPLY);
    private static final int DELAY_FOR_CLICK = 3;
    protected ThemeItem mThemeItem;
    protected LinearLayout ll_gallery;
    protected LinearLayout allapp_bottom_layout;
    protected LayoutInflater mInflater;
    protected ArrayList<ImageView> indicatorViews = new ArrayList<ImageView>();
    protected ImageView tempView;
    protected ActionBar mActionBar;
    protected ImageAdapter adapter;
    protected ArrayList<String> uri_list;
    protected List<String> sub_uri_list;
    protected MenuItem themeinfo;
    protected MenuItem apply;
    protected MenuItem delete;
    protected Uri mThemeUri;
    protected ThemeStatus mThemeStatus;
    private LewaGallery gallery;
    private ActionBar.LayoutParams params;
    private int next_count;
    private Bundle bundle;
    private boolean isFromFileManager;
    private String iconPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) &&
                getIntent().getType().equals("vnd.lewa.cursor.dir/theme")) {
            Intent i = new Intent(this, com.lewa.themechooser.receiver.ThemeInstallService.class);
            i.putExtra("THEME_PACKAGE", getIntent().getData().toSafeString());
            i.putExtra("isFromFileManager", true);
            startService((i));
            finish();
            return;
        } else if (Intent.ACTION_VIEW.equals(getIntent().getAction()) &&
                (getIntent().getType().equals("vnd.lewa.cursor.dir/deskicon") ||
                        getIntent().getType().equals("vnd.lewa.cursor.dir/lockicon"))) {
            try {
                iconPath = java.net.URLDecoder.decode(getIntent().getData().toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        setContentView(R.layout.theme_preview_icons);
        mThemeStatus = ThemeApplication.sThemeStatus;
        mInflater = LayoutInflater.from(this);
        View actionbar_title = mInflater.inflate(R.layout.actionbar_title, null);
        params = new ActionBar.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.RIGHT;
        params.rightMargin = 0;
        mActionBar = getSupportActionBar();
        mActionBar.setCustomView(actionbar_title, params);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);
        gallery = (LewaGallery) findViewById(R.id.gallery);
        ll_gallery = (LinearLayout) findViewById(R.id.ll_gallery);
        allapp_bottom_layout = (LinearLayout) findViewById(R.id.allapp_bottom_layout);
        mChangeHelper.dispatchOnCreate();
        bundle = getIntent().getBundleExtra("extras_themes_uri");
        if (bundle != null) {
            uri_list = bundle.getStringArrayList("themes_uri");
        }
        isFromFileManager = getIntent().getBooleanExtra("isFromFileManager", false);
        if (mThemeUri == null) {
            mThemeUri = getIntent().getData();
        }

        if (iconPath != null) {
            Cursor c = getContentResolver().query(ThemeColumns.CONTENT_PLURAL_URI, null,
                    ThemeColumns.LOCK_WALLPAPER_URI + "=?", new String[] { iconPath }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String pkgName = c.getString(c.getColumnIndex(ThemeColumns.THEME_PACKAGE));
                String themeId = c.getString(c.getColumnIndex(ThemeColumns.THEME_ID));
                mThemeUri = Themes.getThemeUri(this, pkgName, themeId);
            } else {
                c = getContentResolver().query(ThemeColumns.CONTENT_PLURAL_URI, null,
                        ThemeColumns.WALLPAPER_URI + "=?", new String[] { iconPath }, null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    String pkgName = c.getString(c.getColumnIndex(ThemeColumns.THEME_PACKAGE));
                    String themeId = c.getString(c.getColumnIndex(ThemeColumns.THEME_ID));
                    mThemeUri = Themes.getThemeUri(this, pkgName, themeId);
                }
            }
            if (c != null) {
                c.close();
            }
        }
        if (bundle != null) {
            String uriString = mThemeUri.toSafeString();
            try {
                uriString = java.net.URLDecoder.decode(uriString, "UTF-8");
            } catch (Exception e) {
            }
            int position = uri_list.indexOf(uriString.startsWith("file://")
                    ? uriString.substring(7, uriString.length()) : uriString);
            sub_uri_list = uri_list.subList(position + 1, uri_list.size());
        }
        mThemeItem = ThemeItem.getInstance(this, mThemeUri);
        mActionBar.setTitle(getThemeName());
        adapter = initAdapter();
        if (adapter == null) {
            Intent intent = new Intent(getApplicationContext(), ThemeChooser.class);
            startActivity(intent);
            finish();
            return;
        }
        initIndicator();
        gallery.setAdapter(adapter);
        gallery.setOnItemSelectedListener(this);
        if (adapter.getCount() > 1) {
            gallery.setSelection(1);
        } else if (adapter.getCount() == 1) {
            gallery.setSelection(0);
        } else {
            gallery.setVisibility(View.INVISIBLE);
        }
        Map<String, String> map = new HashMap<String, String>();
        if (mThemeItem != null) {
            map.put(mThemeItem.getPackageName(), "local");
            //BIAgent.onEvent(this, "theme_preview", map);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mThemeItem == null) {
            mThemeItem = ThemeItem.getInstance(this, mThemeUri);
        }
    }

    protected String getThemeName() {
        if (mThemeItem != null) {
            if (this instanceof DeskTopWallpaperPreview) {
                return getString(R.string.theme_model_wallpaper);
            } else if (this instanceof LockScreenWallpaperPreview) {
                return getString(R.string.lockscreen_wallpaper);
            } else {
                return mThemeItem.getName();
            }

        } else {
            return null;
        }
    }

    protected String getThemePackageName() {
        return mThemeItem.getPackageName();
    }

    protected boolean isThemeApplied() {
        if (mThemeItem == null)
            return false;
        return mThemeStatus.isApplied(mThemeItem.getPackageName());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onStop() {
        if (null != mThemeItem)
            mThemeItem.close();
        mThemeItem = null;
        gallery.setOnItemSelectedListener(null);
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.themeinfo) {
            if (this instanceof LocalPreviewIconsActivity) {
                Intent intent = new Intent();
                intent.setClass(this, ThemeInfoActivity.class);
                intent.setData(mThemeUri);
                startActivity(intent);
            }
        } else if (item.getItemId() == R.id.apply) {
            boolean reboot = getResources().getBoolean(R.bool.config_font_reboot);
            if ((this instanceof FontsPreview || this instanceof LocalPreviewIconsActivity)
                    && (mThemeItem == null ? false :
                    mThemeItem.getFontUril() != null || "com.lewa.theme.LewaDefaultTheme"
                            .equals(mThemeItem == null ? null : mThemeItem.getPackageName()))) {
                ThemeUtil.isChangeFont = true;
            }
            if (ThemeUtil.isChangeFont && reboot) {
                new AlertDialog.Builder(this)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.font_change_reboot)
                        .setPositiveButton(R.string.apply_reboot,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mChangeHelper.beginChange(getThemeName());
                                        doApply(mThemeItem);
                                        //BIAgent.onEvent(PreviewIconsActivity.this, "theme_change", getThemePackageName());
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null).show();
            } else {
                mChangeHelper.beginChange(getThemeName());
                doApply(mThemeItem);
                //BIAgent.onEvent(this, "theme_change", getThemePackageName());
            }
        } else if (item.getItemId() == R.id.delete) {
            if (isThemeApplied()) {
                Toast.makeText(this
                        , getDeleteUsingThemeToastMessage(), Toast.LENGTH_SHORT).show();
                return true;
            }

            if (mThemeItem != null && !mThemeItem.isRemovable()) {
                Toast.makeText(this
                        , getString(R.string.delete_system_theme), Toast.LENGTH_SHORT).show();
                return true;
            }
            showDialog(SHOW_DELETE_DIALOG);
        }
        return true;
    }

    protected ImageAdapter initAdapter() {
        return new ImageAdapter(this, mThemeItem);
    }

    protected boolean isThemeItemAvailable(ThemeItem themeItem,  PreviewsType type) {
        Context context = getApplicationContext();
        List<Uri> list = NewMechanismHelp.getPreviews(context, themeItem, type);;
        boolean result = true;
        for (int i = 0; list != null && i < list.size(); i++) {
            InputStream is = null;
            try {
                is = context.getContentResolver().openInputStream(list.get(i));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                result = false;
            } finally {
                if (is != null)
                    FileUtilities.close(is);
            }
        }
        return result;
    }

    protected boolean isThemeUriAvailable(Uri themeUri) {
        InputStream is = null;
        try {
            is = getApplicationContext().getContentResolver().openInputStream(themeUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (is != null)
                FileUtilities.close(is);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mChangeHelper.dispatchOnResume();
        gallery.setOnItemSelectedListener(this);
    }

    @Override
    protected void onPause() {
        if (!(this instanceof FontsPreview))
            mChangeHelper.dispatchOnPause();
        super.onPause();
    }

    protected String getDeleteToast() {
        return getString(R.string.theme_delete_success);
    }

    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme);
    }

    protected void doDeleteTheme() {
        PackageDeleteObserver observer = new PackageDeleteObserver();
        try {
            getPackageManager().deletePackage(mThemeItem.getPackageName(), observer, 0);
        } catch (Exception e) {
            // Log.e(TAG, e.getMessage());
        }
        ThemeUtil.deleteThemeInfo(this, mThemeItem);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case SHOW_DELETE_DIALOG: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                if (this instanceof DeskTopWallpaperPreview) {
                    builder.setTitle(R.string.deskwallpaper_delete_title);
                    builder.setMessage(getString(R.string.deskwallpaper_delete));
                } else if (this instanceof LockScreenWallpaperPreview) {
                    builder.setTitle(R.string.lockwallpaper_delete_title);
                    builder.setMessage(getString(R.string.lockwallpaper_delete));
                } else if (this instanceof IconPreview) {
                    builder.setTitle(R.string.icon_delete_title);
                    builder.setMessage(getString(R.string.icon_delete_message));
                } else if (this instanceof SystemAppPreview) {
                    builder.setTitle(R.string.systemapp_delete_title);
                    builder.setMessage(getString(R.string.systemapp_delete_message));
                } else if (this instanceof FontsPreview) {
                    builder.setTitle(R.string.fonts_delete_title);
                    builder.setMessage(getString(R.string.fonts_delete_message));
                } else if (this instanceof LockScreenStylePreview) {
                    builder.setTitle(R.string.lockscreen_style_delete_title);
                    builder.setMessage(getString(R.string.lockscreen_style_delete_message));
                } else {
                    builder.setTitle(R.string.theme_delete_title);
                    builder.setMessage(getString(R.string.theme_delete));
                }
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doDeleteTheme();
                                finish();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                return builder.create();
            }
            default:
                return mChangeHelper.dispatchOnCreateDialog(id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        mChangeHelper.dispatchOnPrepareDialog(id, dialog);
    }

    protected void initIndicator() {
        allapp_bottom_layout.removeAllViews();
        indicatorViews.clear();
        for (int i = 0; i < adapter.getCount(); i++) {
            View iView = mInflater.inflate(R.layout.allapp_indicator, null);
            ImageView addView = (ImageView) iView.findViewById(R.id.allapp_indicator_children);
            allapp_bottom_layout.addView(iView);
            indicatorViews.add(addView);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preview_icons_option, menu);
        themeinfo = menu.findItem(R.id.themeinfo);
        apply = menu.findItem(R.id.apply);
        delete = menu.findItem(R.id.delete);
        themeinfo.setVisible(false);
        if (isFromFileManager) {
            themeinfo.setVisible(false);
            //delete.setVisible(false);
        }
        String pkg = null;
        if (mThemeItem != null) {
            pkg = mThemeItem.getPackageName();
        }
        if (mThemeItem != null && (pkg.equals("com.lewa.theme.LewaDefaultTheme") ||
                !mThemeItem.isRemovable())) {
            delete.setEnabled(false);
            delete.setVisible(false);
        }
        if (!(this instanceof LocalPreviewIconsActivity)) {
            if (mThemeItem != null && (pkg.equals("com.lewa.theme.LewaDefaultTheme") ||
                    !mThemeItem.isRemovable())) {
                apply.setIcon(R.drawable.ic_menu_done);
                apply.setTitle(R.string.apply);
                delete.setVisible(false);
                themeinfo.setVisible(false);
            } else {
                themeinfo.setEnabled(false);
                themeinfo.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public void onItemSelected(View view, int position) {
        if (tempView != null) {
            tempView.setImageResource(R.drawable.indicator_normal);
        }
        if (position >= indicatorViews.size() || position < 0) {
            return;
        }
        tempView = indicatorViews.get(position);
        tempView.setImageResource(R.drawable.indicator_highlight);
        adapter.setSelectItem(position);
    }

    protected void doApply(ThemeItem bean) {
        Uri uri = bean.getUri(this);
        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        ThemeUtil.updateCurrentThemeInfo(bean);
        if (mThemeItem.hasThemePackageScope()) {
            i.putExtra(ThemeManager.EXTRA_SYSTEM_APP, true);
            ThemeUtil.isKillProcess = true;
        }
        if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                && mThemeItem.getThemeId().equals("LewaDefaultTheme")) {
            if (ThemeApplication.sThemeStatus.isApplied("com.lewa.theme.LewaDefaultTheme",
                    com.lewa.themechooser.ThemeStatus.THEME_TYPE_FONT) &&
                    ThemeApplication.sThemeStatus.isApplied("com.lewa.theme.LewaDefaultTheme",
                            com.lewa.themechooser.ThemeStatus.THEME_TYPE_STYLE)) {
                ThemeUtil.isKillProcess = false;
            } else {
                ThemeUtil.isKillProcess = true;
            }

            i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, true);
            i.putExtra(ThemeManager.DEFAULT_FONT, true);
            i.putExtra(ThemeManager.DEFAULT_ICON, true);
            i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_WALLPAPER, true);
        } else if (ThemeManager.STANDALONE &&
                mThemeItem.getPackageName().equals(ThemeManager.THEME_ELEMENTS_PACKAGE)) {
            i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_STYLE, true);
            i.putExtra(ThemeManager.DEFAULT_ICON, true);
            i.putExtra(ThemeManager.DEFAULT_LOCKSCREEN_WALLPAPER, true);
        }

        mThemeStatus.setAppliedPkgName(mThemeItem.getPackageName(), ThemeStatus.THEME_TYPE_PACKAGE);
        //#67068 add begin by bin.dong
        if (mThemeItem.getWallpaperUri(this) != null) {
            Settings.System.putString(getContentResolver(), "lewa_wallpaper_path",
                    mThemeItem.getWallpaperUri(this).getPath().toString());
        }
        //#67068 add end by bin.dong
        ApplyThemeHelp.changeTheme(this, i);

        if (mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")) {
            mThemeStatus
                    .setAppliedPkgName(mThemeItem.getPackageName(), ThemeStatus.THEME_TYPE_ICONS);
            mThemeStatus
                    .setAppliedPkgName(mThemeItem.getPackageName(), ThemeStatus.THEME_TYPE_FONT);
            mThemeStatus.setAppliedPkgName(mThemeItem.getPackageName(),
                    ThemeStatus.THEME_TYPE_LOCK_SCREEN);
            mThemeStatus
                    .setAppliedPkgName(mThemeItem.getPackageName(), ThemeStatus.THEME_TYPE_STYLE);
            mThemeStatus.setAppliedPkgName(mThemeItem.getPackageName(),
                    ThemeStatus.THEME_TYPE_WALLPAPER);
            mThemeStatus.setAppliedPkgName(mThemeItem.getPackageName(),
                    ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
        } else {
            if (mThemeItem.getIconsUri() != null) {
                mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(
                        this, bean, PreviewsType.LAUNCHER_ICONS), ThemeStatus.THEME_TYPE_ICONS);
            }
            if (mThemeItem.getWallpaperUri(this) != null) {
                mThemeStatus.setAppliedThumbnail(mThemeItem.getWallpaperUri(this)
                        , ThemeStatus.THEME_TYPE_WALLPAPER);
            }
            if (mThemeItem.getFontUril() != null) {
                mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(
                        this, bean, PreviewsType.FONTS), ThemeStatus.THEME_TYPE_FONT);
            }
            if (mThemeItem.getLockscreenUri() != null) {
                if (mThemeItem.getPackageName().equals(ThemeManager.THEME_ELEMENTS_PACKAGE)) {
                    mThemeStatus.setAppliedThumbnail(ThemeManager.THEME_LOCKSCREEN2_PACKAGE,
                            ThemeStatus.THEME_TYPE_LOCK_SCREEN);
                } else {
                    mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(
                                    this, bean, PreviewsType.LOCKSCREEN),
                            ThemeStatus.THEME_TYPE_LOCK_SCREEN);
                }
                mThemeStatus.setAppliedThumbnail("", ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
            }
            if (mThemeItem.getLockWallpaperUri(this) != null) {
                mThemeStatus.setAppliedThumbnail(mThemeItem.getLockWallpaperUri(this)
                        , ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
                mThemeStatus.setAppliedPkgName(mThemeItem.getPackageName(),ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
            }
            if (mThemeItem.hasThemePackageScope()) {
                mThemeStatus.setAppliedThumbnail(NewMechanismHelp.getApplyThumbnails(
                        this, bean, PreviewsType.FRAMEWORK_APPS), ThemeStatus.THEME_TYPE_STYLE);
            }
            mThemeStatus.setAppliedPkgName(mThemeItem.getPackageName(),ThemeStatus.THEME_TYPE_STYLE);
        }
    }

    private void showMenu() {
        if (delete == null || apply == null || themeinfo == null) {
            return;
        }
        if (mThemeItem != null &&
                !mThemeItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                && mThemeItem.isRemovable()) {
            delete.setVisible(true);
            delete.setEnabled(true);
        } else {
            delete.setEnabled(false);
        }
        if (!(this instanceof LocalPreviewIconsActivity)) {
            if (mThemeItem != null &&
                    ("com.lewa.theme.LewaDefaultTheme").equals(mThemeItem.getPackageName())
                    || !mThemeItem.isRemovable()) {
                delete.setVisible(false);
                apply.setIcon(R.drawable.ic_menu_done);
                themeinfo.setVisible(false);
            } else {
                themeinfo.setVisible(true);
                themeinfo.setTitle(null);
                themeinfo.setEnabled(false);
                apply.setTitle(R.string.apply);
                delete.setVisible(true);
            }
        }
    }

    @Override
    public synchronized void onClick(View v) {
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        @Override
        public void packageDeleted(String packageName, int returnCode) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PreviewIconsActivity.this, getDeleteToast(), Toast.LENGTH_SHORT)
                            .show();
                    PreviewIconsActivity.this.finish();
                }
            });
        }
    }

    public class ImageAdapter extends BaseAdapter {
        protected ImageLoader imageLoader = ImageLoader.getInstance();
        int mGalleryItemBackground;
        private Context mContext;
        private ThemeItem mThemeItem;
        private List<Uri> list;
        private int selectItem;
        private int mSpacing;
        private boolean mShowInfo;
        private PreviewsType mPreType;
        private int mPreviewHeight;
        private int mPreviewWidth;

        public ImageAdapter(Context context, ThemeItem themeItem) {
            mContext = context;
            mShowInfo = true;
            if ((mThemeItem = themeItem) != null) {
                list = NewMechanismHelp.getPreview(themeItem, context);
            }
            init(context);
        }

        public ImageAdapter(Context context, ThemeItem themeItem, PreviewsType type) {
            mContext = context;
            //TCL937553 modify by Fan.Yang
            if (type == PreviewsType.DESKWALLPAPER || type == PreviewsType.LOCKSCREEN ||
                    type == PreviewsType.DEFAULT_THEME_WALLPAPER ||
                    type == PreviewsType.LOCKWALLPAPER) {
                mPreType = type;
                mShowInfo = false;
            } else {
                mShowInfo = true;
            }
            if ((mThemeItem = themeItem) != null) {
                list = NewMechanismHelp.getPreviews(context, themeItem, type);
            }

            init(context);
        }

        public ImageAdapter(Context context, Uri previewUri) {
            mContext = context;
            mShowInfo = false;
            list = new ArrayList<Uri>(1);
            list.add(previewUri);
            init(context);
        }

        private void init(Context context) {
            Resources res = context.getResources();
            mPreviewHeight = res.getDimensionPixelSize(R.dimen.preview_icon_selected_height);
            mPreviewWidth = res.getDimensionPixelSize(R.dimen.preview_icon_selected_width);
            mSpacing = (int) res.getDisplayMetrics().density * 12;
        }

        @Override
        public int getCount() {
            if (list != null) {
                return list.size() + (mShowInfo ? 1 : 0);
            } else {
                return mShowInfo ? 1 : 0;
            }
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setSelectItem(int selectItem) {
            if (this.selectItem != selectItem) {
                this.selectItem = selectItem;
                notifyDataSetChanged();
            }
        }

        private Bitmap zoomBitmap(Bitmap bitmap, int w, int h, boolean recycleInput) {
            int width = bitmap.getWidth() / 2;
            int height = bitmap.getHeight();
            Bitmap bmp = Bitmap.createBitmap(bitmap, width / 2, 0, width, height);
            Matrix matrix = new Matrix(); // 创建操作图片用的Matrix对象
            float scaleWidth = ((float) w / width); // 计算缩放比例
            float scaleHeight = ((float) h / height);
            matrix.postScale(scaleWidth, scaleHeight); // 设置缩放比例
            Bitmap newbmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix,
                    true); // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            if (recycleInput)
                bitmap.recycle();
            bmp.recycle();
            return newbmp;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewCache viewCache = null;
            if (position != 0 || !mShowInfo) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(mContext)
                            .inflate(R.layout.theme_imageview, null);
                    viewCache = new ViewCache();
                    viewCache.thumbnail = (ImageView) convertView.findViewById(R.id.preview_icon);
                    convertView.setTag(viewCache);
                } else {
                    viewCache = (ViewCache) convertView.getTag();
                }
                if (list.size() != 0) {
                    if (!mShowInfo) {
                        Bitmap bitmap;
                        InputStream is = null;
                        try {
                            is = mContext.getContentResolver().openInputStream(list.get(position));
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeStream(is, null, options);
                            options.inSampleSize = ThemeUtil
                                    .calculateInSampleSize(options, mPreviewWidth, mPreviewHeight);
                            options.inJustDecodeBounds = false;
                            bitmap = BitmapFactory.decodeStream(mContext.getContentResolver()
                                    .openInputStream(list.get(position)), null, options);

                            if (mPreType != null && (mPreType == PreviewsType.DESKWALLPAPER ||
                                    mPreType == PreviewsType.DEFAULT_THEME_WALLPAPER)) {
                                bitmap = zoomBitmap(bitmap, mPreviewWidth, mPreviewHeight, false);
                            }
                            viewCache.thumbnail.setImageBitmap(bitmap);
                        } catch (OutOfMemoryError e) {
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        } finally {
                            if (is != null)
                                FileUtilities.close(is);
                        }
                    } else {
                        imageLoader.displayImage(list.get(position - 1).toString(),
                                viewCache.thumbnail);
                    }
                }
            } else {
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.theme_preview_icons_details, null);
                TextView tv_name = (TextView) convertView.findViewById(R.id.theme_name);
                if (mThemeItem == null) {
                    return convertView;
                }
                tv_name.setText(mThemeItem.getName());
                if (ThemeUtil.CONFIG_IS_B2B) {
                    TextView tv_author_name = (TextView) convertView
                            .findViewById(R.id.theme_author);
                    tv_author_name.setVisibility(View.GONE);
                    TextView tv_author = (TextView) convertView
                            .findViewById(R.id.theme_info_author);
                    tv_author.setText(mThemeItem.getAuthor());
                    tv_author.setVisibility(View.GONE);
                } else {
                    TextView tv_author = (TextView) convertView
                            .findViewById(R.id.theme_info_author);
                    tv_author.setText(mThemeItem.getAuthor());
                }
                TextView tv_size = (TextView) convertView.findViewById(R.id.theme_info_size);
                tv_size.setText(Formatter.formatFileSize(mContext, mThemeItem.getSize()));
                TextView tv_version = (TextView) convertView.findViewById(R.id.theme_info_version);
                tv_version.setText(mThemeItem.getVersionName());
                TextView tv_date = (TextView) convertView.findViewById(R.id.theme_info_date);
                tv_date.setVisibility(View.GONE);
                TextView tv_date_info = (TextView) convertView.findViewById(R.id.theme_date);
                tv_date_info.setVisibility(View.GONE);
                TextView tv_download_info = (TextView) convertView
                        .findViewById(R.id.theme_download_count);
                tv_download_info.setVisibility(View.GONE);
                TextView tv_download = (TextView) convertView
                        .findViewById(R.id.theme_info_download_count);
                tv_download.setVisibility(View.GONE);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (mContext.getResources()
                            .getDimension(R.dimen.preview_icon_selected_width))
                    , (int) (mContext.getResources()
                    .getDimension(R.dimen.preview_icon_selected_height)));
            if (position != getCount() - 1) {
                params.setMargins(0, 0, mSpacing, 0);
            }
            convertView.setLayoutParams(params);
            return convertView;
        }

    }

    private class ViewCache {
        public ImageView thumbnail;
    }
}
