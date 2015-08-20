package com.lewa.themechooser;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.lewa.themechooser.adapters.online.ThumbnailOnlineAdapter;
import com.lewa.themechooser.custom.fragment.online.OnLineFontsFragment;
import com.lewa.themechooser.pojos.ThemeBase;
import com.lewa.themechooser.server.intf.ClientResolver;
import com.lewa.themechooser.server.intf.NetBaseParam;
import com.lewa.themechooser.server.intf.jsonimpl.LewaServerJsonParser;
import com.lewa.themechooser.widget.PageGridView;
import com.lewa.themes.ThemeManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import util.ThemeUtil;

public abstract class OnLineBaseFragment
        extends Fragment implements View.OnClickListener, OnScrollListener {
    //static data
    private static final Boolean DBG = true;
    private static final String TAG = "OnLineBaseFragment";
    public static boolean mBusy = false;
    public boolean isLoading = false;
    protected Animation mBottomBarAnimShow = null;
    protected Animation mBootomBarAnimHide = null;
    protected ThumbnailOnlineAdapter mOnlinePreviewAdapter = null;
    protected ArrayList<ThemeBase> themeBases = new ArrayList<ThemeBase>();
    //view
    protected Fragment mFragment;
    protected int mLayoutRes;
    //object
    private ClientResolver clientResolver;
    private LoadPictruesTask mlp;
    private NetBaseParam netBaseParam = null;
    private RequestQueue mRequestQueue = null;
    private Button mSetNetWorkNoNetWorkBtn = null;
    private ImageView mThemeErrorImage = null;
    private TextView mThemeErrorMes = null;
    private Button mRefreshBtn = null;
    private ProgressBar mProgressBar = null;
    private TextView mNoDataTv = null;
    private ImageView mNoDataIv = null;
    private ListView mOnlineGridView = null;
    private PageGridView mPageGridView = null;
    //data
    private int mLastItem;
    private boolean mLoadOnCreate = true;
    private boolean mViewCreated = false;
    private boolean mIsReady;
    private int countPage = 1;
    private boolean firstGetOnLineWallpaper = true;
    //yixiao add for volley NOConnectionError
    int requestCount = 0;
    private boolean isNoData = false;
    private void networkTimeout() {
        showNetWorkBadView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragment = this;
        mLayoutRes = com.lewa.themechooser.R.layout.theme_online_no_network;
        mRequestQueue = ThemeUtil.getInstence();
    }

    @Override
    public View onCreateView(LayoutInflater inflater
            , ViewGroup container, Bundle savedInstanceState) {
        View mNoNetworkView = inflater.inflate(mLayoutRes, container, false);
        mSetNetWorkNoNetWorkBtn = (Button) mNoNetworkView
                .findViewById(R.id.theme_network_set_nonetwork);
        mRefreshBtn = (Button) mNoNetworkView.findViewById(R.id.theme_refresh);
        mThemeErrorImage = (ImageView) mNoNetworkView.findViewById(R.id.theme_err_img);
        mThemeErrorMes = (TextView) mNoNetworkView.findViewById(R.id.theme_err_msg);
        if (this instanceof OnLineFontsFragment) {
            mProgressBar = (ProgressBar) mNoNetworkView.findViewById(R.id.online_font_progress_bar);
            mOnlineGridView = (ListView) mNoNetworkView.findViewById(R.id.online_theme_grid);
        } else {
            mProgressBar = (ProgressBar) mNoNetworkView.findViewById(R.id.online_base_progress_bar);
            mPageGridView = (PageGridView) mNoNetworkView.findViewById(R.id.online_theme_grid);
        }
        mNoDataTv = (TextView) mNoNetworkView.findViewById(R.id.theme_no_data);
        mNoDataIv = (ImageView) mNoNetworkView.findViewById(R.id.iv_no_data);
        mBottomBarAnimShow = AnimationUtils.loadAnimation(
                getActivity(), R.anim.theme_bottom_bar_enter);
        mBootomBarAnimHide = AnimationUtils.loadAnimation(
                getActivity(), R.anim.theme_bottom_bar_exit);
        mViewCreated = true;
        return mNoNetworkView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mLoadOnCreate) {
            getAllData();
        }
    }

    public void getAllData() {
        if (!mViewCreated) {
            mLoadOnCreate = true;
            return;
        }
        if (!ThemeUtil.isNetWorkEnable(getActivity())) {
            showNetWorkBadView();
        } else {
            //get request url
            if (countPage == 1)
                showLoadingView();
            netBaseParam = initUrl();
            String url = netBaseParam.changeString(countPage);

            //request data
            if (null != mRequestQueue) {
                mRequestQueue.add(new JsonArrayRequest(url,
                                new Response.Listener<JSONArray>() {

                                    @Override
                                    public void onResponse(JSONArray response) {
                                        ArrayList<ThemeBase> tempList;
                                        tempList = (ArrayList<ThemeBase>) jsonParser(response);

                                        ThemeBase tb = tempList.get(0);
                                        if (countPage == 1 && tb.getPackageName().equals("nothing")) {
                                            showNoDataView();
                                            return;
                                        } else if (countPage != 1 && tb.getPackageName().equals("nothing")) {
                                            mPageGridView.updateFooter(View.GONE);
                                            mPageGridView.setOnScrollListener(null);
                                            Toast.makeText(getActivity(),
                                                    getString(R.string.theme_loading_more_end), Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }
                                        showLoadedView();
                                        if (null == mOnlinePreviewAdapter) {
                                            mOnlinePreviewAdapter = onlineAdapterInstance();
                                        }

                                        themeBases.addAll(tempList);
                                        //#65679 add begin by bin.dong
                                        if (netBaseParam.typeId == ThemeConstants.DESKTOP_WALLPAPER && firstGetOnLineWallpaper) {
                                            for (int i = 5; i >= 0; i--) {
                                                themeBases.remove(themeBases.get(i));
                                            }
                                            firstGetOnLineWallpaper = false;
                                        }
                                        //#65679 add end by bin.dong
                                        if (mLayoutRes == R.layout.online_fonts) {
                                            mOnlineGridView.setAdapter(mOnlinePreviewAdapter);
                                        } else {
                                            if (countPage == 1) {
                                                mPageGridView.setAdapter(mOnlinePreviewAdapter);
                                            } else {
                                                mPageGridView.updateFooter(View.GONE);
                                                mOnlinePreviewAdapter.notifyDataSetChanged();
                                            }
                                        }
                                        if (mLayoutRes == R.layout.online_fonts) {
                                            mOnlineGridView.setOnItemClickListener(mOnlinePreviewAdapter);
                                            if (tempList.size() == ThemeConstants.DEFAULT_PAGE_SIZE) {
                                                if (mPageGridView != null) {
                                                    mPageGridView.setGravity(Gravity.CENTER);
                                                }
                                                mOnlineGridView.setOnScrollListener(OnLineBaseFragment.this);
                                            } else {
                                                mOnlineGridView.setOnScrollListener(null);
                                            }
                                        } else {
                                            if (tempList.size() == ThemeConstants.DEFAULT_PAGE_SIZE) {
                                                mPageGridView.setGravity(Gravity.CENTER);
                                                mPageGridView.setOnScrollListener(OnLineBaseFragment.this);
                                            } else {
                                                mPageGridView.setOnScrollListener(null);
                                            }
                                        }
                                        isLoading = false;
                                        countPage = ++countPage;
                                        isNoData = false;
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                //yixiao add for volley NOConnectionError
                                if(requestCount < 3){
                                    getAllData();
                                    requestCount ++;
                                }else{
                                    // #64926 Modify by Fan.Yang
                                  if (countPage <= 1) {
                                    showNoDataView();
                                  } else {
                                    mPageGridView.updateFooter(View.GONE);
                                  }
                                }
                                return;
                            }
                        }).setShouldCache(true)
                );
                mRequestQueue.start();
            }
        }
    }

    public List<ThemeBase> jsonParser(JSONArray jsonArray) {
        return LewaServerJsonParser.parseListThemeBase(jsonArray
                , NetBaseParam.isPackgeResource(netBaseParam.actualtype)
                , netBaseParam
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mOnlinePreviewAdapter) {
//            mOnlinePreviewAdapter.notifyDataSetChanged();
        }
        mIsReady = true;
        if (ThemeUtil.isUsingChanged) {
            ThemeUtil.isUsingChanged = false;
        }
    }

    public void doResume() {
        if (!mIsReady || !getUserVisibleHint()) {
            return;
        }
        if (null != mOnlinePreviewAdapter) {
            mOnlinePreviewAdapter.notifyDataSetChanged();
        }
        if(isNoData){
            getAllData();
        }
    }

    private void showLoadingView() {

        mSetNetWorkNoNetWorkBtn.setVisibility(View.GONE);

        mRefreshBtn.setVisibility(View.GONE);
        mThemeErrorImage.setVisibility(View.GONE);
        mThemeErrorMes.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        if (this instanceof OnLineFontsFragment) {
            mOnlineGridView.setVisibility(View.VISIBLE);
        } else {
            mPageGridView.setVisibility(View.VISIBLE);
        }
        if (null != mNoDataIv) mNoDataIv.setVisibility(View.GONE);
        mNoDataTv.setVisibility(View.GONE);
    }

    private void showLoadedView() {
        mSetNetWorkNoNetWorkBtn.setVisibility(View.GONE);

        mRefreshBtn.setVisibility(View.GONE);
        mThemeErrorImage.setVisibility(View.GONE);
        mThemeErrorMes.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);

        if (null != mNoDataIv) mNoDataIv.setVisibility(View.GONE);
        mNoDataTv.setVisibility(View.GONE);
    }

    private void showNoDataView() {
        mSetNetWorkNoNetWorkBtn.setVisibility(View.GONE);
        mRefreshBtn.setVisibility(View.GONE);
        mThemeErrorImage.setVisibility(View.GONE);
        mThemeErrorMes.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        if (this instanceof OnLineFontsFragment) {
            mOnlineGridView.setVisibility(View.GONE);
        } else {
            mPageGridView.setVisibility(View.GONE);
        }
        if (null != mNoDataIv) mNoDataIv.setVisibility(View.VISIBLE);
        mNoDataTv.setVisibility(View.VISIBLE);
        isNoData = true;
    }

    private void showNetWorkBadView() {
        //Delete for standalone by Fan.Yang
        if (!ThemeManager.STANDALONE /**&& lewa.util.NetPolicyUtils.isNetworkBlocked(getActivity(), getActivity().getPackageName())*/) {
            mSetNetWorkNoNetWorkBtn.setText(R.string.set_network);
            mSetNetWorkNoNetWorkBtn.setVisibility(View.VISIBLE);
            mSetNetWorkNoNetWorkBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Delete for standalone by Fan.Yang
                    //lewa.util.NetPolicyUtils.startFirewallActivity(getActivity());
                }
            });
            mRefreshBtn.setVisibility(View.VISIBLE);
            mRefreshBtn.setOnClickListener(this);
            mThemeErrorImage.setVisibility(View.VISIBLE);
            mThemeErrorMes.setText(R.string.network_closed);
            mThemeErrorMes.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            if (this instanceof OnLineFontsFragment) {
                mOnlineGridView.setVisibility(View.GONE);
            } else {
                mPageGridView.setVisibility(View.GONE);
            }
            if (null != mNoDataIv) mNoDataIv.setVisibility(View.GONE);
            mNoDataTv.setVisibility(View.GONE);

        } else {
            mSetNetWorkNoNetWorkBtn.setText(R.string.theme_network_setting);
            mSetNetWorkNoNetWorkBtn.setVisibility(View.VISIBLE);
            mSetNetWorkNoNetWorkBtn.setOnClickListener(this);

            mRefreshBtn.setVisibility(View.VISIBLE);
            mRefreshBtn.setOnClickListener(this);
            mThemeErrorImage.setVisibility(View.VISIBLE);
            mThemeErrorMes.setText(R.string.theme_err_msg_network);
            mThemeErrorMes.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            if (this instanceof OnLineFontsFragment) {
                mOnlineGridView.setVisibility(View.GONE);
            } else {
                mPageGridView.setVisibility(View.GONE);
            }
            if (null != mNoDataIv) mNoDataIv.setVisibility(View.GONE);
            mNoDataTv.setVisibility(View.GONE);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.theme_network_set_nonetwork: {
                setNetWork();
                break;
            }
            case R.id.theme_refresh: {
                refreshNetWork();
                break;
            }
            default:
                break;
        }
    }

    private void setNetWork() {
        Intent intent = new Intent();
        intent.setAction(ThemeManager.SET_NETWORK);
        startActivity(intent);
    }

    private void refreshNetWork() {
        if (ThemeUtil.isNetWorkEnable(getActivity())) {

            netBaseParam = initUrl();

            showLoadingView();

            clientResolver = new ClientResolver(netBaseParam, ClientResolver.JSON_IMPL,
                    ClientResolver.DEFAULT_PAGE_SIZE);
            if (mlp != null && !mlp.isCancelled()) {
                mlp.cancel(true);
            }
            mlp = new LoadPictruesTask(getActivity());
            mlp.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "firstpage");
        } else {
            showNetWorkBadView();
        }
    }

    protected abstract NetBaseParam initUrl();

    protected abstract ThumbnailOnlineAdapter onlineAdapterInstance();

    @Override
    public void onStop() {
        super.onStop();
        if (mlp != null && !mlp.isCancelled()) {
            mlp.cancel(true);
        }
        if (mOnlinePreviewAdapter != null) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void requestNextPage() {
        if (this instanceof OnLineFontsFragment) {
            mOnlineGridView.setVisibility(View.VISIBLE);
        } else {
            mPageGridView.updateFooter(View.VISIBLE);
        }
        getAllData();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                mBusy = false;
                if (view.getLastVisiblePosition() == (view.getCount() - 1)) {
                    if (this instanceof OnLineFontsFragment) {

                    } else {
                        if (!mPageGridView.isVisible()) {
                            requestNextPage();
                        }
                    }
                }
                break;
            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                mBusy = true;
                break;
            case OnScrollListener.SCROLL_STATE_FLING:
                mBusy = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        mLastItem = firstVisibleItem + visibleItemCount - 1;
    }

    public boolean getLoadOnCreate() {
        return mLoadOnCreate;
    }

    public void setLoadOnCreate(boolean loadOnCreate) {
        mLoadOnCreate = loadOnCreate;
    }

    private class LoadPictruesTask extends AsyncTask<String, String, Boolean> {
        private ArrayList<ThemeBase> tempList = new ArrayList<ThemeBase>();

        public LoadPictruesTask(Context context) {
        }

        @Override
        protected Boolean doInBackground(String... parms) {
            tempList = (ArrayList<ThemeBase>) clientResolver.getPageResolver()
                    .getNextPageContent(countPage);
            if (tempList != null
                    && tempList.size() != 0) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            isLoading = true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                ThemeBase tb = tempList.get(0);
                if (countPage == 1 && tb.getPackageName().equals("nothing")) {
                    showNoDataView();
                    return;
                } else if (countPage != 1 && tb.getPackageName().equals("nothing")) {
                    mPageGridView.updateFooter(View.GONE);
                    mPageGridView.setOnScrollListener(null);
                    return;
                }
                showLoadedView();
                if (null != mOnlinePreviewAdapter) {
                } else {
                    mOnlinePreviewAdapter = onlineAdapterInstance();
                }
                themeBases.addAll(tempList);
                if (mLayoutRes == com.lewa.themechooser.R.layout.online_fonts) {
                    mOnlineGridView.setAdapter(mOnlinePreviewAdapter);
                } else {
                    if (countPage == 1) {
                        mPageGridView.setAdapter(mOnlinePreviewAdapter);
                    } else {
                        mPageGridView.updateFooter(View.GONE);
                        mOnlinePreviewAdapter.notifyDataSetChanged();
                    }
                }

                if (mLayoutRes == com.lewa.themechooser.R.layout.online_fonts) {
                    mOnlineGridView.setOnItemClickListener(mOnlinePreviewAdapter);
                    if (tempList.size() == ThemeConstants.DEFAULT_PAGE_SIZE) {
                        mPageGridView.setGravity(Gravity.CENTER);
                        mOnlineGridView.setOnScrollListener(OnLineBaseFragment.this);
                    } else {
                        mOnlineGridView.setOnScrollListener(null);
                    }
                } else {
                    if (tempList.size() == ThemeConstants.DEFAULT_PAGE_SIZE) {
                        mPageGridView.setGravity(Gravity.CENTER);
                        mPageGridView.setOnScrollListener(OnLineBaseFragment.this);
                    } else {
                        mPageGridView.setOnScrollListener(null);
                    }
                }
                isLoading = false;
                countPage = ++countPage;
            } else {
                if (countPage != 1) {
                    Toast.makeText(getActivity(), getString(R.string.theme_loading_more_fail), Toast.LENGTH_SHORT).show();
                    if (mLayoutRes == com.lewa.themechooser.R.layout.online_fonts) {

                    } else {
                        mPageGridView.updateFooter(View.GONE);
                    }
                } else {
                    networkTimeout();
                }
            }
        }
    }
}
