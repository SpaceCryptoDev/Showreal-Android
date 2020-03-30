package com.showreal.app.cookiecutter;

import android.util.Log;

import com.showreal.app.BuildConfig;


/**
 * Created by adamstyrc on 31/03/16.
 */
public class Logger {

    private static boolean sEnabled = false;

    public static void log(String message) {
        if (BuildConfig.DEBUG || sEnabled) {
            Log.d("cookie-cutter", message);
        }
    }

    public static void setEnabled(boolean sEnabled) {
        Logger.sEnabled = sEnabled;
    }
}
