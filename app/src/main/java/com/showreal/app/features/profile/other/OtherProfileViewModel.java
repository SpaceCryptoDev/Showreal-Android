package com.showreal.app.features.profile.other;

import android.databinding.Bindable;
import android.text.TextUtils;
import android.view.View;

import com.facebook.AccessToken;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Profile;
import com.showreal.app.features.profile.ProfileViewModel;
import com.showreal.app.features.reviews.ReviewViewModel;


public class OtherProfileViewModel extends ProfileViewModel {

    private final ReviewViewModel reviewViewModel;
    private int source;

    public void setSource(int source) {
        this.source = source;
    }

    interface OtherProfileView extends ProfileView {
        void report(Profile profile);
    }

    public OtherProfileViewModel(Profile profile, boolean isUser, OtherProfileView profileView, ReviewViewModel reviewViewModel) {
        super(profile, isUser, profileView);
        this.reviewViewModel = reviewViewModel;
    }

    @Bindable
    public ReviewViewModel getReviewViewModel() {
        return reviewViewModel;
    }

    public void onReport(View view) {
        ((OtherProfileView) profileView).report(getProfile());
    }

    @Bindable
    public boolean getKeepEnabled() {
        return source == OtherProfileActivity.SOURCE_REVIEWS;
    }

    @Bindable
    public float getKeepAlpha() {
        return getKeepEnabled() ? 1f : 0.5f;
    }

    @Bindable
    public int getFriendsVisibility() {
        AccessToken token = AccessToken.getCurrentAccessToken();
        return (token != null && !TextUtils.isEmpty(token.getToken()) && !TextUtils.isEmpty(getProfile().facebookId)) ? View.VISIBLE : View.GONE;
    }

    @Override
    public int getInstagramVisibility() {
        return !TextUtils.isEmpty(profile.instagramId) && !profile.photos.isEmpty() ? View.VISIBLE : View.GONE;
    }
}
