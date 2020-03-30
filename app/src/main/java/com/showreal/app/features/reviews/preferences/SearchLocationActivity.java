package com.showreal.app.features.reviews.preferences;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.LocationHelper;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.maps.model.GeocodeResult;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.databinding.ActivityPreferencesLocationBinding;
import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class SearchLocationActivity extends BaseActivity implements OnMapReadyCallback, SearchLocationViewModel.SearchLocationView, GoogleMap.OnCameraIdleListener {

    private final LatLng LONDON = new LatLng(51.5517, -0.1588);
    private ActivityPreferencesLocationBinding binding;
    private GoogleMap googleMap;
    private ReactiveLocationProvider locationProvider;
    private Subscription geoSubscription;
    private Profile profile;
    private String city = "";
    private double radius;
    private Settings settings;
    private Circle circle;
    private Subscription locationSubscription;
    private boolean disableLocationUpdate;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_preferences_location);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        int type = getAppComponent().accountHelper().getSearchLocationType();
        switch (type) {
            case AccountHelper.TypeHome:
                binding.locationOptions.check(R.id.option_home);
                break;
            case AccountHelper.TypeSearch:
                binding.locationOptions.check(R.id.option_search);
                break;
            case AccountHelper.TypeFollow:
                binding.locationOptions.check(R.id.option_follow);
                break;
        }

        profile = getIntent().getParcelableExtra("profile");

        locationProvider = new ReactiveLocationProvider(this);
        if ((savedInstanceState == null || !savedInstanceState.containsKey("profile"))) {
            getAppComponent().accountHelper().getSettings()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Settings>() {
                        @Override
                        public void call(Settings settings) {
                            SearchLocationActivity.this.profile = profile;
                            radius = profile.searchRadius;
                            SupportMapFragment mapFragment = SupportMapFragment.newInstance();

                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.map_frame, mapFragment)
                                    .commit();

                            binding.setViewModel(new SearchLocationViewModel(SearchLocationActivity.this, settings, profile));
                            binding.getViewModel().setType(getAppComponent().accountHelper().getSearchLocationType());
                            binding.distanceSeeker.setProgress((int) ((profile.searchRadius / SearchLocationViewModel.RADIUS_MAX) * 100));
                            mapFragment.getMapAsync(SearchLocationActivity.this);
                        }
                    });
        } else {
            profile = savedInstanceState.getParcelable("profile");
            settings = savedInstanceState.getParcelable("settings");
            radius = profile.searchRadius;
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_frame);
            mapFragment.getMapAsync(this);
            binding.getViewModel().setType(getAppComponent().accountHelper().getSearchLocationType());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (profile != null) {
            outState.putParcelable("profile", profile);
            outState.putDouble("radius", radius);
            outState.putString("city", city);
        }
        if (settings != null) {
            outState.putParcelable("settings", settings);
        }
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
        getAppComponent().accountHelper().setSearchLocationType(binding.getViewModel().locationType);
        Intent intent = getIntent();
        intent.putExtra("latitude", profile.searchLatitude);
        intent.putExtra("longitude", profile.searchLongitude);
        intent.putExtra("city", city);
        intent.putExtra("radius", profile.searchRadius);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setupLocation() {
        if (googleMap == null) {
            return;
        }

        setType(binding.getViewModel().locationType);
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
                    }
                });
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnCameraIdleListener(this);

        circle = this.googleMap.addCircle(new CircleOptions()
                .radius(profile.searchRadius / 2f)
                .fillColor(getResources().getColor(R.color.map_overlay))
                .strokeWidth(0)
                .center(googleMap.getCameraPosition().target));

        this.googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                circle.setCenter(googleMap.getCameraPosition().target);
            }
        });

        setupLocation();
    }

    @Override
    public void onSearch(String text) {
        showSearching(true);
        LocationHelper.geocodeObservable(this, text)
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
                        city = LocationHelper.getCity(geocodeResult);

                        disableLocationUpdate = true;
                        profile.searchLatitude = geocodeResult.latLng.latitude;
                        profile.searchLongitude = geocodeResult.latLng.longitude;
                        updateMap();
                    }
                });
    }

    @Override
    public void setType(@AccountHelper.LocationType int type) {
        if (googleMap == null) {
            return;
        }

        switch (type) {
            case AccountHelper.TypeHome:
                googleMap.getUiSettings().setAllGesturesEnabled(false);
                goHome();

                break;
            case AccountHelper.TypeSearch:
                googleMap.getUiSettings().setAllGesturesEnabled(true);
                goCurrent();
                break;
            case AccountHelper.TypeFollow:
                googleMap.getUiSettings().setAllGesturesEnabled(false);
                findMe();
                break;
        }
    }

    @Override
    public void updateRadius(double radius) {
        circle.setRadius(radius / 2f);
        profile.searchRadius = radius;
    }

    private void goCurrent() {
        if (locationSubscription != null && !locationSubscription.isUnsubscribed()) {
            locationSubscription.unsubscribe();
            locationSubscription = null;
        }
        if (profile.searchLatitude == 0 && profile.searchLongitude == 0) {
            goHome();
            return;
        }

        disableLocationUpdate = true;
        updateMap();
    }

    private void findMe() {
        RxPermissions.getInstance(this)
                .requestEach(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Action1<Permission>() {
                    @Override
                    public void call(Permission permission) {
                        if (permission.granted) {
                            getLocationUpdates();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            getAppComponent().accountHelper().setSearchLocationType(AccountHelper.TypeHome);
                            binding.circle.setVisibility(View.VISIBLE);
                            goHome();
                        }
                    }
                });
    }

    private void goHome() {
        if (locationSubscription != null && !locationSubscription.isUnsubscribed()) {
            locationSubscription.unsubscribe();
            locationSubscription = null;
        }
        if (profile.latitude == 0 && profile.longitude == 0) {
            goToLondon();
            return;
        }

        profile.searchLatitude = profile.latitude;
        profile.searchLongitude = profile.longitude;
        disableLocationUpdate = true;
        updateMap();
    }

    @Override
    public void onLocation() {
        RxPermissions.getInstance(this)
                .requestEach(Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe(new Action1<Permission>() {
                    @Override
                    public void call(Permission permission) {
                        if (permission.granted) {
                            getLocation();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            showPermissionRationale();
                        }
                    }
                });
    }

    private void goToLondon() {
        city = "London";
        profile.latitude = LONDON.latitude;
        profile.longitude = LONDON.longitude;
        profile.searchLatitude = LONDON.latitude;
        profile.searchLongitude = LONDON.longitude;
        binding.getViewModel().setSearchText(city);
        disableLocationUpdate = true;
        updateMap();
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
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Location location) {
                        profile.searchLatitude = location.getLatitude();
                        profile.searchLongitude = location.getLongitude();
                        disableLocationUpdate = true;
                        updateMap();
                    }
                });
    }

    @SuppressWarnings({"MissingPermission"})
    private void getLocationUpdates() {
        if (locationSubscription != null && !locationSubscription.isUnsubscribed()) {
            return;
        }
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationSubscription = locationProvider.getUpdatedLocation(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Location>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Location location) {
                        profile.searchLatitude = location.getLatitude();
                        profile.searchLongitude = location.getLongitude();
                        disableLocationUpdate = true;
                        updateMap();
                    }
                });
    }

    private void updateMap() {
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(profile.searchLatitude, profile.searchLongitude), 8)
        );
    }

    @Override
    public void onCameraIdle() {
        if (geoSubscription != null && !geoSubscription.isUnsubscribed()) {
            geoSubscription.unsubscribe();
        }

        if (!disableLocationUpdate) {
            profile.searchLatitude = googleMap.getCameraPosition().target.latitude;
            profile.searchLongitude = googleMap.getCameraPosition().target.longitude;
        }

        disableLocationUpdate = false;

        showSearching(true);
        String latlng = String.format("%f,%f", profile.searchLatitude, profile.searchLongitude);
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
                        city = LocationHelper.getCity(geocodeResult);
                        binding.getViewModel().setSearchText(city);
                    }
                });
    }

    private void showSearching(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.buttonSearch.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        binding.search.setEnabled(!show);
    }
}
