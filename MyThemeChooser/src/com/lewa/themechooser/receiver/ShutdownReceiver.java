package com.lewa.themechooser.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FilenameFilter;

public class ShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        File[] files = null;

        try {
            files = (context.createPackageContext("com.lewa.themechooser", 0).getFilesDir())
                    .listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".lwt");
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (File f : files) {
            f.delete();
        }
    }
}
