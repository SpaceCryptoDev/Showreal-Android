package com.showreal.app.analytics;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.showreal.app.R;

import uk.co.thedistance.components.analytics.interfaces.AnalyticsTracker;
import uk.co.thedistance.components.analytics.model.AnalyticEvent;
import uk.co.thedistance.components.analytics.model.AnalyticSession;
import uk.co.thedistance.components.analytics.model.TimingEvent;

public class GoogleAnalyticsTracker extends AnalyticsTracker {

    private final Tracker tracker;

    public GoogleAnalyticsTracker(Context context) {
        tracker = GoogleAnalytics.getInstance(context)
                .newTracker(R.xml.global_tracker);
        setSession(new AnalyticSession(null));
    }

    @Override
    public void sendScreen(String screenName) {
        tracker.setScreenName(screenName);
        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder();
        if (session.getCustomDimensions() != null) {
            for (int key : session.getCustomDimensions().keySet()) {
                builder.setCustomDimension(key, session.getCustomDimensions().get(key));
            }
        }

        tracker.send(builder.build());
    }

    @Override
    public void sendEvent(AnalyticEvent event) {
        HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder()
                .setCategory(event.category)
                .setAction(event.action)
                .setLabel(event.label);

        if (session.getCustomDimensions() != null) {
            for (int key : session.getCustomDimensions().keySet()) {
                builder.setCustomDimension(key, session.getCustomDimensions().get(key));
            }
        }

        tracker.send(builder.build());
    }

    @Override
    public void sendTiming(TimingEvent event) {
        HitBuilders.TimingBuilder builder = new HitBuilders.TimingBuilder()
                .setCategory(event.category)
                .setLabel(event.label)
                .setVariable(event.name)
                .setValue(event.value);

        if (session.getCustomDimensions() != null) {
            for (int key : session.getCustomDimensions().keySet()) {
                builder.setCustomDimension(key, session.getCustomDimensions().get(key));
            }
        }

        tracker.send(builder.build());
    }
}
