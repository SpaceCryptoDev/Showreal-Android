package com.showreal.app.features.profile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.model.LatLng;
import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.databinding.ActivityProfileEditBinding;
import com.showreal.app.features.onboarding.signup.SignUpProfileActivity;
import com.showreal.app.features.onboarding.signup.SignUpProfileUploader;
import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.util.Calendar;

import pl.aprilapps.easyphotopicker.EasyImage;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.time.DatePickerDialogCompat;
import uk.co.thedistance.components.time.DatePickerFragment;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class EditProfileActivity extends BaseActivity implements UploadingPresenterView<Profile>, EditProfileViewModel.EditProfileView {
    private ActivityProfileEditBinding binding;
    private static final int RC_LOCATION = 0x0;
    private static final int RC_IMAGE_CROP = 0x1;

    private PresenterLoaderHelper<ProfilePresenter> loaderHelper;
    private ProfilePresenter presenter;
    private Settings settings;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_edit);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.title_edit_profile);

        getAppComponent().accountHelper().getSettings()
                .flatMap(new Func1<Settings, Observable<Profile>>() {
                    @Override
                    public Observable<Profile> call(Settings settings) {
                        EditProfileActivity.this.settings = settings;
                        return getAppComponent().accountHelper().getProfile();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Profile>() {
                    @Override
                    public void call(Profile profile) {
                        binding.heightSlider.setProgress((int) profile.height);

                        EditProfileViewModel viewModel = new EditProfileViewModel(profile, EditProfileActivity.this);
                        if (settings != null) {
                            viewModel.setUnits(settings.height_unit);
                        }
                        binding.setViewModel(viewModel);

                        if (!TextUtils.isEmpty(profile.image)) {
                            Glide.with(EditProfileActivity.this)
                                    .load(profile.image)
                                    .asBitmap()
                                    .into(binding.profileImage);
                        }
                    }
                });

        loaderHelper = new PresenterLoaderHelper<>(this, new ProfilePresenterFactory(this));
        getSupportLoaderManager().initLoader(0, null, loaderHelper);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_location, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                Profile profile = binding.getViewModel().profile;
                if (validate(profile)) {
                    presenter.uploadContent(profile);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);
    }

    @Override
    public void changeLocation(String city, LatLng latLng) {
        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra(LocationActivity.EXTRA_CITY, city);
        intent.putExtra(LocationActivity.EXTRA_LAT_LNG, latLng);
        startActivityForResult(intent, RC_LOCATION);
    }

    private void getPermission() {
        RxPermissions.getInstance(this)
                .requestEach(Manifest.permission.CAMERA)
                .subscribe(new Action1<Permission>() {
                    @Override
                    public void call(Permission permission) {
                        if (permission.granted) {
                            changeImage();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            showPermissionRationale();
                        } else {
                            SREasyImage.openChooserWithDocuments(EditProfileActivity.this, getString(R.string.choose_image), 0, false);
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
        if (RxPermissions.getInstance(this).isGranted(Manifest.permission.CAMERA)) {
            EasyImage.openChooserWithDocuments(this, getString(R.string.choose_image), 0);
        } else {
            getPermission();
        }
    }

    @Override
    public void proceed(Profile profile) {
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
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        finish();

    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, this)) {
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

    public boolean validate(Profile profile) {
        if (TextUtils.isEmpty(profile.firstName)) {
            binding.fieldFirstName.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }
        if (TextUtils.isEmpty(profile.lastName)) {
            binding.fieldLastName.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }
        if (TextUtils.isEmpty(profile.email)) {
            binding.fieldEmail.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }
        if (TextUtils.isEmpty(profile.dateOfBirth)) {
            binding.fieldDob.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }

        return true;
    }

    @Override
    public void clearErrors() {
        binding.fieldFirstName.setError(null);
        binding.fieldLastName.setError(null);
        binding.fieldEmail.setError(null);
        binding.fieldDob.setError(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new EasyImage.Callbacks() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {

            }

            @Override
            public void onImagePicked(File imageFile, EasyImage.ImageSource source, int type) {

                Uri uri = Uri.fromFile(imageFile);

                Intent cropIntent = new Intent(EditProfileActivity.this, CropActivity.class);
                cropIntent.putExtra(CropActivity.EXTRA_IMAGE_URI, uri.toString());
                startActivityForResult(cropIntent, RC_IMAGE_CROP);

            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                if (source == EasyImage.ImageSource.CAMERA) {
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(EditProfileActivity.this);
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

            Glide.with(this)
                    .load(Uri.fromFile(new File(image)))
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.profileImage);

            binding.getViewModel().setImage(image);

        }
    }

    @Override
    public void changeDob() {
        Calendar calendar = binding.getViewModel().getDob();
        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, 1998);
        }
        DatePickerFragment fragment = DatePickerFragment.newInstance(calendar);

        fragment.setListener(new DatePickerDialogCompat.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                binding.getViewModel().setDob(year, monthOfYear, dayOfMonth);
            }
        });
        fragment.show(getSupportFragmentManager(), "dob");
    }

    public static class ProfilePresenter extends UploadingPresenter<Profile, Profile, SignUpProfileUploader, UploadingPresenterView<Profile>> {

        public ProfilePresenter(SignUpProfileUploader dataUploader) {
            super(dataUploader);
        }
    }

    public static class ProfilePresenterFactory implements PresenterFactory<ProfilePresenter> {
        final Context context;

        public ProfilePresenterFactory(Context context) {
            this.context = context;
        }

        @Override
        public ProfilePresenter create() {
            return new ProfilePresenter(new ProfileUploader(context, TheDistanceApplication.getApplicationComponent(context).api()));
        }
    }

}
