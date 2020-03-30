package com.showreal.app.features.real.myreal;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Point;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.ragnarok.rxcamera.RxCamera;
import com.ragnarok.rxcamera.config.CameraUtil;
import com.ragnarok.rxcamera.config.RxCameraConfig;
import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;
import com.showreal.app.databinding.ActivityRecordBinding;
import com.showreal.app.databinding.ReelCountdownBinding;
import com.showreal.app.features.real.ReelPlayer;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.TDObservers;
import uk.co.thedistance.thedistancecore.Version;

public class RecordActivity extends BaseActivity implements CountdownView.CountdownListener {

    static final String EXTRA_QUESTION = "question";
    static final String EXTRA_PROFILE = "profile";
    static final String EXTRA_VIDEO = "video";
    private static final int RC_CROP = 0x0;
    private static final int MAX_RECORD = 8;
    private ActivityRecordBinding binding;
    private RxCamera camera;
    private MediaRecorder mediaRecorder;
    private Question question;
    private Profile profile;
    private ReelPlayer player;
    private Video video;
    private MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

    @Override
    protected String getScreenName() {
        return Screens.RECORD_CLIP;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_record);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.cameraTexture.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.cameraTexture.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                if (RxPermissions.getInstance(RecordActivity.this).isGranted(Manifest.permission.CAMERA)
                        && RxPermissions.getInstance(RecordActivity.this).isGranted(Manifest.permission.RECORD_AUDIO)) {
                    openCamera();
                } else {
                    getPermission();
                }
            }
        });

        question = getIntent().getParcelableExtra(EXTRA_QUESTION);
        profile = getIntent().getParcelableExtra(EXTRA_PROFILE);
        binding.question.setText(question.text);

        binding.buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.buttonCapture.setActivated(!binding.buttonCapture.isActivated());
                record(binding.buttonCapture.isActivated());
            }
        });

        binding.buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishRecording();
            }
        });

        binding.buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelRecording();
            }
        });

        if (Version.isLollipop()) {
            binding.buttonFlip.setImageResource(R.drawable.front_back_switch_button_animation);
        }
        binding.buttonFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipCamera();
            }
        });
    }

    private void cancelRecording() {
        video = null;
        showRecordingUi(true);
        binding.hint.setVisibility(View.VISIBLE);
        File file = getAppComponent().videoHelper().getFileForVideo(profile, question.id, VideoHelper.VideoSuffix.Temp);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (player != null) {
            player.pause();
            player.clear();
        }

        View view = binding.layout.findViewById(R.id.countdown);
        if (view != null) {
            ((CountdownView) view).stopCountdown();
            binding.layout.removeView(view);
            binding.hint.setVisibility(View.VISIBLE);
        }
        stopRecording();
        binding.buttonCapture.setActivated(false);

        closeCamera();
        releaseMediaRecorder();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.destroy();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (player != null) {
            player.ready();
        }

        if (camera != null) {
            if (RxPermissions.getInstance(RecordActivity.this).isGranted(Manifest.permission.CAMERA)
                    && RxPermissions.getInstance(RecordActivity.this).isGranted(Manifest.permission.RECORD_AUDIO)) {
                openCamera();
            } else {
                getPermission();
            }
        }
    }

    private void finishRecording() {
        File file = getAppComponent().videoHelper().getFileForVideo(profile, question.id, VideoHelper.VideoSuffix.Temp);

        if (video.duration > 5) {
            File rename = getAppComponent().videoHelper().getFileForVideo(profile, question.id, VideoHelper.VideoSuffix.Original);
            if (!video.url.equals(rename.getPath())) {
                if (rename.exists()) {
                    rename.delete();
                }
                if (file.renameTo(rename)) {
                    video.url = rename.getPath();
                }
                if (player != null) {
                    player.destroy();
                }

                List<Video> videos = new ArrayList<>(1);
                videos.add(video);
                player = ReelPlayer.with(RecordActivity.this, -1)
                        .audio(true)
                        .binding(binding)
                        .videoOnly(true)
                        .videos(videos)
                        .skipCache(true)
                        .profile(profile)
                        .create();
                player.setup();
            }

            Intent intent = new Intent(this, CropVideoActivity.class);
            intent.putExtra(CropVideoActivity.EXTRA_VIDEO, video);
            intent.putExtra(CropVideoActivity.EXTRA_PROFILE, profile);
            startActivityForResult(intent, RC_CROP);
            return;
        }

        File rename = getAppComponent().videoHelper().getFileForVideo(profile, question.id, null);
        if (rename.exists()) {
            rename.delete();
        }
        if (file.renameTo(rename)) {
            video.url = rename.getPath();
        }

        Intent intent = getIntent();
        intent.putExtra(EXTRA_VIDEO, video);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void flipCamera() {
        if (camera == null) {
            return;
        }
        if (Version.isLollipop()) {
            Drawable drawable = binding.buttonFlip.getDrawable();
            if (drawable instanceof AnimatedVectorDrawableCompat) {
                ((AnimatedVectorDrawableCompat) drawable).start();
            } else if (drawable instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) drawable).start();
            }
        }
        camera.switchCamera().subscribeOn(Schedulers.io())
                .subscribe(TDObservers.<Boolean>empty());
    }

    private void getPermission() {
        RxPermissions.getInstance(this)
                .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            openCamera();
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(RecordActivity.this, Manifest.permission.CAMERA)
                                || ActivityCompat.shouldShowRequestPermissionRationale(RecordActivity.this, Manifest.permission.RECORD_AUDIO)) {
                            showPermissionRationale();
                        }
                    }
                });
    }

    private void openCamera() {

        float ratio = (float) binding.cameraTexture.getHeight() / (float) binding.cameraTexture.getWidth();
        int height = (int) (720 * ratio);

        RxCameraConfig config = new RxCameraConfig.Builder()
                .setAutoFocus(true)
                .setHandleSurfaceEvent(true)
                .setMuteShutterSound(true)
                .useFrontCamera()
                .setPreferPreviewSize(new Point(height, 720), false)
                .build();
        RxCamera.open(this, config)
                .flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
                    @Override
                    public Observable<RxCamera> call(RxCamera rxCamera) {
                        return rxCamera.bindTexture(binding.cameraTexture);
                    }
                }).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                return rxCamera.startPreview();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<RxCamera>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        RxAlertDialog.with(RecordActivity.this)
                                .title(R.string.alert_title_error)
                                .message(R.string.alert_msg_error_camera)
                                .positiveButton(R.string.button_ok)
                                .cancelable(false)
                                .subscribe(new Action1<Integer>() {
                                    @Override
                                    public void call(Integer integer) {
                                        finish();
                                    }
                                });
                    }

                    @Override
                    public void onNext(RxCamera rxCamera) {
                        camera = rxCamera;
                    }
                });

    }

    private void showPermissionRationale() {
        RxAlertDialog.with(this)
                .title(R.string.permission_camera_rationale_title)
                .message(R.string.permission_camera_rationale_msg)
                .positiveButton(R.string.button_ok)
                .create()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        getPermission();
                    }
                });
    }

    int countdown = MAX_RECORD;

    private TimerTask initTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.timout.setText(String.format("00:0%d", countdown));
                        if (countdown > 0) {
                            countdown--;
                        }
                    }
                });
            }
        };
    }

    private Timer timer;

    private void record(boolean record) {
        if (record) {
            ReelCountdownBinding countdownBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.reel_countdown, binding.layout, true);
            countdownBinding.countdown.setListener(this);
            countdownBinding.countdown.startCountdown();
            binding.hint.setVisibility(View.GONE);

        } else {
            View view = binding.layout.findViewById(R.id.countdown);
            if (view != null) {
                ((CountdownView) view).stopCountdown();
                binding.layout.removeView(view);
            }
            if (mediaRecorder == null) {
                binding.hint.setVisibility(View.VISIBLE);
            }
            stopRecording();
        }
    }

    @Override
    public void onCountdownFinished() {
        View view = binding.layout.findViewById(R.id.countdown);
        if (view != null) {
            binding.layout.removeView(view);
        }
        if (prepareRecorder()) {
            countdown = MAX_RECORD;

            try {
                mediaRecorder.start();
            } catch (Throwable t) {
                binding.buttonCapture.setActivated(false);
                releaseMediaRecorder();
                closeCamera();
                openCamera();
                return;
            }

            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer = null;
            }
            binding.overlay.setDrawArc(true);
            timer = new Timer();
            timer.scheduleAtFixedRate(initTimerTask(), 0, 1000);

            binding.timout.setVisibility(View.VISIBLE);
            binding.hint.setVisibility(View.GONE);

            binding.buttonFlip.setEnabled(false);
            binding.buttonFlip.setVisibility(View.INVISIBLE);

            showRecordingUi(true);
        }
    }

    private void showRecordingUi(boolean show) {
        binding.buttonCapture.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.buttonFlip.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.cameraTexture.setVisibility(show ? View.VISIBLE : View.GONE);

        binding.frame.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        binding.buttonCancel.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.buttonConfirm.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.overlay.setOverlayColor(getResources().getColor(show ? R.color.black54 : android.R.color.black));

        getSupportActionBar().setDisplayHomeAsUpEnabled(show);
    }

    private boolean prepareRecorder() {
        camera.getNativeCamera().unlock();
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }

        mediaRecorder.setCamera(camera.getNativeCamera());
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        try {
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
        } catch (Exception e) {
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        }
        mediaRecorder.setVideoEncodingBitRate(2000000);
        mediaRecorder.setAudioChannels(1);

        File file = getAppComponent().videoHelper().getFileForVideo(profile, question.id, VideoHelper.VideoSuffix.Temp);
        if (file.exists()) {
            file.delete();
        }
        mediaRecorder.setOutputFile(file.getPath());
        mediaRecorder.setMaxDuration(MAX_RECORD * 1000);

        int orientation = CameraUtil.getPortraitCameraDisplayOrientation(this, camera.getConfig().currentCameraId, camera.getConfig().isFaceCamera);
        if (camera.getConfig().isFaceCamera) {
            if (orientation == 90) {
                orientation = 270;
            } else if (orientation == 270) {
                orientation = 90;
            }
        }
        mediaRecorder.setOrientationHint(orientation);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecording();
                    binding.buttonCapture.setActivated(false);
                }
            }
        });

        try {
            mediaRecorder.prepare();
        } catch (Throwable e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    public void stopRecording() {
        if (mediaRecorder == null) {
            return;
        }
        try {
            mediaRecorder.stop();
        } catch (Exception e) {
            cancelRecording();
            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer = null;
            }
            binding.overlay.setDrawArc(false);
            binding.timout.setVisibility(View.GONE);
            releaseMediaRecorder();
            return;
        }
        releaseMediaRecorder();
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        binding.overlay.setDrawArc(false);
        binding.timout.setVisibility(View.GONE);
        binding.buttonFlip.setEnabled(true);
        binding.buttonFlip.setVisibility(View.VISIBLE);

        final File file = getAppComponent().videoHelper().getFileForVideo(profile, question.id, VideoHelper.VideoSuffix.Temp);
        if (file.exists()) {
            Observable.create(new Observable.OnSubscribe<Video>() {
                @Override
                public void call(Subscriber<? super Video> subscriber) {

                    metadataRetriever.setDataSource(RecordActivity.this, Uri.fromFile(file));

                    String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long duration = Long.parseLong(time);

                    subscriber.onNext(new Video(file.getPath(), duration, question));
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

                        }

                        @Override
                        public void onNext(Video video) {
                            RecordActivity.this.video = video;
                            ArrayList<Video> videos = new ArrayList<>(1);
                            videos.add(video);

                            if (player != null) {
                                player.destroy();
                            }

                            showRecordingUi(false);
                            player = ReelPlayer.with(RecordActivity.this, -1)
                                    .audio(true)
                                    .binding(binding)
                                    .videoOnly(true)
                                    .videos(videos)
                                    .skipCache(true)
                                    .profile(profile)
                                    .create();
                            player.setup();
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_CROP) {
            if (resultCode == RESULT_OK) {
                Intent intent = getIntent();
                intent.putExtra(EXTRA_VIDEO, data.getParcelableExtra(CropVideoActivity.EXTRA_VIDEO));
                setResult(RESULT_OK, intent);
                finish();
            }
            return;
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {

            try {
                mediaRecorder.stop();
            } catch (Throwable t) {
                //noinspection ResultOfMethodCallIgnored
                t.printStackTrace();
            }

            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;

            camera.getNativeCamera().lock();
        }
    }

    private void closeCamera() {
        if (camera != null && camera.isOpenCamera()) {
            try {
                camera.getNativeCamera().lock();
            } catch (Throwable ignored) {
            }
            camera.closeCamera();
            camera = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (video != null) {
            RxAlertDialog.with(this)
                    .title(R.string.alert_title_cancel_recording)
                    .message(R.string.alert_msg_cancel_recording)
                    .positiveButton(R.string.button_yes)
                    .negativeButton(R.string.button_no)
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            if (integer == RxAlertDialog.ButtonPositive) {
                                cancelRecording();
                                finish();
                            }
                        }
                    });
            return;
        }
        stopRecording();
        cancelRecording();
        super.onBackPressed();
    }


}
