package com.lewa.themechooser.newmechanism;

import java.util.ArrayList;
import java.util.Locale;

/**
 * ThemeDescription.java:
 *
 * @author yljiang@lewatek.com 2013-11-26
 */
public class ThemeDescription {

    private int versionCode;
    private int mechanismVersion = 1;
    private long fileSize;
    private String versionName;
    private String packageName;
    private String downloadUrl;
    private String thumbnailUrl;
    private String zipName;
    private ArrayList<String> applyPackages;
    private ArrayList<ThemeName> themeNames;
    private String wallpaperUrl;
    private String lockWallpaperUrl;
    private String bootAnimationUri;
    private String iconUrl;
    private String frontUrl;
    private String lockscreenUrl;

    private String inCallStyleUrl;
    private String inCallRingtoneUrl;
    private String messageRingtoneUrl;
    private String notifRingtoneUrl;


    public String getNotifRingtoneUrl() {
        return notifRingtoneUrl;
    }

    public void setNotifRingtoneUrl(String notifRingtoneUrl) {
        this.notifRingtoneUrl = notifRingtoneUrl;
    }

    public String getInCallRingtoneUrl() {
        return inCallRingtoneUrl;
    }

    public void setInCallRingtoneUrl(String inCallRingtoneUrl) {
        this.inCallRingtoneUrl = inCallRingtoneUrl;
    }

    public String getMessageRingtoneUrl() {
        return messageRingtoneUrl;
    }

    public void setMessageRingtoneUrl(String messageRingtoneUrl) {
        this.messageRingtoneUrl = messageRingtoneUrl;
    }


    public String getInCallStyleUrl() {
        return inCallStyleUrl;
    }

    public void setInCallStyleUrl(String inCallStyleUrl) {
        this.inCallStyleUrl = inCallStyleUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getFrontUrl() {
        return frontUrl;
    }

    public void setFrontUrl(String frontUrl) {
        this.frontUrl = frontUrl;
    }


    public String getLockscreenUrl() {
        return lockscreenUrl;
    }

    public void setLockscreenUrl(String lockscreenUrl) {
        this.lockscreenUrl = lockscreenUrl;
    }

    public String getBootAnimationUri() {
        return bootAnimationUri;
    }

    public void setBootAnimationUri(String bootAnimationUri) {
        this.bootAnimationUri = bootAnimationUri;
    }

    public void setThemeNames(ArrayList<ThemeName> themeNames) {
        this.themeNames = themeNames;
    }

    public String getName() {
        ThemeName themeName = getThemeName();
        if (themeName != null) {
            return themeName.getName();
        }
        return null;
    }

    public String getAuthor() {
        ThemeName themeName = getThemeName();
        if (themeName != null) {
            return themeName.getAuthor();
        }
        return null;
    }

    private ThemeName getThemeName() {
        if (themeNames == null || themeNames.size() <= 0)
            return null;
        final String language = Locale.getDefault().getLanguage();
        for (ThemeName themeName : themeNames) {
            if (language.equals(themeName.getLanguage())) {
                return themeName;
            }
        }
        return themeNames.get(0);
    }

    public ArrayList<String> getApplyPackages() {
        return applyPackages;
    }

    public void setApplyPackages(ArrayList<String> applyPackages) {
        this.applyPackages = applyPackages;
    }

    public String getApplyPackagesStr() {
        if (applyPackages == null || applyPackages.size() <= 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String str : applyPackages) {
            builder.append(str).append(Globals.SPLIT);
        }
        return builder.toString();
    }

    public ArrayList<String> getApplyPackagesByStr(String strs) {
        if (NewMechanismUtils.isBlankStr(strs))
            return null;
        ArrayList<String> arrayList = new ArrayList<String>();
        String[] strings = strs.split(Globals.SPLIT);
        for (String str : strings) {
            if (str.startsWith(Globals.OTHER)) {
                arrayList.add(str);
            }
        }
        return arrayList;
    }

    public String getZipName() {
        return zipName;
    }

    public void setZipName(String zipName) {
        this.zipName = zipName;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }


    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getWallpaperUrl() {
        return wallpaperUrl;
    }

    public void setWallpaperUrl(String wallpaperUrl) {
        this.wallpaperUrl = wallpaperUrl;
    }

    public String getLockWallpaperUrl() {
        return lockWallpaperUrl;
    }

    public void setLockWallpaperUrl(String lockWallpaperUrl) {
        this.lockWallpaperUrl = lockWallpaperUrl;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getMechanismVersion() {
        return mechanismVersion;
    }

    public void setMechanismVersion(int mechanismVersion) {
        this.mechanismVersion = mechanismVersion;
    }

    public final class ThemeName {

        private String language;
        private String name;
        private String author;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

    }

}
