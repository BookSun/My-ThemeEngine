
package com.lewa.support.util;

/*import com.lewa.lockscreen.R;*/
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.Time;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.lewa.support.R;

@SuppressWarnings("unused")
public class LunarDate {

    public static final int MAX_LUNAR_YEAR = 2050;

    public static final int MIN_LUNAR_YEAR = 1900;

    private static final long[] luYearData = new long[] {
            19416L, 19168L, 42352L, 21717L, 53856L, 55632L, 91476L, 22176L, 39632L, 21970L, 19168L,
            42422L, 42192L, 53840L, 119381L, 46400L, 54944L, 44450L, 38320L, 84343L, 18800L,
            42160L, 46261L, 27216L, 27968L, 109396L, 11104L, 38256L, 21234L, 18800L, 25958L,
            54432L, 59984L, 28309L, 23248L, 11104L, 100067L, 37600L, 116951L, 51536L, 54432L,
            120998L, 46416L, 22176L, 107956L, 9680L, 37584L, 53938L, 43344L, 46423L, 27808L,
            46416L, 86869L, 19872L, 42448L, 83315L, 21200L, 43432L, 59728L, 27296L, 44710L, 43856L,
            19296L, 43748L, 42352L, 21088L, 62051L, 55632L, 23383L, 22176L, 38608L, 19925L, 19152L,
            42192L, 54484L, 53840L, 54616L, 46400L, 46496L, 103846L, 38320L, 18864L, 43380L,
            42160L, 45690L, 27216L, 27968L, 44870L, 43872L, 38256L, 19189L, 18800L, 25776L, 29859L,
            59984L, 27480L, 21952L, 43872L, 38613L, 37600L, 51552L, 55636L, 54432L, 55888L, 30034L,
            22176L, 43959L, 9680L, 37584L, 51893L, 43344L, 46240L, 47780L, 44368L, 21977L, 19360L,
            42416L, 86390L, 21168L, 43312L, 31060L, 27296L, 44368L, 23378L, 19296L, 42726L, 42208L,
            53856L, 60005L, 54576L, 23200L, 30371L, 38608L, 19415L, 19152L, 42192L, 118966L,
            53840L, 54560L, 56645L, 46496L, 22224L, 21938L, 18864L, 42359L, 42160L, 43600L,
            111189L, 27936L, 44448L
    };

    private static final char[] iSolarLunarOffsetTable = new char[] {
            '1', '&', '\u001c', '.', '\"', '\u0018', '+', ' ', '\u0015', '(', '\u001d', '0', '$',
            '\u0019', ',', '!', '\u0016', ')', '\u001f', '2', '&', '\u001b', '.', '#', '\u0017',
            '+', ' ', '\u0016', '(', '\u001d', '/', '$', '\u0019', ',', '\"', '\u0017', ')',
            '\u001e', '1', '&', '\u001a', '-', '#', '\u0018', '+', ' ', '\u0015', '(', '\u001c',
            '/', '$', '\u001a', ',', '!', '\u0017', '*', '\u001e', '0', '&', '\u001b', '-', '#',
            '\u0018', '+', ' ', '\u0014', '\'', '\u001d', '/', '$', '\u001a', '-', '!', '\u0016',
            ')', '\u001e', '0', '%', '\u001b', '.', '#', '\u0018', '+', ' ', '2', '\'', '\u001c',
            '/', '$', '\u001a', '-', '\"', '\u0016', '(', '\u001e', '1', '%', '\u001b', '.', '#',
            '\u0017', '*', '\u001f', '\u0015', '\'', '\u001c', '0', '%', '\u0019', ',', '!',
            '\u0016', '(', '\u001e', '1', '&', '\u001b', '.', '#', '\u0018', '*', '\u001f',
            '\u0015', '(', '\u001c', '/', '$', '\u0019', '+', '!', '\u0016', ')', '\u001e', '1',
            '&', '\u001b', '-', '\"', '\u0017', '*', '\u001f', '\u0015', '(', '\u001d', '/', '$',
            '\u0019', ',', ' ', '\u0016'
    };

    private static int[] lunarHolidaysTable = new int[] {
            101, 115, 505, 707, 815, 909, 1208
    };

    private static int[] solarHolidaysTable = new int[] {
            101, 214, 308, 312, 401, 501, 504, 601, 701, 801, 910, 1001, 1225
    };

    private static int[] solarHolidaysTable_TW = new int[] {
            101, 214, 228, 308, 312, 501, 928, 1010, 1112, 1225
    };

