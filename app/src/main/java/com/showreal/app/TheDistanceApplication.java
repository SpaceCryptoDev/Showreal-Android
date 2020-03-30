package com.showreal.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.multidex.MultiDex;

import com.appboy.Appboy;
import com.appboy.AppboyLifecycleCallbackListener;
import com.bumptech.glide.request.target.ViewTarget;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sendbird.android.SendBird;
import com.showreal.app.data.model.Device;
import com.showreal.app.features.notifications.NotificationFactory;
import com.showreal.app.injection.ApiModule;
import com.showreal.app.injection.ApplicationComponent;
import com.showreal.app.injection.ApplicationModule;
import com.showreal.app.injection.DaggerApplicationComponent;
import com.showreal.app.injection.VideoModule;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.TDObservers;

public class TheDistanceApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private ApplicationComponent applicationComponent;
    private int activityCount;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            MultiDex.install(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ViewTarget.setTagId(R.id.glide_tag);

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .apiModule(new ApiModule())
                .videoModule(new VideoModule())
                .build();

        FacebookSdk.sdkInitialize(this, new FacebookSdk.InitializeCallback() {
            @Override
            public void onInitialized() {
                AppEventsLogger.activateApp(TheDistanceApplication.this);
            }
        });

        SendBird.init(getResources().getString(R.string.sendbird_api_id), this);

        registerActivityLifecycleCallbacks(new AppboyLifecycleCallbackListener());
        Appboy.setCustomAppboyNotificationFactory(new NotificationFactory());

        if (BuildConfig.USE_CRASHLYTICS && applicationComponent.preferences().getBoolean(getResources().getString(R.string.pref_key_crashlytics_enabled), true)) {
            Fabric.with(this, new Crashlytics());
        }

        if (applicationComponent.accountHelper().isLoggedIn()) {
            registerPush(this);
            applicationComponent.sendbird().initialise().subscribeOn(Schedulers.io())
                    .subscribe(TDObservers.<Boolean>empty());
        }

        registerActivityLifecycleCallbacks(this);
    }

    public static void registerPush(Context context) {

        try {
            String appboyToken = FirebaseInstanceId.getInstance().getToken("139554035552", "FCM");
            if (appboyToken != null) {
                Appboy.getInstance(context).registerAppboyPushMessages(appboyToken);
            }
        } catch (IOException ignored) {
        }

        String token = FirebaseInstanceId.getInstance().getToken();
        if (token == null) {
            return;
        }
        TheDistanceApplication.getApplicationComponent(context).api()
                .registerDevice(new Device(token, FirebaseInstanceId.getInstance().getId()))
                .subscribeOn(Schedulers.io())
                .subscribe(TDObservers.<Void>empty());
    }

    public static TheDistanceApplication get(Context context) {
        return (TheDistanceApplication) context.getApplicationContext();
    }

    public static ApplicationComponent getApplicationComponent(Context context) {
        return get(context).applicationComponent;
    }

    public ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }

    private Runnable disconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (activityCount <= 0) {
                applicationComponent.sendbird().destroy();
            }
        }
    };

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    private final Handler disconnectHandler = new Handler();

    @Override
    public void onActivityResumed(Activity activity) {
        activityCount++;
        disconnectHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        activityCount--;
        if (activityCount == 0) {
            disconnectHandler.postDelayed(disconnectRunnable, 500);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public boolean isForeground() {
        return activityCount > 0;
    }
}
