package com.showreal.app.features.notifications;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.messaging.RemoteMessage;
import com.showreal.app.MainActivity;

public class NotificationRouter {

    public static void routeUserWithNotificationOpenedIntent(Context context, Intent intent) {

        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
    }

    public static Intent getNotificationIntent(Context context, int type, int matchId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_NOTIFICATION_TYPE, type);
        intent.putExtra(MainActivity.EXTRA_NOTIFICATION_MATCH_ID, matchId);
        return intent;
    }
}
