package com.example.watacrab.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Reminder implements Serializable {
    private String id;
    private String title;
    private Date time;
    private boolean isActive;
    private boolean isDaily;
    private String userId;

    // Empty constructor for Firebase
    public Reminder() {
        this.id = UUID.randomUUID().toString();
        this.isActive = true;
    }

    public Reminder(String title, Date time, boolean isDaily, String userId) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.time = time;
        this.isActive = true;
        this.isDaily = isDaily;
        this.userId = userId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isDaily() {
        return isDaily;
    }

    public void setDaily(boolean daily) {
        isDaily = daily;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Helper method to get formatted time string
    public String getTimeString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return time != null ? sdf.format(time) : "00:00";
    }
} 