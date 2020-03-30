package com.showreal.app.data.model;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.WorkerThread;

import com.appboy.Constants;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.showreal.app.TheDistanceApplication;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.lists.interfaces.Sortable;

public class Notification implements Sortable {

    public static final String EXTRA_TYPE = "notification_type";
    public static final int Matched = 0;
    public static final int Message = 1;
    public static final int MatchRealUpdated = 2;
    public static final int SecondChanceRealUpdated = 3;
    public static final int NewQuestion = 4;
    public static final int Event = 5;

    public Long _id;
    public String title;
    public String summary;
    public int type;
    public Date startDate;
    public Date endDate;
    public String url;
    public int matchId = -1;
    public String image;

    public Notification() {
    }

    public String getTitle() {
        if (type == Event) {
            return String.format("ShowReal %s", title);
        }
        return title;
    }

    @WorkerThread
    public static Notification with(Intent intent, Context context) {
        String title = intent.getStringExtra(Constants.APPBOY_PUSH_TITLE_KEY);
        String message = intent.getStringExtra(Constants.APPBOY_PUSH_CONTENT_KEY);
        Bundle extras = intent.getBundleExtra(Constants.APPBOY_PUSH_EXTRAS_KEY);

        int type = Integer.parseInt(extras.getString(EXTRA_TYPE, "-1"));

        final Notification notification = new Notification();
        notification.title = title;
        notification.summary = message;
        notification.type = type;
        notification.startDate = Calendar.getInstance().getTime();

        switch (type) {
            case SecondChanceRealUpdated:
            case Matched:
            case MatchRealUpdated:
                notification.matchId = Integer.parseInt(extras.getString("pk", "-1"));
                if (notification.matchId != -1) {
                    List<Profile> profiles = TheDistanceApplication.getApplicationComponent(context)
                            .cache().<List<Profile>>provider().expirable(false).withKey("profiles")
                            .readNullable()
                            .toBlocking()
                            .first();
                    if (profiles != null) {
                        try {
                            notification.image = Observable.just(profiles)
                                    .flatMapIterable(new Func1<List<Profile>, Iterable<Profile>>() {
                                        @Override
                                        public Iterable<Profile> call(List<Profile> profiles) {
                                            return profiles;
                                        }
                                    }).filter(new Func1<Profile, Boolean>() {
                                        @Override
                                        public Boolean call(Profile profile) {
                                            return profile.id == notification.matchId;
                                        }
                                    }).toBlocking().first().image;
                        } catch (Exception ignored) {
                            try {
                                notification.image = TheDistanceApplication.getApplicationComponent(context).api().getProfile(notification.matchId)
                                        .toBlocking().first().image;
                            } catch (Exception ignored2) {
                            }
                        }
                    }
                }

                break;
        }

        return notification;
    }

    @WorkerThread
    public static Notification with(RemoteMessage remoteMessage, Context context) {
        Map<String, String> data = remoteMessage.getData();
        String ctx = data.get("context");

        int type = -1;
        JsonObject cData = null;
        try {
            cData = new JsonParser().parse(ctx).getAsJsonObject();
            type = cData.has(com.showreal.app.data.model.Notification.EXTRA_TYPE) ? cData.get(com.showreal.app.data.model.Notification.EXTRA_TYPE).getAsInt() : -1;
        } catch (Exception ignored) {

        }

        String title = data.get("message");

        final Notification notification = new Notification();
        notification.title = title;
        notification.type = type;
        notification.startDate = Calendar.getInstance().getTime();

        switch (type) {
            case SecondChanceRealUpdated:
            case Matched:
            case MatchRealUpdated:
                notification.matchId = (cData != null && cData.has("pk")) ? cData.get("pk").getAsInt() : -1;
                if (notification.matchId != -1) {
                    List<Profile> profiles = TheDistanceApplication.getApplicationComponent(context)
                            .cache().<List<Profile>>provider().expirable(false).withKey("profiles")
                            .readNullable()
                            .toBlocking()
                            .first();
                    if (profiles != null) {
                        try {
                            notification.image = Observable.just(profiles)
                                    .flatMapIterable(new Func1<List<Profile>, Iterable<Profile>>() {
                                        @Override
                                        public Iterable<Profile> call(List<Profile> profiles) {
                                            return profiles;
                                        }
                                    }).filter(new Func1<Profile, Boolean>() {
                                        @Override
                                        public Boolean call(Profile profile) {
                                            return profile.id == notification.matchId;
                                        }
                                    }).toBlocking().first().image;
                        } catch (Exception ignored) {
                            try {
                                notification.image = TheDistanceApplication.getApplicationComponent(context).api().getProfile(notification.matchId)
                                        .toBlocking().first().image;
                            } catch (Exception ignored2) {
                            }
                        }
                    }
                }

                break;
        }

        return notification;
    }

    @Override
    public boolean isSameItem(Sortable other) {
        return other.equals(this);
    }

    @Override
    public boolean isSameContent(Sortable other) {
        return other.equals(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Notification that = (Notification) o;

        if (type != that.type) {
            return false;
        }
        if (title != null ? !title.equals(that.title) : that.title != null) {
            return false;
        }
        if (summary != null ? !summary.equals(that.summary) : that.summary != null) {
            return false;
        }
        if (startDate != null ? !startDate.equals(that.startDate) : that.startDate != null) {
            return false;
        }
        if (endDate != null ? !endDate.equals(that.endDate) : that.endDate != null) {
            return false;
        }
        return url != null ? url.equals(that.url) : that.url == null;

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (summary != null ? summary.hashCode() : 0);
        result = 31 * result + type;
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
