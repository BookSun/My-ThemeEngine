package com.lewa.themechooser.pojos;

import com.lewa.themechooser.ThemeConstants;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ThemeModelInfo implements Serializable {


    /**
     *
     */
    private static final long serialVersionUID = 4047148437084588586L;

    private String lockscreenWallpaper;
    private String wallpaper;
    private String lockscreen;
    private String icons;
    private String launcher;
    private String boots;
    private String fonts;

    private String containModelNum;

    private String pkg;

    private ArrayList<Integer> modelNames = new ArrayList<Integer>();
    private ArrayList<String> modelFiles = new ArrayList<String>();

    public ThemeModelInfo(String pkg) {
        this.pkg = pkg;

        parseTheme(null);
    }

    public String getLockscreenWallpaper() {
        return lockscreenWallpaper;
    }

    public void setLockscreenWallpaper(String lockscreenWallpaper) {
        this.lockscreenWallpaper = lockscreenWallpaper;
    }

    public String getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(String wallpaper) {
        this.wallpaper = wallpaper;
    }

    public String getIcons() {
        return icons;
    }

    public void setIcons(String icons) {
        this.icons = icons;
    }

    public String getLauncher() {
        return launcher;
    }

    public void setLauncher(String launcher) {
        this.launcher = launcher;
    }

    public String getBoots() {
        return boots;
    }

    public void setBoots(String boots) {
        this.boots = boots;
    }

    public String getFonts() {
        return fonts;
    }

    public void setFonts(String fonts) {
        this.fonts = fonts;
    }

    public String getLockscreen() {
        return lockscreen;
    }

    public void setLockscreen(String lockscreen) {
        this.lockscreen = lockscreen;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getContainModelNum() {
        return containModelNum;
    }

    public void setContainModelNum(String containModelNum) {
        this.containModelNum = containModelNum;
    }

    public ArrayList<Integer> getModelNames() {
        return modelNames;
    }

    public void setModelNames(ArrayList<Integer> modelNames) {
        this.modelNames = modelNames;
    }

    public ArrayList<String> getModelFiles() {
        return modelFiles;
    }

    public void setModelFiles(ArrayList<String> modelFiles) {
        this.modelFiles = modelFiles;
    }

    /**
     * �涨���pathΪ�գ����ʾ�����Ϊtheme/lwt/��
     * ���path��Ϊ�գ����ʾ�����Ϊ����λ��
     *
     * @param path
     */
    public void parseTheme(String path) {

        if (pkg == null) {
            return;
        }

        File themeFile = null;

        if (path == null) {
            themeFile = new File(new StringBuilder().append(ThemeConstants.THEME_LWT).append("/").append(pkg).toString());
        } else {
            themeFile = new File(path);
        }

        if (!themeFile.exists()) {
            return;
        }

        ZipFile themeZip = null;
//        try {
//            themeZip = new ZipFile(themeFile);
//            int modleNum = 0;
//            if(containModel(themeZip, ThemeConstants.THEME_MODEL_LOCKSCREEN_WALLPAPER)){
//                lockscreenWallpaper = ThemeConstants.THEME_MODEL_LOCKSCREEN_WALLPAPER;
//                modelFiles.add(ThemeConstants.THEME_MODEL_LOCKSCREEN_WALLPAPER);
//                modelNames.add(R.string.theme_model_lockscreen_wallpaper);
//                ++modleNum;
//            }
//
//            if(containModel(themeZip, ThemeConstants.THEME_MODEL_WALLPAPER)){
//                wallpaper = ThemeConstants.THEME_MODEL_WALLPAPER;
//                modelFiles.add(ThemeConstants.THEME_MODEL_WALLPAPER);
//                modelNames.add(R.string.theme_model_wallpaper);
//                ++modleNum;
//            }
//
//            if(containModel(themeZip, ThemeConstants.THEME_MODEL_LOCKSCREEN)){
//                lockscreen = ThemeConstants.THEME_MODEL_LOCKSCREEN;
//                modelFiles.add(ThemeConstants.THEME_MODEL_LOCKSCREEN);
//                modelNames.add(R.string.theme_model_lockscreen_style);
//                ++modleNum;
//            }
//
//            if(containModel(themeZip, ThemeConstants.THEME_MODEL_ICONS)){
//                icons = ThemeConstants.THEME_MODEL_ICONS;
//                modelFiles.add(ThemeConstants.THEME_MODEL_ICONS);
//                modelNames.add(R.string.theme_model_icon_style);
//                ++modleNum;
//            }
//
//            if(containModel(themeZip, ThemeConstants.THEME_MODEL_LAUNCHER)){
//                launcher = ThemeConstants.THEME_MODEL_LAUNCHER;
//                modelFiles.add(ThemeConstants.THEME_MODEL_LAUNCHER);
//                modelNames.add(R.string.theme_model_launcher);
//                ++modleNum;
//            }
//
//            if(containModel(themeZip, ThemeConstants.THEME_MODEL_BOOTS)){
//                boots = ThemeConstants.THEME_MODEL_BOOTS;
//                modelFiles.add(ThemeConstants.THEME_MODEL_BOOTS);
//                modelNames.add(R.string.theme_model_boots);
//                ++modleNum;
//            }
//
//            if(containModel(themeZip, ThemeConstants.THEME_MODEL_FONTS)){
//                fonts = ThemeConstants.THEME_MODEL_FONTS;
//                modelFiles.add(ThemeConstants.THEME_MODEL_FONTS);
//                modelNames.add(R.string.theme_model_fonts);
//                ++modleNum;
//            }
//
//            containModelNum = String.valueOf(modleNum);
//
//        } catch (Exception e) {
        // TODO Auto-generated catch block
//            e.printStackTrace();
//        }finally{
//            try{
//                if(themeZip != null){
//                    themeZip.close();
//                    themeZip = null;
//                }
//            }catch(Exception e){
//                e.printStackTrace();
//            }
//
//        }
    }

    private boolean containModel(ZipFile themeZip, String modelName) {
        ZipEntry zipEntry = themeZip.getEntry(modelName);
        if (zipEntry != null) {
            return true;
        }
        return false;
    }

}
