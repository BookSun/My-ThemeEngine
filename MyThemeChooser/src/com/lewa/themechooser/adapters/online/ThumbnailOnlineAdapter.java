package com.lewa.themechooser.adapters.online;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themechooser.pojos.ThemeBase;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.ArrayList;

import util.ThemeUtil;

public abstract class ThumbnailOnlineAdapter extends BaseAdapter implements OnClickListener
        , AdapterView.OnItemClickListener, ThemeStatus.OnStatusChangeListener {
    public static final int REQUESTCODE = 1;
    private static final String TAG = "ThumbnailOnlineAdapter";
    private static final Boolean DBG = true;
    public static Activity comingSource;
    public static ArrayList<ThemeBase> tempThemeBases;
    protected static Context mContext;
    public ArrayList<ThemeBase> mThemeBases;
    public ArrayList<String> downloadedModels = new ArrayList<String>();
    protected String url = null;
    protected String thumbnailPath = null;
    protected ImageLoader imageLoader = ImageLoader.getInstance();
    protected ThemeStatus mStatus;
    protected int mThemeType;
    protected int mLayoutRes;
    protected DisplayImageOptions options;
    private boolean sdcardIsFull = false;
    private Bitmap flagDownloaded = null;
    private Bitmap flagDownloading = null;
    private Bitmap flagHasNewVersion = null;
    private Bitmap flagUsing = null;
    private LayoutInflater mInflater = null;
    private int mCount = 0;
    private SparseArray<Bitmap> statusFlags = new SparseArray<Bitmap>();

    public ThumbnailOnlineAdapter(Context context, ArrayList<ThemeBase> themeBases, int themeType) {
        mContext = context;
        mThemeType = themeType;
        mStatus = ThemeApplication.sThemeStatus;
        mStatus.setOnStatusChangeListener(this);
        mThemeBases = themeBases;
        mInflater = LayoutInflater.from(context);
        if (mThemeType == ThemeStatus.THEME_TYPE_FONT) {
            options = new DisplayImageOptions.Builder().showImageOnFail(R.drawable.bg_text_style)
                    .showImageForEmptyUri(R.drawable.bg_text_style).cacheInMemory(true).cacheOnDisc(true)
                    .showImageOnFail(R.drawable.bg_text_style)
                    .showImageOnLoading(R.drawable.bg_text_style)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .build();
        } else {
            options = new DisplayImageOptions.Builder()
                    .showImageOnFail(R.drawable.theme_no_default)
                    .showImageForEmptyUri(R.drawable.theme_no_default)
                    .showImageOnFail(R.drawable.theme_no_default)
                    .showImageOnLoading(R.drawable.theme_no_default)
                    .cacheInMemory(true).cacheOnDisc(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .imageScaleType(ImageScaleType.EXACTLY)
                            // .displayer(new RoundedBitmapDisplayer(12))
                    .build();
        }
        if (mThemeType == ThemeStatus.THEME_TYPE_LIVEWALLPAPER) {
            mLayoutRes = R.layout.live_wallpaper_entry;
        } else {
            mLayoutRes = R.layout.theme_grid_item_thumbnail;
        }
        int count = mThemeBases.size();
        for (int i = 0; i < count; i++) {
            statusFlags.put(i, null);
        }

        sdcardIsFull = false;
    }

    @Override
    public void onStatusChange() {
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View arg0) {
        comingSource = (Activity) mContext;
    }

    public Bitmap getFlagHasNewVersion() {
        if (null == flagHasNewVersion) {
            try {
                flagHasNewVersion = BitmapFactory
                        .decodeResource(mContext.getResources(), R.drawable.ic_theme_update);
            } catch (OutOfMemoryError e) {
            }
        }
        return flagHasNewVersion;
    }

    public Bitmap getFlagDownloading() {
        if (null == flagDownloading) {
            try {
                flagDownloading = BitmapFactory
                        .decodeResource(mContext.getResources(), R.drawable.ic_theme_downloading);
            } catch (OutOfMemoryError e) {
            }
        }
        return flagDownloading;
    }

    public Bitmap getFlagDownloaded() {
        if (null == flagDownloaded) {
            try {
                flagDownloaded = BitmapFactory
                        .decodeResource(mContext.getResources(), R.drawable.ic_theme_downloaded);
            } catch (OutOfMemoryError e) {
            }
        }
        return flagDownloaded;
    }

    public Bitmap getFlagUsing() {
        if (null == flagUsing) {
            try {
                flagUsing = BitmapFactory
                        .decodeResource(mContext.getResources(), R.drawable.ic_theme_using);
            } catch (OutOfMemoryError e) {
            }
        }
        return flagUsing;
    }

    public void clearBitmaps() {
        if (null != flagDownloaded) {
            flagDownloaded.recycle();
            flagDownloaded = null;
        }
        if (null != flagDownloading) {
            flagDownloading.recycle();
            flagDownloading = null;
        }
        if (null != flagHasNewVersion) {
            flagHasNewVersion.recycle();
            flagHasNewVersion = null;
        }
        if (null != flagUsing) {
            flagUsing.recycle();
            flagUsing = null;
        }
    }

    @Override
    public int getCount() {
        return this.mThemeBases.size();
    }

    @Override
    public Object getItem(int position) {
        if (mThemeBases != null) {
            return mThemeBases.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        // reuse views
        if (rowView == null) {
            rowView  = LayoutInflater.from(mContext).inflate(mLayoutRes, null);
            // configure view holder
            ViewCache viewCache = new ViewCache();
            viewCache.thumbnail = (ImageView) rowView.findViewById(R.id.thumbnail);
            viewCache.statusFlag = (ImageView) rowView.findViewById(R.id.status_flag);
            viewCache.theme_name = (TextView) rowView.findViewById(R.id.theme_name);
            Animation animation = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in);
            animation.setDuration(2000);
            viewCache.thumbnail.startAnimation(animation);

            if (mThemeType == ThemeStatus.THEME_TYPE_FONT) {
                viewCache.thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                viewCache.thumbnail.setScaleType(ImageView.ScaleType.FIT_XY);
            }
            rowView.setTag(viewCache);
        }

        //fill data
        ViewCache viewCache = (ViewCache)rowView.getTag();
        ThemeBase themeBase = mThemeBases.get(position);
        if (this instanceof OnlineWallPaperAdapter
                || (this instanceof OnlineLockScreenWallPaperAdapter)) {
            viewCache.theme_name.setBackgroundDrawable(null);
        } else {
            if (ThemeUtil.isEN) {
                viewCache.theme_name.setText(themeBase.getEnName());
            } else {
                viewCache.theme_name.setText(themeBase.getCnName());
            }
        }
        String mFileName = themeBase.getPkg();
        int status = mStatus.getStatus(
                mThemeType == ThemeStatus.THEME_TYPE_LIVEWALLPAPER ? themeBase.getPackageName()
                        + themeBase.getThemeId() : themeBase.getPackageName(), mFileName,
                mThemeType, themeBase.getVersionCode()
        );
        switch (status) {
            case ThemeStatus.STATUS_APPLIED:
                statusFlags.put(position, getFlagUsing());
                break;
            case ThemeStatus.STATUS_OUTDATED:
                statusFlags.put(position, getFlagHasNewVersion());
                break;
            case ThemeStatus.STATUS_DOWNLOADING:
                statusFlags.put(position, getFlagDownloading());
                break;
            case ThemeStatus.STATUS_DOWNLOADED:
                statusFlags.put(position, getFlagDownloaded());
                break;
            default:
                statusFlags.put(position, null);
        }

        viewCache.statusFlag.setImageBitmap(statusFlags.get(position));

        /**
         * 将对应位置的标签Image给定pkg和position
         */
        viewCache.statusFlag.setTag(themeBase.getPkg());
        initPath(themeBase);
        if (!sdcardIsFull && !ThemeUtil.sdcardHasSpace(1)) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(mContext, mContext.getString(R.string.sdcard_removed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.sdcardfull),
                        Toast.LENGTH_SHORT).show();
            }
            sdcardIsFull = true;
        }
        imageLoader.displayImage(url, viewCache.thumbnail, options);
        viewCache.thumbnail.setTag(themeBase);
        viewCache.thumbnail.setOnClickListener(this);
        return rowView;
    }

    protected abstract void initPath(ThemeBase themeBase);

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    protected class ViewCache {
        public ImageView thumbnail;
        public ImageView statusFlag;
        public TextView theme_name;
    }
}
