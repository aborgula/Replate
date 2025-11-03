package com.example.replate;

import android.util.Log;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class Parcel {
    private String id;
    private String title;
    private String imageUrl;
    private String userId;
    private String ownerconfirmed;
    private String status;
    private List<String> tags;
    private String date;
    private String startTime;
    private String endTime;
    private double price; // 🔹 nowe pole

    // Pusty konstruktor wymagany przez Firestore
    public Parcel() {}

    // Gettery
    @PropertyName("ownerconfirmed")
    public String getOwnerConfirmed() { return ownerconfirmed; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getTags() { return tags; }

    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public double getPrice() { return price; } // 🔹 getter

    // Settery
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        Log.d("ParcelSetter", "Setting imageUrl: " + imageUrl);
    }
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("ownerconfirmed")
    public void setOwnerConfirmed(String ownerconfirmed) { this.ownerconfirmed = ownerconfirmed; }

    public void setStatus(String status) { this.status = status; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public void setDate(String date) { this.date = date; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setPrice(double price) { this.price = price; } // 🔹 setter
}
