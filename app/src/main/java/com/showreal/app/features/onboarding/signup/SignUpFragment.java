package com.showreal.app.features.onboarding.signup;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import com.facebook.FacebookAuthorizationException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.SRSnackbar;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Events;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.account.FacebookHelper;
import com.showreal.app.data.model.NewLogin;
import com.showreal.app.databinding.FragmentSignUpBinding;
import com.showreal.app.features.status.StatusActivity;

import org.json.JSONObject;

import java.util.Calendar;

import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;
import rx.functions.Action1;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.time.DatePickerDialogCompat;
import uk.co.thedistance.components.time.DatePickerFragment;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class SignUpFragment extends BaseFragment implements SignUpViewModel.SignUpView, UploadingPresenterView<Intent> {

    FragmentSignUpBinding binding;
    PresenterLoaderHelper<SignUpPresenter> loaderHelper;
    private SignUpPresenter presenter;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up, container, false);

        binding.setViewModel(new SignUpViewModel(this));
        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new SignUpPresenterFactory());
        getLoaderManager().initLoader(0, null, loaderHelper);

        mAccountAuthenticatorResponse = getActivity().getIntent().getParcelableExtra("response");
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);
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
        fragment.show(getChildFragmentManager(), "dob");
    }

    @Override
    public void signUp(final NewLogin login) {
        RxAlertDialog.with(getActivity())
                .title(R.string.app_name)
                .message(R.string.prompt_terms)
                .positiveButton(R.string.button_accept)
                .negativeButton(R.string.button_cancel)
                .create()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer button) {
                        if (button == RxAlertDialog.ButtonPositive) {
                            getAppComponent().analytics().send(new AppboyEvent(Events.REGISTER));
                            presenter.uploadContent(login);
                        }
                    }
                });
    }

    @Override
    public void loginWithFacebook() {
        RxAlertDialog.with(getActivity())
                .title(R.string.app_name)
                .message(R.string.prompt_terms)
                .positiveButton(R.string.button_accept)
                .negativeButton(R.string.button_cancel)
                .create()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer button) {
                        if (button == RxAlertDialog.ButtonPositive) {
                            facebookLogin();
                        }
                    }
                });
    }

    private void facebookLogin() {
        getAppComponent().analytics().send(new AppboyEvent(Events.REGISTER_FACEBOOK));
        FacebookHelper.login(getActivity(), ((AccountActivity) getActivity()).getFacebookCallbackManager())
                .subscribe(new Subscriber<LoginResult>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        showError(e, e.getMessage());
                    }

                    @Override
                    public void onNext(final LoginResult loginResult) {
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        if (response.getError() != null) {
                                            SRSnackbar.show(binding.coordinator, "Something went wrong, please try again", SRSnackbar.LENGTH_SHORT);
                                            return;
                                        }

                                        NewLogin login = NewLogin.with(loginResult, object);
                                        presenter.getUploader().setTryLogin(true);
                                        presenter.uploadContent(login);

                                    }
                                });
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "email,first_name,last_name,birthday");
                        request.setParameters(parameters);
                        request.executeAsync();
                    }
                });
    }

    @Override
    public boolean validate(NewLogin login) {
        if (TextUtils.isEmpty(login.firstName)) {
            binding.firstNameLayout.setErrorEnabled(true);
            binding.firstNameLayout.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }
        if (TextUtils.isEmpty(login.lastName)) {
            binding.lastNameLayout.setErrorEnabled(true);
            binding.lastNameLayout.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }
        if (TextUtils.isEmpty(login.email)) {
            binding.emailLayout.setErrorEnabled(true);
            binding.emailLayout.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }
        if (TextUtils.isEmpty(login.dateOfBirth)) {
            binding.dobLayout.setErrorEnabled(true);
            binding.dobLayout.setError(getResources().getString(R.string.error_field_empty));
            return false;
        }
        if (TextUtils.isEmpty(login.password) || login.password.length() < 8) {
            binding.passwordLayout.setErrorEnabled(true);
            binding.passwordLayout.setError(getResources().getString(R.string.error_field_password));
            return false;
        }

        return true;
    }

    @Override
    public void clearErrors() {
        binding.firstNameLayout.setErrorEnabled(false);
        binding.lastNameLayout.setErrorEnabled(false);
        binding.emailLayout.setErrorEnabled(false);
        binding.dobLayout.setErrorEnabled(false);
        binding.passwordLayout.setErrorEnabled(false);
    }

    @Override
    public void showUploading(boolean show) {
        if (getActivity() == null) {
            return;
        }
        ((AccountActivity) getActivity()).showLoading(show);
    }

    @Override
    public void uploadComplete(Intent intent) {
        boolean newUser = intent.getBooleanExtra("new_user", false);
        intent.removeExtra("new_user");
        setAccountAuthenticatorResult(intent.getExtras());
        getActivity().setResult(Activity.RESULT_OK, intent);
        finish(newUser);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, getActivity())) {
            return;
        }

        if (throwable instanceof FacebookAuthorizationException && throwable.getMessage().contains("CONNECTION_FAILURE")) {
            startActivity(new Intent(getActivity(), StatusActivity.class));
            return;
        }

        FabricHelper.logException(throwable);

        int title = R.string.alert_title_registration;
        int msg = -1;
        if (throwable instanceof HttpException) {
            int code = ((HttpException) throwable).code();
            switch (code) {
                case 409:
                    title = R.string.alert_title_registration_email;
                    msg = R.string.alert_registration_email_msg;
                    break;
            }
        }

        RxAlertDialog dialog = RxAlertDialog.with(this)
                .title(title);

        if (msg == -1) {
            dialog.message(getString(R.string.alert_msg_error, "registering your account"));
        } else {
            dialog.message(msg);
        }
        dialog.positiveButton(R.string.button_ok)
                .create()
                .subscribe();
    }

    Bundle mResultBundle;

    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    public void finish(boolean newUser) {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }

        ((AccountActivity) getActivity()).finishLogin(newUser);
    }

    class SignUpPresenter extends UploadingPresenter<NewLogin, Intent, SignUpUploader, UploadingPresenterView<Intent>> {

        public SignUpPresenter(SignUpUploader dataUploader) {
            super(dataUploader);
        }

        public SignUpUploader getUploader() {
            return dataUploader;
        }
    }

    class SignUpPresenterFactory implements PresenterFactory<SignUpPresenter> {

        @Override
        public SignUpPresenter create() {
            ShowRealApi api = getAppComponent().api();
            return new SignUpPresenter(new SignUpUploader(getActivity(), api));
        }
    }
}
