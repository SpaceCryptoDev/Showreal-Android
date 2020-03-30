package com.showreal.app.features.real.myreal;

import android.os.Parcel;
import android.os.Parcelable;

import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;

import java.io.File;

public class ShowRealVideo implements Parcelable {

    public ShowRealVideo(Video video, Question question, String path) {
        this.video = video;
        this.question = question;
        this.path = path;
    }

    public final Video video;
    public boolean added;
    public final Question question;
    public final String path;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ShowRealVideo)) {
            return false;
        }
        return video.equals(((ShowRealVideo) obj).video);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.video, flags);
        dest.writeByte(this.added ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.question, flags);
        dest.writeString(this.path);
    }

    protected ShowRealVideo(Parcel in) {
        this.video = in.readParcelable(Video.class.getClassLoader());
        this.added = in.readByte() != 0;
        this.question = in.readParcelable(Question.class.getClassLoader());
        this.path = in.readString();
    }

    public static final Creator<ShowRealVideo> CREATOR = new Creator<ShowRealVideo>() {
        @Override
        public ShowRealVideo createFromParcel(Parcel source) {
            return new ShowRealVideo(source);
        }

        @Override
        public ShowRealVideo[] newArray(int size) {
            return new ShowRealVideo[size];
        }
    };
}
