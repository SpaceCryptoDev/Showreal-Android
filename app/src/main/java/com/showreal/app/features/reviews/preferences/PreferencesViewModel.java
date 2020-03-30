package com.showreal.app.features.reviews.preferences;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RadioGroup;

import com.google.android.gms.maps.model.LatLng;
import com.showreal.app.BR;
import com.showreal.app.R;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;

import io.apptik.widget.MultiSlider;

public class PreferencesViewModel extends BaseObservable {

    private Profile profile;
    private final Settings settings;
    private final PreferencesView preferencesView;
    private String searchCity = "";

    public PreferencesViewModel(Profile profile, Settings settings, PreferencesView preferencesView) {
        this.profile = profile;
        this.settings = settings;
        this.preferencesView = preferencesView;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
        notifyChange();
    }

    public Profile getProfile() {
        return profile;
    }

    interface PreferencesView {

        void changeLocation(String city, LatLng latLng);

        Context getTextContext();
    }

    @Bindable
    public SpannableString getLocation() {
        String name = (profile.searchLatitude == profile.latitude && profile.searchLongitude == profile.longitude) ? "Home" : searchCity;
        String locationString = String.format("%s    +%s", name, getSearchDistance());
        SpannableString location = new SpannableString(locationString);

        location.setSpan(new ForegroundColorSpan(ContextCompat.getColor(preferencesView.getTextContext(), R.color.black54)), name.length(), locationString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return location;
    }

    public boolean isGenderSelected() {
        return genderSelected;
    }

    @Bindable
    public Drawable getLocationDrawable() {
        return (profile.searchLatitude == profile.latitude && profile.searchLongitude == profile.longitude) ? ContextCompat.getDrawable(preferencesView.getTextContext(), R.drawable.ic_home) : null;
    }

    @Bindable
    public String getAgeRange() {
        return String.format("%d - %d", profile.preferredAge.lower, profile.preferredAge.upper);
    }

    private String getSearchDistance() {
        if (profile.searchRadius > SearchLocationViewModel.RADIUS_MAX) {
            profile.searchRadius = SearchLocationViewModel.RADIUS_MAX;
        }
        int distance = (int) (settings == null || settings.radius_unit == Settings.RADIUS_KM ? Math.ceil(profile.searchRadius * 0.001) : Math.ceil(profile.searchRadius * 0.000621371192));
        return (settings == null || settings.radius_unit == Settings.RADIUS_KM) ? String.format("%d km", distance > 80 ? 80 : distance) : String.format("%d miles", distance);
    }

    public void setSearchCity(String searchCity, LatLng latLng) {
        this.searchCity = searchCity;
        profile.searchLatitude = latLng.latitude;
        profile.searchLongitude = latLng.longitude;
        notifyPropertyChanged(BR.location);
        notifyPropertyChanged(BR.locationDrawable);
    }

    public void setSearchCity(String searchCity) {
        this.searchCity = searchCity;
        notifyPropertyChanged(BR.location);
    }

    public void setSearchRadius(double meters) {
        profile.searchRadius = meters;
        notifyPropertyChanged(BR.location);
    }

    public MultiSlider.OnThumbValueChangeListener rangeListener = new MultiSlider.OnThumbValueChangeListener() {
        @Override
        public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
            if (thumbIndex == 0) {
                profile.preferredAge.lower = value;
            } else {
                profile.preferredAge.upper = value;
            }
            notifyPropertyChanged(BR.ageRange);
        }
    };

    protected boolean genderSelected;
    public RadioGroup.OnCheckedChangeListener onGenderChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.gender_male:
                    profile.interestedIn = 0;
                    genderSelected = true;
                    return;
                case R.id.gender_female:
                    profile.interestedIn = 1;
                    genderSelected = true;
                    return;
                case R.id.gender_both:
                    profile.interestedIn = 2;
                    genderSelected = true;
                    return;
            }
        }
    };

    public void onLocation(View view) {
        preferencesView.changeLocation(profile.city, new LatLng(profile.latitude, profile.longitude));
    }
}
