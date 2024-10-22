package com.study.firedetection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.study.firedetection.DeviceActivity;
import com.study.firedetection.R;
import com.study.firedetection.entity.DeviceItem;

import java.util.ArrayList;
import java.util.List;

public class DevicesRecyclerAdapter extends RecyclerView.Adapter<DevicesRecyclerAdapter.ItemViewHolder> {
    private final Context mContext;
    private final List<DeviceItem> originalData = new ArrayList<>();

    public DevicesRecyclerAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public List<DeviceItem> getOriginalData() {
        return originalData;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadOriginalData(List<DeviceItem> data) {
        this.originalData.clear();
        this.originalData.addAll(data);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addNewItem(DeviceItem item) {
        this.originalData.add(item);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        DeviceItem item = this.originalData.get(position);

        String devicePath = String.format("devices/%s", item.getId());
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference deviceRef = database.getReference(devicePath);
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                holder.loadingStatus.setVisibility(View.VISIBLE);
                DeviceItem updatedItem = snapshot.getValue(DeviceItem.class);
                if (updatedItem != null) {
                    item.setName(updatedItem.getName());
                    item.setOnline(updatedItem.isOnline());
                    item.setDetect(updatedItem.isDetect());

                    holder.tvName.setText(item.getName());
                    int onlineId = item.isOnline() ? R.drawable.icon_online : R.drawable.icon_offline;
                    holder.ivState.setImageDrawable(ContextCompat.getDrawable(mContext, onlineId));
                    if (item.isOnline()) {
                        int detectId = item.isDetect() ? R.drawable.icon_fire : R.drawable.icon_none;
                        holder.ivDetect.setImageDrawable(ContextCompat.getDrawable(mContext, detectId));
                        int statusId = item.isDetect() ? R.drawable.bg_fire : R.drawable.bg_none;
                        holder.layoutMain.setBackground(ContextCompat.getDrawable(mContext, statusId));
                    } else {
                        holder.ivDetect.setImageDrawable(null);
                        holder.layoutMain.setBackground(ContextCompat.getDrawable(mContext, R.drawable.bg_offline));
                    }

                    holder.loadingStatus.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        holder.tvName.setOnClickListener(v -> {
            Intent intent = new Intent(this.mContext, DeviceActivity.class);
            intent.putExtra("deviceId", item.getId());
            intent.putExtra("deviceName", item.getName());
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return this.originalData.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ProgressBar loadingStatus;
        private final TextView tvName;
        private final ImageView ivState, ivDetect;
        private final RelativeLayout layoutMain;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.loadingStatus = itemView.findViewById(R.id.loading_view);
            this.tvName = itemView.findViewById(R.id.tv_name);
            this.ivState = itemView.findViewById(R.id.iv_online);
            this.ivDetect = itemView.findViewById(R.id.iv_detect);
            this.layoutMain = itemView.findViewById(R.id.layout_main);
        }
    }
}
