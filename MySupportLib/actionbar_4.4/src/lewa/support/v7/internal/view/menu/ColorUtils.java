package lewa.support.v7.internal.view.menu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.Canvas;
import android.graphics.drawable.StateListDrawable;
import java.util.List;
import java.util.ArrayList;

//import lewa.util.MMCQ;
//import lewa.util.MMCQ.VBox;
//import lewa.util.LewaUiUtil;

public class ColorUtils {
    public static final int INVALID_COLOR = 0xFFFFFF;

    public static int[] color2rgb(int color) {
        int rgb[] = {getRed(color), getGreen(color), getBlue(color)};
        return rgb;
    }

    public static int[] rgb2HSB(int color) {
        int h = 0, s = 0, b = 0;
        float hsbvals[] = new float[3];
        rgbtoHSB(getRed(color), getGreen(color), getBlue(color), hsbvals);

//        System.out.println("rgb2hsb color=" + color
//                + ", colorHex=" + String.format("#%06x", color)
//                + ", red=" + c.getRed()
//                + ", green=" + c.getGreen()
//                + ", blue=" + c.getBlue()
//                + ", h=" + Math.round(hsbvals[0] * 360)
//                + ", s=" + Math.round(hsbvals[1] * 100)
//                + ", b=" + Math.round(hsbvals[2] * 100));
//        System.out.println("########");

        h = (int) Math.round(hsbvals[0] * 360);
        s = (int) Math.round(hsbvals[1] * 100);
        b = (int) Math.round(hsbvals[2] * 100);

        int hsb[] = {h, s, b};
        return hsb;
    }

    public static String[] parseHSB(String hsv) {
        if (hsv == null || hsv.isEmpty()) {
            return null;
        }

        String hsvValue[] = new String[3];
        hsvValue = hsv.split(",");

        return hsvValue;
    }

    public static double diffHsb(int hsb1[], final int hsb2[]) {
        if (hsb1 == null || hsb2 == null) {
            return 0.0;
        }
        if (hsb1.length < 3 || hsb2.length < 3) {
            return 0.0;
        }

        double h = Math.pow(hsb1[0] - hsb2[0], 2);
        double s = Math.pow(hsb1[1] - hsb2[1], 2);
        double b = Math.pow(hsb1[2] - hsb2[2], 2);

        return Math.sqrt(h + s + b);
    }

    public static int getRed(int color) {
//        int hexColor = Integer.parseInt(String.format("0x%06x", color), 16);
        return (color & 0xFF0000) >> 16;
    }

    public static int getGreen(int color) {
        //int hexColor = Integer.parseInt(String.format("0x%06x", color), 16);
        return (color & 0xFF00) >> 8;
    }

    public static int getBlue(int color) {
        //int hexColor = Integer.parseInt(String.format("0x%06x", color), 16);
        return (color & 0xFF);
    }

    /**
     * Converts the components of a color, as specified by the default RGB
     * model, to an equivalent set of values for hue, saturation, and
     * brightness that are the three components of the HSB model.
     * <p>
     * If the <code>hsbvals</code> argument is <code>null</code>, then a
     * new array is allocated to return the result. Otherwise, the method
     * returns the array <code>hsbvals</code>, with the values put into
     * that array.
     * @param     r   the red component of the color
     * @param     g   the green component of the color
     * @param     b   the blue component of the color
     * @param     hsbvals  the array used to return the
     *                     three HSB values, or <code>null</code>
     * @return    an array of three elements containing the hue, saturation,
     *                     and brightness (in that order), of the color with
     *                     the indicated red, green, and blue components.
     * @see       java.awt.Color#getRGB()
     * @see       java.awt.Color#Color(int)
     * @see       java.awt.image.ColorModel#getRGBdefault()
     * @since     JDK1.0
     */
    public static float[] rgbtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
            int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
            hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
                else
            hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
            hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static int rgb2ARGB(int color) {
//        System.out.println("rgb2argb color=" + color + ", hexColor=" + String.format("#FF%06x", color));
        return android.graphics.Color.parseColor(String.format("#FF%06x", color));
    }

