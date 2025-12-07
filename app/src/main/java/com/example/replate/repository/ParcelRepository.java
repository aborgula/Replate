package com.example.replate.repository;

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
}