    private static int[] lunarHolidays = new int[] {
            R.string.the_spring_festival, R.string.lantern_festival, R.string.the_dragon_boat_festival, R.string.double_seventh_day, R.string.the_mid_autumn_festival, R.string.the_double_ninth_festival, R.string.the_laba_rice_porridge_festival
    };

    private static int[] solarHolidays = new int[] {
            R.string.new_years_day, R.string.valentines_day, R.string.international_womens_day, R.string.arbor_day, R.string.fools_day, R.string.labour_day, R.string.chinese_youth_day, R.string.childrens_day,
            R.string.partys_day, R.string.the_armys_day, R.string.teachers_day, R.string.national_day, R.string.christmas_day
    };

    private static int[] solarHolidays_TW = new int[] {
            R.string.new_years_day, R.string.valentines_day, 101450126, R.string.international_womens_day, R.string.arbor_day, R.string.labour_day, R.string.teachers_day, R.string.national_day,
            R.string.national_father_day, R.string.christmas_day
    };

    private static int[] solarTerms = new int[] {
            R.string.slight_cold,
            R.string.great_cold,
            R.string.spring_begins,
            R.string.the_rains,
            R.string.insects_awaken,
            R.string.vernal_equinox,
            R.string.clear_and_bright,
            R.string.grain_rain,
            R.string.summer_begins,
            R.string.grain_buds,
            R.string.grain_in_ear,
            R.string.summer_solstice,
            R.string.slight_heat,
            R.string.great_heat,
            R.string.autumn_begins,
            R.string.stopping_the_heat,
            R.string.white_dews,
            R.string.autumn_equinox,
            R.string.cold_dews,
            R.string.hoar_frost_falls,
            R.string.winter_begins,
            R.string.light_snow,
            R.string.heavy_snow,
            R.string.winter_solstice
    };

