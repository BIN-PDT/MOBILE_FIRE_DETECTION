package com.study.firedetection.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.study.firedetection.R;

public class ChangePasswordUtils {
    private final Activity activity;
    private final FirebaseAuth mAuth;
    private final LoadingUtils loadingUtils;
    private AlertDialog dialog;

    public ChangePasswordUtils(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
        this.loadingUtils = new LoadingUtils(activity);
    }

    public void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_change_password, null);
        this.onChangePasswordView(view);
        builder.setView(view);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onChangePasswordView(@NonNull View view) {
        EditText edtCurrentPassword = view.findViewById(R.id.edt_current_password);
        EditText edtPassword = view.findViewById(R.id.edt_password);
        EditText edtPassword2 = view.findViewById(R.id.edt_password2);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        this.onPasswordField(edtCurrentPassword);
        this.onPasswordField(edtPassword);
        this.onPasswordField(edtPassword2);

        btnConfirm.setOnClickListener(v -> {
            String currentPassword = edtCurrentPassword.getText().toString().trim();
            if (currentPassword.isEmpty()) {
                Toast.makeText(activity, "CURRENT PASSWORD CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtCurrentPassword.requestFocus();
                return;
            }

            String password = edtPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(activity, "PASSWORD CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtPassword.requestFocus();
                return;
            }

            String password2 = edtPassword2.getText().toString().trim();
            if (!password.equals(password2)) {
                Toast.makeText(activity, "WRONG CONFIRM PASSWORD", Toast.LENGTH_SHORT).show();
                edtPassword2.requestFocus();
                return;
            }

            this.changePassword(currentPassword, password);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onPasswordField(EditText edtPassword) {
        edtPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRightPosition = edtPassword.getRight() - edtPassword.getCompoundPaddingRight();

                if (event.getRawX() >= drawableRightPosition) {
                    boolean isHidden = edtPassword.getInputType() != InputType.TYPE_TEXT_VARIATION_PASSWORD;
                    updatePasswordField(edtPassword, isHidden);
                    return true;
                }
            }
            return false;
        });
    }

    private void updatePasswordField(EditText edtPassword, boolean isHidden) {
        if (isHidden) {
            edtPassword.setTransformationMethod(new PasswordTransformationMethod());
            edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.icon_field_hidden, 0);
        } else {
            edtPassword.setTransformationMethod(null);
            edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.icon_field_shown, 0);
        }
        edtPassword.setTypeface(ResourcesCompat.getFont(activity, R.font.bree_serif_regular));
        edtPassword.setSelection(edtPassword.getText().length());
        edtPassword.requestFocus();
    }

    private void changePassword(String currentPassword, String newPassword) {
        this.loadingUtils.showLoadingDialog();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email == null) {
                this.loadingUtils.hideLoadingDialog();
                Toast.makeText(activity, "EMAIL ACCOUNT NOT FOUND", Toast.LENGTH_SHORT).show();
                return;
            }

            this.mAuth.signInWithEmailAndPassword(email, currentPassword).addOnCompleteListener(task -> {
                this.loadingUtils.hideLoadingDialog();
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    this.dialog.dismiss();
                                    Toast.makeText(activity, "CHANGE PASSWORD SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(activity, "WRONG CURRENT PASSWORD", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            this.loadingUtils.hideLoadingDialog();
            Toast.makeText(activity, "ACCOUNT NOT FOUND", Toast.LENGTH_SHORT).show();
        }
    }
}
