package com.lewa.support.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

import lewa.os.Build;

public final class ExtraSettings {
    /**
     * Intent actions for Settings
     *
     * @hide
     */
    public static final String SETTINGS_CHANGED = "android.settings.SETTINGS_CHANGED_ACTION";

    /// LEWA BEGIN
    public static final String LAST_ASKBTPERM_TIME = "last_askbtperm_time";
    /// LEWA END

    public static final class System {

        public static final int DISPLAY_SMART_AVATAR_DISABLE = 0;
        public static final int DISPLAY_SMART_AVATAR_ENABLE = 1;
        public static final String DISPLAY_SMART_AVATAR_SETTING = "display_smart_avatar";

        public static final String RINGTONE_2 = "ringtone_2";
        public static final Uri DEFAULT_RINGTONE_URI_2 = Settings.System.getUriFor(RINGTONE_2);
       /**
         * SMS default sim<br/>
         * <b>Values: sim ID</b><br/>
         * @hide
         */
        public static final String SMS_SIM_SETTING = "sms_sim_setting";

        /*
         * @hide
         *  added by zhangyawei for sim setting guide
         */
        public static final String AVALIABLE_CARD_COUNTS = "available_sim_card_counts";

        /**
         * Dual SIM mode setting.<br/>
         * <b>Values:</b><br/>
         * 1 - SIM1 only mode.<br/>
         * 2 - SIM2 only mode.<br/>
         * 3 - Dual SIM mode.<br/>
         * 4 - Flight mode.<br/>
         * @hide
         */
        public static final String DUAL_SIM_MODE_SETTING = "dual_sim_mode_setting";

        /**
         * Dual SIM mode setting default value
         * @hide
         */
        public static final int DUAL_SIM_MODE_SETTING_DEFAULT = 3;

        /**
         * Roaming reminder mode<br/>
         * <b>Values: sim ID</b><br/>
         * 0 - once.<br/>
         * 1 - Always ask.<br/>
         * 2 - Never.<br/>
         * @hide
         */
        public static final String ROAMING_REMINDER_MODE_SETTING = "roaming_reminder_mode_setting";

        /**
         * flag to indicate whether need to popup the roaming indication<br/>
         * <b>Values: </b><br/>
         * true - in roaming service and hasnt popup roaming indication.<br/>
         * false - not in roaming service or some other apps has popup roaming indication.<br/>
         * @hide
         */
        public static final String ROAMING_INDICATION_NEEDED = "roaming_indication_needed";

        /**
         * Record sim lock state
         * @hide
         */
        public static final String SIM_LOCK_STATE_SETTING = "sim_lock_state_setting";

        /**
         * SoftAp Netmask,for some wifi Boardcast
         * @hide
         */
        public static final String WIFI_SOFTAP_NETMASK = "wifi_softap_netmask";

        /**
         * voice call default sim<br/>
         * <b>Values: sim ID</b><br/>
         * @hide
         */
        public static final String VOICE_CALL_SIM_SETTING = "voice_call_sim_setting";

        /**
         * Voice call setting as Internet call
         * @hide
         */
        public static final long VOICE_CALL_SIM_SETTING_INTERNET = -2;

        /**
         * GPRS connection mode<br/>
         * <b>Values:</b><br/>
         * 0 - Not allow GPRS connection.<br/>
         * 1 - Prefer SIM1.<br/>
         * 2 - Prefer SIM2.<br/>
         * @hide
         */
        public static final String GPRS_CONNECTION_SETTING = "gprs_connection_setting";

        /**
         * GPRS connection mode default value.
         * @hide
         */

        public static final int GPRS_CONNECTION_SETTING_DEFAULT = 1;//FeatureOption.MTK_DEFAULT_DATA_OFF ? 0 : 1;

        /**
         * GPRS connection setting as never
         * @hide
         */
        public static final long GPRS_CONNECTION_SIM_SETTING_NEVER = 0;

        /**
         * Voice call and sms setting as always ask
         * @hide
         */
        public static final long DEFAULT_SIM_SETTING_ALWAYS_ASK = -1;

        /**
         * Internet call state<br/>
         * <b>Values:</b><br/>
         * 0 - Internet call is disabled.<br/>
         * 1 - Internet call is enabled.<br/>
         * @hide
         */
        public static final String ENABLE_INTERNET_CALL =
                "enable_internet_call_value";

