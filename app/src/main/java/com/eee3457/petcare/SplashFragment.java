package com.eee3457.petcare;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SplashFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Call setup animations after the view is created
        setupAnimations(view);
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

        Animator dotAnim1 = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_dot_animation);
        dotAnim1.setTarget(dot1);

        Animator dotAnim2 = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_dot_animation);
        dotAnim2.setTarget(dot2);
        dotAnim2.setStartDelay(300);


        Animator dotAnim3 = AnimatorInflater.loadAnimator(getContext(), R.animator.loading_dot_animation);
        dotAnim3.setTarget(dot3);
        dotAnim3.setStartDelay(600);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(dotAnim1, dotAnim2, dotAnim3);
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