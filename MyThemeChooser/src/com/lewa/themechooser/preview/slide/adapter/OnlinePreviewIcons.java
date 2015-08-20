package com.lewa.themechooser.preview.slide.adapter;

//import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.LewaDownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.Downloads;
import android.text.TextUtils;
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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.themechooser.ChangeThemeHelper;
import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeChooser;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeInfoActivity;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.adapters.online.ThumbnailOnlineAdapter;
import com.lewa.themechooser.custom.preview.online.OnLineDeskTopWallpaperPreview;
import com.lewa.themechooser.custom.preview.online.OnLineFontsPreview;
import com.lewa.themechooser.custom.preview.online.OnLineLockScreenWallpaperPreview;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.preview.slide.online.PreviewOnlineIconsActivity;
import com.lewa.themechooser.server.intf.ClientResolver;
import com.lewa.themechooser.server.intf.UrlParam;
import com.lewa.themechooser.server.intf.jsonimpl.LewaServerJsonParser;
import com.lewa.themechooser.widget.LewaGallery;
import com.lewa.themechooser.widget.LewaGallery.OnItemSelectedListener;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import lewa.bi.BIAgent;
import util.ThemeUtil;

import static com.lewa.themes.ThemeManager.STANDALONE;

import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBar.TabListener;
import lewa.support.v7.app.ActionBarActivity;

