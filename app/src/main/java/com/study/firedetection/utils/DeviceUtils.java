package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

            String devicePath = String.format("devices/%s", deviceId);
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
        });
    }

    private void onShareDeviceView(View view) {
        AtomicBoolean isPhoneAuth = new AtomicBoolean(true);
        ImageView ivLoginPhone = view.findViewById(R.id.iv_login_phone);
        ImageView ivLoginEmail = view.findViewById(R.id.iv_login_email);
        LinearLayout layoutLoginPhone = view.findViewById(R.id.layout_login_phone);
        LinearLayout layoutLoginEmail = view.findViewById(R.id.layout_login_email);
        CountryCodePicker ccpCountry = view.findViewById(R.id.ccp_country);
        EditText edtPhone = view.findViewById(R.id.edt_phone);
        EditText edtEmail = view.findViewById(R.id.edt_email);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        // EVENT.
        ivLoginPhone.setOnClickListener(v -> {
            isPhoneAuth.set(true);
            this.updateAuthenticationLayout(true, edtPhone, edtEmail,
                    ivLoginEmail, ivLoginPhone, layoutLoginEmail, layoutLoginPhone);
        });
        ivLoginEmail.setOnClickListener(v -> {
            isPhoneAuth.set(false);
            this.updateAuthenticationLayout(false, edtPhone, edtEmail,
                    ivLoginEmail, ivLoginPhone, layoutLoginEmail, layoutLoginPhone);
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
            if (isPhoneAuth.get()) {
                if (!ccpCountry.isValidFullNumber()) {
                    Toast.makeText(activity, "INVALID PHONE NUMBER", Toast.LENGTH_SHORT).show();
                    edtPhone.requestFocus();
                    return;
                }
                userId = ccpCountry.getFullNumberWithPlus();
            } else {
                String email = edtEmail.getText().toString();
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(activity, "INVALID EMAIL ADDRESS", Toast.LENGTH_SHORT).show();
                    edtEmail.requestFocus();
                    return;
                }
                userId = email;
            }

            Map<String, String> shareData = new HashMap<>();
            shareData.put("date", DateUtils.CURRENT_DATE_TEXT_1);
            shareData.put("sender", HomeActivity.USER_ID);
            shareData.put("receiver", userId);
            shareData.put("deviceId", this.deviceId);
            shareData.put("deviceName", this.deviceName);

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userShareRef = database.getReference("shares");
            userShareRef.push().setValue(shareData).addOnCompleteListener(task -> {
                this.dialog.dismiss();
                String announcement = task.isSuccessful() ? String.format("A INVITATION HAS SENT TO %s", userId) : "TASK FAILED";
                Toast.makeText(activity, announcement, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateAuthenticationLayout(boolean isPhoneAuth, EditText edtPhone, EditText edtEmail,
                                            ImageView ivLoginEmail, ImageView ivLoginPhone,
                                            LinearLayout layoutLoginEmail, LinearLayout layoutLoginPhone) {
        if (isPhoneAuth) {
            edtPhone.setText("");
            edtPhone.requestFocus();

            ivLoginEmail.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_email));
            ivLoginPhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_phone_active));
            layoutLoginEmail.setVisibility(View.GONE);
            layoutLoginPhone.setVisibility(View.VISIBLE);
        } else {
            edtEmail.setText("");
            edtEmail.requestFocus();

            ivLoginPhone.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_phone));
            ivLoginEmail.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.icon_email_active));
            layoutLoginPhone.setVisibility(View.GONE);
            layoutLoginEmail.setVisibility(View.VISIBLE);
        }
    }
}
