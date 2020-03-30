package com.showreal.app.features.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.ContextCompat;

import com.appboy.support.IntentUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.PreviousMessageListQuery;
import com.sendbird.android.SendBirdException;
import com.showreal.app.MainActivity;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.DatabaseHelper;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.Message;
import com.showreal.app.data.model.MessageNotification;
import com.showreal.app.features.conversations.ConversationActivity;
import com.showreal.app.features.conversations.ReplyService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivecache.Provider;
import nl.nl2312.rxcupboard.RxCupboard;
import nl.nl2312.rxcupboard.RxDatabase;
import nl.qbusict.cupboard.DatabaseCompartment;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SRFirebaseMessagingService extends FirebaseMessagingService {

    public static final String ACTION_SENDBIRD_NOTIFICATION = "com.showreal.app.conversations.NOTIFICATION";
    public static final String EXTRA_CONVERSATION_URL = "conversation_url";
    public static final String EXTRA_MESSAGE = "message";
    private static final String PREFS_FIELD_NOTIF_ID = "notif_id";

    public static final String ACTION_NOTIFICATION_TOAST = "com.showreal.app.intent.ACTION_NOTIFICATION_TOAST";
    public static final String EXTRA_FROM_SENDBIRD = "from_sendbird";
    public static final String EXTRA_TYPE = "type";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (!remoteMessage.getData().containsKey("sendbird")) {
            sendNotification(remoteMessage);
            return;
        }

        final String text = remoteMessage.getData().get("message");

        TheDistanceApplication application = TheDistanceApplication.get(getApplicationContext());
        if (application.isForeground()) {
            Intent toastIntent = new Intent(ACTION_NOTIFICATION_TOAST);
            toastIntent.putExtra(EXTRA_MESSAGE, "You received a new message.");
            sendBroadcast(toastIntent);
            return;
        }

        JsonObject payload = new JsonParser().parse(remoteMessage.getData().get("sendbird")).getAsJsonObject();

        final String channel = payload.get("channel").getAsJsonObject().get("channel_url").getAsString();

        TheDistanceApplication.getApplicationComponent(getApplicationContext())
                .sendbird().initialise()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        addMessage(channel, text);
                    }
                });

        sendMessageNotification(text, payload);
    }

    private static AtomicInteger notifId;

    private static int getNotifId(Context context) {
        if (notifId == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            notifId = new AtomicInteger(preferences.getInt(PREFS_FIELD_NOTIF_ID, 0));
        }
        return notifId.incrementAndGet();
    }

    private void sendNotification(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (!data.containsKey("context")) {
            return;
        }

        String context = data.get("context");

        int type = -1;
        int matchId = -1;
        try {
            JsonObject cData = new JsonParser().parse(context).getAsJsonObject();
            type = cData.has(com.showreal.app.data.model.Notification.EXTRA_TYPE) ? cData.get(com.showreal.app.data.model.Notification.EXTRA_TYPE).getAsInt() : -1;
            if (cData.has("pk")) {
                matchId = cData.get("pk").getAsInt();
            }
        } catch (Exception ignored) {
        }

        String message = data.get("message");

        com.showreal.app.data.model.Notification notification = com.showreal.app.data.model.Notification.with(remoteMessage, getApplicationContext());
        RxDatabase cupboard = RxCupboard.withDefault(DatabaseHelper.getConnection(this));
        cupboard.put(notification);

        TheDistanceApplication application = TheDistanceApplication.get(getApplicationContext());
        if (application.isForeground()) {
            Intent toastIntent = new Intent(ACTION_NOTIFICATION_TOAST);
            toastIntent.putExtra(EXTRA_TYPE, type);
            toastIntent.putExtra(EXTRA_MESSAGE, message);
            sendBroadcast(toastIntent);
            return;
        }

        Intent pushOpenedIntent = NotificationRouter.getNotificationIntent(getApplicationContext(), type, matchId);

        PendingIntent pushOpenedPendingIntent = PendingIntent.getActivity(getApplicationContext(), IntentUtils.getRequestCode(), pushOpenedIntent, 0);

        Bitmap largeIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.showreal_profile);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle("ShowReal")
                .setContentText(message)
                .setWhen(remoteMessage.getSentTime())
                .setShowWhen(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setColor(ContextCompat.getColor(SRFirebaseMessagingService.this, R.color.red))
                .setContentIntent(pushOpenedPendingIntent);

        if (type != -1) {
            builder.setGroup(String.valueOf(type));
        }

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(getNotifId(getApplicationContext()), builder.build());

        NotificationFactory.showSummary(getApplicationContext(), message, type, matchId);


    }

    private void addMessage(final String channel, final String text) {
        GroupChannel.getChannel(channel, new GroupChannel.GroupChannelGetHandler() {
            @Override
            public void onResult(GroupChannel groupChannel, SendBirdException e) {
                if (groupChannel == null) {
                    return;
                }
                PreviousMessageListQuery query = groupChannel.createPreviousMessageListQuery();
                query.load(10, true, new PreviousMessageListQuery.MessageListQueryResult() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                        if (list == null) {
                            return;
                        }
                        SendBirdHelper helper = TheDistanceApplication.getApplicationComponent(SRFirebaseMessagingService.this).sendbird();
                        for (BaseMessage baseMessage : list) {
                            Message message = Message.with(baseMessage);
                            helper.getCupboard().put(message);
                        }

                        helper.incrementUnreadCount(channel);

                        Intent intent = new Intent(ACTION_SENDBIRD_NOTIFICATION);
                        intent.putExtra(EXTRA_CONVERSATION_URL, channel);
                        intent.putExtra(EXTRA_MESSAGE, text);
                        sendBroadcast(intent);
                    }
                });

            }
        });
    }

    public static final String KEY_TEXT_REPLY = "key_text_reply";
    private static final String KEY_GROUP = "messages";

    private void sendMessageNotification(final String message, final JsonObject payload) {
        final String conversationUrl = payload.get("channel").getAsJsonObject().get("channel_url").getAsString();

        RxDatabase cupboard = RxCupboard.withDefault(DatabaseHelper.getConnection(this));

        DatabaseCompartment.QueryBuilder<MessageNotification> queryBuilder = cupboard.buildQuery(MessageNotification.class)
                .withSelection("channel = ?", conversationUrl)
                .orderBy("timeReceived desc")
                .limit(4);

        cupboard.query(queryBuilder)
                .toList()
                .subscribe(new Subscriber<List<MessageNotification>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        sendMessageNotification(message, payload, new ArrayList<MessageNotification>());
                    }

                    @Override
                    public void onNext(List<MessageNotification> messageNotifications) {
                        Collections.reverse(messageNotifications);
                        sendMessageNotification(message, payload, messageNotifications);
                    }
                });
    }

    private void sendMessageNotification(String message, JsonObject payload, List<MessageNotification> messageNotifications) {
        final String conversationUrl = payload.get("channel").getAsJsonObject().get("channel_url").getAsString();
        String sender = payload.get("sender").getAsJsonObject().get("name").getAsString();

        String replyLabel = getResources().getString(R.string.hint_chat);
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build();

        Intent notifyIntent;

        String chatId = payload.get("sender").getAsJsonObject().get("id").getAsString();
        Provider<List<Match>> cache = TheDistanceApplication.getApplicationComponent(SRFirebaseMessagingService.this).cache().<List<Match>>provider().withKey("matches_stale");
        List<Match> matches = cache.readNullable().toBlocking().first();

        Match messageMatch = null;
        if (matches != null) {
            for (Match match : matches) {
                if (match.profile.chatId.equals(chatId)) {
                    messageMatch = match;
                    break;
                }
            }
        }

        if (messageMatch == null) {
            notifyIntent = new Intent(SRFirebaseMessagingService.this, MainActivity.class);
            notifyIntent.putExtra(MainActivity.EXTRA_TAB, 2);
        } else {
            notifyIntent = new Intent(SRFirebaseMessagingService.this, ConversationActivity.class);
            notifyIntent.putExtra(ConversationActivity.EXTRA_MATCH, messageMatch);
            notifyIntent.putExtra(ConversationActivity.EXTRA_FROM_NOTIFICATION, true);
        }

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        SRFirebaseMessagingService.this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        Intent replyIntent = new Intent(SRFirebaseMessagingService.this, ReplyService.class);
        replyIntent.putExtra("channel", conversationUrl);
        PendingIntent replyPendingIntent = PendingIntent.getService(SRFirebaseMessagingService.this, 0, replyIntent, 0);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_send_red_24dp,
                getString(R.string.button_reply), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(
                new NotificationCompat.Builder(getApplicationContext())
                        .setContentTitle("")
        );

        if (messageNotifications == null) {
            messageNotifications = new ArrayList<>();
        }

        MessageNotification notification = new MessageNotification(message, conversationUrl, new Date());
        messageNotifications.add(notification);

        RxDatabase cupboard = RxCupboard.withDefault(DatabaseHelper.getConnection(this));
        cupboard.put(notification);

        if (messageNotifications.size() > 5) {
            messageNotifications = messageNotifications.subList(messageNotifications.size() - 5, messageNotifications.size() - 1);
        }

        for (MessageNotification messageNotification : messageNotifications) {
            style.addLine(messageNotification.message);
        }

        Notification newMessageNotification =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.tab_messages)
                        .setContentTitle(sender)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setStyle(style)
                        .addAction(action)
                        .setColor(ContextCompat.getColor(SRFirebaseMessagingService.this, R.color.red))
                        .setContentIntent(notifyPendingIntent)
                        .setGroup(KEY_GROUP)
                        .build();

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(conversationUrl.hashCode(), newMessageNotification);
    }
}
