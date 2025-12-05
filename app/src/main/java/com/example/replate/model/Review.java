package com.example.replate.model;

public class Review {
    private int value;
    private String description;
    private String userId;
    private String authorLogin;
    private String parcelTitle;
    private String parcelId; // 🟢 NOWE POLE

    public Review(int value, String description, String userId, String authorLogin, String parcelId, String parcelTitle) {
        this.value = value;
        this.description = description;
        this.userId = userId;
        this.authorLogin = authorLogin;
        this.parcelId = parcelId;
        this.parcelTitle = parcelTitle;
    }


    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuthorLogin() {
        return authorLogin;
    }

    public void setAuthorLogin(String authorLogin) {
        this.authorLogin = authorLogin;
    }

    public String getParcelTitle() {
        return parcelTitle;
    }

    public void setParcelTitle(String parcelTitle) {
        this.parcelTitle = parcelTitle;
    }

    public String getParcelId() {
        return parcelId;
    }

    public void setParcelId(String parcelId) {
        this.parcelId = parcelId;
    }
}
