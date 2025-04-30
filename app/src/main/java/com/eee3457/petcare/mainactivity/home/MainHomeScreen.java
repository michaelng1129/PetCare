package com.eee3457.petcare.mainactivity.home;

import static android.content.ContentValues.TAG;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eee3457.petcare.R;
import com.eee3457.petcare.mainactivity.home.healthtracker.HealthTrackerViewPager;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainHomeScreen extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LinearLayout noPetsView;
    private androidx.cardview.widget.CardView petProfileCard;
    private GridLayout quickActions;
    private MaterialButton addPetButton;
    private TextView upcomingTitle, petName, petDetails;
    private LinearLayout upcomingEventsContainer, healthStatsSection;
    private String petId;
    private ListenerRegistration petDataListener;
    private List<ListenerRegistration> eventListeners = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_home_screen, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        noPetsView = view.findViewById(R.id.no_pets_view);
        petProfileCard = view.findViewById(R.id.pet_profile_card);
        quickActions = view.findViewById(R.id.quick_actions);
        upcomingTitle = view.findViewById(R.id.upcoming_title);
        upcomingEventsContainer = view.findViewById(R.id.upcomingEventsContainer);
        healthStatsSection = view.findViewById(R.id.health_stats_section);
        petName = view.findViewById(R.id.pet_name);
        petDetails = view.findViewById(R.id.pet_details);
        addPetButton = view.findViewById(R.id.add_pet_button);

        // Set add pet button click listener
        addPetButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_mainHomeScreen_to_addPetScreen);
        });

        // Set pet profile card click listener to edit pet
        petProfileCard.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            Bundle bundle = new Bundle();
            bundle.putString("petId", petId);
            navController.navigate(R.id.action_mainHomeScreen_to_addPetScreen, bundle);
        });


        // Set health action click listener
        View healthAction = view.findViewById(R.id.health_action);
        healthAction.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            AppBarLayout appBarLayout = requireActivity().findViewById(R.id.appBarLayout);
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
            if (appBarLayout != null) {
                appBarLayout.setVisibility(View.GONE);
            }
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            navController.navigate(R.id.action_mainHomeScreen_to_healthTrackerViewPager);
        });

        View videoAction = view.findViewById(R.id.video_action);
        videoAction.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            AppBarLayout appBarLayout = requireActivity().findViewById(R.id.appBarLayout);
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
            if (appBarLayout != null) {
                appBarLayout.setVisibility(View.GONE);
            }
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            navController.navigate(R.id.action_mainHomeScreen_to_clipsScreen);

        });

        // Load pet data from Firestore
        loadPetData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh pet data when returning to this fragment
        loadPetData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listeners to prevent memory leaks
        if (petDataListener != null) {
            petDataListener.remove();
        }
        for (ListenerRegistration listener : eventListeners) {
            listener.remove();
        }
        eventListeners.clear();
    }

    private void loadPetData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            }
            Log.e(TAG, "User not authenticated");
            return;
        }

        // Remove previous listener if exists
        if (petDataListener != null) {
            petDataListener.remove();
        }

        petDataListener = db.collection("users").document(userId).collection("pets").limit(1) // Assume one pet for simplicity
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to load pet data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        Log.e(TAG, "Failed to load pet data: " + e.getMessage(), e);
                        updateUIForNoPet();
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        updateUIForNoPet();
                        petId = null;
                        Log.d(TAG, "No pets found, petId reset to null");
                    } else {
                        // Pet found, populate UI
                        noPetsView.setVisibility(View.GONE);
                        petProfileCard.setVisibility(View.VISIBLE);
                        quickActions.setVisibility(View.VISIBLE);
                        upcomingTitle.setVisibility(View.VISIBLE);
                        upcomingEventsContainer.setVisibility(View.VISIBLE);
                        healthStatsSection.setVisibility(View.VISIBLE);

                        DocumentSnapshot petDoc = querySnapshot.getDocuments().get(0);
                        String name = petDoc.getString("name");
                        String birthdate = petDoc.getString("birthdate");
                        petId = petDoc.getId();
                        Log.d(TAG, "Loaded petId: " + petId + ", Name: " + name + ", Birthdate: " + birthdate);

                        // Set pet name
                        petName.setText(name != null ? name : "Unknown");

                        // Calculate age from birthdate and display
                        double age = calculateAge(birthdate);
                        if (age < 1.0) {
                            petDetails.setText(String.format(Locale.US, "%.1f years old", age));
                        } else {
                            petDetails.setText(String.format(Locale.US, "%.0f years old", age));
                        }

                        // Check and upload fake events if none exist
                        checkAndUploadFakeEvents(userId, petId);

                        // Load upcoming events
                        loadUpcomingEvents(userId, petId);
                    }
                });
    }

    private void updateUIForNoPet() {
        if (isAdded()) {
            noPetsView.setVisibility(View.VISIBLE);
            petProfileCard.setVisibility(View.GONE);
            quickActions.setVisibility(View.GONE);
            upcomingTitle.setVisibility(View.GONE);
            upcomingEventsContainer.setVisibility(View.GONE);
            healthStatsSection.setVisibility(View.GONE);
        }
    }

    private void checkAndUploadFakeEvents(String userId, String petId) {
        ListenerRegistration listener = db.collection("users").document(userId).collection("pets").document(petId).collection("events").addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Failed to check events: " + e.getMessage(), e);
                return;
            }

            if (querySnapshot == null || querySnapshot.isEmpty()) {
                // No events found, upload fake data
                List<Map<String, Object>> fakeEvents = generateFakeEvents();
                for (Map<String, Object> event : fakeEvents) {
                    db.collection("users").document(userId).collection("pets").document(petId).collection("events").add(event).addOnSuccessListener(docRef -> {
                        Log.d(TAG, "Fake event added with ID: " + docRef.getId());
                    }).addOnFailureListener(e1 -> {
                        Log.e(TAG, "Failed to add fake event: " + e1.getMessage(), e1);
                    });
                }
            } else {
                Log.d(TAG, "Events already exist, skipping fake data upload");
            }
        });
        eventListeners.add(listener);
    }

    private List<Map<String, Object>> generateFakeEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        Random random = new Random();
        String[] titles = {"Vet Appointment", "Flea Medication", "Grooming Session"};
        String[] descriptions = {"Annual checkup with Dr. Smith", "Administer monthly flea treatment", "Bath and grooming at Pet Salon"};
        String[] icons = {"ic_medical_services", "ic_medication", "ic_shower"};

        // Generate 3 fake events in 2025
        for (int i = 0; i < 3; i++) {
            Map<String, Object> event = new HashMap<>();
            event.put("title", titles[i]);
            event.put("description", descriptions[i]);
            event.put("iconResId", icons[i]);

            // Random date in 2025 (May to July)
            int month = 5 + i; // May (5), June (6), July (7)
            int day = random.nextInt(28) + 1; // 1 to 28
            event.put("day", String.valueOf(day));
            event.put("month", String.valueOf(month));
            event.put("year", "2025");

            events.add(event);
        }
        return events;
    }

    private double calculateAge(String birthdate) {
        if (birthdate == null) {
            return 0.0;
        }
        try {
            // Parse birthdate (format: yyyy-MM-dd)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date birthDate = sdf.parse(birthdate);

            // Use current system date instead of fixed date
            Calendar currentCal = Calendar.getInstance();

            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);

            // Calculate age
            int years = currentCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            int months = currentCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH);
            int days = currentCal.get(Calendar.DAY_OF_MONTH) - birthCal.get(Calendar.DAY_OF_MONTH);

            if (months < 0 || (months == 0 && days < 0)) {
                years--;
                months += 12;
            }

            if (days < 0) {
                months--;
                Calendar tempCal = (Calendar) currentCal.clone();
                tempCal.add(Calendar.MONTH, -1);
                days += tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            }

            if (years < 1) {
                double totalMonths = months + days / 30.0;
                double fractionalAge = totalMonths / 12.0;
                return Math.round(fractionalAge * 10) / 10.0; // Keep 1 decimal
            }

            return years;
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Invalid birthdate format: " + birthdate, Toast.LENGTH_SHORT).show();
            }
            return 0.0;
        }
    }

    private void loadUpcomingEvents(String userId, String petId) {
        ListenerRegistration listener = db.collection("users").document(userId).collection("pets").document(petId).collection("events").addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "Failed to load events: " + e.getMessage(), e);
                return;
            }

            if (isAdded()) {
                upcomingEventsContainer.removeAllViews();
            }
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                String title = doc.getString("title");
                String description = doc.getString("description");
                String day = doc.getString("day");
                String month = doc.getString("month");
                String year = doc.getString("year");
                String iconResId = doc.getString("iconResId"); // Store as string or map to resource

                if (isAdded()) {
                    View cardView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_main_home_screen_event_card, upcomingEventsContainer, false);
                    TextView titleView = cardView.findViewById(R.id.eventTitle);
                    TextView descView = cardView.findViewById(R.id.eventDesc);
                    TextView dayView = cardView.findViewById(R.id.eventDay);
                    TextView monthView = cardView.findViewById(R.id.eventMonth);
                    ImageView iconView = cardView.findViewById(R.id.eventIcon);

                    titleView.setText(title);
                    descView.setText(description);
                    dayView.setText(day);
                    monthView.setText(convertMonthNumberToName(month));
                    // Map iconResId to actual resource (simplified example)
                    if ("ic_medical_services".equals(iconResId)) {
                        iconView.setImageResource(R.drawable.ic_medical_services);
                    } else if ("ic_medication".equals(iconResId)) {
                        iconView.setImageResource(R.drawable.ic_medication);
                    } else if ("ic_shower".equals(iconResId)) {
                        iconView.setImageResource(R.drawable.ic_shower);
                    }

                    upcomingEventsContainer.addView(cardView);
                }
            }
        });
        eventListeners.add(listener);
    }

    private String convertMonthNumberToName(String monthNumber) {
        try {
            int month = Integer.parseInt(monthNumber);
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            if (month >= 1 && month <= 12) {
                return months[month - 1];
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid month number: " + monthNumber, e);
        }
        return ""; // fallback
    }
}