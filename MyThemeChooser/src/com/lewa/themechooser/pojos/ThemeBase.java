package com.lewa.themechooser.pojos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import util.ThemeUtil;

public class ThemeBase implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 2866915953243943771L;
    public String thumbnailpath;
    public List<String> previewpath = new ArrayList<String>();
    public String attachment;
    private String cnName;
    private String enName;
    private String cnAuthor;
    private String enAuthor;
    private String packageName;
    private String themeId;
    private String isApplied;
    private String internal_version;
    /**
     * 600K or 2.5M
     */
    private String size;
    private String version;
    /**
     * such as :default.lwt or a.jpg
     */
    private String pkg;
    /**
     * long length = file.length();
     */
    private Long length;
    private String createDate;
    /**
     * 相对于在线主题中模块数量而言
     */

    private String modelNum;
    /**
     * 相对于本地主题中模块数量而言
     */

    private ThemeModelInfo themeModelInfo;
    private boolean containLockScreen = false;
    /**
     * lwt源文件路径,如果lwtPath为空，则说明此lwt文件已经在/theme/lwt下,否则不是
     */
    private String lwtPath;
    private String id;
    private String downloads;

    // Begin, added by yljiang@lewatek.com 2013-11-19   ,new Mechanism is version>0

    private int mMechanismVersion = 0;

    public ThemeBase() {

    }

    public ThemeBase(ThemeModelInfo themeModelInfo, String pkg, String lwtPath,
                     boolean parseJson) {
        this.pkg = pkg.trim();
        this.themeModelInfo = themeModelInfo;
        this.lwtPath = lwtPath;
    }

    // End

    public int getmMechanismVersion() {
        return mMechanismVersion;
    }

    public void setmMechanismVersion(int mMechanismVersion) {
        this.mMechanismVersion = mMechanismVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Only meaningfull to wallpapers
    public String getCombinedName() {
        return cnName + "___" + enName + ".jpg";
    }

    public String getCnName() {
        return cnName;
    }

    public void setCnName(String cnName) {
        this.cnName = cnName.trim();
    }

    public String getNameByLocale() {
        if (Locale.getDefault().getLanguage().equals("zh")) {
            return getCnName();
        } else {
            return getEnName();
        }
    }

    public int getVersionCode() {
        try {
            return Integer.valueOf(internal_version);
        } catch (Exception e) {
        }
        return 0;
    }

    public String getInternal_version() {
        return internal_version;
    }

    public void setInternal_version(String internal_version) {
        this.internal_version = internal_version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName.trim();
    }

    public String getAuthor() {
        if (Locale.getDefault().getLanguage().equals("zh")) {
            return getCnAuthor();
        } else {
            return getEnAuthor();
        }
    }

    public String getCnAuthor() {
        return cnAuthor;
    }

    public void setCnAuthor(String cnAuthor) {
        this.cnAuthor = cnAuthor;
    }

    public String getEnAuthor() {
        return enAuthor;
    }

    public void setEnAuthor(String enAuthor) {
        this.enAuthor = enAuthor;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg.trim();
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public String getDownloads() {
        return downloads;
    }

    public void setDownloads(String downloads) {
        this.downloads = downloads;
    }

    /**
     * such as:default or a
     */
    public String getName() {

        return ThemeUtil.getNameNoBuffix(pkg);
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getModelNum() {
        return modelNum;
    }

    public void setModelNum(String modelNum) {
        this.modelNum = modelNum;
    }

    public ThemeModelInfo getThemeModelInfo() {
        return themeModelInfo;
    }

    public void setThemeModelInfo(ThemeModelInfo themeModelInfo) {
        this.themeModelInfo = themeModelInfo;
    }

    public boolean getContainLockScreen() {

        return containLockScreen;
    }

    public void setContainLockScreen(boolean containLockScreen) {
        this.containLockScreen = containLockScreen;
    }

    public String getLwtPath() {
        return lwtPath;
    }

    public void setLwtPath(String lwtPath) {
        this.lwtPath = lwtPath;
    }

    @Override
    public String toString() {
        return "{ cnName = " + cnName + ", enName = " + enName
                + "thumbnailpath=" + thumbnailpath
                + ", size = " + size + ", version = " + version + ", pkg = "
                + pkg + ", length = " + length
                + ", modelNum = " + modelNum + ", lwtPath = " + lwtPath + "size=" + size
                + ", themeId=" + themeId + ", packageName = " + packageName + " }";
    }

}
