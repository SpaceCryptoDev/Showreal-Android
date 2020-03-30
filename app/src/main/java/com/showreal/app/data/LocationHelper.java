package com.showreal.app.data;

import android.content.Context;

import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.maps.model.GeocodeResult;
import com.showreal.app.data.maps.model.GeocodeResults;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.text.StringUtils;

public class LocationHelper {

    public static Observable<GeocodeResult> geocodeObservable(Context context, String address) {
        return TheDistanceApplication.getApplicationComponent(context).mapsApi().geocode(address, context.getResources().getString(R.string.maps_api_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<GeocodeResults, List<GeocodeResult>>() {
                    @Override
                    public List<GeocodeResult> call(GeocodeResults geocodeResults) {
                        return geocodeResults.results;
                    }
                })
                .flatMapIterable(new Func1<List<GeocodeResult>, Iterable<GeocodeResult>>() {
                    @Override
                    public Iterable<GeocodeResult> call(List<GeocodeResult> geocodeResults) {
                        return geocodeResults;
                    }
                })
                .first();
    }

    public static Observable<GeocodeResult> reverseGeocodeObservable(Context context, String latlng) {
        return TheDistanceApplication.getApplicationComponent(context).mapsApi().reverseGeocode(latlng, context.getResources().getString(R.string.maps_api_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<GeocodeResults, List<GeocodeResult>>() {
                    @Override
                    public List<GeocodeResult> call(GeocodeResults geocodeResults) {
                        return geocodeResults.results;
                    }
                })
                .flatMapIterable(new Func1<List<GeocodeResult>, Iterable<GeocodeResult>>() {
                    @Override
                    public Iterable<GeocodeResult> call(List<GeocodeResult> geocodeResults) {
                        return geocodeResults;
                    }
                })
                .first();
    }

    public static String getCity(GeocodeResult result) {
        String city = result.getCity();
        String postalTown = result.getPostalTown();
        String area = result.getArea();
        String neighborhood = result.getNeighborhood();
        String state = result.getState();

        Set<String> components = new LinkedHashSet<>();

        if (neighborhood != null) {
            components.add(neighborhood);
        }
        if (area != null) {
            components.add(area);
        }
        if (city != null) {
            components.add(city);
        }
        if (postalTown != null) {
            components.add(postalTown);
        }

        if (components.isEmpty() && state != null) {
            components.add(state);
        }

        return StringUtils.join(new ArrayList<>(components), ", ");
    }
}
