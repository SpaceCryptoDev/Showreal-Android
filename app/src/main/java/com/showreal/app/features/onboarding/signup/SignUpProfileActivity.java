package com.showreal.app.features.onboarding.signup;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.model.LatLng;
import com.showreal.app.BaseActivity;
import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.MainActivity;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.ActivityProfileSignupBinding;
import com.showreal.app.databinding.FragmentProfileSetupBinding;
import com.showreal.app.features.profile.CropActivity;
import com.showreal.app.features.profile.LocationActivity;
import com.showreal.app.features.profile.SREasyImage;
import com.showreal.app.features.real.myreal.MyRealActivity;
import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import pl.aprilapps.easyphotopicker.EasyImage;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class SignUpProfileActivity extends BaseActivity {
    private ActivityProfileSignupBinding binding;
    private SignUpProfileFragment fragment;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_signup);

        getAppComponent().accountHelper().getProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Profile>() {
                    @Override
                    public void call(Profile profile) {
                        binding.signupSteps.setVisibility(profile.videos.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });

        fragment = (SignUpProfileFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        binding.stepShowreal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.binding.getViewModel().onArrow(null);
            }
        });
    }

    public static class SignUpProfileFragment extends BaseFragment implements SignUpProfileViewModel.SignUpProfileView, UploadingPresenterView<Profile> {
        private static final int RC_LOCATION = 0x0;
        private static final int RC_IMAGE_CROP = 0x1;

        private FragmentProfileSetupBinding binding;
        private PresenterLoaderHelper<ProfilePresenter> loaderHelper;
        private ProfilePresenter presenter;

        @Override
        protected String getScreenName() {
            return null;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_setup, container, false);

            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbar);
            getActivity().setTitle(R.string.sign_up_profile);

            getAppComponent().accountHelper().getProfile()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Profile>() {
                        @Override
                        public void call(Profile profile) {
                            binding.heightSlider.setProgress((int) profile.height);

                            binding.setViewModel(new SignUpProfileViewModel(profile, SignUpProfileFragment.this));

                            if (!TextUtils.isEmpty(profile.image)) {
                                Glide.with(SignUpProfileFragment.this)
                                        .load(profile.image)
                                        .asBitmap()
                                        .into(binding.profileImage);
                            }
                        }
                    });

            loaderHelper = new PresenterLoaderHelper<>(getActivity(), new ProfilePresenterFactory());
            getLoaderManager().initLoader(0, null, loaderHelper);
            return binding.getRoot();
        }

        @Override
        public void onResume() {
            super.onResume();

            presenter = loaderHelper.getPresenter();
            presenter.onViewAttached(this);
        }

        @Override
        public void changeLocation(String city, LatLng latLng) {
            Intent intent = new Intent(getActivity(), LocationActivity.class);
            intent.putExtra(LocationActivity.EXTRA_CITY, city);
            intent.putExtra(LocationActivity.EXTRA_LAT_LNG, latLng);
            startActivityForResult(intent, RC_LOCATION);
        }

        private void getPermission() {
            RxPermissions.getInstance(getActivity())
                    .requestEach(Manifest.permission.CAMERA)
                    .subscribe(new Action1<Permission>() {
                        @Override
                        public void call(Permission permission) {
                            if (permission.granted) {
                                changeImage();
                            } else if (permission.shouldShowRequestPermissionRationale) {
                                showPermissionRationale();
                            } else {
                                SREasyImage.openChooserWithDocuments(SignUpProfileFragment.this, getString(R.string.choose_image), 0, false);
                            }
                        }
                    });
        }

        private void showPermissionRationale() {
            RxAlertDialog.with(this)
                    .title(R.string.permission_camera_rationale_title)
                    .message(R.string.permission_camera_rationale_msg_profile)
                    .positiveButton(R.string.button_ok)
                    .create()
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            getPermission();
                        }
                    });
        }

        @Override
        public void changeImage() {
            if (RxPermissions.getInstance(getActivity()).isGranted(Manifest.permission.CAMERA)) {
                EasyImage.openChooserWithDocuments(this, getString(R.string.choose_image), 0);
            } else {
                getPermission();
            }
        }

        @Override
        public void proceed(Profile profile) {
            if (!binding.getViewModel().isGenderSelected()) {
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_profile_validation)
                        .message(R.string.alert_msg_profile_validation_gender)
                        .positiveButton(R.string.button_ok)
                        .subscribe();
                return;
            }
            if (TextUtils.isEmpty(profile.city)) {
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_profile_validation)
                        .message(R.string.alert_msg_profile_validation_location)
                        .positiveButton(R.string.button_ok)
                        .subscribe();
                return;
            }
            presenter.uploadContent(profile);
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
            getAppComponent().accountHelper().setProfileUpdateNeeded(false);
            if (response.videos == null || response.videos.isEmpty()) {
                Intent intent = new Intent(getActivity(), MyRealActivity.class);
                intent.putExtra(MyRealActivity.EXTRA_PROFILE, binding.getViewModel().profile);
                intent.putExtra(MyRealActivity.EXTRA_SIGN_UP, true);
                startActivity(intent);
                return;
            }

            getAppComponent().accountHelper().setRealNeeded(false);
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }

        @Override
        public void showError(Throwable throwable, String error) {
            if (ErrorHandler.handle(throwable, getActivity())) {
                return;
            }

            FabricHelper.logException(throwable);

            RxAlertDialog.with(this)
                    .title(R.string.alert_title_profile)
                    .message(getString(R.string.alert_msg_error, "when updating your profile"))
                    .positiveButton(R.string.button_ok)
                    .create()
                    .subscribe();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            EasyImage.handleActivityResult(requestCode, resultCode, data, getActivity(), new EasyImage.Callbacks() {
                @Override
                public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {

                }

                @Override
                public void onImagePicked(File imageFile, EasyImage.ImageSource source, int type) {

                    Uri uri = Uri.fromFile(imageFile);

                    Intent cropIntent = new Intent(getActivity(), CropActivity.class);
                    cropIntent.putExtra(CropActivity.EXTRA_IMAGE_URI, uri.toString());
                    startActivityForResult(cropIntent, RC_IMAGE_CROP);

                }

                @Override
                public void onCanceled(EasyImage.ImageSource source, int type) {
                    if (source == EasyImage.ImageSource.CAMERA) {
                        File photoFile = EasyImage.lastlyTakenButCanceledPhoto(getActivity());
                        if (photoFile != null) {
                            photoFile.delete();
                        }
                    }
                }
            });

            if (requestCode == RC_LOCATION && resultCode == RESULT_OK) {
                String city = data.getStringExtra(LocationActivity.EXTRA_CITY);
                LatLng latLng = data.getParcelableExtra(LocationActivity.EXTRA_LAT_LNG);
                binding.getViewModel().setLocation(latLng);
                binding.getViewModel().setCity(city);
                return;
            }
            if (requestCode == RC_IMAGE_CROP && resultCode == RESULT_OK) {
                String image = data.getStringExtra(CropActivity.EXTRA_IMAGE_URI);

                Glide.with(SignUpProfileFragment.this)
                        .load(Uri.fromFile(new File(image)))
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(binding.profileImage);

                binding.getViewModel().setImage(image);

            }
        }

        private class ProfilePresenter extends UploadingPresenter<Profile, Profile, SignUpProfileUploader, UploadingPresenterView<Profile>> {

            public ProfilePresenter(SignUpProfileUploader dataUploader) {
                super(dataUploader);
            }
        }

        private class ProfilePresenterFactory implements PresenterFactory<ProfilePresenter> {

            @Override
            public ProfilePresenter create() {
                return new ProfilePresenter(new SignUpProfileUploader(getActivity().getApplicationContext(), getAppComponent().api()));
            }
        }
    }


}