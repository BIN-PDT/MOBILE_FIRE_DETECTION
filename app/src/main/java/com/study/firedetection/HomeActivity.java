package com.study.firedetection;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.study.firedetection.adapter.HomePagerAdapter;

public class HomeActivity extends AppCompatActivity {
    public static String USER_UID, USER_ID;
    private final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        this.requestPermission();
        this.loadUserData();
        this.onReady();
        this.onEvent();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, this.NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.createNotificationChannel();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            String channel_id = getString(R.string.channel_id);

            if (notificationManager.getNotificationChannel(channel_id) == null) {
                String name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
                channel.setDescription(description);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            USER_UID = user.getUid();
            String userPhone = user.getPhoneNumber(), userEmail = user.getEmail();
            USER_ID = userPhone != null && !userPhone.isEmpty() ? userPhone : userEmail;

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    // TOKEN.
                    String tokenPath = String.format("users/%s/token", USER_UID);
                    DatabaseReference tokenRef = database.getReference(tokenPath);
                    tokenRef.setValue(task.getResult());
                    // IDENTIFIER.
                    String identifierPath = String.format("users/%s/identifier", USER_UID);
                    DatabaseReference identifierRef = database.getReference(identifierPath);
                    identifierRef.setValue(USER_ID);
                }
            });
        }
    }

    private void onReady() {
        this.bottomNavigation = findViewById(R.id.bottom_navigation);
        // DISPLAY LAYOUT.
        this.viewPager = findViewById(R.id.view_pager);
        this.viewPager.setAdapter(new HomePagerAdapter(this));
    }

    private void onEvent() {
        this.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemID = item.getItemId() == R.id.action_devices ? 0 : 1;
            this.viewPager.setCurrentItem(itemID);
            return true;
        });

        this.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                int itemID = position == 0 ? R.id.action_devices : R.id.action_account;
                bottomNavigation.getMenu().findItem(itemID).setChecked(true);
            }
        });
    }
}