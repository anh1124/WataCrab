package com.ptithcm.watacrab.model;

import java.io.Serializable;
import java.util.UUID;

public class Reminder implements Serializable {
    private String id;
    private String title;
    private int hour;
    private int minute;
    private boolean isActive;
    private boolean isDaily;

    public Reminder() {
        this.id = UUID.randomUUID().toString();
        this.title = "Uống nước";
        this.isActive = true;
        this.isDaily = true;
    }

    public Reminder(String title, int hour, int minute, boolean isActive, boolean isDaily) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.hour = hour;
        this.minute = minute;
        this.isActive = isActive;
        this.isDaily = isDaily;
    }

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

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
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

    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }
} 