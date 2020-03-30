package com.showreal.app.features.settings;

import android.content.Context;
import android.support.v4.util.Pair;

import com.facebook.login.LoginResult;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Login;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Session;

import java.util.HashMap;
import java.util.Map;

import io.reactivecache.Provider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;
import uk.co.thedistance.thedistancecore.TDObservers;

public class FacebookConnectUploader implements DataUploader<FacebookConnectUploader.FacebookConnect, Profile> {

    protected final ShowRealApi api;
    private final Provider<Profile> profileProvider;
    private final Provider<Profile> staleProfileProvider;
    private FacebookConnect connect;

    public static class FacebookConnect {
        final Profile profile;
        final LoginResult result;

        public FacebookConnect(Profile profile, LoginResult result) {
            this.profile = profile;
            this.result = result;
        }
    }


    public FacebookConnectUploader(Context context, ShowRealApi api) {
        this.api = api;
        profileProvider = TheDistanceApplication.getApplicationComponent(context).profileProvider();
        staleProfileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(FacebookConnect content) {
        this.connect = content;
    }

    private Observable<Profile> getUploadObservable() {
        Map<String, RequestBody> body = createProfileBody();
        return api.updateProfile(body);
    }


    private Map<String, RequestBody> createProfileBody() {
        Map<String, RequestBody> bodyMap = new HashMap<>();

        bodyMap.put("facebook_id", RequestBody.create(MediaType.parse("text/plain"), connect.result.getAccessToken().getUserId()));

        return bodyMap;
    }

    @Override
    public Observable<Profile> getUpload() {
        Login login = Login.with(connect.result);

        return api.login(login)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Session>>() {
                    @Override
                    public Observable<? extends Session> call(Throwable throwable) {
                        return Observable.just(null);
                    }
                })
                .flatMap(new Func1<Session, Observable<Profile>>() {
                    @Override
                    public Observable<Profile> call(Session session) {
                        if (session != null && session.profile != null) {
                            return Observable.error(new FacebookConnectException());
                        }

                        return getUploadObservable()
                                .flatMap(new Func1<Profile, Observable<Profile>>() {
                                    @Override
                                    public Observable<Profile> call(Profile profile) {
                                        profile.videos = connect.profile.videos;
                                        profile.photos = connect.profile.photos;

                                        api.updateFacebookToken(connect.result.getAccessToken().getToken()).subscribe(TDObservers.<Void>empty());

                                        return Observable.just(profile)
                                                .compose(profileProvider.replace())
                                                .compose(staleProfileProvider.replace());
                                    }
                                });
                    }
                });

    }

    static class FacebookConnectException extends IllegalArgumentException {
        FacebookConnectException() {
            super("Facebook account is already connected to another ShowReal account");
        }
    }
}
