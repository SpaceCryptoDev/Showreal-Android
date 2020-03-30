package com.showreal.app.features.real.myreal;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;
import com.showreal.app.databinding.ActivityChooseVideoBinding;
import com.showreal.app.features.conversations.EasyVideo;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import pl.aprilapps.easyphotopicker.EasyImage;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class ClipAdjustActivity extends BaseActivity {

    static final String EXTRA_QUESTION = "question";
    static final String EXTRA_PROFILE = "profile";
    private static final int RC_CROP = 0x0;
    private ActivityChooseVideoBinding binding;
    private File videoFile;
    private final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
    private Question question;
    private Profile profile;

    @Override
    protected String getScreenName() {
        return Screens.CHOOSE_CLIP;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_choose_video);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        question = getIntent().getParcelableExtra(EXTRA_QUESTION);
        profile = getIntent().getParcelableExtra(EXTRA_PROFILE);

        if (videoFile == null) {
            chooseVideo();
        }

        binding.buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideo();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crop_video, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_done).setEnabled(videoFile != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                goToCrop();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToCrop() {
        binding.loadingLayout.loadingLayout.show();
        Observable.create(new Observable.OnSubscribe<Video>() {
            @Override
            public void call(Subscriber<? super Video> subscriber) {

                Video video = new Video();
                video.url = "file:///" + videoFile.getPath();
                video.offsetX = binding.crop.getVideoOffsetX();
                video.offsetY = binding.crop.getVideoOffsetY();
                video.scale = binding.crop.getScale();
                video.question = question;
                video.id = question.id;

                metadataRetriever.setDataSource(ClipAdjustActivity.this, Uri.parse(video.url));
                String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration = Long.parseLong(time) / 1000;
                video.duration = duration;

                if (video.duration <= 5) {
                    File dest = getAppComponent().videoHelper().getFileForVideo(profile, video.question.id, null);
                    if (videoFile.renameTo(dest)) {
                        video.url = "file:///" + dest.getPath();
                    }
                }

                subscriber.onNext(video);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Video>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        binding.loadingLayout.loadingLayout.hide();
                    }

                    @Override
                    public void onNext(Video video) {
                        compressAndCrop(video);
                    }
                });
    }

    private void compressAndCrop(final Video video) {
        File file = new File(video.url.substring(video.url.indexOf(":///") + 3));
        VideoCompressor compressor = new VideoCompressor(profile);
        compressor.compress(file, this)
                .subscribe(new Subscriber<VideoCompressor.Result>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        binding.loadingLayout.loadingLayout.hide();
                        Intent intent = new Intent(ClipAdjustActivity.this, CropVideoActivity.class);
                        intent.putExtra(CropVideoActivity.EXTRA_VIDEO, video);
                        intent.putExtra(CropVideoActivity.EXTRA_PROFILE, profile);
                        startActivityForResult(intent, RC_CROP);
                    }

                    @Override
                    public void onNext(VideoCompressor.Result result) {
                        binding.loadingLayout.loadingLayout.hide();
                        video.url = result.file.getPath();
                        videoFile = result.file;

                        Intent intent = new Intent(ClipAdjustActivity.this, CropVideoActivity.class);
                        intent.putExtra(CropVideoActivity.EXTRA_VIDEO, video);
                        intent.putExtra(CropVideoActivity.EXTRA_PROFILE, profile);
                        startActivityForResult(intent, RC_CROP);
                    }
                });


    }

    private void chooseVideo() {
        EasyVideo.openChooserWithDocuments(this, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_CROP) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                intent.putExtra(RecordActivity.EXTRA_VIDEO, data.getParcelableExtra(CropVideoActivity.EXTRA_VIDEO));
                intent.putExtra(RecordActivity.EXTRA_PROFILE, profile);
                setResult(RESULT_OK, intent);
                finish();
            }
            return;
        }

        EasyVideo.handleActivityResult(requestCode, resultCode, data, this, new EasyImage.Callbacks() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {

            }

            @Override
            public void onImagePicked(File file, EasyImage.ImageSource source, int type) {

                File renamed = getAppComponent().videoHelper().getFileForVideo(profile, question.id, VideoHelper.VideoSuffix.Original);
                renamed.getParentFile().mkdirs();
                if (renamed.exists()) {
                    renamed.delete();
                }

                try {
                    FileUtils.moveFile(file, renamed);
                    file = renamed;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                videoFile = file;
                showThumbnail();
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {

            }
        });
    }

    private void showThumbnail() {
        if (videoFile == null) {
            return;
        }
        Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoFile.getPath(), MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                if (bitmap != null) {
                    subscriber.onNext(bitmap);
                } else {
                    subscriber.onError(new IllegalArgumentException("Could not get thumbnail"));
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        binding.crop.setImageBitmap(bitmap);
                    }
                });
    }
}
