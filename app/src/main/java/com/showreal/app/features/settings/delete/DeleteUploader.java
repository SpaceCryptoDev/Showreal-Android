package com.showreal.app.features.settings.delete;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;

import rx.Observable;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class DeleteUploader implements DataUploader<Void, Void> {

    private final ShowRealApi api;

    public DeleteUploader(Context context) {
        this.api = TheDistanceApplication.getApplicationComponent(context).api();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(Void content) {

    }

    @Override
    public Observable<Void> getUpload() {
        return api.deleteProfile();
    }
}
