package com.eee3457.petcare.startactivity.auth;

import static android.content.ContentValues.TAG;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eee3457.petcare.R;
import com.eee3457.petcare.mainactivity.MainActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;


public class StartLoginScreen extends Fragment {
    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailLayout, passwordLayout;
    private TextView signupLink, forgotPasswordLink;
    private MaterialButton loginButton, googleSignInButton;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private FirebaseFirestore db; // Firestore instance


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start_login_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize Credential Manager
        credentialManager = CredentialManager.create(requireContext());

        // Initialize views
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        loginButton = view.findViewById(R.id.login_button);
        googleSignInButton = view.findViewById(R.id.google_button);


        // Set signup link click listener
        signupLink = view.findViewById(R.id.signup_link);
        signupLink.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_startLoginScreen_to_startSignUpScreen);
        });

        // Set forgot password link click listener
        forgotPasswordLink = view.findViewById(R.id.forgot_password);
        forgotPasswordLink.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_startLoginScreen_to_forgotPasswordScreen);
        });

        // Set login button click listener
        loginButton.setOnClickListener(v -> {
            if (validateForm()) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                loginWithFirebase(email, password);
            }
        });

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        // Configure Google Sign-In options
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(getString(R.string.default_web_client_id)).setAutoSelectEnabled(true).build();

        // Build Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build();

        // Execute Google Sign-In request with Credential Manager
        credentialManager.getCredentialAsync(requireContext(), request, null, Executors.newSingleThreadExecutor(), new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleGoogleSignInResult(result.getCredential());
            }

            @Override
            public void onError(GetCredentialException e) {
                Log.e(TAG, "Google Sign-In failed: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(requireContext(), "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleGoogleSignInResult(Credential credential) {

        if (credential instanceof CustomCredential customCredential && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            Bundle credentialData = customCredential.getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);
            if (!googleIdTokenCredential.getIdToken().isEmpty()) {
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());

            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(requireContext(), "Invalid Google ID token", Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(requireContext(), "Invalid credential type: " + credential.getClass().getSimpleName() + ". Please try again.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(requireActivity(), task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                saveUserToFirestore(user);
                // Sign-in successful, navigate to MainActivity
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Google Sign-In failed";
                Log.e(TAG, "Firebase auth failed: " + errorMessage);
                new MaterialAlertDialogBuilder(requireContext()).setTitle("Google Sign-In Error").setMessage("Failed to sign in with Google: " + errorMessage).setPositiveButton("OK", null).show();
            }
        });
    }
    private void saveUserToFirestore(FirebaseUser user) {
        // Split displayName into firstName and lastName
        String firstName = "";
        String lastName = "";
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            String[] nameParts = displayName.trim().split("\\s+", 2);
            firstName = nameParts[0];
            lastName = nameParts.length > 1 ? nameParts[1] : "";
        }

        // Use the same structure as storeUserData
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore under 'users' collection with UID as document ID
        db.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully written to Firestore for UID: " + user.getUid());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing user data to Firestore: " + e.getMessage(), e);
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
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task -> {
            loginButton.setEnabled(true);
            if (task.isSuccessful()) {
                // Login successful, navigate to MainActivity
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed";
                new AlertDialog.Builder(requireContext()).setTitle("Login Error").setMessage("Failed to login: " + errorMessage).setPositiveButton("OK", null).show();
                if (errorMessage.contains("no user record")) {
                    emailLayout.setError("No account found with this email");
                } else if (errorMessage.contains("password is invalid")) {
                    passwordLayout.setError("Incorrect password");
                }
            }
        });
    }

}