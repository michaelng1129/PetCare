package com.eee3457.petcare.mainactivity.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.eee3457.petcare.R;
import com.eee3457.petcare.startactivity.StartActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainSettingsScreen extends Fragment {
    private LinearLayout profileItem, emailItem;
    private TextView profileNameTextView, emailTextView;
    private MaterialButton logoutButton;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_settings_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        profileItem = view.findViewById(R.id.profile_item);
        emailItem = view.findViewById(R.id.email_item);
        profileNameTextView = view.findViewById(R.id.profile_name);
        emailTextView = view.findViewById(R.id.email_address);
        logoutButton = view.findViewById(R.id.logout_button);

        // Update user info
        updateUserInfo();

        // Set logout button click listener
        logoutButton.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    private void updateUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        // Update profile name
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            profileNameTextView.setText(displayName);
        } else {
            profileNameTextView.setText("No name set");
        }
        // Update email
        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            emailTextView.setText(email);
        } else {
            emailTextView.setText("No email set");
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext()).setTitle("Confirm Logout").setMessage("Are you sure you want to log out?").setPositiveButton("Yes", (dialog, which) -> performLogout()).setNegativeButton("No", null).show();
    }

    private void performLogout() {
        mAuth.signOut();
        // Navigate to StartActivity (Login Screen)
        Intent intent = new Intent(requireContext(), StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}