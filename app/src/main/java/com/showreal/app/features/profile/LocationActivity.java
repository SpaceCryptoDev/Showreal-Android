package com.showreal.app.features.profile;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.LocationHelper;
import com.showreal.app.data.maps.model.GeocodeResult;
import com.showreal.app.databinding.ActivityLocationBinding;
import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class LocationActivity extends BaseActivity implements OnMapReadyCallback, LocationViewModel.LocationView, GoogleMap.OnCameraIdleListener {

    public static final String EXTRA_CITY = "current_city";
    public static final String EXTRA_LAT_LNG = "location";
    private ActivityLocationBinding binding;
    private GoogleMap googleMap;
    private boolean locationFound;
    private ReactiveLocationProvider locationProvider;
    private String newCity;
    private Subscription geoSubscription;
    private LatLng newLocation;
    private boolean disableLocationUpdate;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_location);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.setViewModel(new LocationViewModel(this));

        binding.search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    binding.getViewModel().onSearch(v);
                    return true;
                }
                return false;
            }
        });

        newCity = getIntent().getStringExtra(EXTRA_CITY);
        newLocation = getIntent().getParcelableExtra(EXTRA_LAT_LNG);

        locationProvider = new ReactiveLocationProvider(this);

        SupportMapFragment mapFragment;
        if (savedInstanceState == null) {
            mapFragment = SupportMapFragment.newInstance();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.map_frame, mapFragment)
                    .commit();
        } else {
            locationFound = savedInstanceState.getBoolean("location_found", false);
            newCity = savedInstanceState.getString(EXTRA_CITY, newCity);
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_frame);
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_CITY, newCity);
        outState.putParcelable(EXTRA_LAT_LNG, newLocation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_location, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                finishWithResult();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void finishWithResult() {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_CITY, newCity);
        intent.putExtra(EXTRA_LAT_LNG, newLocation);
        setResult(RESULT_OK, intent);
        finish();
    }

    @SuppressWarnings({"MissingPermission"})
    private void setupLocation() {
        if (googleMap == null) {
            return;
        }

        if (locationFound) {
            return;
        }

        if (!TextUtils.isEmpty(newCity)) {
            if (newLocation == null || (newLocation.latitude == 0 && newLocation.longitude == 0)) {
                showSearching(true);
                LocationHelper.geocodeObservable(this, newCity)
                        .subscribe(new Subscriber<GeocodeResult>() {
                            @Override
                            public void onCompleted() {
                                showSearching(false);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(GeocodeResult geocodeResult) {
                                locationFound = true;
                                disableLocationUpdate = true;
                                updateMap(geocodeResult.latLng);
                                newCity = LocationHelper.getCity(geocodeResult);
                            }
                        });
                return;
            } else {
                locationFound = true;
                disableLocationUpdate = true;
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                newLocation, 13)
                );

                return;
            }
        }


        if (RxPermissions.getInstance(this).isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            getLocation();
        } else {
            onLocation();
        }

    }

    private void showPermissionRationale() {
        RxAlertDialog.with(this)
                .title(R.string.permission_location_rationale_title)
                .message(R.string.permission_location_rationale_msg)
                .positiveButton(R.string.button_ok)
                .create()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        onLocation();
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnCameraIdleListener(this);

        setupLocation();
    }

    @Override
    public void onSearch(String text) {
        showSearching(true);

        LocationHelper.geocodeObservable(this, text)
                .subscribe(new Subscriber<GeocodeResult>() {
                    @Override
                    public void onCompleted() {
                        showSearching(false);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(GeocodeResult geocodeResult) {
                        disableLocationUpdate = true;
                        updateMap(geocodeResult.latLng);
                        newCity = LocationHelper.getCity(geocodeResult);
                    }
                });
    }

    @Override
    public void onLocation() {
        RxPermissions.getInstance(this)
                .requestEach(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Action1<Permission>() {
                    @Override
                    public void call(Permission permission) {
                        if (permission.granted) {
                            binding.buttonLocation.setVisibility(View.VISIBLE);
                            getLocation();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            showPermissionRationale();
                        } else if (TextUtils.isEmpty(newCity)) {
                            binding.buttonLocation.setVisibility(View.GONE);
                        } else {
                            binding.buttonLocation.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void getLocation() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1);

        showSearching(true);
        locationProvider.getUpdatedLocation(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Location>() {
                    @Override
                    public void onCompleted() {
                        showSearching(false);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Location location) {
                        locationFound = true;
                        disableLocationUpdate = true;
                        updateMap(new LatLng(location.getLatitude(), location.getLongitude()));
                    }
                });
    }

    private void updateMap(LatLng latLng) {
        newLocation = latLng;
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        newLocation, 13)
        );
    }

    @Override
    public void onCameraIdle() {
        if (geoSubscription != null && !geoSubscription.isUnsubscribed()) {
            geoSubscription.unsubscribe();
        }
        showSearching(true);

        if (!disableLocationUpdate) {
            newLocation = new LatLng(googleMap.getCameraPosition().target.latitude, googleMap.getCameraPosition().target.longitude);
        }

        disableLocationUpdate = false;

        String latlng = String.format("%f,%f", newLocation.latitude, newLocation.longitude);

        geoSubscription = LocationHelper.reverseGeocodeObservable(this, latlng)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GeocodeResult>() {
                    @Override
                    public void onCompleted() {
                        showSearching(false);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(GeocodeResult geocodeResult) {
                        newCity = LocationHelper.getCity(geocodeResult);
                        binding.getViewModel().setSearchText(newCity);
                    }
                });
    }

    private void showSearching(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.buttonSearch.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        binding.search.setEnabled(!show);
    }

}
