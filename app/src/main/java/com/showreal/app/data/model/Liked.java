package com.showreal.app.data.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

import uk.co.thedistance.components.lists.interfaces.Sortable;

public class Liked implements Sortable {

    @SerializedName("liked")
    public Profile profile;

    @Override
    public boolean isSameItem(Sortable other) {
        return ((Liked) other).profile.isSameItem((((Liked) other).profile));
    }

    @Override
    public boolean isSameContent(Sortable other) {
        return ((Liked) other).profile.isSameContent((((Liked) other).profile));
    }

    private static final Profile.ProfileDeserializer PROFILE_DESERIALIZER = new Profile.ProfileDeserializer();

    public static class LikedDeserializer implements JsonDeserializer<Liked> {

        @Override
        public Liked deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            Liked liked = new Liked();
            JsonElement profile = json.getAsJsonObject().get("liked");
            liked.profile = PROFILE_DESERIALIZER.deserialize(profile, Profile.class, context);

            return liked;
        }
    }
}
