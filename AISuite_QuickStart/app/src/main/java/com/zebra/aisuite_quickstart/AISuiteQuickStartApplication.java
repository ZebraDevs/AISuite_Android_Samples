package com.zebra.aisuite_quickstart;

import android.app.Application;

import com.zebra.aisuite_quickstart.utils.AppLog;

public class AISuiteQuickStartApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AppLog.init("AISuiteQuickStart");
    }
}