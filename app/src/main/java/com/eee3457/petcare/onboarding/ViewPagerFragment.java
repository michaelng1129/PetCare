package com.eee3457.petcare.onboarding;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eee3457.petcare.R;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;


public class ViewPagerFragment extends Fragment {
    private ViewPager2 viewPager;
    private DotsIndicator dotsIndicator;
    private ViewPagerAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_view_pager, container, false);

        viewPager = view.findViewById(R.id.view_pager);
        dotsIndicator = view.findViewById(R.id.dots_indicator);

        ArrayList<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new FirstScreen());
        fragmentList.add(new SecondScreen());
        fragmentList.add(new ThirdScreen());

        adapter = new ViewPagerAdapter(getChildFragmentManager(), getLifecycle(), fragmentList);
        viewPager.setAdapter(adapter);

        dotsIndicator.attachTo(viewPager);
        return view;
    }
}