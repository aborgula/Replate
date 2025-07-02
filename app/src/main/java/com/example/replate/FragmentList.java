package com.example.replate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FragmentList extends Fragment {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String> currentSelectedChips = new ArrayList<>();
    private LinearLayout parcelContainer;
    private LayoutInflater layoutInflater;


    // Launcher przekazywany z PickupFragment
    private androidx.activity.result.ActivityResultLauncher<Intent> parcelLauncher;

    // Pozwala PickupFragment przekazać parcelLauncher
    public void setParcelLauncher(androidx.activity.result.ActivityResultLauncher<Intent> launcher) {
        this.parcelLauncher = launcher;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        parcelContainer = view.findViewById(R.id.parcelContainer);
        layoutInflater = inflater;

        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Set<String> selectedChipsSet = prefs.getStringSet("selectedChips", null);
        currentSelectedChips = selectedChipsSet != null ? new ArrayList<>(selectedChipsSet) : new ArrayList<>();

        loadParcelsAndDisplay();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Odśwież filtry z SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Set<String> selectedChipsSet = prefs.getStringSet("selectedChips", null);
        currentSelectedChips = selectedChipsSet != null ? new ArrayList<>(selectedChipsSet) : new ArrayList<>();

        // Przeładuj paczki
        loadParcelsAndDisplay();
    }

    // Publiczna metoda do ponownego załadowania
    public void loadFilteredParcels() {
        loadParcelsAndDisplay();
    }

    private void loadParcelsAndDisplay() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String selectedDate = prefs.getString("selectedDate", null);
        int filterStart = prefs.getInt("startTimeMinutes", 0);
        int filterEnd = prefs.getInt("endTimeMinutes", 1439);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Najpierw pobierz listę ulubionych użytkownika
        db.collection("users").document(currentUserId).get().addOnSuccessListener(userDoc -> {
            List<String> likedUsers = (List<String>) userDoc.get("likedUsers");
            if (likedUsers == null) likedUsers = new ArrayList<>();
            List<String> finalLikedUsers = likedUsers; // użyte wewnątrz lambdy

            db.collection("parcels")
                    .whereEqualTo("status", "active")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        parcelContainer.removeAllViews();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String creatorId = document.getString("userId");

                            if (creatorId != null && creatorId.equals(currentUserId)) {
                                continue; // Pomijamy własne paczki
                            }

                            // Filtrowanie po tagach
                            List<String> tags = (List<String>) document.get("tags");
                            if (tags == null) tags = new ArrayList<>();
                            boolean showParcel = currentSelectedChips.isEmpty();
                            for (String tag : tags) {
                                if (currentSelectedChips.contains(tag)) {
                                    showParcel = true;
                                    break;
                                }
                            }

                            // Filtrowanie po dacie
                            if (showParcel && selectedDate != null) {
                                String parcelDate = document.getString("date");
                                if (!selectedDate.equals(parcelDate)) {
                                    showParcel = false;
                                }
                            }

                            // Filtrowanie po czasie
                            if (showParcel) {
                                String parcelStartStr = document.getString("startTime");
                                String parcelEndStr = document.getString("endTime");

                                if (parcelStartStr != null && parcelEndStr != null
                                        && parcelStartStr.matches("\\d{2}:\\d{2}") && parcelEndStr.matches("\\d{2}:\\d{2}")) {

                                    int parcelStartMin = timeStringToMinutes(parcelStartStr);
                                    int parcelEndMin = timeStringToMinutes(parcelEndStr);

                                    if (parcelEndMin < filterStart || parcelStartMin > filterEnd) {
                                        showParcel = false;
                                    }
                                }
                            }

                            if (showParcel) {
                                View parcelView = layoutInflater.inflate(R.layout.layout_package_list, parcelContainer, false);

                                TextView title = parcelView.findViewById(R.id.parcelTitle);
                                TextView time = parcelView.findViewById(R.id.parcelTime);
                                TextView price = parcelView.findViewById(R.id.price);
                                TextView rating = parcelView.findViewById(R.id.rating);
                                ImageView image = parcelView.findViewById(R.id.parcelImage);
                                ImageButton favoriteButton = parcelView.findViewById(R.id.favoriteButton);

                                title.setText(document.getString("title"));
                                Double priceValue = document.getDouble("price");
                                String startTime = document.getString("startTime");
                                String endTime = document.getString("endTime");

                                String whenText = prefs.getString("when", "Today");
                                time.setText(whenText + ", " + startTime + " - " + endTime);

                                if (priceValue != null && priceValue > 0) {
                                    price.setText(String.format("%.2f zł", priceValue));
                                } else {
                                    price.setText("Free");
                                }

                                rating.setText(String.valueOf(document.getDouble("rating")));

                                parcelContainer.addView(parcelView);
                                String documentId = document.getId();

                                // Obsługa kliknięcia paczki
                                parcelView.setOnClickListener(v -> {
                                    Intent intent = new Intent(requireContext(), ParcelActivity.class);
                                    intent.putExtra("parcelId", documentId);
                                    if (parcelLauncher != null) {
                                        parcelLauncher.launch(intent);
                                    } else {
                                        startActivity(intent);
                                    }
                                });

                                // 🔁 Serduszko (ulubione)
                                AtomicBoolean isLiked = new AtomicBoolean(finalLikedUsers.contains(creatorId));

                                if (isLiked.get()) {
                                    favoriteButton.setImageResource(R.drawable.ic_favouritefull);
                                    favoriteButton.setColorFilter(Color.parseColor("#F40F0F"));
                                } else {
                                    favoriteButton.setImageResource(R.drawable.ic_favourite);
                                    favoriteButton.setColorFilter(Color.parseColor("#F8F8F8"));
                                }

                                favoriteButton.setOnClickListener(v -> {
                                    if (isLiked.get()) {
                                        db.collection("users").document(currentUserId)
                                                .update("likedUsers", FieldValue.arrayRemove(creatorId))
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                                    favoriteButton.setImageResource(R.drawable.ic_favourite);
                                                    favoriteButton.setColorFilter(Color.parseColor("#F8F8F8"));
                                                });
                                    } else {
                                        db.collection("users").document(currentUserId)
                                                .update("likedUsers", FieldValue.arrayUnion(creatorId))
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
                                                    favoriteButton.setImageResource(R.drawable.ic_favouritefull);
                                                    favoriteButton.setColorFilter(Color.parseColor("#F40F0F"));
                                                });
                                    }

                                    isLiked.set(!isLiked.get());
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }


    private int timeStringToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
