package com.showreal.app;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class FabricHelper {

    public static void logException(Throwable throwable) {
        if (Fabric.isInitialized()) {
            Crashlytics.logException(throwable);
        }
    }
}
