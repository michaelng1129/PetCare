package com.eee3457.petcare.mainactivity.home.healthtracker;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eee3457.petcare.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

public class HealthTrackerViewPager extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private HealthTrackerViewPagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_home_health_tracker_screen, container, false);
        tabLayout = view.findViewById(R.id.health_tabs);
        viewPager = view.findViewById(R.id.health_tracker_view_pager);

        ArrayList<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new HealthMetricsScreen());
        fragmentList.add(new HealthVaccinationsScreen());
        fragmentList.add(new HealthMedicationsScreen());

        adapter = new HealthTrackerViewPagerAdapter(getChildFragmentManager(), getLifecycle(), fragmentList);
        viewPager.setAdapter(adapter);

        // Bind TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Metrics");
                    break;
                case 1:
                    tab.setText("Vaccinations");
                    break;
                case 2:
                    tab.setText("Medications");
                    break;
            }
        }).attach();

        return view;
    }

}