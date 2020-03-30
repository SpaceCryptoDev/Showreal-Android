package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Settings implements Parcelable {

    public static final int HEIGHT_FEET = 1;
    public static final int HEIGHT_CM = 0;
    public static final int RADIUS_MILES = 1;
    public static final int RADIUS_KM = 0;

    public int id;
    public boolean isPrivate;
    public boolean blockFriends;
    public int height_unit;
    public int radius_unit;
    public boolean nudgeNotification;
    public boolean matchNotification;
    public boolean userUpdateMatchNotification;
    public boolean newMessageMatchNotification;

    public Settings() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeByte(this.isPrivate ? (byte) 1 : (byte) 0);
        dest.writeByte(this.blockFriends ? (byte) 1 : (byte) 0);
        dest.writeInt(this.height_unit);
        dest.writeInt(this.radius_unit);
        dest.writeByte(this.nudgeNotification ? (byte) 1 : (byte) 0);
        dest.writeByte(this.matchNotification ? (byte) 1 : (byte) 0);
        dest.writeByte(this.userUpdateMatchNotification ? (byte) 1 : (byte) 0);
        dest.writeByte(this.newMessageMatchNotification ? (byte) 1 : (byte) 0);
    }

    protected Settings(Parcel in) {
        this.id = in.readInt();
        this.isPrivate = in.readByte() != 0;
        this.blockFriends = in.readByte() != 0;
        this.height_unit = in.readInt();
        this.radius_unit = in.readInt();
        this.nudgeNotification = in.readByte() != 0;
        this.matchNotification = in.readByte() != 0;
        this.userUpdateMatchNotification = in.readByte() != 0;
        this.newMessageMatchNotification = in.readByte() != 0;
    }

    public static final Creator<Settings> CREATOR = new Creator<Settings>() {
        @Override
        public Settings createFromParcel(Parcel source) {
            return new Settings(source);
        }

        @Override
        public Settings[] newArray(int size) {
            return new Settings[size];
        }
    };
}
