package com.lewa.themechooser.appwidget.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.appwidget.CoveringPageActivity;
import com.lewa.themechooser.appwidget.util.CommonUtils;
import com.lewa.themechooser.newmechanism.ApplyThemeHelp;
import com.lewa.themes.CustomType;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;
import com.lewa.themes.provider.Themes;
import com.lewa.themes.provider.Themes.ThemeColumns;

import util.ThemeUtil;

public class FontChangeService extends Service {
    private static Context mContext;
    private static Toast mToast;
    private BroadcastReceiver mThemeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d("FontChangeService", "mThemeChangedReceiver");
            if (ThemeManager.ACTION_THEME_CHANGED.equals(intent.getAction())
                    && ThemeUtil.isKillProcess && CommonUtils.isCoveringPageOn(context)) {
                context.sendBroadcast(new Intent(CoveringPageActivity.ACTION_FINISH_COERINGPAGE));
                /*Intent i = new Intent("android.intent.action.killProcess");
                mContext.sendBroadcast(i);*/
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mToast.cancel();
                }
            }, 1500);
        }
    };

    private static void setView(int resId) {
        View view = View.inflate(mContext, R.layout.onekey_font__toast, null);
        TextView tvToast = (TextView) view.findViewById(R.id.tv_onekey_font_toast);
        tvToast.setText(mContext.getResources().getString(resId));
        mToast.setView(view);
    }

    @Override
    public void onCreate() {
        initParams();
        super.onCreate();
    }

    private void initParams() {
        mContext = getApplicationContext();
        registerReceiver();
        initToast();
    }

    private void initToast() {
        mToast = new Toast(mContext);
        mToast.setDuration(10000);
        mToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0,
                CommonUtils.dip2px(mContext, 90));
        setView(R.string.onekey_font_changing);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mThemeChangedReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FontController.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter(ThemeManager.ACTION_THEME_CHANGED);
        try {
            intentFilter.addAction(ThemeManager.ACTION_KILL_PROCESS_FINISH);
            intentFilter.addDataType(ThemeColumns.CONTENT_ITEM_TYPE);
            intentFilter.addDataType(ThemeColumns.STYLE_CONTENT_ITEM_TYPE);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException(e);
        }
        registerReceiver(mThemeChangedReceiver, intentFilter);
    }

    private static class FontController extends AsyncTask<Void, String, Void> {
        private static FontController mFontController;
        private static int lastFontIndex = -1;

        public static void start() {
            if (mFontController != null && mFontController.getStatus() != AsyncTask.Status.FINISHED) {
                return;
            }
            mFontController = (FontController) new FontController()
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {
                ThemeItem fontItem = getFontItem(mContext);
                if (fontItem != null) {
                    publishProgress("changing");
                    changeFont(fontItem);
                } else {
                    publishProgress("errorChange");
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if ("changing".equals(values[0])) {
                mToast.show();
            } else if ("errorChange".equals(values[0])) {
                mToast.setDuration(3000);
                setView(R.string.onekey_font_no_font);
                mToast.show();
                mToast.setDuration(10000);
                setView(R.string.onekey_font_changing);
                cancel(true);
            }
        }

        private void changeFont(ThemeItem fontItem) {
            startCoveringPageActivity();

            ThemeItem appliedTheme = Themes.getAppliedTheme(mContext);
            if (null == appliedTheme) {
                return;
            }
            Uri uri = Themes.getThemeUri(mContext, appliedTheme.getPackageName(),
                    appliedTheme.getThemeId());
            appliedTheme.close();
            Intent i = new Intent(ThemeManager.ACTION_CHANGE_THEME, uri);
            i.putExtra(ThemeManager.EXTRA_EXTENDED_THEME_CHANGE, true);
            i.putExtra(ThemeManager.EXTRA_FONT_URI, fontItem.getFontUril());
            if (fontItem.getPackageName().equals("com.lewa.theme.LewaDefaultTheme")
                    && fontItem.getThemeId().equals("LewaDefaultTheme")) {
                i.putExtra(ThemeManager.DEFAULT_FONT, true);
            }
            i.putExtra(CustomType.EXTRA_NAME, CustomType.FONT_TYPE);
            ThemeUtil.isKillProcess = true;
            ApplyThemeHelp.changeTheme(mContext, i);
            ThemeApplication.sThemeStatus.setAppliedPkgName(fontItem.getPackageName(),
                    com.lewa.themechooser.ThemeStatus.THEME_TYPE_FONT);
            cancel(true);
        }

        private ThemeItem getFontItem(Context context) {
            // ThemeColumns.THEME_PACKAGE + "='com.lewa.theme.LewaDefaultTheme' or " +
            Cursor cursor = context.getContentResolver().query(
                    ThemeColumns.CONTENT_PLURAL_URI,
                    null,
                    ThemeColumns.FONT_URI + " is not null", null,
                    ThemeColumns.IS_SYSTEM + " desc, " + ThemeColumns._ID + " desc");

            if (!cursor.moveToNext()) {
                return null;
            }
            ThemeItem daoItem = new ThemeItem(cursor);
            int fontsCount = daoItem.getCount();
            if (fontsCount <= 1) {
                return null;
            }
            daoItem.setPosition(getRandomFontIndex(fontsCount));
            return daoItem;
        }

        private int getRandomFontIndex(int fontsCount) {
            int randomIndex = (int) (Math.random() * fontsCount);
            if (randomIndex == lastFontIndex) {
                return getRandomFontIndex(fontsCount);
            }
            lastFontIndex = randomIndex;
            return randomIndex;
        }

        private void startCoveringPageActivity() {
            Intent activity = new Intent(mContext, CoveringPageActivity.class);
            activity.putExtra("flag", "FontChange");
            activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(activity);
        }
    }
}
