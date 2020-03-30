package com.showreal.app.features.profile.other;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Events;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.ProfileResponse;
import com.showreal.app.databinding.ActivityProfileOtherBinding;
import com.showreal.app.features.profile.instagram.OtherInstagramGalleryFragment;
import com.showreal.app.features.profile.other.facebook.MutualFriendsFragment;
import com.showreal.app.features.real.ReelPlayer;
import com.showreal.app.features.reviews.ProfileResponseUploader;
import com.showreal.app.features.reviews.ReviewViewModel;
import com.showreal.app.features.settings.ReportActivity;

import rx.functions.Action1;
import uk.co.thedistance.components.base.UploaderFactory;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;
import uk.co.thedistance.thedistancecore.TDSubscribers;


public class OtherProfileActivity extends BaseActivity implements OtherProfileViewModel.OtherProfileView, ReviewViewModel.ReviewView, UploadingPresenterView<Integer> {

    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_PROFILE = "profile";
    private ActivityProfileOtherBinding binding;
    private UploaderLoaderHelper<ResponseUploader> uploaderHelper;
    private ResponseUploader uploader;
    private ReelPlayer player;
    private int source;

    public static final int SOURCE_REVIEWS = 0;
    public static final int SOURCE_MATCHES = 1;
    public static final int SOURCE_POTENTIAL = 2;
    public static final int SOURCE_NOTIFICATIONS = 3;

    private enum Action {
        None,
        Cut,
        Chance,
        Keep
    }

    private Action action = Action.None;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_other);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        source = getIntent().getIntExtra(EXTRA_SOURCE, SOURCE_REVIEWS);

        Profile profile = getIntent().getParcelableExtra(EXTRA_PROFILE);

        setTitle(getString(R.string.title_profile, profile.firstName));

        showContent(profile);
        uploaderHelper = new UploaderLoaderHelper<>(this, new ResponseUploaderFactory(this, profile.id));
        getSupportLoaderManager().initLoader(1, null, uploaderHelper);
    }

    private void setupInstagram(Profile profile) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.instagram_fragment);

        if (TextUtils.isEmpty(profile.instagramId) || profile.photos.isEmpty()) {
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
            }

            return;
        }

        if (fragment == null) {
            fragment = OtherInstagramGalleryFragment.newInstance(profile);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.instagram_fragment, fragment)
                    .commit();
        } else {
            ((OtherInstagramGalleryFragment) fragment).setProfile(profile);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (player != null) {
            player.pause();
            player.clear();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        uploader = uploaderHelper.getUploader();
        uploader.onViewAttached(this);

        if (player != null) {
            player.ready();
        }
    }

    @Override
    public void edit() {
    }

    @Override
    public void connectInstagram() {
    }

    @Override
    public void disconnectInstagram() {
    }

    @Override
    public void editReal() {

    }

    @Override
    public void secondChance(Profile profile) {
        showShowRealMessage(R.string.button_second_chance)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (shown) {
                            return;
                        }
                        action = Action.Chance;
                        uploader.uploadContent(ProfileResponse.SecondChance);
                    }
                }));

    }

    @Override
    public void goforit(Profile profile) {

    }

    @Override
    public void keep(Profile profile) {
        showShowRealMessage(R.string.button_keep)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (shown) {
                            return;
                        }
                        action = Action.Keep;
                        uploader.uploadContent(ProfileResponse.Keep);
                    }
                }));
    }

    @Override
    public void cut(Profile profile) {
        showShowRealMessage(R.string.button_cut)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (shown) {
                            return;
                        }
                        action = Action.Cut;
                        uploader.uploadContent(ProfileResponse.Cut);
                    }
                }));
    }

    @Override
    public void open(Profile unused) {

    }

    @Override
    public void report(Profile profile) {
        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra(ReportActivity.EXTRA_PROFILE_ID, profile.id);
        startActivity(intent);
    }

    @Override
    public int getViewWidth() {
        return binding.getRoot().getWidth();
    }

    @Override
    public Context getImageContext() {
        return this;
    }

    public void showContent(Profile content) {
        OtherProfileViewModel viewModel = binding.getViewModel();
        if (viewModel == null) {
            binding.setViewModel(new OtherProfileViewModel(content, false, this, new ReviewViewModel(content, this)));
        } else {
            binding.getViewModel().setProfile(content);
        }
        binding.getViewModel().setSource(source);
        setupDetails(content);
        setupInstagram(content);
        setupReel(content);

        if (content.videos.isEmpty() && content.image != null) {
            Glide.with(this)
                    .load(content.image)
                    .centerCrop()
                    .crossFade()
                    .into(binding.real.questionOverlay);
        }

        setupFacebookMutual(content);
    }

    private void setupDetails(Profile profile) {
        if (binding.real.detailsStub.isInflated()) {
            return;
        }
        binding.real.detailsStub.setContainingBinding(binding);

        if (profile.city.length() <= 16) {
            binding.real.detailsStub.getViewStub().setLayoutResource(R.layout.profile_other_details);
        } else {
            binding.real.detailsStub.getViewStub().setLayoutResource(R.layout.profile_other_details_long);
        }
        binding.real.detailsStub.getViewStub().inflate();
    }

    private void setupFacebookMutual(Profile profile) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) {
            return;
        }
        String token = accessToken.getToken();
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(profile.facebookId)) {
            return;
        }
        MutualFriendsFragment fragment = MutualFriendsFragment.newInstance(profile.facebookId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.friends_fragment, fragment)
                .commit();
    }

    private void setupReel(Profile profile) {
        player = ReelPlayer.with(this, profile.getReelId())
                .audio(true)
                .videos(profile.videos)
                .binding(binding.real)
                .promo(profile.id == 6)
                .create();
        player.setup();
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
    public void uploadComplete(Integer response) {
        switch (action) {
            case Cut:
                getAppComponent().analytics().send(new AppboyEvent(Events.PROFILE_CUT));
                break;
            case Chance:
                getAppComponent().analytics().send(new AppboyEvent(Events.PROFILE_CHANCE));
                break;
            case Keep:
                getAppComponent().analytics().send(new AppboyEvent(Events.PROFILE_KEEP));
        }
        action = Action.None;
        finish();
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, this)) {
            return;
        }

        FabricHelper.logException(throwable);

        switch (action) {
            case Cut:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reviews)
                        .message(getString(R.string.alert_msg_error_review_cut))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
                break;
            case Chance:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reviews)
                        .message(getString(R.string.alert_msg_error_review_chance))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
                break;
            case Keep:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reviews)
                        .message(getString(R.string.alert_msg_error_review_keep))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
        }
        action = Action.None;
    }

    public static class ResponseUploader extends UploadingPresenter<ProfileResponse, Integer, ProfileResponseUploader, UploadingPresenterView<Integer>> {

        public ResponseUploader(ProfileResponseUploader dataUploader) {
            super(dataUploader);
        }

        public void setUserId(int id) {
            dataUploader.setUserId(id);
        }
    }

    public static class ResponseUploaderFactory implements UploaderFactory<ResponseUploader> {

        final Context context;
        final int id;

        public ResponseUploaderFactory(Context context, int id) {
            this.context = context;
            this.id = id;
        }

        @Override
        public ResponseUploader create() {
            return new ResponseUploader(new ProfileResponseUploader(TheDistanceApplication.getApplicationComponent(context).api(), id));
        }
    }
}