    private static char[] solarTermsTable = new char[] {
            '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x',
            'w', '\u0096', '\u00a4', '\u0096', '\u0096', '\u0097', '\u0087', 'y', 'y', 'y', 'i',
            'x', 'x', '\u0096', '\u00a5', '\u0087', '\u0096', '\u0087', '\u0087', 'y', 'i', 'i',
            'i', 'x', 'x', '\u0086', '\u00a5', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088',
            'x', 'x', 'y', 'x', '\u0087', '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097',
            '\u0097', 'x', 'y', 'y', 'i', 'x', 'w', '\u0096', '\u00a4', '\u0096', '\u0096',
            '\u0097', '\u0097', 'y', 'y', 'y', 'i', 'x', 'x', '\u0096', '\u00a5', '\u0087',
            '\u0096', '\u0087', '\u0087', 'y', 'i', 'i', 'i', 'x', 'x', '\u0086', '\u00a5',
            '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087',
            '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x',
            'w', '\u0096', '\u00a4', '\u0096', '\u0096', '\u0097', '\u0097', 'y', 'y', 'y', 'i',
            'x', 'x', '\u0096', '\u00a5', '\u0087', '\u0096', '\u0087', '\u0087', 'y', 'i', 'i',
            'i', 'x', 'x', '\u0086', '\u00a5', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088',
            'x', 'x', 'i', 'x', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a6', '\u0097',
            '\u0097', 'x', 'y', 'y', 'i', 'x', 'w', '\u0096', '\u00b4', '\u0096', '\u00a6',
            '\u0097', '\u0097', 'y', 'y', 'y', 'i', 'x', 'x', '\u0096', '\u00a5', '\u0097',
            '\u0096', '\u0097', '\u0087', 'y', 'y', 'i', 'i', 'x', 'x', '\u0096', '\u00a5',
            '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w', '\u0087',
            '\u0095', '\u00b4', '\u0096', '\u00a6', '\u0096', '\u0097', 'x', 'y', 'x', 'i', 'x',
            '\u0087', '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'y', 'y', 'y',
            'i', 'x', 'w', '\u0096', '\u00a5', '\u0097', '\u0096', '\u0097', '\u0087', 'y', 'y',
            'i', 'i', 'x', 'x', '\u0096', '\u00a5', '\u0096', '\u00a5', '\u0096', '\u0097',
            '\u0088', 'x', 'x', 'y', 'w', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5',
            '\u0096', '\u0097', 'x', 'y', 'x', 'i', 'x', '\u0087', '\u0096', '\u00b4', '\u0096',
            '\u00a6', '\u0097', '\u0097', 'y', 'y', 'y', 'i', 'x', 'w', '\u0096', '\u00a4',
            '\u0096', '\u0096', '\u0097', '\u0087', 'y', 'y', 'i', 'i', 'x', 'x', '\u0096',
            '\u00a5', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w',
            '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', 'x', 'y', 'x',
            'i', 'x', '\u0087', '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x',
            'y', 'y', 'i', 'x', 'w', '\u0096', '\u00a4', '\u0096', '\u0096', '\u0097', '\u0087',
            'y', 'y', 'y', 'i', 'x', 'x', '\u0096', '\u00a5', '\u0096', '\u00a5', '\u0096',
            '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w', '\u0087', '\u0096',
            '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x', 'w',
            '\u0096', '\u00a4', '\u0096', '\u0096', '\u0097', '\u0087', 'y', 'y', 'y', 'i', 'x',
            'x', '\u0096', '\u00a5', '\u0096', '\u00a5', '\u0096', '\u0096', '\u0088', 'x', 'x',
            'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097',
            '\u0088', 'x', 'x', 'i', 'x', '\u0087', '\u0096', '\u00b4', '\u0096', '\u00a6',
            '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x', 'w', '\u0096', '\u00a4', '\u0096',
            '\u0096', '\u0097', '\u0097', 'y', 'y', 'y', 'i', 'x', 'x', '\u0096', '\u00a5',
            '\u0096', '\u00a5', '\u0096', '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087',
            '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i',
            'x', '\u0087', '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y',
            'y', 'i', 'x', 'w', '\u0096', '\u00a4', '\u0096', '\u0096', '\u0097', '\u0097', 'y',
            'y', 'y', 'i', 'x', 'x', '\u0096', '\u00a5', '\u0096', '\u00a5', '\u0096', '\u0096',
            '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5',
            '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087', '\u0096', '\u00b4',
            '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x', 'w', '\u0096',
            '\u00a4', '\u0096', '\u0096', '\u0097', '\u0097', 'y', 'y', 'y', 'i', 'x', 'x',
            '\u0096', '\u00a5', '\u0096', '\u00a5', '\u00a6', '\u0096', '\u0088', 'x', 'x', 'x',
            '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097',
            '\u0088', 'x', 'x', 'y', 'w', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a6',
            '\u0097', '\u0097', 'x', 'y', 'x', 'i', 'x', 'w', '\u0096', '\u00b4', '\u0096',
            '\u00a6', '\u0097', '\u0097', 'y', 'y', 'y', 'i', 'x', 'x', '\u0096', '\u00a5',
            '\u00a6', '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', 'x', 'x', '\u0087',
            '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'y',
            'x', 'y', 'w', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097',
            'x', 'y', 'x', 'i', 'x', 'w', '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097',
            '\u0097', 'y', 'y', 'y', 'i', 'x', 'x', '\u0096', '\u00a5', '\u00a6', '\u00a5',
            '\u00a6', '\u0096', '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087', '\u00a5',
            '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w',
            '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', 'x', 'y', 'x',
            'h', 'x', '\u0087', '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x',
            'y', 'y', 'i', 'x', 'w', '\u0096', '\u00a5', '\u00a5', '\u00a5', '\u00a6', '\u0096',
            '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w', '\u0087', '\u0095',
            '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x',
            '\u0087', '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y',
            'i', 'x', 'w', '\u0096', '\u00a4', '\u00a5', '\u00a5', '\u00a6', '\u0096', '\u0088',
            '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5',
            '\u0096', '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0096', '\u00b4',
            '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087',
            '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x',
            'w', '\u0096', '\u00a4', '\u00a5', '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088',
            '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096',
            '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087', '\u0096',
            '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x', 'w',
            '\u0096', '\u00a4', '\u00a5', '\u00a5', '\u00a6', '\u00a6', '\u0088', '\u0088',
            '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096',
            '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087', '\u0096',
            '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'y', 'i', 'x', 'w',
            '\u0096', '\u00a4', '\u00a5', '\u00a5', '\u00a6', '\u00a6', '\u0088', '\u0088',
            '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b5', '\u0096', '\u00a5', '\u00a6',
            '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087', '\u0096',
            '\u00b4', '\u0096', '\u00a6', '\u0097', '\u0097', 'x', 'y', 'x', 'i', 'x', 'w',
            '\u0096', '\u00a4', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0088', '\u0089',
            '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096',
            '\u0096', '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4',
            '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'x', '\u0087',
            '\u0096', '\u00b4', '\u0096', '\u00a6', '\u0096', '\u0097', 'x', 'y', 'x', 'i', 'x',
            'w', '\u0096', '\u00a4', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0088', '\u0088',
            '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u00a6',
            '\u0096', '\u0088', '\u0088', 'x', 'x', 'w', '\u0087', '\u0095', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w', '\u0087', '\u0095',
            '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', 'x', 'y', 'x', 'i', 'x', 'w',
            '\u0096', '\u00b4', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087', '\u0088',
            '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u00a6', '\u00a5', '\u00a6',
            '\u0096', '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4',
            '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w', '\u0087',
            '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'y', 'x', 'i',
            'x', '\u0087', '\u0096', '\u00b4', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087',
            '\u0088', '\u0088', 'x', '\u0087', '\u0086', '\u00a5', '\u00b4', '\u00a5', '\u00a5',
            '\u00a6', '\u0096', '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5',
            '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0096', '\u0088', 'x', 'x', 'y', 'w',
            '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0086', '\u0097', '\u0088', 'x',
            'x', 'i', 'x', '\u0087', '\u0096', '\u00b4', '\u00a5', '\u00b5', '\u00a6', '\u00a6',
            '\u0087', '\u0088', '\u0088', 'x', '\u0087', '\u0086', '\u00a5', '\u00b3', '\u00a5',
            '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0087',
            '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0096', '\u0088', 'x', 'x', 'x',
            '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097',
            '\u0088', 'v', 'x', 'i', 'x', '\u0087', '\u0096', '\u00b4', '\u00a5', '\u00b5',
            '\u00a6', '\u00a6', '\u0087', '\u0088', '\u0088', 'x', '\u0087', '\u0086', '\u00a5',
            '\u00b3', '\u00a5', '\u00a5', '\u00a6', '\u00a6', '\u0088', '\u0088', '\u0088', 'x',
            '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0096',
            '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5',
            '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087', '\u0096', '\u00b4',
            '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087', '\u0088', '\u0088', 'x', '\u0087',
            '\u0086', '\u00a5', '\u00b3', '\u00a5', '\u00a5', '\u00a6', '\u00a6', '\u0088',
            '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5',
            '\u0096', '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4',
            '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'i', 'x', '\u0087',
            '\u0096', '\u00b4', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087', '\u0088',
            '\u0088', 'x', '\u0087', '\u0086', '\u00a5', '\u00b3', '\u00a5', '\u00a5', '\u00a6',
            '\u00a6', '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4',
            '\u0096', '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', 'x', 'x', '\u0087',
            '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097', '\u0088', 'x',
            'x', 'i', 'x', '\u0087', '\u0096', '\u00b4', '\u00a5', '\u00b5', '\u00a6', '\u00a6',
            '\u0087', '\u0088', '\u0087', 'x', '\u0087', '\u0086', '\u00a5', '\u00b3', '\u00a5',
            '\u00b5', '\u00a6', '\u00a6', '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0087',
            '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', 'x',
            'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0097',
            '\u0088', 'x', 'x', 'y', 'x', '\u0087', '\u0096', '\u00b4', '\u00a5', '\u00b5',
            '\u00a5', '\u00a6', '\u0087', '\u0088', '\u0087', 'x', '\u0087', '\u0086', '\u00a5',
            '\u00b3', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087', '\u0088', '\u0088', 'x',
            '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u00a6', '\u0096',
            '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0097', '\u0088', 'x', 'x', 'y', 'w', '\u0087', '\u0095',
            '\u00b4', '\u00a5', '\u00b4', '\u00a5', '\u00a6', '\u0087', '\u0088', '\u0087', 'x',
            '\u0087', '\u0086', '\u00a5', '\u00c3', '\u00a5', '\u00b5', '\u00a6', '\u00a6',
            '\u0087', '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u00a6',
            '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087',
            '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0096', '\u0088', 'x', 'x', 'y',
            'w', '\u0087', '\u0095', '\u00b4', '\u00a5', '\u00b4', '\u00a5', '\u00a6', '\u0097',
            '\u0087', '\u0087', 'x', '\u0087', '\u0086', '\u00a5', '\u00c3', '\u00a5', '\u00b5',
            '\u00a6', '\u00a6', '\u0087', '\u0088', '\u0088', 'x', '\u0087', '\u0086', '\u00a5',
            '\u00b4', '\u00a5', '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', '\u0088', 'x',
            '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0096',
            '\u0088', 'x', 'x', 'y', 'w', '\u0087', '\u0095', '\u00b4', '\u00a5', '\u00b4',
            '\u00a5', '\u00a6', '\u0097', '\u0087', '\u0087', 'x', '\u0087', '\u0096', '\u00a5',
            '\u00c3', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087', '\u0088', '\u0088', 'x',
            '\u0087', '\u0086', '\u00a5', '\u00b3', '\u00a5', '\u00a5', '\u00a6', '\u00a6',
            '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096',
            '\u00a5', '\u0096', '\u0096', '\u0088', 'x', 'x', 'x', '\u0087', '\u0087', '\u0095',
            '\u00b4', '\u00a5', '\u00b4', '\u00a5', '\u00a6', '\u0097', '\u0087', '\u0087', 'x',
            '\u0087', '\u0096', '\u00a5', '\u00c3', '\u00a5', '\u00b5', '\u00a6', '\u00a6',
            '\u0087', '\u0088', '\u0088', 'x', '\u0087', '\u0086', '\u00a5', '\u00b3', '\u00a5',
            '\u00a5', '\u00a6', '\u00a6', '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0087',
            '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u0096', '\u0096', '\u0088', 'x', 'x', 'x',
            '\u0087', '\u0087', '\u0095', '\u00b4', '\u00a5', '\u00b4', '\u00a5', '\u00a6',
            '\u0097', '\u0087', '\u0087', 'x', '\u0087', '\u0096', '\u00a5', '\u00c3', '\u00a5',
            '\u00b5', '\u00a6', '\u00a6', '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0086',
            '\u00a5', '\u00b3', '\u00a5', '\u00a5', '\u00a6', '\u00a6', '\u0088', 'x', '\u0088',
            'x', '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u00a6', '\u0096',
            '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u00a5',
            '\u00b4', '\u00a5', '\u00a6', '\u0097', '\u0087', '\u0087', 'x', '\u0087', '\u0096',
            '\u00a5', '\u00c3', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087', '\u0088',
            '\u0088', 'x', '\u0087', '\u0086', '\u00a5', '\u00b3', '\u00a5', '\u00a5', '\u00a6',
            '\u00a6', '\u0088', '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5', '\u00b4',
            '\u0096', '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', 'x', 'x', '\u0087',
            '\u0087', '\u0095', '\u00b4', '\u00a5', '\u00b4', '\u00a5', '\u00a6', '\u0097',
            '\u0087', '\u0087', 'x', '\u0087', '\u0096', '\u00a5', '\u00c3', '\u00a5', '\u00b5',
            '\u00a5', '\u00a6', '\u0087', '\u0088', '\u0087', 'x', '\u0087', '\u0086', '\u00a5',
            '\u00b3', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0088', '\u0088', '\u0088', 'x',
            '\u0087', '\u0087', '\u00a5', '\u00b4', '\u0096', '\u00a5', '\u00a6', '\u0096',
            '\u0088', '\u0088', 'x', 'x', '\u0087', '\u0087', '\u0095', '\u00b4', '\u00a5',
            '\u00b4', '\u00a5', '\u00a6', '\u0097', '\u0087', '\u0087', '\u0088', '\u0087',
            '\u0096', '\u00a5', '\u00c3', '\u00a5', '\u00b4', '\u00a5', '\u00a6', '\u0087',
            '\u0088', '\u0087', 'x', '\u0087', '\u0086', '\u00a5', '\u00b3', '\u00a5', '\u00b5',
            '\u00a6', '\u00a6', '\u0087', '\u0088', '\u0088', 'x', '\u0087', '\u0087', '\u00a5',
            '\u00b4', '\u0096', '\u00a5', '\u00a6', '\u0096', '\u0088', '\u0088', 'x', 'x',
            '\u0087', '\u0087', '\u0095', '\u00b4', '\u00a5', '\u00b4', '\u00a5', '\u00a5',
            '\u0097', '\u0087', '\u0087', '\u0088', '\u0086', '\u0096', '\u00a4', '\u00c3',
            '\u00a5', '\u00a5', '\u00a5', '\u00a6', '\u0097', '\u0087', '\u0087', 'x', '\u0087',
            '\u0086', '\u00a5', '\u00c3', '\u00a5', '\u00b5', '\u00a6', '\u00a6', '\u0087',
            '\u0088', 'x', 'x', '\u0087', '\u0087'
    };

