package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import nl.qbusict.cupboard.annotation.Column;

public class Question implements Parcelable, Comparable<Question> {

    public Long _id;
    public int id;
    @SerializedName("question")
    public
    String text;
    @SerializedName("question_US")
    public
    String textUS;
    @SerializedName("question_AUS")
    public
    String textAUS;
    public int questionType;
    @Column("col_index")
    public int index;
    public boolean isActive;
    public String colour;
    public Date expiryDate;
    public String smallImage;
    public String largeImage;
    public String reelImage;
    public boolean dummyQuestion;

    public Question() {
    }

    @Override
    public int compareTo(Question o) {
        return Integer.valueOf(index).compareTo(o.index);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this._id);
        dest.writeInt(this.id);
        dest.writeString(this.text);
        dest.writeString(this.textUS);
        dest.writeString(this.textAUS);
        dest.writeInt(this.questionType);
        dest.writeInt(this.index);
        dest.writeByte(this.isActive ? (byte) 1 : (byte) 0);
        dest.writeString(this.colour);
        dest.writeLong(this.expiryDate != null ? this.expiryDate.getTime() : -1);
        dest.writeString(this.smallImage);
        dest.writeString(this.largeImage);
        dest.writeString(this.reelImage);
        dest.writeByte(this.dummyQuestion ? (byte) 1 : (byte) 0);
    }

    protected Question(Parcel in) {
        this._id = (Long) in.readValue(Long.class.getClassLoader());
        this.id = in.readInt();
        this.text = in.readString();
        this.textUS = in.readString();
        this.textAUS = in.readString();
        this.questionType = in.readInt();
        this.index = in.readInt();
        this.isActive = in.readByte() != 0;
        this.colour = in.readString();
        long tmpExpiryDate = in.readLong();
        this.expiryDate = tmpExpiryDate == -1 ? null : new Date(tmpExpiryDate);
        this.smallImage = in.readString();
        this.largeImage = in.readString();
        this.reelImage = in.readString();
        this.dummyQuestion = in.readByte() != 0;
    }

    public static final Creator<Question> CREATOR = new Creator<Question>() {
        @Override
        public Question createFromParcel(Parcel source) {
            return new Question(source);
        }

        @Override
        public Question[] newArray(int size) {
            return new Question[size];
        }
    };
}
