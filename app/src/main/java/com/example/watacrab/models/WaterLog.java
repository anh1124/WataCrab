package com.example.watacrab.models;

import java.util.Date;

public class WaterLog {
    private String id;
    private String userId;
    private int amount; // in milliliters
    private Date timestamp;
    private String note;

    public WaterLog() {
        // Required empty constructor for Firestore
    }

    public WaterLog(String userId, int amount, String note) {
        this.userId = userId;
        this.amount = amount;
        this.timestamp = new Date();
        this.note = note;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
} 