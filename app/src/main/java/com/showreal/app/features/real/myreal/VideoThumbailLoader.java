package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Profile;
import com.showreal.app.features.real.VideoDownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

public class VideoThumbailLoader implements StreamModelLoader<VideoThumbailLoader.VideoThumbnail> {

    private Context context;

    public static class VideoThumbnail {
        final Profile profile;
        final ShowRealVideo video;

        public VideoThumbnail(Profile profile, ShowRealVideo video) {
            this.profile = profile;
            this.video = video;
        }
    }

    public VideoThumbailLoader(Context context) {
        this.context = context;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(VideoThumbnail model, int width, int height) {
        return new VideoThumbnailFetcher(model, context);
    }

    public static class Factory implements ModelLoaderFactory<VideoThumbnail, InputStream> {

        @Override
        public ModelLoader<VideoThumbnail, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new VideoThumbailLoader(context);
        }

        @Override
        public void teardown() {

        }
    }

    public VideoThumbailLoader() {
    }

    private static class VideoThumbnailFetcher implements DataFetcher<InputStream> {

        private final ShowRealVideo video;
        private final VideoDownloader videoDownloader;
        private final Profile profile;

        private VideoThumbnailFetcher(VideoThumbnail thumbnail, Context context) {
            this.video = thumbnail.video;
            this.profile = thumbnail.profile;
            videoDownloader = TheDistanceApplication.getApplicationComponent(context).videoDownloader();
        }

        @Override
        public InputStream loadData(Priority priority) throws Exception {
            ByteArrayInputStream inputStream = null;
            MediaMetadataRetriever mediaMetadataRetriever = null;

            try {
                Bitmap bitmap;

                if (profile != null && videoDownloader.userVideoDownloaded(video.video, profile)) {
                    bitmap = ThumbnailUtils.createVideoThumbnail(videoDownloader.userVideoFile(video.video, profile).getPath(), MediaStore.Video.Thumbnails.MINI_KIND);
                } else if (video.path.startsWith("file:")) {
                    bitmap = ThumbnailUtils.createVideoThumbnail(video.path.substring(video.path.indexOf("://") + 3), MediaStore.Video.Thumbnails.MINI_KIND);
                } else {

                    mediaMetadataRetriever = new MediaMetadataRetriever();
                    if (Build.VERSION.SDK_INT >= 14) {
                        mediaMetadataRetriever.setDataSource(video.path, new HashMap<String, String>());
                    } else {
                        mediaMetadataRetriever.setDataSource(video.path);
                    }

                    bitmap = mediaMetadataRetriever.getFrameAtTime();
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                byte[] bitmapdata = bos.toByteArray();
                inputStream = new ByteArrayInputStream(bitmapdata);

            } catch (Exception e) {
                throw e;

            } finally {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                }
            }
            return inputStream;
        }

        @Override
        public void cleanup() {

        }

        @Override
        public String getId() {
            return video.path;
        }

        @Override
        public void cancel() {

        }


    }
}