public abstract class OnlinePreviewIcons extends ActionBarActivity
        implements OnClickListener, OnItemSelectedListener {
    protected static final int SHOWLOAD = 0;
    protected static final int SHOWLOADING = 1;
    protected static final int SHOWLOADED = 2;
    protected static final int SHOW_DELETE_DIALOG = 30;
    protected static final int DELETE_THEME = 20;
    protected static final int DOWNLOAD = 0;
    protected static final int CANCEL = 1;
    protected static final int APPLY = 2;
    private static final String TAG = "OnlinePreviewIcons";
    private static final boolean DBG = true;
    private static final int DIALOG_APPLY = 0;
    protected final ChangeThemeHelper mChangeHelper = new ChangeThemeHelper(this, DIALOG_APPLY);
    private static final int SHOWLOADING_MSG = 100;
    private static final int SHOWLOADED_MSG = 200;
    private static final int ACTION_PROGRESSBAR_UPDATEING = 300;
    private static final int DELAY_FOR_CLICK = 400;
    private static final int SHOW_DOWNLOAD_FAILED = 500;
    protected String PREFS_NAME = "CUSTOM_URI";
    protected ThemeBase mThemeBase;
    protected ArrayList<ThemeBase> mThemeBases;
    protected List<ThemeBase> sub_themeBases = null;
    protected LinearLayout allapp_bottom_layout;
    protected RelativeLayout rl;
    protected LayoutInflater mInflater;
    protected ArrayList<ImageView> indicatorViews = new ArrayList<ImageView>();
    protected ImageView tempView;
    protected ActionBar mActionBar;
    protected ImageAdapter adapter;
    protected int size;
    protected boolean isDownloading = false;
    protected StorageManager mStorageManager;
    protected boolean mSDCardMounted = true;
    protected boolean mSDCard2Mounted = false;
    protected String mSDCardPath = null;
    protected String mSDCard2Path = null;
    protected AlertDialog alertDialog = null;
    protected ProgressBar mpDialog;
    protected LinearLayout mLinearLayoutLoading;
    protected LinearLayout mloadingprogressbar;
    protected TextView mPerTextView;
    protected long containPkg;

    protected Handler mHandler;
    protected int mThemeType;
    protected ThemeStatus mThemeStatus;
    protected long mDownloadId;
    private LewaGallery gallery;
    private String installThemeName;
    private int button_status;
    private ActionBar.LayoutParams params;
    private int next_count;
    private MenuItem themeinfo;
    private MenuItem apply;
    private MenuItem delete;
    private LewaDownloadManager manager;
    private DownloadsChangeObserver mObserver = new DownloadsChangeObserver();
    private int mCurrentSelection;
    private float mLastX;
    private ClientResolver clientResolver;
    private boolean isFromOtherApp = false;
    private String themeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeStatus = ThemeApplication.sThemeStatus;
        setContentView(R.layout.theme_preview_icons);
        mInflater = LayoutInflater.from(this);
        View actionbar_title = mInflater.inflate(R.layout.actionbar_title, null);
        params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.RIGHT;
        params.rightMargin = 0;
        mActionBar = getSupportActionBar();
        mActionBar.setCustomView(actionbar_title, params);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);
        gallery = (LewaGallery) findViewById(R.id.gallery);
        manager = LewaDownloadManager.getInstance(getContentResolver(), getPackageName());
        //manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        allapp_bottom_layout = (LinearLayout) findViewById(R.id.allapp_bottom_layout);
        mpDialog = (ProgressBar) findViewById(R.id.online_load_progress);
        mLinearLayoutLoading = (LinearLayout) findViewById(R.id.online_theme_bottom_bar_loading);
        mPerTextView = (TextView) findViewById(R.id.online_percent);
        mHandler = new HandlerAll(this);
        if ("com.lewa.downloadlwt.action".equals(getIntent().getAction())
                || "com.lewa.downloadwallpaper.action".equals(getIntent().getAction())
                || "com.lewa.downloadlockwallpaper.action".equals(getIntent().getAction())) {
            mloadingprogressbar = (LinearLayout) findViewById(R.id.theme_online_loadingprogressbar);
            mloadingprogressbar.setVisibility(View.VISIBLE);
            themeId = getIntent().getStringExtra("themeId");
            clientResolver = new ClientResolver(UrlParam.newUrlParam(themeId, true),
                    ClientResolver.JSON_IMPL, ClientResolver.DEFAULT_PAGE_SIZE);
            LoadTheme mlp = new LoadTheme();
            isFromOtherApp = true;
            mLinearLayoutLoading.setVisibility(View.INVISIBLE);
            mlp.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        if (!isFromOtherApp) {
            if ("com.lewa.downloading.action".equals(getIntent().getAction()) && !ThemeManager.STANDALONE) {
                String str = getIntent().getStringExtra(LewaDownloadManager.EXTRA_DOWNLOAD_USER_EXTRA);
                List<ThemeBase> list = LewaServerJsonParser.parseListThemeBase(str, false);
                mThemeBase = list.get(0);
            } else {
                mThemeBase = (ThemeBase) getIntent().getSerializableExtra(ThemeConstants.THEMEBASE);
            }

            //RC51125 FC when open themechooer by birdview second times, add by Jianwu Gao ,begin
            if (null != mThemeBase) {
                //RC51125 FC when open themechooer by birdview second times, add by Jianwu Gao ,end
                String mFileName = mThemeBase.getPkg();
                mDownloadId = mThemeStatus.getDownloadId(mThemeBase.getPackageName(),
                        mFileName, mThemeType);
                mThemeBases = ThumbnailOnlineAdapter.tempThemeBases;
                ThumbnailOnlineAdapter.tempThemeBases = null;
                // end
                int position = -1;
                if (mThemeBases != null) {
                    for (int i = 0; i < mThemeBases.size(); i++) {
                        ThemeBase temp_theme = mThemeBases.get(i);
                        if (temp_theme.getPackageName().equals(mThemeBase.getPackageName())
                                && temp_theme.getThemeId().equals(mThemeBase.getThemeId())) {
                            position = i;
                            break;
                        }
                    }
                    sub_themeBases = mThemeBases.subList(position + 1, mThemeBases.size());
                }
                if (this instanceof OnLineDeskTopWallpaperPreview) {
                    mActionBar.setTitle(getString(R.string.theme_model_wallpaper));
                } else if (this instanceof OnLineLockScreenWallpaperPreview) {
                    mActionBar.setTitle(getString(R.string.lockscreen_wallpaper));
                } else {
                    mActionBar.setTitle(mThemeBase.getNameByLocale());
                }
                allapp_bottom_layout = (LinearLayout) findViewById(R.id.allapp_bottom_layout);
                mpDialog = (ProgressBar) findViewById(R.id.online_load_progress);
                mLinearLayoutLoading = (LinearLayout) findViewById(R.id.online_theme_bottom_bar_loading);
                mPerTextView = (TextView) findViewById(R.id.online_percent);
                adapter = initAdapter();
                initIndicator();
                gallery.setAdapter(adapter);
                gallery.setOnItemSelectedListener(this);
                if (adapter.getCount() > 1) {
                    gallery.setSelection(1);
                    mCurrentSelection = 1;
                }
                mChangeHelper.dispatchOnCreate();
                Map<String, String> map = new HashMap<String, String>();
                map.put(mThemeBase.getPackageName(), "online");
                //BIAgent.onEvent(this, "theme_preview", map);

                //RC51125 FC when open themechooer by birdview second times, add by Jianwu Gao ,begin
            } else {
                //reload the all theme view
                isFromOtherApp = true;
                Intent intent = new Intent();
                intent.setClass(OnlinePreviewIcons.this, ThemeChooser.class);
                startActivity(intent);
                finish();
            }
            //RC51125 FC when open themechooer by birdview second times, add by Jianwu Gao ,end
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCurrentSelection = 1;
        mChangeHelper.dispatchOnResume();
        getContentResolver().registerContentObserver(Downloads.Impl.CONTENT_URI, true, mObserver);

        LewaDownloadManager downloadManager = LewaDownloadManager
                .getInstance(getContentResolver(), getPackageName());
        int status = downloadManager.getStatusById(mDownloadId);
        if (DownloadManager.STATUS_FAILED == status) {
            ThemeApplication.sThemeStatus.setDownloadingCancelled(mDownloadId);
        }
        if (null != themeinfo && !isFromOtherApp) {
            showLoadStatus();
        }
    }

    protected void onPause() {
        if (!(this instanceof OnLineFontsPreview)) {
            mChangeHelper.dispatchOnPause();
        }
        getContentResolver().unregisterContentObserver(mObserver);
        super.onPause();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preview_icons_option, menu);
        themeinfo = menu.findItem(R.id.themeinfo);
        apply = menu.findItem(R.id.apply);
        delete = menu.findItem(R.id.delete);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        themeinfo.setVisible(false);
        if (isFromOtherApp) {
            themeinfo.setEnabled(false);
            themeinfo.setVisible(false);
            apply.setEnabled(false);
            apply.setTitle(R.string.theme_download);
            delete.setEnabled(false);
            delete.setVisible(false);

        } else {
            showLoadStatus();
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.themeinfo) {
            if (this instanceof PreviewOnlineIconsActivity) {
                Intent i = new Intent();
                i.setClass(this, ThemeInfoActivity.class);
                i.setData(Themes.getThemeUri(this, mThemeBase.getPackageName(), mThemeBase.getThemeId()));
                if (ThemeItem.getInstance(this, Themes.getThemeUri(this, mThemeBase.getPackageName(), mThemeBase.getThemeId())) == null) {
                    return true;
                }
                startActivity(i);
            }
        } else if (item.getItemId() == R.id.apply) {
            switch (button_status) {
                case APPLY:
                    applyTheme();
                    break;
                case DOWNLOAD:
                    downloadTheme();
                    break;
                case CANCEL:
                    if (mDownloadId != -1) {
                        manager.remove(mDownloadId);
                    }
                    mThemeStatus.setDownloadingCancelled(mDownloadId);
                    isDownloading = false;
                    showViews(SHOWLOAD);
                    break;
                default:
                    break;
            }
        } else if (item.getItemId() == R.id.delete) {
            if (mThemeStatus.isApplied(mThemeType == ThemeStatus.THEME_TYPE_LIVEWALLPAPER ? mThemeBase.getPackageName() + mThemeBase.getThemeId() : mThemeBase.getPackageName())
                    || mThemeStatus.isApplied(mThemeBase.getPackageName(), mThemeBase.getPkg(), ThemeStatus.THEME_TYPE_LIVEWALLPAPER)) {
                Toast.makeText(this
                        , getDeleteUsingThemeToastMessage(), Toast.LENGTH_SHORT).show();
                return true;
            }
            if (mThemeStatus.isApplied(mThemeType == ThemeStatus.THEME_TYPE_WALLPAPER ? mThemeBase.getPackageName() + mThemeBase.getThemeId() : mThemeBase.getPackageName())
                    || mThemeStatus.isApplied(mThemeBase.getPackageName(), mThemeBase.getPkg(), ThemeStatus.THEME_TYPE_WALLPAPER)) {
                Toast.makeText(this
                        , getDeleteUsingThemeToastMessage(), Toast.LENGTH_SHORT).show();
                return true;
            }
            showDialog(SHOW_DELETE_DIALOG);
        }

        return true;
    }

    protected String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme);
    }

    private void showLoadStatus() {
        if (isFromOtherApp && mThemeBase != null) {
            //themeinfo.setEnabled(true);
            //themeinfo.setVisible(true);
            apply.setEnabled(true);
            apply.setVisible(true);
            delete.setEnabled(true);
            delete.setVisible(true);
        }
        String mFileName = mThemeBase.getPkg();
        int status = isFromOtherApp ?
                2 : mThemeStatus.getStatus(mThemeType == ThemeStatus.THEME_TYPE_LIVEWALLPAPER ?
                        mThemeBase.getPackageName()+ mThemeBase.getThemeId() :
                        mThemeBase.getPackageName(), mFileName, mThemeType,
                mThemeBase.getVersionCode());
        //Log.d("simply","+++++showLoadStatus,status:"+status);
        switch (status) {
            case ThemeStatus.STATUS_APPLIED:
            case ThemeStatus.STATUS_DOWNLOADED:
                mLinearLayoutLoading.setVisibility(View.GONE);
                button_status = APPLY;
                if (null != apply) {
                    apply.setIcon(R.drawable.ic_menu_done);
                    apply.setTitle(R.string.apply);
                }
                if (null != delete) {
                    delete.setVisible(true);
                }
               /* if (this instanceof PreviewOnlineIconsActivity) {
                    if (null != themeinfo) {
                        themeinfo.setVisible(true);
                    }
                } else {
                    if (null != themeinfo) {
                        themeinfo.setVisible(false);
                        themeinfo.setEnabled(false);
                    }
                }*/
                break;
            case ThemeStatus.STATUS_DOWNLOADING:
                mLinearLayoutLoading.setVisibility(View.GONE);
                button_status = CANCEL;
                if (null != apply) {
                    apply.setTitle(android.R.string.cancel);
                    int iconId = ThemeManager.STANDALONE ? R.drawable.ic_menu_cancel : lewa.R.drawable.ic_menu_cancel;
                    apply.setIcon(iconId);
                    getSupportActionBar().hideSplit();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getSupportActionBar().showSplit();
                        }
                    }, 500);
                }
                if (null != delete) {
                    delete.setVisible(false);
                }
               
                Object[] values = getPercent();
                long percent = 0;
                long max = (Integer)values[0];
                long process = (Integer)values[1];
                if(values != null&&max!= 0){
                   percent = process*100/max;
                }
                mpDialog.setMax((Integer)values[0]);
                mpDialog.setProgress((Integer)values[1]);
                mPerTextView.setText(getString(R.string.percent, String.valueOf(percent)));
                mLinearLayoutLoading.setVisibility(View.VISIBLE);
                if (null != themeinfo) {
                    themeinfo.setVisible(false);
                }
                break;
            default:
                mLinearLayoutLoading.setVisibility(View.GONE);
                button_status = DOWNLOAD;
                isDownloading = false;
                mpDialog.setProgress(0);
                mPerTextView.setText(getString(R.string.percent, String.valueOf(0)));
                if (null != apply) {
                    apply.setTitle(R.string.theme_download);
                    apply.setIcon(R.drawable.ic_menu_download);
                }
                if (null != delete) {
                    delete.setVisible(false);
                }
                if (null != themeinfo) {
                    themeinfo.setVisible(false);
                }
        }
    }

    protected abstract ImageAdapter initAdapter();

    protected abstract void applyTheme();

    protected void onPrepareDialog(int id, Dialog dialog) {
        mChangeHelper.dispatchOnPrepareDialog(id, dialog);
    }

    @Override
    public void onItemSelected(View view, int position) {
        if (tempView != null) {
            tempView.setImageResource(R.drawable.indicator_normal);
        }
        tempView = indicatorViews.get(position);
        tempView.setImageResource(R.drawable.indicator_highlight);
        adapter.setSelectItem(position);

    }

    // 判断sdcard是否挂载上，返回值为true证明挂载上了，否则不存在
    protected boolean checkSDCardMount(String mountPoint) {
        if (mountPoint == null) {
            return false;
        }
        String state = null;
        state = mStorageManager.getVolumeState(mountPoint);
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case SHOW_DELETE_DIALOG: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                if (this instanceof OnLineDeskTopWallpaperPreview) {
                    builder.setTitle(R.string.deskwallpaper_delete_title);
                    builder.setMessage(getString(R.string.deskwallpaper_delete));
                } else if (this instanceof OnLineLockScreenWallpaperPreview) {
                    builder.setTitle(R.string.lockwallpaper_delete_title);
                    builder.setMessage(getString(R.string.lockwallpaper_delete));
                } else {
                    builder.setTitle(R.string.theme_delete_title);
                    builder.setMessage(getString(R.string.theme_delete));
                }
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doDeleteTheme();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                return builder.create();
            }
        }
        if (id == DIALOG_APPLY) {
            return mChangeHelper.dispatchOnCreateDialog(id);
        }
        return super.onCreateDialog(id, args);
    }

    protected void doDeleteTheme() {
        // Begin, for the bug 46199, zhumeiquan, 20140319
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
            PackageDeleteObserver observer = new PackageDeleteObserver();
            try {
                // here "android.permission.DELETE_PACKAGES" permission is needed
                getPackageManager().deletePackage(mThemeBase.getPackageName(), observer, 0);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            Message msg = mHandler.obtainMessage(DELETE_THEME);
            mHandler.sendMessage(msg);
        }
        // End

        if ((mThemeType == ThemeStatus.THEME_TYPE_WALLPAPER
                || mThemeType == ThemeStatus.THEME_TYPE_LOCK_WALLPAPER)
                && !mThemeBase.getPkg().endsWith("lwt")) {
            getContentResolver().delete(Themes.getThemeUri(
                    this, mThemeBase.getPackageName(), mThemeBase.getThemeId()), null, null);

            File f = new File(new String(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + getLoadPath(mThemeBase, this) + mThemeBase.getPackageName() + ".jpg"));
            if (f.exists()) {
                f.delete();
            }
        }
        mThemeStatus.setDeleted(mThemeType == ThemeStatus.THEME_TYPE_LIVEWALLPAPER ?
                        mThemeBase.getPackageName() + mThemeBase.getThemeId() : mThemeBase.getPackageName(),
                mThemeBase.getPkg(), mThemeType
        );
        ThemeUtil.deleteThemeInfo(this, mThemeBase);
    }

    @Override
    public void onClick(View v) {

    }

    private void downloadTheme() {
        if (!isDownloading) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            String[] storagePathList = mStorageManager.getVolumePaths();
            if (storagePathList != null) {
                if (DBG) Log.d(TAG, "StorgaeList size: " + storagePathList.length);
                if (storagePathList.length >= 2) {
                    mSDCardPath = storagePathList[0];
                    mSDCard2Path = storagePathList[1];
                } else if (storagePathList.length == 1) {
                    mSDCardPath = storagePathList[0];
                }
            }
            mSDCardMounted = checkSDCardMount(mSDCardPath);
            mSDCard2Mounted = checkSDCardMount(mSDCard2Path);

            if (mSDCardMounted || mSDCard2Mounted) {
                /**
                 * SDCard no space
                 */
                if (!ThemeUtil.sdcardHasSpace(20)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.theme_sd_nospace_title);
                    builder.setMessage(R.string.theme_sd_nospace_msg);
                    builder.setNegativeButton(
                            R.string.theme_back, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    isDownloading = false;
                                    alertDialog.dismiss();
                                }
                            }
                    );
                    //if (!STANDALONE) {
                    builder.setPositiveButton(
                            R.string.theme_sd_clear, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        Intent fileManager = new Intent();
                                        isDownloading = false;
                                        fileManager.setAction(
                                                "com.lewa.systemclean.SYSTEM_CLEAN_ACTIVITY");
                                        startActivity(fileManager);
                                    } catch (Exception e) {
                                        Toast.makeText(OnlinePreviewIcons.this, getString(
                                                R.string.can_not_find_app), Toast.LENGTH_SHORT)
                                                .show();
                                        e.printStackTrace();
                                    }
                                }
                            }
                    );
                    //}
                    alertDialog = builder.create();
                    alertDialog.show();
                    return;
                }

            } else {
                isDownloading = false;
                Toast.makeText(this, getString(R.string.no_sdcard), Toast.LENGTH_SHORT).show();
                return;
            }

            //Delete for standalone by Fan.Yang
            if (!ThemeManager.STANDALONE && false/*&& lewa.util.NetPolicyUtils.isNetworkBlocked(this, getPackageName())*/) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.theme_network_error_title);
                builder.setMessage(R.string.network_closed);
                builder.setNegativeButton(
                        R.string.theme_back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isDownloading = false;
                                alertDialog.dismiss();
                            }
                        }
                );
                builder.setPositiveButton(
                        R.string.set_network, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isDownloading = false;
                                //Delete for standalone by Fan.Yang
                                //lewa.util.NetPolicyUtils.startFirewallActivity(OnlinePreviewIcons.this);
                            }
                        }
                );
                alertDialog = builder.create();
                alertDialog.show();
                return;

            }
            /**
             * the network is not connect
             */
            if (!ThemeUtil.isNetWorkEnable(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.theme_network_error_title);
                builder.setMessage(R.string.theme_network_error_msg);
                builder.setNegativeButton(
                        R.string.theme_back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isDownloading = false;
                                alertDialog.dismiss();
                            }
                        }
                );
                builder.setPositiveButton(
                        R.string.theme_network_set, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isDownloading = false;
                                Intent intent = new Intent();
                                intent.setAction("android.settings.SETTINGS");
                                startActivity(intent);
                            }
                        }
                );
                alertDialog = builder.create();
                alertDialog.show();
                return;
            }

            /**
             * the network is not wifi
             */
            if (!ThemeUtil.getNetworkType(this).equals("wifi")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.theme_network_remind_title);
                builder.setMessage(R.string.theme_network_remind_msg);
                builder.setNegativeButton(
                        R.string.theme_back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isDownloading = false;
                                alertDialog.dismiss();
                            }
                        }
                );
                builder.setPositiveButton(
                        R.string.theme_ignore, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                /**
                                 * 忽略gprs网络，直接下载
                                 */
                                showViews(SHOWLOADING);
                            }
                        }
                );
                builder.setNeutralButton(
                        R.string.theme_network_set, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isDownloading = false;
                                Intent intent = new Intent();
                                intent.setAction("android.settings.SETTINGS");
                                startActivity(intent);
                            }
                        }
                );
                alertDialog = builder.create();
                alertDialog.show();
                return;
            }
            /**
             * 如果以上条件都满足，直接下载
             */
            showViews(SHOWLOADING);
        }
    }

    protected void showViews(int type) {
        switch (type) {
            case SHOWLOAD:
                button_status = DOWNLOAD;
                getSupportActionBar().hideSplit();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        apply.setTitle(R.string.theme_download);
                        apply.setIcon(R.drawable.ic_menu_download);
                        getSupportActionBar().showSplit();
                    }
                }, 500);
                delete.setVisible(false);
                themeinfo.setVisible(false);
                mLinearLayoutLoading.setVisibility(View.GONE);
                break;
            case SHOWLOADING:
                mHandler.sendEmptyMessageDelayed(SHOWLOADING_MSG, 0);
                getSupportActionBar().hideSplit();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        apply.setTitle(android.R.string.cancel);
                        apply.setIcon(R.drawable.ic_menu_cancel);
                        getSupportActionBar().showSplit();
                    }
                }, 500);
                button_status = CANCEL;
                delete.setVisible(false);
                themeinfo.setVisible(false);
                mLinearLayoutLoading.setVisibility(View.VISIBLE);
                break;
            case SHOWLOADED:
                delete.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                apply.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                apply.setTitle(R.string.apply);
                apply.setIcon(R.drawable.ic_menu_done);
                button_status = APPLY;
                delete.setVisible(true);
                apply.setVisible(true);
               /* if (!(this instanceof PreviewOnlineIconsActivity)) {
                    themeinfo.setVisible(false);
                    themeinfo.setEnabled(false);
                } else {
                    themeinfo.setVisible(true);
                }*/
                mLinearLayoutLoading.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    protected void initIndicator() {
        allapp_bottom_layout.removeAllViews();
        indicatorViews.clear();
        for (int i = 0; i < size + 1; i++) {
            View iView = mInflater.inflate(R.layout.allapp_indicator, null);
            ImageView addView = (ImageView)iView.findViewById(R.id.allapp_indicator_children);
            allapp_bottom_layout.addView(iView);
            indicatorViews.add(addView);
        }
    }

    protected String getFitAttachment() {
        return mThemeBase.attachment;
    }

    public void startDownloadPackage(ThemeBase themeBase) {
        isDownloading = true;
        //开始下载
        Uri uri = Uri.parse(getFitAttachment());
        LewaDownloadManager.Request request = null;
        request = new LewaDownloadManager.Request(uri);
        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        //在通知栏中显示
        request.setShowRunningNotification(true);
        request.setVisibleInDownloadsUi(true);
        JSONObject jso = new JSONObject();
        JSONArray jsa = new JSONArray();
        try {
            jso.put("themeid", "" + themeBase.getId());
            jso.put("Tid", "" + themeBase.getThemeId());
            jso.put("Packagename", "" + themeBase.getPackageName());
            jso.put("Internalversion", "" + themeBase.getInternal_version());
            jso.put("Filename", "" + themeBase.getPkg());
            jso.put("Name_zh", "" + themeBase.getCnName());
            jso.put("Name_en", "" + themeBase.getEnName());
            jso.put("Author", "" + themeBase.getCnAuthor());
            jso.put("Author_en", "" + themeBase.getEnAuthor());
            jso.put("Size", "" + themeBase.getSize());
            jso.put("Module_num", "" + themeBase.getModelNum());
            jso.put("Theme_version", "" + themeBase.getVersion());
            jso.put("Dateline", "" + themeBase.getCreateDate());
            jso.put("Downloads", "" + themeBase.getDownloads());
            jso.put("Attachment", "" + themeBase.attachment);
            String path = "";
            for (int i = 0; i < themeBase.previewpath.size(); i++) {
                path = themeBase.previewpath.get(i) + "," + path;
            }
            jso.put("Previewpath", "" + path);
            jso.put("Thumbnailpath", "" + themeBase.thumbnailpath);
            jsa.put(jso);

        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        //if (!ThemeManager.STANDALONE) {
        ((LewaDownloadManager.Request) request).setNotiExtras(jsa.toString());
        //}
        if (themeBase.getPkg().endsWith("lwt")) {
            request.setMimeType("vnd.lewa.cursor.dir/theme");
        } else if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER) {
            request.setMimeType("vnd.lewa.cursor.dir/deskicon");
        } else if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER) {
            request.setMimeType("vnd.lewa.cursor.dir/lockicon");
        }
        //sdcard的目录下的download文件夹
        File file = new File(ThemeConstants.THEME_LWT);
        if (!file.exists()) {
            file.mkdir();
        }
        File wallpaper_file = new File(ThemeConstants.THEME_WALLPAPER);
        if (!wallpaper_file.exists()) {
            wallpaper_file.mkdir();
        }
        File lockwallpaper_file = new File(ThemeConstants.THEME_LOCK_SCREEN_WALLPAPER);
        if (!lockwallpaper_file.exists()) {
            lockwallpaper_file.mkdir();
        }
        try {
            (new File(wallpaper_file, ".nomedia")).createNewFile();
        } catch (Exception e) {
        }
        try {
            String dest = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + getLoadPath(themeBase, this)
                    + getFileName(themeBase);
            if (dest.endsWith("lwt")) {
                dest = dest.substring(0, dest.length() - 4) + "_v" + themeBase.getVersionCode() + ".lwt";
            }
            Uri imageUri = Uri.parse("file://" + dest);
            request.setDestinationUri(imageUri);
            if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER || mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER) {
                request.setTitle(themeBase.getPackageName() + ".jpg");
            } else {
                request.setTitle(themeBase.getNameByLocale());
            }
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            mDownloadId = manager.enqueue(request);
            startDownloading(themeBase, imageUri, mDownloadId);

            android.content.SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

            editor.putString(String.valueOf(mDownloadId), themeBase.getId()).apply();
            editor.putString("PkgName" + String.valueOf(mDownloadId), themeBase.getPackageName()).apply();
            if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER) {
                editor.putString("TypeName" + String.valueOf(mDownloadId), getString(R.string.desktop_wallpaper)).apply();
            } else if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER) {
                editor.putString("TypeName" + String.valueOf(mDownloadId), getString(R.string.lockscreen_wallpaper)).apply();
            }
            String mFileName = themeBase.getPkg();
            mThemeStatus.setDownloading(mDownloadId
                    , mThemeType == ThemeStatus.THEME_TYPE_LIVEWALLPAPER ? themeBase.getPackageName()
                    + themeBase.getThemeId() : themeBase.getPackageName(), themeBase.getPkg(), mThemeType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to download theme: " + e);
        }
    }

    protected void startDownloading(ThemeBase themeBase, Uri uri, long downloadId) {
        if (themeBase.getPkg().endsWith(".lwt")
                || (mThemeType != ThemeStatus.THEME_TYPE_WALLPAPER
                && mThemeType != ThemeStatus.THEME_TYPE_LOCK_WALLPAPER)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(ThemeColumns.THEME_PACKAGE, themeBase.getPackageName());
        values.put(ThemeColumns.THEME_ID, themeBase.getThemeId());
        values.put(ThemeColumns.AUTHOR, "");
        values.put(ThemeColumns.NAME, themeBase.getPkg());
        values.put(ThemeColumns.STYLE_NAME, "");
        values.put(ThemeColumns.IS_IMAGE_FILE, downloadId);
        values.put(ThemeColumns.SIZE, themeBase.getLength());
        values.put(ThemeColumns.DOWNLOAD_PATH, Environment.getExternalStorageDirectory().getAbsolutePath()
                + getLoadPath(themeBase, this)
                + getFileName(themeBase));
        values.put((mThemeType == ThemeStatus.THEME_TYPE_WALLPAPER)
                ? ThemeColumns.WALLPAPER_URI : ThemeColumns.LOCK_WALLPAPER_URI, uri.toString());
        getContentResolver().insert(ThemeColumns.CONTENT_PLURAL_URI, values);
    }

    protected String getFileName(ThemeBase themeBase) {
        String pkg = themeBase.getPkg();
        return pkg.endsWith(".lwt") ? pkg : themeBase.getPackageName() + ".jpg";
    }

    protected abstract String getLoadPath(ThemeBase themeBase, Context context);

    private void updateProgress(long max, long progress, int status) {
        if (progress < max && max != 0 && status == Downloads.Impl.STATUS_RUNNING) {
            mpDialog.setMax((int)max);
            mpDialog.setProgress((int)progress);
            long percent = progress * 100 / max;
            mPerTextView.setText(getString(R.string.percent, String.valueOf(percent)));
        } else if (max == progress && max != 0 && status == Downloads.Impl.STATUS_SUCCESS) {
            showViews(SHOWLOADED);
        } else if (max != progress && Downloads.Impl.isStatusError(status)) {
            //Log.d("simply", ".............");
            showViews(SHOWLOAD);
            isDownloading = false;
            mHandler.removeMessages(SHOW_DOWNLOAD_FAILED);
            mHandler.sendEmptyMessageDelayed(SHOW_DOWNLOAD_FAILED, 200);
        }
    }

    private static class HandlerAll extends Handler {
        private OnlinePreviewIcons mActivity;

        public HandlerAll(OnlinePreviewIcons activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOWLOADING_MSG:
                    mActivity.startDownloadPackage(mActivity.mThemeBase);
                    break;
                case SHOWLOADED_MSG:
                    break;
                case DELETE_THEME: {
                    Toast.makeText(mActivity
                            , mActivity.getString(R.string.theme_delete_success)
                            , Toast.LENGTH_SHORT).show();
                    mActivity.finish();
                }
                break;
                case ACTION_PROGRESSBAR_UPDATEING:
                    Object[] values = (Object[]) msg.obj;
                    mActivity.updateProgress((Integer) values[0], (Integer) values[1], (Integer) values[2]);
                    break;
                case SHOW_DOWNLOAD_FAILED:
                    Toast.makeText(mActivity, R.string.download_or_unzip_fail, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    private class LoadTheme extends AsyncTask {

        @Override
        protected Object doInBackground(Object... params) {
            try {
                mThemeBases = (ArrayList<ThemeBase>) clientResolver.getPageResolver()
                        .getNextPageContent(1);
                mThemeBase = mThemeBases.get(0);
                String mFileName = mThemeBase.getPkg();
                if (mFileName.endsWith(".lwt")) {
                    mFileName = mFileName.substring(0, mFileName.length() - 4) + "_v" + mThemeBase.getVersionCode() + ".lwt";
                }
                mDownloadId = mThemeStatus.getDownloadId(mThemeBase.getPackageName(),
                        mFileName, mThemeType);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (null == mThemeBase) {
                Toast.makeText(OnlinePreviewIcons.this, getString(R.string.theme_not_exist), Toast.LENGTH_SHORT).show();
                OnlinePreviewIcons.this.finish();
                return;
            }
            if (mThemeBase.getPackageName().equals("nothing:error")) {
                Toast.makeText(OnlinePreviewIcons.this, getString(R.string.theme_not_fit_phone), Toast.LENGTH_SHORT).show();
                OnlinePreviewIcons.this.finish();
                return;
            }
            mloadingprogressbar.setVisibility(View.INVISIBLE);
            showLoadStatus();
            if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER) {
                mActionBar.setTitle(getString(R.string.theme_model_wallpaper));
            } else if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER) {
                mActionBar.setTitle(getString(R.string.lockscreen_wallpaper));
            } else {
                mActionBar.setTitle(mThemeBase.getNameByLocale());
            }
            mHandler = new HandlerAll(OnlinePreviewIcons.this);
            adapter = initAdapter();
            initIndicator();
            gallery.setAdapter(adapter);
            gallery.setOnItemSelectedListener(OnlinePreviewIcons.this);
            if (adapter.getCount() > 1) {
                gallery.setSelection(1);
                mCurrentSelection = 1;
            }
            mChangeHelper.dispatchOnCreate();
            Map<String, String> map = new HashMap<String, String>();
            map.put(mThemeBase.getPackageName(), "online");
            //BIAgent.onEvent(OnlinePreviewIcons.this, "theme_preview", map);
            downloadTheme();
        }
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(String packageName, int returnCode) {
            Message msg = mHandler.obtainMessage(DELETE_THEME);
            mHandler.sendMessage(msg);
        }
    }

    private class DownloadsChangeObserver extends ContentObserver {
        public DownloadsChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mDownloadId < 0) {
                return;
            }
            /*Cursor c = getContentResolver().query(Downloads.Impl.CONTENT_URI
                    , new String[]{Downloads.Impl.COLUMN_TOTAL_BYTES
                    , Downloads.Impl.COLUMN_CURRENT_BYTES, Downloads.Impl.COLUMN_STATUS}
                    , Downloads.Impl._ID + "=?", new String[]{String.valueOf(mDownloadId)}
                    , null);
            if (null == c) return;

            Object[] values = new Object[3];
            if (c.moveToFirst()) {
                values[0] = c.getInt(0);
                values[1] = c.getInt(1);
                values[2] = c.getInt(2);
                Log.e(TAG, "value:"+values[0]+",1:"+values[1]+",2:"+values[2]);
            } else {
                values[0] = 0;
                values[1] = 0;
                values[2] = 0;
            }
            c.close();*/
            Object[] values = getPercent();
            Message msg = Message.obtain(mHandler, ACTION_PROGRESSBAR_UPDATEING, values);
            mHandler.sendMessage(msg);
        }
    }
    private Object[] getPercent(){
        Cursor c = getContentResolver().query(Downloads.Impl.CONTENT_URI
                , new String[]{Downloads.Impl.COLUMN_TOTAL_BYTES
                , Downloads.Impl.COLUMN_CURRENT_BYTES, Downloads.Impl.COLUMN_STATUS}
                , Downloads.Impl._ID + "=?", new String[]{String.valueOf(mDownloadId)}
                , null);
        if (null == c) return null;
        Object[] values = new Object[3];
        if (c.moveToFirst()) {
            values[0] = c.getInt(0);
            values[1] = c.getInt(1);
            values[2] = c.getInt(2);
            Log.e(TAG, "value:"+values[0]+",1:"+values[1]+",2:"+values[2]);
        } else {
            values[0] = 0;
            values[1] = 0;
            values[2] = 0;
        }
        c.close();
        return values;
    }

    public class ImageAdapter extends BaseAdapter {
        protected ImageLoader imageLoader = ImageLoader.getInstance();
        private Context mContext;
        private ThemeBase mThemeBase;
        private ArrayList<String> list = new ArrayList<String>();
        private int selectItem;
        private String previewLocalPrefix;
        private int mSpacing;

        public ImageAdapter(Context context, ThemeBase mThemeBase) {
            mContext = context;
            this.mThemeBase = mThemeBase;
            list.clear();
            //TCL937553 add by Fan.Yang
            for (int i = 0; i < mThemeBase.previewpath.size() && STANDALONE; i++) {
                String uri = mThemeBase.previewpath.get(i);
                //yixiao add theme filter for independent launcher
                if (!uri.contains("preview_systemapp")) {
                    if((!uri.contains("preview_lockscreen") || (!ThemeManager.INDEPENDENT)) ){
                        list.add(uri);
                    }
                    
                }
            }

            previewLocalPrefix = new StringBuilder().append(ThemeConstants.THEME_ONLINE_PREVIEW)
                    .append("/").append(mThemeBase.getName()).append("/").toString();
            size = list.size();
            mSpacing = (int) android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP
                    , 12, context.getResources().getDisplayMetrics());
        }

        @Override
        public int getCount() {
            return list.size() + 1;
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

        private void setViewText(TextView tv, String str, TableRow tr) {
            if (TextUtils.isEmpty(str)) {
                tv.setVisibility(View.GONE);
                tr.setVisibility(View.GONE);
            } else {
                tv.setText(str);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewCache viewCache = null;
            if (position != 0) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.theme_imageview, null);
                    viewCache = new ViewCache();
                    viewCache.thumbnail = (ImageView) convertView.findViewById(R.id.preview_icon);
                    convertView.setTag(viewCache);
                } else {
                    viewCache = (ViewCache) convertView.getTag();
                }
                String previewPath = list.get(position - 1);
                if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER
                        || mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER) {
                    if (previewPath.lastIndexOf("/") != -1) {
                        String start = previewPath.substring(0, previewPath.lastIndexOf("/") + 1);
                        String end = previewPath.substring(previewPath.lastIndexOf("/"),
                                previewPath.length());
                        previewPath = start + "_" + ThemeUtil.previewWidth + "-"
                                + ThemeUtil.previewHeight + end;
                    }
                }
                String previewFileName = list.get(position - 1);
                previewFileName = previewFileName.substring(previewFileName.lastIndexOf("/") + 1);
                Log.d(TAG, "previewLocalPrefix=" + previewLocalPrefix + ",previewFileName=" + previewFileName + ",previewPath=" + previewPath);
                imageLoader.displayImage(previewPath, viewCache.thumbnail);
            } else {
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.theme_preview_icons_details, null);

                TextView tv_name = (TextView) convertView.findViewById(R.id.theme_name);
                TableRow tr_name = (TableRow) convertView.findViewById(R.id.tr_name);
                if (mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_WALLPAPER
                        || mThemeType == com.lewa.themechooser.ThemeStatus.THEME_TYPE_LOCK_WALLPAPER) {
                    setViewText(tv_name, null, tr_name);

                } else {
                    setViewText(tv_name, mThemeBase.getNameByLocale(), tr_name);
                }
                if (ThemeUtil.CONFIG_IS_B2B) {
                    TextView tv_author_name = (TextView) convertView.findViewById(R.id.theme_author);
                    tv_author_name.setVisibility(View.GONE);
                    TextView tv_author = (TextView) convertView.findViewById(R.id.theme_info_author);
                    tv_author.setVisibility(View.GONE);
                } else {
                    TableRow tr_author = (TableRow) convertView.findViewById(R.id.tr_author);
                    TextView tv_author = (TextView) convertView.findViewById(R.id.theme_info_author);
                    setViewText(tv_author, mThemeBase.getAuthor(), tr_author);
                }
                TextView tv_size = (TextView) convertView.findViewById(R.id.theme_info_size);
                String size = mThemeBase.getSize().split("K")[0];
                Long mlong = Double.valueOf(size).longValue();
                TableRow tr_size = (TableRow) convertView.findViewById(R.id.tr_size);
                setViewText(tv_size, Formatter.formatFileSize(mContext, mlong * 1024), tr_size);
                TextView tv_version = (TextView) convertView.findViewById(R.id.theme_info_version);
                TableRow tr_version = (TableRow) convertView.findViewById(R.id.tr_version);
                setViewText(tv_version, mThemeBase.getVersion(), tr_version);
                TextView tv_date = (TextView) convertView.findViewById(R.id.theme_info_date);
                TableRow tr_date = (TableRow) convertView.findViewById(R.id.tr_date);
                setViewText(tv_date, mThemeBase.getCreateDate(), tr_date);
                TextView tv_download = (TextView) convertView.findViewById(R.id.theme_info_download_count);
                TableRow tr_count = (TableRow) convertView.findViewById(R.id.tr_count);
                setViewText(tv_download, mThemeBase.getDownloads(), tr_count);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (mContext.getResources().getDimension(R.dimen.preview_icon_selected_width))
                    , (int) (mContext.getResources().getDimension(R.dimen.preview_icon_selected_height)));
            if (position != getCount() - 1) {
                params.setMargins(0, 0, mSpacing, 0);
            }
            convertView.setLayoutParams(params);
            return convertView;
        }
    }

    private class ViewCache {
        public ImageView thumbnail;
        public ImageView statusFlag;
        public TextView theme_name;
    }
}
