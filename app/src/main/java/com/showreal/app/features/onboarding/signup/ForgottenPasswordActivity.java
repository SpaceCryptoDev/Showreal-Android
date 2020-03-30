package com.showreal.app.features.onboarding.signup;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.showreal.app.BaseActivity;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.databinding.ActivityForgotPasswordBinding;

import retrofit2.adapter.rxjava.HttpException;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;
import uk.co.thedistance.thedistancecore.animation.AnimationHelper;


public class ForgottenPasswordActivity extends BaseActivity implements ForgottenPasswordViewModel.ForgottenPasswordView, UploadingPresenterView<String> {

    private ActivityForgotPasswordBinding binding;
    private PresenterLoaderHelper<ForgottenPasswordPresenter> loaderHelper;
    private ForgottenPasswordPresenter presenter;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_forgot_password);
        binding.setViewModel(new ForgottenPasswordViewModel(this));

        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loaderHelper = new PresenterLoaderHelper<>(this, new ForgottenPasswordPresenterFactory());

        getSupportLoaderManager().initLoader(0, null, loaderHelper);
    }

    @Override
    protected void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);
    }

    @Override
    public void send(String email) {
        if (TextUtils.isEmpty(email)) {
            binding.passwordReset.emailLayout.setErrorEnabled(true);
            binding.passwordReset.emailLayout.setError(getString(R.string.error_field_empty));
            return;
        }
        presenter.uploadContent(email);
    }

    @Override
    public void clearError() {
        binding.passwordReset.emailLayout.setError(null);
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
    public void uploadComplete(String response) {
        binding.passwordResetSent.email.setText(response);
        int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        AnimationHelper.fadeOut(binding.passwordReset.getRoot(), duration, 0);
        AnimationHelper.fadeIn(binding.passwordResetSent.getRoot(), duration, (int) (duration / 2f));
    }

    @Override
    public void showError(Throwable throwable, String error) {
        FabricHelper.logException(throwable);

        int title = R.string.alert_title_error;
        int msg = -1;
        if (throwable instanceof HttpException) {
            int code = ((HttpException) throwable).code();
            switch (code) {
                case 400:
                    title = R.string.alert_password_title;
                    msg = R.string.alert_password_msg;
                    break;
            }
        }

        RxAlertDialog dialog = RxAlertDialog.with(this)
                .title(title);

        if (msg == -1) {
            dialog.message(getString(R.string.alert_msg_error, "when trying to reset your password"));
        } else {
            dialog.message(msg);
        }
        dialog.positiveButton(R.string.button_ok)
                .create()
                .subscribe();

    }

    private class ForgottenPasswordPresenter extends UploadingPresenter<String, String, ForgottenPasswordUploader, UploadingPresenterView<String>> {

        public ForgottenPasswordPresenter(ForgottenPasswordUploader dataUploader) {
            super(dataUploader);
        }
    }

    private class ForgottenPasswordPresenterFactory implements PresenterFactory<ForgottenPasswordPresenter> {

        @Override
        public ForgottenPasswordPresenter create() {
            ShowRealApi api = getAppComponent().api();
            return new ForgottenPasswordPresenter(new ForgottenPasswordUploader(api));
        }
    }
}
