package com.lewa.themechooser;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.CustomTheme;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.Themes.ThemeColumns;

import java.io.File;
import java.util.List;

import util.L;
import util.ThemeUtil;

import static com.lewa.themes.ThemeManager.STANDALONE;

/**
 * Utility class to centralize common logic found in the profile chooser, theme
 * chooser, and style chooser. This logic is designed to "seize" the user and
 * hold them onto the chooser screen until the change event has finished
 * processing.
 * <p/>
 * To use, a new instance must be created with the activity and the various
 * on<Event> methods must be connected into the activity lifecycle.
 */
public class ChangeThemeHelper {
    /**
     * Set when the "Select" button is clicked, used by the 'setting theme'
     * dialog to show the applying theme. This value is used only in that
     * situation and is not preserved across config changes.
     */
    private static String mApplyingName;
    private final Activity mContext;
    private final int mDialogId;
    /**
     * Used to impose a short delay between theme change "completion" and the
     * actual finish() call to work around imprecisions inherent to detecing
     * theme change completion.
     */
    private final ChangeHandler mHandler;
    private final BroadcastReceiver mThemeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            ThemeApplication.sThemeStatus.persistPreferences();
            boolean reboot = mContext.getResources().getBoolean(R.bool.config_font_reboot);

            if (ThemeManager.ACTION_THEME_CHANGED.equals(intent.getAction())) {
                if (ThemeUtil.isChangeFont && !reboot) {
                    Intent i = new Intent("android.intent.action.killProcess");
                    i.putExtra("wallpaperUri", intent.getParcelableExtra("wallpaperUri"));
                    mContext.sendBroadcast(i);
                    return;
                }
            }