        /**
         * GPRS connection default sim<br/>
         * <b>Values: sim ID</b><br/>
         * @hide
         */
        public static final String GPRS_CONNECTION_SIM_SETTING = "gprs_connection_sim_setting";

        /**
         * Default SIM not set
         * @hide
         */
        public static final long DEFAULT_SIM_NOT_SET = -5;

        /**
         * Default SIM not set of int
         * @hide
         */
        public static final int DEFAULT_SIM_NOT_SET_INT = -5;

        /**
         * dual sim switch setting<br/>
         *
         * @hide
         */
        public static final String DUAL_SIM_SWITCH_SETTING = "dual_sim_switch_setting";

        /**
         * DUAL SIM switch setting set
         *
         * @hide
         */

        public static final long DUAL_SIM_SWITCH_SET = 0;
        public static final long DUAL_SIM_SWITCH_ACTION_SET = 1;

        /**
         * fast mode switch setting<br/>
         *
         * @hide
         */
        public static final String FAST_MODE_SWITCH_SETTING = "fast_mode_switch_setting";
        public static final String FIRST_BOOT_FAST_MODE_SWITCH_SETTING = "first_boot_fast_mode_switch_setting";
        public static final String FIRST_BOOT_AFTER_DOUBLE_CLEAR = "first_boot_after_double_clear";

        /**
         * fast mode switch setting set
         *
         * @hide
         */
        public static final long FAST_MODE_DISABLE = 0;
        public static final long FAST_MODE_ENABLE = 1;
        public static final long FAST_MODE_DEFAULT =
                lewa.os.Build.LEWA_DIAL_FASTMODE_SUPPORT ? FAST_MODE_ENABLE : FAST_MODE_DISABLE; // lewa modify

        public static final long DOUBLE_CLEAR_NO = 0;
        public static final long DOUBLE_CLEAR_YES = 1;
        public static final long DOUBLE_CLEAR_DEFAULT = DOUBLE_CLEAR_YES;


        /**
         * Whether PowerManager Mode is on.
         */
        public static final String POWERMANAGER_MODE_ON = "powermanager_mode_on";
        /**
         * Whether battery low warnning val.
         */
        public static final String POWE_LOW_VAL = "powerlowwarningval";

        /**
         *  battery charging time estimation values.
         */
        public static final String POWER_CHARGING_VAL_AC = "powerchargingval_ac";
        public static final String POWER_CHARGING_VAL_USB = "powerchargingval_usb";
        public static final String POWER_CHARGING_INIT = "powerchargingval_inited";

        /**
         * Indicates that custom light sensor settings has changed.
         * The value is random and changes reloads light settings.
         *
         * @hide
         */
        public static final String LIGHTS_CHANGED = "lights_changed";

        /**
         * Whether custom light sensor levels & values are enabled. The value is
         * boolean (1 or 0).
         *
         * @hide
         */
        public static final String LIGHT_SENSOR_CUSTOM = "light_sensor_custom";

        /**
         * Screen dim value to use if LIGHT_SENSOR_CUSTOM is set. The value is int.
         * Default is android.os.BRIGHTNESS_DIM.
         *
         * @hide
         */
        public static final String LIGHT_SCREEN_DIM = "light_screen_dim";

        /**
         * Custom light sensor levels. The value is a comma separated int array
         * with length N.
         * Example: "100,300,3000".
         *
         * @hide
         */
        public static final String LIGHT_SENSOR_LEVELS = "light_sensor_levels";

        /**
         * Custom light sensor lcd values. The value is a comma separated int array
         * with length N+1.
         * Example: "10,50,100,255".
         *
         * @hide
         */
        public static final String LIGHT_SENSOR_LCD_VALUES = "light_sensor_lcd_values";

        /**
         * Custom light sensor lcd values. The value is a comma separated int array
         * with length N+1.
         * Example: "10,50,100,255".
         *
         * @hide
         */
        public static final String LIGHT_SENSOR_BUTTON_VALUES = "light_sensor_button_values";

