package com.showreal.app.data.maps.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class GeocodeResult {

    List<AddressComponent> addressComponents;
    public LatLng latLng;

    static class AddressComponent {

        String longName;
        String shortName;
        List<String> types;
    }

    static class Geometry {
        String lat;
        String lng;
    }

    public String getNeighborhood() {
        for (AddressComponent component : addressComponents) {
            if (component.types.contains("neighborhood")) {
                return component.longName;
            }
        }
        return null;
    }

    public String getArea() {
        for (AddressComponent component : addressComponents) {
            if (component.types.contains("sublocality")) {
                return component.longName;
            }
        }
        return null;
    }

    public String getCity() {
        for (AddressComponent component : addressComponents) {
            if (component.types.contains("locality")) {
                return component.longName;
            }
        }
        return null;
    }

    public String getPostalTown() {
        for (AddressComponent component : addressComponents) {
            if (component.types.contains("postal_town")) {
                return component.longName;
            }
        }
        return null;
    }

    public String getState() {
        boolean isUS = false;
        for (AddressComponent component : addressComponents) {
            if (component.types.contains("country") && component.shortName.equals("US")) {
                isUS = true;
                break;
            }
        }

        String admin1 = null;
        String admin2 = null;

        for (AddressComponent component : addressComponents) {
            if (component.types.contains("administrative_area_level_1")) {
                admin1 = component.longName;
            } else if (component.types.contains("administrative_area_level_2")) {
                admin2 = component.longName;
            }
        }

        if (!isUS && admin2 != null) {
            return admin2;
        }

        if (admin1 != null) {
            return admin1;
        }
        if (admin2 != null) {
            return admin2;
        }
        return null;
    }
}
