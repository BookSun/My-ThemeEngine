package com.lewa.themechooser.custom.fragment.local;

import android.app.Activity;
import android.app.Fragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.themechooser.LocalFragment;
import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.appwidget.util.WallpaperUtils;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themechooser.newmechanism.NewMechanismHelp;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.PackageResources;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.ThemeItem.PreviewsType;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.widget.ThemeAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lewa.os.FileUtilities;
import util.ThemeUtil;

/**
 * @author xufeng
 */
public abstract class LocalBase extends Fragment implements AdapterView.OnItemClickListener {
    protected static final int LOCKSCREEN_WALLPAPER_TYPE = 0;
    protected static final int LOCKSCREEN_STYLE_TYPE = 1;
    protected static final int DESKTOP_ICON_TYPE = 2;
    protected static final int DESKTOP_STYLE_TYPE = 3;
    protected static final int DESKTOP_WALLPAPER_TYPE = 4;
    protected static final int BOOT_ANIMATION_TYPE = 5;
    protected static final int FONT_TYPE = 6;
    protected static final int SYSTEM_APP = 7;
    protected static final int LIVE_WALLPAPER_TYPE = 8;

    protected static final int SELECT_PICTRUE_FOR_LOCKSCREEN = 1;
    protected static final int FROM_LOCKSCREEN = 2;
    protected static final int SELECT_PICTRUE_FOR_WALLPAPER = 3;
    protected static final int FROM_WALLPAPER = 4;
    private static final Boolean DBG = true;
    private static final String TAG = "LocalFragment";
    private static final int MSG_HIDE_BARS = 1;
    private static final int MSG_SET_WALLPAPER = 2;
    private static final int HIDE_BARS_TIMEOUT = 2000;
    public static boolean mBusy = false; // 标识是否存在滚屏操作
    protected static AbsListView mLocalGridView = null;
    private static ThemeChooserAdapter mAdapter;
    private static WallpaperUtils sWallpaperUtils;
    public ArrayList<String> uriList = new ArrayList<String>();
    protected Animation mBottomBarAnimShow = null;
    protected Animation mBootomBarAnimHide = null;
    protected int mLayoutRes;
    protected int mItemLayoutResource;
    protected int mDefaultImageRes;
    protected ImageLoader imageLoader = ImageLoader.getInstance();
    protected DisplayImageOptions options;
    private LinearLayout mEndToast;
    private boolean isEndShowing = false;
    private Handler mHandler;
    private Uri temp_uri;
    private Bitmap mUsingBitmap;
    private Activity mContext;
    private ThemeStatus mThemeStatus;
    private boolean mIsReady;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(mLayoutRes, container, false);

        mLocalGridView = (AbsListView) view.findViewById(R.id.local_theme_grid);
        if (mLocalGridView instanceof android.widget.ListView) {
            mLocalGridView.setOnItemClickListener(this);
        }

        mEndToast = (LinearLayout) view.findViewById(R.id.theme_local_end_view);

        mHandler = new HandlerAll(this);

        mThemeStatus = ThemeApplication.sThemeStatus;

        mAdapter = new ThemeChooserAdapter(mContext);
        mLocalGridView.setAdapter(mAdapter);
        mBottomBarAnimShow = AnimationUtils.loadAnimation(mContext, R.anim.theme_bottom_bar_enter);
        mBootomBarAnimHide = AnimationUtils.loadAnimation(mContext, R.anim.theme_bottom_bar_exit);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayoutRes = R.layout.theme_local_main;
        mItemLayoutResource = R.layout.theme_grid_item_thumbnail;
        mDefaultImageRes = R.drawable.theme_no_default;
        mContext = getActivity();
        sWallpaperUtils = new WallpaperUtils(mContext);
        if (getType() == FONT_TYPE) {
            options = new DisplayImageOptions.Builder().showImageOnFail(R.drawable.bg_text_style)
                    .showImageForEmptyUri(R.drawable.bg_text_style).cacheInMemory(true).cacheOnDisc(true)
                    .showImageOnFail(R.drawable.bg_text_style)
                    .showImageOnLoading(R.drawable.bg_text_style)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
        } else {
            options = new DisplayImageOptions.Builder().showImageOnFail(R.drawable.theme_no_default)
                    .showImageForEmptyUri(R.drawable.theme_no_default)
                    .showImageOnFail(R.drawable.theme_no_default)
                    .showImageOnLoading(R.drawable.theme_no_default)
                    .cacheInMemory(true)
                    .cacheOnDisc(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
        }
    }

    private String getThemePackageName() {
        String name = temp_uri.toString();
        int lastSlash = name.lastIndexOf('/');
        name = name.substring(++lastSlash, name.length());
        try {
            name = java.net.URLDecoder.decode(name, "UTF-8");
        } catch (Exception e) {
            return name;
        }
        return name;
    }

    private void setWallpaper() {
        Toast.makeText(mContext
                , getString(R.string.theme_change_dialog_title_success), Toast.LENGTH_SHORT)
                .show();
        mContext.finish();
        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(mHomeIntent);
    }

