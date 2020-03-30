package com.showreal.app.features.conversations.unmatch;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import com.showreal.app.R;
import com.showreal.app.data.model.Match;


public class UnmatchViewModel extends BaseObservable {

    interface UnmatchView {
        void unmatch(Match match);

        void cancel();

        Context getTextContext();
    }

    private final Match match;
    private final UnmatchView unmatchView;

    public UnmatchViewModel(Match match, UnmatchView unmatchView) {
        this.match = match;
        this.unmatchView = unmatchView;
    }

    @Bindable
    public String getImage() {
        return match.profile.image;
    }

    @Bindable
    public String getName() {
        return match.profile.firstName;
    }

    @Bindable
    public String getMessage() {
        String pronoun = match.profile.gender == 0 ? "He" : "She";
        return unmatchView.getTextContext().getResources().getString(R.string.unmatch_message, pronoun, pronoun.toLowerCase());
    }

    @Bindable
    public String getQuestion() {
        return unmatchView.getTextContext().getResources().getString(R.string.unmatch_question, match.profile.firstName);
    }

    public void onUnmatch(View view) {
        unmatchView.unmatch(match);
    }

    public void onCancel(View view) {
        unmatchView.cancel();
    }
}
