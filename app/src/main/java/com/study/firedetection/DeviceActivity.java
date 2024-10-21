package com.study.firedetection;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.study.firedetection.adapter.DevicePagerAdapter;

import java.util.HashMap;
import java.util.Map;

public class DeviceActivity extends AppCompatActivity {
    public static final Map<DatabaseReference, ValueEventListener> FIREBASE_EVENTS = new HashMap<>();
    public static String DEVICE_ID, DEVICE_NAME;
    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        this.getIntentData();
        this.onReady();
        this.onEvent();
    }

    private void getIntentData() {
        DEVICE_ID = getIntent().getStringExtra("deviceId");
        DEVICE_NAME = getIntent().getStringExtra("deviceName");
    }

    private void onReady() {
        this.bottomNavigation = findViewById(R.id.bottom_navigation);
        // DISPLAY LAYOUT.
        this.viewPager = findViewById(R.id.view_pager);
        this.viewPager.setAdapter(new DevicePagerAdapter(this));
    }

    private void onEvent() {
        this.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemID = item.getItemId() == R.id.action_today ? 0 : 1;
            this.viewPager.setCurrentItem(itemID);
            return true;
        });

        this.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                int itemID = position == 0 ? R.id.action_today : R.id.action_history;
                bottomNavigation.getMenu().findItem(itemID).setChecked(true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FIREBASE_EVENTS.forEach(Query::removeEventListener);
    }
}