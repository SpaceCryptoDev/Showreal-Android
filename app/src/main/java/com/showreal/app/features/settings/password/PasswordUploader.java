package com.showreal.app.features.settings.password;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.PasswordChange;

import rx.Observable;
import rx.functions.Action1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class PasswordUploader implements DataUploader<PasswordChange, Void> {

    private final ShowRealApi api;
    private final AccountHelper accountHelper;
    private PasswordChange passwordChange;

    public PasswordUploader(Context context) {
        this.api = TheDistanceApplication.getApplicationComponent(context).api();
        this.accountHelper = TheDistanceApplication.getApplicationComponent(context).accountHelper();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(PasswordChange content) {
        this.passwordChange = content;
    }

    @Override
    public Observable<Void> getUpload() {
        return api.changePassword(passwordChange)
                .doOnNext(new Action1<Void>() {
                    @Override
                    public void call(Void unused) {
                        accountHelper.updatePassword(passwordChange.newPassword);
                    }
                });
    }
}