    public static final long[] calLunar(int y, int m, int d) {
        long[] lunar_date = new long[7];
        int temp = 0;
        long offset = (long) getDayOffset(y, m, d);
        lunar_date[5] = 40L + offset;
        lunar_date[4] = 14L;

        int var9;
        for (var9 = MIN_LUNAR_YEAR; var9 < MAX_LUNAR_YEAR && offset > 0L; ++var9) {
            temp = yrDays(var9);
            offset -= (long) temp;
            lunar_date[4] += 12L;
        }

        if (offset < 0L) {
            offset += (long) temp;
            --var9;
            lunar_date[4] -= 12L;
        }

        lunar_date[0] = (long) var9;
        lunar_date[3] = (long) (var9 - 1864);
        int var10 = rMonth(var9);
        lunar_date[6] = 0L;

        int var11;
        for (var11 = 1; var11 < 13 && offset > 0L; ++var11) {
            if (var10 > 0 && var11 == var10 + 1 && lunar_date[6] == 0L) {
                --var11;
                lunar_date[6] = 1L;
                temp = rMthDays((int) lunar_date[0]);
            } else {
                temp = mthDays((int) lunar_date[0], var11);
            }

            if (lunar_date[6] == 1L && var11 == var10 + 1) {
                lunar_date[6] = 0L;
            }

            offset -= (long) temp;
            if (lunar_date[6] == 0L) {
                ++lunar_date[4];
            }
        }

        if (offset == 0L && var10 > 0 && var11 == var10 + 1) {
            if (lunar_date[6] == 1L) {
                lunar_date[6] = 0L;
            } else {
                lunar_date[6] = 1L;
                --var11;
                --lunar_date[4];
            }
        }

        if (offset < 0L) {
            offset += (long) temp;
            --var11;
            --lunar_date[4];
        }

        lunar_date[1] = (long) var11;
        lunar_date[2] = 1L + offset;
        return lunar_date;
    }

