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

import com.facebook.AccessToken;
import com.facebook.FacebookAuthorizationException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.showreal.app.BaseFragment;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.SRSnackbar;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Events;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.account.FacebookHelper;
import com.showreal.app.data.model.NewLogin;
import com.showreal.app.databinding.FragmentLoginBinding;
import com.showreal.app.features.settings.FacebookConnectUploader;
import com.showreal.app.features.status.StatusActivity;

import org.json.JSONObject;

import java.io.IOException;

import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class LoginFragment extends BaseFragment implements UploadingPresenterView<Intent>, LoginViewModel.LoginView {

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse;
    private FragmentLoginBinding binding;
    private PresenterLoaderHelper<LoginPresenter> loaderHelper;
    private LoginPresenter presenter;

    enum Action {
        Login,
        FacebookLogin
    }

    private Action action;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountAuthenticatorResponse = getActivity().getIntent().getParcelableExtra("response");
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new LoginPresenterFactory());
        getLoaderManager().initLoader(0, null, loaderHelper);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false);

        binding.setViewModel(new LoginViewModel(this));

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);
    }

    @Override
    protected String getScreenName() {
        return null;
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
        switch (action) {
            case Login:
                getAppComponent().analytics().send(new AppboyEvent(Events.LOGIN));
                break;
            case FacebookLogin:
                getAppComponent().analytics().send(new AppboyEvent(Events.LOGIN_FACEBOOK));
                break;
        }
        action = null;

        setAccountAuthenticatorResult(intent.getExtras());
        getActivity().setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void showError(Throwable throwable, String error) {
        action = null;
        if (throwable instanceof IOException) {
            startActivity(new Intent(getActivity(), StatusActivity.class));
            return;
        }

        if (throwable instanceof FacebookAuthorizationException && throwable.getMessage().contains("CONNECTION_FAILURE")) {
            startActivity(new Intent(getActivity(), StatusActivity.class));
            return;
        }

        FabricHelper.logException(throwable);

        int title = R.string.alert_title_login;
        int msg = -1;
        if (throwable instanceof HttpException) {
            int code = ((HttpException) throwable).code();
            switch (code) {
                case 401:
                    msg = R.string.alert_login_msg;
                    break;
            }
        }

        RxAlertDialog dialog = RxAlertDialog.with(this)
                .title(title);

        if (msg == -1) {
            dialog.message(getString(R.string.alert_msg_error, "logging into your account"));
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

    public void finish() {
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

        ((AccountActivity) getActivity()).finishLogin(false);
    }

    @Override
    public void login(NewLogin login) {
        if (action == null) {
            action = Action.Login;
        }
        presenter.uploadContent(login);
    }

    @Override
    public void loginWithFacebook() {
        AccessToken.setCurrentAccessToken(null);
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
                                        action = Action.FacebookLogin;
                                        login(login);

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

        if (TextUtils.isEmpty(login.email)) {
            binding.emailLayout.setErrorEnabled(true);
            binding.emailLayout.setError(getResources().getString(R.string.error_field_empty));
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
        binding.emailLayout.setErrorEnabled(false);
        binding.passwordLayout.setErrorEnabled(false);
    }

    @Override
    public void forgotPassword() {
        startActivity(new Intent(getActivity(), ForgottenPasswordActivity.class
        ));
    }

    class LoginPresenter extends UploadingPresenter<NewLogin, Intent, LoginUploader, UploadingPresenterView<Intent>> {

        public LoginPresenter(LoginUploader dataUploader) {
            super(dataUploader);
        }
    }

    class LoginPresenterFactory implements PresenterFactory<LoginPresenter> {

        @Override
        public LoginPresenter create() {
            ShowRealApi api = getAppComponent().api();
            return new LoginPresenter(new LoginUploader(getActivity(), api));
        }
    }
}
