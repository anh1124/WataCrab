package com.example.watacrab.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.UUID;

public class Reminder implements Parcelable {
    private String id;
    private String title;
    private Date time;
    private boolean isActive;
    private boolean isDaily;
    private String userId;

    // Constructor mặc định
    public Reminder() {
        this.id = UUID.randomUUID().toString();
        this.isActive = true;
    }

    // Constructor đầy đủ
    public Reminder(String title, Date time, boolean isDaily, String userId) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.time = time;
        this.isActive = true;
        this.isDaily = isDaily;
        this.userId = userId;
    }

    // Constructor từ Parcel
    protected Reminder(Parcel in) {
        id = in.readString();
        title = in.readString();
        long tmpTime = in.readLong();
        time = tmpTime == -1 ? null : new Date(tmpTime);
        isActive = in.readByte() != 0;
        isDaily = in.readByte() != 0;
        userId = in.readString();
    }

    public static final Creator<Reminder> CREATOR = new Creator<Reminder>() {
        @Override
        public Reminder createFromParcel(Parcel in) {
            return new Reminder(in);
        }

        @Override
        public Reminder[] newArray(int size) {
            return new Reminder[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeLong(time != null ? time.getTime() : -1);
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeByte((byte) (isDaily ? 1 : 0));
        dest.writeString(userId);
    }
} 