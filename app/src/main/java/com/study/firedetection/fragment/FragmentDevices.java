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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;
import com.study.firedetection.adapter.DevicesRecyclerAdapter;
import com.study.firedetection.entity.DeviceItem;
import com.study.firedetection.utils.LoadingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FragmentDevices extends Fragment {
    private Context mContext;
    private ProgressBar loadingView;
    private LoadingUtils loadingUtils;
    private ImageView ivAddDevice;
    private DevicesRecyclerAdapter devicesRecyclerAdapter;

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
        this.loadDevices();
    }

    private void loadDevices() {
        String devicesUserPath = String.format("users/%s/devices", HomeActivity.USER_ID);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference devicesUserRef = database.getReference(devicesUserPath);
        devicesUserRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DeviceItem> data = new ArrayList<>();
                AtomicLong devicesUserCount = new AtomicLong(task.getResult().getChildrenCount());
                // NO DATA.
                if (devicesUserCount.get() == 0) {
                    this.devicesRecyclerAdapter.loadOriginalData(new ArrayList<>());
                    // DISABLE LOADING.
                    this.loadingView.setVisibility(View.GONE);
                    return;
                }
                // GET DATA.
                task.getResult().getChildren().forEach(device -> {
                    String deviceId = device.getKey();
                    // CHECK BE REMOVED FROM DEVICE.
                    String userDevicePath = String.format("devices/%s/users/%s", deviceId, HomeActivity.USER_ID);
                    DatabaseReference userDeviceRef = database.getReference(userDevicePath);
                    userDeviceRef.get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            if (task1.getResult().getValue(Boolean.class) == null)
                                userDeviceRef.removeValue();
                            else {
                                DeviceItem item = new DeviceItem();
                                item.setId(deviceId);
                                data.add(item);
                            }
                        }
                        // CHECK COMPLETE.
                        if (devicesUserCount.decrementAndGet() == 0) {
                            this.devicesRecyclerAdapter.loadOriginalData(data);
                            // DISABLE LOADING.
                            this.loadingView.setVisibility(View.GONE);
                        }
                    });
                });
            }
        });
    }

    private void onReady(View view) {
        this.mContext = getContext();
        this.loadingUtils = new LoadingUtils(getActivity());
        this.loadingView = view.findViewById(R.id.loading_view);
        this.ivAddDevice = view.findViewById(R.id.iv_add_device);
        // DEVICES LAYOUT.
        this.devicesRecyclerAdapter = new DevicesRecyclerAdapter(this.mContext, getActivity());
        RecyclerView rvDevices = view.findViewById(R.id.rv_devices);
        rvDevices.setLayoutManager(new LinearLayoutManager(
                this.mContext, LinearLayoutManager.VERTICAL, false));
        rvDevices.setAdapter(this.devicesRecyclerAdapter);
    }

    private void onEvent() {
        this.ivAddDevice.setOnClickListener(v -> this.showAddDeviceDialog());
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        View view = LayoutInflater.from(this.mContext).inflate(R.layout.dialog_add_device, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        this.onAddDeviceView(view, dialog);
        dialog.show();
    }

    private void onAddDeviceView(View view, AlertDialog dialog) {
        EditText edtDeviceId = view.findViewById(R.id.edt_device_id);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        btnConfirm.setOnClickListener(v -> {
            String deviceId = edtDeviceId.getText().toString();
            if (deviceId.isEmpty()) {
                Toast.makeText(mContext, "DEVICE ID CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtDeviceId.requestFocus();
                return;
            }

            String usersDevicePath = String.format("devices/%s/users", deviceId);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference usersDeviceRef = database.getReference(usersDevicePath);
            // ENABLE LOADING.
            this.loadingUtils.showLoadingDialog();
            usersDeviceRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        Toast.makeText(mContext, "DEVICE WAS LINKED TO ACCOUNT", Toast.LENGTH_SHORT).show();
                    } else {
                        // LINK USER TO DEVICE.
                        usersDeviceRef.child(HomeActivity.USER_ID).setValue(true);
                        // LINK DEVICE TO USER.
                        String userPath = String.format("users/%s/devices", HomeActivity.USER_ID);
                        DatabaseReference devicesUserRef = database.getReference(userPath);
                        devicesUserRef.child(deviceId).setValue(true);
                        // RELOAD DEVICES ADAPTER.
                        DeviceItem item = new DeviceItem();
                        item.setId(deviceId);
                        this.devicesRecyclerAdapter.addNewItem(item);
                        dialog.dismiss();
                    }
                } else {
                    Toast.makeText(mContext, "TASK FAILED", Toast.LENGTH_SHORT).show();
                }
                // DISABLE LOADING.
                this.loadingUtils.hideLoadingDialog();
            });
        });
    }
}