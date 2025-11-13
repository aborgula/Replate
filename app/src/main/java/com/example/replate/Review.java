package com.example.replate;

public class Review {
    private int value;
    private String description;
    private String userId;
    private String authorLogin; // nowe pole

    public Review(int value, String description, String userId, String authorLogin) {
        this.value = value;
        this.description = description;
        this.userId = userId;
        this.authorLogin = authorLogin;
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
}
