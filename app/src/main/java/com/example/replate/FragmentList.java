package com.example.replate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FragmentList extends Fragment {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String> currentSelectedChips = new ArrayList<>();
    private LinearLayout parcelContainer;
    private LayoutInflater layoutInflater;
    private ScrollView scrollView;
    private LinearLayout layoutEmptyParcels;
    private LottieAnimationView emptyParcelsAnim;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;



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

        scrollView = view.findViewById(R.id.scrollView);
        layoutEmptyParcels = view.findViewById(R.id.layoutEmptyParcels);
        emptyParcelsAnim = view.findViewById(R.id.emptyParcelsAnim);
        checkLocationPermission();

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

    public void loadFilteredParcels() {
        loadParcelsAndDisplay();
    }

    private void loadParcelsAndDisplay() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String filterPlaceId = prefs.getString("lastPlaceId", null);
        int selectedDistanceKm = prefs.getInt("selected_distance_km", 0);
        Double filterLat = Double.longBitsToDouble(prefs.getLong("lastLat", Double.doubleToLongBits(0.0)));
        Double filterLng = Double.longBitsToDouble(prefs.getLong("lastLng", Double.doubleToLongBits(0.0)));

        String storedDate = prefs.getString("selectedDate", "Today");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayString = sdf.format(Calendar.getInstance().getTime());

        if (storedDate == null || storedDate.equals("Today")) {
            storedDate = todayString;
        } else {
            try {
                Date selected = sdf.parse(storedDate);
                Date today = sdf.parse(todayString);

                if (selected != null && today != null && selected.before(today)) {
                    // 🔄 Wybrana data jest wcześniejsza niż dziś → ustawiamy na dzisiaj
                    storedDate = todayString;

                    // Opcjonalnie zaktualizuj SharedPreferences
                    prefs.edit().putString("selectedDate", todayString).apply();
                }
            } catch (ParseException e) {
                e.printStackTrace();
                storedDate = todayString; // w razie błędu ustawiamy na dzisiaj
            }
        }

        final String selectedDate = storedDate;
        int filterStart = prefs.getInt("startTimeMinutes", 0);
        int filterEnd = prefs.getInt("endTimeMinutes", 1439);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(currentUserId).get().addOnSuccessListener(userDoc -> {
            List<String> likedUsers = (List<String>) userDoc.get("likedUsers");
            if (likedUsers == null) likedUsers = new ArrayList<>();
            List<String> finalLikedUsers = likedUsers; // użyte wewnątrz lambdy

            db.collection("parcels")
                    .whereEqualTo("status", "active")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        parcelContainer.removeAllViews();
                        boolean hasParcels = false;

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String creatorId = document.getString("userId");
                            String docId = document.getId();

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
                                    Log.d("FilterDebug", "❌ Odrzucono po dacie: " + parcelDate);

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

                            //Filtrowanie po odległości
                            if (showParcel && selectedDistanceKm > 0) {
                                Double parcelLat = document.getDouble("lat");
                                Double parcelLng = document.getDouble("lng");
                                if (parcelLat != null && parcelLng != null) {
                                    float[] results = new float[1];
                                    android.location.Location.distanceBetween(filterLat, filterLng, parcelLat, parcelLng, results);
                                    float distanceKm = results[0] / 1000f;

                                    if (distanceKm > selectedDistanceKm) {
                                        showParcel = false;
                                    }
                                }else{
                                }
                            }

                            if (showParcel) {
                                hasParcels = true;
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

                                String imageUrl = document.getString("imageUrl");
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    Glide.with(requireContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.photo_bakery)
                                            .error(R.drawable.photo_bakery)
                                            .into(image);
                                } else {
                                    // Jeśli brak zdjęcia, ustaw na podstawie tagu
                                    if (tags != null && tags.size() == 1) {
                                        switch (tags.get(0).toLowerCase(Locale.ROOT)) {
                                            case "sweets": image.setImageResource(R.drawable.photo_sweets); break;
                                            case "fast food": image.setImageResource(R.drawable.photo_fastfood); break;
                                            case "bakery": image.setImageResource(R.drawable.photo_bakery); break;
                                            case "dairy": image.setImageResource(R.drawable.photo_dairy); break;
                                            case "vegetables": image.setImageResource(R.drawable.photo_vegetables); break;
                                            case "vegetarian": image.setImageResource(R.drawable.photo_vegetarian); break;
                                            case "vegan": image.setImageResource(R.drawable.photo_vegan); break;
                                            case "fruits": image.setImageResource(R.drawable.photo_fruits); break;
                                            case "liquids": image.setImageResource(R.drawable.photo_liquids); break;
                                            case "meals": image.setImageResource(R.drawable.photo_meal); break;
                                            case "meat": image.setImageResource(R.drawable.photo_meat); break;
                                            default: image.setImageResource(R.drawable.photo_mixed); break;
                                        }
                                    } else {
                                        image.setImageResource(R.drawable.photo_mixed);
                                    }
                                }

                                if (creatorId != null) {
                                    db.collection("users").document(creatorId)
                                            .get()
                                            .addOnSuccessListener(creatorDoc -> {
                                                if (creatorDoc.exists()) {
                                                    Double userRating = creatorDoc.getDouble("rating");
                                                    if (userRating != null && userRating > 0) {
                                                        rating.setText("" + String.format(Locale.US, "%.1f", userRating));
                                                    } else {
                                                        rating.setText("No ratings");
                                                    }
                                                } else {
                                                    rating.setText("No ratings");
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                rating.setText("No ratings");
                                            });
                                }

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
                                        // Usuń z ulubionych
                                        db.collection("users").document(currentUserId)
                                                .update("likedUsers", FieldValue.arrayRemove(creatorId));
                                        db.collection("users").document(creatorId)
                                                .update("likedBy", FieldValue.arrayRemove(currentUserId));

                                        favoriteButton.setImageResource(R.drawable.ic_favourite);
                                        favoriteButton.setColorFilter(Color.parseColor("#F8F8F8"));
                                        Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Dodaj do ulubionych
                                        db.collection("users").document(currentUserId)
                                                .update("likedUsers", FieldValue.arrayUnion(creatorId));
                                        db.collection("users").document(creatorId)
                                                .update("likedBy", FieldValue.arrayUnion(currentUserId));

                                        favoriteButton.setImageResource(R.drawable.ic_favouritefull);
                                        favoriteButton.setColorFilter(Color.parseColor("#F40F0F"));
                                        Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
                                    }

                                    isLiked.set(!isLiked.get());
                                });

                            }
                        }

                        if (hasParcels) {
                            Log.d("FilterDebug", "✅ Są paczki do wyświetlenia");
                            scrollView.setVisibility(View.VISIBLE);
                            layoutEmptyParcels.setVisibility(View.GONE);
                            emptyParcelsAnim.cancelAnimation();
                        } else {
                            Log.d("FilterDebug", "❌ Nie znaleziono paczek po filtrach");
                            scrollView.setVisibility(View.GONE);
                            layoutEmptyParcels.setVisibility(View.VISIBLE);
                            emptyParcelsAnim.playAnimation();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }


    private int timeStringToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // 👀 Jeśli użytkownik wcześniej odrzucił, pokaż dialog z wyjaśnieniem
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Allow location access?")
                        .setMessage("Replate needs your location to show nearby food parcels and offers around you.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            requestPermissions(
                                    new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                    },
                                    LOCATION_PERMISSION_REQUEST_CODE
                            );
                        })
                        .setNegativeButton("No thanks", (dialog, which) -> {
                            Toast.makeText(requireContext(),
                                    "Location permission denied — only general parcels will be shown.",
                                    Toast.LENGTH_LONG).show();
                        })
                        .show();
            } else {
                // Poproś o pozwolenie bez dialogu, jeśli pierwszy raz
                requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        } else {
            Log.d("PermissionCheck", "✅ Location permission already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Location access granted ✅", Toast.LENGTH_SHORT).show();
                // Można tu wywołać ponowne wczytanie paczek z aktualnej lokalizacji
                loadParcelsAndDisplay();
            } else {
                Toast.makeText(requireContext(), "Location access denied ❌", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
