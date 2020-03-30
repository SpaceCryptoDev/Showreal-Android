package com.showreal.app;

import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.showreal.app.data.model.Message;
import com.showreal.app.databinding.MediaContentBinding;

public class Bindings {

    @BindingAdapter("underline")
    public static void applyUnderline(TextView textView, boolean underline) {
        if (textView == null) {
            return;
        }
        if (underline) {
            SpannableString spannableString = new SpannableString(textView.getText());
            spannableString.setSpan(new UnderlineSpan(), 0, textView.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textView.setText(spannableString);
        }
    }

    @BindingAdapter("imageUrl")
    public static void loadImage(ImageView imageView, String url) {
        if (imageView == null) {
            return;
        }

        imageView.setImageBitmap(null);
        imageView.setImageDrawable(null);

        if (url == null) {
            Glide.with(imageView.getContext())
                    .load(R.drawable.profile_no_image)
                    .asBitmap()
                    .centerCrop()
                    .into(imageView);
            return;
        }

        Glide.with(imageView.getContext())
                .load(Uri.parse(url))
                .asBitmap()
                .centerCrop()
                .into(imageView);
    }

    @BindingAdapter("unscaledImage")
    public static void loadUnscaledImage(ImageView imageView, String url) {
        if (imageView == null) {
            return;
        }

        imageView.setImageBitmap(null);
        imageView.setImageDrawable(null);

        if (url == null) {
            return;
        }

        Glide.with(imageView.getContext())
                .load(Uri.parse(url))
                .into(imageView);
    }

    @BindingAdapter("notificationImage")
    public static void loadNotificationImage(ImageView imageView, String url) {
        if (imageView == null) {
            return;
        }

        if (url == null) {
            Glide.with(imageView.getContext())
                    .load(R.drawable.showreal_profile)
                    .centerCrop()
                    .into(imageView);
            return;
        }

        Glide.with(imageView.getContext())
                .load(Uri.parse(url))
                .asBitmap()
                .centerCrop()
                .into(imageView);
    }

    public static void loadThumbnail(final MediaContentBinding binding, String url) {
        Glide.with(binding.image.getContext())
                .load(Uri.parse(url))
                .fitCenter()
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                        binding.progress.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        binding.progress.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(binding.image);
    }

    @BindingAdapter("thumbnail")
    public static void loadVideoThumbnail(final ImageView imageView, final Message message) {
        if (imageView == null) {
            return;
        }

        imageView.setImageBitmap(null);
        imageView.setImageDrawable(null);

        if (message == null) {
            return;
        }

        final MediaContentBinding binding = DataBindingUtil.findBinding(imageView);
        binding.progress.setVisibility(View.VISIBLE);
        binding.play.setVisibility(View.INVISIBLE);

        if (message.mediaType.startsWith("image")) {
            loadThumbnail(binding, message.mediaUrl);
            return;
        }

        Glide.with(imageView.getContext())
                .load(message)
                .fitCenter()
                .listener(new RequestListener<Message, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Message model, Target<GlideDrawable> target, boolean isFirstResource) {
                        binding.progress.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Message model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        binding.progress.setVisibility(View.GONE);
                        binding.play.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(imageView);
    }
}
