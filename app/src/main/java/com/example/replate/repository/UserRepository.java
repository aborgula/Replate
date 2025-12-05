package com.example.replate.repository;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final FirebaseFirestore db;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
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
}