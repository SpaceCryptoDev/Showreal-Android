package com.showreal.app.features.onboarding.signup;

import android.databinding.BaseObservable;
import android.view.View;

public class ForgottenPasswordViewModel extends BaseObservable {

    private final ForgottenPasswordView passwordView;
    private String email;

    public ForgottenPasswordViewModel(ForgottenPasswordView passwordView) {
        this.passwordView = passwordView;
    }

    interface ForgottenPasswordView {
        void send(String email);

        void clearError();
    }

    public void onEmailChanged(CharSequence sequence, int start, int before, int count) {
        passwordView.clearError();
        email = sequence.toString();
    }

    public void onSend(View view) {
        passwordView.send(email);
    }
}
