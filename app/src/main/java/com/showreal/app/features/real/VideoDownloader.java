package com.showreal.app.features.real;

import android.content.Context;

import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Video;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class VideoDownloader {

    private static final String TAG = VideoDownloader.class.getSimpleName();
    private final Context context;
    private final VideoHelper videoHelper;
    private final ShowRealApi api;

    public VideoDownloader(Context context, VideoHelper videoHelper, ShowRealApi api) {
        this.context = context;
        this.videoHelper = videoHelper;
        this.api = api;
    }

    public boolean userVideoDownloaded(Video video, Profile profile) {
        File file = videoHelper.getFileForVideo(profile, video.question.id, VideoHelper.VideoSuffix.Published);
        return file.exists();
    }

    public File userVideoFile(Video video, Profile profile) {
        return videoHelper.getFileForVideo(profile, video.question.id, VideoHelper.VideoSuffix.Published);
    }

    public boolean videoCached(Video video) {
        File file = videoHelper.getCacheFileForVideo(video);
        return file.exists();
    }

    public File videoFile(Video video) {
        return videoHelper.getCacheFileForVideo(video);
    }

    public Observable<Boolean> downloadVideo(Video video) {
        final File dest = videoFile(video);
        return api.downloadFile(video.url)
                .map(new Func1<ResponseBody, Boolean>() {
                    @Override
                    public Boolean call(ResponseBody responseBody) {
                        return writeToFile(responseBody, dest);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Video> downloadUserVideo(final Video video, Profile profile) {
        final File dest = userVideoFile(video, profile);

        return api.downloadFile(video.url)
                .map(new Func1<ResponseBody, Boolean>() {
                    @Override
                    public Boolean call(ResponseBody responseBody) {
                        return writeToFile(responseBody, dest);
                    }
                }).map(new Func1<Boolean, Video>() {
                    @Override
                    public Video call(Boolean downloaded) {
                        return downloaded ? video : null;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<List<String>> downloadUserVideos(Profile profile, List<Video> videos) {
        List<Observable<Video>> downloads = new ArrayList<>(videos.size());
        for (Video video : videos) {
            if (!userVideoDownloaded(video, profile)) {
                downloads.add(downloadUserVideo(video, profile));
            } else {
                downloads.add(Observable.just(video));
            }
        }

        return Observable.merge(downloads)
                .onErrorReturn(new Func1<Throwable, Video>() {
                    @Override
                    public Video call(Throwable throwable) {
                        return null;
                    }
                }).filter(new Func1<Video, Boolean>() {
                    @Override
                    public Boolean call(Video video) {
                        return video != null;
                    }
                }).map(new Func1<Video, String>() {
                    @Override
                    public String call(Video video) {
                        return video.url;
                    }
                }).toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private boolean writeToFile(ResponseBody body, File file) {
        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }


}
