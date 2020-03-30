package com.showreal.app.features.potential;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

import com.showreal.app.databinding.ItemPotentialBinding;

class PotentialClickListener implements RecyclerView.OnItemTouchListener {

    interface OnItemClickListener {

        boolean onItemClick(PotentialAdapter.MatchViewHolder holder, MotionEvent event);
    }

    private long swipeThreshhold = 0;
    private final OnItemClickListener listener;

    public PotentialClickListener(Context context, OnItemClickListener listener) {
        this.listener = listener;
    }

    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
        View childView = rv.findChildViewUnder(event.getX(), event.getY());
        ItemPotentialBinding binding = DataBindingUtil.getBinding(childView);
        if (binding == null) {
            return false;
        }

        if (swipeThreshhold == 0) {
            swipeThreshhold = binding.buttons.getWidth();
        }

        float pos = Math.abs(binding.content.getX());
        if (pos >= swipeThreshhold) {
            return true;
        }

        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent event) {
        View childView = rv.findChildViewUnder(event.getX(), event.getY());

        if (event.getAction() == MotionEvent.ACTION_UP) {
            listener.onItemClick((PotentialAdapter.MatchViewHolder) rv.getChildViewHolder(childView), event);
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
