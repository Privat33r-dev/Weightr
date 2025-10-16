package com.snhu.weightr;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class Weightr extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