        /**
         * Custom light sensor lcd values. The value is a comma separated int array
         * with length N+1.
         * Example: "10,50,100,255".
         *
         * @hide
         */
        public static final String LIGHT_SENSOR_KEYBOARD_VALUES = "light_sensor_keyboard_values";

        /**
         * Whether light sensor is allowed to decrease when calculating automatic
         * backlight. The value is boolean (1 or 0).
         *
         * @hide
         */
        public static final String LIGHT_DECREASE = "light_decrease";

        /**
         * Light sensor hysteresis for decreasing backlight. The value is
         * int (0-99) representing % (0-0.99 as float). Example:
         *
         * Levels     Output
         * 0 - 100    50
         * 100 - 200  100
         * 200 - Inf  255
         *
         * Current sensor value is 150 which gives light value 100. Hysteresis is 50.
         * Current level lower bound is 100 and previous lower bound is 0.
         * Sensor value must drop below 100-(100-0)*(50/100)=50 for output to become 50
         * (corresponding to the 0 - 100 level).
         * @hide
         */
        public static final String LIGHT_HYSTERESIS = "light_hysteresis";

        /**
         * Whether light sensor used when calculating automatic backlight should
         * be filtered through an moving average filter.
         * The value is boolean (1 or 0).
         *
         * @hide
         */
        public static final String LIGHT_FILTER = "light_filter";

        /**
         * Window length of filter used when calculating automatic backlight.
         * One minute means that the average sensor value last minute is used.
         * The value is integer (milliseconds)
         *
         * @hide
         */
        public static final String LIGHT_FILTER_WINDOW = "light_filter_window";

        /**
         * Reset threshold of filter used when calculating automatic backlight.
         * Sudden large jumps in sensor value resets the filter. This is used
         * to make the filter respond quickly to large enough changes in input
         * while still filtering small changes. Example:
         *
         * Current filter value (average) is 100 and sensor value is changing to
         * 10, 150, 100, 30, 50. The filter is continously taking the average of
         * the samples. Now the user goes outside and the value jumps over 1000.
         * The difference between current average and new sample is larger than
         * the reset threshold and filter is reset. It begins calculating a new
         * average on samples around 1000 (say, 800, 1200, 1000, 1100 etc.)
         *
         * The value is integer (lux)
         *
         * @hide
         */
        public static final String LIGHT_FILTER_RESET = "light_filter_reset";

        /**
         * Sample interval of filter used when calculating automatic backlight.
         * The value is integer (milliseconds)
         *
         * @hide
         */
        public static final String LIGHT_FILTER_INTERVAL = "light_filter_interval";

        public static final String ALERTDIALOG_STYLES = "alertdiaog_styles";

        /**
         * @hide
         */
        public static final String NIGHT_MODES = "night_modes";

        /**
         * @hide
         */
        public static final String NIGHTMODES_ENABLE = "nightmode_enable";

        /**
         * Whether the audible piano tones are played by the dialer when dialing.
         * The value is boolean (1 or 0).
         */
        public static final String PIANO_TONE_WHEN_DIALING = "piano_tone";

        /**
         * Switch widget page style
         * 1: shows switches and notifications in a single page
         * 2: shows switches and notifications in separate pages
         * default: 2
         * @hide
         */
        public static final String SWITCH_WIDGET_STYLE = "switch_widget_style";

        /**
         * Determines the order in which switch buttons are shown in the widget in the dual pages mode
         * @hide
         */
        public static final String SWITCH_WIDGET_BUTTONS = "switch_widget_buttons";

        /**
         * Determines which switch buttons are shown in the widget in the single page mode
         * @hide
         */
        public static final String SWITCH_WIDGET_BUTTONS_TINY = "switch_widget_buttons_tiny";

        /**
         * Determines whether it's now in screen capture mode;
         * MENU key event will be intercepted in screen capture mode
         * @hide
         */
        public static final String SCREEN_CAPTURE_MODE = "screen_capture_mode";

        /**
         * The screen capture style: continual or single shot
         * @hide
         */
        public static final String SCREEN_CAPTURE_STYLE = "screen_capture_style";

