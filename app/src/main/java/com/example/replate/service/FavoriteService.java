package com.example.replate.service;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class FavoriteService {

    private final FirebaseFirestore db;

    public FavoriteService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnFavoriteToggleListener {
        void onSuccess(boolean isNowLiked);
        void onFailure(Exception e);
    }

    public void toggleFavorite(String currentUserId, String creatorId, boolean isCurrentlyLiked,
                               OnFavoriteToggleListener listener) {
        if (isCurrentlyLiked) {
            // Usuń z ulubionych
            db.collection("users").document(currentUserId)
                    .update("likedUsers", FieldValue.arrayRemove(creatorId))
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(creatorId)
                                .update("likedBy", FieldValue.arrayRemove(currentUserId))
                                .addOnSuccessListener(aVoid2 -> listener.onSuccess(false))
                                .addOnFailureListener(listener::onFailure);
                    })
                    .addOnFailureListener(listener::onFailure);
        } else {
            // Dodaj do ulubionych
            db.collection("users").document(currentUserId)
                    .update("likedUsers", FieldValue.arrayUnion(creatorId))
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(creatorId)
                                .update("likedBy", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener(aVoid2 -> listener.onSuccess(true))
                                .addOnFailureListener(listener::onFailure);
                    })
                    .addOnFailureListener(listener::onFailure);
        }
    }
}