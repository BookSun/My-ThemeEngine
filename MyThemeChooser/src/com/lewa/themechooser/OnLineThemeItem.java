package com.lewa.themechooser;

import android.content.Context;
import android.net.Uri;

import com.lewa.themes.provider.Themes;

import java.io.Serializable;
import java.util.List;


public class OnLineThemeItem implements Serializable {
    private String theme_Package;
    private String theme_Id;
    private boolean theme_Is_Applied;
    private String theme_Author;
    private boolean theme_Is_Drm;
    private String theme_Name;
    private String theme_Style_Name;
    private String theme_Wallpaper_Name;
    private String theme_Wallpaper_Uri;
    private String theme_Lock_Wallpaper_Name;
    private String theme_Lock_Wallpaper_Uri;
    private String theme_Ringtone_Name;
    private String theme_Ringtone_Uri;
    private String theme_notif_Ringtone_Name;
    private String theme_notif_Ringtone_Uri;
    private String theme_thumbnail_Uri;
    private String theme_Preview_Uri;
    private boolean theme_Has_Host_density;
    private boolean theme_Has_Theme_Package_Scope;
    private String theme_Boot_Animation_Uri;
    private String theme_Font_Uri;
    private String theme_Lockscreen_Uri;
    private long theme_size;
    private int theme_Preview_Thmubnail_Count;
    private List<String> Theme_Preview_Thmubnail_Uri_List;


    public int getTheme_Preview_Thmubnail_Count() {
        return theme_Preview_Thmubnail_Count;
    }


    public void setTheme_Preview_Thmubnail_Count(int theme_Preview_Thmubnail_Count) {
        this.theme_Preview_Thmubnail_Count = theme_Preview_Thmubnail_Count;
    }


    public List<String> getTheme_Preview_Thmubnail_Uri_List() {
        return Theme_Preview_Thmubnail_Uri_List;
    }


    public void setTheme_Preview_Thmubnail_Uri_List(
            List<String> theme_Preview_Thmubnail_Uri_List) {
        Theme_Preview_Thmubnail_Uri_List = theme_Preview_Thmubnail_Uri_List;
    }


    public long getTheme_size() {
        return theme_size;
    }


    public void setTheme_size(long theme_size) {
        this.theme_size = theme_size;
    }


    public String getTheme_Package() {
        return theme_Package;
    }


    public void setTheme_Package(String theme_Package) {
        this.theme_Package = theme_Package;
    }


    public String getTheme_Id() {
        return theme_Id;
    }


    public void setTheme_Id(String theme_Id) {
        this.theme_Id = theme_Id;
    }


    public boolean isTheme_Is_Applied() {
        return theme_Is_Applied;
    }


    public void setTheme_Is_Applied(boolean theme_Is_Applied) {
        this.theme_Is_Applied = theme_Is_Applied;
    }


    public String getTheme_Author() {
        return theme_Author;
    }


    public void setTheme_Author(String theme_Author) {
        this.theme_Author = theme_Author;
    }


    public boolean isTheme_Is_Drm() {
        return theme_Is_Drm;
    }


    public void setTheme_Is_Drm(boolean theme_Is_Drm) {
        this.theme_Is_Drm = theme_Is_Drm;
    }


    public String getTheme_Name() {
        return theme_Name;
    }


    public void setTheme_Name(String theme_Name) {
        this.theme_Name = theme_Name;
    }


    public String getTheme_Style_Name() {
        return theme_Style_Name;
    }


    public void setTheme_Style_Name(String theme_Style_Name) {
        this.theme_Style_Name = theme_Style_Name;
    }


    public String getTheme_Wallpaper_Name() {
        return theme_Wallpaper_Name;
    }


    public void setTheme_Wallpaper_Name(String theme_Wallpaper_Name) {
        this.theme_Wallpaper_Name = theme_Wallpaper_Name;
    }


    public String getTheme_Wallpaper_Uri() {
        return theme_Wallpaper_Uri;
    }