        /**
         * The screen capture method: MENU key, timer or shake
         * @hide
         */
        public static final String SCREEN_CAPTURE_METHOD = "screen_capture_method";
        public static final String SCREEN_CAPTURE_INTERVAL = "screen_capture_interval ";
        public static final String SCREEN_CAPTURE_FORMAT = "screen_capture_format";
        public static final String SCREEN_CAPTURE_SHUTTER = "screen_capture_shutter";

        public static final int SCREEN_CAPTURE_METHOD_SHAKE = 1;
        public static final int SCREEN_CAPTURE_METHOD_MENU = 2;
        public static final int SCREEN_CAPTURE_METHOD_TIMER = 3;

        public static final String TORCH_STATE = "torch_state";

        //end,added by zhuyaopeng 2012/05/26

        //add by zhangxianjia 20120725, add statusbarbattery settings
        public static final String STATUS_BAR_BATTERY = "status_bar_batery";
        public static final String STATUS_BAR_BATTERY_STYLE= "status_bar_battery_style";
        //add by shenkerong
        public static final String STATUS_BAR_NET_SPEED= "status_bar_net_speed";

        /**
         * Whether to unlock the screen with the trackball. The value is boolean
         * (1 or 0).
         *
         * @hide
         */
        public static final String TRACKBALL_UNLOCK_SCREEN = "trackball_unlock_screen";

        /**
         * Whether to unlock the screen with the slide-out keyboard. The value
         * is boolean (1 or 0).
         *
         * @hide
         */
        public static final String SLIDER_UNLOCK_SCREEN = "slider_unlock_screen";

        /**
         * Whether the lockscreen should be disabled if security is on
         *
         */
        public static final String LOCKSCREEN_DISABLE_ON_SECURITY = "lockscreen_disable_on_security";

        /**
         * Whether to use the custom quick unlock screen control
         *
         * @hide
         */
        public static final String LOCKSCREEN_QUICK_UNLOCK_CONTROL = "lockscreen_quick_unlock_control";

        /**
         * Whether to use the custom app on both slider style and rotary style
         *
         * @hide
         */
        public static final String LOCKSCREEN_CUSTOM_APP_TOGGLE = "lockscreen_custom_app_toggle";

        /**
         * App to launch with custom app toggle enabled
         *
         * @hide
         */
        public static final String LOCKSCREEN_CUSTOM_APP_ACTIVITY = "lockscreen_custom_app_activity";

        /**
         * Ring Apps to launch with ring style and custom app toggle enabled
         *
         * @hide
         */
        public static final String[] LOCKSCREEN_CUSTOM_RING_APP_ACTIVITIES = new String[] {
            "lockscreen_custom_app_activity_1",
            "lockscreen_custom_app_activity_2",
            "lockscreen_custom_app_activity_3",
        "lockscreen_custom_app_activity_4" };

        /**
         * 1: Show custom app icon (currently cm logo) as with new patch 2: Show
         * messaging app icon as in old lockscreen possibly more in the future
         * (if more png files are drawn)
         *
         * @hide
         */
        public static final String LOCKSCREEN_CUSTOM_ICON_STYLE = "lockscreen_custom_icon_style";

        /**
         * Modify lockscreen widgets layout (time,date,carrier,msg,status)
         *
         * @hide
         */
        public static final String LOCKSCREEN_WIDGETS_LAYOUT = "lockscreen_widgets_layout";

        /**
         * When enabled, rotary lockscreen switches app starter and unlock, so
         * you can drag down to unlock
         *
         * @hide
         */
        public static final String LOCKSCREEN_ROTARY_UNLOCK_DOWN = "lockscreen_rotary_unlock_down";

        /**
         * When enabled, directional hint arrows are supressed
         *
         * @hide
         */
        public static final String LOCKSCREEN_ROTARY_HIDE_ARROWS = "lockscreen_rotary_hide_arrows";

        // Woody Guo @ 2012/05/04
        /**
         * 1 is enabled, 0 is disabled When enabled, will change power mode when
         * battery level is lower/higher than configured level
         *
         * @hide
         */
        public static final String CHANGE_POWER_MODE_IF_LOW_BATTERY = "change_power_mode_if_low_battery";
        /**
         * Configured battery level decides whether or not to change power mode
         *
         * @hide
         */
        public static final String CHANGE_POWER_MODE_BATTERY_LEVEL = "change_power_mode_battery_level";
        // END

