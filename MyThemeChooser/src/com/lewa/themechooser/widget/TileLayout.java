package com.lewa.themechooser.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.lewa.themechooser.R;

import util.ThemeUtil;

public class TileLayout extends ViewGroup {
    public int cellWidth;
    public int cellHeight;
    public int gapVertical;
    public int gapHorizontal;
    public int columns = 2;
    public boolean capital = true;

    Paint mPaint = new Paint();
    Path mPath = new Path();

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 1);
    }

    public TileLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TileLayout, defStyle, 0);
        capital = a.getBoolean(R.styleable.TileLayout_capital, true);
        cellHeight = a.getDimensionPixelSize(R.styleable.TileLayout_tileHeight, res.getDimensionPixelSize(R.dimen.cell_height));
        cellWidth = a.getDimensionPixelSize(R.styleable.TileLayout_tileHeight, res.getDimensionPixelSize(R.dimen.cell_width));
        gapVertical = a.getDimensionPixelSize(R.styleable.TileLayout_gapVertical, res.getDimensionPixelSize(R.dimen.gap_vertical));
        gapHorizontal = a.getDimensionPixelSize(R.styleable.TileLayout_gapHorizontal, res.getDimensionPixelSize(R.dimen.gap_horizontal));

        a.recycle();
        setLayerType(LAYER_TYPE_HARDWARE, mPaint);
        setWillNotDraw(false);
    }

    private void updateShader() {
        int width = getWidth();
        int height = getHeight();
        if (height <= 0)
            return;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper_rain, options);
            options.inSampleSize = ThemeUtil.calculateInSampleSize(options, width, height);
            options.inJustDecodeBounds = false;
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper_rain, options);
            if (bmp != null && height > 0) {
                int bH = bmp.getHeight();
                int bW = bmp.getWidth();
                int x = bW > width ? ((bW - width) / 2) : 0;
                int w = bW > width ? width : bW;
                int y = bH > height ? ((bH - height) / 2) : 0;
                int h = bH > height ? height : bH;
                Bitmap bg = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
                Canvas c = new Canvas(bg);
                c.drawBitmap(bmp, -x, -y, null);
                bmp.recycle();
                mPaint.setShader(new BitmapShader(bg, TileMode.CLAMP, TileMode.CLAMP));
            }
        } catch (OutOfMemoryError e) {
            mPaint.setShader(null);
            mPaint.setColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int cWidthSpec = MeasureSpec.makeMeasureSpec(cellWidth,
                MeasureSpec.EXACTLY);
        int cHeightSpec = MeasureSpec.makeMeasureSpec(cellHeight,
                MeasureSpec.EXACTLY);

        int count = getChildCount();
        boolean expandLast = (count + (capital ? 1 : 0)) % columns > 0;
        for (int index = 0; index < count; index++) {
            final View child = getChildAt(index);
            if (capital && index == 0)
                child.measure(cWidthSpec, MeasureSpec.makeMeasureSpec(cellHeight * 2 + gapHorizontal,
                        MeasureSpec.EXACTLY));
            else if (capital && expandLast && index == count - 1)
                child.measure(MeasureSpec.makeMeasureSpec(cellWidth * 2 + gapVertical,
                        MeasureSpec.EXACTLY), cHeightSpec);
            else
                child.measure(cWidthSpec, cHeightSpec);
        }
        float minCount = (count > 2 ? count : 2) + (capital ? 1 : 0);
        setMeasuredDimension(resolveSize(cellWidth * columns + (columns - 1) * gapVertical, widthMeasureSpec),
                resolveSize((cellHeight + gapHorizontal) * Math.round(minCount / columns) + getPaddingBottom(), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final Path path = mPath;
        path.reset();
        int cWidth = cellWidth;
        int cHeight = cellHeight;
        int gV = gapVertical;
        int gH = gapHorizontal;
        int col = columns < 0 ? (r - l) / cWidth : columns;
        if (col < 0) {
            col = 1;
        }
        int x = 0;
        int y = 0;
        int i = 0;
        int count = getChildCount();
        boolean cap = capital;
        if (cap) {
            boolean expandLast = (count + (capital ? 1 : 0)) % col > 0;
            for (int index = 0; index < count; index++) {
                final View child = getChildAt(index);
                final int width = x + (expandLast && index == count - 1 ? cWidth * 2 + gV : cWidth);
                final int height = y + (index == 0 ? cHeight * 2 + gH : cHeight);
                child.layout(x, y, width, height);
                path.addRoundRect(new RectF(x, y, width, height), 10, 10, Direction.CW);
                if (i >= (col - 1)) {
                    if (index == col - 1) {
                        // advance to next row and increase one column
                        i = 1;
                        x = cWidth + gV;
                    } else {
                        // advance to next row
                        i = 0;
                        x = 0;
                    }
                    y += cHeight + gH;
                } else {
                    i++;
                    x += cWidth + gV;
                }
            }
        } else {
            for (int index = 0; index < count; index++) {
                final View child = getChildAt(index);
                child.layout(x, y, x + cWidth, y + cHeight);
                path.addRect(x, y, x + cWidth, y + cHeight, Direction.CW);
                if (i >= (col - 1)) {
                    // advance to next row
                    i = 0;
                    x = 0;
                    y += cHeight + gH;
                } else {
                    i++;
                    x += cWidth + gV;
                }
            }
        }
        updateShader();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mPath, mPaint);
    }
}
