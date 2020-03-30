package com.showreal.app.features.notifications;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import com.showreal.app.R;
import com.showreal.app.data.model.Notification;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class NotificationViewModel extends BaseObservable {

    private final Notification notification;
    private final NotificationView notificationView;
    final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM", Locale.UK);

    public Notification getNotification() {
        return notification;
    }

    interface NotificationView {
        void openNotification(Notification notification);

        void addToCalendar(Notification notification);

        void deleteNotifications(List<Notification> notifications);

        Context getResourceContext();
    }

    public NotificationViewModel(Notification notification, NotificationView notificationView) {
        this.notification = notification;
        this.notificationView = notificationView;
    }

    public void onClick(View view) {
        notificationView.openNotification(notification);
    }

    public void onCalendar(View view) {
        notificationView.addToCalendar(notification);
    }

    @Bindable
    public SpannableString getTitle() {
        if (notification.type == Notification.Event) {
            return new SpannableString(notification.getTitle());
        }

        String action = "";

        switch (notification.type) {
            case Notification.Matched:
                action = "Send message";
                break;
            case Notification.Message:
                action = "Read message";
                break;
            case Notification.MatchRealUpdated:
            case Notification.SecondChanceRealUpdated:
                action = "See reel";
                break;
            case Notification.NewQuestion:
                action = "See question";
                break;
        }

        SpannableString spannableString = new SpannableString(notification.getTitle() + " " + action);
        applyActionSpans(spannableString, action);
        return spannableString;
    }

    private void applyActionSpans(SpannableString spannableString, String action) {
        if (TextUtils.isEmpty(action)) {
            return;
        }
        int start = spannableString.toString().indexOf(action);
        spannableString.setSpan(new ForegroundColorSpan(notificationView.getResourceContext().getResources().getColor(R.color.colorAccent)), start, start + action.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new UnderlineSpan(), start, start + action.length() - 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    @Bindable
    public String getSummary() {
        if (notification.type == Notification.Event) {
            return notification.summary;
        }
        long time = notification.startDate.getTime();
        return time < DateUtils.MINUTE_IN_MILLIS ? "Just now" : DateUtils.getRelativeTimeSpanString(time, new Date().getTime(), DateUtils.MINUTE_IN_MILLIS).toString();
    }

    @Bindable
    public SpannableString getDate() {
        if (notification.type != Notification.Event) {
            return null;
        }
        return applySpans(notification.startDate);
    }

    @Bindable
    public int getIconResource() {
        switch (notification.type) {
            case Notification.Matched:
            case Notification.MatchRealUpdated:
            case Notification.SecondChanceRealUpdated:
                return R.drawable.new_match;
            case Notification.Message:
                return R.drawable.new_message;
            case Notification.NewQuestion:
                return R.drawable.new_question;
        }
        return 0;
    }

    @Bindable
    public int getIconVisibility() {
        return notification.type == Notification.Event ? View.GONE : View.VISIBLE;
    }

    @Bindable
    public int getHandleVisibility() {
        return notification.type == Notification.Event ? View.GONE : View.VISIBLE;
    }

    @Bindable
    public String getImage() {
        return notification.type == Notification.Event || TextUtils.isEmpty(notification.image) ? null : notification.image;
    }

    private SpannableString applySpans(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String suffix = getDayOfMonthSuffix(day);

        String dateString = DATE_FORMAT.format(date);
        String dayString = String.valueOf(day);
        dateString = dateString.replace(dayString, dayString + suffix);

        SpannableString spannableString = new SpannableString(dateString);
        int index = dateString.indexOf(suffix);

        if (index == -1) {
            return spannableString;
        }
        spannableString.setSpan(new SuperscriptSpan(), index, index + 2, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), index, index + 2, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        return spannableString;
    }

    @Bindable
    public int getCalendarVisibility() {
        return notification.type == Notification.Event ? View.VISIBLE : View.GONE;
    }

    private static String getDayOfMonthSuffix(final int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }
}
