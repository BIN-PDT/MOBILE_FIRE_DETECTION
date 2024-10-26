package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;

import java.util.concurrent.TimeUnit;

public class OTPUtils implements ConfirmUtils.IOnClickListener {
    private final Activity activity;
    private final FirebaseAuth mAuth;
    private final LoadingUtils loadingUtils;
    private final ConfirmUtils confirmUtils;
    private String mPhoneNumber;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;
    private boolean useForDeleteAccount = false;
    private AlertDialog dialog;

    public OTPUtils(Activity activity) {
        this.mAuth = FirebaseAuth.getInstance();
        this.activity = activity;
        this.loadingUtils = new LoadingUtils(activity);
        this.confirmUtils = new ConfirmUtils(activity, this);
    }

    public boolean isUseForDeleteAccount() {
        return useForDeleteAccount;
    }

    public void setUseForDeleteAccount(boolean useForDeleteAccount) {
        this.useForDeleteAccount = useForDeleteAccount;
    }

    public void sendOTP(String phoneNumber) {
        this.mPhoneNumber = phoneNumber;
        this.loadingUtils.showLoadingDialog();
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(this.mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                if (useForDeleteAccount) showDeleteAccountConfirmDialog();
                                else signInWithPhoneAuthCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                loadingUtils.hideLoadingDialog();
                                Toast.makeText(activity, "VERIFICATION FAILED", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(verificationId, forceResendingToken);
                                loadingUtils.hideLoadingDialog();
                                mVerificationId = verificationId;
                                mForceResendingToken = forceResendingToken;
                                showOTPDialog();
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOTPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_otp, null);
        this.onOTPView(view);
        builder.setView(view);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onOTPView(@NonNull View view) {
        TextView tvResend = view.findViewById(R.id.tv_resend);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        EditText edtOTP1, edtOTP2, edtOTP3, edtOTP4, edtOTP5, edtOTP6;
        edtOTP1 = view.findViewById(R.id.edt_otp_1);
        edtOTP2 = view.findViewById(R.id.edt_otp_2);
        edtOTP3 = view.findViewById(R.id.edt_otp_3);
        edtOTP4 = view.findViewById(R.id.edt_otp_4);
        edtOTP5 = view.findViewById(R.id.edt_otp_5);
        edtOTP6 = view.findViewById(R.id.edt_otp_6);

        edtOTP1.addTextChangedListener(new OTPTextWatcher(edtOTP1, edtOTP2));
        edtOTP2.addTextChangedListener(new OTPTextWatcher(edtOTP2, edtOTP3));
        edtOTP3.addTextChangedListener(new OTPTextWatcher(edtOTP3, edtOTP4));
        edtOTP4.addTextChangedListener(new OTPTextWatcher(edtOTP4, edtOTP5));
        edtOTP5.addTextChangedListener(new OTPTextWatcher(edtOTP5, edtOTP6));
        edtOTP6.addTextChangedListener(new OTPTextWatcher(edtOTP6, null));

        edtOTP2.setOnKeyListener(new OTPKeyListener(edtOTP2, edtOTP1));
        edtOTP3.setOnKeyListener(new OTPKeyListener(edtOTP3, edtOTP2));
        edtOTP4.setOnKeyListener(new OTPKeyListener(edtOTP4, edtOTP3));
        edtOTP5.setOnKeyListener(new OTPKeyListener(edtOTP5, edtOTP4));
        edtOTP6.setOnKeyListener(new OTPKeyListener(edtOTP6, edtOTP5));

        tvResend.setOnClickListener(v -> this.resendOTP());
        btnConfirm.setOnClickListener(v -> {
            String OTPCode = edtOTP1.getText().toString() + edtOTP2.getText().toString() + edtOTP3.getText().toString()
                    + edtOTP4.getText().toString() + edtOTP5.getText().toString() + edtOTP6.getText().toString();
            if (OTPCode.length() != 6) {
                Toast.makeText(activity, "INVALID OTP", Toast.LENGTH_SHORT).show();
            }

            this.verifyOTP(OTPCode);
        });
    }

    private void resendOTP() {
        this.loadingUtils.showLoadingDialog();
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(this.mAuth)
                        .setPhoneNumber(this.mPhoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setForceResendingToken(this.mForceResendingToken)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                if (!useForDeleteAccount)
                                    signInWithPhoneAuthCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                loadingUtils.hideLoadingDialog();
                                Toast.makeText(activity, "VERIFICATION FAILED", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(verificationId, forceResendingToken);
                                loadingUtils.hideLoadingDialog();
                                mVerificationId = verificationId;
                                mForceResendingToken = forceResendingToken;
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOTP(String OTPCode) {
        this.loadingUtils.showLoadingDialog();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(this.mVerificationId, OTPCode);
        this.signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        this.mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    this.loadingUtils.hideLoadingDialog();
                    if (task.isSuccessful()) {
                        if (useForDeleteAccount) this.showDeleteAccountConfirmDialog();
                        else {
                            FirebaseUser user = task.getResult().getUser();
                            if (user != null) {
                                this.dialog.dismiss();
                                Intent intent = new Intent(activity, HomeActivity.class);
                                activity.startActivity(intent);
                                activity.finish();
                            } else {
                                Toast.makeText(activity, "ACCOUNT NOT FOUND", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(activity, "INVALID VERIFICATION CODE", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showDeleteAccountConfirmDialog() {
        this.dialog.dismiss();
        String message = ContextCompat.getString(activity, R.string.message_delete_account);
        this.confirmUtils.setMessage(message);
        this.confirmUtils.showConfirmDialog();
    }

    @Override
    public void onConfirm() {
        this.confirmUtils.confirmDeleteAccount();
    }

    private static class OTPKeyListener implements View.OnKeyListener {
        private final EditText currentView, previousView;

        public OTPKeyListener(EditText currentView, EditText previousView) {
            this.currentView = currentView;
            this.previousView = previousView;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (currentView.getText().toString().isEmpty()) {
                    if (previousView != null) {
                        previousView.requestFocus();
                    }
                }
            }
            return false;
        }
    }

    private static class OTPTextWatcher implements TextWatcher {
        private final EditText currentView, nextView;

        public OTPTextWatcher(EditText currentView, EditText nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            } else if (s.length() == 0 && currentView != null) {
                currentView.requestFocus();
            }
        }
    }
}
