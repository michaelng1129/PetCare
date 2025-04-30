package com.eee3457.petcare.startactivity.auth;

import static android.content.ContentValues.TAG;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eee3457.petcare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordScreen extends Fragment {
    private TextInputEditText emailInput;
    private TextInputLayout emailInputLayout;
    private MaterialButton resetPasswordButton;
    private TextView backToLoginText;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start_forgot_password_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        emailInputLayout = view.findViewById(R.id.email_input_layout);
        emailInput = view.findViewById(R.id.email_input);
        resetPasswordButton = view.findViewById(R.id.reset_password_button);
        backToLoginText = view.findViewById(R.id.back_to_login_text);

        // Set reset password button click listener
        resetPasswordButton.setOnClickListener(v -> {
            if (validateEmail()) {
                String email = emailInput.getText().toString().trim();
                sendPasswordResetEmail(email);
            }
        });

        // Set back to login click listener
        backToLoginText.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_forgotPasswordScreen_to_startLoginScreen);
        });
    }

    private boolean validateEmail() {
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Invalid email format");
            return false;
        } else {
            emailInputLayout.setError(null);
            return true;
        }
    }

    private void sendPasswordResetEmail(String email) {
        resetPasswordButton.setEnabled(false);
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            resetPasswordButton.setEnabled(true);
            if (task.isSuccessful()) {
                // Show success dialog
                new MaterialAlertDialogBuilder(requireContext()).setTitle("Reset Password Email Sent").setMessage("A password reset link has been sent to " + email + ". Please check your inbox and follow the instructions.").setPositiveButton("OK", (dialog, which) -> {
                    // Navigate back to login screen
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.action_forgotPasswordScreen_to_startLoginScreen);
                }).setCancelable(false).show();
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                Log.e(TAG, "Password reset failed: " + errorMessage);
                // Show error dialog
                new MaterialAlertDialogBuilder(requireContext()).setTitle("Reset Password Error").setMessage("Failed to send password reset email: " + errorMessage).setPositiveButton("OK", null).show();
                if (errorMessage.contains("no user record")) {
                    emailInputLayout.setError("No account found with this email");
                }
            }
        });
    }
}