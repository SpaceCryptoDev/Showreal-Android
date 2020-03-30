package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

public class Video implements Parcelable, Comparable<Video> {

    public Long _id;
    @SerializedName("video")
    public String url;
    public Question question;
    public Reel reel;
    public double duration;
    public double offsetX;
    public double offsetY;
    public double scale;
    public int id;

    public boolean published = true;
    public boolean modified = false;
    public boolean deleted = false;
    public boolean videoModified = false;
    public long reelId;
    public boolean dummyVideo;
    @SerializedName("is_light_filter_applied")
    public boolean isLightFilterEnabled;

    public Video(String path, long duration, Question question) {
        this.url = path;
        this.question = question;
        this.duration = duration / 1000;
        this.id = question.id;
    }

    @Override
    public int compareTo(Video video) {
        return Integer.valueOf(question.index).compareTo(video.question.index);
    }

    public static class VideoDeserializer implements JsonDeserializer<Video> {

        static Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        @Override
        public Video deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Video video = gson.fromJson(json, typeOfT);

            JsonObject object = json.getAsJsonObject();
            JsonObject response = object.getAsJsonObject("response");
            JsonObject question = response.getAsJsonObject("question");

            video.question = context.deserialize(question, Question.class);
            video.question.index = response.get("index").getAsInt();

            return video;
        }
    }


    public Video() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this._id);
        dest.writeString(this.url);
        dest.writeParcelable(this.question, flags);
        dest.writeParcelable(this.reel, flags);
        dest.writeDouble(this.duration);
        dest.writeDouble(this.offsetX);
        dest.writeDouble(this.offsetY);
        dest.writeDouble(this.scale);
        dest.writeInt(this.id);
        dest.writeByte(this.published ? (byte) 1 : (byte) 0);
        dest.writeByte(this.modified ? (byte) 1 : (byte) 0);
        dest.writeByte(this.deleted ? (byte) 1 : (byte) 0);
        dest.writeByte(this.videoModified ? (byte) 1 : (byte) 0);
        dest.writeLong(this.reelId);
        dest.writeByte(this.dummyVideo ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isLightFilterEnabled ? (byte) 1 : (byte) 0);
    }

    protected Video(Parcel in) {
        this._id = (Long) in.readValue(Long.class.getClassLoader());
        this.url = in.readString();
        this.question = in.readParcelable(Question.class.getClassLoader());
        this.reel = in.readParcelable(Reel.class.getClassLoader());
        this.duration = in.readDouble();
        this.offsetX = in.readDouble();
        this.offsetY = in.readDouble();
        this.scale = in.readDouble();
        this.id = in.readInt();
        this.published = in.readByte() != 0;
        this.modified = in.readByte() != 0;
        this.deleted = in.readByte() != 0;
        this.videoModified = in.readByte() != 0;
        this.reelId = in.readLong();
        this.dummyVideo = in.readByte() != 0;
        this.isLightFilterEnabled = in.readByte() != 0;
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel source) {
            return new Video(source);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };
}