        // Begin for lockscreen sound ,by fulianwu 20111228
        /**
         * 0 is off,1 is on
         **/
        public static final String LOCKSCREEN_SOUND_SWITCH = "lockscreen_sound_switch";

        /**
         * 0 is first time start lewa rom,1 is not the first time start lewa rom
         *
         */
        public static final String LOCKSCREEN_FIRST_TIME_UNLOCK_PROMPT = "lockscreen_first_time_unlock_prompt";
        // End

        //Begin for lockscreen changed ,by fulianwu 20120529
        /**
         * 0 is not change, 1 is changed
         * @hide
         */
        public static final String LOCKSCREEN_CHANGED = "lockscreen_changed";
        //End


        //Begin for incallstyle changed ,by yljiang@lewatek.com 2013-12-13
        /**
         * 0 is not change, 1 is changed
         * @hide
         */
        public static final String INCALLSTYLE_CHANGED = "incallstyle_changed";
        //End
        /**
         * When enabled, ring style lockscreen switches app started and unlock,
         * so the unlock ring is in the middle
         *
         * @hide
         */
        public static final String LOCKSCREEN_RING_UNLOCK_MIDDLE = "lockscreen_ring_unlock_middle";

        /**
         * When enabled, ring style lockscreen has only one ring in the middle
         * for unlock
         *
         * @hide
         */
        public static final String LOCKSCREEN_RING_MINIMAL = "lockscreen_ring_minimal";

        /**
         * Sets the lockscreen style
         *
         */
        public static final String LOCKSCREEN_STYLE_PREF = "lockscreen_style_pref";

        /**
         * Sets the lockscreen background style
         *
         * @hide
         */
        public static final String LOCKSCREEN_BACKGROUND = "lockscreen_background";

        /**
         * Whether to unlock the menu key. The value is boolean (1 or 0).
         *
         * @hide
         */
        public static final String MENU_UNLOCK_SCREEN = "menu_unlock_screen";

        /**
         * Whether to always show battery status
         *
         * @hide
         */
        public static final String LOCKSCREEN_ALWAYS_BATTERY = "lockscreen_always_battery";

        /**
         * Whether to show the next calendar event
         *
         * @hide
         */
        public static final String LOCKSCREEN_CALENDAR_ALARM = "lockscreen_calendar_alarm";

        /**
         * Whether to show the next calendar event's location
         *
         * @hide
         */
        public static final String LOCKSCREEN_CALENDAR_SHOW_LOCATION = "lockscreen_calendar_show_location";

        /**
         * Whether to show the next calendar event's description
         *
         * @hide
         */
        public static final String LOCKSCREEN_CALENDAR_SHOW_DESCRIPTION = "lockscreen_calendar_show_description";

        /**
         * Which calendars to look for events
         *
         * @hide
         */
        public static final String LOCKSCREEN_CALENDARS = "lockscreen_calendars";

        /**
         * How far in the future to look for events
         *
         * @hide
         */
        public static final String LOCKSCREEN_CALENDAR_LOOKAHEAD = "lockscreen_calendar_lookahead";

        /**
         * Whether to find only events with reminders
         *
         * @hide
         */
        public static final String LOCKSCREEN_CALENDAR_REMINDERS_ONLY = "lockscreen_calendar_reminders_only";

        /**
         * Whether to use lockscreen music controls
         *
         * @hide
         */
        public static final String LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";

        /**
         * Whether to show currently playing song title and artist
         *
         * @hide
         */
        public static final String LOCKSCREEN_NOW_PLAYING = "lockscreen_now_playing";

        /**
         * Whether to show currently playing song album art
         *
         * @hide
         */
        public static final String LOCKSCREEN_ALBUM_ART = "lockscreen_album_art";

        /**
         * Whether to use lockscreen music controls with headset connected
         *
         * @hide
         */
        public static final String LOCKSCREEN_MUSIC_CONTROLS_HEADSET = "lockscreen_music_controls_headset";

        /**
         * Whether to use always use lockscreen music controls
         *
         * @hide
         */
        public static final String LOCKSCREEN_ALWAYS_MUSIC_CONTROLS = "lockscreen_always_music_controls";

