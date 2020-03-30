package com.showreal.app.features.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.github.florent37.tutoshowcase.TutoShowcase;
import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.model.InstagramUser;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.FragmentProfileBinding;
import com.showreal.app.features.onboarding.signup.SignUpProfileUploader;
import com.showreal.app.features.profile.instagram.InstagramGalleryFragment;
import com.showreal.app.features.profile.instagram.InstagramLoginActivity;
import com.showreal.app.features.real.ReelPlayer;
import com.showreal.app.features.real.myreal.MyRealActivity;
import com.showreal.app.features.settings.SettingsActivity;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.base.UploaderFactory;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenter;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;
import uk.co.thedistance.components.contentloading.DataSource;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;


public class ProfileFragment extends BaseFragment implements ProfileViewModel.ProfileView, ContentLoadingPresenterView<Profile>, UploadingPresenterView<Profile>, ProfileReelViewModel.ReelView {

    private static final String PREFS_FIELD_PROFILE_HELP = "show_profile_help";
    private static final int RC_INSTAGRAM = 0x6;
    private static final int RC_EDIT_REAL = 0x8;
    final int RC_EDIT = 0x4;
    private FragmentProfileBinding binding;
    private PresenterLoaderHelper<ProfilePresenter> loaderHelper;
    private ProfilePresenter presenter;
    private UploaderLoaderHelper<ProfileUploadingPresenter> uploaderHelper;
    private ProfileUploadingPresenter uploader;
    private ReelPlayer player;
    private boolean showingHelp;

    @Override
    protected String getScreenName() {
        return Screens.PROFILE;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false);

        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
        getActivity().setTitle("Profile");

