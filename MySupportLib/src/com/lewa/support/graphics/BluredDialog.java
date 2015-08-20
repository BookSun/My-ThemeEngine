package com.lewa.support.graphics;

import com.lewa.support.util.ImageUtils;
import com.lewa.support.util.SurfaceWrapper;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

public class BluredDialog {

    private BluredDialog(Context context){
    }

    public static Dialog showBlured(Dialog dialog) {
        setParam(dialog);
        dialog.show();
        return dialog;
    }

    private static void setParam(Dialog dialog) {
        Context context = dialog.getContext();
        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        @SuppressWarnings("deprecation")
        Bitmap shot = SurfaceWrapper.screenshot(Math.min(display.getWidth(), display.getHeight()) / 3,
                                                Math.max(display.getWidth(), display.getHeight()) / 3);
        if (shot != null && shot.getWidth() > 1) {
            Bitmap blur = Bitmap.createBitmap(shot.getWidth(), shot.getHeight(), Bitmap.Config.ARGB_8888);
            blur.eraseColor(0xff000000);
            ImageUtils.fastBlur(shot, blur, 5);
            shot.recycle();
            int rotation = display.getRotation();
            if (rotation != Surface.ROTATION_0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(360 - rotation * 90);
                blur = Bitmap.createBitmap(blur, 0, 0, blur.getWidth(), blur.getHeight(), matrix, false);
            }
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new LayerDrawable(new Drawable[] {
                    new BitmapDrawable(context.getResources(), blur), new ColorDrawable(0xaf000000) }));
            window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.setGravity(Gravity.CENTER);
        }
    }

}
