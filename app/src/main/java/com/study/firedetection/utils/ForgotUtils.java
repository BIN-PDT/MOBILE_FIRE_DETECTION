package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.study.firedetection.R;

public class ForgotUtils {
    private final Activity activity;
    private final FirebaseAuth mAuth;
    private final LoadingUtils loadingUtils;
    private AlertDialog dialog;

    public ForgotUtils(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
        this.loadingUtils = new LoadingUtils(activity);
    }

    public void showForgotDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_forgot, null);
        this.onForgotView(view);
        builder.setView(view);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onForgotView(@NonNull View view) {
        EditText edtEmail = view.findViewById(R.id.edt_email);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);


        btnConfirm.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(activity, "INVALID EMAIL ADDRESS", Toast.LENGTH_SHORT).show();
                edtEmail.requestFocus();
                return;
            }

            this.sendResetPasswordEmail(email);
        });
    }

    private void sendResetPasswordEmail(String email) {
        this.loadingUtils.showLoadingDialog();
        this.mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    this.loadingUtils.hideLoadingDialog();
                    if (task.isSuccessful()) {
                        this.dialog.dismiss();
                        String notification = String.format("AN EMAIL HAS SENT TO %s", email);
                        Toast.makeText(activity, notification, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "AUTHENTICATION FAILED", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
