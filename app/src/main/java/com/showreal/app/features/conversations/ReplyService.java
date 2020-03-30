package com.showreal.app.features.conversations;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;

import com.sendbird.android.BaseChannel;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.DatabaseHelper;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.model.Message;
import com.showreal.app.data.model.MessageNotification;
import com.showreal.app.features.notifications.SRFirebaseMessagingService;

import nl.nl2312.rxcupboard.RxCupboard;
import nl.nl2312.rxcupboard.RxDatabase;
import rx.Subscriber;
import rx.functions.Action1;
import uk.co.thedistance.thedistancecore.TDSubscribers;

public class ReplyService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        doReply(intent);

        return START_STICKY;
    }

    private void doReply(Intent intent) {
        final String channel = intent.getStringExtra("channel");
        final String message = getMessageText(intent);
        if (message == null) {
            finish(channel);
            return;
        }

        final RxDatabase cupboard = RxCupboard.withDefault(DatabaseHelper.getConnection(this));
        cupboard.query(MessageNotification.class, "channel = ?", channel)
                .subscribe(TDSubscribers.ignorant(new Action1<MessageNotification>() {
                    @Override
                    public void call(MessageNotification messageNotification) {
                        cupboard.delete(messageNotification);
                    }
                }));

        SendBirdHelper sendBirdHelper = TheDistanceApplication.getApplicationComponent(this).sendbird();

        sendBirdHelper.initialise()
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        reply(channel, message);
                    }
                });
    }

    private void reply(final String channel, final String message) {
        GroupChannel.getChannel(channel, new GroupChannel.GroupChannelGetHandler() {
            @Override
            public void onResult(GroupChannel groupChannel, SendBirdException e) {
                if (groupChannel == null) {
                    finish(channel);
                    return;
                }
                groupChannel.markAsRead();
                groupChannel.sendUserMessage(message, new BaseChannel.SendUserMessageHandler() {
                    @Override
                    public void onSent(UserMessage userMessage, SendBirdException e) {
                        if (e != null) {
                            finish(channel);
                            return;
                        }
                        Message message = Message.with(userMessage);

                        final SendBirdHelper helper = TheDistanceApplication.getApplicationComponent(ReplyService.this).sendbird();
                        helper.getCupboard().put(message);
                        finish(channel);
                    }
                });
            }
        });
    }

    private void finish(String channel) {
        NotificationManagerCompat.from(this).cancel(channel.hashCode());
        stopSelf();
    }

    private String getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(SRFirebaseMessagingService.KEY_TEXT_REPLY).toString();
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
