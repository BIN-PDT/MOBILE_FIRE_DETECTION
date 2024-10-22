package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

    public ConfirmUtils(Activity activity, IOnClickListener onClickListener) {
        this.activity = activity;
        this.onClickListener = onClickListener;
    }

    public void showConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_confirm, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        this.onConfirmView(view, dialog);
        dialog.show();
    }

    private void onConfirmView(@NonNull View view, AlertDialog dialog) {
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnConfirm.setOnClickListener(v -> this.onClickListener.onConfirm());
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            this.onClickListener.onCancel();
        });
    }

    public void confirmDeleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String userPath = String.format("users/%s", HomeActivity.USER_ID);
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference userRef = database.getReference(userPath);
                    userRef.child("devices").get().addOnCompleteListener(task1 -> {
                        // REMOVE USER FROM DEVICES.
                        task1.getResult().getChildren().forEach(deviceData -> {
                            String usersDevicePath = String.format("devices/%s/users", deviceData.getKey());
                            DatabaseReference usersDeviceRef = database.getReference(usersDevicePath);
                            // INVITOR.
                            if (Boolean.FALSE.equals(deviceData.getValue(Boolean.class))) {
                                usersDeviceRef.child(HomeActivity.USER_ID).removeValue();
                                return;
                            }
                            // OWNER.
                            if (Boolean.TRUE.equals(deviceData.getValue(Boolean.class))) {
                                usersDeviceRef.removeValue();
                            }
                        });
                        // REMOVE USER FROM DATABASE.
                        userRef.removeValue();
                        // BACK TO LOGIN.
                        Intent intent = new Intent(activity, LoginActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                    });
                } else {
                    Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(activity, "ACCOUNT NOT FOUND", Toast.LENGTH_SHORT).show();
        }
    }

    public interface IOnClickListener {
        void onConfirm();

        default void onCancel() {
        }
    }
}