    private void hideBars() {
        if (null == mEndToast) {
            return;
        }
        mEndToast.startAnimation(mBootomBarAnimHide);
        mBootomBarAnimHide.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mLocalGridView.scrollTo(0, 0);
                isEndShowing = false;
                mEndToast.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private Intent getCropIntent(Uri uri, int width, int height) {
        boolean isSetOut = false;
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (uri != null) {
            // Modify for standalone by Fan.Yang
            // File f = new File(android.os.Environment.getExternalStorageAndroidDataDir(
            //"com.lewa.themechooser")[0], "cropped_image");

            File f = new File(android.os.Environment.buildExternalStorageAppCacheDirs(
                    "com.lewa.themechooser")[0], "cropped_image");
            try {
                //RC26808-jianwu.gao delete begin
                //wrong crop image when set wallpaper from files or gallery
                /*
                    Bitmap bitmap=BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri));
                    if(bitmap.getWidth()>720&&bitmap.getHeight()>1280){
                	    isSetOut=true;
                    }else{
                	    isSetOut=false;
                    }
                */
                //RC26808-jianwu.gao delete end

                FileUtilities.forceMkdir(f.getParentFile(), true);
                f.createNewFile();
            } catch (Exception e) {
            }
            temp_uri = Uri.fromFile(f);
            try {
                intent.setDataAndType(uri, "image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);

                //RC26808-jianwu.gao modify begin
                //wrong crop image when set wallpaper from files or gallery
                intent.putExtra("scaleUpIfNeeded", true);
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                //RC26808-jianwu.gao modify end

                intent.putExtra("scale", true);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, temp_uri);
                intent.putExtra("return-data", false);
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                intent.putExtra("noFaceDetection", true); // no face detection
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return intent;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) return;
        Bitmap photo = null;
        Uri icon_uri = data.getData();
        switch (requestCode) {
            case FROM_LOCKSCREEN:
                if (!ThemeUtil.supportsLockWallpaper(mContext)) {
                    return;
                }
                ThemeItem mappliedTheme = Themes.getAppliedTheme(mContext);
                if (null == mappliedTheme) {
                    return;
                }
                Uri muri = Themes.getThemeUri(mContext
                        , mappliedTheme.getPackageName(), mappliedTheme.getThemeId());
                mappliedTheme.close();
                Long mName = System.currentTimeMillis();
                File f = new File(ThemeConstants.THEME_LOCK_SCREEN_WALLPAPER + "/com.lewa.pkg." + mName + ".jpg");
                try {
                    if (!f.exists()) {
                        boolean iscreate = f.createNewFile();
                    }
                    FileUtils.copyToFile(mContext.getContentResolver()
                            .openInputStream(temp_uri), f);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent mintent = new Intent(ThemeManager.ACTION_CHANGE_THEME, muri);
                mintent.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
                mintent.putExtra(ThemeManager.EXTRA_LOCK_WALLPAPER_URI, PackageResources.convertFilePathUri(temp_uri));
                mintent.putExtra(CustomType.EXTRA_NAME, CustomType.LOCKSCREEN_WALLPAPER_TYPE);
                ApplyThemeHelp.changeTheme(mContext, mintent);
                mThemeStatus.setAppliedPkgName(
                        "com.lewa.pkg." + mName, ThemeStatus.THEME_TYPE_LOCK_WALLPAPER);
                break;
            case SELECT_PICTRUE_FOR_LOCKSCREEN:
                startActivityForResult(getCropIntent(icon_uri, ThemeUtil.screen_width, ThemeUtil.screen_height), FROM_LOCKSCREEN);
                break;
            case SELECT_PICTRUE_FOR_WALLPAPER:
                startActivityForResult(getCropIntent(icon_uri, ThemeUtil.screen_width * 2, ThemeUtil.screen_height), FROM_WALLPAPER);
                break;
            case FROM_WALLPAPER:
                SetWallpaperThread swt = new SetWallpaperThread();
                swt.start();
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsReady = true;
        doResume();
    }

    public void doResume() {
        if (!mIsReady || !getUserVisibleHint()) {
            return;
        }
        mAdapter.refreshDownloadedThemes();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (null != mUsingBitmap) {
            mUsingBitmap.recycle();
            mUsingBitmap = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
    }

    protected abstract int getType();

    protected List<String> getAdditionalThemes(ThemeStatus status) {
        return new ArrayList<String>(0);
    }

    public abstract boolean isApplied(ThemeItem themeItem);

    public boolean isApplied(Uri themeUri) {
        return false;
    }

    public abstract void startActivity(View v);

    public abstract PreviewsType getThumbnailType();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    private static class HandlerAll extends Handler {
        private LocalBase mActivity;

        public HandlerAll(LocalBase activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE_BARS:
                    mActivity.hideBars();
                    break;
                case MSG_SET_WALLPAPER:
                    mActivity.setWallpaper();
                    break;
                default:
                    break;
            }
        }
    }

    protected static class ViewHolder {
        public ImageView thumbnail;
        public ImageView statusFlag;
        public TextView theme_name;

        public ViewHolder(View row) {
            thumbnail = (ImageView) row.findViewById(R.id.thumbnail);
            statusFlag = (ImageView) row.findViewById(R.id.status_flag);
            theme_name = (TextView) row.findViewById(R.id.theme_name);
            Animation animation = AnimationUtils.loadAnimation(row.getContext(), android.R.anim.fade_in);
            animation.setDuration(2000);
            thumbnail.startAnimation(animation);
        }
    }

    class SetWallpaperThread extends Thread {
        @Override
        public void run() {
            try {
                mContext.setWallpaper(mContext.getContentResolver()
                        .openInputStream(temp_uri));
                Long mName = System.currentTimeMillis();
                File f = new File(ThemeConstants.THEME_WALLPAPER + "/com.lewa.pkg." + mName + ".jpg");
                if (!f.exists()) {
                    boolean iscreate = f.createNewFile();
                }
                FileUtils.copyToFile(mContext.getContentResolver().openInputStream(temp_uri), f);
                WallpaperManager.getInstance(mContext).setStream(sWallpaperUtils.getCalculateStream(f.getAbsolutePath()));
                Settings.System.putString(mContext.getContentResolver(),"lewa_wallpaper_path", f.getPath());
                ThemeApplication.sThemeStatus.setAppliedThumbnail(Uri.fromFile(f)
                        , ThemeStatus.THEME_TYPE_WALLPAPER);
                ThemeApplication.sThemeStatus.setAppliedPkgName(
                        null, ThemeStatus.THEME_TYPE_LIVEWALLPAPER);
                mHandler.sendEmptyMessage(MSG_SET_WALLPAPER);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected class ThemeChooserAdapter extends ThemeAdapter implements View.OnClickListener {
        protected Context mContext = null;
        protected List<String> mAdditionalThemes;
        protected int mNumOfThemesInCursor;

        public ThemeChooserAdapter(Activity context) {
            super(context, getType());
            mContext = context;
            refreshDownloadedThemes();
        }

        public void refreshDownloadedThemes() {
            uriList.clear();
            mNumOfThemesInCursor = super.getCount();
            mAdditionalThemes = getAdditionalThemes(mThemeStatus);
            ThemeItem item;
            for (int i = 0; i < mNumOfThemesInCursor; i++) {
                item = (ThemeItem) getItem(i);
                String uri = Themes.getThemeUri(mContext, item.getPackageName(), item.getThemeId()).toSafeString();
                uriList.add(uri);
            }

            for (String s : mAdditionalThemes) {
                uriList.add(s);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View row = LayoutInflater.from(context).inflate(mItemLayoutResource, parent, false);
            row.setTag(new ViewHolder(row));
            return row;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ThemeItem themeItem = mDAOItem;
            if (null == themeItem) return;
            ViewHolder holder = (ViewHolder) view.getTag();
            int orientation = context.getResources().getConfiguration().orientation;
            if (!LocalFragment.mBusy) {
                if ((NewMechanismHelp.getThumbnails(context, themeItem, PreviewsType.LOCKSCREEN) == null)) {
                    holder.thumbnail.setImageURI(themeItem.getThumbnailUri());
                } else {
                    if (getType() == LOCKSCREEN_WALLPAPER_TYPE) {
                        imageLoader.displayImage(
                                themeItem.getLockWallpaperUri(mContext).toString(),
                                holder.thumbnail, options);
                    } else if (getType() == DESKTOP_WALLPAPER_TYPE){
                        imageLoader.displayImage(
                                themeItem.getWallpaperUri(mContext).toString(),
                                holder.thumbnail, options);
                    } else {
                        Uri url = NewMechanismHelp.getThumbnails(context, themeItem, getThumbnailType());
                        if (url != null && holder.thumbnail != null) {
                            imageLoader.displayImage(url.toString(), holder.thumbnail, options);
                        }
                    }
                }
            }
            if (getType() == DESKTOP_WALLPAPER_TYPE || getType() == LOCKSCREEN_WALLPAPER_TYPE) {
                holder.theme_name.setBackgroundDrawable(null);
            } else {
                holder.theme_name.setText(themeItem.getName());
            }
            holder.thumbnail.setTag(Themes.getThemeUri(
                    mContext, themeItem.getPackageName(), themeItem.getThemeId()));
            if (isApplied(themeItem)) {
                if (null == mUsingBitmap) {
                    try {
                        mUsingBitmap = BitmapFactory.decodeResource(
                                mContext.getResources(), R.drawable.ic_theme_using);
                    } catch (OutOfMemoryError e) {
                    }
                }
                holder.statusFlag.setImageBitmap(mUsingBitmap);
            } else {
                holder.statusFlag.setImageBitmap(null);
            }
            if (getType() != FONT_TYPE) holder.thumbnail.setOnClickListener(this);
        }

        @Override
        public Object getItem(int position) {
            if (position < mNumOfThemesInCursor) {
                return getDAOItem(position);
            } else {
                return mAdditionalThemes.get(position - mNumOfThemesInCursor);
            }
        }

        @Override
        public int getCount() {
            mNumOfThemesInCursor = super.getCount();
            return mNumOfThemesInCursor + mAdditionalThemes.size();
        }

        @Override
        public void onClick(View v) {
            startActivity(v);
        }
    }
}
