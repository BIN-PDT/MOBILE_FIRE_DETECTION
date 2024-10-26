package com.study.firedetection.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;

public class EmailAccountUtils implements ConfirmUtils.IOnClickListener {
    private final Activity activity;
    private final FirebaseAuth mAuth;
    private final LoadingUtils loadingUtils;
    private final ConfirmUtils confirmUtils;
    private AlertDialog dialog;

    public EmailAccountUtils(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
        this.loadingUtils = new LoadingUtils(activity);
        this.confirmUtils = new ConfirmUtils(activity, this);
    }

    public void showEmailAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_email_account, null);
        this.onEmailAccountView(view);
        builder.setView(view);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onEmailAccountView(@NonNull View view) {
        EditText edtEmail = view.findViewById(R.id.edt_email);
        EditText edtPassword = view.findViewById(R.id.edt_password);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        this.onPasswordField(edtPassword);

        btnConfirm.setOnClickListener(v -> {
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

    private void checkEmailAndPassword(String email, String password) {
        this.loadingUtils.showLoadingDialog();
        this.mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            this.loadingUtils.hideLoadingDialog();
            if (task.isSuccessful()) {
                this.dialog.dismiss();
                String message = ContextCompat.getString(activity, R.string.message_delete_account);
                this.confirmUtils.setMessage(message);
                this.confirmUtils.showConfirmDialog();
            } else Toast.makeText(activity, "WRONG EMAIL OR PASSWORD", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onConfirm() {
        this.confirmUtils.confirmDeleteAccount();
    }

    public void signInWithEmailAndPassword(String email, String password) {
        this.loadingUtils.showLoadingDialog();
        this.mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            this.loadingUtils.hideLoadingDialog();
            if (task.isSuccessful()) {
                FirebaseUser user = this.mAuth.getCurrentUser();
                if (user != null) {
                    Intent intent = new Intent(activity, HomeActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                } else {
                    Toast.makeText(activity, "ACCOUNT NOT FOUND", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "AUTHENTICATION FAILED", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
