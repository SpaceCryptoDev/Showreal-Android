package com.showreal.app.features.reviews.preferences;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.appboy.models.outgoing.AppboyProperties;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Events;
import com.showreal.app.data.LocationHelper;
import com.showreal.app.data.maps.model.GeocodeResult;
import com.showreal.app.data.maps.model.GeocodeResults;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.databinding.ActivityReviewPreferencesBinding;
import com.showreal.app.features.profile.EditProfileActivity;
import com.showreal.app.features.profile.ProfileFragment;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class PreferencesActivity extends BaseActivity implements PreferencesViewModel.PreferencesView, ContentLoadingPresenterView<Profile>, UploadingPresenterView<Profile> {

    private static final int RC_LOCATION = 0x0;
    public static final String EXTRA_FIRST_TIME = "first_time";
    private ActivityReviewPreferencesBinding binding;
    private PresenterLoaderHelper<ProfileFragment.ProfilePresenter> loaderHelper;
    private Settings settings;
    private ProfileFragment.ProfilePresenter presenter;
    private PresenterLoaderHelper<EditProfileActivity.ProfilePresenter> uploaderHelper;
    private EditProfileActivity.ProfilePresenter uploader;
    private Subscription geoSubscription;
    private boolean dontUpdate;
    private boolean firstTime;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_review_preferences);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firstTime = getIntent().getBooleanExtra(EXTRA_FIRST_TIME, false);

        loaderHelper = new PresenterLoaderHelper<>(this, new ProfileFragment.ProfilePresenterFactory(this));
        getSupportLoaderManager().initLoader(0, null, loaderHelper);

        uploaderHelper = new PresenterLoaderHelper<>(this, new ProfileUploaderFactory(this));
        getSupportLoaderManager().initLoader(1, null, uploaderHelper);

        getAppComponent().accountHelper().getSettings()
                .flatMap(new Func1<Settings, Observable<Profile>>() {
                    @Override
                    public Observable<Profile> call(Settings settings) {
                        PreferencesActivity.this.settings = settings;
                        return getAppComponent().accountHelper().getProfile();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Profile>() {
                    @Override
                    public void call(Profile profile) {
                        if (profile == null || profile.preferredAge == null) {
                            return;
                        }
                        binding.setViewModel(new PreferencesViewModel(profile, settings, PreferencesActivity.this));

                        if (!firstTime) {
                            switch (profile.interestedIn) {
                                case 1:
                                    binding.genderFemale.setChecked(true);
                                    break;
                                case 2:
                                    binding.genderBoth.setChecked(true);
                                    break;
                                case 0:
                                default:
                                    binding.genderMale.setChecked(true);
                            }
                            binding.getViewModel().genderSelected = true;
                        }


                        binding.rangeSlider.getThumb(0).setValue(profile.preferredAge.lower);
                        binding.rangeSlider.getThumb(1).setValue(profile.preferredAge.upper);
                        getLocationText(profile.searchLatitude, profile.searchLongitude);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);

        uploader = uploaderHelper.getPresenter();
        uploader.onViewAttached(this);
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
                update(binding.getViewModel().getProfile());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void changeLocation(String city, LatLng latLng) {
        Intent intent = new Intent(this, SearchLocationActivity.class);
        intent.putExtra("profile", binding.getViewModel().getProfile());
        startActivityForResult(intent, RC_LOCATION);
    }

    public void update(Profile profile) {
        if (!binding.getViewModel().isGenderSelected()) {
            RxAlertDialog.with(this)
                    .title(R.string.alert_title_profile_validation)
                    .message(R.string.alert_msg_profile_validation_interested)
                    .positiveButton(R.string.button_ok)
                    .subscribe();
            return;
        }
        uploader.uploadContent(profile);
    }

    @Override
    public Context getTextContext() {
        return this;
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {
        showUploading(show);
    }

    @Override
    public void showContent(Profile profile, boolean refresh) {
        if (dontUpdate) {
            dontUpdate = false;
            return;
        }
        if (binding.getViewModel() == null) {
            binding.setViewModel(new PreferencesViewModel(profile, settings, PreferencesActivity.this));
        } else {
            binding.getViewModel().setProfile(profile);
        }
        getLocationText(profile.searchLatitude, profile.searchLongitude);
        binding.rangeSlider.getThumb(0).setValue(profile.preferredAge.lower);
        binding.rangeSlider.getThumb(1).setValue(profile.preferredAge.upper);
    }

    @Override
    public void showUploading(boolean show) {
        if (show) {
            binding.loading.loadingLayout.show();
        } else {
            binding.loading.loadingLayout.hide();
        }
    }

    @Override
    public void uploadComplete(Profile response) {
        sendEvent(response);
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void sendEvent(Profile response) {

        AppboyProperties properties = new AppboyProperties();
        JsonObject age = new JsonObject();
        age.add("lower", new JsonPrimitive(response.preferredAge.lower));
        age.add("upper", new JsonPrimitive(response.preferredAge.upper));

        properties.addProperty("preferred_age", age.toString());
        properties.addProperty("interested_in", response.interestedIn);
        properties.addProperty("search_radius", response.searchRadius);
        properties.addProperty("search_latitude", response.searchLatitude);
        properties.addProperty("search_longitude", response.searchLongitude);

        getAppComponent().analytics().send(new AppboyEvent(Events.REVIEWS_PREFERENCES, properties));
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, this)) {
            return;
        }
        RxAlertDialog.with(this)
                .title(R.string.alert_title_error_oops)
                .message(R.string.alert_msg_preferences)
                .positiveButton(R.string.button_ok)
                .subscribe();
    }

    private void getLocationText(double latitude, double longitude) {
        if (geoSubscription != null && !geoSubscription.isUnsubscribed()) {
            geoSubscription.unsubscribe();
        }

        String latlng = String.format("%f,%f", latitude, longitude);
        geoSubscription = LocationHelper.reverseGeocodeObservable(this, latlng)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GeocodeResult>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(GeocodeResult geocodeResult) {
                        binding.getViewModel().setSearchCity(LocationHelper.getCity(geocodeResult));
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_LOCATION && resultCode == RESULT_OK) {
            dontUpdate = true;
            double lat = data.getDoubleExtra("latitude", 0);
            double lon = data.getDoubleExtra("longitude", 0);
            double radius = data.getDoubleExtra("radius", 0);
            String city = data.getStringExtra("city");
            binding.getViewModel().setSearchCity(city, new LatLng(lat, lon));
            binding.getViewModel().setSearchRadius(radius);

        }
    }

    private static class ProfileUploaderFactory implements PresenterFactory<EditProfileActivity.ProfilePresenter> {
        final Context context;

        public ProfileUploaderFactory(Context context) {
            this.context = context;
        }

        @Override
        public EditProfileActivity.ProfilePresenter create() {
            return new EditProfileActivity.ProfilePresenter(new PreferencesUploader(context, TheDistanceApplication.getApplicationComponent(context).api()));
        }
    }
}
