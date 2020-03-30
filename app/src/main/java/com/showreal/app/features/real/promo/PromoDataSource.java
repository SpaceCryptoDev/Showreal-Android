package com.showreal.app.features.real.promo;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Profile;
import com.showreal.app.features.real.ReelDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import rx.Observable;
import rx.Subscriber;

public class PromoDataSource implements ReelDataSource {

    private final String JSON_PATH = "promo/profile.json";
    final AssetManager assets;
    final Gson gson;

    public PromoDataSource(Context context) {
        assets = context.getAssets();
        gson = TheDistanceApplication.getApplicationComponent(context).gson();
    }

    @Override
    public void reset() {

    }

    @Override
    public Observable<Profile> getData() {
        return Observable.create(new Observable.OnSubscribe<Profile>() {
            @Override
            public void call(Subscriber<? super Profile> subscriber) {
                try {
                    InputStream stream = assets.open(JSON_PATH);

                    Profile profile = gson.fromJson(new InputStreamReader(stream), Profile.class);

                    subscriber.onNext(profile);
                    subscriber.onCompleted();

                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
