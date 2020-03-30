package com.showreal.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.facebook.AccessToken;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.AppSetting;
import com.showreal.app.data.model.Profile;

import java.util.List;

import rx.functions.Action1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.TDObservers;
import uk.co.thedistance.thedistancecore.TDSubscribers;

public class AppSettings {

    private static final String PREFS_SETTINGS = "app_settings";
    private static final String SETTING_MIN_VIDEOS = "min_videos_required";
    private static final String SETTING_MAX_VIDEOS = "max_videos_required";
    private final SharedPreferences preferences;
    private final ShowRealApi api;
    private final AccountHelper accountHelper;

    public AppSettings(TheDistanceApplication application) {
        preferences = application.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        api = TheDistanceApplication.getApplicationComponent(application).api();
        accountHelper = TheDistanceApplication.getApplicationComponent(application).accountHelper();
    }

    public void update() {
        api.getAppSettings()
                .subscribeOn(Schedulers.io())
                .subscribe(TDSubscribers.ignorant(new Action1<List<AppSetting>>() {
                    @Override
                    public void call(List<AppSetting> appSettings) {
                        updateSettings(appSettings);
                    }
                }));

        accountHelper.getProfile()
                .subscribeOn(Schedulers.io())
                .subscribe(TDSubscribers.ignorant(new Action1<Profile>() {
                    @Override
                    public void call(Profile profile) {
                        if (!TextUtils.isEmpty(profile.facebookId)) {
                            AccessToken token = AccessToken.getCurrentAccessToken();
                            if (token != null) {
                                api.updateFacebookToken(token.getToken()).subscribe(TDObservers.<Void>empty());
                            }
                        }
                    }
                }));
    }

    private void updateSettings(List<AppSetting> appSettings) {
        SharedPreferences.Editor editor = preferences.edit();
        for (AppSetting setting : appSettings) {
            editor.putString(setting.name, setting.value);
        }
        editor.apply();
    }

    public int getMinVideos() {
        int min = 2;
        try {
            min = Integer.parseInt(preferences.getString(SETTING_MIN_VIDEOS, "2"));
        } catch (NumberFormatException ignored) {
        }

        return min;
    }

    public int getMaxVideos() {
        int max = 4;
        try {
            max = Integer.parseInt(preferences.getString(SETTING_MAX_VIDEOS, "4"));
        } catch (NumberFormatException ignored) {
        }

        return max;
    }
}
