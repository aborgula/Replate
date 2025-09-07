package com.example.replate;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ncorti.slidetoact.SlideToActView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout ordersContainer;
    private ScrollView ordersScrollView;
    private LinearLayout emptyOrdersLayout;
    private int currentSelectedRating = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);
        db = FirebaseFirestore.getInstance();
        ordersContainer = view.findViewById(R.id.ordersContainer);
        ordersScrollView = view.findViewById(R.id.ordersScrollView);
        emptyOrdersLayout = view.findViewById(R.id.emptyOrdersLayout);
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

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        return;
                    }

                    // Pobierz listę zamówień (parcelId) z pola orders
                    List<String> parcelIds = (List<String>) userDoc.get("orders");

                    if (parcelIds == null || parcelIds.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    ordersContainer.removeAllViews();
                    ordersScrollView.setVisibility(View.VISIBLE);
                    emptyOrdersLayout.setVisibility(View.GONE);

                    // Firestore ogranicza whereIn do 10 elementów
                    List<List<String>> batches = new ArrayList<>();
                    int batchSize = 10;
                    for (int i = 0; i < parcelIds.size(); i += batchSize) {
                        int end = Math.min(parcelIds.size(), i + batchSize);
                        batches.add(parcelIds.subList(i, end));
                    }

                    for (List<String> batch : batches) {
                        db.collection("parcels")
                                .whereIn(FieldPath.documentId(), batch)
                                .whereEqualTo("finished", false)
                                .get()
                                .addOnSuccessListener(parcelSnapshots -> {
                                    if (parcelSnapshots.isEmpty() && ordersContainer.getChildCount() == 0) {
                                        showEmptyState();
                                    } else {
                                        for (QueryDocumentSnapshot parcelDoc : parcelSnapshots) {
                                            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_order, ordersContainer, false);

                                            TextView titleView = itemView.findViewById(R.id.itemTitle);
                                            ImageView imageView = itemView.findViewById(R.id.itemImage);
                                            Button deleteButton = itemView.findViewById(R.id.deleteButton);
                                            SlideToActView pickupButton = itemView.findViewById(R.id.pickupSlide);
                                            SlideToActView pickedupButton = itemView.findViewById(R.id.pickedupSlide);
                                            pickedupButton.setVisibility(View.GONE);

                                            String ownerPhone = parcelDoc.getString("phone");
                                            String parcelDate = parcelDoc.getString("date");
                                            String parcelStartTime = parcelDoc.getString("startTime");
                                            String parcelEndTime = parcelDoc.getString("endTime");

                                            boolean ready = isParcelReadyNow(parcelDate, parcelStartTime, parcelEndTime);
                                            if (ready) {
                                                pickupButton.setVisibility(View.VISIBLE);
                                                pickupButton.setOnClickListener(null);
                                                pickupButton.setOnSlideCompleteListener(slide -> {
                                                    showParcelDialog(parcelDoc.getId(), ownerPhone);
                                                });
                                            } else {
                                                pickupButton.setVisibility(View.GONE);
                                            }

                                            String title = parcelDoc.getString("title");
                                            String imageUrl = parcelDoc.getString("imageUrl");
                                            String documentId = parcelDoc.getId();

                                            titleView.setText(title != null ? title : "Untitled");

                                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                                Glide.with(this).load(imageUrl).into(imageView);
                                            } else {
                                                imageView.setImageResource(R.drawable.photo_mixed);
                                            }

                                            deleteButton.setText("Cancel");
                                            deleteButton.setOnClickListener(v -> {
                                                AlertDialog dialog = new AlertDialog.Builder(getContext())
                                                        .setTitle("Cancel Reservation")
                                                        .setMessage("The parcel was ready for its second life! Really cancel the rescue?")
                                                        .setPositiveButton("Yes", (dialogInterface, which) -> {
                                                            db.collection("parcels").document(documentId)
                                                                    .update("status", "active")
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        db.collection("users").document(currentUserId)
                                                                                .update("orders", FieldValue.arrayRemove(documentId))
                                                                                .addOnSuccessListener(unused -> {
                                                                                    Toast.makeText(getContext(), "Reservation cancelled", Toast.LENGTH_SHORT).show();
                                                                                    ordersContainer.removeView(itemView);

                                                                                    if (ordersContainer.getChildCount() == 0) {
                                                                                        showEmptyState();
                                                                                    }
                                                                                })
                                                                                .addOnFailureListener(e ->
                                                                                        Toast.makeText(getContext(), "Failed to update user orders", Toast.LENGTH_SHORT).show());
                                                                    })
                                                                    .addOnFailureListener(e ->
                                                                            Toast.makeText(getContext(), "Failed to update parcel status", Toast.LENGTH_SHORT).show());
                                                        })
                                                        .setNegativeButton("No", null)
                                                        .create();

                                                dialog.show();
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
                                                intent.putExtra("reserved", true);
                                                startActivity(intent);
                                            });

                                            ordersContainer.addView(itemView);
                                        }
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed to load parcels: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Pokazuje pusty ekran
    private void showEmptyState() {
        ordersScrollView.setVisibility(View.GONE);
        emptyOrdersLayout.setVisibility(View.VISIBLE);
    }


    private boolean isParcelReadyNow(String date, String startTime, String endTime) {
        if (date == null || startTime == null || endTime == null) return false;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();

        try {
            String today = dateFormat.format(now);
            if (!today.equals(date)) return false;

            Date currentTime = timeFormat.parse(timeFormat.format(now));
            Date start = timeFormat.parse(startTime);
            Date end = timeFormat.parse(endTime);

            if (currentTime == null || start == null || end == null) return false;

            if (end.before(start)) {
                return !currentTime.before(start) || !currentTime.after(end);
            } else {
                return !currentTime.before(start) && !currentTime.after(end);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showParcelDialog(String parcelId, String ownerPhone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_parcel_confirmation, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {
            // Sprawdź w Firestore ownerconfirmed
            db.collection("parcels").document(parcelId).get()
                    .addOnSuccessListener(parcelDoc -> {
                        if (parcelDoc.exists()) {
                            String ownerConfirmed = parcelDoc.getString("ownerconfirmed");

                            if (ownerConfirmed == null || ownerConfirmed.equals("no")) {
                                // Wyświetl okno informacyjne z własnym layoutem
                                AlertDialog.Builder innerBuilder = new AlertDialog.Builder(getContext());
                                View innerView = inflater.inflate(R.layout.inform_owner_window, null);
                                innerBuilder.setView(innerView);

                                AlertDialog innerDialog = innerBuilder.create();
                                innerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                                Button btnOk = innerView.findViewById(R.id.btnOk);
                                btnOk.setOnClickListener(v2 -> innerDialog.dismiss());

                                innerDialog.show();
                            }

                            else if ("yes".equals(ownerConfirmed)) {
                                // TODO: logika, gdy owner potwierdził
                                Toast.makeText(getContext(), "Owner already confirmed this parcel", Toast.LENGTH_SHORT).show();


                                showRatingDialog(parcelId);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error checking parcel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> {
            if (ownerPhone != null && !ownerPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + ownerPhone));
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Owner's phone number not available", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showRatingDialog(String parcelId) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.rating_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Tutaj pobierasz widoki z rating_dialog: gwiazdki, EditText, przyciski itp.
        ImageView star1 = dialogView.findViewById(R.id.star1);
        ImageView star2 = dialogView.findViewById(R.id.star2);
        ImageView star3 = dialogView.findViewById(R.id.star3);
        ImageView star4 = dialogView.findViewById(R.id.star4);
        ImageView star5 = dialogView.findViewById(R.id.star5);

        EditText description = dialogView.findViewById(R.id.descriptionEditText);
        Button skipBtn = dialogView.findViewById(R.id.skipBtn);
        Button rateBtn = dialogView.findViewById(R.id.rateBtn);


        ImageView[] stars = {star1, star2, star3, star4, star5};

        // Aktualnie wybrana ocena
        final int[] selectedRating = {0};

        // Ustawienie kliknięć na gwiazdki
        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i].setOnClickListener(v -> {
                selectedRating[0] = index + 1; // liczba gwiazdek
                updateStars(stars, selectedRating[0]);

                currentSelectedRating = index + 1;
                updateStars(stars, currentSelectedRating);
            });
        }

        // Można też ustawić skipBtn i rateBtn później
        skipBtn.setOnClickListener(v -> dialog.dismiss());

        rateBtn.setOnClickListener(v -> {
            if (selectedRating[0] == 0) {
                Toast.makeText(getContext(), "Please select a rating before submitting.", Toast.LENGTH_SHORT).show();
                return;
            }

            String descriptionText = description.getText().toString().trim();
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Najpierw pobierz dane paczki, żeby znaleźć autora
            db.collection("parcels").document(parcelId).get()
                    .addOnSuccessListener(parcelDoc -> {
                        if (parcelDoc.exists()) {
                            String authorId = parcelDoc.getString("userId"); // autor paczki

                            if (authorId == null) {
                                Toast.makeText(getContext(), "Parcel owner not found", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 1. Zapisz ocenę do kolekcji ratings
                            Map<String, Object> ratingData = new HashMap<>();
                            ratingData.put("authorId", authorId);
                            ratingData.put("userId", currentUserId);
                            ratingData.put("value", selectedRating[0]);
                            ratingData.put("description", descriptionText);
                            ratingData.put("timestamp", new Date());

                            Log.d("RatingDialog", "Saving rating: " + ratingData.toString());
                            db.collection("ratings")
                                    .add(ratingData)
                                    .addOnSuccessListener(ref -> {
                                        Log.d("RatingDialog", "Rating saved: " + ref.getId());

                                        // 2. Zaktualizuj średnią ocen autora w kolekcji users
                                        DocumentReference userRef = db.collection("users").document(authorId);
                                        db.runTransaction(transaction -> {
                                            DocumentSnapshot snapshot = transaction.get(userRef);

                                            Double oldRating = snapshot.getDouble("rating");
                                            Long ratingCountLong = snapshot.getLong("ratingCount");

                                            double currentRating = (oldRating != null) ? oldRating : 0.0;
                                            long ratingCount = (ratingCountLong != null) ? ratingCountLong : 0;

                                            double newAvg = ((currentRating * ratingCount) + selectedRating[0]) / (ratingCount + 1);

                                            transaction.update(userRef, "rating", newAvg);
                                            transaction.update(userRef, "ratingCount", ratingCount + 1);

                                            return null;
                                        }).addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                                        }).addOnFailureListener(e -> {
                                            Log.e("RatingDialog", "Error updating rating", e);
                                        });

                                        db.collection("parcels").document(parcelId)
                                                .update("finished", true)
                                                .addOnSuccessListener(aVoid -> Log.d("RatingDialog", "Parcel marked as finished"))
                                                .addOnFailureListener(e -> Log.e("RatingDialog", "Error marking parcel finished", e));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("RatingDialog", "Error saving rating", e);
                                    });
                        }
                    });

            dialog.dismiss();
        });


        dialog.show();
    }


    // Metoda do aktualizacji wyglądu gwiazdek
    private void updateStars(ImageView[] stars, int rating) {

        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.star_full);
            } else {
                stars[i].setImageResource(R.drawable.star_empty);
                ViewGroup.LayoutParams params = stars[i].getLayoutParams();
                params.width = dpToPx(45);
                params.height = dpToPx(45);
                stars[i].setLayoutParams(params);
            }
        }
    }

    // Pomocnicza metoda do konwersji dp na px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

}
