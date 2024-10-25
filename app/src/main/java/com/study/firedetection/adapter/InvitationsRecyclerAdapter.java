package com.study.firedetection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;
import com.study.firedetection.entity.DeviceItem;
import com.study.firedetection.entity.InvitationItem;

import java.util.ArrayList;
import java.util.List;

public class InvitationsRecyclerAdapter extends RecyclerView.Adapter<InvitationsRecyclerAdapter.ItemViewHolder> {
    private final Context mContext;
    private final List<InvitationItem> originalData = new ArrayList<>();
    private DevicesRecyclerAdapter devicesRecyclerAdapter;

    public InvitationsRecyclerAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setDevicesRecyclerAdapter(DevicesRecyclerAdapter devicesRecyclerAdapter) {
        this.devicesRecyclerAdapter = devicesRecyclerAdapter;
    }

    public List<InvitationItem> getOriginalData() {
        return originalData;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadOriginalData(List<InvitationItem> data) {
        this.originalData.clear();
        this.originalData.addAll(data);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void removeItem(int position) {
        this.originalData.remove(position);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        InvitationItem item = this.originalData.get(position);

        holder.tvDeviceName.setText(item.getDeviceName().toUpperCase());
        holder.tvDate.setText(item.getDate());
        holder.tvSender.setText(item.getSender());
        // FIREBASE EVENT.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String invitationPath = String.format("shares/%s", item.getId());
        holder.btnAccept.setOnClickListener(v -> {
            // CHECK SHARE MAXIMUM.
            String usersDevicePath = String.format("devices/%s/users", item.getDeviceId());
            DatabaseReference usersDeviceRef = database.getReference(usersDevicePath);
            usersDeviceRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().getChildrenCount() == SharesRecyclerAdapter.MAX_SHARES + 1) {
                        Toast.makeText(mContext, "DEVICE HAS REACHED SHARING LIMIT", Toast.LENGTH_SHORT).show();
                    } else {
                        // LINK USER TO DEVICE.
                        usersDeviceRef.child(HomeActivity.USER_UID).setValue(false);
                        // LINK DEVICE TO USER.
                        String devicesUserPath = String.format("users/%s/devices", HomeActivity.USER_UID);
                        DatabaseReference devicesUserRef = database.getReference(devicesUserPath);
                        devicesUserRef.child(item.getDeviceId()).setValue(false);
                        // ADD DEVICE TO DEVICES ADAPTER.
                        this.devicesRecyclerAdapter.addNewItem(new DeviceItem(item.getDeviceId()));
                        // DELETE INVITATION.
                        DatabaseReference invitationRef = database.getReference(invitationPath);
                        invitationRef.removeValue();
                        this.removeItem(position);
                    }
                } else {
                    Toast.makeText(this.mContext, "TASK FAILED", Toast.LENGTH_SHORT).show();
                }
            });
        });
        holder.btnReject.setOnClickListener(v -> {
            DatabaseReference invitationRef = database.getReference(invitationPath);
            invitationRef.removeValue();
            this.removeItem(position);
        });
    }

    @Override
    public int getItemCount() {
        return this.originalData.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDeviceName, tvDate, tvSender;
        private final Button btnAccept, btnReject;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            this.tvDate = itemView.findViewById(R.id.tv_date);
            this.tvSender = itemView.findViewById(R.id.tv_sender);
            this.btnAccept = itemView.findViewById(R.id.btn_accept);
            this.btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