    public static String formatLunarDate(int year, int monthOfYear, int dayOfMonth) {
        StringBuilder sb = new StringBuilder();
        if (year > 0) {
            sb.append(year);
            sb.append("-");
        }

        sb.append(monthOfYear + 1);
        sb.append("-");
        sb.append(dayOfMonth);
        return sb.toString();
    }

    private static final int getDayOffset(int y, int m, int d) {
        int dayOffset = 0;
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        cal.clear();

        for (int i = MIN_LUNAR_YEAR; i < y; ++i) {
            if (cal.isLeapYear(i)) {
                dayOffset += 366;
            } else {
                dayOffset += 365;
            }
        }

        cal.set(y, m, d);
        int var6 = dayOffset + cal.get(6);
        cal.set(MIN_LUNAR_YEAR, 0, 31);
        return var6 - cal.get(6);
    }

    public static final String getDayString(Resources res, int day) {
        String a = "";
        if (day == 10) {
            return res.getString(R.string.lunar_chu_shi);
        } else if (day == 20) {
            return res.getString(R.string.lunar_er_shi);
        } else if (day == 30) {
            return res.getString(R.string.lunar_san_shi);
        } else {
            int two = day / 10;
            if (two == 0) {
                a = res.getString(R.string.lunar_chu);
            }

            if (two == 1) {
                a = res.getString(R.string.lunar_shi);
            }

            if (two == 2) {
                a = res.getString(R.string.lunar_nian);
            }

            if (two == 3) {
                a = res.getString(R.string.lunar_san);
            }

            int one = day % 10;
            switch (one) {
                case 1:
                    a = a + res.getString(R.string.lunar_yi);
                    break;
                case 2:
                    a = a + res.getString(R.string.lunar_er);
                    break;
                case 3:
                    a = a + res.getString(R.string.lunar_san);
                    break;
                case 4:
                    a = a + res.getString(R.string.lunar_si);
                    break;
                case 5:
                    a = a + res.getString(R.string.lunar_wu);
                    break;
                case 6:
                    a = a + res.getString(R.string.lunar_liu);
                    break;
                case 7:
                    a = a + res.getString(R.string.lunar_qi);
                    break;
                case 8:
                    a = a + res.getString(R.string.lunar_ba);
                    break;
                case 9:
                    a = a + res.getString(R.string.lunar_jiu);
            }

            return a;
        }
    }

