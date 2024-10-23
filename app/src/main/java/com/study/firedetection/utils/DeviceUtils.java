package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.R;

public class DeviceUtils {
    private final Activity activity;
    private final LoadingUtils loadingUtils;
    private AlertDialog dialog;
    private String deviceId, deviceName;

    public DeviceUtils(Activity activity) {
        this.activity = activity;
        this.loadingUtils = new LoadingUtils(activity);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void showDeviceDialog(int layoutId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(layoutId, null);
        if (layoutId == R.layout.dialog_update_device) this.onUpdateDeviceView(view);
        builder.setView(view);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onUpdateDeviceView(View view) {
        EditText edtDeviceName = view.findViewById(R.id.edt_device_name);
        edtDeviceName.setText(this.deviceName);
        edtDeviceName.setSelection(this.deviceName.length());

        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            String deviceName = edtDeviceName.getText().toString().trim();
            if (deviceName.isEmpty()) {
                Toast.makeText(activity, "DEVICE NAME CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtDeviceName.requestFocus();
                return;
            }

            if (deviceName.equals(this.deviceName)) {
                this.dialog.dismiss();
                return;
            }

            String devicePath = String.format("devices/%s", deviceId);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference deviceRef = database.getReference(devicePath);

            this.loadingUtils.showLoadingDialog();
            deviceRef.child("name").setValue(deviceName).addOnCompleteListener(task -> {
                this.loadingUtils.hideLoadingDialog();
                if (task.isSuccessful()) {
                    this.dialog.dismiss();
                    Toast.makeText(activity, "DEVICE UPDATED", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
