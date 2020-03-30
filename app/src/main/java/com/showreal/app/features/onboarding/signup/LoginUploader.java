package com.showreal.app.features.onboarding.signup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.NewLogin;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Session;
import com.showreal.app.features.real.myreal.ReelHelper;
import com.showreal.app.injection.ApplicationComponent;

import io.reactivecache.Provider;
import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class LoginUploader implements DataUploader<NewLogin, Intent> {

    private final ShowRealApi api;
    private final AccountManager accountManager;
    private final String accountType;
    private final Provider<Profile> staleProfileProvider;
    private final AccountHelper accountHelper;
    private final ReelHelper reelHelper;
    private final Context context;
    private NewLogin login;
    private final Provider<Profile> profileProvider;
    private final ApplicationComponent applicationComponent;
    private boolean isRegister;

    public LoginUploader(Context context, ShowRealApi api) {
        this.context = context.getApplicationContext();
        this.applicationComponent = TheDistanceApplication.getApplicationComponent(context);
        this.api = api;
        this.accountManager = AccountManager.get(context);
        this.accountType = context.getResources().getString(R.string.account_type);
        this.accountHelper = TheDistanceApplication.getApplicationComponent(context).accountHelper();
        profileProvider = TheDistanceApplication.getApplicationComponent(context).profileProvider();
        staleProfileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
        reelHelper = TheDistanceApplication.getApplicationComponent(context).reelHelper();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(NewLogin login) {
        this.login = login;
    }

    @Override
    public Observable<Intent> getUpload() {
        if (TextUtils.isEmpty(login.facebookToken)) {
            return accountHelper.ensureDataClean()
                    .flatMap(new Func1<Void, Observable<Session>>() {
                        @Override
                        public Observable<Session> call(Void aVoid) {
                            return api.login(login);
                        }
                    })
                    .map(new Func1<Session, Intent>() {
                        @Override
                        public Intent call(Session session) {
                            Account account = new Account(session.profile.email, accountType);

                            Profile.updateAppboy(session.profile, applicationComponent.appContext());

                            Observable.just(session.profile)
                                    .compose(profileProvider.replace())
                                    .compose(staleProfileProvider.replace())
                                    .subscribe();

                            reelHelper.clean();

                            accountManager.addAccountExplicitly(account, login.password, null);
                            accountManager.setAuthToken(account, "session", session.token);
                            applicationComponent.accountHelper().setInstagramToken(session.profile.instagramAccessToken);

                            Intent intent = new Intent();

                            Bundle bundle = new Bundle(4);
                            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                            bundle.putString(AccountManager.KEY_AUTHTOKEN, session.token);

                            TheDistanceApplication.registerPush(context);

                            return intent;
                        }
                    });
        } else {
            isRegister = false;
            return accountHelper.ensureDataClean()
                    .flatMap(new Func1<Void, Observable<Session>>() {
                        @Override
                        public Observable<Session> call(Void aVoid) {
                            return api.login(login);
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<? extends Session>>() {
                        @Override
                        public Observable<? extends Session> call(Throwable throwable) {
                            isRegister = true;
                            return api.register(login);
                        }
                    })
                    .map(new Func1<Session, Intent>() {
                        @Override
                        public Intent call(Session session) {
                            Account account = new Account(session.profile.email, accountType);

                            Profile.updateAppboy(session.profile, applicationComponent.appContext());

                            Observable.just(session.profile)
                                    .compose(profileProvider.replace())
                                    .compose(staleProfileProvider.replace())
                                    .subscribe();

                            if (isRegister) {
                                reelHelper.migrateReel(session.profile);
                            } else {
                                reelHelper.clean();
                            }

                            accountManager.addAccountExplicitly(account, login.password, null);
                            accountManager.setAuthToken(account, "session", session.token);
                            applicationComponent.accountHelper().setInstagramToken(session.profile.instagramAccessToken);

                            Intent intent = new Intent();

                            Bundle bundle = new Bundle(4);
                            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                            bundle.putString(AccountManager.KEY_AUTHTOKEN, session.token);

                            TheDistanceApplication.registerPush(context);

                            return intent;
                        }
                    });
        }
    }
}
