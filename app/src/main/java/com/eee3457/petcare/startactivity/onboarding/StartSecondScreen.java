package com.eee3457.petcare.startactivity.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eee3457.petcare.R;
import com.google.android.material.button.MaterialButton;

public class StartSecondScreen extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_second_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton getStartedButton = view.findViewById(R.id.back_button);
        getStartedButton.setOnClickListener(v -> goToPreviousPage());

        MaterialButton backButton = view.findViewById(R.id.next_button);
        backButton.setOnClickListener(v -> goToNextPage());
    }

    private void goToPreviousPage() {
        ViewPager2 viewPager = requireActivity().findViewById(R.id.view_pager);
        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
    }

    private void goToNextPage() {
        ViewPager2 viewPager = requireActivity().findViewById(R.id.view_pager);
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
    }
}