package com.eee3457.petcare.mainactivity.home.healthtracker;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.eee3457.petcare.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HealthTrackerViewPager extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private HealthTrackerViewPagerAdapter adapter;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView petName, petAge;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_home_health_tracker_screen, container, false);
        tabLayout = view.findViewById(R.id.health_tabs);
        viewPager = view.findViewById(R.id.health_tracker_view_pager);
        petName = view.findViewById(R.id.pet_name);
        petAge = view.findViewById(R.id.pet_age);

        ArrayList<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new HealthMetricsScreen());
        fragmentList.add(new HealthVaccinationsScreen());
        fragmentList.add(new HealthMedicationsScreen());

        adapter = new HealthTrackerViewPagerAdapter(getChildFragmentManager(), getLifecycle(), fragmentList);
        viewPager.setAdapter(adapter);

        // Bind TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Metrics");
                    break;
                case 1:
                    tab.setText("Vaccinations");
                    break;
                case 2:
                    tab.setText("Medications");
                    break;
            }
        }).attach();

        // Load pet data from Firestore
        loadPetData();

        return view;
    }

    private void loadPetData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).collection("pets").limit(1) // Assume one pet for simplicity
                .get().addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No pets found, hide pet profile card
                        View petProfileCard = getView().findViewById(R.id.pet_profile_card);
                        if (petProfileCard != null) {
                            petProfileCard.setVisibility(View.GONE);
                        }
                    } else {
                        // Pet found, populate UI
                        DocumentSnapshot petDoc = querySnapshot.getDocuments().get(0);
                        String name = petDoc.getString("name");
                        String birthdate = petDoc.getString("birthdate");

                        // Set pet name
                        petName.setText(name != null ? name : "Unknown");

                        // Calculate age and display with breed
                        double age = calculateAge(birthdate);
                        String ageText;
                        if (age < 1.0) {
                            petAge.setText(String.format(Locale.US, "%.1f years old", age));
                        } else {
                            petAge.setText(String.format(Locale.US, "%.0f years old", age));
                        }
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load pet data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    View petProfileCard = getView().findViewById(R.id.pet_profile_card);
                    if (petProfileCard != null) {
                        petProfileCard.setVisibility(View.GONE);
                    }
                });
    }

    private double calculateAge(String birthdate) {
        if (birthdate == null) {
            return 0.0; // Default age if birthdate is missing
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
            Toast.makeText(requireContext(), "Invalid birthdate format: " + birthdate, Toast.LENGTH_SHORT).show();
            return 0.0;
        }
    }
}