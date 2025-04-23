package com.eee3457.petcare.startactivity.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eee3457.petcare.R;
import com.eee3457.petcare.mainactivity.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StartSignUpScreen extends Fragment {
    private TextInputEditText firstNameInput, lastNameInput, emailInput, passwordInput;
    private TextInputLayout firstNameLayout, lastNameLayout, emailLayout, passwordLayout;
    private MaterialCheckBox termsCheckbox;
    private MaterialButton signupButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start_sign_up_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        firstNameLayout = view.findViewById(R.id.first_name_layout);
        lastNameLayout = view.findViewById(R.id.last_name_layout);
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        firstNameInput = view.findViewById(R.id.first_name_input);
        lastNameInput = view.findViewById(R.id.last_name_input);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        termsCheckbox = view.findViewById(R.id.terms_checkbox);
        signupButton = view.findViewById(R.id.signup_button);

        // Set signup button click listener
        signupButton.setOnClickListener(v -> {
            if (validateForm()) {
                String firstName = firstNameInput.getText().toString().trim();
                String lastName = lastNameInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                signupWithFirebase(firstName, lastName, email, password);
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate first name
        String firstName = firstNameInput.getText().toString().trim();
        if (TextUtils.isEmpty(firstName)) {
            firstNameLayout.setError("First name is required");
            isValid = false;
        } else {
            firstNameLayout.setError(null);
        }

        // Validate last name
        String lastName = lastNameInput.getText().toString().trim();
        if (TextUtils.isEmpty(lastName)) {
            lastNameLayout.setError("Last name is required");
            isValid = false;
        } else {
            lastNameLayout.setError(null);
        }

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
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        // Validate terms checkbox
        if (!termsCheckbox.isChecked()) {
            termsCheckbox.setError("You must agree to the terms");
            new AlertDialog.Builder(requireContext()).setTitle("Terms Required").setMessage("Please agree to the Terms of Service to continue.").setPositiveButton("OK", null).show();
            isValid = false;
        } else {
            termsCheckbox.setError(null);
        }

        return isValid;
    }

    private void signupWithFirebase(String firstName, String lastName, String email, String password) {
        signupButton.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    // Update user profile
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(firstName + " " + lastName).build();
                    user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                        if (profileTask.isSuccessful()) {
                            // Store user data in Firestore
                            storeUserData(user.getUid(), firstName, lastName, email);
                            // Navigate to MainActivity
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        } else {
                            signupButton.setEnabled(true);
                            new AlertDialog.Builder(requireContext()).setTitle("Sign Up error").setMessage("Failed to Sign Up: " + profileTask.getException().getMessage()).setPositiveButton("OK", null).show();
                        }
                    });
                }
            } else {
                signupButton.setEnabled(true);
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Sign up failed";
                new AlertDialog.Builder(requireContext()).setTitle("Sign Up Error").setMessage(errorMessage).setPositiveButton("OK", null).show();
                if (errorMessage.contains("email address is already in use")) {
                    emailLayout.setError("Email is already registered");
                } else if (errorMessage.contains("password is invalid")) {
                    passwordLayout.setError("Password is too weak");
                }

            }
        });
    }

    private void storeUserData(String uid, String firstName, String lastName, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid).set(userData).addOnSuccessListener(aVoid -> Log.d("Firestore", "User data stored successfully")).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to store user data: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}