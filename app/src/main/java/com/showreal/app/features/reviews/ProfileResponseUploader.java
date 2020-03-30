package com.showreal.app.features.reviews;

import com.google.gson.JsonSyntaxException;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Like;
import com.showreal.app.data.model.ProfileResponse;

import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class ProfileResponseUploader implements DataUploader<ProfileResponse, Integer> {


    private ProfileResponse response;
    private final ShowRealApi api;
    private int userId;

    public ProfileResponseUploader(ShowRealApi api, int userId) {
        this.api = api;
        this.userId = userId;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(ProfileResponse content) {
        this.response = content;
    }

    @Override
    public Observable<Integer> getUpload() {
        switch (response) {
            case Cut:
                return api.cutUser(userId).map(new Func1<Void, Integer>() {
                    @Override
                    public Integer call(Void avoid) {
                        return userId;
                    }
                });
            case SecondChance:
                return api.chanceUser(userId).map(new Func1<Void, Integer>() {
                    @Override
                    public Integer call(Void avoid) {
                        return userId;
                    }
                });
            case Keep:
            default:
                return api.likeUser(new Like(userId)).map(new Func1<Like, Integer>() {
                    @Override
                    public Integer call(Like like) {
                        return like.liked;
                    }
                }).onErrorResumeNext(new Func1<Throwable, Observable<? extends Integer>>() {
                    @Override
                    public Observable<? extends Integer> call(Throwable throwable) {
                        if (throwable instanceof JsonSyntaxException) {
                            return Observable.just(userId);
                        }
                        return Observable.error(throwable);
                    }
                });
        }
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
