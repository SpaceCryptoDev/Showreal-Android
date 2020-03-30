package com.showreal.app.features.profile.other;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Profile;

import io.reactivecache.Provider;
import rx.Observable;
import uk.co.thedistance.components.contentloading.DataSource;


public class OtherProfileDataSource implements DataSource<Profile> {

    private final Context context;
    private final ShowRealApi api;
    private final Provider<Profile> profileProvider;
    private final int id;

    public OtherProfileDataSource(Context context, int id) {
        this.context = context;
        this.id = id;
        profileProvider = TheDistanceApplication.getApplicationComponent(context).cache().<Profile>provider().expirable(false).withKey("profile_" + id);
        api = TheDistanceApplication.getApplicationComponent(context).api();
    }

    @Override
    public void reset() {

    }

    @Override
    public Observable<Profile> getData() {
        return api.getProfile(id)
                .compose(profileProvider.readWithLoader());
    }
}