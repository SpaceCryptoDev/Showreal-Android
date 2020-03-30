package com.showreal.app.data.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.appboy.Appboy;
import com.appboy.enums.Gender;
import com.appboy.enums.Month;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.injection.ApplicationComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivecache.Provider;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.lists.interfaces.Sortable;
import uk.co.thedistance.thedistancecore.TDObservers;

public class Profile implements Parcelable, Sortable {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM, yyyy");
    public static final SimpleDateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String PREF_FIELD_REGION = "region";
    public static final int DUMMY_ID = -182;

    public int id;
    public String firstName;
    public String lastName;
    public String facebookId;
    public String facebookToken;
    public String email;
    @SerializedName("dob")
    public String dateOfBirth;
    public int gender;
    public int interestedIn;
    public float height;
    @SerializedName("profile_latitude")
    public double latitude;
    @SerializedName("profile_longitude")
    public double longitude;
    @SerializedName("search_latitude")
    public double searchLatitude;
    @SerializedName("search_longitude")
    public double searchLongitude;
    public String city;
    public String chatId;
    public boolean onlineStatus;
    public Date lastOnline;
    public String instagramId;
    public String instagramAccessToken;
    public List<Video> videos;
    public List<Photo> photos;
    @SerializedName("profile_image")
    public String image;
    public int notNowCount;
    public AgeRange preferredAge;
    public double searchRadius;

    public String newImage;

    public int getReelId() {
        return videos.size() > 0 ? videos.get(0).reel.id : -1;
    }

