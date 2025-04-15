package com.eee3457.petcare.mainactivity.care;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;

import com.eee3457.petcare.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainCareScreen extends Fragment implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LinearLayout clinicsContainer;
    private LatLng lastSearchedLocation;
    private static final float SEARCH_DISTANCE_THRESHOLD = 5000; // Meters
    private static final float MAX_DISPLAY_DISTANCE_KM = 5.0f; // Max display distance 5 km

    // Permission request launcher
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableLocationFeatures();
                } else {
                    Toast.makeText(requireContext(), "Location permission required to show nearby vet clinics", Toast.LENGTH_SHORT).show();
                    setMapToDefaultLocation();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_care_screen, container, false);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Initialize CardView container
        clinicsContainer = view.findViewById(R.id.clinics_container);
        if (clinicsContainer == null) {
            Log.e("MainCareScreen", "clinicsContainer is null, check layout XML");
            Toast.makeText(requireContext(), "Layout error: Container not found", Toast.LENGTH_SHORT).show();
        }

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MainCareScreen", "SupportMapFragment not found in layout");
            Toast.makeText(requireContext(), "Failed to load map", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);

        // Set initial default location
        setMapToDefaultLocation();

        // Check location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocationFeatures();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableLocationFeatures() {
        try {
            // Enable "My Location" button
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }

            // Set map to current location and search for vet clinics
            setMapToCurrentLocation();
        } catch (SecurityException e) {
            Log.e("MainCareScreen", "SecurityException: " + e.getMessage());
            Toast.makeText(requireContext(), "Location permission error", Toast.LENGTH_SHORT).show();
            setMapToDefaultLocation();
        }
    }

    private void setMapToCurrentLocation() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        searchNearbyVetClinicsWithNearbySearch(currentLocation);
                    } else {
                        setMapToDefaultLocation();
                        Toast.makeText(requireContext(), "Cannot get current location, using default location", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    setMapToDefaultLocation();
                    Toast.makeText(requireContext(), "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                setMapToDefaultLocation();
            }
        } catch (SecurityException e) {
            Log.e("MainCareScreen", "SecurityException in setMapToCurrentLocation: " + e.getMessage());
            setMapToDefaultLocation();
        }
    }

    private void setMapToDefaultLocation() {
        LatLng defaultLocation = new LatLng(22.3906452, 114.1980672); // Hong Kong
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }

    @Override
    public void onCameraIdle() {
        LatLng newLocation = mMap.getCameraPosition().target;
        if (lastSearchedLocation == null || calculateDistance(lastSearchedLocation, newLocation) > SEARCH_DISTANCE_THRESHOLD) {
            searchNearbyVetClinicsWithNearbySearch(newLocation);
        }
    }

    private float calculateDistance(LatLng loc1, LatLng loc2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                loc1.latitude, loc1.longitude,
                loc2.latitude, loc2.longitude,
                results);
        return results[0];
    }

    private void searchNearbyVetClinicsWithNearbySearch(LatLng searchLocation) {
        lastSearchedLocation = searchLocation;

        // Build Nearby Search URL
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + searchLocation.latitude + "," + searchLocation.longitude +
                "&radius=5000" +
                "&type=veterinary_care" +
                "&key=" + getString(R.string.google_maps_key);

        // Send request using OkHttp
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to fetch vet clinics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Search failed: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String json = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray results = jsonObject.getJSONArray("results");
                    List<Clinic> clinics = new ArrayList<>();

                    // Limit to 10 clinics
                    for (int i = 0; i < results.length() && i < 10; i++) {
                        JSONObject place = results.getJSONObject(i);
                        JSONObject geometry = place.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");
                        String name = place.optString("name", "Unknown name");
                        double rating = place.optDouble("rating", 0.0);
                        int reviews = place.optInt("user_ratings_total", 0);
                        JSONObject openingHours = place.optJSONObject("opening_hours");
                        boolean openNow = openingHours != null && openingHours.optBoolean("open_now", false);
                        String placeId = place.optString("place_id", "");

                        clinics.add(new Clinic(name, new LatLng(lat, lng), rating, reviews, openNow, placeId));
                    }

                    // Fetch distances
                    fetchDistances(searchLocation, clinics);
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "JSON parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private float parseDistanceToKm(String distanceText) {
        try {
            // Remove non-numeric parts (e.g., "1.2 miles" -> "1.2")
            String numericPart = distanceText.replaceAll("[^0-9.]", "");
            float distance = Float.parseFloat(numericPart);

            // Check unit ("km" or "miles")
            if (distanceText.toLowerCase().contains("mi")) {
                // Convert miles to kilometers (1 mile = 1.60934 km)
                distance *= 1.60934f;
            }
            return distance;
        } catch (NumberFormatException e) {
            Log.e("MainCareScreen", "Failed to parse distance: " + distanceText, e);
            return Float.MAX_VALUE; // Invalid distance, ensure it’s filtered out
        }
    }

    private void fetchDistances(LatLng origin, List<Clinic> clinics) {
        if (clinics.isEmpty()) {
            updateUI(clinics);
            return;
        }

        // Build Distance Matrix URL
        StringBuilder destinations = new StringBuilder();
        for (int i = 0; i < clinics.size(); i++) {
            Clinic clinic = clinics.get(i);
            destinations.append(clinic.getLocation().latitude).append(",").append(clinic.getLocation().longitude);
            if (i < clinics.size() - 1) {
                destinations.append("|");
            }
        }

        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "origins=" + origin.latitude + "," + origin.longitude +
                "&destinations=" + destinations +
                "&mode=driving" +
                "&key=" + getString(R.string.google_maps_key);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to fetch distances: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI(clinics); // Update UI even on failure
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Distance request failed: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                        updateUI(clinics);
                    });
                    return;
                }

                String json = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray rows = jsonObject.getJSONArray("rows");
                    if (rows.length() > 0) {
                        JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
                        for (int i = 0; i < elements.length() && i < clinics.size(); i++) {
                            JSONObject element = elements.getJSONObject(i);
                            if (element.getString("status").equals("OK")) {
                                JSONObject distance = element.getJSONObject("distance");
                                JSONObject duration = element.getJSONObject("duration");
                                String distanceText = distance.getString("text");
                                String durationText = duration.getString("text");
                                clinics.get(i).setDistanceText(distanceText);
                                clinics.get(i).setDurationText(durationText);
                            }
                        }
                    }

                    // Filter clinics within 5 km
                    List<Clinic> filteredClinics = new ArrayList<>();
                    for (Clinic clinic : clinics) {
                        float distanceKm = parseDistanceToKm(clinic.getDistanceText());
                        if (distanceKm <= MAX_DISPLAY_DISTANCE_KM) {
                            filteredClinics.add(clinic);
                        }
                    }

                    requireActivity().runOnUiThread(() -> updateUI(filteredClinics));
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Distance parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateUI(clinics);
                    });
                }
            }
        });
    }

    private void updateUI(List<Clinic> clinics) {
        if (clinicsContainer == null) {
            Log.e("MainCareScreen", "clinicsContainer is null in updateUI");
            Toast.makeText(requireContext(), "Container not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear map and container
        mMap.clear();
        clinicsContainer.removeAllViews();

        // Show message if no clinics found
        if (clinics.isEmpty()) {
            Toast.makeText(requireContext(), "No vet clinics within 5 km", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update map markers and CardView
        for (Clinic clinic : clinics) {
            // Add map marker
            mMap.addMarker(new MarkerOptions()
                    .position(clinic.getLocation())
                    .title(clinic.getName()));

            // Create CardView
            View cardView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.fragment_main_care_screen_clinic_card, clinicsContainer, false);

            // Populate data
            TextView nameText = cardView.findViewById(R.id.clinic_name);
            TextView distanceText = cardView.findViewById(R.id.clinic_distance);
            TextView hoursText = cardView.findViewById(R.id.clinic_hours);
            TextView ratingText = cardView.findViewById(R.id.clinic_rating);
            TextView reviewsText = cardView.findViewById(R.id.clinic_reviews);
            Button callButton = cardView.findViewById(R.id.btn_call);
            Button directionsButton = cardView.findViewById(R.id.btn_directions);

            nameText.setText(clinic.getName());
            distanceText.setText(clinic.getDistanceText() + " • " + clinic.getDurationText());
            hoursText.setText(clinic.isOpenNow() ? "Open now" : "Closed");
            ratingText.setText(getStarRating(clinic.getRating()));
            reviewsText.setText("(" + clinic.getReviews() + " reviews)");

            // Button events
            callButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Call function not implemented", Toast.LENGTH_SHORT).show();
            });

            directionsButton.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + clinic.getLocation().latitude + "," + clinic.getLocation().longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(requireContext(), "Please install Google Maps", Toast.LENGTH_SHORT).show();
                }
            });

            // Add to container
            clinicsContainer.addView(cardView);
        }
    }

    private String getStarRating(double rating) {
        int stars = (int) Math.round(rating);
        StringBuilder starString = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            starString.append(i < stars ? "★" : "☆");
        }
        return starString.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up map
        if (mMap != null) {
            mMap.clear();
            mMap = null;
        }
    }
}