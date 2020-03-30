package com.showreal.app.features.profile;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableField;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.google.android.gms.maps.model.LatLng;
import com.showreal.app.BR;
import com.showreal.app.R;
import com.showreal.app.data.model.Profile;
import com.showreal.app.features.onboarding.signup.SignUpProfileViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class EditProfileViewModel extends BaseObservable {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM, yyyy");
    private static final SimpleDateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    final Profile profile;
    final EditProfileView profileView;
    private
    @Units
    int units = UNITS_FEET;
    private ObservableField<String> heightString = new ObservableField<>();
    private ObservableField<String> heightStringCM = new ObservableField<>();
    public ObservableField<String> dateOfBirth = new ObservableField<>();

    public static final int UNITS_FEET = 1;
    public static final int UNITS_CM = 0;

    @IntDef({UNITS_FEET, UNITS_CM})
    public @interface Units {
    }

    interface EditProfileView extends SignUpProfileViewModel.SignUpProfileView {

        void changeDob();
        void clearErrors();
    }


    public EditProfileViewModel(Profile profile, EditProfileView profileView) {
        this.profile = profile;
        this.profileView = profileView;

        if (!TextUtils.isEmpty(profile.dateOfBirth)) {
            try {
                Date date = API_DATE_FORMAT.parse(profile.dateOfBirth);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                setDob(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        setHeight((int) profile.height);
    }

    public void setUnits(@Units int units) {
        this.units = units;
    }

    @Bindable
    public int getCheckedGender() {
        switch (profile.gender) {
            case 1:
                return R.id.gender_female;
            case 0:
            default:
                return R.id.gender_male;
        }
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
                    profile.gender = 0;
                    return;
                case R.id.gender_female:
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

    @Bindable
    public String getFirstName() {
        return profile.firstName;
    }

    @Bindable
    public String getLastName() {
        return profile.lastName;
    }

    @Bindable
    public String getEmail() {
        return profile.email;
    }

    public void onDobClick(View view) {
        profileView.changeDob();
    }

    public void setDob(int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);

        dateOfBirth.set(DATE_FORMAT.format(calendar.getTime()));
        profile.dateOfBirth = API_DATE_FORMAT.format(calendar.getTime());
    }

    public Calendar getDob() {
        if (TextUtils.isEmpty(profile.dateOfBirth)) {
            return null;
        }
        try {
            Date date = API_DATE_FORMAT.parse(profile.dateOfBirth);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        } catch (ParseException e) {
            return null;
        }
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

    public void onFirstNameChanged(CharSequence sequence, int start, int before, int count) {
        profileView.clearErrors();
        profile.firstName = sequence.toString();
    }

    public void onLastNameChanged(CharSequence sequence, int start, int before, int count) {
        profileView.clearErrors();
        profile.lastName = sequence.toString();
    }

    public void onEmailChanged(CharSequence sequence, int start, int before, int count) {
        profileView.clearErrors();
        profile.email = sequence.toString();
    }

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

    public void onImage(View view) {
        profileView.changeImage();
    }
}
