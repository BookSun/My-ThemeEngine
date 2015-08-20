/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.lewa.themechooser.custom.preview.local;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.service.wallpaper.WallpaperService;
import android.service.wallpaper.WallpaperSettingsActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.themechooser.ChangeThemeHelper;
import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;

import java.util.List;

import util.ThemeUtil;

public class LiveWallpaperPreview extends Activity {
    static final String EXTRA_LIVE_WALLPAPER_INTENT = "android.live_wallpaper.intent";
    static final String EXTRA_LIVE_WALLPAPER_SETTINGS = "android.live_wallpaper.settings";
    static final String EXTRA_LIVE_WALLPAPER_PACKAGE = "android.live_wallpaper.package";
    private static final int SHOW_DELETE_DIALOG = 3;
    private static final int DIALOG_APPLY = 0;
    private final ChangeThemeHelper mChangeHelper = new ChangeThemeHelper(this, DIALOG_APPLY);
    private static final int DELETE_THEME = 2;
    private static final String LOG_TAG = "LiveWallpaperPreview";
    protected ThemeItem mThemeItem;
    private WallpaperManager mWallpaperManager;
    private WallpaperConnection mWallpaperConnection;
    private String mSettings;
    private String mPackageName;
    private String mClassName;
    private Intent mWallpaperIntent;
    private WallpaperInfo minfo;
    private View mView;
    private Dialog mDialog;
    private ActionBar mActionBar;
    private MenuItem themeinfo;
    private MenuItem apply;
    private MenuItem delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        minfo = (WallpaperInfo) getIntent().getExtra(ThemeConstants.LIVEWALLPAPER_INFO_INTENT);
        if (minfo == null) {
            mThemeItem = ThemeItem.getInstance(this, getIntent().getData());
            mPackageName = mThemeItem.getPackageName();
            mClassName = mThemeItem.getThemeId();
            mWallpaperIntent = new Intent(WallpaperService.SERVICE_INTERFACE).setClassName(
                    mPackageName, mClassName);
            List<ResolveInfo> services = getPackageManager().queryIntentServices(
                    new Intent(WallpaperService.SERVICE_INTERFACE).setClassName(mPackageName,
                            mThemeItem.getThemeId()), PackageManager.GET_META_DATA
            );
            try {
                minfo = new WallpaperInfo(this, services.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mWallpaperIntent = (Intent) getIntent().getExtra(ThemeConstants.LIVEWALLPAPER_INTENT);
            mThemeItem = Themes.getTheme(this, minfo.getPackageName(), minfo.getServiceName());
        }
        if (mWallpaperIntent == null) {
            finish();
        }

        setContentView(R.layout.live_wallpaper_preview);
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);

        mActionBar.setTitle(minfo.loadLabel(getPackageManager()));
        mSettings = minfo.getSettingsActivity();
        mView = findViewById(R.id.configure);
        if (mSettings == null) {
            mView.setVisibility(View.GONE);
        }
        mWallpaperManager = WallpaperManager.getInstance(this);
        mWallpaperConnection = new WallpaperConnection(mWallpaperIntent);
        mChangeHelper.dispatchOnCreate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.themeinfo) {
            configureLiveWallpaper();
        } else if (item.getItemId() == R.id.apply) {
            setLiveWallpaper(mView);
        } else if (item.getItemId() == R.id.delete) {
            deleteLiveWallpaper();
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preview_icons_option, menu);
        themeinfo = menu.findItem(R.id.themeinfo);
        apply = menu.findItem(R.id.apply);
        delete = menu.findItem(R.id.delete);
        themeinfo.setIcon(R.drawable.ic_set);
        apply.setTitle(R.string.wallpaper_instructions);
        apply.setIcon(R.drawable.ic_menu_done);
        delete.setIcon(R.drawable.ic_delect);
        if (!getIsUninstall()) {
            delete.setVisible(false);
        }
        if (mSettings == null) {
            themeinfo.setVisible(false);
        }
        return true;
    }

    public void setLiveWallpaper(View v) {
        mChangeHelper.beginChange(minfo.loadLabel(getPackageManager()).toString());
        ThemeItem appliedTheme = Themes.getAppliedTheme(this);
        if (null == appliedTheme) {
            return;
        }
        Uri uri = Themes.getThemeUri(this, appliedTheme.getPackageName(), appliedTheme.getThemeId());
        appliedTheme.close();

        Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
        i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
        i.putExtra(ThemeManager.EXTRA_LIVE_WALLPAPER_COMPONENT, mWallpaperIntent.getComponent());
        i.putExtra(CustomType.EXTRA_NAME, CustomType.LIVE_WALLPAPER);
        ApplyThemeHelp.changeTheme(this, i);
        ThemeApplication.sThemeStatus.setAppliedPkgName(
                minfo.getPackageName() + minfo.getServiceName(), ThemeStatus.THEME_TYPE_WALLPAPER);
        ThemeApplication.sThemeStatus.setAppliedPkgName(
                minfo.getPackageName() + minfo.getServiceName(), ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
    }

    private boolean getIsUninstall() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.packageName.equals(mWallpaperIntent.getComponent().getPackageName())) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                boolean isUninstall = false;
                if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    isUninstall = true;
                } else if ((appInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    isUninstall = true;
                }
                return isUninstall;
            }
        }
        return true;
    }

    public void deleteLiveWallpaper() {
        WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(this).getWallpaperInfo();
        if (ThemeApplication.sThemeStatus.isApplied(minfo.getServiceName()
                , com.lewa.themechooser.ThemeStatus.THEME_TYPE_LIVEWALLPAPER) || (wallpaperInfo != null && minfo.getPackageName().equals(wallpaperInfo.getPackageName()))) {
            Toast.makeText(this
                    , getDeleteUsingThemeToastMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (getIsUninstall()) {
            showDialog(SHOW_DELETE_DIALOG);
        } else {
            Toast.makeText(this, getString(R.string.system_live_wallpaper), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void configureLiveWallpaper() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(mWallpaperIntent.getComponent().getPackageName(), mSettings));
        intent.putExtra(WallpaperSettingsActivity.EXTRA_PREVIEW_MODE, true);
        startActivity(intent);
    }

    private void doDeleteTheme() {
        PackageDeleteObserver observer = new PackageDeleteObserver();
        getPackageManager().deletePackage(mWallpaperIntent.getComponent().getPackageName(), observer, 0);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case SHOW_DELETE_DIALOG: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.live_wallpaper_delete_title);
                builder.setMessage(getString(R.string.live_wallpaper_delete));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doDeleteTheme();
                        ThemeUtil.deleteLiveWallpaper(LiveWallpaperPreview.this, mThemeItem);
                        String apkFileName = mThemeItem.getName() + ".lwt";
                        ThemeApplication.sThemeStatus.setDeleted(
                                mThemeItem.getPackageName() + minfo.getServiceName(), apkFileName, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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
    public void onResume() {
        super.onResume();
        mChangeHelper.dispatchOnResume();
        if (mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            try {
                mWallpaperConnection.mEngine.setVisibility(true);
            } catch (RemoteException e) {
                // Ignore
            }
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        mChangeHelper.dispatchOnPrepareDialog(id, dialog);
    }

    @Override
    public void onPause() {
        super.onPause();
        mChangeHelper.dispatchOnPause();
        if (mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            try {
                mWallpaperConnection.mEngine.setVisibility(false);
            } catch (RemoteException e) {
                // Ignore
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        showLoading();

        mView.post(new Runnable() {
            @Override
            public void run() {
                if (!mWallpaperConnection.connect()) {
                    mWallpaperConnection = null;
                }
            }
        });
    }

    private void showLoading() {
        LayoutInflater inflater = LayoutInflater.from(this);
        TextView content = (TextView) inflater.inflate(R.layout.live_wallpaper_loading, null);

        mDialog = new Dialog(this, android.R.style.Theme_Light);

        Window window = mDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();

        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.MATCH_PARENT;
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA);

        mDialog.setContentView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mDialog.show();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (mWallpaperConnection != null) {
            mWallpaperConnection.disconnect();
        }
        mWallpaperConnection = null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            MotionEvent dup = MotionEvent.obtainNoHistory(ev);
            try {
                mWallpaperConnection.mEngine.dispatchPointer(dup);
            } catch (RemoteException e) {
            } finally {
                dup.recycle();
            }
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            onUserInteraction();
        }
        boolean handled = getWindow().superDispatchTouchEvent(ev);
        if (!handled) {
            handled = onTouchEvent(ev);
        }

        if (!handled && mWallpaperConnection != null && mWallpaperConnection.mEngine != null) {
            int action = ev.getActionMasked();
            try {
                if (action == MotionEvent.ACTION_UP) {
                    mWallpaperConnection.mEngine.dispatchWallpaperCommand(WallpaperManager.COMMAND_TAP, (int) ev.getX(), (int) ev.getY(), 0, null);
                } else if (action == MotionEvent.ACTION_POINTER_UP) {
                    int pointerIndex = ev.getActionIndex();
                    mWallpaperConnection.mEngine.dispatchWallpaperCommand(WallpaperManager.COMMAND_SECONDARY_TAP, (int) ev.getX(pointerIndex), (int) ev.getY(pointerIndex), 0, null);
                }
            } catch (RemoteException e) {
            }
        }
        return handled;
    }

    private String getDeleteUsingThemeToastMessage() {
        return getString(R.string.delete_using_theme_live_wallpaper);
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        @Override
        public void packageDeleted(String packageName, int returnCode) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LiveWallpaperPreview.this, LiveWallpaperPreview.this
                            .getString(R.string.theme_delete_success), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        final Intent mIntent;
        IWallpaperService mService;
        IWallpaperEngine mEngine;
        boolean mConnected;

        WallpaperConnection(Intent intent) {
            mIntent = intent;
        }

        public boolean connect() {
            synchronized (this) {
                if (!bindService(mIntent, this, Context.BIND_AUTO_CREATE)) {
                    return false;
                }

                mConnected = true;
                return true;
            }
        }

        public void disconnect() {
            synchronized (this) {
                mConnected = false;
                if (mEngine != null) {
                    try {
                        mEngine.destroy();
                    } catch (RemoteException e) {
                        // Ignore
                    }
                    mEngine = null;
                }
                unbindService(this);
                mService = null;
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mWallpaperConnection == this) {
                mService = IWallpaperService.Stub.asInterface(service);
                Rect rect = new Rect();
                try {
                    final View view = mView;
                    mView.setVisibility(View.VISIBLE);
                    final View root = view.getRootView();
                    mService.attach(this, view.getWindowToken(),
                            WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY, true,
                            root.getWidth(), root.getHeight(), rect);
                } catch (RemoteException e) {
                    Log.w(LOG_TAG, "Failed attaching wallpaper; clearing", e);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mEngine = null;
            if (mWallpaperConnection == this) {
                Log.w(LOG_TAG, "Wallpaper service gone: " + name);
            }
        }

        @Override
        public void attachEngine(IWallpaperEngine engine) {
            synchronized (this) {
                if (mConnected) {
                    mEngine = engine;
                    try {
                        engine.setVisibility(true);
                    } catch (RemoteException e) {
                        // Ignore
                    }
                } else {
                    try {
                        engine.destroy();
                    } catch (RemoteException e) {
                        // Ignore
                    }
                }
            }
        }

        @Override
        public ParcelFileDescriptor setWallpaper(String name) {
            return null;
        }

        public void engineShown(IWallpaperEngine engine) throws RemoteException {
        }
    }
}
