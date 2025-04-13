package com.eee3457.petcare.startactivity;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eee3457.petcare.R;

public class StartSplashFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Call setup animations after the view is created
        setupAnimations(view);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_splashFragment_to_viewPagerFragment);
        }, 1000);
    }

    private void setupAnimations(View view) {
        // Logo pulse animation
        CardView logoContainer = view.findViewById(R.id.logo_container);
        Animator animator = AnimatorInflater.loadAnimator(getContext(), R.animator.pulse_animation);
        animator.setTarget(logoContainer);
        animator.start();

        // Loading dots animations with different delays
        View dot1 = view.findViewById(R.id.loading_dot1);
        View dot2 = view.findViewById(R.id.loading_dot2);
        View dot3 = view.findViewById(R.id.loading_dot3);

        Animator dotAnimator1 = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_dot_animation);
        dotAnimator1.setTarget(dot1);

        Animator dotAnimator2 = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_dot_animation);
        dotAnimator2.setTarget(dot2);
        dotAnimator2.setStartDelay(300);


        Animator dotAnimator3 = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_dot_animation);
        dotAnimator3.setTarget(dot3);
        dotAnimator3.setStartDelay(600);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(dotAnimator1, dotAnimator2, dotAnimator3);
        animatorSet.start();

        // Fade in animations for text
        View appName = view.findViewById(R.id.app_name);
        View appTagline = view.findViewById(R.id.app_tagline);

        Animator nameAnimator = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_app_name_animation);
        nameAnimator.setTarget(appName);

        Animator taglineAnimator = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_app_tagline_animation);
        taglineAnimator.setTarget(appTagline);

        AnimatorSet textAnimations = new AnimatorSet();
        textAnimations.playTogether(nameAnimator, taglineAnimator);
        textAnimations.start();
    }
}