package com.lewa.themechooser.custom;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.lewa.themechooser.OnLineBaseFragment;
import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.custom.fragment.local.DeskTopWallpaperFragment;
import com.lewa.themechooser.custom.fragment.local.FontsFragment;
import com.lewa.themechooser.custom.fragment.local.IconFragment;
import com.lewa.themechooser.custom.fragment.local.LiveWallpaperFragment;
import com.lewa.themechooser.custom.fragment.local.LocalBase;
import com.lewa.themechooser.custom.fragment.local.LocalLockScreenStyleFragment;
import com.lewa.themechooser.custom.fragment.local.LocalLockScreenWallpaperFragment;
import com.lewa.themechooser.custom.fragment.local.SystemAppFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineDeskTopWallpaperFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineFontsFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineIconFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineLiveWallpaperFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineLockScreenStyleFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineLockScreenWallpaperFragment;
import com.lewa.themechooser.custom.fragment.online.OnLineSystemAppFragment;
import com.lewa.themechooser.receiver.VerifyThemesCompletedReceiver.VerifyDownloadedImagesThread;
import com.lewa.themes.ThemeManager;

import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBar.TabListener;
import lewa.support.v7.app.ActionBarActivity;

public abstract class CustomBase extends ActionBarActivity {
    private static final int TAB_INDEX_LOCAL = 0;
    private static final int TAB_INDEX_ONLINE = 1;

    private static final int TAB_INDEX_COUNT = 2;
    final String LOCAL_TAG = "tab-pager-local";
    final String ONLINE_TAG = "tab-pager-online";

