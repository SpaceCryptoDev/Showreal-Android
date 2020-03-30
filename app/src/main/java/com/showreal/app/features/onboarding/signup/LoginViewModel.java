package com.showreal.app.features.onboarding.signup;

import android.databinding.BaseObservable;
import android.view.View;

import com.showreal.app.data.model.NewLogin;


public class LoginViewModel extends BaseObservable {

    private NewLogin login;
    private final LoginView loginView;

    interface LoginView {

        void login(NewLogin login);

        void loginWithFacebook();

        boolean validate(NewLogin login);

        void clearErrors();

        void forgotPassword();
    }

    public LoginViewModel(LoginView loginView) {
        this.loginView = loginView;
        login = new NewLogin();
    }


    public void onEmailChanged(CharSequence sequence, int start, int before, int count) {
        loginView.clearErrors();
        login.email = sequence.toString().trim();
    }

    public void onPasswordChanged(CharSequence sequence, int start, int before, int count) {
        loginView.clearErrors();
        login.password = sequence.toString();
    }

    public void onLogin(View view) {
        if (loginView.validate(login)) {
            loginView.login(login);
        }
    }

    public void onFacebook(View view) {
        loginView.loginWithFacebook();
    }

    public void onForgot(View view) {
        loginView.forgotPassword();
    }


}
