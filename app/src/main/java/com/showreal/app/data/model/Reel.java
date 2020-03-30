package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Reel implements Parcelable {

    public Long _id;
    public int id;
    String name;
    public int viewCount;
    float offsetX;
    float offsetY;
    float duration;


    public Reel() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this._id);
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeInt(this.viewCount);
        dest.writeFloat(this.offsetX);
        dest.writeFloat(this.offsetY);
        dest.writeFloat(this.duration);
    }

    protected Reel(Parcel in) {
        this._id = (Long) in.readValue(Long.class.getClassLoader());
        this.id = in.readInt();
        this.name = in.readString();
        this.viewCount = in.readInt();
        this.offsetX = in.readFloat();
        this.offsetY = in.readFloat();
        this.duration = in.readFloat();
    }

    public static final Creator<Reel> CREATOR = new Creator<Reel>() {
        @Override
        public Reel createFromParcel(Parcel source) {
            return new Reel(source);
        }

        @Override
        public Reel[] newArray(int size) {
            return new Reel[size];
        }
    };
}
