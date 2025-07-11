package com.example.replate;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        adapter = new ParcelAdapter(
                this,
                parcelList,
                currentUser != null ? currentUser.getUid() : "",

                parcel -> {
                    AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Delete parcel")
                            .setMessage("Are you sure you want to delete this parcel?")
                            .setPositiveButton("Yes", (d, which) -> {
                                db.collection("parcels").document(parcel.getId())
                                        .delete()
                                        .addOnSuccessListener(unused -> {
                                            parcelList.remove(parcel);
                                            adapter.notifyDataSetChanged();
                                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("Cancel", null)
                            .create();

                    dialog.show();

                    // ⬇️ Zmieniamy kolory po pokazaniu dialogu
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                            ContextCompat.getColor(this, R.color.brown));

                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                            ContextCompat.getColor(this, R.color.yellow));
                },


                parcel -> {
                    Intent intent = new Intent(this, ParcelActivity.class);
                    intent.putExtra("parcelId", parcel.getId());
                    startActivity(intent);
                }
        );

        arrowBack.setOnClickListener(v -> finish()); // back button

        recyclerView.setAdapter(adapter);

        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish(); // lub załaduj aktualnego zalogowanego
            return;
        }

        // Dalej ładuj paczki tylko tego użytkownika
        loadParcelsForUser(userId);
    }

    private void loadParcelsForUser(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("parcels")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    parcelList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String image = doc.getString("imageBase64");
                        String parcelUserId = doc.getString("userId");
                        parcelList.add(new Parcel(id, title, image, parcelUserId));

                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading parcels", Toast.LENGTH_SHORT).show();
                });
    }

}
