package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.WorkerThread;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.DatabaseHelper;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.DeviceReel;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;
import com.showreal.app.data.model.VideoUpload;
import com.showreal.app.injection.ApplicationComponent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivecache.Provider;
import nl.nl2312.rxcupboard.RxCupboard;
import nl.nl2312.rxcupboard.RxDatabase;
import nl.qbusict.cupboard.DatabaseCompartment;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class ReelHelper {

    final Context context;
    private final Provider<Profile> profileProvider;
    private final Provider<Profile> staleProfileProvider;
    private final VideoHelper videoHelper;
    private SQLiteDatabase database;

    public ReelHelper(Context context) {
        this.context = context;
        ApplicationComponent component = TheDistanceApplication.getApplicationComponent(context);
        profileProvider = component.profileProvider();
        staleProfileProvider = component.staleProfileProvider();
        videoHelper = component.videoHelper();
    }

    public RxDatabase getCupboard() {
        if (database == null || !database.isOpen()) {
            database = DatabaseHelper.getConnection(context);
        }

        return RxCupboard.withDefault(database);
    }

    public Observable<DeviceReel> getReel() {
        return getProfile()
                .map(new Func1<Profile, DeviceReel>() {
                    @Override
                    public DeviceReel call(Profile profile) {
                        return getReelInternal(profile);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<List<Video>> getVideos() {
        return getCupboard().query(Video.class, "deleted = ?", "0")
                .map(new Func1<Video, Video>() {
                    @Override
                    public Video call(Video video) {
                        video.question = getCupboard().buildQuery(Question.class).byId(video._id).get();
                        return video;
                    }
                })
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<DeviceReel> deleteVideo(final Video video) {
        DatabaseCompartment.QueryBuilder<Video> queryBuilder = getCupboard().buildQuery(Video.class).byId(video._id);
        return getCupboard().query(queryBuilder)
                .map(new Func1<Video, Boolean>() {
                    @Override
                    public Boolean call(Video video) {

                        if (video.id != 0) {
                            video.deleted = true;
                            video.reelId = 0;
                            getCupboard().put(video);

                            return true;
                        }

                        getCupboard().delete(Question.class, video._id);
                        return getCupboard().delete(video);
                    }
                }).flatMap(new Func1<Boolean, Observable<DeviceReel>>() {
                    @Override
                    public Observable<DeviceReel> call(Boolean success) {
                        if (success) {
                            return removeVideoFiles(video)
                                    .flatMap(new Func1<Void, Observable<DeviceReel>>() {
                                        @Override
                                        public Observable<DeviceReel> call(Void aVoid) {
                                            return getReel();
                                        }
                                    });
                        }
                        return getReel();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Void> removeVideoFiles(final Video video) {
        return getProfile()
                .flatMap(new Func1<Profile, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Profile profile) {
                        VideoHelper videoHelper = TheDistanceApplication.getApplicationComponent(context).videoHelper();
                        return videoHelper.deleteFilesDorVideo(profile, video);
                    }
                }).subscribeOn(Schedulers.io());
    }

    public Observable<List<Video>> addVideo(final Video video, final Profile profile) {

        return Observable.defer(new Func0<Observable<List<Video>>>() {
            @Override
            public Observable<List<Video>> call() {
                Question oldQuestion = getCupboard().buildQuery(Question.class).withSelection("id = ?", String.valueOf(video.question.id)).get();
                if (oldQuestion != null) {
                    Video oldVideo = getCupboard().buildQuery(Video.class).byId(oldQuestion._id).get();
                    if (oldVideo != null) {
                        getCupboard().delete(oldVideo);
                    }
                    getCupboard().delete(oldQuestion);
                }

                video.published = false;
                video.dummyVideo = profile.isDummy();
                video.question.dummyQuestion = profile.isDummy();

                long id = getCupboard().put(video);
                video.question._id = id;
                getCupboard().put(video.question);

                return getVideos();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<DeviceReel> addVideoToReel(final Video video) {
        return getReel()
                .map(new Func1<DeviceReel, Observable<Video>>() {
                    @Override
                    public Observable<Video> call(DeviceReel reel) {

                        Video latest = getCupboard().buildQuery(Video.class).byId(video._id).get();
                        Question question = getCupboard().buildQuery(Question.class).byId(video._id).get();

                        question.index = video.question.index;
                        latest.published = false;
                        latest.modified = true;
                        latest.reelId = reel._id;

                        getCupboard().put(latest);
                        getCupboard().put(question);

                        return Observable.just(latest);
                    }
                })
                .flatMap(new Func1<Observable<Video>, Observable<DeviceReel>>() {
                    @Override
                    public Observable<DeviceReel> call(Observable<Video> videoObservable) {
                        return getReel();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    public Observable<DeviceReel> updateIndexes(final List<Video> videos) {
        return Observable.defer(new Func0<Observable<DeviceReel>>() {
            @Override
            public Observable<DeviceReel> call() {
                for (Video video : videos) {
                    Video latest = getCupboard().buildQuery(Video.class).byId(video._id).get();
                    Question question = getCupboard().buildQuery(Question.class).byId(video._id).get();

                    latest.modified = true;
                    question.index = video.question.index;

                    getCupboard().put(latest);
                    getCupboard().put(question);
                }

                return getReel();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @WorkerThread
    private DeviceReel getReelInternal(Profile profile) {
        DeviceReel reel = getCupboard().buildQuery(DeviceReel.class).get();
        if (reel == null) {
            reel = new DeviceReel();
            reel.videos = profile.videos;

            if (profile.id == Profile.DUMMY_ID) {
                reel.dummyReel = true;
            }

            getCupboard().put(reel);
            reel = getCupboard().buildQuery(DeviceReel.class).get();

            for (Video video : profile.videos) {

                video.reelId = reel._id;
                long id = getCupboard().put(video);

                video.question._id = id;
                getCupboard().put(video.question);
            }
        }

        reel.videos = getCupboard().buildQuery(Video.class).withSelection("reelId = ?", String.valueOf(reel._id)).list();
        for (Video video : reel.videos) {
            video.question = getCupboard().buildQuery(Question.class).byId(video._id).get();
        }

        Collections.sort(reel.videos);

        closeDatabase();

        return reel;
    }

    @WorkerThread
    public DeviceReel getBasicReel() {
        DeviceReel reel = getCupboard().buildQuery(DeviceReel.class).withSelection("dummyReel = ?", String.valueOf(1)).get();
        if (reel != null) {
            reel.videos = getCupboard().buildQuery(Video.class).withSelection("reelId = ?", String.valueOf(reel._id)).list();
        }
        return reel;
    }

    private Observable<Profile> getProfile() {
        return profileProvider.read()
                .onErrorResumeNext(staleProfileProvider.read());
    }

    public void closeDatabase() {
        if (database == null) {
            return;
        }

        if (database.isOpen()) {
            database.close();
        }
        database = null;
    }

    public void updateReel(Profile profile, List<VideoUpload> errors) {
        DeviceReel reel = getReelInternal(profile);

        List<Long> errorIds = new ArrayList<>(errors.size());
        List<Integer> errorQuestionIds = new ArrayList<>();

        for (VideoUpload error : errors) {
            errorIds.add(error.id);
            if (error.questionId != -1) {
                errorQuestionIds.add(error.questionId);
            }
        }

        for (Video video : reel.videos) {
            if (errorIds.contains(video._id)) {
                continue;
            }
            getCupboard().delete(video.question);
            getCupboard().delete(video);
        }

        for (Video video : profile.videos) {
            if (errorQuestionIds.contains(video.question.id)) {
                continue;
            }
            video.reelId = reel._id;
            long id = getCupboard().put(video);

            video.question._id = id;
            getCupboard().put(video.question);
        }
    }

    public Observable<Boolean> removeVideo(final Video video) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Video latest = getCupboard().buildQuery(Video.class).byId(video._id).get();
                if (latest != null) {
                    latest.reelId = 0;
                    getCupboard().put(latest);
                }
                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @WorkerThread
    public void migrateReel(Profile profile) {
        DeviceReel reel = getBasicReel();
        if (reel == null) {
            return;
        }

        for (Video video : reel.videos) {
            File dummyFile = new File(video.url);
            File newFile = getFile(dummyFile, profile);

            if (dummyFile.exists()) {
                try {
                    FileUtils.moveFile(dummyFile, newFile);
                    video.url = Uri.fromFile(newFile).getPath();
                    video.dummyVideo = false;
                    getCupboard().put(video);

                    Question question = getCupboard().buildQuery(Question.class).withSelection("id = ?", String.valueOf(video._id)).get();
                    if (question != null) {
                        question.dummyQuestion = false;
                        getCupboard().put(question);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        reel.dummyReel = false;

        getCupboard().put(reel);
    }

    private File getFile(File dummyFile, Profile profile) {
        String path = dummyFile.getPath();
        if (path.contains(String.valueOf(Profile.DUMMY_ID))) {
            path = path.replace(String.valueOf(Profile.DUMMY_ID), String.valueOf(profile.id));
        }

        return new File(path);
    }

    public void clean() {
        if (database == null || !database.isOpen()) {
            database = DatabaseHelper.getConnection(context);
        }

        DatabaseCompartment compartment = cupboard().withDatabase(database);
        compartment.delete(DeviceReel.class, null);
        compartment.delete(Video.class, null);
        compartment.delete(Question.class, null);
    }
}
