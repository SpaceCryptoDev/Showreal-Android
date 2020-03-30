package com.showreal.app.features.settings;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Settings;

import io.reactivecache.Provider;
import io.reactivecache.ReactiveCache;
import rx.Observable;
import uk.co.thedistance.components.contentloading.DataSource;

public class SettingsDataSource implements DataSource<Settings> {

    private final ShowRealApi api;
    private final Provider<Settings> settingsProvider;

    public SettingsDataSource(Context context) {
        api = TheDistanceApplication.getApplicationComponent(context).api();
        ReactiveCache cache = TheDistanceApplication.getApplicationComponent(context).cache();

        settingsProvider = cache.<Settings>provider().withKey("settings");
    }

    @Override
    public void reset() {

    }

    @Override
    public Observable<Settings> getData() {
        return api.getSettings()
                .compose(settingsProvider.replace());
    }
}
