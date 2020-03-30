package com.showreal.app.features.profile;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;

import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Reel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class ProfileViewModel extends BaseObservable {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    protected Profile profile;
    private final boolean isUser;
    protected final ProfileView profileView;
    private Reel reel;

    public void setProfile(Profile profile) {
        this.profile = profile;
        this.reel = (profile.videos == null || profile.videos.isEmpty()) ? null : profile.videos.get(0).reel;
        notifyChange();
    }

    public Profile getProfile() {
        return profile;
    }

    public interface ProfileView {

        void edit();

        Context getImageContext();

        void connectInstagram();

        void disconnectInstagram();

        void editReal();
    }


    public ProfileViewModel(Profile profile, boolean isUser, ProfileView profileView) {
        this.profile = profile;
        this.reel = (profile.videos == null || profile.videos.isEmpty()) ? null : profile.videos.get(0).reel;
        this.isUser = isUser;
        this.profileView = profileView;
    }

    @Bindable
    public String getHeight() {
        int height = (int) (profile.height / 2.54f);
        int feet = (int) Math.floor(height / 12f);
        int inches = height - (feet * 12);
        return String.format("%dft %din", feet, inches);
    }

    @Bindable
    public String getCity() {
        return profile.city;
    }

    @Bindable
    public String getName() {
        String birth = profile.dateOfBirth;

        int years = 0;
        try {
            Date date = DATE_FORMAT.parse(birth);
            years = getAge(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return String.format("%s, %d", profile.firstName, years);
    }

    @Bindable
    public SpannableString getChancesLabelText() {
        SpannableString spannableString = new SpannableString("2nd ");
        spannableString.setSpan(new SuperscriptSpan(), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    @Bindable
    public String getViewCount() {
        if (reel == null) {
            return "0";
        }
        return String.valueOf(reel.viewCount);
    }

    @Bindable
    public String getChancesCount() {
        return String.valueOf(profile.notNowCount);
    }

    private int getAge(Date date) {
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        dob.setTime(date);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }

    @Bindable
    public String getGender() {
        return profile.gender == 0 ? "Male" : "Female";
    }

    @Bindable
    public Drawable getGenderIcon() {
        return ContextCompat.getDrawable(profileView.getImageContext(), profile.gender == 0 ? R.drawable.ic_male : R.drawable.ic_female);
    }

    @Bindable
    public String getImage() {
        return profile.image;
    }

    public void onEdit(View view) {
        profileView.edit();
    }

    public void onEditReal(View view) {
        profileView.editReal();
    }

    @Bindable
    public int getInstagramButtonVisibility() {
        String token = TheDistanceApplication.getApplicationComponent(profileView.getImageContext()).accountHelper().getInstagramToken();
        return TextUtils.isEmpty(profile.instagramId) || TextUtils.isEmpty(token) ? View.VISIBLE : View.GONE;
    }

    public void onInstagram(View view) {
        profileView.connectInstagram();
    }

    @Bindable
    public int getInstagramVisibility() {
        String token = TheDistanceApplication.getApplicationComponent(profileView.getImageContext()).accountHelper().getInstagramToken();
        return !TextUtils.isEmpty(profile.instagramId) && !TextUtils.isEmpty(token) ? View.VISIBLE : View.GONE;
    }

    public void onDisconnect(View view) {
        profileView.disconnectInstagram();
    }

    @Bindable
    public String getInstagramTitle() {
        return isUser ? profileView.getImageContext().getResources().getString(R.string.instagram_user) :
                profileView.getImageContext().getResources().getString(R.string.instagram_other, profile.firstName);
    }

    @Bindable
    public String getEditButtonText() {
        return profileView.getImageContext().getString(profile.hasShowReal(profileView.getImageContext()) ? R.string.edit_hint : R.string.create_hint);
    }
}
