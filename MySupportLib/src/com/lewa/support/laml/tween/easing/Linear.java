package com.lewa.support.laml.tween.easing;

public class Linear implements IEasing {

    @Override
    public double tick(double t, double b, double c, double d) {
        return c * t / d + b;
    }

}
