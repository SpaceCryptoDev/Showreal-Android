package com.showreal.app.data;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.Nullable;

import com.danikula.videocache.file.FileNameGenerator;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Video;
import com.showreal.app.data.model.VideoUpload;

import java.io.File;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.Version;

public class VideoHelper {

    private static final String VIDEO_FOLDER = "ShowReal";
    private final Context context;
    private File videoDir = null;
    private File cacheDir = null;

    public VideoHelper(Context context) {
        this.context = context;
    }

    public File getUserVideoDir(@Nullable Profile profile) {
        File dir = getVideoDirectory();


        if (profile != null) {
            String userDir = String.valueOf(profile.id);
            dir = new File(dir, userDir);
        }

        dir.mkdirs();
        return dir;
    }

    private File getVideoDirectory() {
        if (videoDir != null) {
            return videoDir;
        }
        if (Version.isLollipop()) {
            File[] dirs = context.getExternalFilesDirs(Environment.DIRECTORY_MOVIES);
            if (dirs.length > 0) {
                long largest = 0;
                for (File file : dirs) {
                    if (file != null && Environment.getExternalStorageState(file).equals(Environment.MEDIA_MOUNTED)) {
                        StatFs statFs = new StatFs(file.getPath());
                        long available = statFs.getAvailableBytes();
                        if (available > largest) {
                            largest = available;
                            videoDir = file;
                        }
                    }
                }
            }
        }

        if (videoDir == null) {
            videoDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (videoDir == null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                videoDir = Environment.getExternalStorageDirectory();
            }
            if (videoDir == null) {
                videoDir = context.getFilesDir();
            }
        }

        videoDir = new File(videoDir, VIDEO_FOLDER);
        videoDir.mkdirs();
        return videoDir;
    }

    public File getVideoCacheDir() {
        if (cacheDir != null) {
            return cacheDir;
        }

        cacheDir = context.getExternalCacheDir();
        boolean available = false;
        if (cacheDir != null) {
            if (Version.isLollipop()) {
                available = Environment.getExternalStorageState(cacheDir).endsWith(Environment.MEDIA_MOUNTED);
            } else {
                available = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            }
        }

        if (!available) {
            cacheDir = context.getCacheDir();
        }

        return cacheDir;
    }

    public enum VideoSuffix {
        Published("_PUBLISHED"),
        Original("_ORIGINAL"),
        Temp("_TEMP");

        public final String value;

        VideoSuffix(String value) {
            this.value = value;
        }
    }

    public File getFileForVideo(Profile profile, int questionId, VideoSuffix suffix) {
        File dir = getUserVideoDir(profile);
        String path = String.valueOf(questionId) + (suffix == null ? "" : suffix.value) + ".mp4";
        return new File(dir, path);
    }

    public File getCacheFileForVideo(Video video) {
        return getCacheFileForUrl(video.url);
    }

    public File getCacheFileForUrl(String url) {
        File dir = getVideoCacheDir();
        Uri uri = Uri.parse(url);
        String path = uri.getLastPathSegment();
        return new File(dir, path);
    }

    public FileNameGenerator getFileNameGenerator() {
        return new FileNameGenerator() {
            @Override
            public String generate(String url) {
                Uri uri = Uri.parse(url);
                return uri.getLastPathSegment();
            }
        };
    }

    public String getPlaybackPath(Profile profile, Video video) {
        File file = getFileForVideo(profile, video.question.id, VideoSuffix.Published);
        if (video.published && file.exists()) {
            return String.format("file://%s", file.getAbsolutePath());
        }
        file = getFileForVideo(profile, video.question.id, null);
        if (file.exists()) {
            return String.format("file://%s", file.getAbsolutePath());
        }
        return video.url;
    }

    public void renamePublished(VideoUpload upload, Profile profile) {
        File file = getFileForVideo(profile, upload.question, null);
        if (file.exists()) {
            File renamed = getFileForVideo(profile, upload.question, VideoSuffix.Published);
            if (renamed.exists()) {
                renamed.delete();
            }
            file.renameTo(renamed);
        }
    }

    public Observable<Void> deleteFilesDorVideo(final Profile profile, final Video video) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                File videoFile = getFileForVideo(profile, video.question.id, null);
                if (videoFile.exists()) {
                    videoFile.delete();
                }
                for (VideoSuffix suffix : VideoSuffix.values()) {
                    File file = getFileForVideo(profile, video.question.id, suffix);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io());
    }
}