    public static String formatRGBColor(int color) {
        return "colorDec=" + color + ", colorHex=" + String.format("#FF%06x", color);
    }

    public static String formatHSBColor(int[] hsb) {
        return "H=" + hsb[0] + ", S=" + hsb[1] + ", B=" + hsb[2];
    }

    private static int resetColor(final int c, final int precent) {
        int destC = 0;

        if (precent <= 0) {
            destC = 0;
        } else if (precent >= 100) {
            destC = 255;
        } else {
            destC = (int) (255D * (precent / 100D));
        }

        return destC;
    }

    public static int resetColor(final int color,
            final int a, final int r,
            final int g, final int b, final int precent) {

        int destA, destR, destG, destB;

        destA = Color.alpha(color);
        destR = Color.red(color);
        destG = Color.green(color);
        destB = Color.blue(color);

        if (a != -1) {
             destA = resetColor(a, precent);
        }

        if (r != -1) {
            destR = resetColor(r, precent);
        }

        if (g != -1) {
            destG = resetColor(g, precent);
        }

        if (b != -1) {
            destB = resetColor(b, precent);
        }

        return Color.argb(destA, destR, destG, destB);
    }

    public static Drawable resetDrawableSize(Context context, Drawable drawable, 
            int width, int height) {
        if (context == null || drawable == null) {
            return drawable;
        }

        if (true) {
            return drawable;
        }

        Bitmap srcBitmap = null;

        if (drawable instanceof BitmapDrawable) {
            srcBitmap = ((BitmapDrawable)drawable).getBitmap();
        } else if (drawable instanceof NinePatchDrawable) {
            srcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(srcBitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
        }

        return new BitmapDrawable(context.getResources(), srcBitmap);
    }

    /**
     * Reset drawable color, especial for dot 9 pictures. Dot 9 pictures should invoke with
     * width & height.
     */
    public static Drawable resetDrawableColor(Context context, Drawable drawable, 
            int width, int height, int color) {
        if (context == null || drawable == null) {
            return drawable;
        }

        if (true) {
            return drawable;
        }

        Bitmap srcBitmap = null;

        if (drawable instanceof BitmapDrawable) {
            srcBitmap = ((BitmapDrawable)drawable).getBitmap();
        } else if (drawable instanceof NinePatchDrawable) {
            srcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(srcBitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
        }

        return new BitmapDrawable(context.getResources(), resetBitmapColor(srcBitmap, color));
    }

    public static Drawable resetDrawableColor(Context context, Drawable drawable, int color) {
        if (context == null || drawable == null) {
            return drawable;
        }

//        if (true) {
//            return drawable;
//        }

        if (drawable instanceof StateListDrawable) {
            return drawable;
        } else {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap srcBitmap = bitmapDrawable.getBitmap();

            return new BitmapDrawable(context.getResources(), resetBitmapColor(srcBitmap, color));
        }
    }

    public static Bitmap resetBitmapColor(Bitmap src, int color) {
        if (src == null) {
            return null;
        }

        // Ignore to paint specific color(white)
        if ((color & INVALID_COLOR) == INVALID_COLOR) {
            return src;
        }

        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        int A, R, G, B;
        int pixelColor;
        int height = src.getHeight();
        int width = src.getWidth();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                pixelColor = src.getPixel(j, i);
                A = Color.alpha(pixelColor);
                R = Color.red(pixelColor);
                G = Color.green(pixelColor);
                B = Color.blue(pixelColor);

                if (pixelColor != 0) {
                    R = Color.red(color);
                    G = Color.green(color);
                    B = Color.blue(color);
                }

                output.setPixel(j, i, Color.argb(A, R, G, B));
            }
        }

        return output;
    }

//    /*
//     * Get average color by system wallpaper
//     *
//     * @param bitmap: Bitmap of system wallpaper
//     */
//    public static int getSystemMainColor(Bitmap bitmap) {
//        //update begin by renxg for bug 51665 at 20140928
//        MMCQ mmcq = new MMCQ();
//        mmcq.GetBitmapMMCQ(bitmap, 5, 0);
//        List<VBox> retList = new ArrayList<VBox>();
//        int domainRGBColor = 0;
//
//        if(mmcq.pq2!=null){
//            while(mmcq.pq2!=null && mmcq.pq2.size()!=0){
//                   VBox vbox = mmcq.pq2.pop();
//                retList.add(vbox);
//             }
//        
//                VBox vbox = (VBox)retList.get(0);
//                if(vbox != null){
//                domainRGBColor = Color.rgb(vbox.avg()[0],vbox.avg()[1], vbox.avg()[2]);
//                }
//        
//            return domainRGBColor;
//        }
//    
//        long redsumColor = 0;
//        long greensumColor = 0;
//        long bluesumColor = 0;
//        int  tempcolor = 0;
//        int colorthreshold = 0x60;
//        int width = bitmap.getWidth();
//        int height = bitmap.getHeight();
//        int[] pixels = new int[width * height];
//        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
//        
//        for (int i = 0; i < height; i++) {
//            for (int j = 0; j < width; j++) {               
//                tempcolor = (pixels[width * i + j] & 0xFFFFFF);                         
//                int red = ((tempcolor & 0x00FF0000) >> 16);  
//                int green = ((tempcolor & 0x0000FF00) >> 8);  
//                int blue = (tempcolor & 0x000000FF);                   
//                redsumColor += red;
//                greensumColor += green;
//                bluesumColor += blue;
//            }
//        }
//        
//        int red_avg_color = (int) (redsumColor / (width * height));
//        int green_avg_color = (int) (greensumColor / (width * height));
//        int blue_avg_color = (int) (bluesumColor / (width * height));
//        
//        redsumColor = 0;
//        greensumColor = 0;
//        bluesumColor = 0;
//                
//        int avg_color = (red_avg_color << 16) + (green_avg_color << 8) + blue_avg_color;
//
//        int hueColorNum = 0;
//        for (int i = 0; i < height; i++) {  
//            for (int j = 0; j < width; j++) {               
//                tempcolor = (pixels[width * i + j] & 0xFFFFFF);
//                int red = ((tempcolor & 0x00FF0000) >> 16);  
//                int green = ((tempcolor & 0x0000FF00) >> 8);  
//                int blue = (tempcolor & 0x000000FF);            
//                
//                if(Math.abs(red - red_avg_color) > colorthreshold) {        
//                    redsumColor += red;
//                    greensumColor += green;
//                    bluesumColor += blue;  
//                    hueColorNum++;
//                } 
//                else if(Math.abs(green - green_avg_color) > colorthreshold) {
//                    redsumColor += red;
//                    greensumColor += green;
//                    bluesumColor += blue;
//                    hueColorNum++;
//                }
//                else if(Math.abs(blue - blue_avg_color) > colorthreshold) {      
//                    redsumColor += red;
//                    greensumColor += green;
//                    bluesumColor += blue;
//                    hueColorNum++;
//                }
//            }
//        }    
//                
//        if(hueColorNum == 0) {
//            return (pixels[10] & 0xFFFFFF);     
//        } else if(hueColorNum < ((width * height * 10)/100)) {
//            return avg_color;           
//        }
//        else {        
//            red_avg_color = (int) (redsumColor / hueColorNum);
//            green_avg_color = (int) (greensumColor / hueColorNum);
//            blue_avg_color = (int) (bluesumColor / hueColorNum);
//            avg_color = (red_avg_color << 16) + (green_avg_color << 8) + blue_avg_color;
//            return avg_color;
//        }
//    
//        //update end by renxg for bug 51665 at 20140928
//    }
}
