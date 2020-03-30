package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.Date;

import uk.co.thedistance.components.lists.interfaces.Sortable;

public class Match implements Sortable, Parcelable {

    @SerializedName("User")
    public Profile profile;

    public int unreadCount;
    public boolean hasSeenMatch;
    public String conversationUrl;
    public int id;
    public String chatId;
    public Date createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Match match = (Match) o;

        if (unreadCount != match.unreadCount) {
            return false;
        }
        if (hasSeenMatch != match.hasSeenMatch) {
            return false;
        }
        if (id != match.id) {
            return false;
        }
        if (profile != null ? !profile.equals(match.profile) : match.profile != null) {
            return false;
        }
        if (conversationUrl != null ? !conversationUrl.equals(match.conversationUrl) : match.conversationUrl != null) {
            return false;
        }
        if (chatId != null ? !chatId.equals(match.chatId) : match.chatId != null) {
            return false;
        }
        return createdAt != null ? createdAt.equals(match.createdAt) : match.createdAt == null;

    }

    @Override
    public int hashCode() {
        int result = profile != null ? profile.hashCode() : 0;
        result = 31 * result + unreadCount;
        result = 31 * result + (hasSeenMatch ? 1 : 0);
        result = 31 * result + (conversationUrl != null ? conversationUrl.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + (chatId != null ? chatId.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        return result;
    }

    @Override
    public boolean isSameItem(Sortable other) {
        return ((Match) other).id == (((Match) other).id);
    }

    @Override
    public boolean isSameContent(Sortable other) {
        return other.equals(other);
    }

    private static final Profile.ProfileDeserializer PROFILE_DESERIALIZER = new Profile.ProfileDeserializer();

    public static class MatchDeserializer implements JsonDeserializer<Match> {

        @Override
        public Match deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            Match match = new Match();
            JsonObject object = json.getAsJsonObject();
            JsonElement profile = object.get("user");
            match.profile = PROFILE_DESERIALIZER.deserialize(profile, Profile.class, context);
            match.unreadCount = context.deserialize(object.get("unread_count"), Integer.TYPE);
            match.hasSeenMatch = context.deserialize(object.get("has_seen_match"), Boolean.TYPE);
            match.createdAt = context.deserialize(object.get("created_at"), Date.class);
            match.chatId = context.deserialize(object.get("chat_id"), String.class);
            match.conversationUrl = context.deserialize(object.get("conversation_url"), String.class);
            match.id = context.deserialize(object.get("id"), Integer.TYPE);

            return match;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.profile, flags);
        dest.writeInt(this.unreadCount);
        dest.writeByte(this.hasSeenMatch ? (byte) 1 : (byte) 0);
        dest.writeString(this.conversationUrl);
        dest.writeInt(this.id);
        dest.writeString(this.chatId);
        dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
    }

    public Match() {
    }

    protected Match(Parcel in) {
        this.profile = in.readParcelable(Profile.class.getClassLoader());
        this.unreadCount = in.readInt();
        this.hasSeenMatch = in.readByte() != 0;
        this.conversationUrl = in.readString();
        this.id = in.readInt();
        this.chatId = in.readString();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    }

    public static final Parcelable.Creator<Match> CREATOR = new Parcelable.Creator<Match>() {
        @Override
        public Match createFromParcel(Parcel source) {
            return new Match(source);
        }

        @Override
        public Match[] newArray(int size) {
            return new Match[size];
        }
    };
}
