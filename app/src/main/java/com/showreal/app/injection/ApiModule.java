package com.showreal.app.injection;

import android.text.TextUtils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.showreal.app.BuildConfig;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.InstagramApi;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.maps.GoogleMapsApi;
import com.showreal.app.data.maps.model.GeocodeResults;
import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.data.model.Liked;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.MutualFriends;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.data.model.Video;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivecache.Provider;
import io.reactivecache.ReactiveCache;
import io.victoralbertos.jolyglot.GsonSpeaker;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class ApiModule {

    @Provides
    @Singleton
    Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Video.class, new Video.VideoDeserializer())
                .registerTypeAdapter(Profile.class, new Profile.ProfileDeserializer())
                .registerTypeAdapter(InstagramMedia.class, new InstagramMedia.MediaDeserializer())
                .registerTypeAdapter(Match.class, new Match.MatchDeserializer())
                .registerTypeAdapter(Liked.class, new Liked.LikedDeserializer())
                .registerTypeAdapter(MutualFriends.class, new MutualFriends.MutualFriendsDeserializer())
                .registerTypeAdapter(GeocodeResults.class, new GeocodeResults.GeocodeDeserializer())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'")
                .create();
    }

    @Provides
    @Singleton
    ShowRealApi provideApi(TheDistanceApplication application, final AccountHelper accountHelper) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        String authToken = accountHelper.getAuthToken();

                        Request request = chain.request();
                        if (!TextUtils.isEmpty(authToken)) {
                            request = chain.request().newBuilder()
                                    .addHeader("Authorization", authToken)
                                    .build();

                        }
                        return chain.proceed(request);
                    }
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(application.getString(R.string.url_api))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(provideGson()))
                .client(client)
                .build();

        return retrofit.create(ShowRealApi.class);
    }

    @Provides
    @Singleton
    ReactiveCache provideCache(TheDistanceApplication application) {
        return new ReactiveCache.Builder()
                .using(application.getFilesDir(), new GsonSpeaker());
    }

    @Provides
    Provider<Profile> provideProfileProvider(ReactiveCache cache) {
        return cache.<Profile>provider()
                .lifeCache(30, TimeUnit.MINUTES)
                .expirable(true)
                .withKey("profile");
    }

    @Provides
    @Named("stale_profile")
    Provider<Profile> provideStaleProfileProvider(ReactiveCache cache) {
        return cache.<Profile>provider()
                .withKey("profile_stale");
    }

    @Provides
    Provider<Settings> provideSettingsProvider(ReactiveCache cache) {
        return cache.<Settings>provider()
                .withKey("settings");
    }

    @Provides
    InstagramApi provideInstagram(Gson gson) {
        return new Retrofit.Builder()
                .baseUrl("https://api.instagram.com/v1/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(InstagramApi.class);
    }

    @Provides
    @Singleton
    GoogleMapsApi provideMapsApi() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(provideGson()))
                .client(client)
                .build();

        return retrofit.create(GoogleMapsApi.class);
    }
}
