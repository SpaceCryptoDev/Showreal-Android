package com.showreal.app.features.reviews.preferences;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Profile;
import com.showreal.app.features.onboarding.signup.SignUpProfileUploader;

import rx.Observable;

public class PreferencesUploader extends SignUpProfileUploader {
    private final Gson gson;

    public PreferencesUploader(Context context, ShowRealApi api) {
        super(context, api);
        this.gson = TheDistanceApplication.getApplicationComponent(context).gson();
    }

    protected Observable<Profile> getUploadObservable() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add("interested_in", new JsonPrimitive(profile.interestedIn));
        jsonObject.add("search_latitude", new JsonPrimitive(profile.searchLatitude));
        jsonObject.add("search_longitude", new JsonPrimitive(profile.searchLongitude));
        jsonObject.add("search_radius", new JsonPrimitive(profile.searchRadius));
        jsonObject.add("preferred_age", gson.toJsonTree(profile.preferredAge));

        return api.updateProfile(jsonObject);
    }
}
