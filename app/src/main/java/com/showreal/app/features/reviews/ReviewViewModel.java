package com.showreal.app.features.reviews;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;

import com.showreal.app.R;
import com.showreal.app.data.model.Profile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class ReviewViewModel extends BaseObservable {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public final Profile profile;
    private final ReviewView reelView;
    private final int[] IMAGES = new int[]{R.drawable.pattern_1, R.drawable.pattern_2, R.drawable.pattern_3, R.drawable.pattern_4
            , R.drawable.pattern_4, R.drawable.pattern_5, R.drawable.pattern_6, R.drawable.pattern_7};
    private static final Random RANDOM = new Random();

    public int getId() {
        return profile.id;
    }

    public interface ReviewView {
        void secondChance(Profile profile);

        void keep(Profile profile);

        void goforit(Profile profile);

        void cut(Profile profile);

        int getViewWidth();

        Context getImageContext();

        void open(Profile profile);
    }

    public ReviewViewModel(Profile profile, ReviewView reelView) {
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
//        SpannableString spannableString = new SpannableString("2nd\nChance");
        SpannableString spannableString = new SpannableString("Maybe");
//        spannableString.setSpan(new SuperscriptSpan(), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//        spannableString.setSpan(new RelativeSizeSpan(0.8f), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    public void onChance(View view) {
        reelView.secondChance(profile);
    }

    public void onKeep(View view) {
        reelView.keep(profile);
    }

    public void onGoforit(View view) {
        reelView.goforit(profile);
    }

    public void onCut(View view) {
        reelView.cut(profile);
    }

    public void onOpen(View view) {
        reelView.open(profile);
    }

    @Bindable
    public Drawable getShapeOne() {
        return randomShape();
    }

    @Bindable
    public Drawable getShapeTwo() {
        return randomShape();
    }

    @Bindable
    public Drawable getShapeThree() {
        return randomShape();
    }

    private Drawable randomShape() {
        int rand = RANDOM.nextInt(7);
        return ContextCompat.getDrawable(reelView.getImageContext(), IMAGES[rand]);
    }
}