    public static Observable<Profile> dummyProfile(Context context) {
        final ApplicationComponent component = TheDistanceApplication.getApplicationComponent(context);
        return Observable.create(new Observable.OnSubscribe<Profile>() {
            @Override
            public void call(Subscriber<? super Profile> subscriber) {

                Profile profile = component.staleProfileProvider().readNullable().toBlocking().first();
                if (profile == null || profile.id != DUMMY_ID) {
                    profile = new Profile();
                    profile.id = DUMMY_ID;
                    profile.videos = new ArrayList<Video>();
                }

                subscriber.onNext(profile);
                subscriber.onCompleted();
            }
        }).compose(component.staleProfileProvider().replace()).compose(component.profileProvider().replace());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Profile profile = (Profile) o;

        if (id != profile.id) {
            return false;
        }
        if (gender != profile.gender) {
            return false;
        }
        if (interestedIn != profile.interestedIn) {
            return false;
        }
        if (Float.compare(profile.height, height) != 0) {
            return false;
        }
        if (Double.compare(profile.latitude, latitude) != 0) {
            return false;
        }
        if (Double.compare(profile.longitude, longitude) != 0) {
            return false;
        }
        if (Double.compare(profile.searchLatitude, searchLatitude) != 0) {
            return false;
        }
        if (Double.compare(profile.searchLongitude, searchLongitude) != 0) {
            return false;
        }
        if (onlineStatus != profile.onlineStatus) {
            return false;
        }
        if (notNowCount != profile.notNowCount) {
            return false;
        }
        if (Double.compare(profile.searchRadius, searchRadius) != 0) {
            return false;
        }
        if (firstName != null ? !firstName.equals(profile.firstName) : profile.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(profile.lastName) : profile.lastName != null) {
            return false;
        }
        if (facebookId != null ? !facebookId.equals(profile.facebookId) : profile.facebookId != null) {
            return false;
        }
        if (facebookToken != null ? !facebookToken.equals(profile.facebookToken) : profile.facebookToken != null) {
            return false;
        }
        if (email != null ? !email.equals(profile.email) : profile.email != null) {
            return false;
        }
        if (dateOfBirth != null ? !dateOfBirth.equals(profile.dateOfBirth) : profile.dateOfBirth != null) {
            return false;
        }
        if (city != null ? !city.equals(profile.city) : profile.city != null) {
            return false;
        }
        if (chatId != null ? !chatId.equals(profile.chatId) : profile.chatId != null) {
            return false;
        }
        if (lastOnline != null ? !lastOnline.equals(profile.lastOnline) : profile.lastOnline != null) {
            return false;
        }
        if (instagramId != null ? !instagramId.equals(profile.instagramId) : profile.instagramId != null) {
            return false;
        }
        if (instagramAccessToken != null ? !instagramAccessToken.equals(profile.instagramAccessToken) : profile.instagramAccessToken != null) {
            return false;
        }
        if (videos != null ? !videos.equals(profile.videos) : profile.videos != null) {
            return false;
        }
        if (photos != null ? !photos.equals(profile.photos) : profile.photos != null) {
            return false;
        }
        if (image != null ? !image.equals(profile.image) : profile.image != null) {
            return false;
        }
        if (preferredAge != null ? !preferredAge.equals(profile.preferredAge) : profile.preferredAge != null) {
            return false;
        }
        return newImage != null ? newImage.equals(profile.newImage) : profile.newImage == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id;
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (facebookId != null ? facebookId.hashCode() : 0);
        result = 31 * result + (facebookToken != null ? facebookToken.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        result = 31 * result + gender;
        result = 31 * result + interestedIn;
        result = 31 * result + (height != +0.0f ? Float.floatToIntBits(height) : 0);
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(searchLatitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(searchLongitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (chatId != null ? chatId.hashCode() : 0);
        result = 31 * result + (onlineStatus ? 1 : 0);
        result = 31 * result + (lastOnline != null ? lastOnline.hashCode() : 0);
        result = 31 * result + (instagramId != null ? instagramId.hashCode() : 0);
        result = 31 * result + (instagramAccessToken != null ? instagramAccessToken.hashCode() : 0);
        result = 31 * result + (videos != null ? videos.hashCode() : 0);
        result = 31 * result + (photos != null ? photos.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + notNowCount;
        result = 31 * result + (preferredAge != null ? preferredAge.hashCode() : 0);
        temp = Double.doubleToLongBits(searchRadius);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (newImage != null ? newImage.hashCode() : 0);
        return result;
    }

    public Profile() {
    }

    public boolean hasShowReal(Context context) {
        int min = TheDistanceApplication.getApplicationComponent(context)
                .settings().getMinVideos();
        return videos != null && videos.size() >= min;
    }

    @Override
    public boolean isSameItem(Sortable other) {
        return id == ((Profile) other).id;
    }

    @Override
    public boolean isSameContent(Sortable other) {
        return equals(other);
    }

    public boolean isDummy() {
        return id == DUMMY_ID;
    }

    public static class ProfileDeserializer implements JsonDeserializer<Profile> {

        static JsonParser parser = new JsonParser();

        @Override
        public Profile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Profile profile = new Profile();

            JsonObject object = json.getAsJsonObject();

            Field[] fields = ((Class) typeOfT).getDeclaredFields();

            for (Field field : fields) {
                if (field.getName().equals("preferredAge")) {
                    continue;
                }
                Type type = field.getGenericType();
                String name = FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field);
                if (field.isAnnotationPresent(SerializedName.class)) {
                    name = field.getAnnotation(SerializedName.class).value();
                }

                JsonElement element = object.get(name);
                Object value = context.deserialize(element, type);
                try {
                    if (value != null) {
                        field.set(profile, value);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            AgeRange range = new AgeRange();
            JsonElement rangeElement = json.getAsJsonObject().get("preferred_age");

            if (profile.searchLongitude == 0 && profile.searchLatitude == 0) {
                profile.searchLatitude = profile.latitude;
                profile.searchLongitude = profile.longitude;
            }

            String rangeString;
            if (rangeElement.isJsonNull()) {
                rangeString = null;
            } else if (rangeElement.isJsonObject()) {
                AgeRange ageRange = context.deserialize(rangeElement, AgeRange.class);
                profile.preferredAge = ageRange;
                return profile;
            } else {
                rangeString = rangeElement.getAsString();
            }

            if (TextUtils.isEmpty(rangeString)) {
                range.lower = 18;
                range.upper = 75;
                profile.preferredAge = range;
                return profile;
            }

            rangeString = rangeString.replaceAll("\\\\", "");

            JsonObject rangeObject = parser.parse(rangeString).getAsJsonObject();
            range.lower = rangeObject.get("lower").getAsInt();
            range.upper = rangeObject.get("upper").getAsInt();

            profile.preferredAge = range;

            for (Video video : profile.videos) {
                if (video.id == 0) {
                    return null;
                }
            }

            return profile;
        }
    }


    private final static Calendar calendar = Calendar.getInstance();

    public static void updateAppboy(Profile profile, Context context) {
        Appboy appboy = Appboy.getInstance(context);
        appboy.changeUser(String.valueOf(profile.id));
        appboy.getCurrentUser().setFirstName(profile.firstName);
        appboy.getCurrentUser().setLastName(profile.lastName);
        appboy.getCurrentUser().setEmail(profile.email);

        try {
            Date dateOfBirth = API_DATE_FORMAT.parse(profile.dateOfBirth);
            calendar.setTime(dateOfBirth);
            appboy.getCurrentUser().setDateOfBirth(calendar.get(Calendar.YEAR), Month.getMonth(calendar.get(Calendar.MONTH)), calendar.get(Calendar.DAY_OF_MONTH));
        } catch (ParseException ignored) {
        }
        appboy.getCurrentUser().setGender(profile.gender == 0 ? Gender.MALE : Gender.FEMALE);
    }

    public static void updateRegion(Profile profile, final Context context) {
        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(context);
        final Provider<QuestionsResponse> cacheProvider = TheDistanceApplication.getApplicationComponent(context).cache().<QuestionsResponse>provider().lifeCache(30, TimeUnit.MINUTES).withKey("questions");
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        locationProvider.getReverseGeocodeObservable(profile.latitude, profile.longitude, 1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Func1<List<Address>, Observable<Region>>() {
                    @Override
                    public Observable<Region> call(List<Address> addresses) {
                        if (addresses.size() > 0) {
                            Address address = addresses.get(0);

                            return getRegion(address.getCountryCode(), context);
                        }

                        return Observable.just(Region.UK);
                    }
                }).subscribe(new Subscriber<Region>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Region region) {
                String current = preferences.getString(PREF_FIELD_REGION, "");

                if (!region.name().equals(current)) {
                    cacheProvider.evict().subscribe(TDObservers.<Void>empty());
                    preferences.edit()
                            .putString(PREF_FIELD_REGION, region.name())
                            .apply();
                }
            }
        });
    }

    public enum Region {
        UK,
        US,
        AUS
    }

    public static Region getRegion(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String regionCode = preferences.getString(PREF_FIELD_REGION, null);
        if (regionCode == null) {
            return Region.UK;
        }
        return Region.valueOf(regionCode);
    }

    private static Observable<Region> getRegion(final String regionCode, final Context context) {
        final AssetManager assetManager = context.getAssets();

        return Observable.create(new Observable.OnSubscribe<HashMap<String, List<String>>>() {
            @Override
            public void call(Subscriber<? super HashMap<String, List<String>>> subscriber) {

                try {
                    InputStream stream = assetManager.open("countries.json");

                    Gson gson = new Gson();
                    Type type = new TypeToken<HashMap<String, List<String>>>() {
                    }.getType();
                    HashMap<String, List<String>> countries = gson.fromJson(new JsonReader(new InputStreamReader(stream)), type);
                    subscriber.onNext(countries);
                    subscriber.onCompleted();

                } catch (IOException e) {
                    subscriber.onError(e);
                    subscriber.onCompleted();
                    return;
                }
            }
        }).map(new Func1<HashMap<String, List<String>>, Region>() {
            @Override
            public Region call(HashMap<String, List<String>> countyMap) {

                if (countyMap == null) {
                    return Region.UK;
                }
                List<String> countries = countyMap.get("Europe");
                if (countries != null && countries.contains(regionCode)) {
                    return Region.UK;
                }
                countries = countyMap.get("Africa");
                if (countries != null && countries.contains(regionCode)) {
                    return Region.UK;
                }
                countries = countyMap.get("Asia");
                if (countries != null && countries.contains(regionCode)) {
                    return Region.AUS;
                }
                countries = countyMap.get("Oceania");
                if (countries != null && countries.contains(regionCode)) {
                    return Region.AUS;
                }
                countries = countyMap.get("Americas");
                if (countries != null && countries.contains(regionCode)) {
                    return Region.US;
                }

                return Region.UK;
            }
        });
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.firstName);
        dest.writeString(this.lastName);
        dest.writeString(this.facebookId);
        dest.writeString(this.facebookToken);
        dest.writeString(this.email);
        dest.writeString(this.dateOfBirth);
        dest.writeInt(this.gender);
        dest.writeInt(this.interestedIn);
        dest.writeFloat(this.height);
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeDouble(this.searchLatitude);
        dest.writeDouble(this.searchLongitude);
        dest.writeString(this.city);
        dest.writeString(this.chatId);
        dest.writeByte(this.onlineStatus ? (byte) 1 : (byte) 0);
        dest.writeLong(this.lastOnline != null ? this.lastOnline.getTime() : -1);
        dest.writeString(this.instagramId);
        dest.writeString(this.instagramAccessToken);
        dest.writeTypedList(this.videos);
        dest.writeTypedList(this.photos);
        dest.writeString(this.image);
        dest.writeInt(this.notNowCount);
        dest.writeParcelable(this.preferredAge, flags);
        dest.writeDouble(this.searchRadius);
        dest.writeString(this.newImage);
    }

    protected Profile(Parcel in) {
        this.id = in.readInt();
        this.firstName = in.readString();
        this.lastName = in.readString();
        this.facebookId = in.readString();
        this.facebookToken = in.readString();
        this.email = in.readString();
        this.dateOfBirth = in.readString();
        this.gender = in.readInt();
        this.interestedIn = in.readInt();
        this.height = in.readFloat();
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.searchLatitude = in.readDouble();
        this.searchLongitude = in.readDouble();
        this.city = in.readString();
        this.chatId = in.readString();
        this.onlineStatus = in.readByte() != 0;
        long tmpLastOnline = in.readLong();
        this.lastOnline = tmpLastOnline == -1 ? null : new Date(tmpLastOnline);
        this.instagramId = in.readString();
        this.instagramAccessToken = in.readString();
        this.videos = in.createTypedArrayList(Video.CREATOR);
        this.photos = in.createTypedArrayList(Photo.CREATOR);
        this.image = in.readString();
        this.notNowCount = in.readInt();
        this.preferredAge = in.readParcelable(AgeRange.class.getClassLoader());
        this.searchRadius = in.readDouble();
        this.newImage = in.readString();
    }

    public static final Creator<Profile> CREATOR = new Creator<Profile>() {
        @Override
        public Profile createFromParcel(Parcel source) {
            return new Profile(source);
        }

        @Override
        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };
}
