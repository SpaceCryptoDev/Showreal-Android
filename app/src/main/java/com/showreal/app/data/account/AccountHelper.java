package com.showreal.app.data.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.IntDef;

import com.google.gson.Gson;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.DatabaseHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.features.onboarding.signup.AccountActivity;

import io.reactivecache.Provider;
import rx.Observable;
import rx.Subscriber;
import uk.co.thedistance.thedistancecore.TDObservers;
import uk.co.thedistance.thedistancecore.TDSubscribers;

public class AccountHelper {

    private static final String PREF_FIELD_LOCATION_TYPE = "search_location_type";
    final AccountManager accountManager;
    private final Gson gson;
    private final String accountType;
    private final Provider<Profile> profileProvider;
    private final Provider<Settings> settingsProvider;

    public static final int TypeHome = 0;
    public static final int TypeSearch = 1;
    public static final int TypeFollow = 2;
    private final Context context;
    private static final String PREFS_FIELD_PROFILE_NEEDED = "profile_update_needed";
    private static final String PREFS_FIELD_SHOWREAL_NEEDED = "showreal_update_needed";
    private final SharedPreferences preferences;

    @IntDef({TypeHome, TypeSearch, TypeFollow})
    public @interface LocationType {
    }

    public AccountHelper(Context context) {
        this.context = context;
        accountManager = AccountManager.get(context);
        gson = TheDistanceApplication.getApplicationComponent(context).gson();
        accountType = context.getResources().getString(R.string.account_type);
        profileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
        settingsProvider = TheDistanceApplication.getApplicationComponent(context).settingsProvider();
        preferences = TheDistanceApplication.getApplicationComponent(context).preferences();
    }

    public boolean isLoggedIn() {
        return accountManager.getAccountsByType(accountType).length > 0;
    }

    public boolean profileUpdateNeeded() {
        return preferences.getBoolean(PREFS_FIELD_PROFILE_NEEDED, false);
    }

    public boolean showRealNeeded() {
        return preferences.getBoolean(PREFS_FIELD_SHOWREAL_NEEDED, false);
    }

    public void setProfileUpdateNeeded(boolean needed) {
        preferences.edit().putBoolean(PREFS_FIELD_PROFILE_NEEDED, needed).apply();
    }

    public void setRealNeeded(boolean needed) {
        preferences.edit().putBoolean(PREFS_FIELD_SHOWREAL_NEEDED, needed).apply();
    }

    public Observable<Profile> getProfile() {
        if (!isLoggedIn()) {
            return Observable.empty();
        }

        return profileProvider.readNullable();
    }

    public Observable<Settings> getSettings() {
        if (!isLoggedIn()) {
            return Observable.empty();
        }
        return settingsProvider.readNullable();
    }

    public String getAuthToken() {
        if (!isLoggedIn()) {
            return "";
        }
        Account account = getAccount();
        return String.format("Token %s", accountManager.peekAuthToken(account, "session"));
    }

    public void updatePassword(String password) {
        Account account = getAccount();
        accountManager.setPassword(account, password);
    }

    public int getSearchLocationType() {
        return TheDistanceApplication.getApplicationComponent(context)
                .preferences().getInt(PREF_FIELD_LOCATION_TYPE, TypeHome);
    }

    public void setSearchLocationType(@LocationType int locationType) {
        TheDistanceApplication.getApplicationComponent(context)
                .preferences().edit()
                .putInt(PREF_FIELD_LOCATION_TYPE, locationType)
                .apply();
    }

    private Account getAccount() {
        return accountManager.getAccountsByType(accountType)[0];
    }

    public void setInstagramToken(String token) {
        if (token == null) {
            return;
        }
        accountManager.setAuthToken(getAccount(), "instagram", token);
    }

    public String getInstagramToken() {
        if (!isLoggedIn()) {
            return "";
        }

        return accountManager.peekAuthToken(getAccount(), "instagram");
    }

    public void logout(final Context context, final boolean expired) {
        if (!isLoggedIn()) {
            removeAccountData(context, expired);
            return;
        }
        Account account = getAccount();
        accountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
            @Override
            public void run(AccountManagerFuture<Boolean> future) {
                removeAccountData(context, expired);

            }
        }, new Handler());

    }

    public Observable<Void> ensureDataClean() {
        preferences.edit().remove(PREFS_FIELD_PROFILE_NEEDED)
                .remove(PREFS_FIELD_SHOWREAL_NEEDED)
                .apply();

        DatabaseHelper.deleteAll(context);
        return TheDistanceApplication.getApplicationComponent(context).cache().evictAll();
    }

    private void removeAccountData(final Context context, final boolean expired) {
        TheDistanceApplication.getApplicationComponent(context)
                .sendbird().remove();

        ensureDataClean().subscribe(new Subscriber<Void>() {
            @Override
            public void onCompleted() {
                Intent intent = new Intent(context, AccountActivity.class);
                intent.putExtra(AccountActivity.EXTRA_EXPIRED, expired);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Void aVoid) {

            }
        });
    }
}
