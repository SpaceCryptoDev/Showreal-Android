package com.showreal.app.features.profile.instagram;

import com.showreal.app.data.InstagramApi;
import com.showreal.app.data.model.InstagramUser;

import rx.Observable;
import uk.co.thedistance.components.contentloading.DataSource;

public class InstagramDataSource implements DataSource<InstagramUser> {

    final InstagramApi api;
    private String token;

    public InstagramDataSource(InstagramApi api) {
        this.api = api;
    }

    public InstagramDataSource setToken(String token) {
        this.token = token;
        return this;
    }

    @Override
    public void reset() {

    }

    @Override
    public Observable<InstagramUser> getData() {
        return api.getUser(token);
    }
}
