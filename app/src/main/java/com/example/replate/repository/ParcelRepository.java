/**package com.example.replate.repository;

import com.example.replate.model.Parcel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParcelRepository {

    private final FirebaseFirestore db;

    public ParcelRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnParcelsLoadedListener {
        void onSuccess(List<Parcel> parcels);
        void onFailure(Exception e);
    }

    ParcelRepository(FirebaseFirestore firestore) {
        this.db = firestore;
    }

    public void getActiveParcels(OnParcelsLoadedListener listener) {
        db.collection("parcels")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Parcel> parcels = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Parcel parcel = document.toObject(Parcel.class);
                        parcel.setId(document.getId());
                        parcels.add(parcel);
                    }

                    listener.onSuccess(parcels);
                })
                .addOnFailureListener(listener::onFailure);
    }
}**/


package com.example.replate.repository;

import android.util.Log;

import com.example.replate.model.Parcel;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParcelRepository {

    private final FirebaseFirestore db;

    public ParcelRepository() {
        this.db = FirebaseFirestore.getInstance();
    }



    public interface OnParcelsLoadedListener {
        void onSuccess(List<Parcel> parcels);
        void onFailure(Exception e);
    }

    // Konstruktor dla testów (Dependency Injection)
    public ParcelRepository(FirebaseFirestore firestore) {
        this.db = firestore;
    }

    public interface OnParcelDeletedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void getActiveParcels(OnParcelsLoadedListener listener) {
        db.collection("parcels")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Parcel> parcels = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Parcel parcel = document.toObject(Parcel.class);
                        parcel.setId(document.getId());
                        parcels.add(parcel);
                    }

                    listener.onSuccess(parcels);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void deleteExpiredParcels() {

        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String now = fullFormat.format(Calendar.getInstance().getTime());

        db.collection("parcels")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        String parcelDate = doc.getString("date");
                        String parcelEnd = doc.getString("endTime");
                        String parcelStart = doc.getString("startTime");
                        String parcelId = doc.getId();
                        String userId = doc.getString("userId");

                        if (parcelDate == null || parcelEnd == null) {
                            Log.w("DeleteParcel", "Missing fields in: " + parcelId);
                            continue;
                        }

                        try {
                            Date nowDate = fullFormat.parse(now);
                            Date parcelEndDate = fullFormat.parse(parcelDate + " " + parcelEnd);

                            // obsługa przejścia przez północ
                            if (parcelStart != null) {
                                Date endOnly = timeFormat.parse(parcelEnd);
                                Date startOnly = timeFormat.parse(parcelStart);

                                if (endOnly != null && startOnly != null && endOnly.before(startOnly)) {
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(parcelEndDate);
                                    cal.add(Calendar.DAY_OF_MONTH, 1);
                                    parcelEndDate = cal.getTime();
                                }
                            }

                            if (parcelEndDate != null && nowDate != null && parcelEndDate.before(nowDate)) {
                                Log.d("DeleteParcel", "Expired: " + parcelId);

                                // usuń parcelId z usera
                                if (userId != null) {
                                    db.collection("users").document(userId)
                                            .update("parcelIds", FieldValue.arrayRemove(parcelId));
                                }

                                // usuń paczkę
                                db.collection("parcels").document(parcelId).delete();

                            }

                        } catch (Exception e) {
                            Log.e("DeleteParcel", "Parse error: " + parcelId, e);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DeleteParcel", "Fetch failed", e));
    }

    public void deleteUserParcels(String userId, OnParcelDeletedListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("UserId is null or empty"));
            return;
        }

        db.collection("parcels")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("ParcelRepository", "Deleted " + querySnapshot.size() + " parcels for user: " + userId);
                                listener.onSuccess();
                            })
                            .addOnFailureListener(listener::onFailure);
                })
                .addOnFailureListener(listener::onFailure);
    }
}