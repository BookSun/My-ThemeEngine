package com.lewa.themechooser.appwidget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.lewa.themechooser.R;
import com.lewa.themechooser.appwidget.util.WallpaperUtils;

public class CoveringPageActivity extends Activity {
    public static final String ACTION_FINISH_COERINGPAGE = "com.lewa.themechooser.finish_covering_page";
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_FINISH_COERINGPAGE)) {
                CoveringPageActivity.this.finish();
                WallpaperUtils.isChanging = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.covering_page);
        String flag = getIntent().getStringExtra("flag");
        if (flag != null && "FontChange".equals(flag)) {
            ProgressBar pb = (ProgressBar) findViewById(R.id.pb_clip);
            pb.setVisibility(View.GONE);
        }
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_FINISH_COERINGPAGE));
    }

    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    ;

}
