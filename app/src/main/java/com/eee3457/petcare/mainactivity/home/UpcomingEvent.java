package com.eee3457.petcare.mainactivity.home;

public class UpcomingEvent {
    private String title;
    private String description;
    private String day;
    private String month;
    private String year;
    private int iconResId;

    public UpcomingEvent(String title, String description, String day, String month, String year, int iconResId) {
        this.title = title;
        this.description = description;
        this.day = day;
        this.month = month;
        this.year = year;
        this.iconResId = iconResId;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDay() {
        return day;
    }

    public String getMonth() {
        return month;
    }

    public String getYear() {
        return year;
    }

    public int getIconResId() {
        return iconResId;
    }
}
