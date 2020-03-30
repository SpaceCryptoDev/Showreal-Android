package com.showreal.app.features.conversations.matches;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;


public class MatchesViewModel extends BaseObservable {

    @Bindable
    public SpannableString getEmptyText() {
        String first = "No matches yet!";
        SpannableString spannableString = new SpannableString(first + "\n\nOnce someone that you like Keeps you they will appear in this section so you can start talking.");
        spannableString.setSpan(new AbsoluteSizeSpan(34, true), 0, first.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        return spannableString;
    }
}
