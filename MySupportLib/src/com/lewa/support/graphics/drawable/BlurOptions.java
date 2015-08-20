package com.lewa.support.graphics.drawable;

public class BlurOptions {
    public static final int STRENGTH_LOW = 0;
    public static final int STRENGTH_MODERATE = 1;
    public static final int STRENGTH_HIGH = 2;

    public BlurOptions() {
        strength = STRENGTH_HIGH;
    }

    public int strength;
}
