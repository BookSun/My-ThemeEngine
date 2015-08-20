package com.lewa.themechooser;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lewa.themechooser.custom.main.DeskTopWallpaper;
import com.lewa.themechooser.custom.main.Fonts;
import com.lewa.themechooser.custom.main.Icon;
import com.lewa.themechooser.custom.main.LiveWallpaper;
import com.lewa.themechooser.custom.main.LockScreenStyle;
import com.lewa.themechooser.custom.main.LockScreenWallpaper;
import com.lewa.themechooser.custom.main.SystemApp;
import com.lewa.themechooser.widget.TileLayout;

import static com.lewa.themes.ThemeManager.STANDALONE;

public class CustomerFragment extends Fragment implements View.OnClickListener {
    //data
    public boolean wallpaperSelector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null)
            wallpaperSelector = args.getBoolean("wallpaper", false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup v = (ViewGroup) inflater.inflate(wallpaperSelector ?
                R.layout.theme_customer_wallpaper :
                (STANDALONE ?
                        R.layout.theme_customer_main_standalone :
                        R.layout.theme_customer_main), container, false);
        batchSetOnClickListener((ViewGroup) v.findViewById(android.R.id.widget_frame));
        return v;
    }

    private void batchSetOnClickListener(ViewGroup group) {
        if (wallpaperSelector) {
            TileLayout t = (TileLayout) group;
            t.columns = 1;
            t.capital = false;
            t.cellWidth = getResources().getDimensionPixelSize(R.dimen.cell_width) * 2;
        }
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View v = group.getChildAt(i);
            v.setOnClickListener(this);
            v.setFocusable(true);
            v.setClickable(true);
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.fl_dynamic_wallpaper:
                startInternalActivity(LiveWallpaper.class);
                break;
            case R.id.fl_lockscreen_wallpaper:
                startInternalActivity(LockScreenWallpaper.class);
                break;
            case R.id.fl_lockscreen:
                startInternalActivity(LockScreenStyle.class);
                break;
            case R.id.fl_icon:
                startInternalActivity(Icon.class);
                break;
            case R.id.fl_desk_wallpaper:
                startInternalActivity(DeskTopWallpaper.class);
                break;
            case R.id.fl_systemapp:
                startInternalActivity(SystemApp.class);
                break;
            case R.id.fl_font:
                startInternalActivity(Fonts.class);
                break;
        }
    }

    void startInternalActivity(Class<?> clazz) {
        startActivity(new Intent(getActivity(), clazz));
    }
}
