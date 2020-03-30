package com.showreal.app.features.onboarding.signup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.DatabaseHelper;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.Login;
import com.showreal.app.data.model.NewLogin;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Session;
import com.showreal.app.features.real.myreal.ReelHelper;
import com.showreal.app.injection.ApplicationComponent;

import io.reactivecache.Provider;
import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class SignUpUploader implements DataUploader<NewLogin, Intent> {

    private final ShowRealApi api;
    private final AccountManager accountManager;
    private final String accountType;
    private final Provider<Profile> profileProvider;
    private final Provider<Profile> staleProfileProvider;
    private final ApplicationComponent applicationComponent;
    private final AccountHelper accountHelper;
    private final ReelHelper reelHelper;
    private final Context context;
    private NewLogin newLogin;
    private boolean tryLogin = false;
    private boolean isRegister;

    public void setTryLogin(boolean tryLogin) {
        this.tryLogin = tryLogin;
    }

    public SignUpUploader(Context context, ShowRealApi api) {
        this.context = context.getApplicationContext();
        this.applicationComponent = TheDistanceApplication.getApplicationComponent(context);
        this.api = api;
        this.accountManager = AccountManager.get(context);
        this.accountType = context.getResources().getString(R.string.account_type);
        this.accountHelper = TheDistanceApplication.getApplicationComponent(context).accountHelper();
        reelHelper = TheDistanceApplication.getApplicationComponent(context).reelHelper();
        profileProvider = TheDistanceApplication.getApplicationComponent(context).profileProvider();
        staleProfileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(NewLogin newLogin) {
        this.newLogin = newLogin;
    }

    @Override
    public Observable<Intent> getUpload() {
        if (tryLogin) {
            tryLogin = false;
            isRegister = false;
            return accountHelper.ensureDataClean()
                    .flatMap(new Func1<Void, Observable<Session>>() {
                        @Override
                        public Observable<Session> call(Void aVoid) {
                            return api.login(Login.with(newLogin));
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<? extends Session>>() {
                        @Override
                        public Observable<? extends Session> call(Throwable throwable) {
                            isRegister = true;
                            return api.register(newLogin);
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

                            accountManager.addAccountExplicitly(account, newLogin.password, null);
                            accountManager.setAuthToken(account, "session", session.token);
                            applicationComponent.accountHelper().setInstagramToken(session.profile.instagramAccessToken);

                            Intent intent = new Intent();

                            Bundle bundle = new Bundle(5);
                            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                            bundle.putString(AccountManager.KEY_AUTHTOKEN, session.token);
                            intent.putExtras(bundle);

                            TheDistanceApplication.registerPush(context);

                            return intent;
                        }
                    });

        }

        return accountHelper.ensureDataClean()
                .flatMap(new Func1<Void, Observable<Session>>() {
                    @Override
                    public Observable<Session> call(Void aVoid) {
                        return api.register(newLogin);
                    }
                })
                .map(new Func1<Session, Intent>() {
                    @Override
                    public Intent call(Session session) {
                        Account account = new Account(session.profile.email, accountType);

                        Profile.updateAppboy(session.profile, applicationComponent.appContext());
                        Profile.updateRegion(session.profile, applicationComponent.appContext());

                        Observable.just(session.profile)
                                .compose(profileProvider.replace())
                                .compose(staleProfileProvider.replace())
                                .subscribe();

                        reelHelper.migrateReel(session.profile);

                        accountManager.addAccountExplicitly(account, newLogin.password, null);
                        accountManager.setAuthToken(account, "session", session.token);

                        Intent intent = new Intent();

                        Bundle bundle = new Bundle(5);
                        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                        bundle.putString(AccountManager.KEY_AUTHTOKEN, session.token);
                        bundle.putBoolean("new_user", true);
                        intent.putExtras(bundle);

                        TheDistanceApplication.registerPush(context);

                        return intent;
                    }
                });
    }
}
