package com.study.firedetection.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;
import com.study.firedetection.adapter.DeviceRecyclerAdapter;
import com.study.firedetection.entity.DeviceItem;

import java.util.ArrayList;
import java.util.List;

public class FragmentDevices extends Fragment {
    private ImageView ivAddDevice;
    private DeviceRecyclerAdapter deviceRecyclerAdapter;

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

    private void onReady(View view) {
        this.ivAddDevice = view.findViewById(R.id.iv_add_device);
        // DEVICES LAYOUT.
        this.deviceRecyclerAdapter = new DeviceRecyclerAdapter(getContext());
        RecyclerView rvDevices = view.findViewById(R.id.rv_devices);
        rvDevices.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        rvDevices.setAdapter(this.deviceRecyclerAdapter);
    }

    private void onEvent() {
        this.ivAddDevice.setOnClickListener(v -> {

        });
        // FIREBASE EVENT.
        String devicesPath = String.format("users/%s/devices", HomeActivity.USER_ID);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference devicesRef = database.getReference(devicesPath);
        devicesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DeviceItem> data = new ArrayList<>();
                task.getResult().getChildren().forEach(device -> {
                    DeviceItem item = new DeviceItem();
                    item.setId(device.getKey());
                    data.add(item);
                });
                this.deviceRecyclerAdapter.setData(data);
            }
        });
    }
}