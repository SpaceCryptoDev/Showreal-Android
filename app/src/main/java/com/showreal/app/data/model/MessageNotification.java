package com.showreal.app.data.model;

import java.util.Date;

public class MessageNotification {

    public Long _id;
    public String message;
    public String channel;
    public Date timeReceived;

    public MessageNotification() {
    }

    public MessageNotification(String message, String channel, Date timeReceived) {
        this.message = message;
        this.channel = channel;
        this.timeReceived = timeReceived;
    }
}
