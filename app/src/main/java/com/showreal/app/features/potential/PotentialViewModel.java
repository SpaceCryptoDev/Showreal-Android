package com.showreal.app.features.potential;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;

import com.showreal.app.data.model.Liked;
import com.showreal.app.data.model.Profile;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;


public class PotentialViewModel extends BaseObservable {

    private final Profile profile;
    private final PotentialView potentialView;
    private final static Calendar CALENDAR = Calendar.getInstance();
    private final static Calendar BIRTH_CALENDAR = Calendar.getInstance();

    public Profile getProfile() {
        return profile;
    }

    interface PotentialView {
        void openPotential(Profile profile);

        void chancePotential(Profile profile);

        void cutPotential(Profile profile);
    }

    public PotentialViewModel(Liked liked, PotentialView potentialView) {
        this.profile = liked.profile;
        this.potentialView = potentialView;
    }

    public void onClick(View view) {
        potentialView.openPotential(profile);
    }

    @Bindable
    public String getTitle() {
        try {
            Date birth = Profile.API_DATE_FORMAT.parse(profile.dateOfBirth);
            BIRTH_CALENDAR.setTime(birth);
            int age = CALENDAR.get(Calendar.YEAR) - BIRTH_CALENDAR.get(Calendar.YEAR);
            return String.format("%s, %d", profile.firstName, age);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return profile.firstName;
    }

    @Bindable
    public String getLocation() {
        return profile.city;
    }

    @Bindable
    public String getLastOnline() {
        long time = profile.lastOnline.getTime();
        return time < DateUtils.MINUTE_IN_MILLIS ? "Just now" : DateUtils.getRelativeTimeSpanString(time, new Date().getTime(), DateUtils.MINUTE_IN_MILLIS).toString();
    }

    @Bindable
    public String getImage() {
        return profile.image;
    }

    public void onCut(View view) {
        potentialView.cutPotential(profile);
    }

    public void onChance(View view) {
        potentialView.chancePotential(profile);
    }

    @Bindable
    public SpannableString getChanceText() {
        SpannableString spannableString = new SpannableString("2nd\nChance");
        spannableString.setSpan(new SuperscriptSpan(), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableString;
    }
}
