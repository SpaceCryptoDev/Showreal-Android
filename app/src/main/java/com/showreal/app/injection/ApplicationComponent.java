package com.showreal.app.injection;

import android.content.SharedPreferences;

import com.danikula.videocache.HttpProxyCacheServer;
import com.google.gson.Gson;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.AppSettings;
import com.showreal.app.data.InstagramApi;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.maps.GoogleMapsApi;
import com.showreal.app.data.model.Notification;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.features.real.VideoDownloader;
import com.showreal.app.features.real.myreal.ReelHelper;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import io.reactivecache.Provider;
import io.reactivecache.ReactiveCache;
import uk.co.thedistance.components.analytics.Analytics;
import uk.co.thedistance.components.analytics.interfaces.AnalyticsTracker;

@Singleton
@Component(modules = {ApplicationModule.class, ApiModule.class, VideoModule.class})
public interface ApplicationComponent {

    TheDistanceApplication appContext();

    Analytics analytics();

    SharedPreferences preferences();

    Gson gson();

    ShowRealApi api();

    AccountHelper accountHelper();

    ReactiveCache cache();

    Provider<Profile> profileProvider();

    @Named("stale_profile") Provider<Profile> staleProfileProvider();

    Provider<Settings> settingsProvider();

    InstagramApi instagram();

    AppSettings settings();

    SendBirdHelper sendbird();

    VideoHelper videoHelper();

    ReelHelper reelHelper();

    VideoDownloader videoDownloader();

    GoogleMapsApi mapsApi();

    HttpProxyCacheServer cacheServer();
}
