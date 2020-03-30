package com.showreal.app.features.settings.password;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.model.PasswordChange;
import com.showreal.app.databinding.ActivityChangePasswordBinding;

import retrofit2.adapter.rxjava.HttpException;
import uk.co.thedistance.components.base.UploaderFactory;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class ChangePasswordActivity extends BaseActivity implements UploadingPresenterView<Void>, PasswordViewModel.PasswordView {

    private UploaderLoaderHelper<PasswordUploadPresenter> loaderHelper;
    private ActivityChangePasswordBinding binding;
    private PasswordUploadPresenter uploader;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_change_password);
        binding.setViewModel(new PasswordViewModel(this));
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loaderHelper = new UploaderLoaderHelper<>(this, new PasswordPresenterFactory());
        getSupportLoaderManager().initLoader(0, null, loaderHelper);
    }

    @Override
    protected void onResume() {
        super.onResume();

        uploader = loaderHelper.getUploader();
        uploader.onViewAttached(this);
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
                changePassword(binding.getViewModel().getPasswordChange());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void changePassword(PasswordChange change) {
        boolean error = false;
        if (TextUtils.isEmpty(change.currentPassword)) {
            binding.passwordOldLayout.setError(getString(R.string.error_field_empty));
            error = true;
        }

        if (TextUtils.isEmpty(change.newPassword) || change.newPassword.length() < 8) {
            binding.passwordLayout.setErrorEnabled(true);
            binding.passwordLayout.setError(getResources().getString(R.string.error_field_password));
            error = true;
        }

        if (error) {
            return;
        }
        uploader.uploadContent(binding.getViewModel().getPasswordChange());
    }

    @Override
    public void showUploading(boolean show) {
        if (show) {
            binding.loadingLayout.loadingLayout.show();
        } else {
            binding.loadingLayout.loadingLayout.hide();
        }
    }

    @Override
    public void uploadComplete(Void response) {
        binding.password.setText("");
        binding.passwordOld.setText("");

        RxAlertDialog.with(this)
                .title(R.string.alert_title_password)
                .message(R.string.alert_msg_password)
                .positiveButton(R.string.button_ok)
                .subscribe();
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, this)) {
            return;
        }
        if (throwable instanceof HttpException && ((HttpException) throwable).code() == 401) {
            getAppComponent().accountHelper().logout(this, true);
            return;
        }

        int msg = R.string.alert_password_error;
        if (throwable instanceof HttpException) {
            int code = ((HttpException) throwable).code();
            if (code == 403) {
                msg = R.string.alert_password_incorrect;
            }
        }
        RxAlertDialog.with(this)
                .title(R.string.alert_title_password_error)
                .message(msg)
                .positiveButton(R.string.button_ok)
                .subscribe();

    }

    @Override
    public void clearErrors() {
        binding.passwordLayout.setError(null);
        binding.passwordOldLayout.setError(null);
    }

    private class PasswordUploadPresenter extends UploadingPresenter<PasswordChange, Void, PasswordUploader, UploadingPresenterView<Void>> {

        public PasswordUploadPresenter(PasswordUploader dataUploader) {
            super(dataUploader);
        }
    }

    private class PasswordPresenterFactory implements UploaderFactory<PasswordUploadPresenter> {
        @Override
        public PasswordUploadPresenter create() {
            return new PasswordUploadPresenter(new PasswordUploader(ChangePasswordActivity.this));
        }
    }
}
