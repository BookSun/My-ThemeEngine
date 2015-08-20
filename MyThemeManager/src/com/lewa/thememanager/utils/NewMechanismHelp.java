package com.lewa.thememanager.utils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import lewa.os.FileUtilities;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import lewa.provider.ExtraSettings;
import android.os.SystemProperties;
import android.util.Log;
import com.lewa.thememanager.Constants;
import com.lewa.themes.ThemeManager;
import com.lewa.themes.provider.ThemeItem;

public class NewMechanismHelp {

    private static final String TAG   = "JYL";
    public static  final String SPLIT = ThemeManager.SPLIT ;
    public static  final String OTHER = ThemeManager.OTHER ;
    public static  final String FRAMEWORK = ThemeManager.FRAMEWORK;
    public static  final String LEWA = ThemeManager.LEWA;
    public static  final String PROVIDER_PATH = "content://com.lewa.themechooser.fileprovider" ;

    public static boolean isNewMechanism(ThemeItem item) {
        if (item != null && item.getMechanismVersion() > 0) {
            return true;
        }
        return false;
    }

    public static Uri makeUri( String name) {
        String path =  PROVIDER_PATH+"/"+name;
        return Uri.parse(path);
    }

    public static void applyTheme(Context context, ThemeItem theme ,Intent request) {
        if(isNewMechanism(theme)) {
            copyOtherPackages(context,theme);
        } else {
            deleteOtherPackages(context,theme);
        }
    }

    private static final String LWT = ".lwt" ;
    private static void copyOtherPackages(Context context, ThemeItem theme) {
        String packages = theme.getApplyPackages();
        if (packages != null && packages.length() > 0) {
            int index= packages.indexOf(LWT);
            if(index >0){
                int end = LWT.length() +index ;
                String lwtPath = packages.substring(0, end )+SPLIT;
                String other = packages.substring(end);
                String[] strings =  other.split(SPLIT);
                if (strings.length > 0) {
                    for (String str : strings) {
                        if (str.startsWith(OTHER) ||  FRAMEWORK.equals(str) ||  LEWA.equals(str)) {
                            copyPackage(context, theme, lwtPath+str,str);
                        }
                    }
                }
            }
        }
    }

    private static void copyPackage(Context context, ThemeItem theme, String urlString ,String packageName) {
        InputStream in = null;
        OutputStream out = null;
        File outFile=new File(ThemeManager.THEME_ELEMENTS_PATH ,packageName);
        if (outFile.exists()) {
            outFile.delete();
        }
        try {
            outFile.createNewFile() ;
            in = context.getContentResolver().openInputStream(makeUri(urlString));
            out = FileUtilities.openOutputStream(outFile);
            FileUtilities.connectIO(in, out);
            FileUtilities.setPermissions(outFile, "755");
        } catch (Exception e) {
            e.printStackTrace();
        } 
        finally {
            if (in != null) {
                FileUtilities.close(in);
            }
            if (null != out) {
                FileUtilities.close(out);
            } 
        }
    }

    private static void deleteOtherPackages(Context context, ThemeItem theme) {
        File dirFile = new File(ThemeManager.THEME_ELEMENTS_PATH);
        if(dirFile.exists() && dirFile.isDirectory()) {
            File[] files = dirFile.listFiles();
            for(File file:files) {
                String name = file.getName() ;
                if (name.startsWith(OTHER) ||  FRAMEWORK.equals(name) ||  LEWA.equals(name) ) {
                    deleteFile(file);
                }
            }
        }
    }

    public static void deleteFile(File file) {
        if(file.isFile() && file.exists()) {
            file.delete();
        }
    }

    public static void applyInCallStyle(Context context, ThemeItem item, Intent request ,boolean sendBroadcast) {
        if(isNewMechanism(item)) {
            String themeType = request.getType();
            Uri uri = (Uri) request.getParcelableExtra(ThemeManager.EXTRA_INCALLSTYLE_URI);
            if (null == uri) {
                uri = item.getInCallStyleUri();
            }
            if (null != uri) {
                setInCallStyle(context,uri);
                if(sendBroadcast) {
                    ThemeUtilities.applyCustom(context, item,false,false);
                    context.sendBroadcast(new Intent(ThemeManager.ACTION_THEME_CHANGED).setDataAndType(item.getUri(context), themeType));
                }
            }
        }
    }
    
    private static void setInCallStyle(Context context ,Uri uri) {
        if (null == uri) {
            return ;
        }
        InputStream in = null;
        OutputStream out = null;
        File outFile= new File(ThemeManager.THEME_ELEMENT_INCALLSYLE);
        if (outFile.exists()) {
            outFile.delete();
        }
        try {
            outFile.createNewFile() ;
            in = context.getContentResolver().openInputStream(uri);
            out = FileUtilities.openOutputStream(outFile);
            FileUtilities.connectIO(in, out);
            FileUtilities.setPermissions(outFile, "755");
        } catch (Exception e) {
            e.printStackTrace();
        } 
        finally {
            if (in != null) {
                FileUtilities.close(in);
            }
            if (null != out) {
                FileUtilities.close(out);
            } 
        }
        try {
            android.os.SystemProperties.set("sys.lewa.themeChanged", String.valueOf(android.os.SystemClock.elapsedRealtime() / 1000));
            Settings.System.putInt(context.getContentResolver(), ExtraSettings.System.INCALLSTYLE_CHANGED, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
