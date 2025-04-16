package com.example.watacrab.models;

import java.util.Date;

public class User {
    private String id;
    private String email;
    private String username;
    private Date createdAt;

    // Empty constructor required for Firestore
    public User() {}

    public User(String id, String email, String username) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 