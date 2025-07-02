package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ParcelActivity extends AppCompatActivity {

    private GoogleMap map;
    private ImageButton favButton;
    private boolean isLiked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel);

        CardView stickyFooter = findViewById(R.id.stickyFooter);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String parcelId = getIntent().getStringExtra("parcelId");
        ImageButton backButton = findViewById(R.id.backButton);
        favButton = findViewById(R.id.favButton);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> map = googleMap);
        }

        if (parcelId != null) {
            db.collection("parcels").document(parcelId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {

                            String creatorId = documentSnapshot.getString("userId");
                            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                                    : null;

                            TextView userLogin = findViewById(R.id.userLogin);
                            ImageView userAvatar = findViewById(R.id.userAvatar);

                            if (creatorId != null) {
                                db.collection("users").document(creatorId)
                                        .get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                // LOGIN
                                                String login = userDoc.getString("login");
                                                if (login != null) {
                                                    userLogin.setText(login);
                                                }

                                                // AVATAR
                                                String profileImageBase64 = userDoc.getString("profileImageBase64");
                                                if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                                                    try {
                                                        if (profileImageBase64.contains(",")) {
                                                            profileImageBase64 = profileImageBase64.substring(profileImageBase64.indexOf(",") + 1);
                                                        }
                                                        byte[] decodedBytes = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                                        userAvatar.setImageBitmap(decodedBitmap);
                                                    } catch (IllegalArgumentException e) {
                                                        e.printStackTrace();
                                                        userAvatar.setImageResource(R.drawable.image_profile); // default
                                                    }
                                                } else {
                                                    userAvatar.setImageResource(R.drawable.image_profile); // default
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            userLogin.setText("Unknown user");
                                            userAvatar.setImageResource(R.drawable.image_profile);
                                        });
                            }


                            Button rescueButton = findViewById(R.id.rescueButton);
                            Log.d("liked", "curruserid: " + currentUserId + " creatorid " + creatorId);

                            if (currentUserId != null && creatorId != null && currentUserId.equals(creatorId)) {
                                // Paczka należy do aktualnego użytkownika
                                favButton.setVisibility(View.GONE);
                                stickyFooter.setCardBackgroundColor(Color.parseColor("#FDFFDADD"));
                                rescueButton.setText("Delete");
                                rescueButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
                                rescueButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                                rescueButton.setOnClickListener(v -> {
                                    db.collection("parcels").document(parcelId)
                                            .delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(this, "Parcel cancelled", Toast.LENGTH_SHORT).show();
                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("deleted", true);
                                                setResult(RESULT_OK, resultIntent);
                                                finish();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show()
                                            );
                                });

                            } else if (currentUserId != null && creatorId != null) {
                                // Paczka innego użytkownika — pokaż "Rescue" i serduszko
                                favButton.setVisibility(View.VISIBLE);
                                stickyFooter.setCardBackgroundColor(Color.parseColor("#FDFFFADA"));
                                rescueButton.setText("Rescue");
                                rescueButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));
                                rescueButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));

                                // Sprawdzenie, czy polubiono już tego twórcę
                                FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                                        .get()
                                        .addOnSuccessListener(userSnapshot -> {
                                            List<String> likedUsers = (List<String>) userSnapshot.get("likedUsers");
                                            isLiked = likedUsers != null && likedUsers.contains(creatorId);

                                            // Ikona serduszka na start
                                            if (isLiked) {
                                                favButton.setImageResource(R.drawable.ic_favouritefull);
                                                favButton.setColorFilter(Color.parseColor("#F40F0F"));
                                            } else {
                                                favButton.setImageResource(R.drawable.ic_favourite);
                                                favButton.setColorFilter(Color.parseColor("#F8F8F8"));
                                            }

                                            // Obsługa kliknięcia w serduszko
                                            favButton.setOnClickListener(v -> {
                                                FirebaseFirestore dbUpdate = FirebaseFirestore.getInstance();
                                                if (isLiked) {
                                                    dbUpdate.collection("users").document(currentUserId)
                                                            .update("likedUsers", FieldValue.arrayRemove(creatorId))
                                                            .addOnSuccessListener(unused -> {
                                                                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                                                                favButton.setImageResource(R.drawable.ic_favourite);
                                                                favButton.setColorFilter(Color.parseColor("#F8F8F8"));
                                                                isLiked = false;
                                                            });
                                                } else {
                                                    dbUpdate.collection("users").document(currentUserId)
                                                            .update("likedUsers", FieldValue.arrayUnion(creatorId))
                                                            .addOnSuccessListener(unused -> {
                                                                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                                                                favButton.setImageResource(R.drawable.ic_favouritefull);
                                                                favButton.setColorFilter(Color.parseColor("#F40F0F"));
                                                                isLiked = true;
                                                            });
                                                }
                                            });
                                        });

                                rescueButton.setOnClickListener(v -> {
                                    Toast.makeText(this, "Parcel rescued!", Toast.LENGTH_SHORT).show();
                                    // Dodaj logikę rezerwacji paczki tutaj
                                });
                            }


                            String title = documentSnapshot.getString("title");
                            String description = documentSnapshot.getString("description");
                            String startTime = documentSnapshot.getString("startTime");
                            String endTime = documentSnapshot.getString("endTime");
                            String day = documentSnapshot.getString("day");
                            String address = documentSnapshot.getString("address");
                            Double price = documentSnapshot.getDouble("price");
                            Double rating = documentSnapshot.getDouble("rating");

                            SharedPreferences prefs = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                            String whenText = prefs.getString("when", "Today");

                            String timeText = startTime != null && endTime != null
                                    ? whenText + ", " + startTime + " - " + endTime
                                    : whenText;

                            ((TextView) findViewById(R.id.title)).setText(title);
                            ((TextView) findViewById(R.id.description)).setText(description);
                            ((TextView) findViewById(R.id.time)).setText(timeText);

                            TextView ratingView = findViewById(R.id.ratings);
                            if (rating != null && rating > 0) {
                                ratingView.setText("🔥 " + String.format(Locale.US, "%.1f", rating));
                            } else {
                                ratingView.setText("No ratings");
                            }

                            ((TextView) findViewById(R.id.priceTag))
                                    .setText(String.format(Locale.US, "%.2f zł", price != null ? price : 0.0));

                            // 🌍 Geolokalizacja adresu i ustawienie markera na mapie
                            if (address != null && map != null) {
                                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocationName(address, 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        Address location = addresses.get(0);
                                        LatLng parcelLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                        map.addMarker(new MarkerOptions()
                                                .position(parcelLocation)
                                                .title("Pick-up Location"));
                                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(parcelLocation, 16));
                                    }
                                } catch (IOException e) {
                                    Toast.makeText(this, "Could not find address on map", Toast.LENGTH_SHORT).show();
                                }
                            }

                            String placeId = documentSnapshot.getString("placeId");
                            if (placeId != null && !placeId.isEmpty()) {
                                findViewById(R.id.mapFragment).setOnClickListener(v -> {
                                    // Użycie placeid w linku do mapy
                                    Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=Google&query_place_id=" + placeId);
                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                    mapIntent.setPackage("com.google.android.apps.maps");

                                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                        startActivity(mapIntent);
                                    } else {
                                        Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            // Wyświetlenie tagów
                            LinearLayout tagContainer = findViewById(R.id.tagContainer);
                            tagContainer.removeAllViews();
                            List<String> tags = (List<String>) documentSnapshot.get("tags");
                            if (tags != null) {
                                for (String tag : tags) {
                                    TextView tagView = new TextView(this);
                                    tagView.setText(tag);
                                    tagView.setTextSize(15);
                                    tagView.setTextColor(ContextCompat.getColor(this, R.color.brown));
                                    tagView.setBackgroundResource(R.drawable.rounded_rect_white);
                                    tagView.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_green));
                                    tagView.setPadding(24, 12, 24, 12);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );
                                    params.setMargins(0, 0, 16, 0);
                                    tagView.setLayoutParams(params);
                                    tagContainer.addView(tagView);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load parcel data", Toast.LENGTH_SHORT).show());
        }


    }


}
