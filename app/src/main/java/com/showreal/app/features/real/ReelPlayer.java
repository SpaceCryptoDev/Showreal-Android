package com.showreal.app.features.real;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.databinding.ViewDataBinding;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.sherazkhilji.videffects.NoEffect;
import com.sherazkhilji.videffects.interfaces.ShaderInterface;
import com.showreal.app.ExoVideoSurfaceView;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;
import com.showreal.app.data.model.VideoViewCount;
import com.showreal.app.features.real.myreal.BindingViewFinder;
import com.showreal.app.injection.ApplicationComponent;
import com.showreal.app.playpause.PlayPauseView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netopen.hotbitmapgg.library.view.RingProgressBar;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.thedistancecore.Version;
import uk.co.thedistance.thedistancecore.animation.AnimationHelper;

public class ReelPlayer {

    private final AccountHelper accountHelper;
    private boolean paused;

    public void clear() {
        surfaceView.setVisibility(View.INVISIBLE);
    }

    public boolean isFilterEnabled() {
        return applyFilter;
    }

    public static class CircleProperties {
        public float centerX;
        public float centerY;
        public float radius;
    }

    private final Context context;
    private final ExoVideoSurfaceView surfaceView;
    private final DefaultBandwidthMeter bandwidthMeter;
    final private SimpleExoPlayer exoPlayer;
    private final int FADE_DURATION;
    private final int QUESTION_DURATION;
    private final ReelView view;
    private final VideoDownloader videoDownloader;
    private final VideoHelper videoHelper;
    private final int reelId;
    private final ShowRealApi api;
    private int currentVideo;
    private List<Video> videos;
    private boolean animating;
    private boolean replay;
    private boolean pauseWhenReady;
    private AnimatorSet animators;
    private boolean started;
    private boolean videoOnly;
    private int startTime;
    private State state = State.Pause;
    private Profile profile;
    private boolean hasImage;
    private boolean useOriginal;
    private boolean skipCache;
    private ValueAnimator animator;
    private boolean reelViewed;
    private AnimatorSet imageAnimators;
    private final PowerManager powerManager;
    private final VideoEventListener eventListener;
    private boolean overrideFilter = false;
    private boolean applyFilter = false;
    private HttpProxyCacheServer cacheServer;

    private enum State {
        Play,
        Pause
    }

    public static class Builder {
        private final int reelId;
        private ReelView view;
        private Context context;
        private List<Video> videos;
        private boolean audio;
        private boolean promo;
        private boolean videoOnly;
        private Profile profile;
        private boolean useOriginal;
        private boolean skipCache;
        private boolean hasImage;

        Builder(Context context, int reelId) {
            this.context = context;
            this.reelId = reelId;
        }

        public Builder binding(ViewDataBinding binding) {
            this.view = new ReelView(binding, context);
            return this;
        }

        public Builder profile(Profile profile) {
            this.profile = profile;
            return this;
        }

        public Builder videos(List<Video> videos) {
            Collections.sort(videos);
            this.videos = videos;
            return this;
        }

        public Builder videoOnly(boolean only) {
            this.videoOnly = only;
            return this;
        }

        public Builder hasImage(boolean hasImage) {
            this.hasImage = hasImage;
            return this;
        }

        public Builder audio(boolean audioEnabled) {
            this.audio = audioEnabled;
            return this;
        }

        public Builder useOriginal(boolean useOriginal) {
            this.useOriginal = useOriginal;
            return this;
        }

        public Builder promo(boolean promo) {
            this.promo = promo;
            return this;
        }

        public Builder skipCache(boolean skip) {
            this.skipCache = skip;
            return this;
        }

        public ReelPlayer create() {
            ReelPlayer player = new ReelPlayer(context, view, videos, hasImage, reelId);
            if (!audio) {
                player.toggleMute();
            }
            player.setPromo(promo);
            player.setVideoOnly(videoOnly);
            player.setProfile(profile);
            player.setUseOriginal(useOriginal);
            player.setSkipCache(skipCache);
            return player;
        }
    }

    private static class ReelView {
        RingProgressBar progress;
        ProgressBar downloadProgress;
        FrameLayout frame;
        PlayPauseView buttonPlay;
        FloatingActionButton buttonMute;
        TextView question;
        View questionOverlay;
        ImageView profileImage;
        ImageView sponsorImage;

