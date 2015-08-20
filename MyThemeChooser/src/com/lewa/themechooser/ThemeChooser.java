package com.lewa.themechooser;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import util.ThemeUtil;
import util.ThemeUtil.UpdateThemeInfoThread;

import static com.lewa.themes.ThemeManager.STANDALONE;

import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBar.TabListener;
import lewa.support.v7.app.ActionBarActivity;

public class ThemeChooser extends ActionBarActivity {

    private static final Boolean DBG = true;
    private static final String TAG = "ThemeChooser";
    private static final int TAB_INDEX_CUSTOMER = 0;
    private static final int TAB_INDEX_LOCAL = 1;
    private static final int TAB_INDEX_ONLINE = 2;
    private static final int TAB_INDEX_COUNT = 3;
    final String CUSTOM_TAG = "tab-pager-custom";
    final String LOCAL_TAG = "tab-pager-local";
    final String ONLINE_TAG = "tab-pager-online";
    private final Handler mHandler = new Handler();
    private final TabListener mTabListener = new TabListener() {
        @Override
        public void onTabSelected(Tab tab, android.support.v4.app.FragmentTransaction ft) {
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, android.support.v4.app.FragmentTransaction ft) {

        }

        @Override
        public void onTabReselected(Tab tab, android.support.v4.app.FragmentTransaction ft) {

        }
    };
    private boolean show_custom_theme;
    private CustomerFragment customerFragment;
    private LocalFragment localFragment;
    private OnLineThemeFragment onLineThemeFragment;
    private ViewPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewPager = new ViewPager(this);
        mViewPager.setId(android.R.id.tabhost);
        setContentView(mViewPager);

        ThemeUtil.initLocale(this);
        show_custom_theme = getResources().getBoolean(R.bool.show_custom_theme);
        //Log.e("yixiao","yixiao model == "+android.os.Build.MODEL);

