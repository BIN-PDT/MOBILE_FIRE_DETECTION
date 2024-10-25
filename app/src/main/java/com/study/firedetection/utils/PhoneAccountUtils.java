package com.study.firedetection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.hbb20.CountryCodePicker;
import com.study.firedetection.HomeActivity;
import com.study.firedetection.R;

public class PhoneAccountUtils {
    private final Activity activity;
    private final OTPUtils otpUtils;
    private AlertDialog dialog;

    public PhoneAccountUtils(Activity activity) {
        this.activity = activity;
        this.otpUtils = new OTPUtils(activity);
        this.otpUtils.setUseForDeleteAccount(true);
    }

    public void showPhoneAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_phone_account, null);
        this.onPhoneAccountView(view);
        builder.setView(view);

        this.dialog = builder.create();
        this.dialog.show();
    }

    private void onPhoneAccountView(@NonNull View view) {
        CountryCodePicker ccpCountry = view.findViewById(R.id.ccp_country);
        EditText edtPhone = view.findViewById(R.id.edt_phone);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

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
            if (!ccpCountry.isValidFullNumber()) {
                Toast.makeText(activity, "INVALID PHONE NUMBER", Toast.LENGTH_SHORT).show();
                edtPhone.requestFocus();
                return;
            }

            String phoneNumber = ccpCountry.getFullNumberWithPlus();
            if (!phoneNumber.equals(HomeActivity.USER_ID)) {
                Toast.makeText(activity, "WRONG PHONE NUMBER", Toast.LENGTH_SHORT).show();
                return;
            }

            this.dialog.dismiss();
            this.otpUtils.sendOTP(phoneNumber);
        });
    }
}
