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
    private static final float SEARCH_DISTANCE_THRESHOLD = 500;
    private static final float MAX_DISPLAY_DISTANCE_KM = 5.0f;

    // 權限請求
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableLocationFeatures();
                } else {
                    Toast.makeText(requireContext(), "需要位置權限以顯示附近獸醫店", Toast.LENGTH_SHORT).show();
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

        // 初始化位置服務
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // 初始化 CardView 容器
        clinicsContainer = view.findViewById(R.id.clinics_container);
        if (clinicsContainer == null) {
            Log.e("MainCareScreen", "clinicsContainer is null, check layout XML");
            Toast.makeText(requireContext(), "布局錯誤：無法找到容器", Toast.LENGTH_SHORT).show();
        }

        // 初始化地圖
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MainCareScreen", "SupportMapFragment not found in layout");
            Toast.makeText(requireContext(), "無法載入地圖", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);

        // 初始顯示預設位置
        setMapToDefaultLocation();

        // 檢查位置權限
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocationFeatures();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableLocationFeatures() {
        try {
            // 啟用“我的位置”按鈕
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }

            // 設置當前位置並搜尋獸醫店
            setMapToCurrentLocation();
        } catch (SecurityException e) {
            Log.e("MainCareScreen", "SecurityException: " + e.getMessage());
            Toast.makeText(requireContext(), "位置權限錯誤", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(), "無法獲取當前位置，使用預設位置", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    setMapToDefaultLocation();
                    Toast.makeText(requireContext(), "位置錯誤: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        LatLng defaultLocation = new LatLng(22.3906452, 114.1980672); // 香港
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

        // 構建 Nearby Search URL
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + searchLocation.latitude + "," + searchLocation.longitude +
                "&radius=5000" +
                "&type=veterinary_care" +
                "&key=" + getString(R.string.google_maps_key);

        // 使用 OkHttp 發送請求
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "無法獲取獸醫店: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "搜尋失敗: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String json = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray results = jsonObject.getJSONArray("results");
                    List<Clinic> clinics = new ArrayList<>();

                    for (int i = 0; i < results.length() && i < 10; i++) { // 限制最多 10 個診所
                        JSONObject place = results.getJSONObject(i);
                        JSONObject geometry = place.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");
                        String name = place.optString("name", "未知名稱");
                        double rating = place.optDouble("rating", 0.0);
                        int reviews = place.optInt("user_ratings_total", 0);
                        JSONObject openingHours = place.optJSONObject("opening_hours");
                        boolean openNow = openingHours != null && openingHours.optBoolean("open_now", false);
                        String placeId = place.optString("place_id", "");

                        clinics.add(new Clinic(name, new LatLng(lat, lng), rating, reviews, openNow, placeId));
                    }

                    // 獲取距離
                    fetchDistances(searchLocation, clinics);
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "JSON 解析錯誤: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private float parseDistanceToKm(String distanceText) {
        try {
            // 移除非數字和單位（例如 "1.2 miles" -> "1.2"）
            String numericPart = distanceText.replaceAll("[^0-9.]", "");
            float distance = Float.parseFloat(numericPart);

            // 檢查單位（"km" 或 "miles"）
            if (distanceText.toLowerCase().contains("mi")) {
                // 英里轉公里（1 英里 = 1.60934 公里）
                distance *= 1.60934f;
            }
            return distance;
        } catch (NumberFormatException e) {
            Log.e("MainCareScreen", "無法解析距離: " + distanceText, e);
            return Float.MAX_VALUE; // 無效距離，確保被過濾掉
        }
    }

    private void fetchDistances(LatLng origin, List<Clinic> clinics) {
        if (clinics.isEmpty()) {
            updateUI(clinics);
            return;
        }

        // 構建 Distance Matrix URL
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
                    Toast.makeText(requireContext(), "無法獲取距離: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI(clinics); // 即使失敗也更新 UI
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "距離請求失敗: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
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

                    // 過濾距離 ≤ 5 公里的診所
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
                        Toast.makeText(requireContext(), "距離解析錯誤: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateUI(clinics);
                    });
                }
            }
        });
    }

    private void updateUI(List<Clinic> clinics) {
        if (clinicsContainer == null) {
            Log.e("MainCareScreen", "clinicsContainer is null in updateUI");
            Toast.makeText(requireContext(), "容器未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        // 清空地圖和容器
        mMap.clear();
        clinicsContainer.removeAllViews();

        // 如果無診所，顯示提示
        if (clinics.isEmpty()) {
            Toast.makeText(requireContext(), "附近 5 公里內無獸醫診所", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新地圖標記和 CardView
        for (Clinic clinic : clinics) {
            // 添加地圖標記
            mMap.addMarker(new MarkerOptions()
                    .position(clinic.getLocation())
                    .title(clinic.getName()));

            // 創建 CardView
            View cardView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.fragment_main_care_screen_clinic_card, clinicsContainer, false);

            // 填充資料
            TextView nameText = cardView.findViewById(R.id.clinic_name);
            TextView distanceText = cardView.findViewById(R.id.clinic_distance);
            TextView hoursText = cardView.findViewById(R.id.clinic_hours);
            TextView ratingText = cardView.findViewById(R.id.clinic_rating);
            TextView reviewsText = cardView.findViewById(R.id.clinic_reviews);
            Button callButton = cardView.findViewById(R.id.btn_call);
            Button directionsButton = cardView.findViewById(R.id.btn_directions);

            nameText.setText(clinic.getName());
            distanceText.setText(clinic.getDistanceText() + " • " + clinic.getDurationText());
            hoursText.setText(clinic.isOpenNow() ? "現正營業" : "目前關閉");
            ratingText.setText(getStarRating(clinic.getRating()));
            reviewsText.setText("(" + clinic.getReviews() + " reviews)");

            // 按鈕事件
            callButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "電話功能待實現", Toast.LENGTH_SHORT).show();
            });

            directionsButton.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + clinic.getLocation().latitude + "," + clinic.getLocation().longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(requireContext(), "請安裝 Google 地圖", Toast.LENGTH_SHORT).show();
                }
            });

            // 添加到容器
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
        // 清理地圖
        if (mMap != null) {
            mMap.clear();
            mMap = null;
        }
    }
}