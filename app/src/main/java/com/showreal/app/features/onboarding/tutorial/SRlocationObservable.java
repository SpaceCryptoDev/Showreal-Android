package com.showreal.app.features.onboarding.tutorial;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import pl.charmas.android.reactivelocation.observables.BaseLocationObservable;
import rx.Observable;
import rx.Observer;


public class SRlocationObservable extends BaseLocationObservable<Location> {

    public static Observable<Location> createObservable(Context ctx) {
        return Observable.create(new SRlocationObservable(ctx));
    }

    protected SRlocationObservable(Context ctx) {
        super(ctx);
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, Observer<? super Location> observer) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        observer.onNext(location);
        observer.onCompleted();
    }
}