        //delete CustomerFragment for escape 2015.2.12
        if (("V350L").equals(android.os.Build.MODEL)) {
            show_custom_theme = false;
        }
        if (show_custom_theme) {
            setupCustomerTheme();
        }
        setupLocalTheme();
        setupOnlineTheme();
        mPagerAdapter = new ViewPagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);
        createViewsAndFragments(savedInstanceState);
        mViewPager.setOnPageChangeListener(mPagerAdapter);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        initURL();
        setCurrentTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, R.string.theme_sd_not_avaliable, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            UpdateThemeInfoThread.start(this, true, true);
        }
        ThemeApplication.sThemeStatus.checkDownloadStatus();
    }

    private void createViewsAndFragments(Bundle savedState) {
        final FragmentManager fragmentManager = getFragmentManager();

        // Hide all tabs (the current tab will later be reshown once a tab is selected)
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        customerFragment = new CustomerFragment();

        localFragment = new LocalFragment();

        onLineThemeFragment = new OnLineThemeFragment();
        onLineThemeFragment.setLoadOnCreate(false);

        transaction.add(android.R.id.tabhost, localFragment, LOCAL_TAG);
        transaction.add(android.R.id.tabhost, customerFragment, CUSTOM_TAG);
        transaction.add(android.R.id.tabhost, onLineThemeFragment, ONLINE_TAG);

        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
    }

    private void setCurrentTab() {
        if (show_custom_theme) {
            mViewPager.setCurrentItem(TAB_INDEX_LOCAL);
        } else {
            mViewPager.setCurrentItem(TAB_INDEX_LOCAL - 1);
        }
    }

    protected void initURL() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        float density = displayMetrics.density;

        if (displayMetrics.widthPixels == 480
                && displayMetrics.heightPixels == 854) {
            ThemeUtil.isWVGA = true;
        }
        if (density == 1.0) {
            ThemeUtil.isWVGA = false;

        } else if (density == 1.5) {
            ThemeUtil.isWVGA = true;
        } else {
            ThemeUtil.isWVGA = false;
        }

        if (displayMetrics.densityDpi == 120) {
            ThemeUtil.screenDPI = "LDPI";
        } else if (displayMetrics.densityDpi == 160) {
            ThemeUtil.screenDPI = "MDPI";
        } else if (displayMetrics.densityDpi == 240) {
            ThemeUtil.screenDPI = "HDPI";
        } else if (displayMetrics.densityDpi == 320) {
            ThemeUtil.screenDPI = "XHDPI";
        } else if (displayMetrics.densityDpi == 480) {
            ThemeUtil.screenDPI = "XXHDPI";
        } else {
            ThemeUtil.screenDPI = "HDPI";
        }
        ThemeUtil.displayMetrics = displayMetrics.densityDpi;
        ThemeUtil.density = density;
        ThemeUtil.initURL();
    }

    private void setupCustomerTheme() {
        final ActionBar.Tab tab = getSupportActionBar().newTab();
        tab.setContentDescription(R.string.theme_customize);
        tab.setText(R.string.theme_customize);
        tab.setTabListener(mTabListener);
        getSupportActionBar().addTab(tab);
    }

    private void setupLocalTheme() {
        final ActionBar.Tab tab = getSupportActionBar().newTab();
        tab.setContentDescription(R.string.theme_local);
        tab.setText(R.string.theme_local);
        tab.setTabListener(mTabListener);
        getSupportActionBar().addTab(tab);
    }

    private void setupOnlineTheme() {
        final ActionBar.Tab tab = getSupportActionBar().newTab();
        tab.setContentDescription(R.string.theme_online);
        tab.setText(R.string.theme_online);
        tab.setTabListener(mTabListener);
        getSupportActionBar().addTab(tab);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Runtime.getRuntime().gc();
    }

    public class ViewPagerAdapter extends PagerAdapter implements OnPageChangeListener {
        private FragmentTransaction mCurTransaction = null;
        private FragmentManager mFragmentManager;
        private Fragment mCurrentPrimaryItem;

        public ViewPagerAdapter() {
            mFragmentManager = getFragmentManager();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            mCurTransaction.show(f);
            return f;
        }

        private Fragment getFragment(int position) {
            if (!show_custom_theme) {
                position += 1;
            }
            switch (position) {
                case TAB_INDEX_CUSTOMER:
                    return customerFragment;
                case TAB_INDEX_LOCAL:
                    return localFragment;
                case TAB_INDEX_ONLINE:
                    return onLineThemeFragment;
            }
            throw new IllegalStateException("No fragment at position "
                    + position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public int getItemPosition(Object object) {
            if (object == customerFragment) {
                return TAB_INDEX_CUSTOMER;
            }
            if (object == localFragment) {
                return TAB_INDEX_LOCAL;
            }
            if (object == onLineThemeFragment) {
                return TAB_INDEX_ONLINE;
            }
            return -1;
        }

        @Override
        public int getCount() {
            if (show_custom_theme) {
                return TAB_INDEX_COUNT;
            } else {
                return TAB_INDEX_COUNT - 1;
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {

            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                }
                if (fragment != null) {
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            getSupportActionBar()
                        .smoothScrollTabIndicator(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            final ActionBar actionBar = getSupportActionBar();

            actionBar.selectTab(actionBar.getTabAt(position));
            if (!show_custom_theme) {
                position += 1;
            }
            switch (position) {
                case TAB_INDEX_ONLINE:
                    localFragment.setUserVisibleHint(false);
                    customerFragment.setUserVisibleHint(false);
                    if (!onLineThemeFragment.getLoadOnCreate()) {
                        onLineThemeFragment.setLoadOnCreate(true);
                        onLineThemeFragment.getAllData();
                    } else {
                        onLineThemeFragment.setUserVisibleHint(true);
                        onLineThemeFragment.doResume();
                    }
                    break;
                case TAB_INDEX_LOCAL:
                    localFragment.setUserVisibleHint(true);
                    onLineThemeFragment.setUserVisibleHint(false);
                    customerFragment.setUserVisibleHint(false);
                    localFragment.doResume();
                    break;
                case TAB_INDEX_CUSTOMER:
                    customerFragment.setUserVisibleHint(true);
                    localFragment.setUserVisibleHint(false);
                    onLineThemeFragment.setUserVisibleHint(false);
//                    customerFragment.doResume();
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            getSupportActionBar().setScrollState(state);
        }
    }
}
