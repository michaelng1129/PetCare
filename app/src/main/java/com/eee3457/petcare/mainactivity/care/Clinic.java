package com.eee3457.petcare.mainactivity.care;

import com.google.android.gms.maps.model.LatLng;

public class Clinic {
    private String name;
    private LatLng location;
    private double rating;
    private int reviews;
    private boolean openNow;
    private String placeId;
    private String distanceText;
    private String durationText;

    public Clinic(String name, LatLng location, double rating, int reviews, boolean openNow, String placeId) {
        this.name = name;
        this.location = location;
        this.rating = rating;
        this.reviews = reviews;
        this.openNow = openNow;
        this.placeId = placeId;
        this.distanceText = "距離未知";
        this.durationText = "行車時間未知";
    }

    // Getters
    public String getName() {
        return name;
    }

    public LatLng getLocation() {
        return location;
    }

    public double getRating() {
        return rating;
    }

    public int getReviews() {
        return reviews;
    }

    public boolean isOpenNow() {
        return openNow;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public String getDurationText() {
        return durationText;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }
}