    private static String getDigitString(Resources res, int digit) {
        switch (digit) {
            case 0:
                return res.getString(R.string.lunar_ling);
            case 1:
                return res.getString(R.string.lunar_yi);
            case 2:
                return res.getString(R.string.lunar_er);
            case 3:
                return res.getString(R.string.lunar_san);
            case 4:
                return res.getString(R.string.lunar_si);
            case 5:
                return res.getString(R.string.lunar_wu);
            case 6:
                return res.getString(R.string.lunar_liu);
            case 7:
                return res.getString(R.string.lunar_qi);
            case 8:
                return res.getString(R.string.lunar_ba);
            case 9:
                return res.getString(R.string.lunar_jiu);
            default:
                return "";
        }
    }

    public static String getHoliday(Resources res, long[] luna, Calendar c, String countryOfLocale) {
        return null;
    }

    public static int[][] getLunarBirthdays(int lunarYear, int lunarMonth, int lunarDay) {
        int var3 = lunarMonth + 1;
        int[][] results;
        if (var3 > 12) {
            if (var3 - 12 == rMonth(lunarYear)) {
                results = new int[2][];
                int day1 = Math.min(rMthDays(lunarYear), lunarDay);
                int[] lunarParts1 = lunarToSolar(lunarYear, var3, day1);
                int[] var12 = new int[] {
                        lunarParts1[0], -1 + lunarParts1[1], lunarParts1[2], 0
                };
                byte var13;
                if (day1 == lunarDay) {
                    var13 = 0;
                } else {
                    var13 = 1;
                }

                var12[3] = var13;
                results[0] = var12;
            } else {
                results = new int[1][];
            }

            var3 -= 12;
        } else {
            results = new int[1][];
        }

        int day = Math.min(mthDays(lunarYear, var3), lunarDay);
        int[] lunarParts = lunarToSolar(lunarYear, var3, day);
        int var7 = -1 + results.length;
        int[] var8 = new int[] {
                lunarParts[0], -1 + lunarParts[1], lunarParts[2], 0
        };
        byte var9 = 0;
        if (day != lunarDay) {
            var9 = 1;
        }

        var8[3] = var9;
        results[var7] = var8;
        return results;
    }