    public void setTheme_Wallpaper_Uri(String theme_Wallpaper_Uri) {
        this.theme_Wallpaper_Uri = theme_Wallpaper_Uri;
    }


    public String getTheme_Lock_Wallpaper_Name() {
        return theme_Lock_Wallpaper_Name;
    }


    public void setTheme_Lock_Wallpaper_Name(String theme_Lock_Wallpaper_Name) {
        this.theme_Lock_Wallpaper_Name = theme_Lock_Wallpaper_Name;
    }


    public String getTheme_Lock_Wallpaper_Uri() {
        return theme_Lock_Wallpaper_Uri;
    }


    public void setTheme_Lock_Wallpaper_Uri(String theme_Lock_Wallpaper_Uri) {
        this.theme_Lock_Wallpaper_Uri = theme_Lock_Wallpaper_Uri;
    }


    public String getTheme_Ringtone_Name() {
        return theme_Ringtone_Name;
    }


    public void setTheme_Ringtone_Name(String theme_Ringtone_Name) {
        this.theme_Ringtone_Name = theme_Ringtone_Name;
    }


    public String getTheme_Ringtone_Uri() {
        return theme_Ringtone_Uri;
    }


    public void setTheme_Ringtone_Uri(String theme_Ringtone_Uri) {
        this.theme_Ringtone_Uri = theme_Ringtone_Uri;
    }


    public String getTheme_notif_Ringtone_Name() {
        return theme_notif_Ringtone_Name;
    }


    public void setTheme_notif_Ringtone_Name(String theme_notif_Ringtone_Name) {
        this.theme_notif_Ringtone_Name = theme_notif_Ringtone_Name;
    }


    public String getTheme_notif_Ringtone_Uri() {
        return theme_notif_Ringtone_Uri;
    }


    public void setTheme_notif_Ringtone_Uri(String theme_notif_Ringtone_Uri) {
        this.theme_notif_Ringtone_Uri = theme_notif_Ringtone_Uri;
    }


    public String getTheme_thumbnail_Uri() {
        return theme_thumbnail_Uri;
    }


    public void setTheme_thumbnail_Uri(String theme_thumbnail_Uri) {
        this.theme_thumbnail_Uri = theme_thumbnail_Uri;
    }


    public String getTheme_Preview_Uri() {
        return theme_Preview_Uri;
    }


    public void setTheme_Preview_Uri(String theme_Preview_Uri) {
        this.theme_Preview_Uri = theme_Preview_Uri;
    }


    public boolean isTheme_Has_Host_density() {
        return theme_Has_Host_density;
    }


    public void setTheme_Has_Host_density(boolean theme_Has_Host_density) {
        this.theme_Has_Host_density = theme_Has_Host_density;
    }


    public boolean isTheme_Has_Theme_Package_Scope() {
        return theme_Has_Theme_Package_Scope;
    }


    public void setTheme_Has_Theme_Package_Scope(
            boolean theme_Has_Theme_Package_Scope) {
        this.theme_Has_Theme_Package_Scope = theme_Has_Theme_Package_Scope;
    }


    public String getTheme_Boot_Animation_Uri() {
        return theme_Boot_Animation_Uri;
    }


    public void setTheme_Boot_Animation_Uri(String theme_Boot_Animation_Uri) {
        this.theme_Boot_Animation_Uri = theme_Boot_Animation_Uri;
    }


    public String getTheme_Font_Uri() {
        return theme_Font_Uri;
    }


    public void setTheme_Font_Uri(String theme_Font_Uri) {
        this.theme_Font_Uri = theme_Font_Uri;
    }


    public String getTheme_Lockscreen_Uri() {
        return theme_Lockscreen_Uri;
    }


    public void setTheme_Lockscreen_Uri(String theme_Lockscreen_Uri) {
        this.theme_Lockscreen_Uri = theme_Lockscreen_Uri;
    }


    public Uri getUri(Context context) {
        return Themes.getThemeUri(context, getTheme_Package(), getTheme_Id());
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
