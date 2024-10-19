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
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.study.firedetection.MainActivity;
import com.study.firedetection.R;

public class SignUpUtils {
    private final Activity activity;
    private final FirebaseAuth mAuth;

    public SignUpUtils(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void showSignUpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_signup, null);
        onSignUpView(view);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void onSignUpView(@NonNull View view) {
        EditText edtEmail = view.findViewById(R.id.edt_email);
        EditText edtPassword = view.findViewById(R.id.edt_password);
        EditText edtPassword2 = view.findViewById(R.id.edt_password2);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        onPasswordField(edtPassword);
        onPasswordField(edtPassword2);

        btnConfirm.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(activity, "INVALID EMAIL", Toast.LENGTH_SHORT).show();
                edtEmail.requestFocus();
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

            this.signUpAccount(email, password);
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
            edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_field_password, 0, R.drawable.icon_field_hidden, 0);
        } else {
            edtPassword.setTransformationMethod(null);
            edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_field_password, 0, R.drawable.icon_field_shown, 0);
        }
        edtPassword.setTypeface(ResourcesCompat.getFont(activity, R.font.bree_serif_regular));
        edtPassword.setSelection(edtPassword.getText().length());
        edtPassword.requestFocus();
    }

    private void signUpAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            this.login(user.getEmail());
                        }
                    } else {
                        Toast.makeText(activity, "AUTHENTICATION FAILED", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void login(String email) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("userID", email);
        activity.startActivity(intent);
        activity.finish();
    }
}
