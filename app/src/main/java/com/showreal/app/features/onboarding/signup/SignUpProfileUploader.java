package com.showreal.app.features.onboarding.signup;

import android.content.Context;
import android.text.TextUtils;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.Profile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.reactivecache.Provider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class SignUpProfileUploader implements DataUploader<Profile, Profile> {

    protected final ShowRealApi api;
    private final Provider<Profile> profileProvider;
    private final Provider<Profile> staleProfileProvider;
    private final Context context;
    protected Profile profile;

    public SignUpProfileUploader(Context context, ShowRealApi api) {
        this.api = api;
        this.context = context;
        profileProvider = TheDistanceApplication.getApplicationComponent(context).profileProvider();
        staleProfileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(Profile content) {
        this.profile = content;
    }

    protected Observable<Profile> getUploadObservable() {

        Map<String, RequestBody> body = createProfileBody();

        if (TextUtils.isEmpty(profile.newImage) ) {
            return api.updateProfile(body);
        }
        RequestBody imageBody = createImageBody();

        return api.updateProfile(imageBody, body);
    }

    private RequestBody createImageBody() {
        return RequestBody.create(MediaType.parse("image/jpg"), new File(profile.newImage));
    }

    protected Map<String, RequestBody> createProfileBody() {
        Map<String, RequestBody> bodyMap = new HashMap<>();

        bodyMap.put("first_name", RequestBody.create(MediaType.parse("text/plain"), profile.firstName));
        bodyMap.put("last_name", RequestBody.create(MediaType.parse("text/plain"), profile.lastName));
        bodyMap.put("gender", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(profile.gender)));
        bodyMap.put("height", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(profile.height)));
        if (profile.city != null) {
            bodyMap.put("city", RequestBody.create(MediaType.parse("text/plain"), profile.city));
        }
        bodyMap.put("profile_latitude", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(profile.latitude)));
        bodyMap.put("profile_longitude", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(profile.longitude)));

        return bodyMap;
    }

    @Override
    public Observable<Profile> getUpload() {
        return getUploadObservable()
                .flatMap(new Func1<Profile, Observable<Profile>>() {
                    @Override
                    public Observable<Profile> call(Profile profile) {
                        profile.videos = SignUpProfileUploader.this.profile.videos;
                        profile.photos = SignUpProfileUploader.this.profile.photos;

                        Profile.updateAppboy(profile, context);
                        Profile.updateRegion(profile, context);
                        TheDistanceApplication.getApplicationComponent(context).accountHelper().setInstagramToken(profile.instagramAccessToken);

                        return Observable.just(profile)
                                .compose(profileProvider.replace())
                                .compose(staleProfileProvider.replace());
                    }
                });
    }
}
