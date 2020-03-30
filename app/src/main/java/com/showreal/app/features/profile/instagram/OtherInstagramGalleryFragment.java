package com.showreal.app.features.profile.instagram;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.data.model.Photo;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.FragmentInstagramBinding;

import java.util.ArrayList;
import java.util.List;


public class OtherInstagramGalleryFragment extends BaseFragment implements InstagramViewModel.InstagramView {

    private FragmentInstagramBinding binding;
    private InstagramMedia media;
    private Profile profile;
    private InstagramAdapter adapter;

    public static OtherInstagramGalleryFragment newInstance(Profile profile) {

        Bundle args = new Bundle();
        args.putParcelable("profile", profile);
        OtherInstagramGalleryFragment fragment = new OtherInstagramGalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.profile = getArguments().getParcelable("profile");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_instagram, container, false);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItems().get(position) instanceof InstagramAdapter.LoadingObject ? 2 : 1;
            }
        });

        binding.gallery.setLayoutManager(layoutManager);
        binding.gallery.setAdapter(adapter = new InstagramAdapter(getActivity(), this));

        showContent(profile);

        return binding.getRoot();
    }

    public void showContent(Profile profile) {
        List<Photo> photos = profile.photos;
        InstagramMedia media = new InstagramMedia();
        media.images = new ArrayList<>(photos.size());
        for (Photo photo : photos) {
            media.images.add(new InstagramMedia.Image(photo));
        }
        this.media = media;

        adapter.addItems(media.images);
    }

    @Override
    public void openImage(View image) {
        PhotoActivity.startWith(getActivity(), media.images, image);
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
        showContent(profile);
    }
}
