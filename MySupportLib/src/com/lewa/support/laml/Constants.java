
package com.lewa.support.laml;

import android.content.Intent;

public interface Constants {
    public static final int INTENT_FLAG = Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
            | Intent.FLAG_RECEIVER_BOOT_UPGRADE | Intent.FLAG_RECEIVER_REPLACE_PENDING;

    public static final boolean CONFIG_SHOW_FPS = false;
}
