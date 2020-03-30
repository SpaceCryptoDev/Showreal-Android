package com.showreal.app.features.notifications;

import com.appboy.Appboy;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Device;

import java.io.IOException;

import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.TDObservers;

public class SRFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        if (!TheDistanceApplication.getApplicationComponent(this).accountHelper().isLoggedIn()) {
            return;
        }

        try {
            String token = FirebaseInstanceId.getInstance().getToken("139554035552", "FCM");
            Appboy.getInstance(getApplicationContext()).registerAppboyPushMessages(token);

            SendBird.registerPushTokenForCurrentUser(token, new SendBird.RegisterPushTokenWithStatusHandler() {
                @Override
                public void onRegistered(SendBird.PushTokenRegistrationStatus pushTokenRegistrationStatus, SendBirdException e) {

                }
            });

            TheDistanceApplication.getApplicationComponent(this).api()
                    .registerDevice(new Device(FirebaseInstanceId.getInstance().getToken(), FirebaseInstanceId.getInstance().getId()))
                    .subscribeOn(Schedulers.io())
                    .subscribe(TDObservers.<Void>empty());

        } catch (IOException ignored) {
        }
    }
}
