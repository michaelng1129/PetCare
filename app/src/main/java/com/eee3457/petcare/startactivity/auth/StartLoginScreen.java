package com.eee3457.petcare.startactivity.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eee3457.petcare.R;
import com.eee3457.petcare.mainactivity.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class StartLoginScreen extends Fragment {
    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialButton loginButton;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start_login_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        loginButton = view.findViewById(R.id.login_button);

        // Set signup link click listener
        TextView signupLink = view.findViewById(R.id.signup_link);
        signupLink.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_startLoginScreen_to_startSignUpScreen);
        });

        // Set login button click listener
        loginButton.setOnClickListener(v -> {
            if (validateForm()) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                loginWithFirebase(email, password);
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate email
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        // Validate password
        String password = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        return isValid;
    }

    private void loginWithFirebase(String email, String password) {
        loginButton.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        // Login successful, navigate to MainActivity
                        Intent intent = new Intent(requireContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Login Error")
                                .setMessage("Failed to login: " + errorMessage)
                                .setPositiveButton("OK", null)
                                .show();
                        if (errorMessage.contains("no user record")) {
                            emailLayout.setError("No account found with this email");
                        } else if (errorMessage.contains("password is invalid")) {
                            passwordLayout.setError("Incorrect password");
                        }
                    }
                });
    }
}