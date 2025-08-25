package com.example.replate;

public class Parcel {
    private String id;
    private String title;
    private String imageBase64;
    private String userId;
    private String ownerconfirmed;
    private String status;

    // Pusty konstruktor wymagany przez Firestore
    public Parcel() {}

    // Konstruktor z podstawowymi polami (opcjonalny)
    public Parcel(String id, String title, String imageBase64, String userId) {
        this.id = id;
        this.title = title;
        this.imageBase64 = imageBase64;
        this.userId = userId;
    }

    // Gettery
    public String getOwnerConfirmed() { return ownerconfirmed; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getImageBase64() { return imageBase64; }
    public String getId() { return id; }
    public String getStatus() { return status; }

    // Settery potrzebne do automatycznego mapowania Firestore
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setOwnerConfirmed(String ownerconfirmed) { this.ownerconfirmed = ownerconfirmed; }
    public void setStatus(String status) { this.status = status; }
}
