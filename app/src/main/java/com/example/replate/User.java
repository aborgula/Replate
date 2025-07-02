package com.example.replate;

public class User {
    private String uid;
    private String username;
    private String photoUrl;
    private Double rating;

    public User(String uid, String username, String photoUrl, double rating) {
        this.uid = uid;
        this.username = username;
        this.photoUrl = photoUrl;
        this.rating = rating;
    }

    // getters
    public String getUid() { return uid; }
    public String getUsername() { return username; }
    public String getPhotoUrl() { return photoUrl; }
    public Double getRating() { return rating; }
}