            (new Thread() {
                public void run() {
                    if (!STANDALONE) {
                        // Kill the current Home process, they tend to be evil and cache drawable references in all apps
                        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        //Lewa modify begin 
                        //forbidden stop com.lewa.PIM due to sms notification disappeared after theme change
                        //am.forceStopPackage("com.lewa.PIM");
                        //Lewa modify end
                        ThemeUtil.updateThemeInfo(context, !ThemeUtil.isKillProcess ||
                                "com.lewa.theme.LewaDefaultTheme"
                                        .equals(ThemeApplication.sThemeStatus
                                                .getAppliedPkgName(ThemeStatus.THEME_TYPE_STYLE)),
                                ThemeManager.ACTION_THEME_CHANGED.equals(intent.getAction()) &&
                                        ThemeUtil.isKillProcess, false);
                    }
                    mHandler.scheduleFinish("Theme change 'complete', closing!");
                }
            }).start();
        }
    };

    /**
     * Tracked to trap theme change configuration events. Note that this could
     * be null if the current theme is operating off the "booted" theme (that
     * is, the Configuration object we got has an uninitialized customTheme
     * member).
     */
    private CustomTheme mCurrentTheme;
    private String mApplyThemeName;

    public ChangeThemeHelper(Activity context, int dialogId) {
        mContext = context;
        mDialogId = dialogId;
        mHandler = new ChangeHandler(this);
    }

    private static void clearIconCache(Context context) {
        File dirFile = new File(getIconCachePath(context));
        if (dirFile.exists() && dirFile.isDirectory()) {
            File[] files = dirFile.listFiles();
            for (File file : files) {
                if (file.isFile() && file.exists()) {
                    file.delete();
                }
            }
        }
    }

    private static String getIconCachePath(Context context) {
        return Environment.getDataDirectory().getAbsolutePath() + "/data/" + context.getPackageName() + "/files/customized_icons";
    }

    /**
     * Return the resources compiled with the ThemeChooser package, so that
     * ChangeThemeHelper may be compiled into other packages without forcing
     * the other packages to duplicate ThemeChooser resources.
     *
     * @param context - The context of the application using ChangeThemeHelper
     * @return - Handle to ThemeChooser resources
     */
    private static Resources getThemeChooserResources(Context context) {
        Resources res;
        String resourcePackageName = ThemeManager.THEME_ELEMENTS_PACKAGE;
        String callerPackageName = context.getPackageName();
        if (callerPackageName.equals(resourcePackageName)) {
            res = context.getResources();
        } else {
            PackageManager pm = context.getPackageManager();
            try {
                res = pm.getResourcesForApplication(resourcePackageName);
            } catch (NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return res;
    }

    public void dispatchOnCreate() {
//        if (!STANDALONE) {
//            mCurrentTheme = mContext.getResources().getConfiguration().customTheme;
//        }
    }

    public boolean dispatchOnConfigurationChanged(Configuration newConfig) {
        /**
         * It is necessary to detect theme changes in this way (as well as via
         * the broadcast) in order to handle the case where the user leaves the
         * activity with the Home button in the middle of a theme change. When
         * the theme change event is received by this activity (when it is
         * brought back to the foreground), we need to finish automatically
         * rather than present a UI with a potentially stale theme applied.
         *
         * @param newConfig
         * @return boolean finishing - true if finish() is scheduled
         */
        boolean finishing = false;
        //Delete for standalone by Fan.Yang
        CustomTheme newTheme  = null;//newConfig.getCustomTheme();
        if (STANDALONE || newTheme != null &&
                (mCurrentTheme == null || !mCurrentTheme.equals(newTheme))) {
            mHandler.scheduleFinish("Theme config change, closing!");
            finishing = true;
        }
        return finishing;
    }

    public void dispatchOnPause() {
        /*
         * If the user leaves this screen, just remove the progress dialog and
         * give them the "raw" experience (the device will be slow for a short
         * while)
         */
        mContext.removeDialog(mDialogId);
        mContext.unregisterReceiver(mThemeChangedReceiver);
    }

    public void dispatchOnResume() {
        /**
         * Register a receiver that will dismiss the dialog and finish this
         * activity when it is believed that theme change is complete (this is
         * an estimate, since theme change is never truly "complete", as it
         * doesn't update every running activity immediately).
         * <p>
         * If theme change occurs (from some other component) while we're
         * looking at this screen it will automatically finish(). Might seem
         * weird to the user, but it is a rare corner case and would be
         * difficult to handle correctly.
         */
        IntentFilter filter = new IntentFilter(ThemeManager.ACTION_THEME_CHANGED);
        try {
            filter.addAction(ThemeManager.ACTION_KILL_PROCESS_FINISH);
            filter.addDataType(ThemeColumns.CONTENT_ITEM_TYPE);
            filter.addDataType(ThemeColumns.STYLE_CONTENT_ITEM_TYPE);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException(e);
        }
        mContext.registerReceiver(mThemeChangedReceiver, filter);
    }

    private void handleThemeChangeSwitch(String message) {
        if (ThemeConstants.DEBUG) {
            Log.i(ThemeConstants.TAG, message);
        }
        boolean isChangeFont = ThemeUtil.isChangeFont;
        ThemeUtil.isChangeFont = false;
        boolean reboot = mContext.getResources().getBoolean(R.bool.config_font_reboot);
        if (reboot && isChangeFont) {
            if (ThemeUtil.reboot(mContext))
                return;
        }
        /*
         * Will dismiss if present, but doesn't require that it is currently
         * being shown. This is important because the user might have left
         * the screen while the dialog was up, or a third party might have
         * actually sent this broadcast (in response to a different event,
         * like maybe automatic profile switching).
         */
        if ("Theme change 'complete', closing!".equals(message)) {
            Toast.makeText(mContext.getApplicationContext(),
                    mContext.getString(R.string.theme_change_dialog_title_success),
                    Toast.LENGTH_SHORT).show();
            mContext.removeDialog(mDialogId);
            mContext.finish();
            startHome();
        }
        ThemeUtil.isUsingChanged = true;
        // TCL938581 delete by fan.yang, unnecessary code
        /*if (ThemeUtil.isKillProcess) {
            startHome();
            if (!ThemeManager.STANDALONE) {
                final ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                am.forceStopPackage("com.lewa.themechooser");
            }
        }
        if (ThemeManager.STANDALONE) {
            clearIconCache(mContext);
            android.os.Process.killProcess(android.os.Process.myPid());
        }*/
    }

    private void startHome() {
        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(mHomeIntent);
    }

    public Dialog dispatchOnCreateDialog(int id) {
        if (id == mDialogId) {
            ProgressDialog dialog = new ProgressDialog(mContext);
            dialog.setTitle(getThemeChooserResources(mContext).getString(
                    R.string.theme_change_dialog_title));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        } else {
            return null;
        }
    }

    public void dispatchOnPrepareDialog(int id, Dialog dialog) {
        if (id == mDialogId) {
            ((ProgressDialog) dialog).setMessage(getThemeChooserResources(
                    mContext).getString(R.string.switching_to_theme,
                    mApplyingName));
        }
    }

    public void beginChange(String applyingName) {
        mApplyingName = applyingName;
        mContext.showDialog(mDialogId);

        /*
         * If no theme change events are seen before the timeout occurs, dismiss
         * the dialog anyway. This is to hide some of the clumsier corners of
         * this implementation from the user. For instance, if a profile
         * specifies a theme that no longer exists, the change theme receiver
         * will return without sending a follow-up broadcast event that the
         * theme has been changed. Eventually this timeout will be reached and
         * we'll tidy up anyway, warning appropriately.
         */
        mHandler.scheduleTimeout();
    }

    private static class ChangeHandler extends Handler {
        private static final int MSG_FINISH_SCHEDULE = 0;
        private static final int MSG_FINISH_EXECUTE = 1;

        private static final int SCHEDULE_DELAY = 500;
        private static final int FINISH_DELAY = 500;
        private static final int TIMEOUT_DELAY = 30000;

        private ChangeThemeHelper mContext;

        ChangeHandler(ChangeThemeHelper context) {
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                /*
                 * MSG_FINISH_SCHEDULE is no longer strictly needed in the open
                 * source tree, but here for legacy purposes.
                 */
                case MSG_FINISH_SCHEDULE:
                    final String message = (String) msg.obj;
                    removeMessages(MSG_FINISH_EXECUTE);
                    sendMessageDelayed(obtainMessage(MSG_FINISH_EXECUTE, message),
                            FINISH_DELAY);
                    break;
                case MSG_FINISH_EXECUTE:
                    mContext.handleThemeChangeSwitch((String) msg.obj);
                    break;
            }
        }

        /**
         * Schedule the finish event to occur after all receivers have finished
         * executing. This is a way to try to better time the stable state of
         * the Profile Manager screen (once wallpapers, ringtones, etc have all
         * been committed to the database). A minimum delay of
         * {@link #SCHEDULE_DELAY} is imposed before this is even attempted in
         * case something has gone horribly wrong with the receiver queue (such
         * as it being full and a new thread must be created for our task).
         * <p/>
         * In addition the scheduling delay, {@link #FINISH_DELAY} is applied
         * before executing the finish() call.
         */
        public void scheduleFinish(String message) {
            removeMessages(MSG_FINISH_SCHEDULE);
            removeMessages(MSG_FINISH_EXECUTE);
            sendMessageDelayed(obtainMessage(MSG_FINISH_SCHEDULE, message), SCHEDULE_DELAY);
        }

        /**
         * Schedule a timeout that will invoke {@link #MSG_FINISH_EXECUTE} after
         * {@link #TIMEOUT_DELAY} inactivity. This is a catch-all to gracefully
         * handle certain unlikely error cases.
         */
        public void scheduleTimeout() {
            if (!hasMessages(MSG_FINISH_SCHEDULE) && !hasMessages(MSG_FINISH_EXECUTE)) {
                sendMessageDelayed(obtainMessage(MSG_FINISH_EXECUTE,
                        "Timed out waiting for theme change event."), TIMEOUT_DELAY);
            }
        }
    }
}
