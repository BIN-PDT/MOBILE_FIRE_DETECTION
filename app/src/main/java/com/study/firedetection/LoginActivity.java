package com.study.firedetection;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.splashscreen.SplashScreenViewProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hbb20.CountryCodePicker;
import com.study.firedetection.service.MyFirebaseMessagingService;
import com.study.firedetection.utils.EmailAccountUtils;
import com.study.firedetection.utils.ForgotUtils;
import com.study.firedetection.utils.OTPUtils;
import com.study.firedetection.utils.SignUpUtils;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private boolean usePhoneAuth = true;
    private ImageView ivLoginPhone, ivLoginEmail;
    private LinearLayout layoutLoginPhone, layoutLoginEmail;
    private CountryCodePicker ccpCountry;
    private EditText edtPhone, edtEmail, edtPassword;
    private TextView tvForgot, tvSignUp;
    private Button btnLogin;
    private OTPUtils otpUtils;
    private EmailAccountUtils emailAccountUtils;
    private ForgotUtils forgotUtils;
    private SignUpUtils signUpUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(new SplashScreen.OnExitAnimationListener() {
            @Override
            public void onSplashScreenExit(@NonNull SplashScreenViewProvider splashScreenViewProvider) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) onLoginUserEvent();
                else {
                    new Handler().postDelayed(() -> {
                        splashScreenViewProvider.remove();
                        findViewById(R.id.layout_main).startAnimation(
                                AnimationUtils.loadAnimation(LoginActivity.this, R.anim.entrance));
                    }, 2000);
                }
            }
        });
        setContentView(R.layout.activity_login);
        this.onReady();
        this.onEvent();
    }

    private void onLoginUserEvent() {
        Intent originalIntent = getIntent();
        if (!Objects.equals(originalIntent.getAction(), MyFirebaseMessagingService.NOTIFICATION_INTENT_ACTION))
            startActivity(new Intent(this, HomeActivity.class));
        else {
            String deviceId = originalIntent.getStringExtra("deviceId");
            String deviceName = originalIntent.getStringExtra("deviceName");

            Intent intent = new Intent(this, DeviceActivity.class);
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("deviceName", deviceName);
            startActivity(intent);
        }
        finish();
    }

    private void onReady() {
        this.ivLoginPhone = findViewById(R.id.iv_phone);
        this.ivLoginEmail = findViewById(R.id.iv_email);
        this.layoutLoginPhone = findViewById(R.id.layout_share_phone);
        this.layoutLoginEmail = findViewById(R.id.layout_share_email);
        this.ccpCountry = findViewById(R.id.ccp_country);
        this.edtPhone = findViewById(R.id.edt_phone);
        this.edtEmail = findViewById(R.id.edt_email);
        this.edtPassword = findViewById(R.id.edt_password);
        this.tvForgot = findViewById(R.id.tv_forgot);
        this.tvSignUp = findViewById(R.id.tv_signup);
        this.btnLogin = findViewById(R.id.btn_login);

        this.otpUtils = new OTPUtils(this);
        this.emailAccountUtils = new EmailAccountUtils(this);
        this.forgotUtils = new ForgotUtils(this);
        this.signUpUtils = new SignUpUtils(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onEvent() {
        // PHONE NUMBER AUTHENTICATION.
        this.ivLoginPhone.setOnClickListener(v -> this.updateLoginLayout(true));
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
            int color = ContextCompat.getColor(this, isValidNumber ? R.color.green : R.color.red);
            this.edtPhone.setTextColor(color);
        });
        // EMAIL & PASSWORD AUTHENTICATION.
        this.ivLoginEmail.setOnClickListener(v -> this.updateLoginLayout(false));
        this.edtPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRightPosition = this.edtPassword.getRight() - this.edtPassword.getCompoundPaddingRight();

                if (event.getRawX() >= drawableRightPosition) {
                    boolean isHidden = this.edtPassword.getInputType() != InputType.TYPE_TEXT_VARIATION_PASSWORD;
                    this.updatePasswordField(isHidden);
                    return true;
                }
            }
            return false;
        });
        this.tvForgot.setOnClickListener(v -> this.forgotUtils.showForgotDialog());
        this.tvSignUp.setOnClickListener(v -> this.signUpUtils.showSignUpDialog());
        // LOGIN.
        this.btnLogin.setOnClickListener(v -> this.login());
    }

    private void updateLoginLayout(boolean isPhoneAuth) {
        usePhoneAuth = isPhoneAuth;
        if (isPhoneAuth) {
            this.edtPhone.setText("");
            this.edtPhone.requestFocus();

            this.ivLoginEmail.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_inactive_email));
            this.ivLoginPhone.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_active_phone));
            this.layoutLoginEmail.setVisibility(View.GONE);
            this.layoutLoginPhone.setVisibility(View.VISIBLE);
        } else {
            this.edtEmail.setText("");
            this.edtPassword.setText("");
            this.updatePasswordField(true);
            this.edtEmail.requestFocus();

            this.ivLoginPhone.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_inactive_phone));
            this.ivLoginEmail.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.icon_active_email));
            this.layoutLoginPhone.setVisibility(View.GONE);
            this.layoutLoginEmail.setVisibility(View.VISIBLE);
        }
    }

    private void updatePasswordField(boolean isHidden) {
        if (isHidden) {
            this.edtPassword.setTransformationMethod(new PasswordTransformationMethod());
            this.edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            this.edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_field_password, 0, R.drawable.icon_field_hidden, 0);
        } else {
            this.edtPassword.setTransformationMethod(null);
            this.edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            this.edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_field_password, 0, R.drawable.icon_field_shown, 0);
        }
        this.edtPassword.setTypeface(ResourcesCompat.getFont(this, R.font.bree_serif_regular));
        this.edtPassword.setSelection(this.edtPassword.getText().length());
        this.edtPassword.requestFocus();
    }

    private void login() {
        // PHONE NUMBER AUTHENTICATION.
        if (usePhoneAuth) {
            if (!this.ccpCountry.isValidFullNumber()) {
                Toast.makeText(this, "INVALID PHONE NUMBER", Toast.LENGTH_SHORT).show();
                this.ccpCountry.requestFocus();
                return;
            }

            String phoneNumber = this.ccpCountry.getFullNumberWithPlus();
            this.otpUtils.sendOTP(phoneNumber);
        }
        // EMAIL & PASSWORD AUTHENTICATION.
        else {
            String email = this.edtEmail.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "INVALID EMAIL ADDRESS", Toast.LENGTH_SHORT).show();
                edtEmail.requestFocus();
                return;
            }

            String password = this.edtPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "PASSWORD CAN'T BE EMPTY", Toast.LENGTH_SHORT).show();
                edtPassword.requestFocus();
                return;
            }

            this.emailAccountUtils.signInWithEmailAndPassword(email, password);
        }
    }
}