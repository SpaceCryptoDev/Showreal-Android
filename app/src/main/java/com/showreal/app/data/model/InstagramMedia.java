package com.showreal.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import uk.co.thedistance.components.lists.interfaces.Sortable;

public class InstagramMedia implements Parcelable {

    public List<Image> images;
    public Pagination pagination;

    public static class Pagination implements Parcelable {
        public String nextUrl;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.nextUrl);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Pagination that = (Pagination) o;

            return nextUrl != null ? nextUrl.equals(that.nextUrl) : that.nextUrl == null;

        }

        @Override
        public int hashCode() {
            return nextUrl != null ? nextUrl.hashCode() : 0;
        }

        public Pagination() {

        }

        protected Pagination(Parcel in) {
            this.nextUrl = in.readString();
        }

        public static final Creator<Pagination> CREATOR = new Creator<Pagination>() {
            @Override
            public Pagination createFromParcel(Parcel source) {
                return new Pagination(source);
            }

            @Override
            public Pagination[] newArray(int size) {
                return new Pagination[size];
            }
        };
    }

    public static class Image implements Parcelable, Sortable {

        public Resolution thumbnail;
        public Resolution lowResolution;
        public Resolution standardResolution;
        public String url;
        public long createdTime;
        public String id;

        public Image() {
        }

        public Image(Photo photo) {
            this.thumbnail = new Resolution(photo.url);
            this.standardResolution = thumbnail;
            this.lowResolution = thumbnail;
            this.id = photo.url;
        }

        @Override
        public boolean isSameItem(Sortable other) {
            return other instanceof Image && ((Image) other).id.equals(id);
        }

        @Override
        public boolean isSameContent(Sortable other) {
            return equals(other);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Image image = (Image) o;

            if (createdTime != image.createdTime) {
                return false;
            }
            if (thumbnail != null ? !thumbnail.equals(image.thumbnail) : image.thumbnail != null) {
                return false;
            }
            if (lowResolution != null ? !lowResolution.equals(image.lowResolution) : image.lowResolution != null) {
                return false;
            }
            if (standardResolution != null ? !standardResolution.equals(image.standardResolution) : image.standardResolution != null) {
                return false;
            }
            if (url != null ? !url.equals(image.url) : image.url != null) {
                return false;
            }
            return id != null ? id.equals(image.id) : image.id == null;

        }

        @Override
        public int hashCode() {
            int result = thumbnail != null ? thumbnail.hashCode() : 0;
            result = 31 * result + (lowResolution != null ? lowResolution.hashCode() : 0);
            result = 31 * result + (standardResolution != null ? standardResolution.hashCode() : 0);
            result = 31 * result + (url != null ? url.hashCode() : 0);
            result = 31 * result + (int) (createdTime ^ (createdTime >>> 32));
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(this.thumbnail, flags);
            dest.writeParcelable(this.lowResolution, flags);
            dest.writeParcelable(this.standardResolution, flags);
            dest.writeString(this.url);
            dest.writeLong(this.createdTime);
            dest.writeString(this.id);
        }

        protected Image(Parcel in) {
            this.thumbnail = in.readParcelable(Resolution.class.getClassLoader());
            this.lowResolution = in.readParcelable(Resolution.class.getClassLoader());
            this.standardResolution = in.readParcelable(Resolution.class.getClassLoader());
            this.url = in.readString();
            this.createdTime = in.readLong();
            this.id = in.readString();
        }

        public static final Creator<Image> CREATOR = new Creator<Image>() {
            @Override
            public Image createFromParcel(Parcel source) {
                return new Image(source);
            }

            @Override
            public Image[] newArray(int size) {
                return new Image[size];
            }
        };
    }

    public static class Resolution implements Parcelable {

        public String url;
        public int width;
        public int height;

        public Resolution(String url) {
            this.url = url;
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
            dest.writeInt(this.width);
            dest.writeInt(this.height);
        }

        public Resolution() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Resolution that = (Resolution) o;

            if (width != that.width) {
                return false;
            }
            if (height != that.height) {
                return false;
            }
            return url != null ? url.equals(that.url) : that.url == null;

        }

        @Override
        public int hashCode() {
            int result = url != null ? url.hashCode() : 0;
            result = 31 * result + width;
            result = 31 * result + height;
            return result;
        }

        protected Resolution(Parcel in) {
            this.url = in.readString();
            this.width = in.readInt();
            this.height = in.readInt();
        }

        public static final Parcelable.Creator<Resolution> CREATOR = new Parcelable.Creator<Resolution>() {
            @Override
            public Resolution createFromParcel(Parcel source) {
                return new Resolution(source);
            }

            @Override
            public Resolution[] newArray(int size) {
                return new Resolution[size];
            }
        };
    }

    public static class MediaDeserializer implements JsonDeserializer<InstagramMedia> {

        @Override
        public InstagramMedia deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            InstagramMedia media = new InstagramMedia();
            media.images = new ArrayList<>();
            media.pagination = context.deserialize(json.getAsJsonObject().get("pagination"), Pagination.class);

            JsonArray data = json.getAsJsonObject().getAsJsonArray("data");
            for (JsonElement element : data) {
                JsonObject object = element.getAsJsonObject();
                Image image = context.deserialize(object.get("images"), Image.class);
                image.url = object.getAsJsonPrimitive("link").getAsString();
                image.id = object.get("id").getAsString();
                image.createdTime = object.get("created_time").getAsLong();
                media.images.add(image);
            }

            return media;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.images);
    }

    public InstagramMedia() {
    }

    protected InstagramMedia(Parcel in) {
        this.images = in.createTypedArrayList(Image.CREATOR);
    }

    public static final Parcelable.Creator<InstagramMedia> CREATOR = new Parcelable.Creator<InstagramMedia>() {
        @Override
        public InstagramMedia createFromParcel(Parcel source) {
            return new InstagramMedia(source);
        }

        @Override
        public InstagramMedia[] newArray(int size) {
            return new InstagramMedia[size];
        }
    };
}
