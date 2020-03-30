package com.showreal.app.features.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.appboy.push.AppboyNotificationUtils;
import com.showreal.app.data.DatabaseHelper;
import com.showreal.app.data.model.Notification;

import nl.nl2312.rxcupboard.RxCupboard;
import nl.nl2312.rxcupboard.RxDatabase;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.TDObservers;

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        String packageName = context.getPackageName();
        String pushReceivedAction = packageName + AppboyNotificationUtils.APPBOY_NOTIFICATION_RECEIVED_SUFFIX;
        String notificationOpenedAction = packageName + AppboyNotificationUtils.APPBOY_NOTIFICATION_OPENED_SUFFIX;
        String action = intent.getAction();

        if (action.equals(pushReceivedAction)) {
            final Notification notification = Notification.with(intent, context);
            RxDatabase cupboard = RxCupboard.withDefault(DatabaseHelper.getConnection(context));
            cupboard.putRx(notification)
                    .subscribeOn(Schedulers.io())
                    .subscribe(TDObservers.<Notification>empty());

        } else if (action.equals(notificationOpenedAction)) {
            NotificationRouter.routeUserWithNotificationOpenedIntent(context, intent);
            AppboyNotificationUtils.handleCancelNotificationAction(context, intent);
        }
    }
}
