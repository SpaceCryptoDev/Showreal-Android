package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class InstagramUser implements Parcelable {

    public Data data;
    public String token;

    public static class Data implements Parcelable {

        public String id;


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.id);
        }

        public Data() {
        }

        protected Data(Parcel in) {
            this.id = in.readString();
        }

        public static final Parcelable.Creator<Data> CREATOR = new Parcelable.Creator<Data>() {
            @Override
            public Data createFromParcel(Parcel source) {
                return new Data(source);
            }

            @Override
            public Data[] newArray(int size) {
                return new Data[size];
            }
        };
    }


    public InstagramUser() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.data, flags);
        dest.writeString(this.token);
    }

    protected InstagramUser(Parcel in) {
        this.data = in.readParcelable(Data.class.getClassLoader());
        this.token = in.readString();
    }

    public static final Creator<InstagramUser> CREATOR = new Creator<InstagramUser>() {
        @Override
        public InstagramUser createFromParcel(Parcel source) {
            return new InstagramUser(source);
        }

        @Override
        public InstagramUser[] newArray(int size) {
            return new InstagramUser[size];
        }
    };
}
