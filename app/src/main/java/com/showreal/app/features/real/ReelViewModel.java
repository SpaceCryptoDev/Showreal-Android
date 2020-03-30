package com.showreal.app.features.real;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;

import com.showreal.app.data.model.Profile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ReelViewModel extends BaseObservable {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final Profile profile;
    private final ReelView reelView;

    interface ReelView {
        void secondChange(Profile profile);

        void keep(Profile profile);

        void cut(Profile profile);

        void goforit(Profile profile);
    }

    public ReelViewModel(Profile profile, ReelView reelView) {
        this.profile = profile;
        this.reelView = reelView;
    }

    @Bindable
    public String getName() {
        String birth = profile.dateOfBirth;

        int years = 0;
        try {
            Date date = DATE_FORMAT.parse(birth);
            Calendar calendar = Calendar.getInstance();
            int current = calendar.get(Calendar.YEAR);
            calendar.setTime(date);
            years = current - calendar.get(Calendar.YEAR);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return String.format("%s, %d", profile.firstName, years);
    }

    @Bindable
    public String getCity() {
        return profile.city;
    }

    @Bindable
    public String getImage() {
        return profile.image;
    }

    @Bindable
    public SpannableString getChanceText() {
        SpannableString spannableString = new SpannableString("2nd\nChance");
        spannableString.setSpan(new SuperscriptSpan(), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    public void onChance(View view) {
        reelView.secondChange(profile);
    }

    public void onKeep(View view) {
        reelView.keep(profile);
    }

    public void onCut(View view) {
        reelView.cut(profile);
    }
}
