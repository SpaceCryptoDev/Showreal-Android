package com.showreal.app.data.account;

import android.app.Activity;
import android.content.Context;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.model.Login;
import com.showreal.app.features.onboarding.signup.AccountActivity;

import java.util.Arrays;

import rx.Observable;
import rx.Subscriber;

public class FacebookHelper {

    public static Observable<LoginResult> login(final Activity activity, final CallbackManager callbackManager) {
        AccessToken.setCurrentAccessToken(null);
        return Observable.create(new Observable.OnSubscribe<LoginResult>() {
            @Override
            public void call(final Subscriber<? super LoginResult> subscriber) {
                LoginManager.getInstance()
                        .registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                            @Override
                            public void onSuccess(final LoginResult loginResult) {
                                if (loginResult.getRecentlyDeniedPermissions().size() != 0) {
                                    subscriber.onError(new FacebookPermissionsError());
                                    showFacebookPermissionsError(activity);
                                    subscriber.onCompleted();
                                    return;
                                }

                                subscriber.onNext(loginResult);
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onCancel() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(FacebookException error) {
                                subscriber.onError(error);
                            }
                        });
                LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList("public_profile", "email", "user_birthday", "user_friends"));
            }
        });
    }

    private static void showFacebookPermissionsError(Context context) {
        RxAlertDialog.with(context)
                .title(R.string.alert_title_facebook)
                .message(R.string.alert_msg_facebook)
                .positiveButton(R.string.button_ok)
                .create().subscribe();
    }

    public static class FacebookPermissionsError extends FacebookException {

    }
}
