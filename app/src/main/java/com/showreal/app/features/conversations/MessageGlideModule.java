package com.showreal.app.features.conversations;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.module.GlideModule;
import com.showreal.app.data.model.Message;
import com.showreal.app.features.real.myreal.ShowRealVideo;
import com.showreal.app.features.real.myreal.VideoThumbailLoader;

import java.io.InputStream;

public class MessageGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(Message.class, InputStream.class, new MessageVideoThumbailLoader.Factory());
        glide.register(VideoThumbailLoader.VideoThumbnail.class, InputStream.class, new VideoThumbailLoader.Factory());
    }
}
