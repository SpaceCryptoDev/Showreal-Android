package com.showreal.app.features.conversations;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.PreviousMessageListQuery;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.ClearConversation;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.Message;
import com.showreal.app.data.model.Profile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nl.nl2312.rxcupboard.DatabaseChange;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.contentloading.DataSource;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;
import uk.co.thedistance.thedistancecore.TDObservers;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class MessagesDataSource implements DataSource<List<Message>>, DataUploader<Message, Message> {

    private final Context context;
    private final Match match;
    private final SendBirdHelper sendbird;
    private final ShowRealApi api;
    private GroupChannel channel;
    private Message message;

    public MessagesDataSource(Context context, Match match) {
        this.match = match;
        this.context = context;
        sendbird = TheDistanceApplication.getApplicationComponent(context).sendbird();
        api = TheDistanceApplication.getApplicationComponent(context).api();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(Message content) {
        this.message = content;
    }

    @Override
    public Observable<Message> getUpload() {
        return getGroupChannel()
                .flatMap(new Func1<GroupChannel, Observable<Message>>() {
                    @Override
                    public Observable<Message> call(final GroupChannel channel) {
                        return Observable.create(new Observable.OnSubscribe<Message>() {
                            @Override
                            public void call(final Subscriber<? super Message> subscriber) {
                                if (message.mediaUrl != null) {
                                    File file = new File(message.mediaUrl);
                                    channel.sendFileMessage(file, file.getName(), message.mediaType, (int) message.mediaSize, "", new BaseChannel.SendFileMessageHandler() {
                                        @Override
                                        public void onSent(FileMessage fileMessage, SendBirdException e) {
                                            if (e != null) {
                                                subscriber.onError(e);
                                            } else {
                                                Message message = Message.with(fileMessage);
                                                sendbird.getCupboard().put(message);
                                                subscriber.onNext(message);
                                            }
                                            subscriber.onCompleted();
                                        }

                                    });
                                } else {
                                    channel.sendUserMessage(message.text, new BaseChannel.SendUserMessageHandler() {
                                        @Override
                                        public void onSent(UserMessage userMessage, SendBirdException e) {
                                            if (e != null) {
                                                subscriber.onError(e);
                                            } else {
                                                Message message = Message.with(userMessage);
                                                sendbird.getCupboard().put(message);
                                                subscriber.onNext(message);
                                            }
                                            subscriber.onCompleted();
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
    }

    @Override
    public Observable<List<Message>> getData() {
        return getGroupChannel()
                .flatMap(new Func1<GroupChannel, Observable<List<Message>>>() {
                    @Override
                    public Observable<List<Message>> call(GroupChannel groupChannel) {
                        return previousMessages(groupChannel);
                    }
                });
    }

    private Observable<List<Message>> previousMessages(final GroupChannel channel) {
        final PreviousMessageListQuery query = channel.createPreviousMessageListQuery();
        return Observable.create(new Observable.OnSubscribe<List<BaseMessage>>() {
            @Override
            public void call(final Subscriber<? super List<BaseMessage>> subscriber) {
                query.load(50, true, new PreviousMessageListQuery.MessageListQueryResult() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                        if (e != null) {
                            subscriber.onError(e);
                        } else {
                            channel.markAsRead();
                            sendbird.getDatabase().beginTransaction();
                            subscriber.onNext(list);
                            api.resetMatch(match.id)
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(TDObservers.<Void>empty());
                        }
                        subscriber.onCompleted();
                    }
                });
            }
        }).flatMapIterable(new Func1<List<BaseMessage>, Iterable<BaseMessage>>() {
            @Override
            public Iterable<BaseMessage> call(List<BaseMessage> baseMessages) {
                return baseMessages;
            }
        }).map(new Func1<BaseMessage, Message>() {
            @Override
            public Message call(BaseMessage baseMessage) {
                Message message = sendbird.getCupboard().buildQuery(Message.class)
                        .withSelection("messageId = ?", String.valueOf(baseMessage.getMessageId()))
                        .get();

                if (message != null) {
                    return message;
                }

                message = Message.with(baseMessage);
                sendbird.getCupboard().put(message);
                return message;
            }
        }).toList()
                .doOnNext(new Action1<List<Message>>() {
                    @Override
                    public void call(List<Message> messages) {
                        sendbird.getDatabase().setTransactionSuccessful();
                        sendbird.getDatabase().endTransaction();
                        sendbird.resetUnreadCount(channel.getUrl());
                    }
                });
    }

    private Observable<GroupChannel> channelObservable;

    Observable<GroupChannel> getGroupChannel() {
        if (channel != null && sendbird.isConnected()) {
            return Observable.just(channel);
        }
        if (channelObservable != null) {
            return channelObservable;
        }

        if (TextUtils.isEmpty(match.conversationUrl)) {
            return sendbird.initialise()
                    .flatMap(new Func1<Boolean, Observable<GroupChannel>>() {
                        @Override
                        public Observable<GroupChannel> call(Boolean aBoolean) {
                            sendbird.ensureHandlers();
                            return Observable.create(new Observable.OnSubscribe<GroupChannel>() {
                                @Override
                                public void call(final Subscriber<? super GroupChannel> subscriber) {
                                    List<String> ids = new ArrayList<>(2);
                                    ids.add(match.profile.chatId);
                                    Profile profile = TheDistanceApplication.getApplicationComponent(context).accountHelper().getProfile().toBlocking().first();
                                    ids.add(profile.chatId);
                                    GroupChannel.createChannelWithUserIds(ids, true, new GroupChannel.GroupChannelCreateHandler() {
                                        @Override
                                        public void onResult(GroupChannel groupChannel, SendBirdException e) {
                                            channelObservable = null;
                                            if (e != null) {
                                                subscriber.onError(e);
                                            } else {
                                                channel = groupChannel;
                                                match.conversationUrl = channel.getUrl();
                                                subscriber.onNext(groupChannel);
                                            }
                                            subscriber.onCompleted();
                                        }
                                    });
                                }
                            }).doOnNext(new Action1<GroupChannel>() {
                                @Override
                                public void call(GroupChannel channel) {
                                    channel.setPushPreference(true, null);
                                }
                            });
                        }
                    });
        }
        return sendbird.initialise()
                .flatMap(new Func1<Boolean, Observable<GroupChannel>>() {
                    @Override
                    public Observable<GroupChannel> call(Boolean aBoolean) {
                        sendbird.ensureHandlers();
                        return Observable.create(new Observable.OnSubscribe<GroupChannel>() {
                            @Override
                            public void call(final Subscriber<? super GroupChannel> subscriber) {
                                GroupChannel.getChannel(match.conversationUrl, new GroupChannel.GroupChannelGetHandler() {
                                    @Override
                                    public void onResult(GroupChannel groupChannel, SendBirdException e) {
                                        channelObservable = null;
                                        if (e != null) {
                                            subscriber.onError(e);
                                        } else {
                                            channel = groupChannel;
                                            subscriber.onNext(groupChannel);
                                        }
                                        subscriber.onCompleted();
                                    }
                                });
                            }
                        }).doOnNext(new Action1<GroupChannel>() {
                            @Override
                            public void call(GroupChannel channel) {
                                channel.setPushPreference(true, null);
                            }
                        });
                    }
                });
    }

    Observable<List<Message>> getCached() {
        if (TextUtils.isEmpty(match.conversationUrl)) {
            return Observable.empty();
        }
        return Observable.create(new Observable.OnSubscribe<List<Message>>() {
            @Override
            public void call(Subscriber<? super List<Message>> subscriber) {
                List<Message> messages = sendbird.getCupboard().buildQuery(Message.class)
                        .withSelection("channel = ?", match.conversationUrl)
                        .orderBy("timeSent desc").limit(50).list();
                subscriber.onNext(messages);
                subscriber.onCompleted();
            }
        });
    }

    Observable<DatabaseChange<Message>> getChanges() {
        return sendbird.getCupboard().changes(Message.class)
                .filter(new Func1<DatabaseChange<Message>, Boolean>() {
                    @Override
                    public Boolean call(DatabaseChange<Message> message) {
                        Log.i("CHANGE", "database change, channel is " + (channel == null ? "null" : "there"));
                        return channel != null && message.entity().channel.equals(channel.getUrl());
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public String getMessage() {
        if (message == null) {
            return null;
        }
        return message.text;
    }
}
