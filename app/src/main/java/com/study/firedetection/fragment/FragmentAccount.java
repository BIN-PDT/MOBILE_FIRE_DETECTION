package com.study.firedetection.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.LoginActivity;
import com.study.firedetection.R;
import com.study.firedetection.utils.OTPUtils;
import com.study.firedetection.utils.PasswordUtils;

public class FragmentAccount extends Fragment {
    private Context mContext;
    private TextView tvUserId, tvDeviceQuantity;
    private TextView tvChangePassword, tvLogout, tvDeleteAccount;
    private PasswordUtils passwordUtils;
    private OTPUtils otpUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.onReady(view);
        this.onEvent();
    }

    private void onReady(View view) {
        this.mContext = getContext();
        this.tvUserId = view.findViewById(R.id.tv_user_id);
        this.tvDeviceQuantity = view.findViewById(R.id.tv_device_quantity);

        this.tvChangePassword = view.findViewById(R.id.tv_change_password);
        this.tvLogout = view.findViewById(R.id.tv_logout);
        this.tvDeleteAccount = view.findViewById(R.id.tv_delete_account);

        this.passwordUtils = new PasswordUtils(getActivity());
        this.otpUtils = new OTPUtils(getActivity());
        this.otpUtils.setUseForDeleteAccount(true);
    }

    @SuppressLint("DefaultLocale")
    private void onEvent() {
        // FIREBASE EVENT.
        String userPath = String.format("users/%s", HomeActivity.USER_ID);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef = database.getReference(userPath);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                long quantity = task.getResult().child("devices").getChildrenCount();
                String quantityString = quantity == 0 ? "No devices" :
                        quantity == 1 ? "1 device" : String.format("%d devices", quantity);
                this.tvDeviceQuantity.setText(quantityString);
            }
        });
        // USER IDENTIFIER.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userPhone = user.getPhoneNumber(), userEmail = user.getEmail();
            if (userEmail != null && !userEmail.isEmpty()) {
                this.tvUserId.setText(userEmail);
                // DELETE EMAIL ACCOUNT.
                this.tvDeleteAccount.setOnClickListener(v -> this.passwordUtils.showPasswordDialog());
            } else {
                this.tvUserId.setText(userPhone);
                this.tvChangePassword.setVisibility(View.GONE);
                // DELETE PHONE ACCOUNT.
                this.tvDeleteAccount.setOnClickListener(v -> this.otpUtils.sendOTP(userPhone));
            }
        }
        // CHANGE PASSWORD.
        this.tvChangePassword.setOnClickListener(v -> {

        });
        // LOGOUT.
        this.tvLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this.mContext, LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
    }
}