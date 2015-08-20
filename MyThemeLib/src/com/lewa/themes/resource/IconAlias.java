
package com.lewa.themes.resource;

import android.os.Build;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

public class IconAlias {

    private static final String TAG = "IconAlias";
    
    private static final String NOKIA_MM_419 = "MM-419";
    
    private static final String NOKIA_X = "Nokia_X";
    
    public static void loadAlias(HashMap<String, String> map, String config) {
        loadDefaultAlias(map);
        loadCustomAlias(map, config);
    }

    public static void loadCustomAlias(HashMap<String, String> map, String file) {
        InputStream in = null;
        try {
            File config = new File(file);
            if (config.exists()) {
                in = new FileInputStream(config);
                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(in, "utf-8");
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.getName().equals("Icon")) {
                        String name = parser.getAttributeValue(null, "name");
                        String alias = parser.getAttributeValue(null, "alias");
                        map.put(name, alias);
                    }
                    eventType = parser.next();
                }
                in.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "load icon alias", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    public static void loadDefaultAlias(HashMap<String, String> map) {
        // activities
        map.put("com.android.gallery3d#com.android.camera.Camera.png", "com.lewa.camera.png");
        map.put("com.android.gallery3d#com.android.camera.CameraLauncher.png","com.lewa.camera.png");
        map.put("com.android.gallery3d.app.Gallery.png", "com.lewa.photo.png");
        map.put("com.android.gallery3d.app.LewaGalleryActivity.png", "com.lewa.photo.png");
        map.put("com.android.settings.bluetooth.RequestPermissionActivity.png", "com.lewa.bluetooth.png");
        map.put("com.lewa.gallery3d#com.android.camera.Camera.png", "com.lewa.camera.png");
        map.put("com.android.camera2#com.android.camera.CameraLauncher.png", "com.lewa.camera.png");
        map.put("com.lewa.gallery3d#com.android.camera.CameraLauncher.png", "com.lewa.camera.png");
        map.put("com.lewa.gallery3d#com.android.gallery3d.app.LewaGalleryActivity.png","com.lewa.photo.png");
        map.put("com.lewa.gallery3d#com.lewa.videoplayer.MovieListActivity.png","com.lewa.video.png");
        map.put("com.lewa.PIM.contacts.activities.ContactsEntryActivity.png","com.lewa.contacts.png");
        map.put("com.lewa.PIM#com.android.contacts.ContactsApplication.png","com.lewa.contacts.png");
        map.put("com.lewa.PIM.contacts.activities.DialtactsActivity.png", "com.lewa.phone.png");
        map.put("com.lewa.PIM#com.android.contacts.activities.DialtactsActivity.png", "com.lewa.phone.png");
        map.put("com.lewa.PIM.contacts.activities.MessageActivity.png", "com.lewa.messages.png");
        map.put("com.lewa.PIM#com.android.contacts.activities.MessageActivity.png", "com.lewa.messages.png");
        map.put("com.lewa.PIM.contacts.ShareContactViaSDCard.png", "com.lewa.sdcard.png");
        map.put("com.lewa.PIM.mms.ui.ComposeMessageActivity.png", "com.lewa.messages.png");
        map.put("com.android.deskclock.DeskClockMainActivity.png", "com.lewa.deskclock.png");
        map.put("com.android.deskclock.AlarmClock.png", "com.lewa.deskclock.png");
        map.put("com.tclmarket.sz.activity.WelcomeActivity.png", "com.lewa.store.png");
        map.put("com.jrdcom.wearable.smartband2.tcl.watch.ui.activities.WelcomeActivity.png", "com.jrdcom.wearable.smartband2.png");
        map.put("com.jrdcom.wearable.smartband2.tcl.watch.png", "com.jrdcom.wearable.smartband2.png");

        // packages
        map.put("android.png", "lewa.png");
        map.put("com.android.browser.png", "com.lewa.browser.png");
        map.put("com.oupeng.browser#com.opera.android.OperaStartActivity.png", "com.lewa.browser.png");
        map.put("com.android.calculator2.png", "com.lewa.calculator.png");
        map.put("com.android.calendar.png", "com.lewa.calendar.png");
        map.put("com.android.calendar.png", "com.lewa.calendar.png");
        map.put("com.android.camera.png", "com.lewa.camera.png");
        map.put("com.android.deskclock.png", "com.lewa.deskclock.png");
        map.put("com.android.email.png", "com.lewa.mail.png");
        map.put("com.android.exchange.png", "com.lewa.mail.png");
        map.put("com.android.facelock.png", "com.lewa.settings.png");
        map.put("com.android.galaxy4.png", "com.lewa.wallpaper.png");
        map.put("com.android.gallery3d.png", "com.lewa.photo.png");
        map.put("com.android.magicsmoke.png", "com.lewa.wallpaper.png");
        map.put("com.android.musicvis.png", "com.lewa.wallpaper.png");
        map.put("com.android.noisefield.png", "com.lewa.wallpaper.png");
        map.put("com.android.phasebeam.png", "com.lewa.wallpaper.png");
        map.put("com.android.phone.png", "com.lewa.phone.png");
        map.put("com.android.providers.calendar.png", "com.lewa.calendar.png");
        map.put("com.android.providers.contacts.png", "com.lewa.contacts.png");
        map.put("com.android.providers.downloads.png", "com.lewa.download.png");
        map.put("com.android.providers.downloads.ui.png", "com.lewa.download.png");
        map.put("com.android.providers.media.png", "com.lewa.music.png");
        map.put("com.android.providers.settings.png", "com.lewa.settings.png");
        map.put("com.android.providers.telephony.png", "com.lewa.phone.png");
        map.put("com.android.settings.png", "com.lewa.settings.png");
        map.put("com.android.soundrecorder.png", "com.lewa.soundrecorder.png");
        map.put("com.android.stk.png", "com.lewa.stk.png");
        map.put("com.android.wallpaper.png", "com.lewa.wallpaper.png");
        map.put("com.android.wallpaper.holospiral.png", "com.lewa.wallpaper.png");
        map.put("com.android.wallpaper.livepicker.png", "com.lewa.wallpaper.png");
        map.put("com.baidu.BaiduMap.png", "com.lewa.maps.png");
        map.put("com.baidu.BaiduMap#com.baidu.baidumaps.WelcomeScreen.png", "com.lewa.maps.png");
        map.put("com.baidu.searchbox.png", "com.lewa.search.png");
        map.put("com.baidu.voiceassistant.png", "com.lewa.voicecommand.png");
        map.put("com.chaozh.iReader.png", "com.lewa.bookstore.png");
        map.put("com.lewa.appstore.png", "com.lewa.store.png");
        map.put("com.lewa.flashlight.png", "com.lewa.torch.png");
        map.put("com.lewa.gallery3d.png", "com.lewa.photo.png");
        map.put("com.lewa.labi.png", "com.lewa.contacts.png");
        map.put("com.lewa.launcher.png", "lewa.png");
        map.put("com.lewa.launcher5.png", "lewa.png");
        map.put("com.lewa.lockscreen.png", "com.lewa.lock.png");
        map.put("com.lewa.lewaguide.png", "com.lewa.guidebook.png");
        map.put("com.lewa.netmgr.adjust.png", "com.lewa.netmgr.png");
        map.put("com.lewa.PIM.png", "com.lewa.contacts.png");
        map.put("com.lewa.player.png", "com.lewa.music.png");
        map.put("com.lewa.musicplayer.png", "com.lewa.music.png");
        map.put("com.lewa.spm.png", "com.lewa.battery.png");
        map.put("com.lewa.themechooser.png", "com.lewa.theme.png");
        map.put("com.lewa.thememanager.png", "com.lewa.theme.png");
        map.put("com.lewa.updater.png", "com.lewa.update.png");
        map.put("com.lewa.providers.downloads.png", "com.lewa.download.png");
        map.put("com.lewa.providers.downloads.ui.png", "com.lewa.download.png");
        map.put("com.mediatek.batterywarning.png", "com.lewa.settings.png");
        map.put("com.mediatek.bluetooth.png", "com.lewa.bluetooth.png");
        map.put("com.mediatek.FMRadio.png", "com.lewa.radio.png");
        map.put("com.caf.fmradio.FMAdapterApp.png", "com.lewa.radio.png");
        map.put("com.caf.fmradio.LewaFMRadio.png", "com.lewa.radio.png");
        map.put("com.mediatek.schpwronoff.png", "com.lewa.settings.png");
        map.put("com.mediatek.StkSelection.png", "com.lewa.stk.png");
        map.put("com.mediatek.videoplayer.png", "com.lewa.video.png");
        map.put("com.mediatek.voicecommand.png", "com.lewa.settings.png");
        map.put("com.mediatek.voiceunlock.png", "com.lewa.settings.png");
        map.put("com.mediatek.datatransfer.png", "com.lewa.datatransfer.png");
        map.put("com.when.android.calendar365.png", "com.lewa.calendar.png");
        map.put("org.dayup.gnotes.lewa.png", "com.lewa.note.png");
        map.put("tv.huohua.android.ocher.png", "com.lewa.movie.png");
        map.put("com.quicinc.fmradio.png", "com.lewa.radio.png");
        map.put("com.quicinc.fmradio.FMRadio.png", "com.lewa.radio.png");
        map.put("com.android.bluetooth.png", "com.lewa.bluetooth.png");
        map.put("com.oppo.camera.CameraLauncher.png", "com.lewa.camera.png");
        map.put("com.oppo.gallery3d.png", "com.lewa.photo.png");
        // for aosp coolpad s5890
        map.put("com.yulong.android.cp_utk.png", "com.lewa.stk.png");
        map.put("com_yulong_android_cp_utk.png", "com_mediatek_stkselection.png");
        //KK
        map.put("com.android.dialer.png", "com.lewa.phone.png");
        map.put("com.android.contacts.png", "com.lewa.contacts.png");
        map.put("com.android.dialer.DialtactsActivity.png", "com.lewa.phone.png");
        map.put("com.android.server.telecom.TelecomApp.png", "com.lewa.phone.png");
        map.put("com.android.contacts.activities.ContactsEntryActivity.png", "com.lewa.contacts.png");
        map.put("com.lewa.PIM#com.android.contacts.activities.ContactsEntryActivity.png", "com.lewa.contacts.png");
        map.put("com.android.contacts.activities.MessageActivity.png", "com.lewa.messages.png");
        map.put("com.android.mms.png","com.lewa.messages.png");
        
        map.put("com_android_contacts_contactsapplication.png", "com_lewa_pim_contacts_activities_contactsentryactivity.png");
        map.put("com_android_contacts_activities_dialtactsactivity.png", "com_lewa_pim_contacts_activities_dialtactsactivity.png");
        map.put("com_android_contacts_activities_messageactivity.png", "com_lewa_pim_contacts_activities_messageactivity.png");
        // v4 alias
        map.put("com_android_deskclock_deskclockmainactivity.png", "com_android_deskclock.png");
        map.put("com_android_deskclock_alarmClock.png", "com_android_deskclock.png");
        map.put("com_lewa_pim_contacts_contactsapplication.png", "com_lewa_pim_contacts_activities_contactsentryactivity.png");
        map.put("com_android_gallery3d_app_galleryappimpl.png", "com_android_galleryx.png");
        map.put("com_android_camera_cameralauncher.png", "com_android_camera.png");
        map.put("com_android_camera_camera.png", "com_android_camera.png");
        map.put("com_android_gallery3d_app_lewagalleryactivity.png", "com_android_galleryx.png");
        map.put("com_android_gallery3d_app_gallery.png", "com_android_galleryx.png");
        map.put("com_lewa_gallery3d_app_lewagalleryactivity.png", "com_android_galleryx.png");
        map.put("com_baidu_baidumap.png", "com_android_mapx.png");
        map.put("com_baidu_voiceassistant.png", "com_lewa_voicecommand.png");
        map.put("com_chaozh_ireader.png", "com_lewa_books.png");
        map.put("com_lewa_appstore.png", "com_lewa_store.png");
        map.put("com_baidu_searchbox.png", "com_lewa_search.png");
        
        map.put("com_mediatek_fmradio.png", "com_lewa_fmradio.png");
        map.put("com_quicinc_fmradio.png", "com_lewa_fmradio.png");
        map.put("com_quicinc_fmradio_fmradio.png", "com_lewa_fmradio.png");
        map.put("com_mediatek_videoplayer.png", "com_android_video.png");
        map.put("com_socogame_ppc.png", "com_android_gamex.png");
        map.put("com_when_android_calendar365.png", "com_android_calendarx.png");
        map.put("com_android_calendar.png", "com_android_calendarx.png");
        map.put("org_dayup_gnotes_lewa.png", "com_android_notesx.png");
        map.put("tv_huohua_android_ocher.png", "com_lewa_movie.png");
        map.put("icon_folder_background.png", "com_android_folder.png");
        map.put("com_lewa_musicplayer.png", "com_lewa_player.png");
        map.put("com_lewa_providers_downloads_ui.png", "com_android_providers_downloads_ui.png");
        map.put("com.android.music.png", "com.lewa.music.png");
        map.put("com_android_music.png", "com_lewa_player.png");
        map.put("com.android.quicksearchbox.png", "com.lewa.search.png");
        map.put("com.android.camera2.png", "com.lewa.camera.png");
        Build bd = new Build();
        // Nokia MM-419
        if (NOKIA_MM_419.equals(bd.MODEL) || NOKIA_X.equals(bd.MODEL)) {
            map.put("com.android.mms.png", "com.lewa.messages.png");
            map.put("com.android.contacts.activities.DialtactsActivity.png",
                    "com.lewa.phone.png");
            map.put("com.nokia.xpress.png", "com.lewa.browser.png");
            map.put("com.android.music.png", "com.lewa.music.png");
            map.put("com.android.quicksearchbox.png", "com.lewa.search.png");
            map.put("com.here.app.maps.png", "com.lewa.maps.png");
            map.put("com.uc.nokiaappstore.png", "com.lewa.store.png");
            map.put("com.lewa.themechooser.ThemeChooser.png",
                    "com.lewa.theme.png");
            map.put("com.lewa.launcher.n#com.lewa.themechooser.ThemeChooser.png",
                    "com.lewa.theme.png");
            map.put("com.lewa.launcher.n#com.lewa.weather.LewaWeather.png",
                    "com.lewa.weather.png");

            // V4 alias
            map.put("com_android_contacts.png",
                    "com_lewa_pim_contacts_activities_contactsentryactivity.png");
            map.put("com_android_mms.png",
                    "com_lewa_pim_contacts_activities_messageactivity.png");
            map.put("com_android_contacts_activities_dialtactsactivity.png",
                    "com_lewa_pim_contacts_activities_dialtactsactivity.png");
            map.put("com_nokia_xpress.png", "com_android_browser.png");
            map.put("com_android_music.png", "com_lewa_player.png");
            map.put("com_android_quicksearchbox.png", "com_lewa_search.png");
            map.put("com_here_app_maps.png", "com_android_mapx.png");
            map.put("com_uc_nokiaappstore.png", "com_lewa_store.png");
            map.put("com_lewa_themechooser_themechooser.png",
                    "com_lewa_themechooser.png");
            map.put("com_quicinc_fmradio.png", "com_lewa_fmradio.png");
        }
        // nokia MM-419 end
        // TCL bug 941758
        map.put("com.qualcomm.qti.networksetting.png", "lewa.png");
    }
}
