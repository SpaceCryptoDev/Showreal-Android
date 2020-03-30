package com.showreal.app.features.reviews.preferences;

import android.databinding.Bindable;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.showreal.app.BR;
import com.showreal.app.R;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.features.profile.LocationViewModel;

public class SearchLocationViewModel extends LocationViewModel {

    private static final double RADIUS_MIN = 1609.344;
    static final double RADIUS_MAX = 80467.2;
    private final Settings settings;
    @AccountHelper.LocationType
    int locationType = AccountHelper.TypeHome;
    private double radiusMeters = RADIUS_MAX;

    public void setType(int locationType) {
        this.locationType = locationType;
        notifyPropertyChanged(BR.checkedType);
        notifyPropertyChanged(BR.searchVisibility);
    }

    interface SearchLocationView extends LocationView {
        void setType(@AccountHelper.LocationType int type);
        void updateRadius(double radius);
    }

    public SearchLocationViewModel(SearchLocationView locationView, Settings settings, Profile profile) {
        super(locationView);
        this.settings = settings;
        this.radiusMeters = profile.searchRadius;
    }

    @Bindable
    public String getDistance() {
        int distance = (int) (settings == null || settings.radius_unit == Settings.RADIUS_KM ? Math.ceil(radiusMeters * 0.001) : Math.ceil(radiusMeters * 0.000621371192));

        return (settings == null || settings.radius_unit == Settings.RADIUS_KM) ? String.format("+%d km", distance > 80 ? 80 : distance) : String.format("+%d miles", distance);
    }

    public SeekBar.OnSeekBarChangeListener onRadiusChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            setRadius(seekBar.getProgress());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void setRadius(int progress) {
        if (progress == 0) {
            radiusMeters = RADIUS_MIN;
        } else {
            float percent = progress / 100f;
            radiusMeters = (percent * (RADIUS_MAX - RADIUS_MIN)) + RADIUS_MIN;
        }
        ((SearchLocationView) locationView).updateRadius(radiusMeters);
        notifyPropertyChanged(BR.distance);
    }

    @Bindable
    public int getSearchVisibility() {
        return locationType == AccountHelper.TypeSearch ? View.VISIBLE : View.GONE;
    }

    @Bindable
    public int getCheckedType() {
        switch (locationType) {
            case AccountHelper.TypeFollow:
                return R.id.option_follow;
            case AccountHelper.TypeSearch:
                return R.id.option_search;
            case AccountHelper.TypeHome:
            default:
                return R.id.option_home;
        }
    }

    public RadioGroup.OnCheckedChangeListener onTypeChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.option_home:
                    locationType = AccountHelper.TypeHome;
                    break;
                case R.id.option_search:
                    locationType = AccountHelper.TypeSearch;
                    break;
                case R.id.option_follow:
                    locationType = AccountHelper.TypeFollow;
                    break;
            }
            ((SearchLocationView) locationView).setType(locationType);
            notifyPropertyChanged(BR.searchVisibility);
        }
    };

}
