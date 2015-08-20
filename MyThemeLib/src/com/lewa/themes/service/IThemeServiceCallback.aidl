package com.lewa.themes.service;

import android.graphics.Bitmap;
/**
 * Created by ivonhoe on 15-3-13.
 */
interface IThemeServiceCallback {
void onThemeChanged();
void onWallpaperChanged(in Bitmap blurredWallpaper);
}
