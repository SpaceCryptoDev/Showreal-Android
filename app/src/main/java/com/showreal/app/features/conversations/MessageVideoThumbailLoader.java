package com.showreal.app.features.conversations;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.showreal.app.data.model.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class MessageVideoThumbailLoader implements StreamModelLoader<Message> {

    @Override
    public DataFetcher<InputStream> getResourceFetcher(Message model, int width, int height) {
        return new VideoThumbnailFetcher(model);
    }

    public static class Factory implements ModelLoaderFactory<Message, InputStream> {

        @Override
        public ModelLoader<Message, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new MessageVideoThumbailLoader();
        }

        @Override
        public void teardown() {

        }
    }

    private static class VideoThumbnailFetcher implements DataFetcher<InputStream> {

        private final Message message;

        private VideoThumbnailFetcher(Message message) {
            this.message = message;
        }

        @Override
        public InputStream loadData(Priority priority) throws Exception {
            ByteArrayInputStream inputStream = null;
            MediaMetadataRetriever mediaMetadataRetriever = null;
            try {
                mediaMetadataRetriever = new MediaMetadataRetriever();
                if (Build.VERSION.SDK_INT >= 14) {
                    mediaMetadataRetriever.setDataSource(message.mediaUrl, new HashMap<String, String>());
                } else {
                    mediaMetadataRetriever.setDataSource(message.mediaUrl);
                }

                Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime();

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
            return message.mediaUrl;
        }

        @Override
        public void cancel() {

        }
    }
}
