package com.study.firedetection.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.study.firedetection.fragment.FragmentHistory;
import com.study.firedetection.fragment.FragmentToday;

public class DevicePagerAdapter extends FragmentStateAdapter {
    public DevicePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new FragmentToday();
        } else {
            return new FragmentHistory();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
