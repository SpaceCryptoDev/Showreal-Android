package com.showreal.app.features.profile;

import android.content.Context;
import android.text.TextUtils;

import com.showreal.app.data.ShowRealApi;
import com.showreal.app.features.onboarding.signup.SignUpProfileUploader;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ProfileUploader extends SignUpProfileUploader {
    public ProfileUploader(Context context, ShowRealApi api) {
        super(context, api);
    }

    @Override
    protected Map<String, RequestBody> createProfileBody() {
        Map<String, RequestBody> bodyMap = super.createProfileBody();
        bodyMap.put("email", RequestBody.create(MediaType.parse("text/plain"), profile.email));
        bodyMap.put("dob", RequestBody.create(MediaType.parse("text/plain"), profile.dateOfBirth));
        bodyMap.put("instagram_id", RequestBody.create(MediaType.parse("text/plain"), profile.instagramId == null ? "" : profile.instagramId));
        bodyMap.put("instagram_access_token", RequestBody.create(MediaType.parse("text/plain"), profile.instagramAccessToken == null ? "" : profile.instagramAccessToken));

        return bodyMap;
    }
}
