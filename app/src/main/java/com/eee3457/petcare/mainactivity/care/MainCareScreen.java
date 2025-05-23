package com.eee3457.petcare.mainactivity.care;

import static com.eee3457.petcare.BuildConfig.MAPS_API_KEY;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eee3457.petcare.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
import java.util.Collections;
import java.util.List;

public class MainCareScreen extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LinearLayout clinicsContainer;
    private LatLng lastSearchedLocation;
    private static final float SEARCH_DISTANCE_THRESHOLD = 200; // Meters
    private static final float MAX_DISPLAY_DISTANCE_KM = 1.0f; // Max display distance 1 km
    private final OkHttpClient client = new OkHttpClient(); // Reusable OkHttpClient
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSearchCompleted = false; // Track search status
    private boolean isSearching = false; // Prevent concurrent searches
    private LocationCallback locationCallback; // For location updates

    // Permission request launcher
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            initializeLocationFeatures();
        } else {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
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
        mMap.getUiSettings().setScrollGesturesEnabled(false); // Disable map dragging
        mMap.setOnMyLocationButtonClickListener(this);
        // Do not trigger search until permission is granted
        checkAndHandleLocationPermission();
    }

    private void checkAndHandleLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initializeLocationFeatures();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void initializeLocationFeatures() {
        try {
            mMap.setMyLocationEnabled(true);
            // Initialize location updates
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        if (lastSearchedLocation == null || calculateDistance(lastSearchedLocation, newLocation) > SEARCH_DISTANCE_THRESHOLD) {
                            if (!isSearching) {
                                Log.d("MainCareScreen", "GPS location changed, updating map and searching: " + newLocation);
                                float currentZoom = mMap.getCameraPosition().zoom;
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, currentZoom > 0 ? currentZoom : 15));
                                searchNearbyVetClinicsWithNearbySearch(newLocation);
                            }
                        }
                    }
                }
            };

            LocationRequest locationRequest = new LocationRequest.Builder(2000) // Update every 2 seconds
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            // Initial location
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
        } catch (SecurityException e) {
            Log.e("MainCareScreen", "SecurityException: " + e.getMessage());
            Toast.makeText(requireContext(), "Location permission error", Toast.LENGTH_SHORT).show();
            setMapToDefaultLocation();
        }
    }

    private void setMapToDefaultLocation() {
        LatLng defaultLocation = new LatLng(22.3906452, 114.1980672); // Hong Kong
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5));
        // Do not trigger search or UI update
    }

    @Override
    public boolean onMyLocationButtonClick() {
        if (isSearching) {
            Log.d("MainCareScreen", "Search in progress, skipping my location button");
            return false;
        }
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d("MainCareScreen", "My location button clicked, moving to: " + currentLocation);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    searchNearbyVetClinicsWithNearbySearch(currentLocation);
                } else {
                    Toast.makeText(requireContext(), "Cannot get current location", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } catch (SecurityException e) {
            Log.e("MainCareScreen", "SecurityException in getLastLocation: " + e.getMessage());
            Toast.makeText(requireContext(), "Location permission error", Toast.LENGTH_SHORT).show();
            setMapToDefaultLocation();
        }

        return false; // Allow default behavior
    }

    private float calculateDistance(LatLng loc1, LatLng loc2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results);
        return results[0];
    }

    private float parseDistanceToKm(String distanceText) {
        if (distanceText == null || distanceText.isEmpty()) {
            Log.w("MainCareScreen", "Empty distance text, skipping parsing");
            return Float.MAX_VALUE;
        }
        try {
            String numericPart = distanceText.replaceAll("[^0-9.]", "");
            float distance = Float.parseFloat(numericPart);
            if (distanceText.toLowerCase().contains("mi")) {
                distance *= 1.60934f;
            }
            return distance;
        } catch (NumberFormatException e) {
            Log.e("MainCareScreen", "Failed to parse distance: " + distanceText, e);
            return Float.MAX_VALUE;
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

    private void searchNearbyVetClinicsWithNearbySearch(LatLng searchLocation) {
        if (isSearching) {
            Log.d("MainCareScreen", "Search in progress, skipping new search");
            return;
        }
        isSearching = true;
        lastSearchedLocation = searchLocation;
        isSearchCompleted = false; // Reset search status

        // Build Nearby Search URL
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" + "location=" + searchLocation.latitude + "," + searchLocation.longitude + "&radius=1000" + "&type=veterinary_care" + "&key=" + MAPS_API_KEY;

        // Send request using OkHttp
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Log.e("MainCareScreen", "Failed to fetch vet clinics: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to fetch vet clinics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isSearchCompleted = true;
                    isSearching = false;
                    updateUI(new ArrayList<>());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Log.e("MainCareScreen", "Search failed: HTTP " + response.code());
                        Toast.makeText(requireContext(), "Search failed: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                        isSearchCompleted = true;
                        isSearching = false;
                        updateUI(new ArrayList<>());
                    });
                    return;
                }

                String json = response.body().string();
                List<Clinic> clinics = new ArrayList<>();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray results = jsonObject.getJSONArray("results");

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

                    // Log fetched clinics
                    Log.d("MainCareScreen", "Fetched " + clinics.size() + " clinics from Nearby Search");

                    // Fetch distances
                    requireActivity().runOnUiThread(() -> fetchDistances(searchLocation, clinics));
                    requireActivity().runOnUiThread(() -> fetchPhoneNumbers(searchLocation, clinics));
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Log.e("MainCareScreen", "JSON parsing error in Nearby Search: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "JSON parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isSearchCompleted = true;
                        isSearching = false;
                        updateUI(new ArrayList<>());
                    });
                }
            }
        });
    }

    private void fetchDistances(LatLng origin, List<Clinic> clinics) {
        if (clinics.isEmpty()) {
            isSearchCompleted = true; // Mark search as completed
            isSearching = false;
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

        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?" + "origins=" + origin.latitude + "," + origin.longitude + "&destinations=" + destinations + "&mode=driving" + "&key=" + MAPS_API_KEY;

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Log.e("MainCareScreen", "Failed to fetch distances: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to fetch distances: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isSearchCompleted = true;
                    isSearching = false;
                    updateUI(clinics);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Log.e("MainCareScreen", "Distance request failed: HTTP " + response.code());
                        Toast.makeText(requireContext(), "Distance request failed: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                        isSearchCompleted = true;
                        isSearching = false;
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
                            } else {
                                clinics.get(i).setDistanceText("Unknown");
                                clinics.get(i).setDurationText("Unknown");
                            }
                        }
                    }

                    // Sort and filter clinics within 1 km
                    List<Clinic> filteredClinics = new ArrayList<>();
                    for (Clinic clinic : clinics) {
                        float distanceKm = parseDistanceToKm(clinic.getDistanceText());
                        if (distanceKm <= MAX_DISPLAY_DISTANCE_KM) {
                            filteredClinics.add(clinic);
                        }
                    }
                    Collections.sort(filteredClinics, (c1, c2) -> Float.compare(parseDistanceToKm(c1.getDistanceText()), parseDistanceToKm(c2.getDistanceText())));

                    // Log filtered and sorted clinics
                    Log.d("MainCareScreen", "Filtered and sorted " + filteredClinics.size() + " clinics within 1 km");

                    requireActivity().runOnUiThread(() -> {
                        isSearchCompleted = true; // Mark search as completed
                        isSearching = false;
                        updateUI(filteredClinics);
                    });
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Log.e("MainCareScreen", "JSON parsing error in Distance Matrix: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Distance parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isSearchCompleted = true;
                        isSearching = false;
                        updateUI(clinics);
                    });
                }
            }
        });
    }

    private void fetchPhoneNumbers(LatLng searchLocation, List<Clinic> clinics) {
        if (clinics.isEmpty()) {
            fetchDistances(searchLocation, clinics);
            return;
        }

        for (Clinic clinic : clinics) {
            String placeId = clinic.getPlaceId();
            String url = "https://maps.googleapis.com/maps/api/place/details/json?" + "place_id=" + placeId + "&fields=formatted_phone_number&key=" + MAPS_API_KEY;

            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.w("MainCareScreen", "Failed to fetch phone number for place_id: " + placeId);
                    // Continue processing even if one request fails
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.w("MainCareScreen", "Phone number request failed for place_id: " + placeId + ", HTTP " + response.code());
                        return;
                    }

                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject result = jsonObject.getJSONObject("result");
                        String phoneNumber = result.optString("formatted_phone_number", null);
                        clinic.setPhoneNumber(phoneNumber);
                    } catch (Exception e) {
                        Log.w("MainCareScreen", "JSON parsing error for phone number, place_id: " + placeId);
                    }
                }
            });
        }

        // Delay fetching distances to allow phone number requests to complete
        handler.postDelayed(() -> fetchDistances(searchLocation, clinics), 1000);
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

        // Show "no clinics" message in clinicsContainer if search is completed and no clinics found
        if (clinics.isEmpty() && isSearchCompleted) {
            // Set LinearLayout gravity to center vertically
            clinicsContainer.setGravity(Gravity.CENTER_VERTICAL);

            // Create TextView for message
            TextView noClinicsText = new TextView(requireContext());
            noClinicsText.setText("No vet clinics found within 1 km"); // Use string resource
            noClinicsText.setTextSize(18); // Match nearby_clinics_title
            noClinicsText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark)); // Theme color
            noClinicsText.setTypeface(null, android.graphics.Typeface.BOLD); // Match nearby_clinics_title
            noClinicsText.setGravity(Gravity.CENTER_HORIZONTAL); // Center horizontally
            noClinicsText.setPadding(16, 16, 16, 16); // Consistent padding

            // Set layout params with top margin
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = (int) (24 * requireContext().getResources().getDisplayMetrics().density); // 24dp
            noClinicsText.setLayoutParams(params);

            // Add TextView to clinicsContainer
            clinicsContainer.addView(noClinicsText);
            return;
        }

        // Reset LinearLayout gravity for clinic cards
        clinicsContainer.setGravity(Gravity.TOP);

        // Update map markers and CardView
        for (Clinic clinic : clinics) {
            // Validate location
            if (clinic.getLocation() == null) {
                Log.w("MainCareScreen", "Skipping clinic with null location: " + clinic.getName());
                continue;
            }

            // Add map marker
            mMap.addMarker(new MarkerOptions().position(clinic.getLocation()).title(clinic.getName()));

            // Create CardView
            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_main_care_screen_clinic_card, clinicsContainer, false);

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
                String phoneNumber = clinic.getPhoneNumber();
                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    Toast.makeText(requireContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sanitize phone number for Hong Kong (8 digits, no country code)
                String sanitizedPhoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                Log.d("MainCareScreen", "Original phone number: " + phoneNumber + ", Sanitized: " + sanitizedPhoneNumber);

                // Validate Hong Kong phone number (8 digits)
                if (sanitizedPhoneNumber.length() != 8 || !sanitizedPhoneNumber.matches("\\d{8}")) {
                    Toast.makeText(requireContext(), "Invalid phone number format", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show confirmation dialog before dialing
                String internationalPhoneNumber = "+852" + sanitizedPhoneNumber;
                new AlertDialog.Builder(requireContext())
                        .setTitle("Call Clinic")
                        .setMessage("Do you want to call " + clinic.getName() + " at " + sanitizedPhoneNumber + "?")
                        .setPositiveButton("Call", (dialog, which) -> {
                            // Create dial Intent
                            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                            dialIntent.setData(Uri.parse("tel:" + internationalPhoneNumber));

                            // Attempt to start the dialer
                            try {
                                startActivity(dialIntent);
                            } catch (ActivityNotFoundException e) {
                                Log.e("MainCareScreen", "Failed to start dialer: " + e.getMessage());
                                Toast.makeText(requireContext(), "Unable to open dialer", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setCancelable(true)
                        .show();
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

        // Center map on current location
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    float currentZoom = mMap.getCameraPosition().zoom;
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, currentZoom > 0 ? currentZoom : 15));
                }
            });
        } catch (SecurityException e) {
            Log.e("MainCareScreen", "SecurityException in getLastLocation: " + e.getMessage());
            Toast.makeText(requireContext(), "Location permission error", Toast.LENGTH_SHORT).show();
            setMapToDefaultLocation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMap != null) {
            mMap.clear();
            mMap = null;
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}