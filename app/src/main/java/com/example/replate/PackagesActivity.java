package com.example.replate;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PackagesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ParcelAdapter adapter;
    private List<Parcel> parcelList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;
    private ImageView arrowBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packages);

        recyclerView = findViewById(R.id.packagesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        arrowBack = findViewById(R.id.arrowback);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish(); // lub załaduj aktualnego zalogowanego
            return;
        }

        boolean isOwnerProfile = currentUser != null && userId.equals(currentUser.getUid());

        // Tworzymy adapter z przekazaniem isOwnerProfile
        adapter = new ParcelAdapter(
                this,
                parcelList,
                currentUser != null ? currentUser.getUid() : "",
                isOwnerProfile, // 🔑 tutaj
                // onDeleteClick
                parcel -> {
                    AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Take back your parcel?")
                            .setMessage("Once removed, nobody will be able to rescue it. Still want to recall it?")
                            .setPositiveButton("Yes, take it back", (d, which) -> {
                                db.collection("parcels").document(parcel.getId())
                                        .delete()
                                        .addOnSuccessListener(unused -> {
                                            parcelList.remove(parcel);
                                            adapter.notifyDataSetChanged();
                                            Toast.makeText(this, "Parcel removed ✅", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to remove ❌", Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("No, keep it", null)
                            .create();

                    dialog.show();

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                            ContextCompat.getColor(this, R.color.brown));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                            ContextCompat.getColor(this, R.color.yellow));
                },
                // onCancelClick
                parcel -> {
                    db.collection("parcels").document(parcel.getId())
                            .update("status", "active")
                            .addOnSuccessListener(unused -> {
                                db.collection("users").document(currentUser.getUid())
                                        .update("orders", FieldValue.arrayRemove(parcel.getId()))
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Reservation cancelled", Toast.LENGTH_SHORT).show();
                                            parcelList.remove(parcel);
                                            adapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to update orders", Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to update parcel status", Toast.LENGTH_SHORT).show());
                },
                // onParcelClick
                parcel -> {
                    Intent intent = new Intent(this, ParcelActivity.class);
                    intent.putExtra("parcelId", parcel.getId());
                    startActivity(intent);
                }
        );

        arrowBack.setOnClickListener(v -> finish());
        recyclerView.setAdapter(adapter);

        // Załaduj paczki użytkownika
        loadParcelsForUser(userId);
    }


    private void loadParcelsForUser(String userId) {
        db.collection("parcels")
                .whereEqualTo("userId", userId)
                .whereEqualTo("finished", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    parcelList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String image = doc.getString("imageBase64");
                        String parcelUserId = doc.getString("userId");
                        String ownerConfirmed = doc.getString("ownerconfirmed");
                        String status = doc.getString("status");

                        Parcel parcel = new Parcel();
                        parcel.setId(id);
                        parcel.setTitle(title);
                        parcel.setImageBase64(image);
                        parcel.setUserId(parcelUserId);
                        parcel.setOwnerConfirmed(ownerConfirmed);
                        parcel.setStatus(status);

                        parcelList.add(parcel);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading parcels", Toast.LENGTH_SHORT).show();
                });
    }


}
