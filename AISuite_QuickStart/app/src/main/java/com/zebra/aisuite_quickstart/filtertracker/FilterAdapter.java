// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.filtertracker;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zebra.aisuite_quickstart.R;

import java.util.List;
import java.util.stream.Collectors;

public class FilterAdapter extends  RecyclerView.Adapter<FilterAdapter.ViewHolder> {

    private final List<FilterItem> filterList;

    public FilterAdapter(List<FilterItem> list) {
        filterList = list;
        Log.d("FilterAdapter", "Filter List " + list.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CheckedTextView textView; // Example
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.checked_text_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.filter_item_view, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FilterItem itemData = filterList.get(position);
        holder.textView.setText(itemData.getTitle());
        holder.textView.setChecked(itemData.isChecked());
        holder.textView.setOnClickListener(v -> {
            boolean newCheckedState = !itemData.isChecked();
            itemData.setChecked(newCheckedState);
            notifyDataSetChanged(); // Refresh all items
        });
    }

    @Override
    public int getItemCount() {
        Log.d("FilterAdapter", "Filter " + filterList.size());

        return filterList.size();
    }

    public List<FilterItem> getFilteredList() {

        return filterList;
    }
}
