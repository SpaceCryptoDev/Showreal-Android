package com.showreal.app.injection;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.FileNameGenerator;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.features.real.VideoDownloader;
import com.showreal.app.features.real.myreal.ReelHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class VideoModule {

    @Provides
    @Singleton
    VideoHelper provideVideoHelper(TheDistanceApplication application) {
        return new VideoHelper(application);
    }

    @Provides
    @Singleton
    ReelHelper provideReelHelper(TheDistanceApplication application) {
        return new ReelHelper(application);
    }

    @Provides
    @Singleton
    VideoDownloader provideVideoDownloader(TheDistanceApplication application, VideoHelper videoHelper, ShowRealApi api) {
        return new VideoDownloader(application, videoHelper, api);
    }

    @Provides
    @Singleton
    HttpProxyCacheServer provideCacheServer(TheDistanceApplication application, VideoHelper videoHelper) {
        return new HttpProxyCacheServer.Builder(application)
                .cacheDirectory(videoHelper.getVideoCacheDir())
                .fileNameGenerator(videoHelper.getFileNameGenerator())
                .build();
    }
}
