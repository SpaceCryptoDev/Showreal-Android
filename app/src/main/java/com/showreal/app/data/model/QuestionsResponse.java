package com.showreal.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class QuestionsResponse {

    @SerializedName("results")
    public List<Question> questions;
}
