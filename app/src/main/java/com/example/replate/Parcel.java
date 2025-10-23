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
    private List<String> tags; // 🔑 Dodaj to pole

    // Pusty konstruktor wymagany przez Firestore
    public Parcel() {}

    // Gettery
    @PropertyName("ownerconfirmed") // 🔑 wymuszamy nazwę z bazy
    public String getOwnerConfirmed() { return ownerconfirmed; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getTags() { return tags; } // 🔑 Dodaj getter

    // Settery potrzebne do automatycznego mapowania Firestore
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }

    public void setImageUrl(String imageUrl) {  // MUSI być setImageUrl (duże U)
        this.imageUrl = imageUrl;
        Log.d("ParcelSetter", "Setting imageUrl: " + imageUrl); // dodaj log
    }
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("ownerconfirmed") // 🔑 wymuszamy nazwę z bazy
    public void setOwnerConfirmed(String ownerconfirmed) { this.ownerconfirmed = ownerconfirmed; }
    public void setStatus(String status) { this.status = status; }
    public void setTags(List<String> tags) { this.tags = tags; } // 🔑 Dodaj setter
}