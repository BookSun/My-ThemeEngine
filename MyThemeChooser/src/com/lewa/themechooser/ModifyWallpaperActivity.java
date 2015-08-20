package com.lewa.themechooser;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.util.XmlUtils;
import com.lewa.themes.provider.Themes;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModifyWallpaperActivity extends ListActivity {

    private static final String LOCKWALLPAPER = "com.lewa.themechooser.custom.main.LockScreenWallpaper";
    private static final String WALLPAPER = "com.lewa.themechooser.custom.main.DeskTopWallpaper";
    private static final String LIVEWALLPAPER = "com.lewa.themechooser.custom.main.LiveWallpaper";
    private static final String VIDEOWALLPAPER = "com.mediatek.vlw.VideoEditor";
    private ActionBar mActionBar;
    private List<ResolveInfo> infos;
    private ThemeStatus mThemeStatus;
    private int mImgWidth;
    private int mImgHeight;
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private int padding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionBar = getActionBar();
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);
        mThemeStatus = ThemeApplication.sThemeStatus;
        mImgWidth = getResources().getDimensionPixelSize(R.dimen.thumb_disp_width);
        mImgHeight = getResources().getDimensionPixelSize(R.dimen.thumb_disp_height);
        padding = (int) getResources().getDimensionPixelSize(R.dimen.modify_wallpapaer_padding);
        getListView().setPadding(padding, 0, padding, 0);
        getListView().setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        setAdapter();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    private void setAdapter() {
        Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        List<ResolveInfo> tempinfos = getPackageManager().queryIntentActivities(pickWallpaper, 0);
        infos = new ArrayList<ResolveInfo>();
        for (int i = 0; i < tempinfos.size(); i++) {
            ResolveInfo info = tempinfos.get(i);
            if (WALLPAPER.equals(info.activityInfo.name)) {
                infos.add(info);
                tempinfos.remove(info);
            }
        }
        for (int i = 0; i < tempinfos.size(); i++) {
            ResolveInfo info = tempinfos.get(i);
            if (LIVEWALLPAPER.equals(info.activityInfo.name)) {
                infos.add(info);
                tempinfos.remove(info);
            }
        }
        for (int i = 0; i < tempinfos.size(); i++) {
            ResolveInfo info = tempinfos.get(i);
            if (VIDEOWALLPAPER.equals(info.activityInfo.name)) {
                infos.add(info);
                tempinfos.remove(info);
            }
        }
        for (int i = 0; i < tempinfos.size(); i++) {
            ResolveInfo info = tempinfos.get(i);
            if (LOCKWALLPAPER.equals(info.activityInfo.name)) {
                infos.add(info);
                tempinfos.remove(info);
            }
        }
        tempinfos.clear();
        tempinfos = null;
        setListAdapter(new WallpaperAdapter(this, infos));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent();
        startActivity(intent.setComponent(new ComponentName(infos.get(position).activityInfo.packageName, infos.get(position).activityInfo.name)));
        finish();
    }

    /**
     * get the thumbnail of a video.
     */
    private Bitmap createVideoThumbnail(Bitmap bm, int width, int height) {
        if (bm == null) {
            return bm;
        }
        bm.setDensity(DisplayMetrics.DENSITY_DEVICE);
        Bitmap newbm = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);
        newbm.setDensity(DisplayMetrics.DENSITY_DEVICE);
        Canvas c = new Canvas(newbm);
        c.setDensity(DisplayMetrics.DENSITY_DEVICE);
        Rect targetRect = new Rect();
        targetRect.left = targetRect.top = 0;
        targetRect.right = bm.getWidth();
        targetRect.bottom = bm.getHeight();

        int deltaw = width - targetRect.right;
        int deltah = height - targetRect.bottom;

        if (deltaw > 0 || deltah > 0) {
            // We need to scale up so it covers the entire
            // area.
            float scale = 1.0f;
            if (deltaw > deltah) {
                scale = width / (float) targetRect.right;
            } else {
                scale = height / (float) targetRect.bottom;
            }
            targetRect.right = (int) (targetRect.right * scale);
            targetRect.bottom = (int) (targetRect.bottom * scale);
            deltaw = width - targetRect.right;
            deltah = height - targetRect.bottom;
        }

        targetRect.offset(deltaw / 2, deltah / 2);
        Paint paint = new Paint();
        paint.setFilterBitmap(false);
        paint.setDither(true);
        c.drawBitmap(bm, null, targetRect, paint);

        bm.recycle();
        return newbm;
    }

    private class WallpaperAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Context context;
        private WallpaperManager wallpaperManager;
        private List<ResolveInfo> infos;

        public WallpaperAdapter(Context context, List infos) {
            mInflater = LayoutInflater.from(context);
            this.context = context;
            wallpaperManager = WallpaperManager.getInstance(context);
            this.infos = infos;

        }

        public int getCount() {
            return infos.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.flag = (ImageView) convertView.findViewById(R.id.status_flag);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(infos.get(position).loadLabel(context.getPackageManager()));
            String className = infos.get(position).activityInfo.name;
            WallpaperInfo wallInfo = wallpaperManager.getWallpaperInfo();
            if (LOCKWALLPAPER.equals(className)) {
                java.io.File f = null;
                f = new java.io.File("/data/system/face/lockwallpaper");
                if (!f.exists()) {
                    f = new java.io.File("/system/media/theme/lockwallpaper");
                }
                imageLoader.displayImage(Uri.fromFile(f).toString(), holder.icon);
                holder.flag.setImageResource(R.drawable.ic_theme_using);
            }
            if (WALLPAPER.equals(className)) {
                if (ThemeApplication.sThemeStatus.getAppliedPkgName(ThemeStatus.THEME_TYPE_WALLPAPER) != null
                        && ThemeApplication.sThemeStatus.getAppliedPkgName(ThemeStatus.THEME_TYPE_LIVEWALLPAPER) == null
                        && wallInfo == null) {
                    holder.icon.setImageDrawable(wallpaperManager.getDrawable());
                    holder.flag.setImageResource(R.drawable.ic_theme_using);
                } else {
                    imageLoader.displayImage(Themes.getTheme(context, "com.lewa.theme.LewaDefaultTheme", "LewaDefaultTheme").getWallpaperUri(context).toString(), holder.icon);
                    holder.flag.setImageBitmap(null);
                }
            }
            if (LIVEWALLPAPER.equals(className)) {
                if (ThemeApplication.sThemeStatus
                        .getAppliedPkgName(ThemeStatus.THEME_TYPE_LIVEWALLPAPER) != null
                        && (wallInfo != null && !"com.mediatek.vlw".equals(wallInfo.getPackageName()))) {
                    holder.icon.setImageDrawable(wallInfo.loadThumbnail(context.getPackageManager()));
                    holder.flag.setImageResource(R.drawable.ic_theme_using);
                } else {
                    List<ResolveInfo> list = context.getPackageManager().queryIntentServices(
                            new Intent(WallpaperService.SERVICE_INTERFACE),
                            PackageManager.GET_META_DATA);
                    for (ResolveInfo info : list) {
                        try {
                            WallpaperInfo winfo = new WallpaperInfo(context, info);
                            if ("com.android.phasebeam".equals(winfo.getPackageName())) {
                                holder.icon.setImageDrawable(winfo.loadThumbnail(context.getPackageManager()));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    holder.flag.setImageBitmap(null);
                }
            }
            if (VIDEOWALLPAPER.equals(className)) {
                Context videoContext = null;
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    videoContext = context.createPackageContext("com.mediatek.vlw", Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
                    File sp = videoContext.getSharedPrefsFile("vlw");
                    BufferedInputStream str = new BufferedInputStream(new FileInputStream(sp), 16 * 1024);
                    HashMap map = XmlUtils.readMapXml(str);
                    str.close();
                    retriever.setDataSource(context, Uri.parse((String) map.get("uri")));
                } catch (Exception e) {
                    String default_video_path = "";
                    default_video_path = videoContext.getResources().getString(videoContext.getResources().getIdentifier("default_video_path", "string", "com.mediatek.vlw"));
                    retriever.setDataSource(context, Uri.parse(default_video_path));
                } finally {
                    Bitmap bitmap = retriever.getFrameAtTime();
                    bitmap = createVideoThumbnail(bitmap, mImgWidth, mImgHeight);
                    holder.icon.setImageBitmap(bitmap);
                }
                if (wallInfo != null && "com.mediatek.vlw".equals(wallInfo.getPackageName())) {
                    holder.flag.setImageResource(R.drawable.ic_theme_using);
                }
            }
            return convertView;
        }

        public class ViewHolder {
            TextView text;
            ImageView icon;
            ImageView flag;
        }
    }
}
