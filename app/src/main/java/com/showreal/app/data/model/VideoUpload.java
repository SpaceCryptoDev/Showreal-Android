package com.showreal.app.data.model;

public class VideoUpload implements Comparable<VideoUpload> {

    public boolean isLightFilterEnabled;
    public int questionId = -1;
    public int question;
    public double duration;
    public double offsetX;
    public double offsetY;
    public double scale;
    public boolean isEdit;
    public int index;
    public boolean noVideo;
    public String path;
    public long id;
    public boolean isRemove;

    public VideoUpload(Video video, boolean isEdit) {
        this.id = video._id;
        this.question = video.id;
        this.duration = video.duration;
        this.offsetX = video.offsetX;
        this.offsetY = video.offsetY;
        this.scale = video.scale;
        this.isEdit = isEdit;
        this.index = video.question.index;
        this.path = video.url;
        this.questionId = video.question.id;
        this.isLightFilterEnabled = video.isLightFilterEnabled;
    }

    public VideoUpload(int id) {
        this.question = id;
        isRemove = true;
    }

    @Override
    public int compareTo(VideoUpload o) {
        if (isRemove) {
            return -1;
        }
        if (o.isRemove) {
            return 1;
        }
        return 0;
    }
}
