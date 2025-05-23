package com.eee3457.petcare.mainactivity.home.healthtracker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class HealthTrackerViewPagerAdapter extends FragmentStateAdapter {
    private final List<Fragment> fragmentList;

    public HealthTrackerViewPagerAdapter(@NonNull FragmentManager fm, @NonNull Lifecycle lc, List<Fragment> fragments) {
        super(fm, lc);
        this.fragmentList = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }
}
