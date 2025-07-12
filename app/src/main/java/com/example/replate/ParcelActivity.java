package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;
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
    private Double price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel);
        CardView stickyFooter = findViewById(R.id.stickyFooter);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String parcelId = getIntent().getStringExtra("parcelId");
        boolean isReserved = getIntent().getBooleanExtra("reserved", false);
        ImageButton backButton = findViewById(R.id.backButton);
        LinearLayout parcelOwnerLayout = findViewById(R.id.parcelOwner);
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

                                parcelOwnerLayout.setOnClickListener(v -> {
                                    Intent intent = new Intent(ParcelActivity.this, HomeActivity.class);
                                    intent.putExtra("showUserId", creatorId);
                                    intent.putExtra("openProfileFragment", true);
                                    startActivity(intent);
                                });

                                db.collection("users").document(creatorId)
                                        .get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String login = userDoc.getString("login");
                                                if (login != null) userLogin.setText(login);

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
                                                        userAvatar.setImageResource(R.drawable.image_profile);
                                                    }
                                                } else {
                                                    userAvatar.setImageResource(R.drawable.image_profile);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            userLogin.setText("Unknown user");
                                            userAvatar.setImageResource(R.drawable.image_profile);
                                        });
                            }

                            Button rescueButton = findViewById(R.id.rescueButton);

                            if (currentUserId != null && creatorId != null && currentUserId.equals(creatorId)) {
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
                                                setResult(RESULT_OK, new Intent().putExtra("deleted", true));
                                                finish();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show());
                                });

                            } else if (currentUserId != null && creatorId != null) {
                                favButton.setVisibility(View.VISIBLE);
                                stickyFooter.setCardBackgroundColor(Color.parseColor("#FDFFFADA"));

                                if (isReserved) {
                                    rescueButton.setText("Cancel");
                                    rescueButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
                                    rescueButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                                    rescueButton.setOnClickListener(v -> {
                                        AlertDialog dialog = new AlertDialog.Builder(this)
                                                .setTitle("Cancel Reservation")
                                                .setMessage("The parcel was ready for its second life! Really cancel the rescue?")
                                                .setPositiveButton("Yes", (dialogInterface, which) -> {
                                                    db.collection("parcels").document(parcelId)
                                                            .update("status", "active")
                                                            .addOnSuccessListener(aVoid -> {
                                                                db.collection("users").document(currentUserId)
                                                                        .update("orders", FieldValue.arrayRemove(parcelId))
                                                                        .addOnSuccessListener(unused -> {
                                                                            Toast.makeText(this, "Reservation cancelled", Toast.LENGTH_SHORT).show();
                                                                            startActivity(new Intent(ParcelActivity.this, HomeActivity.class).putExtra("openOrdersFragment", true));
                                                                            finish();
                                                                        })
                                                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update user orders", Toast.LENGTH_SHORT).show());
                                                            })
                                                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update parcel status", Toast.LENGTH_SHORT).show());
                                                })
                                                .setNegativeButton("No", null)
                                                .create();
                                        dialog.show();
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.brown));
                                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.yellow));
                                    });

                                } else {
                                    rescueButton.setText("Rescue");
                                    rescueButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));
                                    rescueButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));

                                    FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                                            .get()
                                            .addOnSuccessListener(userSnapshot -> {
                                                List<String> likedUsers = (List<String>) userSnapshot.get("likedUsers");
                                                isLiked = likedUsers != null && likedUsers.contains(creatorId);

                                                favButton.setImageResource(isLiked ? R.drawable.ic_favouritefull : R.drawable.ic_favourite);
                                                favButton.setColorFilter(Color.parseColor(isLiked ? "#F40F0F" : "#F8F8F8"));

                                                favButton.setOnClickListener(v -> {
                                                    FirebaseFirestore dbUpdate = FirebaseFirestore.getInstance();
                                                    if (isLiked) {
                                                        dbUpdate.collection("users").document(currentUserId)
                                                                .update("likedUsers", FieldValue.arrayRemove(creatorId));
                                                        dbUpdate.collection("users").document(creatorId)
                                                                .update("likedBy", FieldValue.arrayRemove(currentUserId))
                                                                .addOnSuccessListener(unused -> {
                                                                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                                                                    favButton.setImageResource(R.drawable.ic_favourite);
                                                                    favButton.setColorFilter(Color.parseColor("#F8F8F8"));
                                                                    isLiked = false;
                                                                });
                                                    } else {
                                                        dbUpdate.collection("users").document(currentUserId)
                                                                .update("likedUsers", FieldValue.arrayUnion(creatorId));
                                                        dbUpdate.collection("users").document(creatorId)
                                                                .update("likedBy", FieldValue.arrayUnion(currentUserId))
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
                                        if (price != null && price == 0.0) {
                                            db.collection("parcels").document(parcelId)
                                                    .update("status", "reserved")
                                                    .addOnSuccessListener(unused -> {
                                                        db.collection("users").document(currentUserId)
                                                                .update("orders", FieldValue.arrayUnion(parcelId))
                                                                .addOnSuccessListener(aVoid -> {
                                                                    Toast.makeText(this, "Parcel reserved!", Toast.LENGTH_SHORT).show();
                                                                    startActivity(new Intent(ParcelActivity.this, HomeActivity.class).putExtra("openOrdersFragment", true));
                                                                    finish();
                                                                })
                                                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to reserve parcel", Toast.LENGTH_SHORT).show());
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to reserve parcel", Toast.LENGTH_SHORT).show());
                                        } else {
                                            Toast.makeText(this, "This parcel is not free — coming soon", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            String title = documentSnapshot.getString("title");
                            String description = documentSnapshot.getString("description");
                            String startTime = documentSnapshot.getString("startTime");
                            String endTime = documentSnapshot.getString("endTime");
                            String day = documentSnapshot.getString("day");
                            String address = documentSnapshot.getString("address");
                            price = documentSnapshot.getDouble("price");
                            Double rating = documentSnapshot.getDouble("rating");

                            SharedPreferences prefs = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                            String whenText = prefs.getString("when", "Today");

                            String timeText = startTime != null && endTime != null
                                    ? whenText + ", " + startTime + " - " + endTime
                                    : whenText;

                            ((TextView) findViewById(R.id.title)).setText(title);
                            ((TextView) findViewById(R.id.description)).setText(description);
                            ((TextView) findViewById(R.id.time)).setText(timeText);
                            ((TextView) findViewById(R.id.address)).setText(address);

                            TextView ratingView = findViewById(R.id.ratings);
                            ratingView.setText((rating != null && rating > 0) ? "🔥 " + String.format(Locale.US, "%.1f", rating) : "No ratings");

                            TextView priceTextView = findViewById(R.id.priceTag);
                            priceTextView.setText((price != null && price == 0.0) ? "Free" : String.format(Locale.US, "%.2f zł", price != null ? price : 0.0));

                            if (address != null && map != null) {
                                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocationName(address, 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        Address location = addresses.get(0);
                                        LatLng parcelLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                        map.addMarker(new MarkerOptions().position(parcelLocation).title("Pick-up Location"));
                                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(parcelLocation, 16));
                                    }
                                } catch (IOException e) {
                                    Toast.makeText(this, "Could not find address on map", Toast.LENGTH_SHORT).show();
                                }
                            }

                            String placeId = documentSnapshot.getString("placeId");
                            if (placeId != null && !placeId.isEmpty()) {
                                findViewById(R.id.mapFragment).setOnClickListener(v -> {
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
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    params.setMargins(0, 0, 16, 0);
                                    tagView.setLayoutParams(params);
                                    tagContainer.addView(tagView);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to load parcel data", Toast.LENGTH_SHORT).show());
        }
    }
}
