package com.showreal.app.features.conversations.unmatch;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.Message;

import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class UnmatchUploader implements DataUploader<Match, Void> {

    private final SendBirdHelper sendbird;
    private Match match;
    private final ShowRealApi api;

    public UnmatchUploader(Context context) {
        this.api = TheDistanceApplication.getApplicationComponent(context).api();
        this.sendbird = TheDistanceApplication.getApplicationComponent(context).sendbird();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(Match content) {
        this.match = content;
    }

    @Override
    public Observable<Void> getUpload() {
        return api.deleteMatch(match.id)
                .map(new Func1<Void, Void>() {
                    @Override
                    public Void call(Void aVoid) {
                        cupboard().withDatabase(sendbird.getDatabase())
                                .delete(Message.class, "channel = ?", match.conversationUrl);
                        return null;
                    }
                });
    }
}
