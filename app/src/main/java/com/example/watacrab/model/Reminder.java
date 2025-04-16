package com.example.watacrab.model;

import java.io.Serializable;

public class Reminder implements Serializable {
    private int id;
    private String time;  // Format: "HH:mm"
    private String title;
    private String repeatInfo;
    private boolean enabled;

    // Empty constructor for Firebase
    public Reminder() {
    }

    public Reminder(int id, String time, String title, String repeatInfo, boolean enabled) {
        this.id = id;
        this.time = time;
        this.title = title;
        this.repeatInfo = repeatInfo;
        this.enabled = enabled;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRepeatInfo() {
        return repeatInfo;
    }

    public void setRepeatInfo(String repeatInfo) {
        this.repeatInfo = repeatInfo;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
} 