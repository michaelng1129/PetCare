package com.eee3457.petcare.startactivity.onboarding;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eee3457.petcare.R;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;


public class OnboardingViewPager extends Fragment {
    private ViewPager2 viewPager;
    private WormDotsIndicator wormDotsIndicator;
    private OnboardingViewPagerAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_start_onboarding_view_pager, container, false);

        viewPager = view.findViewById(R.id.onboarding_view_pager);
        wormDotsIndicator = view.findViewById(R.id.worm_dots_indicator);

        ArrayList<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new StartFirstScreen());
        fragmentList.add(new StartSecondScreen());
        fragmentList.add(new StartThirdScreen());

        adapter = new OnboardingViewPagerAdapter(getChildFragmentManager(), getLifecycle(), fragmentList);
        viewPager.setAdapter(adapter);

        wormDotsIndicator.attachTo(viewPager);
        return view;
    }
}