    private static int getLunarNewYearOffsetDays(int y, int m, int d) {
        int iLeapMonth = rMonth(y);
        int var5 = 0;
        if (iLeapMonth > 0) {
            int var7 = m - 12;
            var5 = 0;
            if (iLeapMonth == var7) {
                m = iLeapMonth;
                var5 = 0 + mthDays(y, iLeapMonth);
            }
        }

        for (int i = 1; i < m; ++i) {
            var5 += mthDays(y, i);
            if (i == iLeapMonth) {
                var5 += rMthDays(y);
            }
        }

        return var5 + (d - 1);
    }

    public static String getLunarString(Resources res, int year, int month, int day) {
        StringBuilder sb = new StringBuilder();
        if (year > 0) {
            sb.append(Integer.toString(year)).append(res.getString(R.string.lunar_year));
        }

        if (month >= 12) {
            sb.append(res.getString(R.string.lunar_leap));
            month -= 12;
        }

        sb.append(getMonthString(res, month + 1));
        sb.append(res.getString(R.string.lunar_yue));
        sb.append(getDayString(res, day));
        return sb.toString();
    }

    public static String getMonthString(Resources res, int k) {
        if (k > 12) {
            return null;
        } else {
            switch (k) {
                case 0:
                    return "";
                case 1:
                    return res.getString(R.string.lunar_yi);
                case 2:
                    return res.getString(R.string.lunar_er);
                case 3:
                    return res.getString(R.string.lunar_san);
                case 4:
                    return res.getString(R.string.lunar_si);
                case 5:
                    return res.getString(R.string.lunar_wu);
                case 6:
                    return res.getString(R.string.lunar_liu);
                case 7:
                    return res.getString(R.string.lunar_qi);
                case 8:
                    return res.getString(R.string.lunar_ba);
                case 9:
                    return res.getString(R.string.lunar_jiu);
                case 10:
                    return res.getString(R.string.lunar_shi);
                case 11:
                    return res.getString(R.string.lunar_shi_yi);
                case 12:
                    return res.getString(R.string.lunar_shi_er);
                default:
                    return null;
            }
        }
    }

    public static long getNextLunarBirthday(int lunarMonth, int lunarDay) {
        Time time = new Time();
        time.setToNow();
        long[] lunarParts = calLunar(time.year, time.month, time.monthDay);
        int lunarYear = (int) lunarParts[0];
        time.second = 0;
        time.minute = 0;
        time.hour = 0;
        long timeNow = time.normalize(false);

        long timeResult;
        for (timeResult = Long.MAX_VALUE; Long.MAX_VALUE == timeResult
                && lunarYear >= MIN_LUNAR_YEAR && lunarYear < MAX_LUNAR_YEAR; ++lunarYear) {
            int[][] birthdays = getLunarBirthdays(lunarYear, lunarMonth, lunarDay);
            for (int i = 0, len = birthdays.length; i < len; ++i) {
                int[] birthday = birthdays[i];
                time.set(birthday[2], birthday[1], birthday[0]);
                long timeInMillis = time.normalize(false);
                if (timeInMillis >= timeNow) {
                    timeResult = Math.min(timeResult, timeInMillis);
                }
            }
        }

        if (Long.MAX_VALUE == timeResult) {
            timeResult = 0;
        }

        return timeResult;
    }

