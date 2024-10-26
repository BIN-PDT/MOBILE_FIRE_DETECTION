package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.LoginActivity;
import com.study.firedetection.R;

public class ConfirmUtils {
    private final Activity activity;
    private final IOnClickListener onClickListener;
    private AlertDialog dialog;
    private String message, deviceId, userUID;

    public ConfirmUtils(Activity activity, IOnClickListener onClickListener) {
        this.activity = activity;
        this.onClickListener = onClickListener;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    public void showConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_confirm, null);
        this.onConfirmView(view);
        builder.setView(view);
        builder.setCancelable(false);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onConfirmView(@NonNull View view) {
        TextView tvMessage = view.findViewById(R.id.tv_message);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        tvMessage.setText(this.message);
        btnConfirm.setOnClickListener(v -> this.onClickListener.onConfirm());
        btnCancel.setOnClickListener(v -> {
            this.onClickListener.onCancel();
            this.dialog.dismiss();
        });
    }

    public void confirmDeleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String userPath = String.format("users/%s", HomeActivity.USER_UID);
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference userRef = database.getReference(userPath);
                    DatabaseReference usersRef = database.getReference("users");
                    userRef.child("devices").get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            task1.getResult().getChildren().forEach(deviceData -> {
                                String deviceId = deviceData.getKey();
                                if (deviceId == null) return;

                                String usersDevicePath = String.format("devices/%s/users", deviceId);
                                DatabaseReference usersDeviceRef = database.getReference(usersDevicePath);
                                // OWNER.
                                if (Boolean.TRUE.equals(deviceData.getValue(Boolean.class))) {
                                    // REMOVE USER OF DEVICE.
                                    usersDeviceRef.removeValue();
                                    // REMOVE DEVICE OF USER.
                                    usersRef.get().addOnCompleteListener(task2 -> {
                                        if (task2.isSuccessful()) {
                                            task2.getResult().getChildren().forEach(invitor -> {
                                                invitor.child("devices").child(deviceId).getRef().removeValue();
                                            });
                                        }
                                    });
                                }
                                // INVITOR.
                                else if (Boolean.FALSE.equals(deviceData.getValue(Boolean.class))) {
                                    usersDeviceRef.child(HomeActivity.USER_UID).removeValue();
                                }
                            });
                            // REMOVE INVITATION.
                            DatabaseReference sharesRef = database.getReference("shares");
                            sharesRef.get().addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    task2.getResult().getChildren().forEach(invitation -> {
                                        String sender = invitation.child("sender").getValue(String.class);
                                        String receiver = invitation.child("receiver").getValue(String.class);
                                        if (sender == null || receiver == null) return;

                                        if (sender.equals(HomeActivity.USER_ID) || receiver.equals(HomeActivity.USER_ID)) {
                                            invitation.getRef().removeValue();
                                        }
                                    });
                                }
                            });
                            // REMOVE USER FROM DATABASE.
                            userRef.removeValue();
                            // BACK TO LOGIN.
                            this.dialog.dismiss();
                            Intent intent = new Intent(activity, LoginActivity.class);
                            activity.startActivity(intent);
                            activity.finish();
                        } else {
                            Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(activity, "ACCOUNT NOT FOUND", Toast.LENGTH_SHORT).show();
        }
    }

    public void confirmUnlinkDevice() {
        String deviceUserPath = String.format("users/%s/devices/%s", HomeActivity.USER_UID, this.deviceId);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference deviceUserRef = database.getReference(deviceUserPath);
        deviceUserRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String usersDevicePath = String.format("devices/%s/users", this.deviceId);
                DatabaseReference usersDeviceRef = database.getReference(usersDevicePath);
                usersDeviceRef.get().addOnCompleteListener(task1 -> {
                    // OWNER.
                    if (Boolean.TRUE.equals(task.getResult().getValue(Boolean.class))) {
                        // REMOVE USER OF DEVICE.
                        usersDeviceRef.removeValue();
                        // REMOVE DEVICE OF USER.
                        DatabaseReference usersRef = database.getReference("users");
                        usersRef.get().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                task2.getResult().getChildren().forEach(invitor -> {
                                    invitor.child("devices").child(this.deviceId).getRef().removeValue();
                                });
                            }
                        });
                        // REMOVE INVITATION.
                        DatabaseReference sharesRef = database.getReference("shares");
                        sharesRef.get().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                task2.getResult().getChildren().forEach(invitation -> {
                                    String deviceId = invitation.child("deviceId").getValue(String.class);
                                    if (deviceId == null) return;

                                    if (deviceId.equals(this.deviceId)) {
                                        invitation.getRef().removeValue();
                                    }
                                });
                            }
                        });
                    }
                    // INVITOR.
                    else if (Boolean.FALSE.equals(task.getResult().getValue(Boolean.class))) {
                        usersDeviceRef.child(HomeActivity.USER_UID).removeValue();
                    }
                    deviceUserRef.removeValue();

                    this.dialog.dismiss();
                    Toast.makeText(activity, "DEVICE UNLIKED", Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void confirmRemoveShare() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        // REMOVE USER OF DEVICE.
        String userDevicePath = String.format("devices/%s/users/%s", this.deviceId, this.userUID);
        DatabaseReference userDeviceRef = database.getReference(userDevicePath);
        userDeviceRef.removeValue();
        // REMOVE DEVICE OF USER.
        String deviceUserPath = String.format("users/%s/devices/%s", this.userUID, this.deviceId);
        DatabaseReference deviceUserRef = database.getReference(deviceUserPath);
        deviceUserRef.removeValue();

        this.dialog.dismiss();
        Toast.makeText(activity, "USER REMOVED", Toast.LENGTH_SHORT).show();
    }

    public interface IOnClickListener {
        void onConfirm();

        default void onCancel() {
        }
    }
}
