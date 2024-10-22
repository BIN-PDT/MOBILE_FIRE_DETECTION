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

public class PasswordUtils implements ConfirmUtils.IOnClickListener {
    private final Activity activity;
    private final FirebaseAuth mAuth;
    private final LoadingUtils loadingUtils;
    private final ConfirmUtils confirmUtils;

    public PasswordUtils(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
        this.loadingUtils = new LoadingUtils(activity);
        this.confirmUtils = new ConfirmUtils(activity, this);
    }

    public void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_password, null);
        onPasswordView(view);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void onPasswordView(@NonNull View view) {
        EditText edtEmail = view.findViewById(R.id.edt_email);
        EditText edtPassword = view.findViewById(R.id.edt_password);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        btnConfirm.setOnClickListener(v -> {
            loadingUtils.showLoadingDialog();

            String email = edtEmail.getText().toString();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(activity, "INVALID EMAIL ADDRESS", Toast.LENGTH_SHORT).show();
                edtEmail.requestFocus();
                return;
            }

            String password = edtPassword.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(activity, "PASSWORD CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtPassword.requestFocus();
                return;
            }

            this.checkEmailAndPassword(email, password);
        });
    }

    private void checkEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            loadingUtils.hideLoadingDialog();
            if (task.isSuccessful()) {
                confirmUtils.showConfirmDialog();
            } else {
                Toast.makeText(activity, "WRONG EMAIL OR PASSWORD", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConfirm() {
        confirmUtils.confirmDeleteAccount();
    }
}
