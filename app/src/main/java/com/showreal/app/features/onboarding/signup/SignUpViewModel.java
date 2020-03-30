package com.showreal.app.features.onboarding.signup;

import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.text.TextUtils;
import android.view.View;

import com.showreal.app.data.model.NewLogin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.showreal.app.data.model.Profile.API_DATE_FORMAT;
import static com.showreal.app.data.model.Profile.DATE_FORMAT;


public class SignUpViewModel extends BaseObservable {

    private NewLogin login;
    private final SignUpView signUpView;
    public ObservableField<String> dateOfBirth = new ObservableField<>();

    public void setDob(int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);

        dateOfBirth.set(DATE_FORMAT.format(calendar.getTime()));
        login.dateOfBirth = API_DATE_FORMAT.format(calendar.getTime());
    }

    public Calendar getDob() {
        if (TextUtils.isEmpty(login.dateOfBirth)) {
            return null;
        }
        try {
            Date date = API_DATE_FORMAT.parse(login.dateOfBirth);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        } catch (ParseException e) {
            return null;
        }
    }

    interface SignUpView {

        void changeDob();

        void signUp(NewLogin login);

        void loginWithFacebook();

        boolean validate(NewLogin login);

        void clearErrors();
    }

    public SignUpViewModel(SignUpView signUpView) {
        this.signUpView = signUpView;
        login = new NewLogin();
    }

    public void onFirstNameChanged(CharSequence sequence, int start, int before, int count) {
        signUpView.clearErrors();
        login.firstName = sequence.toString();
    }

    public void onLastNameChanged(CharSequence sequence, int start, int before, int count) {
        signUpView.clearErrors();
        login.lastName = sequence.toString();
    }

    public void onEmailChanged(CharSequence sequence, int start, int before, int count) {
        signUpView.clearErrors();
        login.email = sequence.toString().trim();
    }

    public void onPasswordChanged(CharSequence sequence, int start, int before, int count) {
        signUpView.clearErrors();
        login.password = sequence.toString();
    }

    public void onDobClick(View view) {
        signUpView.changeDob();
    }

    public void onSignUp(View view) {
        if (signUpView.validate(login)) {
            signUpView.signUp(login);
        }
    }

    public void onFacebook(View view) {
        signUpView.loginWithFacebook();
    }
}
