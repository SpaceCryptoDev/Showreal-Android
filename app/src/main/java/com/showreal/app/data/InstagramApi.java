package com.showreal.app.data;

import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.data.model.InstagramUser;

import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface InstagramApi {

    @GET("users/self")
    Observable<InstagramUser> getUser(@Query("access_token") String token);
    @GET("users/self/media/recent")
    Observable<InstagramMedia> getMedia(@Query("access_token") String token);
    @GET
    Observable<InstagramMedia> getNextMedia(@Url String url);
}
