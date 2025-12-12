package com.example.replate.service;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RatingService {

    private final FirebaseFirestore db;

    public RatingService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public RatingService(FirebaseFirestore firestore) {
        this.db = firestore;
    }

    public interface RatingSubmissionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface ParcelOwnerCallback {
        void onSuccess(String ownerId, String parcelTitle);
        void onParcelNotFound();
        void onFailure(Exception e);
    }

    public static class RatingData {
        public String authorId;
        public String userId;
        public String parcelId;
        public String parcelTitle;
        public int value;
        public String description;
        public Date timestamp;
    }

    /**
     * Pobiera ID właściciela paczki
     */
    public void getParcelOwner(String parcelId, ParcelOwnerCallback callback) {
        if (parcelId == null || parcelId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("ParcelId is null or empty"));
            return;
        }

        db.collection("parcels").document(parcelId).get()
                .addOnSuccessListener(parcelDoc -> {
                    if (parcelDoc.exists()) {
                        String authorId = parcelDoc.getString("userId");
                        String parcelTitle = parcelDoc.getString("title");

                        if (authorId != null) {
                            callback.onSuccess(authorId, parcelTitle);
                        } else {
                            callback.onParcelNotFound();
                        }
                    } else {
                        callback.onParcelNotFound();
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Zapisuje ocenę i aktualizuje średnią użytkownika
     */
    public void submitRating(String authorId, String userId, String parcelId,
                             String parcelTitle, int rating, String description,
                             RatingSubmissionCallback callback) {

        if (authorId == null || userId == null || parcelId == null) {
            callback.onFailure(new IllegalArgumentException("Required parameters are null"));
            return;
        }

        if (rating < 1 || rating > 5) {
            callback.onFailure(new IllegalArgumentException("Rating must be between 1 and 5"));
            return;
        }

        // 1. Zapisz ocenę do kolekcji ratings
        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("authorId", authorId);
        ratingData.put("userId", userId);
        ratingData.put("parcelId", parcelId);
        ratingData.put("parcelTitle", parcelTitle);
        ratingData.put("value", rating);
        ratingData.put("description", description);
        ratingData.put("timestamp", new Date());

        db.collection("ratings")
                .add(ratingData)
                .addOnSuccessListener(ref -> {
                    // 2. Zaktualizuj średnią ocen użytkownika
                    updateUserRating(authorId, rating, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Aktualizuje średnią ocen użytkownika
     */
    private void updateUserRating(String userId, int newRating, RatingSubmissionCallback callback) {
        DocumentReference userRef = db.collection("users").document(userId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userRef);

                    Double oldRating = snapshot.getDouble("rating");
                    Long ratingCountLong = snapshot.getLong("ratingCount");

                    double currentRating = (oldRating != null) ? oldRating : 0.0;
                    long ratingCount = (ratingCountLong != null) ? ratingCountLong : 0;

                    double newAvg = ((currentRating * ratingCount) + newRating) / (ratingCount + 1);

                    transaction.update(userRef, "rating", newAvg);
                    transaction.update(userRef, "ratingCount", ratingCount + 1);

                    return null;
                })
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Oznacza paczkę jako zakończoną
     */
    public void markParcelAsFinished(String parcelId, RatingSubmissionCallback callback) {
        if (parcelId == null || parcelId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("ParcelId is null or empty"));
            return;
        }

        db.collection("parcels").document(parcelId)
                .update("finished", true)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}