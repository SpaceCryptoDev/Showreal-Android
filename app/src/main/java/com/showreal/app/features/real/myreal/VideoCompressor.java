package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.support.annotation.WorkerThread;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.Profile;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatExtraConstants;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class VideoCompressor {

    public interface Listener {
        void onCompressed(File file, float scale);

        void onFailed(Exception exception);
    }

    public static class Result {
        final File file;
        final float scale;

        public Result(File file, float scale) {
            this.file = file;
            this.scale = scale;
        }
    }

    private final Profile profile;
    private File input;
    private File output;
    private static final int VIDEO_BITRATE = 2000 * 1000;
    private static final int AUDIO_BITRATE = 128 * 1000;
    private static final int RESOLUTION_LOW = 720;

    public VideoCompressor(Profile profile) {
        this.profile = profile;
    }

    public Observable<Result> compress(final File file, final Context context) {
        return Observable.create(new Observable.OnSubscribe<Result>() {
            @Override
            public void call(final Subscriber<? super Result> subscriber) {
                compress(file, context, new Listener() {
                    @Override
                    public void onCompressed(File file, float scale) {
                        subscriber.onNext(new Result(file, scale));
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailed(Exception exception) {
                        subscriber.onError(exception);
                        subscriber.onCompleted();
                    }
                });
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @WorkerThread
    public void compress(File file, Context context, final Listener listener) {
        input = file;
        try {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            FileInputStream inputStream = new FileInputStream(file);
            FileDescriptor descriptor = inputStream.getFD();
            metaRetriever.setDataSource(descriptor);

            String heightS = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String widthS = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

            inputStream.close();
            metaRetriever.release();

            int height = Integer.parseInt(heightS);
            int width = Integer.parseInt(widthS);

            int lowest = Math.min(height, width);
            if (lowest > RESOLUTION_LOW) {
                if (height > width) {
                    float ratio = (float) height / width;
                    width = RESOLUTION_LOW;
                    height = (int) (width * ratio);
                } else {
                    float ratio = (float) width / height;
                    height = RESOLUTION_LOW;
                    width = (int) (height * ratio);
                }
            }

            File dir = TheDistanceApplication.getApplicationComponent(context).videoHelper().getUserVideoDir(profile);
            output = File.createTempFile("compressed", ".mp4", dir);

            final float scale = width / Float.parseFloat(widthS);
            MediaTranscoder.getInstance().transcodeVideo(file.getAbsolutePath(), output.getAbsolutePath(), new ShowRealFormatStrategy(width, height), new MediaTranscoder.Listener() {
                @Override
                public void onTranscodeProgress(double progress) {

                }

                @Override
                public void onTranscodeCompleted() {
                    input.delete();
                    if (output.renameTo(input)) {
                        listener.onCompressed(input, scale);
                        return;
                    }

                    listener.onCompressed(output, scale);
                }

                @Override
                public void onTranscodeCanceled() {

                }

                @Override
                public void onTranscodeFailed(Exception exception) {
                    listener.onFailed(exception);
                }
            });
        } catch (Exception e) {
            listener.onFailed(e);
        }

    }

    private static class ShowRealFormatStrategy implements MediaFormatStrategy {

        private final int width;
        private final int height;

        private ShowRealFormatStrategy(int width, int height) {
            this.width = width;
            this.height = height;
        }


        @Override
        public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormatExtraConstants.MIMETYPE_VIDEO_AVC, width, height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

            return format;
        }

        @Override
        public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
            final MediaFormat format = MediaFormat.createAudioFormat(MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC,
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), 1);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
            return format;
        }
    }

}
