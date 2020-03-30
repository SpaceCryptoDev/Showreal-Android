package com.showreal.app.features.conversations;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.showreal.app.data.model.Message;

import java.text.SimpleDateFormat;

public class MessageViewModel extends BaseObservable {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    private final Message message;

    public MessageViewModel(Message message) {
        this.message = message;
    }

    @Bindable
    public String getText() {
        return message.text;
    }

    @Bindable
    public String getTime() {
        return TIME_FORMAT.format(message.timeSent);
    }
}
