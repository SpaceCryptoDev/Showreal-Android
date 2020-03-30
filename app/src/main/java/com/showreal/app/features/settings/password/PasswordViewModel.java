package com.showreal.app.features.settings.password;

import android.databinding.BaseObservable;
import android.view.View;

import com.showreal.app.data.model.Login;
import com.showreal.app.data.model.PasswordChange;


public class PasswordViewModel extends BaseObservable {

    private PasswordChange passwordChange;
    private final PasswordView passwordView;

    interface PasswordView {

        void clearErrors();
    }

    public PasswordViewModel(PasswordView passwordView) {
        this.passwordView = passwordView;
        passwordChange = new PasswordChange();
    }


    public void onCurrentPasswordChange(CharSequence sequence, int start, int before, int count) {
        passwordView.clearErrors();
        passwordChange.currentPassword = sequence.toString();
    }

    public void onPasswordChanged(CharSequence sequence, int start, int before, int count) {
        passwordView.clearErrors();
        passwordChange.newPassword = sequence.toString();
    }

    public PasswordChange getPasswordChange() {
        return passwordChange;
    }
}
