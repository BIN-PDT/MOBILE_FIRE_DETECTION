package com.study.firedetection.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.study.firedetection.R;
import com.study.firedetection.entity.ShareItem;
import com.study.firedetection.utils.ConfirmUtils;

import java.util.ArrayList;
import java.util.List;

public class SharesRecyclerAdapter extends RecyclerView.Adapter<SharesRecyclerAdapter.ItemViewHolder>
        implements ConfirmUtils.IOnClickListener {
    public static final int MAX_SHARES = 3;
    private final Activity activity;
    private final List<ShareItem> originalData = new ArrayList<>();
    private final ConfirmUtils confirmUtils;
    private final String deviceId;
    private final TextView tvShareQuantity;

    public SharesRecyclerAdapter(Activity activity, String deviceId, TextView tvShareQuantity) {
        this.activity = activity;
        this.confirmUtils = new ConfirmUtils(activity, this);
        this.deviceId = deviceId;
        this.tvShareQuantity = tvShareQuantity;
    }

    public List<ShareItem> getOriginalData() {
        return originalData;
    }

    @SuppressLint({"NotifyDataSetChanged", "DefaultLocale"})
    public void loadOriginalData(List<ShareItem> data) {
        this.originalData.clear();
        this.originalData.addAll(data);
        notifyDataSetChanged();
        // DISPLAY TO TEXTVIEW.
        this.tvShareQuantity.setText(String.format("%d/%d", this.originalData.size(), MAX_SHARES));
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share, parent, false);
        return new ItemViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ShareItem item = this.originalData.get(position);

        holder.tvReceiver.setText(item.getUserID());
        holder.ivRemove.setOnClickListener(v -> {
            String message = ContextCompat.getString(activity, R.string.message_remove_user);
            this.confirmUtils.setMessage(String.format(message, item.getUserID()));
            this.confirmUtils.setDeviceId(this.deviceId);
            this.confirmUtils.setUserUID(item.getUserUID());
            this.confirmUtils.showConfirmDialog();
        });
    }

    @Override
    public int getItemCount() {
        return this.originalData.size();
    }

    @SuppressLint({"NotifyDataSetChanged", "DefaultLocale"})
    @Override
    public void onConfirm() {
        this.confirmUtils.confirmRemoveShare();
        // REMOVE ITEM DATA.
        this.originalData.removeIf(shareItem -> shareItem.getUserUID().equals(this.confirmUtils.getUserUID()));
        notifyDataSetChanged();
        // DISPLAY TO TEXTVIEW.
        this.tvShareQuantity.setText(String.format("%d/%d", this.originalData.size(), MAX_SHARES));
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvReceiver;
        private final ImageView ivRemove;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvReceiver = itemView.findViewById(R.id.tv_receiver);
            this.ivRemove = itemView.findViewById(R.id.iv_remove);
        }
    }
}