        ReelView(ViewDataBinding binding, Context context) {
            BindingViewFinder view = new BindingViewFinder(binding, context);
            progress = (RingProgressBar) view.findViewById(R.id.progress);
            frame = (FrameLayout) view.findViewById(R.id.frame);
            buttonMute = (FloatingActionButton) view.findViewById(R.id.button_mute);
            buttonPlay = (PlayPauseView) view.findViewById(R.id.button_play);
            question = (TextView) view.findViewById(R.id.question);
            questionOverlay = view.findViewById(R.id.question_overlay);
            downloadProgress = (ProgressBar) view.findViewById(R.id.download_progress);
            profileImage = (ImageView) view.findViewById(R.id.profile_real_image);
            sponsorImage = (ImageView) view.findViewById(R.id.sponsor_image);
        }
    }

    private class VideoEventListener implements ExoPlayer.EventListener {
        @Override
        public void onLoadingChanged(boolean isLoading) {
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (!playWhenReady) {
                showDownloadProgress(false);
            }
            if (playWhenReady) {
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    showDownloadProgress(true);
                    stopProgressTimer();
                } else if (playbackState == ExoPlayer.STATE_READY) {
                    showDownloadProgress(false);
                    if (animator == null) {
                        startProgressTimer();
                    }
                }
            }
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                exoPlayer.setPlayWhenReady(false);
                playNext();
            }
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
        }

        @Override
        public void onPositionDiscontinuity() {
        }
    }

    public static Builder with(Context context, int reelId) {
        return new Builder(context, reelId);
    }

    private ReelPlayer(Context context, ReelView view, List<Video> videos, boolean hasImage, int reelId) {
        this.context = context;
        this.view = view;
        this.videos = videos;
        this.reelId = reelId;
        this.hasImage = hasImage;
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.accountHelper = TheDistanceApplication.getApplicationComponent(context).accountHelper();

        ApplicationComponent component = TheDistanceApplication.getApplicationComponent(context);
        this.api = component.api();
        this.videoDownloader = component.videoDownloader();
        this.videoHelper = component.videoHelper();
        this.cacheServer = component.cacheServer();

        Resources resources = context.getResources();
        FADE_DURATION = resources.getInteger(R.integer.question_fade_duration);
        QUESTION_DURATION = resources.getInteger(R.integer.question_duration);

        if (view.questionOverlay != null) {
            Drawable drawable = view.questionOverlay.getBackground();
            if (!(drawable instanceof ColorDrawable) || ((ColorDrawable) drawable).getColor() != Color.BLACK) {
                ObjectAnimator colorFade = ObjectAnimator.ofObject(view.questionOverlay, "backgroundColor", new ArgbEvaluator(), Color.TRANSPARENT, Color.BLACK);
                colorFade.setDuration(200);
                colorFade.start();
            } else {
                view.question.setAlpha(1.0f);
                if (view.sponsorImage != null) {
                    view.sponsorImage.setAlpha(1.0f);
                }
            }

        }
        this.view.buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayback();
            }
        });
        this.view.frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayback();
            }
        });
        if (view.buttonMute != null) {
            this.view.buttonMute.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleMute();
                }
            });
        }

        if (view.profileImage != null) {
            AnimationHelper.fadeOut(view.profileImage, 200, 200);
        }

        Handler mainHandler = new Handler();
        bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl();

        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
        exoPlayer.addListener(eventListener = new VideoEventListener());

        surfaceView = new ExoVideoSurfaceView(context);
        surfaceView.setId(R.id.surface);
        exoPlayer.setVideoListener(surfaceView);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        view.frame.addView(surfaceView, 0, params);
        surfaceView.init(exoPlayer, new NoEffect());
        surfaceView.onResume();

        if (view.buttonPlay.isPlay()) {
            view.buttonPlay.toggle(false);
        }
    }

    private final float exposurePower = 0.5f;
    private final ShaderInterface exposureEffect = new ExposureEffect(exposurePower);

    public void setApplyFilter(boolean applyFilter) {
        overrideFilter = true;
        this.applyFilter = applyFilter;

        surfaceView.setEffect(applyFilter ? exposureEffect : null);
    }

    public void toggleFilter() {
        setApplyFilter(!applyFilter);
    }

    public void setStartTime(int seconds) {
        if (exoPlayer.getPlayWhenReady()) {
            pause();
        }
        exoPlayer.seekTo(seconds * 1000);
        startTime = seconds;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setPromo(boolean promo) {
//        surfaceView.setAdjust(promo);
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    private void setUseOriginal(boolean useOriginal) {
        this.useOriginal = useOriginal;
    }

    private void setSkipCache(boolean skipCache) {
        this.skipCache = skipCache;
    }


    public void setVideos(List<Video> videos) {
        this.videos = videos;
        setup();
    }

    private void setVideoOnly(boolean videoOnly) {
        this.videoOnly = videoOnly;
    }

    private void toggleMute() {
        if (view.buttonMute == null) {
            return;
        }
        if (exoPlayer.getVolume() == 0) {
            view.buttonMute.setImageResource(R.drawable.ic_sound_on);
            exoPlayer.setVolume(1);
        } else {
            exoPlayer.setVolume(0);
            view.buttonMute.setImageResource(R.drawable.ic_sound_off);
        }
    }

    public void pause() {
        if (exoPlayer.getPlayWhenReady()) {
            togglePlayback();
        } else if (animating) {
            if (Version.isKitKat()) {
                if (paused) {
                    return;
                }
                animators.pause();
                startHandler.removeCallbacksAndMessages(null);
                if (!view.buttonPlay.isPlay()) {
                    view.buttonPlay.toggle();
                }
                paused = true;
                stopProgressTimer();
                if (imageAnimators != null) {
                    imageAnimators.pause();
                }
            } else {
                pauseWhenReady = true;
                state = State.Pause;
            }
        }
    }

    public void ready() {
        pauseWhenReady = false;
        surfaceView.setVisibility(View.VISIBLE);
    }

    public void destroy() {
        startHandler.removeCallbacksAndMessages(null);
        stopProgressTimer();
        showDownloadProgress(false);

        surfaceView.onPause();
        exoPlayer.removeListener(eventListener);
        exoPlayer.clearVideoSurface();
        exoPlayer.stop();
        exoPlayer.release();

        if (view.progress != null) {
            view.progress.setProgress(0);
        }

        if (view.profileImage != null) {
            AnimationHelper.fadeIn(view.profileImage, 200, 0);
        }

        if (animators != null) {
            animators.removeAllListeners();
            animators.cancel();
        }

        if (imageAnimators != null) {
            imageAnimators.removeAllListeners();
            imageAnimators.cancel();
        }

        if (view.questionOverlay != null) {
            view.questionOverlay.setAlpha(1.0f);
            if (hasImage) {
                ObjectAnimator colorFade = ObjectAnimator.ofObject(view.questionOverlay, "backgroundColor", new ArgbEvaluator(), Color.BLACK, Color.TRANSPARENT);
                colorFade.setDuration(200);
                colorFade.start();
            }
        }
        if (view.buttonMute != null) {
            view.buttonMute.setImageResource(R.drawable.ic_sound_on);
        }

        if (view.question != null) {
            if (profile != null || hasImage) {
                view.question.setAlpha(0f);
            } else {
                view.question.setAlpha(1.0f);
            }
        }

        if (view.sponsorImage != null) {
            if (profile != null || hasImage) {
                view.sponsorImage.setAlpha(0f);
            } else {
                view.sponsorImage.setAlpha(1.0f);
            }
        }

        view.buttonPlay.hide(false);
        view.buttonPlay.setEnabled(true);

        view.frame.removeView(surfaceView);

        videos = null;
    }

    private void stopProgressTimer() {
        if (animator == null) {
            return;
        }
        animator.cancel();
        animator = null;
    }

    public boolean isAudioEnabled() {
        return exoPlayer.getVolume() == 1;
    }


    private void setupVideo(Video video) {
        initVideo(video);
    }

    private void showDownloadProgress(boolean show) {
        if (view.downloadProgress == null) {
            return;
        }
        view.downloadProgress.post(new VisibilityRunnable(view.downloadProgress, show));
    }

    private static class VisibilityRunnable implements Runnable {
        final View view;
        final boolean show;

        private VisibilityRunnable(View view, boolean show) {
            this.view = view;
            this.show = show;
        }

        @Override
        public void run() {
            if (view == null) {
                return;
            }
            view.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void initVideo(Video video) {
        if (overrideFilter) {
            surfaceView.setEffect(applyFilter ? exposureEffect : null);
        } else {
            surfaceView.setEffect(video.isLightFilterEnabled ? exposureEffect : null);
        }
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "ShowReal"), bandwidthMeter);

        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        Uri uri;
        File cache = videoDownloader.videoFile(video);
        if (!skipCache && !cache.exists() && profile != null) {
            if (useOriginal) {
                cache = videoHelper.getFileForVideo(profile, video.question.id, VideoHelper.VideoSuffix.Original);
            }
            if (!cache.exists()) {
                cache = videoDownloader.userVideoFile(video, profile);
            }
        }

        if (profile == null) {
            String url = cacheServer.getProxyUrl(video.url);
            uri = Uri.parse(url);

        } else {
            if (!skipCache && cache.exists()) {
                uri = Uri.fromFile(cache);
            } else if (video.url.startsWith("/")) {
                uri = Uri.parse("file://" + video.url);
            } else {
                uri = Uri.parse(video.url);
            }
        }
        MediaSource videoSource = new ExtractorMediaSource(uri,
                dataSourceFactory, extractorsFactory, null, null);

        surfaceView.setVideo(video);

        exoPlayer.setPlayWhenReady(false);
        exoPlayer.prepare(videoSource);
    }

    public void setup() {
        setup(0);
    }

    public void setup(int index) {
        currentVideo = index;

        if (videos.isEmpty()) {
            return;
        }

        setupVideo(videos.get(currentVideo));

        if (!videoOnly) {
            int totalTime = 0;
            for (int i = 0; i < videos.size(); i++) {
                Video video = videos.get(i);
                totalTime += video.duration * 1000;

                if (i == 0) {
                    totalTime += (QUESTION_DURATION + (3f * FADE_DURATION));
                } else {
                    totalTime += (QUESTION_DURATION + (4f * FADE_DURATION));
                }
            }

            view.progress.setMax(totalTime);

            if (index == 0 && !hasImage && profile == null) {
                view.question.setAlpha(1.0f);
                if (view.sponsorImage != null) {
                    view.sponsorImage.setAlpha(1.0f);
                }
            }

            final Question question = videos.get(currentVideo).question;

            view.question.setText(question.text);
            if (view.sponsorImage != null) {
                if (!TextUtils.isEmpty(question.reelImage)) {
                    Glide.with(view.sponsorImage.getContext())
                            .load(question.reelImage)
                            .fitCenter()
                            .into(view.sponsorImage);
                } else {
                    view.sponsorImage.setImageBitmap(null);
                }
            }

            if (currentVideo != 0) {
                int currentTime = 0;
                for (int i = 0; i < currentVideo; i++) {
                    Video video = videos.get(i);
                    currentTime += ((video.duration * 1000) + (QUESTION_DURATION + (2.8f * FADE_DURATION)));
                }
                stopProgressTimer();
                currentTime -= (2f * FADE_DURATION);
                if (!Version.isLollipop() || !powerManager.isPowerSaveMode()) {
                    animator = ValueAnimator.ofInt(view.progress.getProgress(), currentTime);
                    animator.setDuration(400);
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            int progress = (int) animation.getAnimatedValue();
                            view.progress.setProgress(progress);
                        }
                    });
                    animator.start();
                } else {
                    view.progress.setProgress(0);
                }
            }
        }

        if (!view.buttonPlay.isPlay()) {
            view.buttonPlay.toggle(false);
        }
        view.buttonPlay.show();
    }

    public void start(boolean showButton) {
        state = State.Play;
        if (started) {
            return;
        }
        if (!Version.isKitKat()) {
            view.buttonPlay.setEnabled(false);
        }

        if (videos.isEmpty()) {
            return;
        }

        view.buttonPlay.toggle();
        view.buttonPlay.hide(showButton);

        started = true;

        if (!videoOnly) {
            animating = true;

            startAnimations(false);
            startVideoWhenReady(true);
            if (exoPlayer.getPlaybackState() != ExoPlayer.STATE_BUFFERING) {
                startProgressTimer();
            }
        } else {
            view.buttonPlay.setEnabled(true);
            exoPlayer.setPlayWhenReady(true);
            view.buttonPlay.setEnabled(true);
        }
    }

    private final Handler startHandler = new Handler(Looper.getMainLooper());

    private void startVideoWhenReady(boolean first) {
        startVideoWhenReady((long) ((QUESTION_DURATION + ((first ? 3f : 4f) * FADE_DURATION)) - 200));
    }

    private void startVideoWhenReady(long duration) {
        startHandler.removeCallbacksAndMessages(null);
        startHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                animating = false;

                view.buttonPlay.setEnabled(true);
                exoPlayer.setPlayWhenReady(true);
                view.buttonPlay.setEnabled(true);

                if (pauseWhenReady) {
                    pauseWhenReady = false;
                    togglePlayback();
                }
            }
        }, duration);
    }

    private void startAnimations(boolean video) {
        if (Version.isLollipop() && powerManager.isPowerSaveMode()) {
            nonAnimated(video);
            return;
        }

        List<Animator> animatorList = new ArrayList<>(video ? 3 : 4);

        if (video) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view.questionOverlay, "alpha", 0, 1).setDuration(FADE_DURATION);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setupVideo(videos.get(currentVideo));
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorList.add(animator);
        }

        animatorList.add(ObjectAnimator.ofFloat(view.question, "alpha", view.question.getAlpha(), 1).setDuration(FADE_DURATION));
        ObjectAnimator questionOut = ObjectAnimator.ofFloat(view.question, "alpha", 1, 0).setDuration(FADE_DURATION);
        questionOut.setStartDelay(QUESTION_DURATION);
        animatorList.add(questionOut);
        animatorList.add(ObjectAnimator.ofFloat(view.questionOverlay, "alpha", 1, 0).setDuration(FADE_DURATION));

        animators = new AnimatorSet();
        animators.playSequentially(animatorList);

        animators.start();

        if (view.sponsorImage != null) {
            ObjectAnimator imageIn = ObjectAnimator.ofFloat(view.sponsorImage, "alpha", view.sponsorImage.getAlpha(), 1).setDuration(FADE_DURATION);
            ObjectAnimator imageOut = ObjectAnimator.ofFloat(view.sponsorImage, "alpha", 1, 0).setDuration(FADE_DURATION);
            if (video) {
                imageIn.setStartDelay(FADE_DURATION);
            }
            imageOut.setStartDelay(QUESTION_DURATION);
            imageAnimators = new AnimatorSet();
            imageAnimators.playSequentially(imageIn, imageOut);
            imageAnimators.start();
        }
    }

    private static class SimpleTransitionTransformer implements Observable.Transformer<Void, Void> {

        final View view;
        final boolean out;
        final int duration;
        final int delay;

        private SimpleTransitionTransformer(View view, boolean out, int duration, int delay) {
            this.view = view;
            this.out = out;
            this.duration = duration;
            this.delay = delay;
        }

        @Override
        public Observable<Void> call(Observable<Void> voidObservable) {
            return voidObservable.delay(delay, TimeUnit.MILLISECONDS)
                    .map(new Func1<Void, Void>() {
                        @Override
                        public Void call(Void aVoid) {
                            if (view != null) {
                                view.setAlpha(out ? 0f : 1f);
                            }
                            return null;
                        }
                    }).delay(duration, TimeUnit.MILLISECONDS);
        }
    }

    private void nonAnimated(boolean video) {

        Observable<Void> transitions = Observable.just(null);

        if (video) {
            view.questionOverlay.setAlpha(1f);

            transitions = transitions.compose(new SimpleTransitionTransformer(view.questionOverlay, false, FADE_DURATION, 0))
                    .map(new Func1<Void, Void>() {
                        @Override
                        public Void call(Void aVoid) {
                            setupVideo(videos.get(currentVideo));
                            return null;
                        }
                    });
        }

        transitions.compose(new SimpleTransitionTransformer(view.question, false, FADE_DURATION, 0))
                .compose(new SimpleTransitionTransformer(view.question, true, FADE_DURATION, QUESTION_DURATION))
                .compose(new SimpleTransitionTransformer(view.questionOverlay, true, FADE_DURATION, 0))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();

        if (view.sponsorImage != null) {
            Observable<Void> image = Observable.just(null);

            image.compose(new SimpleTransitionTransformer(view.sponsorImage, false, FADE_DURATION, video ? FADE_DURATION : 0))
                    .compose(new SimpleTransitionTransformer(view.sponsorImage, true, FADE_DURATION, QUESTION_DURATION))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
    }

    private void playNext() {
        if (currentVideo < videos.size() - 1) {

            currentVideo++;
            doAnimations();

        } else {
            currentVideo = 0;
            if (!videoOnly) {
                stopProgressTimer();
                view.progress.setProgress(0);
                startProgressTimer();
                doAnimations();
            } else {
                view.buttonPlay.toggle();
                view.buttonPlay.show();
                view.buttonPlay.setEnabled(true);
                setupVideo(videos.get(0));
            }
        }
    }

    private void startProgressTimer() {
        if (Version.isLollipop() && powerManager.isPowerSaveMode()) {
            view.progress.setProgress(0);
            return;
        }
        if (animator != null && animator.isRunning()) {
            return;
        }

        animator = ValueAnimator.ofInt(view.progress.getProgress(), view.progress.getMax());
        animator.setDuration(view.progress.getMax() - view.progress.getProgress());
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int progress = (int) animation.getAnimatedValue();
                view.progress.setProgress(progress);

                if (!reelViewed && profile == null && progress > (view.progress.getMax() / 2)) {
                    reelViewed();
                }
            }
        });
        animator.start();
    }

    private void reelViewed() {
        reelViewed = true;
        if (!accountHelper.isLoggedIn()) {
            return;
        }
        if (reelId == -1) {
            return;
        }

        api.videoWatched(reelId)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<VideoViewCount>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        reelViewed = false;
                    }

                    @Override
                    public void onNext(VideoViewCount videoViewCount) {

                    }
                });
    }

    private void doAnimations() {
        if (!videoOnly) {
            if (!Version.isKitKat()) {
                view.buttonPlay.setEnabled(false);
            }
            final Question question = videos.get(currentVideo).question;

            animating = true;
            view.question.setText(question.text);
            if (view.sponsorImage != null) {
                if (!TextUtils.isEmpty(question.reelImage)) {
                    Glide.with(view.sponsorImage.getContext())
                            .load(question.reelImage)
                            .fitCenter()
                            .into(view.sponsorImage);
                } else {
                    view.sponsorImage.setImageBitmap(null);
                }
            }

            startAnimations(true);
            startVideoWhenReady(false);

        } else {
            setupVideo(videos.get(currentVideo));
            view.buttonPlay.setEnabled(true);
            exoPlayer.setPlayWhenReady(true);
            view.buttonPlay.setEnabled(true);

            if (pauseWhenReady) {
                pauseWhenReady = false;
                togglePlayback();
            }
        }
    }

    private void togglePlayback() {
        if (videos == null) {
            return;
        }
        if (!view.buttonPlay.isEnabled()) {
            return;
        }

        if (!started) {
            start(true);
            return;
        }

        view.buttonPlay.toggle();

        if (exoPlayer.getPlayWhenReady()) {
            view.buttonPlay.show();
            exoPlayer.setPlayWhenReady(false);
            stopProgressTimer();
        } else {
            if (Version.isKitKat()) {
                if (animating && !paused) {
                    animators.pause();
                    startHandler.removeCallbacksAndMessages(null);
                    paused = true;
                    stopProgressTimer();

                    if (imageAnimators != null) {
                        imageAnimators.pause();
                    }

                    return;
                } else if (paused) {
                    long timeRemaining = 0;
                    for (Animator animator : animators.getChildAnimations()) {
                        ObjectAnimator objectAnimator = (ObjectAnimator) animator;
                        if (objectAnimator.getCurrentPlayTime() < animator.getDuration()) {
                            timeRemaining += (objectAnimator.getDuration() - objectAnimator.getCurrentPlayTime());
                        }
                    }

                    startVideoWhenReady(timeRemaining - 200);
                    animators.resume();
                    view.buttonPlay.hideAnimated(400);
                    paused = false;

                    if (exoPlayer.getPlaybackState() != ExoPlayer.STATE_BUFFERING) {
                        startProgressTimer();
                    }

                    if (imageAnimators != null) {
                        imageAnimators.resume();
                    }
                    return;
                }
            }

            if (replay) {
                replay = false;
                doAnimations();
            } else {
                exoPlayer.setPlayWhenReady(true);
                if (!videoOnly && exoPlayer.getPlaybackState() != ExoPlayer.STATE_BUFFERING) {
                    startProgressTimer();
                }
            }
            view.buttonPlay.hideAnimated(400);
        }
    }

    public CircleProperties getCircleProperties() {
        CircleProperties properties = new CircleProperties();
        properties.radius = surfaceView.getWidth() / 2f;
        properties.centerX = surfaceView.getX() + properties.radius;
        properties.centerY = surfaceView.getY() + properties.radius;

        return properties;
    }
}