        /**
         * Whether to listen for gestures on the lockscreen
         *
         * @hide
         */
        public static final String LOCKSCREEN_GESTURES_ENABLED = "lockscreen_gestures_enabled";

        /**
         * Whether to show the gesture trail on the lockscreen
         *
         * @hide
         */
        public static final String LOCKSCREEN_GESTURES_TRAIL = "lockscreen_gestures_trail";

        /**
         * Sensitivity for parsing gestures on the lockscreen
         *
         * @hide
         */
        public static final String LOCKSCREEN_GESTURES_SENSITIVITY = "lockscreen_gestures_sensitivity";

        /**
         * Color value for gestures on lockscreen
         *
         * @hide
         */
        public static final String LOCKSCREEN_GESTURES_COLOR = "lockscreen_gestures_color";

        /**
         * Use the Notification Power Widget? (Who wouldn't!)
         *
         * @hide
         */
        public static final String EXPANDED_VIEW_WIDGET = "expanded_view_widget";

        /**
         * Whether to hide the notification screen after clicking on a widget
         * button
         *
         * @hide
         */
        public static final String EXPANDED_HIDE_ONCHANGE = "expanded_hide_onchange";

        /**
         * Hide scroll bar in power widget
         *
         * @hide
         */
        public static final String EXPANDED_HIDE_SCROLLBAR = "expanded_hide_scrollbar";

        /**
         * Hide indicator in status bar widget
         *
         * @hide
         */
        public static final String EXPANDED_HIDE_INDICATOR = "expanded_hide_indicator";

        /**
         * Haptic feedback in power widget
         *
         * @hide
         */
        public static final String EXPANDED_HAPTIC_FEEDBACK = "expanded_haptic_feedback";

        /**
         * Notification Indicator Color
         *
         * @hide
         */
        public static final String EXPANDED_VIEW_WIDGET_COLOR = "expanded_widget_color";
        /** 
         * Notification Power Widget - Custom Brightness Mode
         * @hide
         */
        public static final String EXPANDED_BRIGHTNESS_MODE = "expanded_brightness_mode";

        /** 
         * Notification Power Widget - Custom Network Mode
         * @hide
         */
        public static final String EXPANDED_NETWORK_MODE = "expanded_network_mode";

        /** 
         * Notification Power Widget - Custom Screen Timeout
         * @hide
         */
        public static final String EXPANDED_SCREENTIMEOUT_MODE = "expanded_screentimeout_mode";

        /** 
         * Notification Power Widget - Custom Ring Mode
         * @hide
         */
        public static final String EXPANDED_RING_MODE = "expanded_ring_mode";

        /** 
         * Notification Power Widget - Custom Torch Mode
         * @hide
         */
        public static final String EXPANDED_FLASH_MODE = "expanded_flash_mode";
        /**
         * Record font scale from EM
         * @hide
         */
        public static final String FONT_SCALE_SMALL = "settings_fontsize_small";

       /**
         * @hide
         */
        public static final String FONT_SCALE_LARGE = "settings_fontsize_large";

       /**
         * @hide
         */
        public static final String FONT_SCALE_EXTRALARGE = "settings_fontsize_extralarge";

        //add by chenqiang for intercept
        /**
         * @hide
         */
        public static final String INTERCEPT_SWITCH = "intercept_switch";

        /**
         * @hide
         */
        public static final String INTERCEPT_NOTIFICATION_SWITCH = "intercept_notification_switch";

        /**
         * @hide
         */
        public static final String INTERCEPT_ONERING_SWITCH = "intercept_onering_switch";

        /**
         * whether volume keys wake the screen. boolean value
         * 
         * @hide
         */
        public static final String VOLUME_WAKE_SCREEN = "volume_wake_screen";

        /**
         * Whether volume up/down can be long pressed to skip tracks
         * @hide
         */
        public static final String VOLUME_MUSIC_CONTROLS = "volume_music_controls";

        /**
         * Whether powerkey can be long pressed to shutter
         * @hide
         */
        public static final String CAMERA_POWER_SHUTTER = "camera_power_shutter";

        /**
         * Whether to allow killing of the foreground app by long-pressing the Back button
         * @hide
         */
        public static final String KILL_APP_LONGPRESS_BACK = "back_to_kill";

