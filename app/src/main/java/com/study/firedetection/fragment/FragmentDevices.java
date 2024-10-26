package com.study.firedetection.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;
import com.study.firedetection.adapter.DevicesRecyclerAdapter;
import com.study.firedetection.adapter.InvitationsRecyclerAdapter;
import com.study.firedetection.entity.DeviceItem;
import com.study.firedetection.entity.InvitationItem;
import com.study.firedetection.utils.LoadingUtils;

import java.util.ArrayList;
import java.util.List;

public class FragmentDevices extends Fragment {
    private Context mContext;
    private LoadingUtils loadingUtils;
    private ProgressBar loadingView;
    private ImageView ivAddDevice;
    private SwipeRefreshLayout srlDevices;
    private DevicesRecyclerAdapter devicesRecyclerAdapter;
    private InvitationsRecyclerAdapter invitationsRecyclerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devices, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.onReady(view);
        this.onEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.loadData();
    }

    private void onReady(View view) {
        this.mContext = getContext();
        this.loadingUtils = new LoadingUtils(getActivity());
        this.loadingView = view.findViewById(R.id.loading_view);
        this.ivAddDevice = view.findViewById(R.id.iv_add_device);
        this.srlDevices = view.findViewById(R.id.srl_devices);
        // DEVICES LAYOUT.
        this.devicesRecyclerAdapter = new DevicesRecyclerAdapter(this.mContext, getActivity());
        RecyclerView rvDevices = view.findViewById(R.id.rv_devices);
        rvDevices.setLayoutManager(new LinearLayoutManager(
                this.mContext, LinearLayoutManager.VERTICAL, false));
        rvDevices.setAdapter(this.devicesRecyclerAdapter);
        // INVITATIONS LAYOUT.
        this.invitationsRecyclerAdapter = new InvitationsRecyclerAdapter(this.mContext);
        this.invitationsRecyclerAdapter.setDevicesRecyclerAdapter(this.devicesRecyclerAdapter);
        RecyclerView rvInvitations = view.findViewById(R.id.rv_invitations);
        rvInvitations.setLayoutManager(new LinearLayoutManager(
                this.mContext, LinearLayoutManager.VERTICAL, false));
        rvInvitations.setAdapter(this.invitationsRecyclerAdapter);
    }

    private void onEvent() {
        this.ivAddDevice.setOnClickListener(v -> this.showAddDeviceDialog());
        // SWIPE REFRESH LAYOUT.
        this.srlDevices.setColorSchemeColors(ContextCompat.getColor(this.mContext, R.color.orange));
        this.srlDevices.setOnRefreshListener(this::loadData);
        // FIRST LOADING.
        this.loadData();
    }

    private void loadData() {
        this.loadingView.setVisibility(View.VISIBLE);
        this.loadInvitations();
        this.loadDevices();
    }

    private void loadInvitations() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sharesRef = database.getReference("shares");
        sharesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<InvitationItem> data = new ArrayList<>();
                task.getResult().getChildren().forEach(invitation -> {
                    InvitationItem item = invitation.getValue(InvitationItem.class);
                    // CHECK RECEIVER.
                    if (item != null && item.getReceiver().equals(HomeActivity.USER_ID)) {
                        item.setId(invitation.getKey());
                        data.add(item);
                    }
                });
                this.invitationsRecyclerAdapter.loadOriginalData(data);
            }
        });
    }

    private void loadDevices() {
        String devicesUserPath = String.format("users/%s/devices", HomeActivity.USER_UID);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference devicesUserRef = database.getReference(devicesUserPath);
        devicesUserRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DeviceItem> data = new ArrayList<>();
                task.getResult().getChildren().forEach(device -> data.add(new DeviceItem(device.getKey())));
                this.devicesRecyclerAdapter.loadOriginalData(data);
                this.loadingView.setVisibility(View.GONE);
                this.srlDevices.setRefreshing(false);
            }
        });
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        View view = LayoutInflater.from(this.mContext).inflate(R.layout.dialog_device_add, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        this.onAddDeviceView(view, dialog);
        dialog.show();
    }

    private void onAddDeviceView(View view, AlertDialog dialog) {
        EditText edtDeviceId = view.findViewById(R.id.edt_device_id);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        btnConfirm.setOnClickListener(v -> {
            String deviceId = edtDeviceId.getText().toString().trim();
            if (deviceId.isEmpty()) {
                Toast.makeText(mContext, "DEVICE ID CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtDeviceId.requestFocus();
                return;
            }

            this.addDevice(deviceId, dialog);
        });
    }

    private void addDevice(String deviceId, AlertDialog dialog) {
        this.loadingUtils.showLoadingDialog();
        String devicePath = String.format("devices/%s", deviceId);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference deviceRef = database.getReference(devicePath);
        deviceRef.get().addOnCompleteListener(task -> {
            this.loadingUtils.hideLoadingDialog();
            if (task.isSuccessful()) {
                // CHECK DEVICE EXIST.
                if (!task.getResult().exists()) {
                    Toast.makeText(mContext, "DEVICE DOESN'T EXIST", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference usersDeviceRef = task.getResult().child("users").getRef();
                usersDeviceRef.get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        if (task1.getResult().getChildrenCount() == 0) {
                            // LINK USER TO DEVICE.
                            usersDeviceRef.child(HomeActivity.USER_UID).setValue(true);
                            // LINK DEVICE TO USER.
                            String userPath = String.format("users/%s/devices", HomeActivity.USER_UID);
                            DatabaseReference devicesUserRef = database.getReference(userPath);
                            devicesUserRef.child(deviceId).setValue(true);
                            // RELOAD DEVICES ADAPTER.
                            dialog.dismiss();
                            this.devicesRecyclerAdapter.addNewItem(new DeviceItem(deviceId));
                            Toast.makeText(mContext, "DEVICE LINKED", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "DEVICE WAS LINKED TO ACCOUNT", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(mContext, "TASK FAILED", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(mContext, "TASK FAILED", Toast.LENGTH_SHORT).show();
            }
        });
    }
}