package com.showreal.app.data.maps;


import com.showreal.app.data.maps.model.GeocodeResults;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface GoogleMapsApi {

    @GET("geocode/json")
    Observable<GeocodeResults> reverseGeocode(@Query("latlng") String latlng, @Query("key") String apiKey);

    @GET("geocode/json")
    Observable<GeocodeResults> geocode(@Query("address") String address, @Query("key") String apiKey);
}