        setHasOptionsMenu(true);

        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new ProfilePresenterFactory(getActivity()));
        getLoaderManager().initLoader(0, null, loaderHelper);

        uploaderHelper = new UploaderLoaderHelper<>(getActivity(), new ProfileUploaderFactory(getActivity()));
        getLoaderManager().initLoader(1, null, uploaderHelper);

        binding.reelView.setViewModel(new ProfileReelViewModel(this));

        getAppComponent().accountHelper().getProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Profile>() {
                    @Override
                    public void call(Profile profile) {
                        if (profile != null) {
                            binding.setViewModel(new ProfileViewModel(profile, true, ProfileFragment.this));
                            setupDetails(profile);
                        }
                    }
                });


        return binding.getRoot();
    }

    private void showHelp() {
        if (showingHelp) {
            return;
        }
        showingHelp = true;

        showHelp(0);
    }

    private int helpScrollPosition = -1;

    private void showHelp(final int page) {
        if (page > 1) {
            showingHelp = false;
            if (helpScrollPosition != -1) {
                binding.scrollView.smoothScrollBy(0, -helpScrollPosition);
            }

            getAppComponent().preferences().edit().putBoolean(PREFS_FIELD_PROFILE_HELP, false).apply();
            return;
        }
        int layoutId = 0;
        View highlight = null;
        switch (page) {
            case 0:
                layoutId = R.layout.tuto_reviews_edit;
                highlight = binding.buttonEdit;
                break;
            case 1:
                layoutId = R.layout.tuto_reviews_feedback;
                highlight = binding.helpFeedback;
                break;
        }

        boolean delay = false;
        if (helpScrollPosition == -1) {
            float bottom = binding.getRoot().getBottom() - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 152f, getResources().getDisplayMetrics());

            Point childOffset = new Point();
            getDeepChildOffset(binding.scrollView, highlight.getParent(), highlight, childOffset);

            int viewBottom = highlight.getHeight() + childOffset.y;

            if (viewBottom > bottom) {
                delay = true;
                helpScrollPosition = (int) (viewBottom - bottom);
                binding.scrollView.smoothScrollBy(0, helpScrollPosition);

            } else {
                helpScrollPosition = -1;
            }
        }

        final int finalLayoutId = layoutId;
        final View finalHighlight = highlight;
        final int marginBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56f, getResources().getDisplayMetrics());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final TutoShowcase showcase = TutoShowcase.from(getActivity())
                        .setContentView(finalLayoutId)
                        .setBackgroundColor(getResources().getColor(R.color.black87))
                        .on(finalHighlight)
                        .addCircle(1.2f)
                        .show();

                showcase.onClickContentView(R.id.button_next, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showcase.dismiss();
                        showHelp(page + 1);
                    }
                });
            }
        }, (delay ? 300 : 0));

    }

    private void getDeepChildOffset(final ViewGroup mainParent, final ViewParent parent, final View child, final Point accumulatedOffset) {
        ViewGroup parentGroup = (ViewGroup) parent;
        accumulatedOffset.x += child.getLeft();
        accumulatedOffset.y += child.getTop();
        if (parentGroup.equals(mainParent)) {
            return;
        }
        getDeepChildOffset(mainParent, parentGroup.getParent(), parentGroup, accumulatedOffset);
    }

    private void setupDetails(Profile profile) {
        if (binding.detailsStub.isInflated()) {
            return;
        }
        binding.detailsStub.setContainingBinding(binding);

        if (profile.city.length() <= 16) {
            binding.detailsStub.getViewStub().setLayoutResource(R.layout.profile_details);
        } else {
            binding.detailsStub.getViewStub().setLayoutResource(R.layout.profile_details_long);
        }
        binding.detailsStub.getViewStub().inflate();

        if (getAppComponent().preferences().getBoolean(PREFS_FIELD_PROFILE_HELP, true)) {
            showHelp();
        }
    }

    private void setupInstagram(Profile profile) {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.instagram_fragment);
        String token = getAppComponent().accountHelper().getInstagramToken();
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(profile.instagramId)) {
            if (fragment != null) {
                getChildFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
            }

            return;
        }

        if (fragment == null) {
            fragment = new InstagramGalleryFragment();
            fragment.setTargetFragment(this, RC_INSTAGRAM);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.instagram_fragment, fragment)
                    .commit();
        } else {
            ((InstagramGalleryFragment) fragment).reload();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);
        uploader = uploaderHelper.getUploader();
        uploader.onViewAttached(this);
        presenter.loadContent(true);

        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden && binding != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
            getActivity().setTitle("Profile");
        }
        super.onHiddenChanged(hidden);

        if (hidden) {
            if (player != null) {
                player.pause();
                player.clear();
            }
        } else {
            if (player != null) {
                player.ready();
            }
        }
    }

    private void pausePlayer() {
        if (player != null) {
            player.pause();
            player.clear();
            player.destroy();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_profile, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void edit() {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivityForResult(intent, RC_EDIT);
    }

    @Override
    public void editReal() {
        Intent intent = new Intent(getActivity(), MyRealActivity.class);
        intent.putExtra(MyRealActivity.EXTRA_PROFILE, binding.getViewModel().profile);
        startActivityForResult(intent, RC_EDIT_REAL);
    }

    @Override
    public void connectInstagram() {
        startActivityForResult(new Intent(getActivity(), InstagramLoginActivity.class), RC_INSTAGRAM);
    }

    @Override
    public void disconnectInstagram() {
        RxAlertDialog.with(this)
                .title(R.string.alert_title_disconnect_instagram)
                .message(R.string.alert_msg_disconnect_instagram)
                .positiveButton(R.string.button_disconnect)
                .negativeButton(R.string.button_cancel)
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        if (integer == RxAlertDialog.ButtonPositive) {
                            Profile profile = binding.getViewModel().getProfile();
                            profile.instagramId = "";
                            uploader.uploadContent(profile);
                        }
                    }
                });
    }

    @Override
    public Context getImageContext() {
        return getActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_EDIT && resultCode == Activity.RESULT_OK) {
            getAppComponent().accountHelper().getProfile()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Profile>() {
                        @Override
                        public void call(Profile profile) {
                            presenter.setProfile(profile);
                        }
                    });
        } else if (requestCode == RC_INSTAGRAM && resultCode == Activity.RESULT_OK) {
            InstagramUser user = data.getParcelableExtra("user");
            Profile profile = binding.getViewModel().getProfile();
            profile.instagramId = user.data.id;
            profile.instagramAccessToken = user.token;
            uploader.uploadContent(profile);
            getAppComponent().accountHelper().setInstagramToken(user.token);
        }
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {

    }

    @Override
    public void showContent(Profile content, boolean refresh) {
        ProfileViewModel viewModel = binding.getViewModel();
        if (viewModel == null) {
            binding.setViewModel(new ProfileViewModel(content, true, this));
        } else {
            binding.getViewModel().setProfile(content);
        }
        setupDetails(content);
        setupInstagram(content);
        setupReel(content);
    }

    private void setupReel(Profile profile) {
        if (player != null) {
            player.destroy();
        }

        if (profile.videos.isEmpty()) {
            return;
        }

        player = ReelPlayer.with(getActivity(), -1)
                .binding(binding.reelView)
                .videos(profile.videos)
                .profile(profile)
                .audio(true)
                .create();
        player.setup();
    }

    @Override
    public void onPause() {
        super.onPause();

        pausePlayer();
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
    public void uploadComplete(Profile response) {
        binding.getViewModel().setProfile(response);
        setupInstagram(response);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, getActivity())) {
            return;
        }
    }

    @Override
    public boolean onBackPressed() {
        if (showingHelp) {
            return true;
        }
        return super.onBackPressed();
    }

    public static class ProfilePresenter extends ContentLoadingPresenter<Profile, DataSource<Profile>, ContentLoadingPresenterView<Profile>> {

        public ProfilePresenter(DataSource<Profile> dataSource) {
            super(dataSource);
        }

        public void setProfile(Profile profile) {
            this.content = profile;
            view.showContent(profile, true);
        }
    }

    public static class ProfilePresenterFactory implements PresenterFactory<ProfilePresenter> {

        final Context context;

        public ProfilePresenterFactory(Context context) {
            this.context = context;
        }

        @Override
        public ProfilePresenter create() {
            return new ProfilePresenter(new ProfileDataSource(context));
        }
    }

    public static class ProfileUploadingPresenter extends UploadingPresenter<Profile, Profile, SignUpProfileUploader, UploadingPresenterView<Profile>> {

        public ProfileUploadingPresenter(SignUpProfileUploader dataUploader) {
            super(dataUploader);
        }
    }

    public static class ProfileUploaderFactory implements UploaderFactory<ProfileUploadingPresenter> {
        final Context context;

        public ProfileUploaderFactory(Context context) {
            this.context = context;
        }

        @Override
        public ProfileUploadingPresenter create() {
            return new ProfileUploadingPresenter(new ProfileUploader(context, TheDistanceApplication.getApplicationComponent(context).api()));
        }
    }
}