    public static String getSolarTerm(Resources res, Calendar c) {
        int year = c.get(1);
        int month = c.get(2);
        int day = c.get(5);
        char term = solarTermsTable[month + 12 * (year - 1901)];
        int termDay = 15 + term % 16;
        return day == termDay ? res.getString(solarTerms[1 + month * 2])
                : (day == 15 - term / 16 ? res.getString(solarTerms[month * 2]) : null);
    }

    static int getSolarYearMonthDays(int iYear, int iMonth) {
        return iMonth != 1 && iMonth != 3 && iMonth != 5 && iMonth != 7 && iMonth != 8
                && iMonth != 10 && iMonth != 12 ? (iMonth != 4 && iMonth != 6 && iMonth != 9
                && iMonth != 11 ? (iMonth == 2 ? (isSolarLeapYear(iYear) ? 29 : 28) : 0) : 30) : 31;
    }

    public static String getString(Resources res, Calendar c) {
        return solar2lunar(res, c.get(1), c.get(2), c.get(5));
    }

    public static String getYearString(Resources res, int year) {
        StringBuffer sb = new StringBuffer();
        do {
            year /= 10;
            sb.insert(0, getDigitString(res, year % 10));
        } while (year > 0);

        return sb.toString();
    }

    static boolean isSolarLeapYear(int iYear) {
        return iYear % 4 == 0 && iYear % 100 != 0 || iYear % 400 == 0;
    }

    public static int[] lunarToSolar(int y, int m, int d) {
        int[] solar_date = new int[3];
        int iOffsetDays = getLunarNewYearOffsetDays(y, m, d) + iSolarLunarOffsetTable[y - MIN_LUNAR_YEAR + 1];
        short iYearDays;
        if (isSolarLeapYear(y)) {
            iYearDays = 366;
        } else {
            iYearDays = 365;
        }

        int iSYear;
        if (iOffsetDays >= iYearDays) {
            iSYear = y + 1;
            iOffsetDays -= iYearDays;
        } else {
            iSYear = y;
        }

        int iSDay = iOffsetDays + 1;

        int iSMonth;
        for (iSMonth = 1; iOffsetDays >= 0; ++iSMonth) {
            iSDay = iOffsetDays + 1;
            iOffsetDays -= getSolarYearMonthDays(iSYear, iSMonth);
        }

        int var9 = iSMonth - 1;
        solar_date[0] = iSYear;
        solar_date[1] = var9;
        solar_date[2] = iSDay;
        return solar_date;
    }

    public static final int mthDays(int y, int m) {
        return (luYearData[y - MIN_LUNAR_YEAR] & (long) (65536 >> m)) == 0L ? 29 : 30;
    }

    public static int[] parseLunarDate(String lunarDate) {
        if (TextUtils.isEmpty(lunarDate)) {
            return null;
        } else {
            int[] result = new int[3];

            try {
                String[] date = lunarDate.split("-");
                if (date.length == 2) {
                    result[0] = Integer.parseInt(date[1].trim());
                    result[1] = -1 + Integer.parseInt(date[0].trim());
                    result[2] = 0;
                    return result;
                } else if (date.length == 3) {
                    result[0] = Integer.parseInt(date[2].trim());
                    result[1] = -1 + Integer.parseInt(date[1].trim());
                    result[2] = Integer.parseInt(date[0].trim());
                    return result;
                } else {
                    return null;
                }
            } catch (NumberFormatException var4) {
                var4.printStackTrace();
                return null;
            }
        }
    }

    public static final int rMonth(int y) {
        return (int) (15L & luYearData[y - MIN_LUNAR_YEAR]);
    }

    public static final int rMthDays(int y) {
        return rMonth(y) != 0 ? ((65536L & luYearData[y - MIN_LUNAR_YEAR]) != 0L ? 30 : 29) : 0;
    }

    public static String solar2lunar(Resources res, int year, int month, int day) {
        long[] l = calLunar(year, month, day);
        StringBuffer sLunar = new StringBuffer();
        if (l[6] == 1) {
            sLunar.append(res.getString(R.string.lunar_leap));
        }

        sLunar.append(getMonthString(res, (int) l[1]));
        sLunar.append(res.getString(R.string.lunar_yue));
        sLunar.append(getDayString(res, (int) l[2]));
        return sLunar.toString();

    }

    private static final int yrDays(int y) {
        int sum = 348;

        for (int i = '\u8000'; i > 8; i >>= 1) {
            if ((luYearData[y - MIN_LUNAR_YEAR] & (long) i) != 0) {
                ++sum;
            }
        }

        return sum + rMthDays(y);
    }
}
