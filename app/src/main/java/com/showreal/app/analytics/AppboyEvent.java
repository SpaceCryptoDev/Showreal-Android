package com.showreal.app.analytics;

import com.appboy.models.outgoing.AppboyProperties;

import uk.co.thedistance.components.analytics.model.AnalyticEvent;

public class AppboyEvent extends AnalyticEvent {

    public AppboyProperties properties;

    public AppboyEvent(String action) {
        super(null, action, null);
    }

    public AppboyEvent(String action, AppboyProperties properties) {
        super(null, action, null);
        this.properties = properties;
    }
}
