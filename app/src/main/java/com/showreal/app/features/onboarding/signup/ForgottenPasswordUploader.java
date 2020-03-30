package com.showreal.app.features.onboarding.signup;

import com.showreal.app.data.ShowRealApi;

import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class ForgottenPasswordUploader implements DataUploader<String, String> {

    private String email;
    private final ShowRealApi api;

    public ForgottenPasswordUploader(ShowRealApi api) {
        this.api = api;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(String content) {
        this.email = content;
    }

    @Override
    public Observable<String> getUpload() {
        return api.resetPassword(email)
                .map(new Func1<Void, String>() {
                    @Override
                    public String call(Void unused) {
                        return email;
                    }
                });
    }
}
