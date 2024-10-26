package com.study.firedetection.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;
import com.study.firedetection.entity.DeviceItem;
import com.study.firedetection.utils.ConfirmUtils;
import com.study.firedetection.utils.DeviceUtils;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevicesRecyclerAdapter extends RecyclerView.Adapter<DevicesRecyclerAdapter.ItemViewHolder>
        implements ConfirmUtils.IOnClickListener {
    private final Map<String, SimpleEntry<DatabaseReference, ValueEventListener>> DEVICE_EVENTS = new HashMap<>();
    private final Context mContext;
    private final List<DeviceItem> originalData = new ArrayList<>();
    private final DeviceUtils deviceUtils;
    private final ConfirmUtils confirmUtils;

    public DevicesRecyclerAdapter(Context context, Activity activity) {
        this.mContext = context;
        this.deviceUtils = new DeviceUtils(activity);
        this.confirmUtils = new ConfirmUtils(activity, this);
    }

    public List<DeviceItem> getOriginalData() {
        return originalData;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadOriginalData(List<DeviceItem> data) {
        this.DEVICE_EVENTS.values().forEach(entry -> entry.getKey().removeEventListener(entry.getValue()));
        this.DEVICE_EVENTS.clear();

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
        String deviceId = item.getId();

        holder.tvName.setOnClickListener(v -> {
            Intent intent = new Intent(this.mContext, DeviceActivity.class);
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("deviceName", item.getName());
            this.mContext.startActivity(intent);
        });
        // FIREBASE EVENT.
        String devicePath = String.format("devices/%s", deviceId);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference deviceRef = database.getReference(devicePath);
        ValueEventListener deviceListener = deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // ENABLE LOADING.
                holder.loadingStatus.setVisibility(View.VISIBLE);
                DeviceItem updatedItem = snapshot.getValue(DeviceItem.class);
                if (updatedItem != null) {
                    // UPDATE DEVICE INFORMATION.
                    item.setName(updatedItem.getName());
                    item.setOnline(updatedItem.isOnline());
                    item.setDetect(updatedItem.isDetect());
                    // INFO LAYOUT.
                    holder.loadInfoLayout(mContext, item);
                    // TOOL LAYOUT.
                    Boolean isOwner = snapshot.child("users").child(HomeActivity.USER_UID).getValue(Boolean.class);
                    holder.loadToolLayout(mContext, deviceId, item.getName(), isOwner, deviceUtils, confirmUtils);
                    // DISABLE LOADING.
                    holder.loadingStatus.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        this.DEVICE_EVENTS.put(deviceId, new SimpleEntry<>(deviceRef, deviceListener));
    }

    @Override
    public int getItemCount() {
        return this.originalData.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onConfirm() {
        this.confirmUtils.confirmUnlinkDevice();
        // REMOVE DEVICE EVENT & DATA.
        String removedDeviceId = this.confirmUtils.getDeviceId();
        SimpleEntry<DatabaseReference, ValueEventListener> deviceEvent = this.DEVICE_EVENTS.get(removedDeviceId);
        if (deviceEvent != null) {
            deviceEvent.getKey().removeEventListener(deviceEvent.getValue());
        }
        this.originalData.removeIf(deviceItem -> deviceItem.getId().equals(removedDeviceId));
        notifyDataSetChanged();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ProgressBar loadingStatus;
        private final TextView tvName;
        private final ImageView ivState, ivDetect;
        private final ImageView ivInfo, ivShare, ivUnlink;
        private final LinearLayout layoutMain, layoutOwnerTool;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.loadingStatus = itemView.findViewById(R.id.loading_view);
            this.tvName = itemView.findViewById(R.id.tv_name);
            this.ivState = itemView.findViewById(R.id.iv_online);
            this.ivDetect = itemView.findViewById(R.id.iv_detect);
            this.ivInfo = itemView.findViewById(R.id.iv_info);
            this.ivShare = itemView.findViewById(R.id.iv_share);
            this.ivUnlink = itemView.findViewById(R.id.iv_unlink);
            this.layoutMain = itemView.findViewById(R.id.layout_main);
            this.layoutOwnerTool = itemView.findViewById(R.id.layout_owner_tool);
        }

        private void loadInfoLayout(Context context, DeviceItem item) {
            // INFORMATION LAYOUT.
            this.tvName.setText(item.getName());
            int onlineId = item.isOnline() ? R.drawable.icon_online : R.drawable.icon_offline;
            this.ivState.setImageDrawable(ContextCompat.getDrawable(context, onlineId));
            if (item.isOnline()) {
                int detectId = item.isDetect() ? R.drawable.icon_fire : R.drawable.icon_none;
                this.ivDetect.setImageDrawable(ContextCompat.getDrawable(context, detectId));
                int statusId = item.isDetect() ? R.drawable.bg_fire : R.drawable.bg_none;
                this.layoutMain.setBackground(ContextCompat.getDrawable(context, statusId));
            } else {
                this.ivDetect.setImageDrawable(null);
                this.layoutMain.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_offline));
            }
        }

        private void loadToolLayout(Context context, String deviceId, String deviceName, Boolean isOwner,
                                    DeviceUtils deviceUtils, ConfirmUtils confirmUtils) {
            this.layoutOwnerTool.setVisibility(View.GONE);
            if (Boolean.TRUE.equals(isOwner)) {
                this.layoutOwnerTool.setVisibility(View.VISIBLE);
                // DEVICE INFORMATION.
                this.ivInfo.setOnClickListener(v -> {
                    deviceUtils.setDeviceId(deviceId);
                    deviceUtils.setDeviceName(deviceName);
                    deviceUtils.showDeviceDialog(R.layout.dialog_device_update);
                });
                // DEVICE SHARE.
                this.ivShare.setOnClickListener(v -> {
                    deviceUtils.setDeviceId(deviceId);
                    deviceUtils.setDeviceName(deviceName);
                    deviceUtils.showDeviceDialog(R.layout.dialog_device_share);
                });
            }
            // DEVICE UNLINK.
            this.ivUnlink.setOnClickListener(v -> {
                String message = ContextCompat.getString(context, R.string.message_unlink_device);
                confirmUtils.setMessage(String.format(message, deviceName));
                confirmUtils.setDeviceId(deviceId);
                confirmUtils.showConfirmDialog();
            });
        }
    }
}
