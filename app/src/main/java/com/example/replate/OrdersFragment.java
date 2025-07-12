package com.example.replate;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout ordersContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);
        db = FirebaseFirestore.getInstance();
        ordersContainer = view.findViewById(R.id.ordersContainer);
        loadUserParcels();
        return view;
    }

    private void loadUserParcels() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pobierz dokument użytkownika
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Pobierz listę zamówień (parcelId) z pola orders
                    List<String> parcelIds = (List<String>) userDoc.get("orders");

                    if (parcelIds == null || parcelIds.isEmpty()) {
                        Toast.makeText(getContext(), "No orders found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ordersContainer.removeAllViews();

                    // Firestore ogranicza whereIn do 10 elementów na zapytanie,
                    // jeśli więcej, trzeba robić podział na mniejsze partie - tutaj przykład dla max 10
                    List<List<String>> batches = new ArrayList<>();
                    int batchSize = 10;
                    for (int i = 0; i < parcelIds.size(); i += batchSize) {
                        int end = Math.min(parcelIds.size(), i + batchSize);
                        batches.add(parcelIds.subList(i, end));
                    }

                    for (List<String> batch : batches) {
                        db.collection("parcels")
                                .whereIn(FieldPath.documentId(), batch)
                                .get()
                                .addOnSuccessListener(parcelSnapshots -> {
                                    for (QueryDocumentSnapshot parcelDoc : parcelSnapshots) {
                                        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_order, ordersContainer, false);

                                        TextView titleView = itemView.findViewById(R.id.itemTitle);
                                        ImageView imageView = itemView.findViewById(R.id.itemImage);
                                        Button deleteButton = itemView.findViewById(R.id.deleteButton);

                                        String title = parcelDoc.getString("title");
                                        String imageUrl = parcelDoc.getString("imageUrl");
                                        String documentId = parcelDoc.getId();

                                        titleView.setText(title != null ? title : "Untitled");

                                        if (imageUrl != null && !imageUrl.isEmpty()) {
                                            Glide.with(this).load(imageUrl).into(imageView);
                                        } else {
                                            imageView.setImageResource(R.drawable.photo_mixed);
                                        }

                                       // deleteButton.setVisibility(View.GONE);
                                        deleteButton.setText("Cancel");

                                        deleteButton.setOnClickListener(v -> {
                                            AlertDialog dialog = new AlertDialog.Builder(getContext())
                                                    .setTitle("Cancel Reservation")
                                                    .setMessage("The parcel was ready for its second life! Really cancel the rescue?")
                                                    .setPositiveButton("Yes", (dialogInterface, which) -> {
                                                        // 1. Zmień status paczki na "active"
                                                        db.collection("parcels").document(documentId)
                                                                .update("status", "active")
                                                                .addOnSuccessListener(aVoid -> {
                                                                    // 2. Usuń ID paczki z pola orders użytkownika
                                                                    db.collection("users").document(currentUserId)
                                                                            .update("orders", com.google.firebase.firestore.FieldValue.arrayRemove(documentId))
                                                                            .addOnSuccessListener(unused -> {
                                                                                Toast.makeText(getContext(), "Reservation cancelled", Toast.LENGTH_SHORT).show();
                                                                                ordersContainer.removeView(itemView); // usuń z widoku
                                                                            })
                                                                            .addOnFailureListener(e ->
                                                                                    Toast.makeText(getContext(), "Failed to update user orders", Toast.LENGTH_SHORT).show());
                                                                })
                                                                .addOnFailureListener(e ->
                                                                        Toast.makeText(getContext(), "Failed to update parcel status", Toast.LENGTH_SHORT).show());
                                                    })
                                                    .setNegativeButton("No", null)
                                                    .create(); // WAŻNE: użyj create() zamiast show()

                                            dialog.show(); // Teraz dialog istnieje i można modyfikować jego przyciski

                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                                    ContextCompat.getColor(getContext(), R.color.brown)
                                            );
                                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                                                    ContextCompat.getColor(getContext(), R.color.yellow)
                                            );
                                        });

                                        itemView.setOnClickListener(v -> {
                                            Intent intent = new Intent(getContext(), ParcelActivity.class);
                                            intent.putExtra("parcelId", documentId);
                                            intent.putExtra("reserved", true);  // dodatkowa flaga, że paczka jest zarezerwowana
                                            startActivity(intent);
                                        });

                                        ordersContainer.addView(itemView);

                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed to load parcels: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
