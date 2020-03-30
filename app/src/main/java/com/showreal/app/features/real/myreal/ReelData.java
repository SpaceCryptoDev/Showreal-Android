package com.showreal.app.features.real.myreal;

import com.showreal.app.data.model.DeviceReel;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;

import java.util.List;

public class ReelData {

    public final List<Question> questions;
    public DeviceReel reel;
    public List<Video> videos;

    public ReelData(List<Question> questions, DeviceReel reel) {
        this.questions = questions;
        this.reel = reel;
    }
}
