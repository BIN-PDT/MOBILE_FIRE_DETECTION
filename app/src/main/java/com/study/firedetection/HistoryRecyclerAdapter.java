package com.study.firedetection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class HistoryRecyclerAdapter extends RecyclerView.Adapter<HistoryRecyclerAdapter.ItemViewHolder> {
    private final Context mContext;
    private List<HistoryItem> data;

    public HistoryRecyclerAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<HistoryItem> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        HistoryItem item = this.data.get(position);
        holder.tvTimestamp.setText(item.getTimestamp());
        for (int i = 0; i < item.getListCaptureUrl().size(); i++) {
            String captureURL = item.getListCaptureUrl().get(i);
            ImageView captureContainer = holder.LIST_CONTAINER.get(i);

            Glide.with(this.mContext)
                    .load(captureURL)
                    .apply(new RequestOptions().transform(new RoundedCorners(15)))
                    .into(captureContainer);

            captureContainer.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri imageUri = Uri.parse(captureURL);
                intent.setDataAndType(imageUri, "image/*");
                if (intent.resolveActivity(this.mContext.getPackageManager()) != null) {
                    this.mContext.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (data != null) {
            return data.size();
        }
        return 0;
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTimestamp;
        private final List<ImageView> LIST_CONTAINER = new ArrayList<>(3);

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            this.LIST_CONTAINER.add(itemView.findViewById(R.id.iv_capture_1));
            this.LIST_CONTAINER.add(itemView.findViewById(R.id.iv_capture_2));
            this.LIST_CONTAINER.add(itemView.findViewById(R.id.iv_capture_3));
        }
    }
}
