package com.example.replate;

public class User {
    private String uid;
    private String username;
    private String photoUrl;
    private String profileImageBase64;
    private Double rating;

    public User(String uid, String username, String photoUrl, String profileImageBase64, double rating) {
        this.uid = uid;
        this.username = username;
        this.photoUrl = photoUrl;
        this.profileImageBase64 = profileImageBase64;
        this.rating = rating;
    }

    public String getUid() { return uid; }
    public String getUsername() { return username; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public Double getRating() { return rating; }

    // setters
    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }
}