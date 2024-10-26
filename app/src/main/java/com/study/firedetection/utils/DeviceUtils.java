package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;
import com.study.firedetection.adapter.SharesRecyclerAdapter;
import com.study.firedetection.entity.InvitationItem;
import com.study.firedetection.entity.ShareItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DeviceUtils {
    private final Activity activity;
    private final LoadingUtils loadingUtils;
    private AlertDialog dialog;
    private String deviceId, deviceName;

    public DeviceUtils(Activity activity) {
        this.activity = activity;
        this.loadingUtils = new LoadingUtils(activity);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void showDeviceDialog(int layoutId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(layoutId, null);
        if (layoutId == R.layout.dialog_device_update) this.onUpdateDeviceView(view);
        else if (layoutId == R.layout.dialog_device_share) this.onShareDeviceView(view);
        builder.setView(view);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onUpdateDeviceView(View view) {
        EditText edtDeviceName = view.findViewById(R.id.edt_device_name);
        edtDeviceName.setText(this.deviceName);
        edtDeviceName.setSelection(this.deviceName.length());

        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            String deviceName = edtDeviceName.getText().toString().trim();
            if (deviceName.isEmpty()) {
                Toast.makeText(activity, "DEVICE NAME CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtDeviceName.requestFocus();
                return;
            }

            if (deviceName.equals(this.deviceName)) {
                this.dialog.dismiss();
                return;
            }

            this.updateDevice(deviceName);
        });
    }

    private void updateDevice(String deviceName) {
        String devicePath = String.format("devices/%s", this.deviceId);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference deviceRef = database.getReference(devicePath);

        this.loadingUtils.showLoadingDialog();
        deviceRef.child("name").setValue(deviceName).addOnCompleteListener(task -> {
            this.loadingUtils.hideLoadingDialog();
            if (task.isSuccessful()) {
                this.dialog.dismiss();
                Toast.makeText(activity, "DEVICE UPDATED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onShareDeviceView(View view) {
        AtomicInteger toolIndex = new AtomicInteger(1);
        ImageView ivPhone = view.findViewById(R.id.iv_phone);
        ImageView ivEmail = view.findViewById(R.id.iv_email);
        ImageView ivShare = view.findViewById(R.id.iv_share);
        LinearLayout layoutSharePhone = view.findViewById(R.id.layout_share_phone);
        LinearLayout layoutShareEmail = view.findViewById(R.id.layout_share_email);
        LinearLayout layoutShares = view.findViewById(R.id.layout_shares);
        TextView tvShareQuantity = view.findViewById(R.id.tv_share_quantity);
        SwipeRefreshLayout srlShares = view.findViewById(R.id.srl_shares);
        CountryCodePicker ccpCountry = view.findViewById(R.id.ccp_country);
        EditText edtPhone = view.findViewById(R.id.edt_phone);
        EditText edtEmail = view.findViewById(R.id.edt_email);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        // SHARES LAYOUT.
        SharesRecyclerAdapter sharesRecyclerAdapter = new SharesRecyclerAdapter(this.activity, this.deviceId, tvShareQuantity);
        RecyclerView rvShares = view.findViewById(R.id.rv_shares);
        rvShares.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        rvShares.setAdapter(sharesRecyclerAdapter);
        // EVENT.
        ivPhone.setOnClickListener(v -> {
            toolIndex.set(1);
            this.updateAuthenticationLayout(1, edtPhone, edtEmail, ivEmail, ivPhone, ivShare,
                    layoutShareEmail, layoutSharePhone, layoutShares, btnConfirm);
        });
        ivEmail.setOnClickListener(v -> {
            toolIndex.set(2);
            this.updateAuthenticationLayout(2, edtPhone, edtEmail, ivEmail, ivPhone, ivShare,
                    layoutShareEmail, layoutSharePhone, layoutShares, btnConfirm);
        });
        ivShare.setOnClickListener(v -> {
            toolIndex.set(3);
            this.updateAuthenticationLayout(3, edtPhone, edtEmail, ivEmail, ivPhone, ivShare,
                    layoutShareEmail, layoutSharePhone, layoutShares, btnConfirm);
        });
        ccpCountry.setTypeFace(ResourcesCompat.getFont(activity, R.font.bree_serif_regular));
        ccpCountry.setCustomDialogTextProvider(new CountryCodePicker.CustomDialogTextProvider() {
            @Override
            public String getCCPDialogTitle(CountryCodePicker.Language language, String defaultTitle) {
                return "SELECT A COUNTRY";
            }

            @Override
            public String getCCPDialogSearchHintText(CountryCodePicker.Language language, String defaultSearchHintText) {
                return "";
            }

            @Override
            public String getCCPDialogNoResultACK(CountryCodePicker.Language language, String defaultNoResultACK) {
                return "";
            }
        });
        ccpCountry.registerCarrierNumberEditText(edtPhone);
        btnConfirm.setOnClickListener(v -> {
            String userId;
            EditText edtUserId;
            if (toolIndex.get() == 1) {
                if (!ccpCountry.isValidFullNumber()) {
                    Toast.makeText(activity, "INVALID PHONE NUMBER", Toast.LENGTH_SHORT).show();
                    edtPhone.requestFocus();
                    return;
                }
                userId = ccpCountry.getFullNumberWithPlus();
                edtUserId = edtPhone;
            } else if (toolIndex.get() == 2) {
                String email = edtEmail.getText().toString();
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(activity, "INVALID EMAIL ADDRESS", Toast.LENGTH_SHORT).show();
                    edtEmail.requestFocus();
                    return;
                }
                userId = email;
                edtUserId = edtEmail;
            } else {
                return;
            }

            this.shareDevice(userId, edtUserId, sharesRecyclerAdapter);
        });
        srlShares.setColorSchemeColors(ContextCompat.getColor(activity, R.color.orange));
        srlShares.setOnRefreshListener(() -> this.loadShares(sharesRecyclerAdapter, srlShares));
        // FIRST LOADING.
        this.loadShares(sharesRecyclerAdapter, srlShares);
    }

    private void loadShares(SharesRecyclerAdapter sharesRecyclerAdapter, SwipeRefreshLayout srlShare) {
        String usersDevicePath = String.format("devices/%s/users", this.deviceId);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersDeviceRef = database.getReference(usersDevicePath);
        usersDeviceRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<ShareItem> data = new ArrayList<>();
                AtomicLong totalItem = new AtomicLong(task.getResult().getChildrenCount());
                // NO DATA.
                if (totalItem.get() == 1) {
                    sharesRecyclerAdapter.loadOriginalData(data);
                    srlShare.setRefreshing(false);
                    return;
                }
                // GET DATA.
                task.getResult().getChildren().forEach(user -> {
                    String userUID = user.getKey();
                    String userIdPath = String.format("users/%s/identifier", userUID);
                    DatabaseReference userIdRef = database.getReference(userIdPath);
                    userIdRef.get().addOnCompleteListener(task1 -> {
                        if (task.isSuccessful()) {
                            String userID = task1.getResult().getValue(String.class);
                            if (Boolean.FALSE.equals(user.getValue(Boolean.class))) {
                                data.add(new ShareItem(userUID, userID));
                                // CHECK COMPLETE.
                                if (totalItem.decrementAndGet() == 1) {
                                    sharesRecyclerAdapter.loadOriginalData(data);
                                    srlShare.setRefreshing(false);
                                }
                            }

                        }
                    });
                });
            }
        });
    }

    private void updateAuthenticationLayout(int toolIndex, EditText edtPhone, EditText edtEmail,
                                            ImageView ivShareEmail, ImageView ivSharePhone, ImageView ivShareShares,
                                            LinearLayout layoutLoginEmail, LinearLayout layoutLoginPhone,
                                            LinearLayout layoutShares, Button btnConfirm) {
        if (toolIndex == 1) {
            edtPhone.setText("");
            edtPhone.requestFocus();

            ivShareShares.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_inactive_share));
            ivShareEmail.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_inactive_email));
            ivSharePhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_active_phone));
            layoutShares.setVisibility(View.GONE);
            layoutLoginEmail.setVisibility(View.GONE);
            layoutLoginPhone.setVisibility(View.VISIBLE);
            btnConfirm.setVisibility(View.VISIBLE);
        } else if (toolIndex == 2) {
            edtEmail.setText("");
            edtEmail.requestFocus();

            ivShareShares.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_inactive_share));
            ivSharePhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_inactive_phone));
            ivShareEmail.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_active_email));
            layoutShares.setVisibility(View.GONE);
            layoutLoginPhone.setVisibility(View.GONE);
            layoutLoginEmail.setVisibility(View.VISIBLE);
            btnConfirm.setVisibility(View.VISIBLE);
        } else {
            ivShareEmail.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_inactive_email));
            ivSharePhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_inactive_phone));
            ivShareShares.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_active_share));
            btnConfirm.setVisibility(View.GONE);
            layoutLoginPhone.setVisibility(View.GONE);
            layoutLoginEmail.setVisibility(View.GONE);
            layoutShares.setVisibility(View.VISIBLE);
        }
    }

    private void shareDevice(String userId, EditText edtUserId, SharesRecyclerAdapter sharesRecyclerAdapter) {
        // CHECK SHARE MAXIMUM.
        if (sharesRecyclerAdapter.getOriginalData().size() == SharesRecyclerAdapter.MAX_SHARES) {
            Toast.makeText(activity, "YOUR DEVICE HAS REACHED SHARING LIMIT", Toast.LENGTH_SHORT).show();
            return;
        }
        // CHECK SHARED USER.
        for (ShareItem item : sharesRecyclerAdapter.getOriginalData()) {
            if (item.getUserID().equals(userId)) {
                edtUserId.setText("");
                Toast.makeText(activity, String.format("%s HAS BEEN SHARED", userId), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // CHECK SHARE WITH SELF.
        this.loadingUtils.showLoadingDialog();
        if (userId.equals(HomeActivity.USER_ID)) {
            this.loadingUtils.hideLoadingDialog();
            edtUserId.setText("");
            Toast.makeText(activity, String.format("A INVITATION HAS SENT TO %s", userId), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sharesRef = database.getReference("shares");
        sharesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String, String> shareData = new HashMap<>();
                shareData.put("date", DateUtils.CURRENT_DATE_TEXT_1);
                shareData.put("sender", HomeActivity.USER_ID);
                shareData.put("receiver", userId);
                shareData.put("deviceId", this.deviceId);
                shareData.put("deviceName", this.deviceName);
                // CHECK INVITATION EXIST.
                for (DataSnapshot invitation : task.getResult().getChildren()) {
                    InvitationItem item = invitation.getValue(InvitationItem.class);
                    if (item != null && item.getReceiver().equals(userId) && item.getDeviceId().equals(this.deviceId)) {
                        item.setId(invitation.getKey());
                        sharesRef.child(item.getId()).child("date").setValue(DateUtils.CURRENT_DATE_TEXT_1);

                        this.loadingUtils.hideLoadingDialog();
                        edtUserId.setText("");
                        Toast.makeText(activity, String.format("A INVITATION HAS SENT TO %s", userId), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // CHECK USER EXIST.
                DatabaseReference usersRef = database.getReference("users");
                usersRef.get().addOnCompleteListener(task1 -> {
                    if (task.isSuccessful()) {
                        for (DataSnapshot user : task1.getResult().getChildren()) {
                            String userIdentifier = user.child("identifier").getValue(String.class);
                            if (Objects.equals(userIdentifier, userId)) {
                                DatabaseReference userShareRef = database.getReference("shares");
                                userShareRef.push().setValue(shareData);
                                break;
                            }
                        }

                        this.loadingUtils.hideLoadingDialog();
                        edtUserId.setText("");
                        Toast.makeText(activity, String.format("A INVITATION HAS SENT TO %s", userId), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(activity, "TASK FAILED", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
