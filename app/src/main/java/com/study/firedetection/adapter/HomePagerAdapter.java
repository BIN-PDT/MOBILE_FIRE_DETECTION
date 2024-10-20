package com.study.firedetection.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.study.firedetection.fragment.FragmentAccount;
import com.study.firedetection.fragment.FragmentDevices;

public class HomePagerAdapter extends FragmentStateAdapter {
    public HomePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new FragmentDevices();
        } else {
            return new FragmentAccount();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