    private final TabListener mTabListener = new TabListener() {
        public void onTabReselected(Tab arg0, android.support.v4.app.FragmentTransaction arg1) {
        }

        public void onTabSelected(Tab tab, android.support.v4.app.FragmentTransaction ft) {
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onTabUnselected(Tab tab, android.support.v4.app.FragmentTransaction ft) {
        }
    };

    protected Fragment mLocalFragment = null;
    protected Fragment mOnlineFragment = null;
    private ViewPager mViewPager;
    private ViewPagerAdapter mPagerAdapter;

    private Fragment localFragment() {
        if (null == mLocalFragment) {
            mLocalFragment = getLocalFragment();
            mLocalFragment.setUserVisibleHint(false);
        }
        return mLocalFragment;
    }

    private Fragment onlineFragment() {
        if (null == mOnlineFragment) {
            mOnlineFragment = getOnLineFragment();
            mOnlineFragment.setUserVisibleHint(false);
        }
        return mOnlineFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createViewsAndFragments(savedInstanceState);
        mViewPager = new ViewPager(this);
        setContentView(mViewPager);
        mViewPager.setId(android.R.id.tabhost);
        mPagerAdapter = new ViewPagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(mPagerAdapter);
        setupLocalTheme();
        setupOnlineTheme();
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    private void createViewsAndFragments(Bundle savedState) {
        final FragmentManager fragmentManager = getFragmentManager();

        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        mLocalFragment = getLocalFragment();
        mOnlineFragment = getOnLineFragment();
        ((OnLineBaseFragment) mOnlineFragment).setLoadOnCreate(false);
        transaction.add(android.R.id.tabhost, mLocalFragment, LOCAL_TAG);
        transaction.add(android.R.id.tabhost, mOnlineFragment, ONLINE_TAG);
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
    }

    private void setupLocalTheme() {
        final ActionBar.Tab tab = getSupportActionBar().newTab();
        if (localFragment() instanceof SystemAppFragment) {
            tab.setContentDescription(R.string.theme_system_app_local);
            tab.setText(R.string.theme_system_app_local);
        }
        if (localFragment() instanceof DeskTopWallpaperFragment) {
            tab.setContentDescription(R.string.theme_wallpaper_local);
            tab.setText(R.string.theme_wallpaper_local);
        }
        if (localFragment() instanceof FontsFragment) {
            tab.setContentDescription(R.string.theme_font_local);
            tab.setText(R.string.theme_font_local);
        }
        if (localFragment() instanceof IconFragment) {
            tab.setContentDescription(R.string.theme_icons_local);
            tab.setText(R.string.theme_icons_local);
        }
        if (localFragment() instanceof LocalLockScreenStyleFragment) {
            tab.setContentDescription(R.string.theme_lockscreen_local);
            tab.setText(R.string.theme_lockscreen_local);
        }
        if (localFragment() instanceof LocalLockScreenWallpaperFragment) {
            tab.setContentDescription(R.string.theme_lockscreen_wallpaper_local);
            tab.setText(R.string.theme_lockscreen_wallpaper_local);
        }
        if (localFragment() instanceof LiveWallpaperFragment) {
            tab.setContentDescription(R.string.live_wallpaper_local);
            tab.setText(R.string.live_wallpaper_local);
        }

        tab.setTabListener(mTabListener);
        getSupportActionBar().addTab(tab);
    }

    private void setupOnlineTheme() {
        final ActionBar.Tab tab = getSupportActionBar().newTab();
        if (onlineFragment() instanceof OnLineSystemAppFragment) {
            tab.setContentDescription(R.string.theme_system_app_online);
            tab.setText(R.string.theme_system_app_online);
        }
        if (onlineFragment() instanceof OnLineDeskTopWallpaperFragment) {
            tab.setContentDescription(R.string.theme_wallpaper_online);
            tab.setText(R.string.theme_wallpaper_online);
        }
        if (onlineFragment() instanceof OnLineFontsFragment) {
            tab.setContentDescription(R.string.theme_font_online);
            tab.setText(R.string.theme_font_online);
        }
        if (onlineFragment() instanceof OnLineIconFragment) {
            tab.setContentDescription(R.string.theme_icons_online);
            tab.setText(R.string.theme_icons_online);
        }
        if (onlineFragment() instanceof OnLineLockScreenStyleFragment) {
            tab.setContentDescription(R.string.theme_lockscreen_online);
            tab.setText(R.string.theme_lockscreen_online);
        }
        if (onlineFragment() instanceof OnLineLockScreenWallpaperFragment) {
            tab.setContentDescription(R.string.theme_lockscreen_wallpaper_online);
            tab.setText(R.string.theme_lockscreen_wallpaper_online);
        }
        if (onlineFragment() instanceof OnLineLiveWallpaperFragment) {
            tab.setContentDescription(R.string.live_wallpaper_online);
            tab.setText(R.string.live_wallpaper_online);
        }
        tab.setTabListener(mTabListener);
        getSupportActionBar().addTab(tab);
    }

    public abstract Fragment getLocalFragment();

    public abstract Fragment getOnLineFragment();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (TAB_INDEX_LOCAL == getSupportActionBar().getSelectedTab().getPosition()
                && (localFragment() instanceof DeskTopWallpaperFragment || localFragment() instanceof LocalLockScreenWallpaperFragment)) {
            MenuItem m = menu.add(Menu.NONE, 1, 0, R.string.import_other_app);
            m.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            m.setIcon(R.drawable.ic_menu_import_export);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ((LocalBase) localFragment()).onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VerifyDownloadedImagesThread.start(this);

        ThemeApplication.sThemeStatus.checkDownloadStatus();
    }

    public class ViewPagerAdapter extends PagerAdapter implements OnPageChangeListener {
        private FragmentTransaction mCurTransaction = null;
        private FragmentManager mFragmentManager;

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
            switch (position) {
                case TAB_INDEX_LOCAL:
                    return mLocalFragment;
                case TAB_INDEX_ONLINE:
                    return mOnlineFragment;
            }
            throw new IllegalStateException("No fragment at position " + position);
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
            if (object == mLocalFragment) {
                return TAB_INDEX_LOCAL;
            }
            if (object == mOnlineFragment) {
                return TAB_INDEX_ONLINE;
            }
            return -1;
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            getSupportActionBar().smoothScrollTabIndicator(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            final ActionBar actionBar = getSupportActionBar();
            actionBar.selectTab(actionBar.getTabAt(position));
            if (TAB_INDEX_LOCAL == position) {
                mLocalFragment.setUserVisibleHint(true);
                mOnlineFragment.setUserVisibleHint(false);
                ((LocalBase) mLocalFragment).doResume();
            } else /* if (TAB_INDEX_ONLINE == position) */ {
                mLocalFragment.setUserVisibleHint(false);
                if (!((OnLineBaseFragment) mOnlineFragment).getLoadOnCreate()) {
                    ((OnLineBaseFragment) mOnlineFragment).setLoadOnCreate(true);
                    ((OnLineBaseFragment) mOnlineFragment).getAllData();
                } else {
                    if (mOnlineFragment != null) {
                        mOnlineFragment.setUserVisibleHint(true);
                        ((OnLineBaseFragment) mOnlineFragment).doResume();
                    }
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            getSupportActionBar().setScrollState(state);
        }
    }

    private class PageChangeListener implements OnPageChangeListener {
        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position
         * {@link #onPageSelected(int)} specified.
         */
        private int mNextPosition = -1;

        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
            final ActionBar actionBar = getSupportActionBar();

            actionBar.selectTab(actionBar.getTabAt(position));
            mNextPosition = position;
        }

        public void setCurrentPosition(int position) {
            mCurrentPosition = position;
        }

        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if (mCurrentPosition >= 0) {
                        // sendFragmentVisibilityChange(mCurrentPosition, false);
                    }
                    if (mNextPosition >= 0) {
                        // sendFragmentVisibilityChange(mNextPosition, true);
                    }
                    invalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING:
                case ViewPager.SCROLL_STATE_SETTLING:
                default:
                    break;
            }
        }
    }
}
