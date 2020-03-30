package com.showreal.app.features.onboarding.signup;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableField;
import android.support.annotation.IntDef;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.google.android.gms.maps.model.LatLng;
import com.showreal.app.R;
import com.showreal.app.BR;
import com.showreal.app.data.model.Profile;


public class SignUpProfileViewModel extends BaseObservable {

    final Profile profile;
    final SignUpProfileView profileView;
    private
    @Units
    int units = UNITS_FEET;
    private ObservableField<String> heightString = new ObservableField<>();
    private ObservableField<String> heightStringCM = new ObservableField<>();

    public static final int UNITS_FEET = 1;
    public static final int UNITS_CM = 0;
    private boolean genderSelected = false;

    @IntDef({UNITS_FEET, UNITS_CM})
    public @interface Units {
    }

    public interface SignUpProfileView {

        void changeLocation(String city, LatLng latLng);

        void proceed(Profile profile);

        void changeImage();
    }


    public SignUpProfileViewModel(Profile profile, SignUpProfileView profileView) {
        this.profile = profile;
        this.profileView = profileView;

        setHeight((int) profile.height);

    }

    public boolean isGenderSelected() {
        return genderSelected;
    }

    @Bindable
    public int getCheckedUnits() {
        switch (units) {
            case UNITS_CM:
                return R.id.height_cm;
            case UNITS_FEET:
            default:
                return R.id.height_feet;
        }
    }

    @Bindable
    public String getLocation() {
        return profile.city;
    }

    public RadioGroup.OnCheckedChangeListener onGenderChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.gender_male:
                    genderSelected = true;
                    profile.gender = 0;
                    return;
                case R.id.gender_female:
                    genderSelected = true;
                    profile.gender = 1;
                    return;
            }
        }
    };

    public RadioGroup.OnCheckedChangeListener onUnitsChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.height_feet:
                    units = UNITS_FEET;
                    break;
                case R.id.height_cm:
                    units = UNITS_CM;
                    break;
            }
            notifyPropertyChanged(BR.heightString);
        }
    };

    @Bindable
    public ObservableField<String> getHeightString() {
        switch (units) {
            case UNITS_FEET:
                return heightString;
            case UNITS_CM:
                return heightStringCM;
        }
        return null;
    }

    public SeekBar.OnSeekBarChangeListener onHeightChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            setHeight(seekBar.getProgress());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void setHeight(int progress) {
        profile.height = progress;
        heightStringCM.set(String.format("%dcm", progress));

        progress = (int) (progress / 2.54f);
        int feet = (int) Math.floor(progress / 12f);
        int inches = progress - (feet * 12);
        heightString.set(String.format("%dft %din", feet, inches));
    }

    public void setCity(String city) {
        profile.city = city;
        notifyPropertyChanged(BR.location);
    }

    public void setLocation(LatLng location) {
        profile.latitude = location.latitude;
        profile.longitude = location.longitude;
    }

    public void setImage(String image) {
        profile.newImage = image;
    }

    public void onLocation(View view) {
        profileView.changeLocation(profile.city, new LatLng(profile.latitude, profile.longitude));
    }

    public void onArrow(View view) {
        profileView.proceed(profile);
    }

    public void onImage(View view) {
        profileView.changeImage();
    }
}
