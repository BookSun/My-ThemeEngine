package com.lewa.themechooser.receiver;

import android.app.DownloadManager;
import android.content.*;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Downloads;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.lewa.themechooser.R;
import com.lewa.themechooser.ThemeApplication;
import com.lewa.themechooser.ThemeConstants;
import com.lewa.themechooser.ThemeStatus;
import com.lewa.themes.provider.Themes.ThemeColumns;

import java.net.HttpURLConnection;
import java.net.URL;

//import lewa.bi.BIAgent;
import util.ThemeUtil;

public class DownloadCompleteReceiver extends BroadcastReceiver {
    private Context mContext;
    private String themeid;
    private long id;
    private ContextThemeWrapper mThemeContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Cursor c = context.getContentResolver().query(Downloads.Impl.CONTENT_URI
                    , new String[]{Downloads.Impl.COLUMN_FILE_NAME_HINT
                    , Downloads.Impl.COLUMN_STATUS, Downloads.Impl.COLUMN_TITLE}
                    , Downloads.Impl._ID + "=?", new String[]{String.valueOf(id)}, null);

            if (null == c) {
                return;
            }

            String installThemeName = "";
            String title = "";
            int state = DownloadManager.STATUS_FAILED;
            if (c.moveToFirst()) {
                installThemeName = c.getString(0);
                int pos = installThemeName.lastIndexOf('/');
                if (pos >= 0) {
                    installThemeName = installThemeName.substring(pos + 1, installThemeName.length());
                    try {
                        installThemeName = java.net.URLDecoder.decode(installThemeName, "UTF-8");
                    } catch (Exception e) {
                    }
                }
                state = c.getInt(1);
                title = c.getString(2);
            }
            c.close();

            ThemeStatus status = ThemeApplication.sThemeStatus;
            if (android.text.TextUtils.isEmpty(installThemeName)
                    || 200 != state) {
                // Do not show completed toast if title is empty
                status.setDownloadingCancelled(id);

                context.getContentResolver().delete(ThemeColumns.CONTENT_PLURAL_URI
                        , ThemeColumns.IS_IMAGE_FILE + "=?", new String[]{String.valueOf(id)});

                return;
            }

            ContentValues values = new ContentValues();
            values.put(ThemeColumns.IS_IMAGE_FILE, -1);
            int d = context.getContentResolver().update(ThemeColumns.CONTENT_PLURAL_URI, values
                    , ThemeColumns.IS_IMAGE_FILE + "=?", new String[]{String.valueOf(id)});

            Intent i = new Intent(context, ThemeInstallService.class);
            i.putExtra("THEME_PACKAGE", new StringBuilder().append(
                    ThemeConstants.THEME_LWT).append("/").append(installThemeName).toString());
            i.putExtra("NOTIFY", true);
            context.startService((i));

            status.setDownloaded(id);

            android.content.SharedPreferences sp
                    = context.getSharedPreferences("CUSTOM_URI", Context.MODE_PRIVATE);
            themeid = sp.getString(String.valueOf(id), null);
            String pkgName = sp.getString("PkgName" + String.valueOf(id), null);
            String mTypeName = sp.getString("TypeName" + String.valueOf(id), null);
            //BIAgent.onEvent(context, "theme_download", pkgName);
            new SendDownLoadFinish().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            if (mThemeContext == null)
                mThemeContext = new ContextThemeWrapper(mContext, R.style.Theme);
            Toast.makeText(mThemeContext, (mTypeName == null ? title : mTypeName) + " "
                    +mContext.getString(R.string.download_success), Toast.LENGTH_SHORT).show();
        }
    }

    private class SendDownLoadFinish extends AsyncTask {

        @Override
        protected Object doInBackground(Object... params) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://admin.lewatek.com/themeapi2/themepackage_download/" + themeid);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("User-Agent", ThemeUtil.userAgent);
                conn.connect();
                if (conn.getResponseCode() == 200) {
                    conn.disconnect();
                    conn = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                conn.disconnect();
                conn = null;
            } finally {
                android.content.SharedPreferences.Editor sp = mContext.getSharedPreferences("CUSTOM_URI", Context.MODE_PRIVATE).edit();
                sp.remove(String.valueOf(id));
                sp.commit();
            }
            return null;
        }

    }
}
