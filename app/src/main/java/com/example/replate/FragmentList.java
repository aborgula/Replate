package com.example.replate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FragmentList extends Fragment {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String> currentSelectedChips = new ArrayList<>();
    private LinearLayout parcelContainer;
    private LayoutInflater layoutInflater;

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

    private void loadParcelsAndDisplay() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String selectedDate = prefs.getString("selectedDate", null);
        int filterStart = prefs.getInt("startTimeMinutes", 0);
        int filterEnd = prefs.getInt("endTimeMinutes", 1439);

        db.collection("parcels")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    parcelContainer.removeAllViews();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        List<String> tags = (List<String>) document.get("tags");
                        if (tags == null) tags = new ArrayList<>();

                        boolean showParcel = currentSelectedChips.isEmpty();

                        for (String tag : tags) {
                            if (currentSelectedChips.contains(tag)) {
                                showParcel = true;
                                break;
                            }
                        }

                        if (showParcel && selectedDate != null) {
                            String parcelDate = document.getString("date");
                            if (!selectedDate.equals(parcelDate)) {
                                showParcel = false;
                            }
                        }

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

                            title.setText(document.getString("title"));
                            time.setText("Pick up between " + document.getString("startTime") + " - " + document.getString("endTime"));
                            Double priceValue = document.getDouble("price");
                            if (priceValue != null && priceValue > 0) {
                                price.setText(String.format("%.2f zł", priceValue));
                            } else {
                                price.setText("Free");
                            }
                            rating.setText(String.valueOf(document.getDouble("rating"))); // upewnij się, że masz to pole

                            // Jeśli masz obrazki w Storage — możesz je ładować np. przez Glide
                            // Glide.with(this).load(document.getString("imageUrl")).into(image);

                            parcelContainer.addView(parcelView);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int timeStringToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
