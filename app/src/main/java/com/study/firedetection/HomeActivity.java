package com.study.firedetection;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.study.firedetection.adapter.ViewPagerAdapter;

public class HomeActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        this.onReady();
        this.onEvent();
    }

    private void onReady() {
        this.bottomNavigation = findViewById(R.id.bottom_navigation);
        // DISPLAY LAYOUT.
        this.viewPager = findViewById(R.id.view_pager);
        this.viewPager.setAdapter(new ViewPagerAdapter(this));
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