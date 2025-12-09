package com.example.replate.repository;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore db;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public UserRepository(FirebaseFirestore firestore) {
        this.db = firestore;
    }


    public interface OnUserRatingLoadedListener {
        void onSuccess(Double rating);
        void onNoRating();
        void onFailure(Exception e);
    }

    public void getUserRating(String userId, OnUserRatingLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onNoRating();
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double rating = documentSnapshot.getDouble("rating");
                        if (rating != null) {
                            listener.onSuccess(rating);
                        } else {
                            listener.onNoRating();
                        }
                    } else {
                        listener.onNoRating();
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnLikedUsersLoadedListener {
        void onSuccess(List<String> likedUsers);
        void onFailure(Exception e);
    }

    public void getLikedUsers(String userId, OnLikedUsersLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                        if (likedUsers != null) {
                            listener.onSuccess(likedUsers);
                        } else {
                            listener.onSuccess(new ArrayList<>());
                        }
                    } else {
                        listener.onSuccess(new ArrayList<>());
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnUserDataLoadedListener {
        void onSuccess(UserData userData);
        void onFailure(Exception e);
    }

    public static class UserData {
        public String address;
        public String phone;
        public String login;
        public String stripeAccountId;
        public Boolean notificationsEnabled;

        public UserData() {}
    }

    public void getUserData(String userId, OnUserDataLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("UserId is null or empty"));
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserData userData = new UserData();
                        userData.address = documentSnapshot.getString("address");
                        userData.phone = documentSnapshot.getString("phone");
                        userData.login = documentSnapshot.getString("login");
                        userData.stripeAccountId = documentSnapshot.getString("stripeAccountId");
                        userData.notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");

                        listener.onSuccess(userData);
                    } else {
                        listener.onFailure(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnUserUpdateListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void updateUserData(String userId, Map<String, Object> updates, OnUserUpdateListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("UserId is null or empty"));
            return;
        }

        if (updates == null || updates.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("No updates provided"));
            return;
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void updateNotificationSettings(String userId, boolean enabled, OnUserUpdateListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("UserId is null or empty"));
            return;
        }

        db.collection("users").document(userId)
                .update("notificationsEnabled", enabled)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void deleteUserDocument(String userId, OnUserUpdateListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("UserId is null or empty"));
            return;
        }

        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }
}