        /**
          * Subscription to be used for SMS on a multi sim device. The supported values
          * are 0 = SUB1, 1 = SUB2.
          * @hide
          */
        public static final String MULTI_SIM_SMS_SUBSCRIPTION = "multi_sim_sms";

        /**
          * Channel name for subcription one and two i.e. channele name 1, channel name 2
          * @hide
          */
        public static final String [] MULTI_SIM_NAME = {"perferred_name_sub1", "preferred_name_sub2"};

        /**
          * Whether to allow custom action by long-pressing the Menu button
          *@hide
          */
        public static final String LONG_PRESS_MENU_CUSTOM_ACTION = "menu_custom_action";

        /**
         * @hide
         */
        public static final String CALL_RECORD = "call_record";

        /**
         * @hide
         */
        public static final String BREATHING_LIGHT_COLOR = "breathing_light_color";

        /**
         * @hide
         */
        public static final String BREATHING_LIGHT_FREQ = "breathing_light_freq";

        /**
         * @hide
         */
        public static final String CALL_BREATHING_LIGHT_COLOR = "call_breathing_light_color";

        /**
         * @hide
         */
        public static final int CALL_BREATHING_LIGHT_COLOR_DEFAULT = Resources.getSystem().getColor(lewa.R.color.android_config_defaultNotificationColor);

        /**
         * @hide
         */
        public static final String CALL_BREATHING_LIGHT_FREQ = "call_breathing_light_freq";

        /**
         * @hide
         */
        public static final int CALL_BREATHING_LIGHT_FREQ_DEFAULT = 0; // NingYi For Launcher // Resources.getSystem().getInteger(lewa.R.integer.config_defaultNotificationLedFreq);

        /**
         * @hide
         */
        public static final String MMS_BREATHING_LIGHT_COLOR = "mms_breathing_light_color";

        /**
         * @hide
         */
        public static final String MMS_BREATHING_LIGHT_FREQ = "mms_breathing_light_freq";

        /**
         * Whether draw rounded corner on activity background
         * The value is boolean (1 or 0).
         * @hide
         */
        public static final String ACTIVITY_ROUNDED_CORNER_ENABLE = "activity_rounded_corner_enable";
///ADD BEGIN by wsliu@lewatek.com for sensorprovider
        /**
         *Whether turn phone switch silence when calling.
         */
        public static final String TURN_SILENT_WHEN_CALLING = "turn_silence";
        /**
         *Whether smart answer when calling.
         */
        public static final String SMART_ANSWER = "smart_answer";
        /**
         *Whether packet ring max when calling.
         */
        public static final String PACKET_RING = "packet_ring";
         /**
          *Whether desk ring adjustvolume when calling.
          */
        public static final String DESK_RING = "desk_ring";
         /**
          *Whether auto speak during calling.
          */
        public static final String SMART_SPEAK = "smart_speak";
         /**
          *Whether auto call on the contact detail.
          */
        public static final String SMART_CALL = "smart_call";
         /**
          *Whether sensor shake degree adjust.
          */
        public static final String SENSOR_SHAKE_DEGREE = "sensor_shake_degree";
        /**
          *Add for shake and shake value.
          */
        public static final String SENSOR_SHAKE_VALUE = "sensor_shake_value";
        /**
         *Add for phone turn down degree value.
         */
        public static final String SENSOR_TURN_DOWN_VALUE = "sensor_turn_down_value";
        /**
          *Add for phone turn up degree value.
          */
        public static final String SENSOR_TURN_UP_VALUE = "sensor_turn_up_value";
        /**
         *Add for phone call delay time value.
         */
        public static final String SENSOR_CALL_DELAY_VALUE = "sensor_call_delay_value";
        /**
         *Add for lock screen weather animation.
         */
        public static final String LOCK_SCREEN_FANCY_WEATHER = "lock_screen_fancy_weather_enabled";
        /**
          *Add for lock screen kill all app.
          */
        public static final String LOCK_SCREEN_KILLAPP = "lock_screen_killapp";

        public static final String FLOAT_BALL_SHOW = "float_ball_show";

