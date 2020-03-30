package com.showreal.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Session {

    public String token;
    @SerializedName("user")
    public Profile profile;
}
