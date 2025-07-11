package com.example.replate;

public class Parcel {
    private String id;
    private String title;
    private String imageBase64;
    private String userId; // <== potrzebne do sprawdzania właściciela

    public Parcel(String id, String title, String imageBase64, String userId) {
        this.id = id;
        this.title = title;
        this.imageBase64 = imageBase64;
        this.userId = userId;
    }

    // Gettery
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getImageBase64() { return imageBase64; }
    public String getId() { return id; }
}