        //LEWA ADD BEGIN
        /**
         *Add for three fingers screenshot
         */
        public static final String THREE_FINGER_SCREENSHOT ="three_finger_screenshot";
        public static final String SCREENSHOT_WINDOW_STATUS ="screenshot_window_status";
        public static final String BRIGHTNESS_WINDOW_STATUS ="brightness_window_status";
         /**
         *Add for two fingers gesture
         */
        public static final String TWO_FINGER_UPDATE_BRIGHTNESS ="two_finger_update_brightness";
        //LEWA ADD END

        //added for ringtone settings
        public static final String RINGTONE_PATH = "ringtone_path";
        public static final String NOTIFICATION_SOUND_PATH = "notification_sound_path";
        public static final String ALARM_ALERT_PATH = "alarm_alert_path";
        public static final String RINGTONE_2_PATH = "ringtone_2_path";
///ADD END

        /**
         * Add for keeping launcher resident
         * @hide
         */
        public static final String KEEP_LAUCHER_RESIDENT = "keep_launcher_resident";

        /**
        * Add for inadvert mode
        * @hide
        */
        public static final String INADVERTMODE_ENABLE = "inadvertmode_enable";

        /**
        * Add for Launcher action_updown setting
        * @hide
        */
        public static final String LAUNCHER_ACTION_UP_DOWN = "launcher_action_up_down";
       /**
        * Add for default launcher model
        * @hide
        */
        public static final String DEFAULT_LAUNCHER_MODEL = "default_launcher_model";
       /*
        * Add for lock screen message 
        * @hide
        */
        public static final String LOCKSCREEN_MESSAGE_ENABLED = "lock_screen_list_msg_enabled";
       /*
        * Add for bird hand model
        * @hide
        */
        public static final String BIRD_HAND_SWITCH_MODEL = "bird_hand_switch_model";
       /*
        * Add for lewa app lock
        * @hide
        */
        public static final String LEWA_APPLOCK_ENABLED = "lewa_applock_enabled";

        /**
         * Whether the audible DTMF tones are played by the dialer when dialing. The value is
         * boolean (1 or 0).
         */
        public static final String DTMF_TONE_WHEN_DIALING = "dtmf_tone";

         /*
        * Add for recording_status
        * @hide
        */
        public static final String AUDIO_RECORDING_STATUS = "recording_status";    
        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @param Value to return if the setting is not defined
         * @return the corresponding value, or null if not present
         */
        public synchronized static String getString(ContentResolver resolver, String name, String defValue) {
            String result = null;
            try {
                result = Settings.System.getString(resolver, name);
            } catch (Exception e) {
            }

            if(result != null) {
                return result;
            } else {
                return defValue;
            }
        }
    }

    public static final class Secure {
        /** @hide */
        public static boolean showMagnifierWhenInput(Context context) {
            return (Build.SHOW_MAGNIFIER_WHEN_INPUT || context.getResources().getBoolean(lewa.R.bool.enable_magnifier_when_input)) && 
                (1 == android.provider.Settings.Secure.getInt(context.getContentResolver(), SHOW_MAGNIFIER_WHEN_INPUT, DEFAULT_SHOW_MAGNIFIER_WHEN_INPUT));
        }

        /**
        * Whether uploading debug log to server
        *
        * 1 = allow uploading debug log to server 0 = not allow uploading
        * from the Android Market
        * @hide
        */
        public static final String LEWA_UPLOAD_DEBUG_LOG = "lewa_upload_debug_log";

        /**
         * Whether to disable the lockscreen unlock tab
         *
         * @hide
         */
        public static final String LOCKSCREEN_GESTURES_DISABLE_UNLOCK = "lockscreen_gestures_disable_unlock";

        /**
         * Whether to show the magnifier when input
         *
         * @hide
         */
        public static final String SHOW_MAGNIFIER_WHEN_INPUT = "show_magnifier_when_input";

        /**
         * @hide
         */
        public static int DEFAULT_SHOW_MAGNIFIER_WHEN_INPUT = 0;

        /**
         * @hide
         */
        public static final String LEWALOCKSCREEN_INUSE = "lewa_lockscreen_inuse";

        /**
         * @hide
         */
        public static final String SCREEN_BUTTONS_STATE = "screen_buttons_state";

        /**
         * @hide
         */
        public static final String SCREEN_BUTTONS_TURN_ON = "screen_buttons_turn_on";
    }
}
