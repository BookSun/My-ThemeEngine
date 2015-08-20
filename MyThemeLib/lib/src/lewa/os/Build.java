package lewa.os;

import android.os.SystemProperties;
import android.text.TextUtils;

/**
 * Information about Lewa OS, extracted from system properties.
 */
public class Build extends android.os.Build {
    /** The device name set by / used in LewaOS. */
    public static final String LEWA_DEVICE = SystemProperties.get("ro.lewa.device", UNKNOWN);

    public static final boolean LEWA_DEFAULT_VIBRATION_GENERAL = SystemProperties.getBoolean("ro.lewa.defvibration", false);
    public static final String LEWA_DEFAULT_RINGER_VOLUME_GENERAL = SystemProperties.get("ro.lewa.defringvolume", "8");
    public static final String LEWA_DEFAULT_NOTIFICATION_VOLUME_GENERAL = SystemProperties.get("ro.lewa.defnotificationvolume", "8");
    public static final String LEWA_DEFAULT_ALARM_VOLUME_GENERAL = SystemProperties.get("ro.lewa.defalarmvolume", "8");
    public static final boolean LEAW_DEFAULT_HAPTIC_FEEDBACK_GENERAL = SystemProperties.getBoolean("ro.lewa.hapticfeedback", false);
    /** The version of LewaOS. */
    public static final String LEWA_VERSION = SystemProperties.get("ro.lewa.version", UNKNOWN);

    public static final String PLATFORM_MTK_6575 = "MT6575";
    public static final String PLATFORM_MTK_6577 = "MT6577";
    /** The platform of LewaOS. */
    public static final String PLATFORM = SystemProperties.get("ro.mediatek.platform", UNKNOWN);

    /** Return true if there are 2 sdcards in the device. */
    public static final boolean HAS_DUAL_SDCARDS = SystemProperties.getBoolean("ro.sys.dualSD", false);

    /** Return true if support LEWA waxin in the device. */
    public static final boolean LEWA_WAXIN_SUPPORT = SystemProperties.getBoolean("ro.lewa.waxin", false);

    /** Return true if support LEWA intercept in the device. */
    public static final boolean LEWA_INTERCEPT_SUPPORT = SystemProperties.getBoolean("ro.lewa.intercept", true);

    /** Return if 24 or 12 hour format*/
    public static final String TimeFormatHour = SystemProperties.get("ro.time.format.hour", UNKNOWN);

    /** Return true if shutdown animation is enable */
    public static final boolean SHUTDOWN_ANIMATIOM = SystemProperties.getBoolean("ro.build.shutdown.animation", false);

    /** add by benwu */
    public static final boolean LEWA_CTA_SUPPORT = (SystemProperties.getBoolean("ro.lewa.cta", false) && android.os.Build.TYPE.equals("eng"));

    /** Return ro.lewa.b2b.version*/
    public static final String B2bVersion = SystemProperties.get("ro.lewa.b2b.version", UNKNOWN);

    /** Return ro.lewa.hardware.version*/
    public static final String HardwareVersion = SystemProperties.get("ro.lewa.hardware.version", UNKNOWN);
    /** Return true if support LEWA pim weibo in the device */
    public static final boolean PIM_WEIBO_SUPPORT = SystemProperties.getBoolean("ro.lewa.b2b.pim.weibo",false);

    /** Return ro.sys.partner*/
    public static final String LEWA_CHANNEL = SystemProperties.get("ro.sys.partner", "Lewa");

    /** Return true if support LEWA dualbutton just switch sim card in the device. */
    public static final boolean LEWA_SWITCHSIM_SUPPORT = SystemProperties.getBoolean("ro.lewa.dualbutton.switchsim", true);

    //by zhangxianjia
    public static final boolean LEWA_CAMERA_FLASHLIGHT = SystemProperties.getBoolean("ro.lewa.camera.flashlight", false);

    /** Return large font size*/
    public static final String DefaultFontScale = SystemProperties.get("ro.lewa.font.default", UNKNOWN);

    /**Return true if support video call  */
    public static final boolean LEWA_VIDEOCALL_SUPPORT = SystemProperties.getBoolean("ro.lewa.videocall", false);

    /**Return true if support customer sales statistics  */
    public static final boolean LEWA_SALES_STATISTICS_SUPPORT = SystemProperties.getBoolean("ro.lewa.sales.statistics", false);

    /**Return true if support launcher SCENE  */
    public static final boolean LEWA_SCENE_SUPPORT = SystemProperties.getBoolean("ro.lewa.scene", false);

    /**Return true if support dial default mode is fastmode  */
    public static final boolean LEWA_DIAL_FASTMODE_SUPPORT = SystemProperties.getBoolean("ro.lewa.dial.fastmode", false);

    /**Return true if support root option  */
    public static final boolean ROOT_ACCESS_SUPPORT = SystemProperties.getBoolean("ro.lewa.root.access", false);

    /**Return true if data off at default  */
    public static final boolean LEWA_DATA_OFF_DEFAULT = SystemProperties.getBoolean("ro.lewa.data.off", false);

    /**Return true if support magnifier when input */
    public static final boolean SHOW_MAGNIFIER_WHEN_INPUT = "maguro".equals(DEVICE) ? true : false;

    public static final boolean IS_GALAXYS_NEXUS = "maguro".equals(DEVICE);

    public static final boolean IS_MIONE_CDMA = ("mione_plus".equals(DEVICE) && isMsm8660());

    public static final boolean IS_MITWO_CDMA = ("aries".equals(DEVICE) && hasCdmaProperty());

    public static final boolean IS_COOLPAD_5890 = "Coolpad5890".equals(DEVICE);

    public static final boolean IS_OPPO_N1 = "OPPO_N1_JB2".equals(LEWA_DEVICE);

    /**Return proximity valid change delay */
    public static final int PROXIMITY_VALID_CHANGE_DELAY = SystemProperties.getInt("ro.lewa.prox.validchangedelay", 260);

    public static final String LewaDefaultSmsNotificationName = SystemProperties.get("ro.config.sms_sound");

    private static boolean isMsm8660() {
        String soc = SystemProperties.get("ro.soc.name");
        return (TextUtils.equals(soc, "msm8660")) || (TextUtils.equals(soc, "unkown"));
    }

    private static boolean hasCdmaProperty() {
        String cdma = SystemProperties.get("persist.radio.modem");
        return (!TextUtils.isEmpty(cdma)) && ("CDMA".equals(cdma));
    }

    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /*
     * public static boolean hasJellyBean() {
     *     return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
     * }
     */
}
