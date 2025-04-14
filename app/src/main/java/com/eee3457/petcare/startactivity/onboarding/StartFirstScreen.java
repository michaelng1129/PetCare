package com.eee3457.petcare.startactivity.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eee3457.petcare.R;
import com.google.android.material.button.MaterialButton;

public class StartFirstScreen extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_first_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton getStartedButton = view.findViewById(R.id.skip_button);
        getStartedButton.setOnClickListener(v -> navigateToLogin());

        MaterialButton backButton = view.findViewById(R.id.next_button);
        backButton.setOnClickListener(v -> goToNextPage());
    }

    private void navigateToLogin() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_startFirstScreen_to_startLoginScreen);
    }

    private void goToNextPage() {
        ViewPager2 viewPager = requireActivity().findViewById(R.id.view_pager);
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
    }

}