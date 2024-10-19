package com.study.firedetection;

import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;

import com.hbb20.CountryCodePicker;
import com.study.firedetection.utils.OTPUtils;

public class LoginActivity extends AppCompatActivity {
    private CountryCodePicker ccpCountry;
    private EditText edtPhone;
    private Button btnLogin;
    private OTPUtils otpUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(new SplashScreen.OnExitAnimationListener() {
            @Override
            public void onSplashScreenExit(@NonNull SplashScreenViewProvider splashScreenViewProvider) {
                new Handler().postDelayed(() -> {
                    splashScreenViewProvider.remove();
                    findViewById(R.id.layout_main).startAnimation(
                            AnimationUtils.loadAnimation(LoginActivity.this, R.anim.entrance));
                }, 2000);
            }
        });
        setContentView(R.layout.activity_login);
        this.onReady();
        this.onEvent();
    }

    private void onReady() {
        this.ccpCountry = findViewById(R.id.ccp_country);
        this.edtPhone = findViewById(R.id.edt_phone);
        this.btnLogin = findViewById(R.id.btn_login);
        this.otpUtils = new OTPUtils(this, this.ccpCountry);
    }

    private void onEvent() {
        this.ccpCountry.setTypeFace(ResourcesCompat.getFont(this, R.font.bree_serif_regular));
        this.ccpCountry.setCustomDialogTextProvider(new CountryCodePicker.CustomDialogTextProvider() {
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
        this.ccpCountry.registerCarrierNumberEditText(this.edtPhone);
        this.ccpCountry.setPhoneNumberValidityChangeListener(isValidNumber -> {
            int color = ContextCompat.getColor(LoginActivity.this, isValidNumber ? R.color.green : R.color.red);
            float alpha = isValidNumber ? 1 : 0.5f;
            edtPhone.setTextColor(color);
            btnLogin.setAlpha(alpha);
            btnLogin.setEnabled(isValidNumber);
        });

        this.btnLogin.setOnClickListener(v -> otpUtils.sendOTP());
    }
}