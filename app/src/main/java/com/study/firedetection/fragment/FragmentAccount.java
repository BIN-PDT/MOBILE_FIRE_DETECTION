package com.study.firedetection.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.LoginActivity;
import com.study.firedetection.R;
import com.study.firedetection.utils.ChangePasswordUtils;
import com.study.firedetection.utils.EmailAccountUtils;
import com.study.firedetection.utils.PhoneAccountUtils;

public class FragmentAccount extends Fragment {
    private Context mContext;
    private TextView tvUserId, tvDeviceQuantity;
    private TextView tvChangePassword, tvLogout, tvDeleteAccount;
    private EmailAccountUtils emailAccountUtils;
    private PhoneAccountUtils phoneAccountUtils;
    private ChangePasswordUtils changePasswordUtils;

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

    @Override
    public void onResume() {
        super.onResume();
        this.loadDeviceQuantity();
    }

    @SuppressLint("DefaultLocale")
    private void loadDeviceQuantity() {
        String userPath = String.format("users/%s", HomeActivity.USER_UID);
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
    }

    private void onReady(View view) {
        this.mContext = getContext();
        this.tvUserId = view.findViewById(R.id.tv_user_id);
        this.tvDeviceQuantity = view.findViewById(R.id.tv_device_quantity);

        this.tvChangePassword = view.findViewById(R.id.tv_change_password);
        this.tvLogout = view.findViewById(R.id.tv_logout);
        this.tvDeleteAccount = view.findViewById(R.id.tv_delete_account);

        Activity activity = getActivity();
        this.emailAccountUtils = new EmailAccountUtils(activity);
        this.phoneAccountUtils = new PhoneAccountUtils(activity);
        this.changePasswordUtils = new ChangePasswordUtils(activity);
    }

    @SuppressLint("DefaultLocale")
    private void onEvent() {
        // USER IDENTIFIER.
        this.tvUserId.setText(this.hideUserInformation(HomeActivity.USER_ID));
        if (HomeActivity.USER_ID.contains("@")) {
            // DELETE EMAIL ACCOUNT.
            this.tvDeleteAccount.setOnClickListener(v -> this.emailAccountUtils.showEmailAccountDialog());
            // CHANGE PASSWORD.
            this.tvChangePassword.setOnClickListener(v -> this.changePasswordUtils.showChangePasswordDialog());
        } else {
            // DISABLE CHANGE PASSWORD.
            this.tvChangePassword.setVisibility(View.GONE);
            // DELETE PHONE ACCOUNT.
            this.tvDeleteAccount.setOnClickListener(v -> this.phoneAccountUtils.showPhoneAccountDialog());
        }
        // LOGOUT.
        this.tvLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this.mContext, LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private String hideUserInformation(String userId) {
        if (userId == null) return null;
        if (userId.contains("@")) {
            String[] components = userId.split("@");
            String username = components[0], domain = components[1];

            if (username.length() <= 2) {
                return "*".repeat(username.length()) + "@" + domain;
            } else {
                int hiddenLength = username.length() / 2;
                int visibleLength = username.length() - hiddenLength;

                return username.substring(0, visibleLength / 2)
                        + "*".repeat(hiddenLength)
                        + username.substring(username.length() - visibleLength / 2)
                        + "@" + domain;
            }
        } else {
            return "*".repeat(userId.length() - 4)
                    + userId.substring(userId.length() - 4);
        }
    }
}