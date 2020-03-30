package com.showreal.app.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.databinding.ObservableBoolean;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.Message;
import com.showreal.app.data.model.Profile;
import com.showreal.app.features.notifications.SRFirebaseMessagingService;
import com.showreal.app.injection.ApplicationComponent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivecache.Provider;
import nl.nl2312.rxcupboard.RxCupboard;
import nl.nl2312.rxcupboard.RxDatabase;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.TDObservers;
import uk.co.thedistance.thedistancecore.TDSubscribers;

public class SendBirdHelper extends SendBird.ChannelHandler implements SendBird.ConnectionHandler {

    public static final String ACTION_SENDBIRD_MESSAGE = "com.showreal.app.conversations.MESSAGE";
    private static final String PREFS_MESSAGES = "unread_counts";
    public static final String EXTRA_CONVERSATION_URL = "conversation_url";
    private final Context context;
    private final SharedPreferences preferences;
    private SQLiteDatabase database;
    private RxDatabase cupboard;
    private final ConnectivityManager connectionManager;

    private boolean connectionAvailable = false;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                boolean connected;
                if (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)) {
                    connected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                } else {
                    NetworkInfo activeNetwork = connectionManager.getActiveNetworkInfo();
                    connected = activeNetwork != null && activeNetwork.isConnected();
                }

                if (connected != connectionAvailable) {
                    if (!connectionAvailable) {
                        connected = false;
                        initialise().subscribe(TDObservers.<Boolean>empty());
                    }
                    connectionAvailable = connected;
                }
            }
        }
    };
    public ObservableBoolean sendbirdAvailable = new ObservableBoolean(false);
    private boolean connected;

    public SendBirdHelper(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREFS_MESSAGES, Context.MODE_PRIVATE);
        database = DatabaseHelper.getConnection(context);
        cupboard = RxCupboard.withDefault(database);

        connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectionManager.getActiveNetworkInfo();
        connectionAvailable = activeNetwork != null && activeNetwork.isConnected();
        context.registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    Observable<Boolean> initObservable;

    public Observable<Boolean> initialise() {
        if (connected) {
            return Observable.just(true);
        }

        if (initObservable == null) {
            initObservable = initInternal().replay().autoConnect();
        }
        return initObservable;
    }

    private Observable<Boolean> initInternal() {
        return connect()
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        SendBird.addConnectionHandler("SHOWREAL_CONNECTION", SendBirdHelper.this);
                        SendBird.addChannelHandler("CHANNEL_HANDLER", SendBirdHelper.this);
                        Log.d("SENDBIRD", "registered");
                        sendbirdAvailable.set(true);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d("SENDBIRD", "failed");
                        sendbirdAvailable.set(false);
                        initObservable = null;
                    }
                });
    }

    public RxDatabase getCupboard() {
        ensureDatabase();
        return cupboard;
    }

    private void ensureDatabase() {
        if (!database.isOpen()) {
            database = DatabaseHelper.getConnection(context);
            cupboard = RxCupboard.withDefault(database);
        }
    }

    public void destroy() {
        SendBird.removeConnectionHandler("SHOWREAL_CONNECTION");
        SendBird.removeChannelHandler("CHANNEL_HANDLER");
        database.close();
    }

    private Observable<Boolean> connect() {
        return TheDistanceApplication.getApplicationComponent(context).accountHelper()
                .getProfile()
                .flatMap(new Func1<Profile, Observable<User>>() {
                    @Override
                    public Observable<User> call(Profile profile) {
                        return connect(profile);
                    }
                })
                .flatMap(new Func1<User, Observable<SendBird.PushTokenRegistrationStatus>>() {
                    @Override
                    public Observable<SendBird.PushTokenRegistrationStatus> call(User user) {
                        return registerPush();
                    }
                })
                .map(new Func1<SendBird.PushTokenRegistrationStatus, Boolean>() {
                    @Override
                    public Boolean call(SendBird.PushTokenRegistrationStatus pushTokenRegistrationStatus) {
                        return true;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<User> connect(final Profile profile) {
        return Observable.create(new Observable.OnSubscribe<User>() {
            @Override
            public void call(final Subscriber<? super User> subscriber) {
                SendBird.connect(profile.chatId, new SendBird.ConnectHandler() {
                    @Override
                    public void onConnected(User user, SendBirdException e) {
                        if (e != null) {
                            subscriber.onError(e);
                        } else {
                            connected = true;
                            subscriber.onNext(user);
                        }
                        subscriber.onCompleted();
                    }
                });
            }
        }).subscribeOn(Schedulers.io());
    }

    private Observable<SendBird.PushTokenRegistrationStatus> registerPush() {
        return Observable.create(new Observable.OnSubscribe<SendBird.PushTokenRegistrationStatus>() {
            @Override
            public void call(final Subscriber<? super SendBird.PushTokenRegistrationStatus> subscriber) {
                String token;
                try {
                    token = FirebaseInstanceId.getInstance().getToken("139554035552", "FCM");
                } catch (IOException e) {
                    token = FirebaseInstanceId.getInstance().getToken();
                }

                SendBird.registerPushTokenForCurrentUser(token, new SendBird.RegisterPushTokenWithStatusHandler() {
                    @Override
                    public void onRegistered(SendBird.PushTokenRegistrationStatus pushTokenRegistrationStatus, SendBirdException e) {
                        if (e != null) {
                            subscriber.onError(e);
                        } else {
                            subscriber.onNext(pushTokenRegistrationStatus);
                        }
                        subscriber.onCompleted();
                    }
                });
            }
        }).subscribeOn(Schedulers.io());
    }

    public int getTotalUnreadCount() {
        int count = 0;
        Map<String, ?> all = preferences.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Integer) {
                count += (int) value;
            }

        }
        return count;
    }

    public void ensureHandlers() {
        SendBird.addConnectionHandler("SHOWREAL_CONNECTION", SendBirdHelper.this);
        SendBird.addChannelHandler("CHANNEL_HANDLER", SendBirdHelper.this);
    }

    public int getUnreadCount(String conversationUrl) {
        return preferences.getInt(conversationUrl, 0);
    }

    public void resetUnreadCount(String conversationUrl) {
        preferences.edit()
                .putInt(conversationUrl, 0)
                .apply();
    }

    public void setUnreadCount(String conversationUrl, int count) {
        preferences.edit()
                .putInt(conversationUrl, count)
                .apply();
    }

    public void incrementUnreadCount(String conversationUrl) {
        preferences.edit()
                .putInt(conversationUrl, getUnreadCount(conversationUrl) + 1)
                .apply();
    }

    @Override
    public void onReconnectStarted() {
        sendbirdAvailable.set(false);
    }

    @Override
    public void onReconnectSucceeded() {
        sendbirdAvailable.set(true);
        connected = true;
    }

    @Override
    public void onReconnectFailed() {
        sendbirdAvailable.set(false);
        connected = false;
        initObservable = null;
        initialise().subscribeOn(Schedulers.io())
                .subscribe(TDObservers.<Boolean>empty());
    }

    @Override
    public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
        Message message = Message.with(baseMessage);
        ensureDatabase();
        cupboard.put(message);

        incrementUnreadCount(message.channel);
        Intent intent = new Intent(ACTION_SENDBIRD_MESSAGE);
        intent.putExtra(EXTRA_CONVERSATION_URL, message.channel);
        context.sendBroadcast(intent);

        Intent toastIntent = new Intent(SRFirebaseMessagingService.ACTION_NOTIFICATION_TOAST);
        toastIntent.putExtra(SRFirebaseMessagingService.EXTRA_MESSAGE, "You received a new message.");
        toastIntent.putExtra(SRFirebaseMessagingService.EXTRA_FROM_SENDBIRD, true);
        context.sendBroadcast(toastIntent);
    }

    @Override
    public void onMessageDeleted(BaseChannel channel, long msgId) {
        super.onMessageDeleted(channel, msgId);

        ensureDatabase();

        cupboard.query(Message.class, "messageId = ?", String.valueOf(msgId))
                .subscribe(TDSubscribers.ignorant(new Action1<Message>() {
                    @Override
                    public void call(Message message) {
                        cupboard.delete(message);
                    }
                }));
    }

    public SQLiteDatabase getDatabase() {
        ensureDatabase();
        return database;
    }

    public void updateMatch(final Match match) {
        ApplicationComponent component = TheDistanceApplication.getApplicationComponent(context);
        final Provider<List<Match>> cache = component.cache().<List<Match>>provider().lifeCache(30, TimeUnit.MINUTES).withKey("matches");
        final Provider<List<Match>> cache_stale = component.cache().<List<Match>>provider().withKey("matches_stale");

        cache_stale.readNullable()
                .flatMap(new Func1<List<Match>, Observable<List<Match>>>() {
                    @Override
                    public Observable<List<Match>> call(List<Match> matches) {
                        Match oldMatch = null;
                        for (Match match1 : matches) {
                            if (match1.id == match.id) {
                                oldMatch = match1;
                                break;
                            }
                        }
                        if (oldMatch != null) {
                            matches.remove(oldMatch);
                        }
                        matches.add(match);

                        return Observable.just(matches)
                                .compose(cache.replace())
                                .compose(cache_stale.replace());
                    }
                })
                .subscribe(TDObservers.<List<Match>>empty());
    }

    public void remove() {
        connected = false;
        preferences.edit().clear().apply();
        SendBird.disconnect(null);
        initObservable = null;
        SendBird.unregisterPushTokenAllForCurrentUser(new SendBird.UnregisterPushTokenHandler() {
            @Override
            public void onUnregistered(SendBirdException e) {
                destroy();
            }
        });
    }

    public boolean isConnected() {
        return connected;
    }
}
