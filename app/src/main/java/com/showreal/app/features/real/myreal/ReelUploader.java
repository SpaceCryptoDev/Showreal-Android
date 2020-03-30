package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.util.SparseArray;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.AppSettings;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Video;
import com.showreal.app.data.model.VideoUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivecache.Provider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class ReelUploader implements DataUploader<List<Video>, ReelUpdateContent> {

    private final Provider<Profile> profileProvider;
    private final Provider<Profile> staleProfileProvider;
    private final Context context;
    private final ReelHelper reelHelper;
    private Profile profile;
    final ShowRealApi api;
    final VideoHelper videoHelper;
    private List<VideoUpload> uploads;
    private SparseArray<String> videoSparseArray = new SparseArray<>();
    private final AppSettings settings;
    private List<Video> videos;

    public ReelUploader(Context context, Profile profile) {
        this.profile = profile;
        this.context = context;
        api = TheDistanceApplication.getApplicationComponent(context).api();
        videoHelper = TheDistanceApplication.getApplicationComponent(context).videoHelper();
        reelHelper = TheDistanceApplication.getApplicationComponent(context).reelHelper();

        for (Video video : profile.videos) {
            videoSparseArray.put(video.id, video.url);
        }
        profileProvider = TheDistanceApplication.getApplicationComponent(context).profileProvider();
        staleProfileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
        settings = TheDistanceApplication.getApplicationComponent(context).settings();
    }

    public void setProfile(Profile profile) {
        this.profile = profile;

        videoSparseArray.clear();
        for (Video video : profile.videos) {
            videoSparseArray.put(video.id, video.url);
        }
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(List<Video> content) {
        this.videos = content;
        uploads = new ArrayList<>();
        List<Integer> skipped = new ArrayList<>();
        for (Video video : content) {

            if (video.published && !video.modified) {
                skipped.add(video.id);
                continue;
            }

            String oldVideo = videoSparseArray.get(video.id);
            if (oldVideo != null) {
                VideoUpload upload = new VideoUpload(video, true);
                upload.noVideo = !video.videoModified;
                uploads.add(upload);
                videoSparseArray.remove(video.id);
            } else {
                uploads.add(new VideoUpload(video, false));
            }
        }

        if (videoSparseArray.size() > 0) {
            for (int i = 0; i < videoSparseArray.size(); i++) {
                int id = videoSparseArray.keyAt(i);
                if (skipped.contains(id)) {
                    continue;
                }
                uploads.add(new VideoUpload(id));
            }
        }
    }

    public List<VideoUpload> getUploads(List<Video> videos) {
        SparseArray<String> videoArray = new SparseArray<>(videoSparseArray.size());
        for (int i = 0; i < videoSparseArray.size(); i++) {
            int key = videoSparseArray.keyAt(i);
            videoArray.put(key, videoSparseArray.get(key));
        }

        List<VideoUpload> uploads = new ArrayList<>();
        List<Integer> skipped = new ArrayList<>();
        for (Video video : videos) {

            if (video.published && !video.modified) {
                skipped.add(video.id);
                continue;
            }

            String oldVideo = videoArray.get(video.id);
            if (oldVideo != null) {
                VideoUpload upload = new VideoUpload(video, true);
                upload.noVideo = !video.videoModified;
                uploads.add(upload);
                videoArray.remove(video.id);
            } else {
                uploads.add(new VideoUpload(video, false));
            }
        }

        if (videoArray.size() > 0) {
            for (int i = 0; i < videoArray.size(); i++) {
                int id = videoArray.keyAt(i);
                if (skipped.contains(id)) {
                    continue;
                }
                uploads.add(new VideoUpload(id));
            }
        }
        return uploads;
    }

    private Observable<ResponseBody> getObservable(VideoUpload videoUpload) {
        if (videoUpload.isRemove) {
            return api.deleteVideo(videoUpload.question);
        }
        if (videoUpload.isEdit) {
            if (videoUpload.noVideo) {
                return api.editVideoData(videoUpload.question, createDataBody(videoUpload));
            }
            return api.editVideo(videoUpload.question, createVideoBody(videoUpload.path), createDataBody(videoUpload));
        }
        return api.uploadVideo(videoUpload.question, createVideoBody(videoUpload.path), createDataBody(videoUpload));
    }

    List<VideoUpload> errors = new ArrayList<>();

    @Override
    public Observable<ReelUpdateContent> getUpload() {
        errors.clear();

        if (videos.size() < settings.getMinVideos()) {
            return Observable.error(new ReelUploadException(1));
        }

        if (uploads.isEmpty()) {
            return Observable.error(new ReelUploadException(0));
        }

        Collections.sort(uploads);

        List<Observable<ResponseBody>> observables = new ArrayList<>(uploads.size());
        for (VideoUpload upload : uploads) {
            observables.add(getObservable(upload));
        }

        return Observable.from(uploads)
                .flatMap(new Func1<VideoUpload, Observable<VideoUpload>>() {
                    @Override
                    public Observable<VideoUpload> call(final VideoUpload videoUpload) {
                        return getObservable(videoUpload)
                                .onErrorReturn(new Func1<Throwable, ResponseBody>() {
                                    @Override
                                    public ResponseBody call(Throwable throwable) {
                                        errors.add(videoUpload);
                                        return null;
                                    }
                                })
                                .map(new Func1<ResponseBody, VideoUpload>() {
                                    @Override
                                    public VideoUpload call(ResponseBody response) {
                                        return response == null ? null : videoUpload;
                                    }
                                });
                    }
                }).doOnNext(new Action1<VideoUpload>() {
                    @Override
                    public void call(VideoUpload upload) {
                        if (upload != null) {
                            if (!upload.isRemove) {
                                videoHelper.renamePublished(upload, profile);
                            }
                        }
                    }
                }).toList()
                .flatMap(new Func1<List<VideoUpload>, Observable<Profile>>() {
                    @Override
                    public Observable<Profile> call(List<VideoUpload> voids) {
                        return api.getProfile();
                    }
                })
                .compose(profileProvider.replace())
                .compose(staleProfileProvider.replace())
                .map(new Func1<Profile, ReelUpdateContent>() {
                    @Override
                    public ReelUpdateContent call(Profile profile) {
                        reelHelper.updateReel(profile, errors);
                        return new ReelUpdateContent(profile, errors.size());
                    }
                });
    }

    private RequestBody createVideoBody(String path) {
        path = path.startsWith("file:") ? path.substring(path.indexOf("://") + 3) : path;
        return RequestBody.create(MediaType.parse("video/mp4"), new File(path));
    }

    protected Map<String, RequestBody> createDataBody(VideoUpload upload) {
        Map<String, RequestBody> bodyMap = new HashMap<>();

        bodyMap.put("index", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(upload.index)));
        if (!upload.isEdit) {
            bodyMap.put("question", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(upload.question)));
        }
        bodyMap.put("offset_x", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(upload.offsetX)));
        bodyMap.put("offset_y", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(upload.offsetY)));
        bodyMap.put("scale", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(upload.scale)));
        bodyMap.put("duration", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(upload.duration)));
        bodyMap.put("is_light_filter_applied", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(upload.isLightFilterEnabled)));

        return bodyMap;
    }


    public static class ReelUploadException extends IllegalArgumentException {

        public final int type;

        public ReelUploadException(int type) {
            this.type = type;
        }

        @Override
        public String getMessage() {
            return "";
        }

        @Override
        public String getLocalizedMessage() {
            return "";
        }
    }
}
