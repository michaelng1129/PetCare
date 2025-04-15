package com.eee3457.petcare.mainactivity.home;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eee3457.petcare.R;
import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;

public class MainHomeScreen extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_home_screen, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<UpcomingEvent> upcomingEvents = new ArrayList<>();
        upcomingEvents.add(new UpcomingEvent("Vet Checkup", "Annual wellness exam", "15", "5", "2025", R.drawable.ic_medical_services));
        upcomingEvents.add(new UpcomingEvent("Vaccination", "Rabies booster", "22", "5", "2025", R.drawable.ic_medication));
        upcomingEvents.add(new UpcomingEvent("Grooming", "Nail trim", "3", "6", "2025", R.drawable.ic_shower));


        LinearLayout containerLayout = view.findViewById(R.id.upcomingEventsContainer);

        for (UpcomingEvent event : upcomingEvents) {
            View cardView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_main_home_screen_event_card, containerLayout, false);

            TextView title = cardView.findViewById(R.id.eventTitle);
            TextView desc = cardView.findViewById(R.id.eventDesc);
            TextView day = cardView.findViewById(R.id.eventDay);
            TextView month = cardView.findViewById(R.id.eventMonth);
            ImageView icon = cardView.findViewById(R.id.eventIcon);

            title.setText(event.getTitle());
            desc.setText(event.getDescription());
            day.setText(event.getDay());
            month.setText(convertMonthNumberToName(event.getMonth()));
            icon.setImageResource(event.getIconResId());

            containerLayout.addView(cardView);
        }
    }

    private String convertMonthNumberToName(String monthNumber) {
        int month = Integer.parseInt(monthNumber);
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        if (month >= 1 && month <= 12) {
            return months[month - 1];
        } else {
            return ""; // fallback
        }
    }

}