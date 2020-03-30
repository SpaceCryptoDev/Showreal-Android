package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class AgeRange implements Parcelable {

    public int upper;
    public int lower;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.upper);
        dest.writeInt(this.lower);
    }

    public AgeRange() {
    }

    protected AgeRange(Parcel in) {
        this.upper = in.readInt();
        this.lower = in.readInt();
    }

    public static final Parcelable.Creator<AgeRange> CREATOR = new Parcelable.Creator<AgeRange>() {
        @Override
        public AgeRange createFromParcel(Parcel source) {
            return new AgeRange(source);
        }

        @Override
        public AgeRange[] newArray(int size) {
            return new AgeRange[size];
        }
    };


}

