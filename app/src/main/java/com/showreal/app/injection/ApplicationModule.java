package com.showreal.app.injection;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.analytics.AppboyAnalyticsTracker;
import com.showreal.app.data.AppSettings;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.Notification;

import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivecache.Provider;
import io.reactivecache.ReactiveCache;
import uk.co.thedistance.components.analytics.Analytics;
import uk.co.thedistance.components.analytics.interfaces.AnalyticsTracker;

@Module
public class ApplicationModule {

    private TheDistanceApplication application;

    public ApplicationModule(TheDistanceApplication application) {
        this.application = application;
    }

    @Provides
    @Singleton
    TheDistanceApplication provideAppContext() {
        return application;
    }

    @Provides
    @Singleton
    AnalyticsTracker provideTracker(TheDistanceApplication application) {
        return new AppboyAnalyticsTracker(application);
    }

    @Provides
    @Singleton
    Analytics provideAnalytics(TheDistanceApplication application, AnalyticsTracker tracker) {
        return new Analytics(application, tracker);
    }

    @Provides
    @Singleton
    SharedPreferences providePreferences(TheDistanceApplication application) {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides
    @Singleton
    AccountHelper provideAccountHelper(TheDistanceApplication application) {
        return new AccountHelper(application);
    }

    @Provides
    @Singleton
    AppSettings provideAppSettings(TheDistanceApplication application) {
        return new AppSettings(application);
    }

    @Provides
    @Singleton
    SendBirdHelper provideSendbird(TheDistanceApplication application) {
        return new SendBirdHelper(application);
    }
}
