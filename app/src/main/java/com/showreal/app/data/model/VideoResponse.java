package com.showreal.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VideoResponse {

    int count;
    @SerializedName("results")
    List<Video> videos;
}
