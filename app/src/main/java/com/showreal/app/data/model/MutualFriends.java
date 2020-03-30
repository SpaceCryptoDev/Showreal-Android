package com.showreal.app.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MutualFriends {

    public int totalCount;
    public String after;
    public List<MutualFriend> friends;

    public static class MutualFriendsDeserializer implements JsonDeserializer<MutualFriends> {

        @Override
        public MutualFriends deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MutualFriends mutualFriends = new MutualFriends();

            if (json == null || json.isJsonNull()) {
                mutualFriends.totalCount = 0;
                mutualFriends.friends = new ArrayList<>();
                return mutualFriends;
            }

            JsonObject object = json.getAsJsonObject();


            mutualFriends.totalCount = object.getAsJsonPrimitive("total_count").getAsInt();
            mutualFriends.after = object.get("after").getAsString();

            JsonArray array = object.getAsJsonObject("data").getAsJsonArray("data");
            mutualFriends.friends = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                JsonObject friendJson = element.getAsJsonObject();
                MutualFriend friend = new MutualFriend(friendJson.getAsJsonObject("picture").getAsJsonObject("data").get("url").getAsString(), friendJson.get("name").getAsString());
                if (friendJson.has("id")) {
                    friend.id = friendJson.get("id").getAsString();
                }
                mutualFriends.friends.add(friend);
            }

            return mutualFriends;
        }
    }
}
