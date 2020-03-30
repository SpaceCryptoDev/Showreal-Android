package com.showreal.app.features.notifications;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.DatabaseHelper;
import com.showreal.app.data.model.Notification;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

import nl.nl2312.rxcupboard.RxCupboard;
import nl.nl2312.rxcupboard.RxDatabase;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.lists.interfaces.ListDataSource;
import uk.co.thedistance.components.lists.model.ListContent;
import uk.co.thedistance.thedistancecore.TDObservers;

public class NotificationsDataSource implements ListDataSource<Notification> {

    private final Context context;

    @Override
    public boolean isListComplete() {
        return true;
    }

    private final String JSON_PATH = "news/news.json";
    private final AssetManager assets;
    private final Gson gson;

    public NotificationsDataSource(Context context) {
        assets = context.getAssets();
        gson = TheDistanceApplication.getApplicationComponent(context).gson();
        this.context = context;
    }

    @Override
    public void reset() {

    }

    private Observable<Notification> getEventsObservable() {
        final Calendar calendar = Calendar.getInstance();
        return Observable.defer(new Func0<Observable<Notification>>() {
            @Override
            public Observable<Notification> call() {
                try {
                    InputStream stream = assets.open(JSON_PATH);

                    Type type = new TypeToken<List<Notification>>() {
                    }.getType();
                    List<Notification> notifications = gson.fromJson(new InputStreamReader(stream), type);

                    return Observable.from(notifications);

                } catch (IOException e) {
                    return Observable.error(e);
                }
            }
        }).filter(new Func1<Notification, Boolean>() {
            @Override
            public Boolean call(Notification notification) {
                return notification.endDate.after(calendar.getTime());
            }
        });
    }

    private Observable<Notification> getNotificationsObservable() {
        RxDatabase cupboard = RxCupboard.withDefault(DatabaseHelper.getConnection(context));
        return cupboard.query(Notification.class);
    }

    @Override
    public Observable<ListContent<Notification>> getData() {
        return getEventsObservable()
                .concatWith(getNotificationsObservable())
                .toList()
                .map(new Func1<List<Notification>, ListContent<Notification>>() {
                    @Override
                    public ListContent<Notification> call(List<Notification> notifications) {
                        return new ListContent<>(notifications, true);
                    }
                });
    }

    public void remove(final List<Notification> removed) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                SQLiteDatabase database = DatabaseHelper.getConnection(context);
                database.beginTransaction();
                RxDatabase rxDatabase = RxCupboard.withDefault(database);
                for (Notification notification : removed) {
                    rxDatabase.delete(notification);
                }
                database.setTransactionSuccessful();
                database.endTransaction();
            }
        }).subscribeOn(Schedulers.io()).subscribe(TDObservers.<Boolean>empty());
    }
}
