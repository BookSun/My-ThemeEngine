package com.lewa.themechooser;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lewa.themechooser.preview.slide.local.LocalPreviewIconsActivity;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.widget.ThemeAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;
import util.ThemeUtil;

import java.util.ArrayList;

public class LocalFragment extends Fragment {
    //static data
    private static final Boolean DBG = true;
    private static final String TAG = "LocalFragment";
    private static final int MSG_HIDE_BARS = 1;
    private static final int HIDE_BARS_TIMEOUT = 2000;
    //data
    public static boolean mBusy = false;
    //view
    protected static GridView mLocalGridView = null;
    //object
    private static ThemeChooserAdapter mAdapter;
    protected Animation mBottomBarAnimShow = null;
    protected Animation mBootomBarAnimHide = null;
    protected ImageLoader imageLoader = ImageLoader.getInstance();
    private Handler mHandler;
    private LinearLayout mEndToast;
    private Bitmap mUsingBitmap;
    private boolean mIsReady;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.theme_local_main, container, false);
        mLocalGridView = (GridView) view.findViewById(R.id.local_theme_grid);

        mEndToast = (LinearLayout) view.findViewById(R.id.theme_local_end_view);
        mHandler = new HandlerAll(this);
        mAdapter = new ThemeChooserAdapter(getActivity());
        mBottomBarAnimShow = AnimationUtils.loadAnimation(getActivity(), R.anim.theme_bottom_bar_enter);
        mBootomBarAnimHide = AnimationUtils.loadAnimation(getActivity(), R.anim.theme_bottom_bar_exit);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void hideBars() {
        mEndToast.startAnimation(mBootomBarAnimHide);
        mBootomBarAnimHide.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mLocalGridView.scrollTo(0, 0);
                mEndToast.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsReady = true;
    }

    public void doResume() {
        if (!mIsReady || !getUserVisibleHint()) {
            return;
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLocalGridView.setAdapter(mAdapter);
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
    }

    private static class HandlerAll extends Handler {
        private LocalFragment mActivity;

        public HandlerAll(LocalFragment activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE_BARS:
                    mActivity.hideBars();
                    break;
                default:
                    break;
            }
        }
    }

    private static class ViewHolder {
        // public ImageView thumbnailframe;
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

    private class ThemeChooserAdapter extends ThemeAdapter implements View.OnClickListener {
        public ArrayList<String> uriList = new ArrayList<String>();
        protected Context mContext = null;
        private String appliedThemeId;
        private String appliedThemePkgName;

        public ThemeChooserAdapter(Activity context) {
            super(context);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View row = LayoutInflater.from(context).inflate(R.layout.theme_grid_item_thumbnail, parent, false);
            row.setTag(new ViewHolder(row));
            return row;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ThemeItem themeItem = mDAOItem;
            ViewHolder holder = (ViewHolder) view.getTag();
            imageLoader.displayImage(themeItem.getThumbnailUri() != null ? themeItem.getThumbnailUri().toString() : null, holder.thumbnail);
            holder.theme_name.setText(themeItem.getName());
            holder.thumbnail.setTag(Themes.getThemeUri(
                    mContext, themeItem.getPackageName(), themeItem.getThemeId()));

            if (ThemeApplication.sThemeStatus.isApplied(themeItem.getPackageName(), ThemeStatus.THEME_TYPE_PACKAGE)) {
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
            holder.thumbnail.setOnClickListener(this);
        }

        @Override
        public Object getItem(int position) {
            return getDAOItem(position);
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            ThemeItem item;
            uriList.clear();
            for (int i = 0; i < getCount(); i++) {
                item = (ThemeItem) getItem(i);
                uriList.add(Themes.getThemeUri(mContext, item.getPackageName(), item.getThemeId()).toSafeString());
            }
            intent.setClass(mContext, LocalPreviewIconsActivity.class);
            Bundle mExtras = new Bundle();
            mExtras.putStringArrayList("themes_uri", uriList);
            intent.putExtra("extras_themes_uri", mExtras);
            intent.setData((Uri) v.getTag());
            mContext.startActivity(intent);

        }
    }
}
