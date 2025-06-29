package com.example.replate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        db.collection("parcels")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ordersContainer.removeAllViews();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.order_layout, ordersContainer, false);

                        TextView titleView = itemView.findViewById(R.id.itemTitle);
                        ImageView imageView = itemView.findViewById(R.id.itemImage);
                        Button deleteButton = itemView.findViewById(R.id.deleteButton);

                        String title = document.getString("title");
                        String imageUrl = document.getString("imageUrl"); // jeśli zapisujesz URL
                        String documentId = document.getId();

                        titleView.setText(title != null ? title : "Untitled");

                        // Załaduj obraz jeśli jest — lub ustaw domyślny
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.photo_mixed); // Twój domyślny obraz
                        }

                        deleteButton.setOnClickListener(v -> {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Confirm Deletion")
                                    .setMessage("Are you sure you want to delete this parcel?")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        db.collection("parcels").document(documentId)
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(getContext(), "Parcel deleted", Toast.LENGTH_SHORT).show();
                                                    ordersContainer.removeView(itemView); // usuń z widoku
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .setNegativeButton("Cancel", (dialog, which) -> {
                                        dialog.dismiss(); // zamknij dialog
                                    })
                                    .show();
                        });

                        ordersContainer.addView(itemView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load orders: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
