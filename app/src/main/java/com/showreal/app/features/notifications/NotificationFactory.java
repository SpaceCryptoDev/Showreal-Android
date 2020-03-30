package com.showreal.app.features.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;

import com.appboy.IAppboyNotificationFactory;
import com.appboy.configuration.XmlAppConfigurationProvider;
import com.appboy.push.AppboyNotificationActionUtils;
import com.appboy.push.AppboyNotificationRemoteViewsUtils;
import com.appboy.push.AppboyNotificationUtils;
import com.appboy.push.AppboyWearableNotificationUtils;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;

import java.util.HashMap;
import java.util.Locale;

import uk.co.thedistance.thedistancecore.Version;

public class NotificationFactory implements IAppboyNotificationFactory {

    private static HashMap<String, Integer> currentMessages = new HashMap<>();

    public NotificationFactory() {

    }

    @Override
    public Notification createNotification(XmlAppConfigurationProvider appConfigurationProvider, Context context, Bundle notificationExtras, Bundle appboyExtras) {


        NotificationCompat.Builder notificationBuilder = (new NotificationCompat.Builder(context)).setAutoCancel(true);
        AppboyNotificationUtils.setTitleIfPresent(notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setContentIfPresent(notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setTickerIfPresent(notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setContentIntentIfPresent(context, notificationBuilder, notificationExtras);
        int smallNotificationIconResourceId = AppboyNotificationUtils.setSmallIcon(appConfigurationProvider, notificationBuilder);
        boolean usingLargeIcon = AppboyNotificationUtils.setLargeIconIfPresentAndSupported(context, appConfigurationProvider, notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setSoundIfPresentAndSupported(notificationBuilder, notificationExtras);
        if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 16) {
            RemoteViews remoteViews = AppboyNotificationRemoteViewsUtils.createMultiLineContentNotificationView(context, notificationExtras, smallNotificationIconResourceId, !usingLargeIcon);
            if (remoteViews != null) {
                notificationBuilder.setContent(remoteViews);

                int type = Integer.parseInt(appboyExtras.getString(com.showreal.app.data.model.Notification.EXTRA_TYPE, "-1"));
                notificationBuilder.setGroup(String.valueOf(type));

                Notification notification = notificationBuilder.build();
                showSummary(context, notificationBuilder.mContentText, type, -1);

                return notification;
            }
        }

        AppboyNotificationUtils.setSummaryTextIfPresentAndSupported(notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setPriorityIfPresentAndSupported(notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setStyleIfSupported(context, notificationBuilder, notificationExtras, appboyExtras);
        AppboyNotificationActionUtils.addNotificationActions(context, notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setAccentColorIfPresentAndSupported(appConfigurationProvider, notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setCategoryIfPresentAndSupported(notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setVisibilityIfPresentAndSupported(notificationBuilder, notificationExtras);
        AppboyNotificationUtils.setPublicVersionIfPresentAndSupported(context, appConfigurationProvider, notificationBuilder, notificationExtras);
        AppboyWearableNotificationUtils.setWearableNotificationFeaturesIfPresentAndSupported(context, notificationBuilder, notificationExtras);

        int type = Integer.parseInt(appboyExtras.getString(com.showreal.app.data.model.Notification.EXTRA_TYPE, "-1"));
        notificationBuilder.setGroup(String.valueOf(type));

        Notification notification = notificationBuilder.build();

        if (TheDistanceApplication.get(context).isForeground()) {
            CharSequence message = notificationBuilder.mContentText;
            if (message == null) {
                message = notificationBuilder.mContentTitle;
            }
            if (message == null) {
                message = notification.tickerText;
            }
            Intent toastIntent = new Intent(SRFirebaseMessagingService.ACTION_NOTIFICATION_TOAST);
            toastIntent.putExtra(SRFirebaseMessagingService.EXTRA_MESSAGE, message);
            context.sendBroadcast(toastIntent);
            return null;
        }

        showSummary(context, notificationBuilder.mContentText, type, -1);

        return notification;
    }


    public static void showSummary(Context context, CharSequence text, int type, int matchId) {
        if (!Version.isNougat()) {
            return;
        }
        if (type != com.showreal.app.data.model.Notification.Event && type != com.showreal.app.data.model.Notification.Matched && type != com.showreal.app.data.model.Notification.MatchRealUpdated
                && type != com.showreal.app.data.model.Notification.Message && type != com.showreal.app.data.model.Notification.SecondChanceRealUpdated
                && type != com.showreal.app.data.model.Notification.NewQuestion) {
            return;
        }

        String groupKey = String.valueOf(type);
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.showreal_profile);

        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon);

        String summary;
        if (Version.isMarshmallow()) {
            StatusBarNotification[] notifications = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications();

            int count = 1;

            for (StatusBarNotification notification : notifications) {
                if (notification.getNotification().getGroup().equals(groupKey) && currentMessages.containsKey(String.valueOf(type))) {
                    Integer messages = currentMessages.get(String.valueOf(type));
                    if (messages != null) {
                        count += messages;
                    }
                    currentMessages.put(String.valueOf(type), count);
                    break;
                }
            }

            summary = String.format(Locale.getDefault(), "%d new %s", count, getSummaryText(type, count));
        } else {
            summary = String.format(Locale.getDefault(), "new %s", getSummaryText(type, 2));
        }

        summaryBuilder.setContentText(summary)
                .build();

        NotificationManagerCompat.from(context)
                .notify(type, summaryBuilder.build());
    }

    private static String getSummaryText(int type, int count) {
        switch (type) {
            case com.showreal.app.data.model.Notification.Event:
                return "ShowReal Event" + (count > 1 ? "s" : "");
            case com.showreal.app.data.model.Notification.Matched:
                return "match" + (count > 1 ? "es" : "");
            case com.showreal.app.data.model.Notification.MatchRealUpdated:
                return "match real update" + (count > 1 ? "s" : "");
            case com.showreal.app.data.model.Notification.Message:
                return "message" + (count > 1 ? "s" : "");
            case com.showreal.app.data.model.Notification.NewQuestion:
                return "question" + (count > 1 ? "s" : "");
        }

        return "notification" + (count > 1 ? "s" : "");
    }
}
