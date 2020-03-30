package com.showreal.app.data.maps.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GeocodeResults {

    public List<GeocodeResult> results;

    public static class GeocodeDeserializer implements JsonDeserializer<GeocodeResults> {

        @Override
        public GeocodeResults deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            GeocodeResults geocodeResults = new GeocodeResults();

            JsonArray array = json.getAsJsonObject().getAsJsonArray("results");
            geocodeResults.results = new ArrayList<>(array.size());

            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();

                JsonArray components = object.getAsJsonArray("address_components");
                GeocodeResult result = new GeocodeResult();
                result.addressComponents = context.deserialize(components, new TypeToken<List<GeocodeResult.AddressComponent>>(){}.getType());


                JsonObject location = object.getAsJsonObject("geometry").getAsJsonObject("location");
                result.latLng = new LatLng(location.get("lat").getAsDouble(), location.get("lng").getAsDouble());

                geocodeResults.results.add(result);
            }

            return geocodeResults;
        }
    }
}
