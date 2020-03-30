package com.showreal.app.features.settings;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Settings;

import io.reactivecache.Provider;
import io.reactivecache.ReactiveCache;
import rx.Observable;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class SettingsUploader implements DataUploader<Settings, Settings> {

    private final ShowRealApi api;
    private final Provider<Settings> settingsProvider;
    private Settings settings;

    public SettingsUploader(Context context) {
        this.api = TheDistanceApplication.getApplicationComponent(context).api();
        ReactiveCache cache = TheDistanceApplication.getApplicationComponent(context).cache();
        settingsProvider = cache.<Settings>provider().withKey("settings");
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(Settings content) {
        this.settings = content;
    }

    @Override
    public Observable<Settings> getUpload() {
        return api.updateSettings(settings)
                .compose(settingsProvider.replace());
    }
}
