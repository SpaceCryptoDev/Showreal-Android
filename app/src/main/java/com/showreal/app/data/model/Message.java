package com.showreal.app.data.model;

import android.webkit.MimeTypeMap;

import com.google.gson.JsonObject;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.UserMessage;

import java.io.File;
import java.util.Date;

import nl.qbusict.cupboard.annotation.Ignore;
import uk.co.thedistance.components.lists.interfaces.Sortable;

public class Message implements Sortable {

    public Long _id;
    public String text;
    public Date timeSent;
    public String sender;
    public String channel;
    public String mediaUrl;
    public String mediaType;
    public long mediaSize;
    public long messageId;

    public Message() {
    }

    public Message(String text) {
        this.text = text;
    }

    public Message(File file) {
        mediaUrl = file.getAbsolutePath();
        mediaSize = file.length();
        mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().indexOf(".") +1));
    }

    public static Message with(BaseMessage baseMessage) {
        Message message = new Message();
        message.messageId = baseMessage.getMessageId();
        message.channel = baseMessage.getChannelUrl();
        message.timeSent = new Date(baseMessage.getCreatedAt());

        if (baseMessage instanceof UserMessage) {
            message.sender = ((UserMessage) baseMessage).getSender().getUserId();
            message.text = ((UserMessage) baseMessage).getMessage();
        } else if (baseMessage instanceof FileMessage) {
            message.sender = ((FileMessage) baseMessage).getSender().getUserId();
            message.mediaUrl = ((FileMessage) baseMessage).getUrl();
            message.mediaType = ((FileMessage) baseMessage).getType();
            message.mediaSize = ((FileMessage) baseMessage).getSize();
        }

        return message;
    }

    @Override
    public boolean isSameItem(Sortable other) {
        return other instanceof Message && messageId == ((Message) other).messageId;
    }

    @Override
    public boolean isSameContent(Sortable other) {
        return other instanceof Message && this.equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Message message = (Message) o;

        if (mediaSize != message.mediaSize) {
            return false;
        }
        if (messageId != message.messageId) {
            return false;
        }
        if (_id != null ? !_id.equals(message._id) : message._id != null) {
            return false;
        }
        if (text != null ? !text.equals(message.text) : message.text != null) {
            return false;
        }
        if (timeSent != null ? !timeSent.equals(message.timeSent) : message.timeSent != null) {
            return false;
        }
        if (sender != null ? !sender.equals(message.sender) : message.sender != null) {
            return false;
        }
        if (channel != null ? !channel.equals(message.channel) : message.channel != null) {
            return false;
        }
        if (mediaUrl != null ? !mediaUrl.equals(message.mediaUrl) : message.mediaUrl != null) {
            return false;
        }
        return mediaType != null ? mediaType.equals(message.mediaType) : message.mediaType == null;

    }

    @Override
    public int hashCode() {
        int result = _id != null ? _id.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (timeSent != null ? timeSent.hashCode() : 0);
        result = 31 * result + (sender != null ? sender.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (mediaUrl != null ? mediaUrl.hashCode() : 0);
        result = 31 * result + (mediaType != null ? mediaType.hashCode() : 0);
        result = 31 * result + (int) (mediaSize ^ (mediaSize >>> 32));
        result = 31 * result + (int) (messageId ^ (messageId >>> 32));
        return result;
    }
}
