package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.text.TextUtils;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.DeviceReel;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.QuestionsResponse;
import com.showreal.app.data.model.Video;
import com.showreal.app.injection.ApplicationComponent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivecache.Provider;
import io.reactivecache.ReactiveCache;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import uk.co.thedistance.components.contentloading.DataSource;

public class VideoDataSource implements DataSource<ReelData> {

    private final ReelHelper reelHelper;
    private final ApplicationComponent component;
    Profile profile;
    private final ShowRealApi api;
    private final Provider<QuestionsResponse> provider;
    private final List<Integer> answered = new ArrayList<>();

    public VideoDataSource(Context context, Profile profile) {
        component = TheDistanceApplication.getApplicationComponent(context);
        api = component.api();
        reelHelper = component.reelHelper();
        this.profile = profile;

        ReactiveCache cache = component.cache();
        provider = cache.<QuestionsResponse>provider().lifeCache(30, TimeUnit.MINUTES).withKey("questions");
        for (Video video : profile.videos) {
            answered.add(video.question.id);
        }
    }

    @Override
    public void reset() {

    }

    private Calendar now;
    private Calendar expiry = Calendar.getInstance();

    @Override
    public Observable<ReelData> getData() {

        return Observable.zip(getQuestions(), reelHelper.getReel(), new Func2<List<Question>, DeviceReel, ReelData>() {
            @Override
            public ReelData call(List<Question> questions, DeviceReel reel) {
                return new ReelData(questions, reel);
            }
        }).flatMap(new Func1<ReelData, Observable<ReelData>>() {
            @Override
            public Observable<ReelData> call(ReelData reelData) {
                return Observable.zip(Observable.just(reelData), reelHelper.getVideos(), new Func2<ReelData, List<Video>, ReelData>() {
                    @Override
                    public ReelData call(ReelData reelData, List<Video> videos) {
                        reelData.videos = videos;
                        return reelData;
                    }
                });
            }
        });
    }

    private Observable<QuestionsResponse> questionsObservable() {
        return (profile.isDummy() ? api.getQuestions() : api.getMyQuestions().onErrorResumeNext(api.getQuestions()));
    }

    private Observable<List<Question>> getQuestions() {
        final Profile.Region region = Profile.getRegion(component.appContext());
        return questionsObservable()
                .compose(provider.readWithLoader())
                .flatMap(new Func1<QuestionsResponse, Observable<QuestionsResponse>>() {
                    @Override
                    public Observable<QuestionsResponse> call(QuestionsResponse response) {
                        return response == null || response.questions == null ? questionsObservable().compose(provider.replace()) : Observable.just(response);
                    }
                })
                .flatMapIterable(new Func1<QuestionsResponse, Iterable<Question>>() {
                    @Override
                    public Iterable<Question> call(QuestionsResponse response) {
                        now = Calendar.getInstance();
                        return response.questions;
                    }
                }).map(new Func1<Question, Question>() {
                    @Override
                    public Question call(Question question) {
                        if (TextUtils.isEmpty(question.textAUS) && TextUtils.isEmpty(question.textUS)) {
                            return question;
                        }
                        switch (region) {
                            case US:
                                question.text = question.textUS;
                                break;
                            case AUS:
                                question.text = question.textAUS;
                                break;
                        }
                        return question;
                    }
                }).filter(new Func1<Question, Boolean>() {
                    @Override
                    public Boolean call(Question question) {
                        if (TextUtils.isEmpty(question.text)) {
                            return false;
                        }
                        if (question.questionType == 1 && question.expiryDate != null) {
                            expiry.setTime(question.expiryDate);
                            if (expiry.before(now)) {
                                return false;
                            }
                        }
                        return question.isActive;
                    }
                }).toList();
    }
}
