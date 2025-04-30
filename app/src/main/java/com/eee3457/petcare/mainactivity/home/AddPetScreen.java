package com.eee3457.petcare.mainactivity.home;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eee3457.petcare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddPetScreen extends Fragment {
    private TextInputEditText nameInput, birthdateInput, notesInput;
    private TextInputLayout nameInputLayout, genderInputLayout, birthdateInputLayout, notesInputLayout;
    private MaterialAutoCompleteTextView genderInput;
    private MaterialButton saveButton;
    private ImageView petImage;
    private TextView changePhotoText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String petId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_home_add_pet_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            petId = args.getString("petId");
        }


        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        petImage = view.findViewById(R.id.pet_image);
        changePhotoText = view.findViewById(R.id.change_photo_text);
        nameInputLayout = view.findViewById(R.id.pet_name_input_layout);
        nameInput = view.findViewById(R.id.pet_name_input);
        genderInputLayout = view.findViewById(R.id.pet_gender_input_layout);
        genderInput = view.findViewById(R.id.pet_gender_input);
        birthdateInputLayout = view.findViewById(R.id.pet_birthdate_input_layout);
        birthdateInput = view.findViewById(R.id.pet_birthdate_input);
        notesInputLayout = view.findViewById(R.id.pet_notes_input_layout);
        notesInput = view.findViewById(R.id.pet_notes_input);
        saveButton = view.findViewById(R.id.save_button);

        // Setup gender dropdown
        String[] genders = {"Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, genders);
        genderInput.setAdapter(genderAdapter);
        genderInput.setThreshold(0); // Show suggestions immediately

        // Setup date picker for birthdate
        birthdateInput.setOnClickListener(v -> showDatePicker());
        birthdateInputLayout.setEndIconOnClickListener(v -> showDatePicker());

        // Setup change photo click (placeholder for future implementation)
        changePhotoText.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Photo upload not implemented yet", Toast.LENGTH_SHORT).show();
            // Future: Implement image picker and Firebase Storage upload
        });

        // Load pet data if editing
        if (petId != null) {
            loadPetData();
        }

        // Set save button click listener
        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                String name = nameInput.getText().toString().trim();
                String gender = genderInput.getText().toString().trim();
                String birthdate = birthdateInput.getText().toString().trim();
                String notes = notesInput.getText().toString().trim();
                savePetToFirestore(name, gender, birthdate, notes);
            }
        });
    }

    private void loadPetData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).collection("pets").document(petId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String gender = documentSnapshot.getString("gender");
                        String birthdate = documentSnapshot.getString("birthdate");
                        String notes = documentSnapshot.getString("notes");

                        // Populate the form
                        nameInput.setText(name);
                        genderInput.setText(gender);
                        birthdateInput.setText(birthdate);
                        notesInput.setText(notes);
                    } else {
                        Toast.makeText(requireContext(), "Pet not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load pet data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Pet Birthdate")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Default to today
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Format selected date as yyyy-MM-dd
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String formattedDate = sdf.format(new Date(selection));
            birthdateInput.setText(formattedDate);
        });

        datePicker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate name
        String name = nameInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Pet name is required");
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        // Validate gender
        String gender = genderInput.getText().toString().trim();
        if (TextUtils.isEmpty(gender)) {
            genderInputLayout.setError("Gender is required");
            isValid = false;
        } else if (!gender.equals("Male") && !gender.equals("Female")) {
            genderInputLayout.setError("Gender must be Male or Female");
            isValid = false;
        } else {
            genderInputLayout.setError(null);
        }

        // Validate birthdate
        String birthdate = birthdateInput.getText().toString().trim();
        if (TextUtils.isEmpty(birthdate)) {
            birthdateInputLayout.setError("Birthdate is required");
            isValid = false;
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setLenient(false);
                Date selectedDate = sdf.parse(birthdate);
                Date currentDate = new Date();
                if (selectedDate == null || selectedDate.after(currentDate)) {
                    birthdateInputLayout.setError("Birthdate must be in the past");
                    isValid = false;
                } else {
                    birthdateInputLayout.setError(null);
                }
            } catch (Exception e) {
                birthdateInputLayout.setError("Invalid date format");
                isValid = false;
            }
        }

        // Notes are optional, no validation required
        return isValid;
    }

    private void savePetToFirestore(String name, String gender, String birthdate, String notes) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        Map<String, Object> petData = new HashMap<>();
        petData.put("name", name);
        petData.put("gender", gender);
        petData.put("birthdate", birthdate);
        petData.put("notes", notes);
        petData.put("imageUrl", ""); // Placeholder for future image upload

        if (petId == null) {
            // Add new pet
            db.collection("users").document(userId).collection("pets").add(petData)
                    .addOnSuccessListener(docRef -> {
                        saveButton.setEnabled(true);
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Pet Added")
                                .setMessage("Your pet " + name + " has been added successfully.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    NavController navController = Navigation.findNavController(requireView());
                                    navController.navigate(R.id.action_addPetScreen_to_mainHomeScreen);
                                })
                                .setCancelable(false)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), "Failed to add pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Update existing pet
            db.collection("users").document(userId).collection("pets").document(petId)
                    .set(petData)
                    .addOnSuccessListener(aVoid -> {
                        saveButton.setEnabled(true);
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Pet Updated")
                                .setMessage("Your pet " + name + " has been updated successfully.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    NavController navController = Navigation.findNavController(requireView());
                                    navController.navigate(R.id.action_addPetScreen_to_mainHomeScreen);
                                })
                                .setCancelable(false)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), "Failed to update pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}