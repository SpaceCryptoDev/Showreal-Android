package com.showreal.app.features.settings.delete;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.util.TypedValue;
import android.view.View;

import com.showreal.app.R;

public class DeleteViewModel extends BaseObservable {

    final DeleteView deleteView;

    public DeleteViewModel(DeleteView deleteView) {
        this.deleteView = deleteView;
    }

    interface DeleteView {
        void deleteAccount();

        void cancel();

        Context getTextContext();
    }

    @Bindable
    public SpannableString getDeleteText() {
        String text = deleteView.getTextContext().getString(R.string.delete_text_two);
        SpannableString spannableString = new SpannableString(text);

        int gap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, deleteView.getTextContext().getResources().getDisplayMetrics());

        int first = text.indexOf("\n");
        int second = text.indexOf("\n", first + 1);

        spannableString.setSpan(new BulletSpan(gap, Color.BLACK), 0, first, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new BulletSpan(gap, Color.BLACK), first + 1, second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new BulletSpan(gap, Color.BLACK), second + 1, text.length() - 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

        return spannableString;
    }

    public void onDelete(View view) {
        deleteView.deleteAccount();
    }

    public void onCancel(View view) {
        deleteView.cancel();
    }
}
