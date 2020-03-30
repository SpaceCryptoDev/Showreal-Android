package com.showreal.app.features.profile;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Profile;

import io.reactivecache.Provider;
import rx.Observable;
import rx.functions.Action1;
import uk.co.thedistance.components.contentloading.DataSource;

public class ProfileDataSource implements DataSource<Profile> {

    private final Provider<Profile> profileProvider;
    private final ShowRealApi api;
    private final Provider<Profile> staleProfileProvider;
    private final Context context;

    public ProfileDataSource(Context context) {
        this.context = context;
        profileProvider = TheDistanceApplication.getApplicationComponent(context).profileProvider();
        api = TheDistanceApplication.getApplicationComponent(context).api();
        staleProfileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
    }

    @Override
    public void reset() {

    }

    @Override
    public Observable<Profile> getData() {
        return api.getProfile()
                .compose(profileProvider.readWithLoader())
                .compose(staleProfileProvider.replace())
                .doOnNext(new Action1<Profile>() {
                    @Override
                    public void call(Profile profile) {
                        Profile.updateAppboy(profile, context);
                    }
                });
    }
}
