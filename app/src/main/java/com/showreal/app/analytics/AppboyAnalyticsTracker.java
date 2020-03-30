package com.showreal.app.analytics;

import android.content.Context;

import com.appboy.Appboy;

import uk.co.thedistance.components.analytics.interfaces.AnalyticsTracker;
import uk.co.thedistance.components.analytics.model.AnalyticEvent;
import uk.co.thedistance.components.analytics.model.TimingEvent;

public class AppboyAnalyticsTracker extends AnalyticsTracker {
    private final Context context;

    public AppboyAnalyticsTracker(Context context) {
        this.context = context;
    }

    @Override
    public void sendScreen(String screenName) {
        Appboy.getInstance(context).logCustomEvent(screenName);
    }

    @Override
    public void sendEvent(AnalyticEvent event) {
        if (event instanceof AppboyEvent && ((AppboyEvent) event).properties != null) {
            Appboy.getInstance(context).logCustomEvent(event.action, ((AppboyEvent) event).properties);
            return;
        }
        Appboy.getInstance(context).logCustomEvent(event.action);
    }

    @Override
    public void sendTiming(TimingEvent event) {

    }
}
